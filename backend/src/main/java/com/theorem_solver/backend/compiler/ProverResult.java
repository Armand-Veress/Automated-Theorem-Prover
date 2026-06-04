package com.theorem_solver.backend.compiler;

import java.util.List;
import java.util.Map;

public record ProverResult(
        boolean valid,
        Map<String, Boolean> counterexample,
        List<String> log, String solvedBy,
        int matrixRows,
        int matrixCols,
        double executionTimeMs
) {}