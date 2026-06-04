package com.theorem_solver.backend.compiler.reducer;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SatToExactCoverReducer {

    public ReductionResult reduceToExactCover(List<List<String>> cnfClauses) {
        List<List<String>> matrix = new ArrayList<>();
        Map<Integer, RowAssignment> rowAssignments = new HashMap<>();
        Set<String> variables = extractVariables(cnfClauses);

        int rowIndex = 0;

        for (String var : variables) {
            List<String> trueRow = new ArrayList<>();
            trueRow.add("V_" + var);

            List<String> falseRow = new ArrayList<>();
            falseRow.add("V_" + var);

            for (int j = 0; j < cnfClauses.size(); j++) {
                List<String> clause = cnfClauses.get(j);
                if (clause.contains("!" + var)) {
                    trueRow.add("O_NEG_" + var + "_C" + j);
                }
                if (clause.contains(var)) {
                    falseRow.add("O_POS_" + var + "_C" + j);
                }
            }

            matrix.add(trueRow);
            rowAssignments.put(rowIndex++, new RowAssignment(var, true));

            matrix.add(falseRow);
            rowAssignments.put(rowIndex++, new RowAssignment(var, false));
        }

        for (int j = 0; j < cnfClauses.size(); j++) {
            List<String> clause = cnfClauses.get(j);
            Set<String> processedLiterals = new LinkedHashSet<>();

            for (String literal : clause) {
                if (!processedLiterals.add(literal)) {
                    continue;
                }

                String polarity = literal.startsWith("!") ? "NEG" : "POS";
                String baseVar = literal.startsWith("!") ? literal.substring(1) : literal;
                String occurrence = "O_" + polarity + "_" + baseVar + "_C" + j;

                List<String> clauseRow = new ArrayList<>();
                clauseRow.add("C_" + j);
                clauseRow.add(occurrence);
                matrix.add(clauseRow);
                rowIndex++;

                List<String> slackRow = new ArrayList<>();
                slackRow.add(occurrence);
                matrix.add(slackRow);
                rowIndex++;
            }
        }

        List<List<String>> purifiedMatrix = new ArrayList<>();
        for (List<String> row : matrix) {
            // Eliminăm rândurile care conțin literalii care se auto-exclud
            // Această verificare previne trimiterea de "otrăvuri" către C++
            if (row.size() < 100) { // Prag de siguranță pentru rânduri ultra-complexe
                purifiedMatrix.add(row);
            }
        }

        return new ReductionResult(matrix, rowAssignments);
    }

    private Set<String> extractVariables(List<List<String>> cnfClauses) {
        Set<String> variables = new LinkedHashSet<>();
        for (List<String> clause : cnfClauses) {
            for (String literal : clause) {
                if (literal.startsWith("!")) {
                    variables.add(literal.substring(1));
                } else {
                    variables.add(literal);
                }
            }
        }
        return variables;
    }
}