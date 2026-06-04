package com.theorem_solver.backend.compiler.reducer;

import java.util.List;
import java.util.Map;

public record ReductionResult(List<List<String>> matrix, Map<Integer, RowAssignment> rowAssignments) {}