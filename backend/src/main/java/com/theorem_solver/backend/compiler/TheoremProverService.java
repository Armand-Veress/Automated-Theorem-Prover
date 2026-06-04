package com.theorem_solver.backend.compiler;

import com.theorem_solver.backend.api.*;
import com.theorem_solver.backend.compiler.ast.*;
import com.theorem_solver.backend.compiler.grounding.*;
import com.theorem_solver.backend.compiler.reducer.*;
import com.theorem_solver.backend.compiler.lexer.*;
import com.theorem_solver.backend.compiler.parser.*;
import com.theorem_solver.backend.compiler.optimizer.UnitPropagator;
import com.theorem_solver.backend.nativeinterop.NativeSolverService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.*;

@Service
public class TheoremProverService {

    private final CnfTransformer transformer = new CnfTransformer();
    private final SatToExactCoverReducer reducer = new SatToExactCoverReducer();
    private final NativeSolverService nativeSolver = new NativeSolverService();
    private final SolutionInterpreter interpreter = new SolutionInterpreter();

    public ProverResult prove(List<String> axioms, String theorem) {
        long startTime = System.nanoTime();
        List<String> log = new ArrayList<>();
        log.add("[INFO] Initialization: Formulating combined expression.");

        try {
            String combinedFormula = combine(axioms, theorem);

            List<Token> tokens = new Lexer().tokenize(combinedFormula);
            LogicNode ast = new Parser(tokens).parse();

            Set<String> constants = Extractors.findConstants(ast);
            ast = new Grounder(constants, 10).ground(ast);

            LogicNode cnf = transformer.convertToCNF(ast);
            List<List<String>> clauses = transformer.extractClauses(cnf);

            UnitPropagator.Result propagationResult = UnitPropagator.optimizeCNF(clauses);
            List<List<String>> optimized = propagationResult.clauses;
            Map<String, Boolean> deducedVariables = propagationResult.deducedVariables;

            if (optimized.isEmpty()) {
                double duration = (System.nanoTime() - startTime) / 1_000_000.0;
                return new ProverResult(false, deducedVariables, log, "Java Optimizer", 0, 0, duration);
            }

            if (optimized.size() == 1 && optimized.get(0).isEmpty()) {
                double duration = (System.nanoTime() - startTime) / 1_000_000.0;
                return new ProverResult(true, null, log, "Java Optimizer", 0, 0, duration);
            }

            ReductionResult reduction = reducer.reduceToExactCover(optimized);
            List<List<String>> matrixList = reduction.matrix();

            if (matrixList.isEmpty() || matrixList.get(0).isEmpty()) {
                double duration = (System.nanoTime() - startTime) / 1_000_000.0;
                return new ProverResult(true, null, log, "Reducer (Empty Matrix)", 0, 0, duration);
            }

            int rows = matrixList.size();
            int maxCols = 0;
            for (List<String> row : matrixList) {
                if (row.size() > maxCols) maxCols = row.size();
            }

            log.add(String.format("[INFO] NativeInterop: Sending %dx%d matrix to C++.", rows, maxCols));

            int[] nativeSolution = null;
            int time_limit = 5;
            try {
                nativeSolution = CompletableFuture.supplyAsync(() -> {
                    return nativeSolver.solve(matrixList);
                }).get(time_limit, TimeUnit.SECONDS);

            } catch (TimeoutException e) {
                double duration = (System.nanoTime() - startTime) / 1_000_000.0;
                Map<String, Boolean> timeoutState = new HashMap<>();
                timeoutState.put("TIMEOUT_EXCEEDED", true);
                return new ProverResult(false, timeoutState, log, "C++ Engine (Timeout)", rows, maxCols, duration);
            }

            double finalDuration = (System.nanoTime() - startTime) / 1_000_000.0;

            if (nativeSolution != null && nativeSolution.length == 1 && nativeSolution[0] == -1) {
                Map<String, Boolean> timeoutState = new HashMap<>();
                timeoutState.put("TIMEOUT_EXCEEDED", true);
                return new ProverResult(false, timeoutState, log, "C++ Engine (Timeout)", rows, maxCols, finalDuration);
            }

            if (nativeSolution == null || nativeSolution.length == 0) {
                return new ProverResult(true, null, log, "C++ Engine (DLX)", rows, maxCols, finalDuration);
            }

            Map<String, Boolean> counterexample = interpreter.interpret(nativeSolution, reduction.rowAssignments());
            counterexample.putAll(deducedVariables);

            return new ProverResult(false, counterexample, log, "C++ Engine (DLX)", rows, maxCols, finalDuration);

        } catch (Throwable t) {
            double finalDuration = (System.nanoTime() - startTime) / 1_000_000.0;
            Map<String, Boolean> errorState = new HashMap<>();
            errorState.put("BACKEND_CRASH", true);
            return new ProverResult(false, errorState, log, "System (Crash)", 0, 0, finalDuration);
        }
    }

    private String combine(List<String> axioms, String theorem) {
        StringBuilder sb = new StringBuilder();
        for (String axiom : axioms) {
            sb.append("( ").append(axiom).append(" ) AND ");
        }
        sb.append("NOT ( ").append(theorem).append(" )");
        return sb.toString();
    }
}