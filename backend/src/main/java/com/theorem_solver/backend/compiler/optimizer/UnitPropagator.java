package com.theorem_solver.backend.compiler.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnitPropagator {

    public static class Result {
        public final List<List<String>> clauses;
        public final Map<String, Boolean> deducedVariables;

        public Result(List<List<String>> clauses, Map<String, Boolean> deducedVariables) {
            this.clauses = clauses;
            this.deducedVariables = deducedVariables;
        }
    }

    public static Result optimizeCNF(List<List<String>> cnf) {
        boolean changed = true;
        Map<String, Boolean> deduced = new HashMap<>();

        while (changed) {
            changed = false;

            if (removeTautologies(cnf)) {
                changed = true;
            }

            if (removePureLiterals(cnf, deduced)) {
                changed = true;
            }

            String unitLiteral = findUnitLiteral(cnf);
            if (unitLiteral != null) {
                recordDeducedVariable(unitLiteral, deduced);
                String complementLiteral = getComplement(unitLiteral);
                Iterator<List<String>> clauseIterator = cnf.iterator();

                while (clauseIterator.hasNext()) {
                    List<String> clause = clauseIterator.next();
                    if (clause.contains(unitLiteral)) {
                        clauseIterator.remove();
                        changed = true;
                    } else if (clause.contains(complementLiteral)) {
                        clause.remove(complementLiteral);
                        changed = true;

                        if (clause.isEmpty()) {
                            List<List<String>> contradiction = new ArrayList<>();
                            contradiction.add(new ArrayList<>());
                            return new Result(contradiction, deduced);
                        }
                    }
                }
            }
        }

        return new Result(cnf, deduced);
    }

    private static boolean removeTautologies(List<List<String>> cnf) {
        boolean changed = false;
        Iterator<List<String>> iterator = cnf.iterator();

        while (iterator.hasNext()) {
            List<String> clause = iterator.next();
            Set<String> literalSet = new HashSet<>(clause);

            for (String literal : literalSet) {
                if (literalSet.contains(getComplement(literal))) {
                    iterator.remove();
                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }

    private static boolean removePureLiterals(List<List<String>> cnf, Map<String, Boolean> deduced) {
        boolean changed = false;
        Set<String> allLiterals = new HashSet<>();

        for (List<String> clause : cnf) {
            allLiterals.addAll(clause);
        }

        List<String> pureLiterals = new ArrayList<>();

        for (String literal : allLiterals) {
            if (!allLiterals.contains(getComplement(literal))) {
                pureLiterals.add(literal);
            }
        }

        if (!pureLiterals.isEmpty()) {
            for (String pure : pureLiterals) {
                recordDeducedVariable(pure, deduced);
                Iterator<List<String>> iterator = cnf.iterator();

                while (iterator.hasNext()) {
                    List<String> clause = iterator.next();
                    if (clause.contains(pure)) {
                        iterator.remove();
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private static void recordDeducedVariable(String literal, Map<String, Boolean> deduced) {
        literal = literal.trim();
        boolean isPositive = true;
        String varName = literal;

        if (literal.startsWith("!")) {
            isPositive = false;
            varName = literal.substring(1).trim();
        } else if (literal.startsWith("NOT ")) {
            isPositive = false;
            varName = literal.substring(4).trim();
        } else if (literal.startsWith("NOT_")) {
            isPositive = false;
            varName = literal.substring(4).trim();
        } else if (literal.startsWith("~")) {
            isPositive = false;
            varName = literal.substring(1).trim();
        } else if (literal.startsWith("-")) {
            isPositive = false;
            varName = literal.substring(1).trim();
        }

        if (!varName.startsWith("aux_") && !varName.startsWith("cnf_")) {
            deduced.put(varName, isPositive);
        }
    }

    private static String findUnitLiteral(List<List<String>> cnf) {
        for (List<String> clause : cnf) {
            if (clause.size() == 1) {
                return clause.get(0);
            }
        }
        return null;
    }

    private static String getComplement(String literal) {
        literal = literal.trim();
        if (literal.startsWith("!")) {
            return literal.substring(1).trim();
        } else if (literal.startsWith("NOT ")) {
            return literal.substring(4).trim();
        } else if (literal.startsWith("NOT_")) {
            return literal.substring(4).trim();
        } else if (literal.startsWith("~")) {
            return literal.substring(1).trim();
        } else if (literal.startsWith("-")) {
            return literal.substring(1).trim();
        }
        return "!" + literal;
    }
}