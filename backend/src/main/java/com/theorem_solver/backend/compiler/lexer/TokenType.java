package com.theorem_solver.backend.compiler.lexer;

public enum TokenType {
    AND("AND|\\^"),
    OR("OR|v"),
    NOT("NOT|~|!"),
    IMPLIES("IMPLIES|->"),
    IFF("IFF|<->"),
    XOR("XOR"),
    FORALL("FORALL|∀"),
    EXISTS("EXISTS|∃"),

    LPAREN("\\("),
    RPAREN("\\)"),
    COMMA(","),

    PREDICATE("[A-Z][a-zA-Z0-9_]*"),
    VARIABLE("[a-z]+"),

    WHITESPACE("\\s+"),
    EOF(""),
    UNKNOWN(".")
    ;


    private final String pattern;

    TokenType(String pattern) {
        this.pattern = pattern;
    }

    String getPattern(){
        return pattern;
    }
}