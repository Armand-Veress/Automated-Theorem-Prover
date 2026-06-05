package com.theorem_solver.backend.compiler.grounding;

import com.theorem_solver.backend.compiler.ast.*;
import java.util.*;

public class Grounder {

    private final Set<String> domain;
    private int skolemCounter = 0;
    private final int maxDepth;

    public Grounder(Set<String> constants, int maxDepth) {
        this.domain = new HashSet<>(constants.isEmpty() ? Set.of("d") : constants);
        this.maxDepth = maxDepth;
    }

    public LogicNode ground(LogicNode node) {
        return groundRecursive(node, 0);
    }

    private LogicNode groundRecursive(LogicNode node, int depth) {
        if (depth >= maxDepth) {
            throw new RuntimeException("Depth limit exceeded. Formula is too complex.");
        }

        if (node instanceof QuantifierNode quantifier) {
            return expandQuantifier(quantifier, depth);
        }
        if (node instanceof BinaryOpNode binary) {
            return new BinaryOpNode(
                    binary.getOperator(),
                    groundRecursive(binary.getLeft(), depth),
                    groundRecursive(binary.getRight(), depth)
            );
        }
        if (node instanceof NotNode notNode) {
            return new NotNode(groundRecursive(notNode.getOperand(), depth));
        }
        if (node instanceof PredicateNode predicate) {
            return flattenPredicate(predicate);
        }
        return node;
    }

    private LogicNode expandQuantifier(QuantifierNode quantifier, int depth) {
        String varName = quantifier.getVariable().getName();
        LogicNode body = quantifier.getBody();

        if (quantifier.getQuantifier().equalsIgnoreCase("EXISTS")) {
            skolemCounter++;
            String skolemConst = "sk_" + varName + "_" + skolemCounter;

            this.domain.add(skolemConst);

            return groundRecursive(substitute(body, varName, skolemConst), depth + 1);
        }


        List<LogicNode> expandedNodes = new ArrayList<>();
        List<String> currentDomainSnapshot = new ArrayList<>(this.domain);

        for (String constant : currentDomainSnapshot) {
            expandedNodes.add(groundRecursive(substitute(body, varName, constant), depth + 1));
        }
        return combineNodes(expandedNodes, "AND");
    }

    private LogicNode substitute(LogicNode node, String varName, String constant) {
        if (node instanceof PredicateNode pred) {
            List<VariableNode> newArgs = new ArrayList<>();
            for (VariableNode arg : pred.getArguments()) {
                if (arg.getName().equals(varName)) {
                    newArgs.add(new VariableNode(constant));
                } else {
                    newArgs.add(arg);
                }
            }
            return new PredicateNode(pred.getName(), newArgs);
        }
        if (node instanceof BinaryOpNode binary) {
            return new BinaryOpNode(
                    binary.getOperator(),
                    substitute(binary.getLeft(), varName, constant),
                    substitute(binary.getRight(), varName, constant)
            );
        }
        if (node instanceof NotNode notNode) {
            return new NotNode(substitute(notNode.getOperand(), varName, constant));
        }
        if (node instanceof QuantifierNode q) {
            if (q.getVariable().getName().equals(varName)) {
                return q;
            }
            return new QuantifierNode(
                    q.getQuantifier(),
                    q.getVariable(),
                    substitute(q.getBody(), varName, constant)
            );
        }
        return node;
    }

    private LogicNode combineNodes(List<LogicNode> nodes, String operator) {
        if (nodes.isEmpty()) return null;
        LogicNode result = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            result = new BinaryOpNode(operator, result, nodes.get(i));
        }
        return result;
    }

    private LogicNode flattenPredicate(PredicateNode predicate) {
        if (predicate.getArguments().isEmpty()) {
            return new VariableNode(predicate.getName());
        }
        StringBuilder sb = new StringBuilder(predicate.getName());
        for (VariableNode arg : predicate.getArguments()) {
            sb.append("_").append(arg.getName());
        }
        return new VariableNode(sb.toString());
    }
}