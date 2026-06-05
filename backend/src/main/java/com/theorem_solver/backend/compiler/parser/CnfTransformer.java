package com.theorem_solver.backend.compiler.parser;

import com.theorem_solver.backend.compiler.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CnfTransformer {
    public CnfTransformer() {}

    public LogicNode eliminateImplications (LogicNode node) {
        if (node instanceof BinaryOpNode binaryNode) {
            LogicNode left = eliminateImplications(binaryNode.getLeft());
            LogicNode right = eliminateImplications(binaryNode.getRight());

            if (binaryNode.getOperator().equals("IMPLIES")) {
                return new BinaryOpNode("OR", new NotNode(left), right);
            }

            if (binaryNode.getOperator().equals("IFF")) {
                LogicNode implies_1 = new BinaryOpNode("OR", new NotNode(left), right);
                LogicNode implies_2 = new BinaryOpNode("OR", new NotNode(right), left);
                return new BinaryOpNode("AND", implies_1, implies_2);
            }

            return new BinaryOpNode(binaryNode.getOperator(), left, right);
        }

        if(node instanceof NotNode notNode) {
            return new NotNode(eliminateImplications(notNode.getOperand()));
        }

        if (node instanceof QuantifierNode qNode) {
            return new QuantifierNode(
                    qNode.getQuantifier(),
                    qNode.getVariable(),
                    eliminateImplications(qNode.getBody())
            );
        }

        return node;
    }

    public LogicNode applyDeMorgan(LogicNode node) {
        if(node instanceof NotNode notNode) {
            LogicNode inner = notNode.getOperand();

            if (inner instanceof NotNode doubleNot) {
                return applyDeMorgan(doubleNot.getOperand());
            }

            if (inner instanceof QuantifierNode qNode) {
                String flippedQuantifier = qNode.getQuantifier().equals("FORALL") ? "EXISTS" : "FORALL";
                LogicNode negatedBody = applyDeMorgan(new NotNode(qNode.getBody()));
                return new QuantifierNode(flippedQuantifier, qNode.getVariable(), negatedBody);
            }

            if (inner instanceof BinaryOpNode binaryNode) {
                if (binaryNode.getOperator().equals("AND")){
                    LogicNode left = applyDeMorgan(new NotNode(binaryNode.getLeft()));
                    LogicNode right = applyDeMorgan(new NotNode(binaryNode.getRight()));
                    return new BinaryOpNode("OR", left, right);
                }
                if (binaryNode.getOperator().equals("OR")){
                    LogicNode left = applyDeMorgan(new NotNode(binaryNode.getLeft()));
                    LogicNode right = applyDeMorgan(new NotNode(binaryNode.getRight()));
                    return new BinaryOpNode("AND", left, right);
                }
            }
            return new NotNode(applyDeMorgan(inner));
        }

        if (node instanceof BinaryOpNode binNode) {
            return new BinaryOpNode(
                    binNode.getOperator(),
                    applyDeMorgan(binNode.getLeft()),
                    applyDeMorgan(binNode.getRight())
            );
        }

        if (node instanceof QuantifierNode qNode) {
            return new QuantifierNode(
                    qNode.getQuantifier(),
                    qNode.getVariable(),
                    applyDeMorgan(qNode.getBody())
            );
        }

        return node;
    }

    public LogicNode distributeOrOverAnd(LogicNode node) {
        if (node instanceof BinaryOpNode binNode) {
            if (binNode.getOperator().equals("OR")) {
                LogicNode left = distributeOrOverAnd(binNode.getLeft());
                LogicNode right = distributeOrOverAnd(binNode.getRight());
                return applyDistribution(left, right);
            } else {
                return new BinaryOpNode(
                        binNode.getOperator(),
                        distributeOrOverAnd(binNode.getLeft()),
                        distributeOrOverAnd(binNode.getRight())
                );
            }
        }
        if (node instanceof NotNode notNode) {
            return new NotNode(distributeOrOverAnd(notNode.getOperand()));
        }
        return node;
    }

    private LogicNode applyDistribution(LogicNode left, LogicNode right) {
        if (left instanceof BinaryOpNode leftBin && leftBin.getOperator().equals("AND")) {
            return new BinaryOpNode(
                    "AND",
                    applyDistribution(leftBin.getLeft(), right),
                    applyDistribution(leftBin.getRight(), right)
            );
        }
        if (right instanceof BinaryOpNode rightBin && rightBin.getOperator().equals("AND")) {
            return new BinaryOpNode(
                    "AND",
                    applyDistribution(left, rightBin.getLeft()),
                    applyDistribution(left, rightBin.getRight())
            );
        }
        return new BinaryOpNode("OR", left, right);
    }

    public LogicNode convertToCNF(LogicNode root) {
        // Flux curățat: Elimina Implicatii -> DeMorgan -> Distribuie
        LogicNode step1 = eliminateImplications(root);
        LogicNode step2 = applyDeMorgan(step1);
        LogicNode step3 = distributeOrOverAnd(step2);

        return step3;
    }

    public List<List<String>> extractClauses(LogicNode node) {
        List<List<String>> clauses = new ArrayList<>();
        if (node instanceof BinaryOpNode binNode && binNode.getOperator().equals("AND")) {
            clauses.addAll(extractClauses(binNode.getLeft()));
            clauses.addAll(extractClauses(binNode.getRight()));
        } else {
            List<String> literals = extractLiterals(node);
            Set<String> uniqueLiterals = new HashSet<>(literals);
            clauses.add(new ArrayList<>(uniqueLiterals));
        }
        return clauses;
    }

    private List<String> extractLiterals(LogicNode node) {
        List<String> literals = new ArrayList<>();

        if (node instanceof BinaryOpNode binNode && binNode.getOperator().equals("OR")) {
            literals.addAll(extractLiterals(binNode.getLeft()));
            literals.addAll(extractLiterals(binNode.getRight()));
        } else {
            literals.add(node.toString());
        }

        return literals;
    }
}