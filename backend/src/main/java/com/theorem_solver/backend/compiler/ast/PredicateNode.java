package com.theorem_solver.backend.compiler.ast;

import java.util.List;

public class PredicateNode implements LogicNode {
    private final String name;
    private final List<VariableNode> arguments;

    public PredicateNode(String name, List<VariableNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public List<VariableNode> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        if (arguments.isEmpty()) {
            return name;
        }

        StringBuilder sb = new StringBuilder(name).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(arguments.get(i).toString());
            if (i < arguments.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}