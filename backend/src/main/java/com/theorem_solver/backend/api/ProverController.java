package com.theorem_solver.backend.api;

import com.theorem_solver.backend.compiler.TheoremProverService;
import com.theorem_solver.backend.compiler.ProverResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prover")
@CrossOrigin(origins = "http://localhost:4200")
public class ProverController {

    private final TheoremProverService proverService;

    public ProverController(TheoremProverService proverService) {
        this.proverService = proverService;
    }

    @PostMapping("/verify")
    public ResponseEntity<ProverResponse> verifyTheorem(@RequestBody ProverRequest request) {
        ProverResult result = proverService.prove(request.axioms(), request.theorem());

        ProverResponse response = new ProverResponse(
                result.valid(),
                result.counterexample(),
                result.log(),
                result.solvedBy(),
                result.matrixRows(),
                result.matrixCols(),
                result.executionTimeMs()
        );
        return ResponseEntity.ok(response);
    }
}

record ProverRequest(List<String> axioms, String theorem) {}

record ProverResponse(
        boolean valid,
        Map<String, Boolean> counterexample,
        List<String> log, String solvedBy,
        int matrixRows,
        int matrixCols,
        double executionTimeMs
) {}