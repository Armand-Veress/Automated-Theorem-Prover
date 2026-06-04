package com.theorem_solver.backend.compiler.ast;

public class QuantifierNode implements LogicNode {
    private final String quantifier;
    private final VariableNode variable;
    private final LogicNode body;

    public QuantifierNode(String quantifier, VariableNode variable, LogicNode body) {
        this.quantifier = quantifier;
        this.variable = variable;
        this.body = body;
    }

    public String getQuantifier() {
        return quantifier;
    }

    public VariableNode getVariable() {
        return variable;
    }

    public LogicNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "(" + quantifier + " " + variable.toString() + " " + body.toString() + ")";
    }
}