package com.theorem_solver.backend.compiler.ast;

public class NotNode implements LogicNode {
    private final LogicNode operand;

    public NotNode(LogicNode operand) {
        this.operand = operand;
    }

    public LogicNode getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return "!" + operand.toString();
    }
}