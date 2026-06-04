package com.theorem_solver.backend.compiler.ast;

public class BinaryOpNode implements LogicNode {
    private final String operator;
    private final LogicNode left;
    private final LogicNode right;

    public BinaryOpNode(String operator, LogicNode left, LogicNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public LogicNode getLeft() {
        return left;
    }

    public LogicNode getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " " + operator + " " + right.toString() + ")";
    }
}