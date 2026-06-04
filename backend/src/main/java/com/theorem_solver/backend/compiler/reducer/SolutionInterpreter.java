package com.theorem_solver.backend.compiler.reducer;

import java.util.HashMap;
import java.util.Map;

public class SolutionInterpreter {

    public Map<String, Boolean> interpret(int[] nativeSolution, Map<Integer, RowAssignment> assignments) {
        Map<String, Boolean> cleanModel = new HashMap<>();

        for (int rowIndex : nativeSolution) {
            RowAssignment assignment = assignments.get(rowIndex);
            if (assignment != null) {
                String name = assignment.variable();
                if (!name.startsWith("aux_") && !name.startsWith("cnf_") && !name.equals("Satisfiabil_Preprocesare")) {
                    cleanModel.put(name, assignment.isTrue());
                }
            }
        }

        return cleanModel;
    }
}