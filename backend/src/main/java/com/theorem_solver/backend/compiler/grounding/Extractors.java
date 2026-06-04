package com.theorem_solver.backend.compiler.grounding;

import com.theorem_solver.backend.compiler.ast.*;
import java.util.HashSet;
import java.util.Set;

public class Extractors {

    public static Set<String> findConstants(LogicNode node) {
        Set<String> constants = new HashSet<>();
        findConstantsRecursive(node, constants);
        return constants;
    }

    private static void findConstantsRecursive(LogicNode node, Set<String> constants) {
        if (node instanceof PredicateNode pred) {
            for (VariableNode arg : pred.getArguments()) {
                if (arg.getName().length() > 1) {
                    constants.add(arg.getName());
                }
            }
        } else if (node instanceof BinaryOpNode bin) {
            findConstantsRecursive(bin.getLeft(), constants);
            findConstantsRecursive(bin.getRight(), constants);
        } else if (node instanceof NotNode notNode) {
            findConstantsRecursive(notNode.getOperand(), constants);
        } else if (node instanceof QuantifierNode q) {
            findConstantsRecursive(q.getBody(), constants);
            constants.remove(q.getVariable().getName());
        }
    }
}