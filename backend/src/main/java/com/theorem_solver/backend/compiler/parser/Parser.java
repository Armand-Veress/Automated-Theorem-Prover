package com.theorem_solver.backend.compiler.parser;

import com.theorem_solver.backend.compiler.ast.*;
import com.theorem_solver.backend.compiler.lexer.Token;
import com.theorem_solver.backend.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int idx;

    private Token consume(TokenType expectedType, String errorMessage) {
        if (peek().type() == expectedType) {
            return advance();
        }
        throw new RuntimeException(errorMessage + " Token: " + peek().type());
    }

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        idx = 0;
    }

    private Token peek () {
        if (idx >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(idx);
    }

    private Token advance() {
        if (idx >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        idx++;
        return tokens.get(idx - 1);
    }

    private LogicNode parsePrimary() {
        if (peek().type() == TokenType.PREDICATE) {
            String name = advance().value();
            List<VariableNode> args = new ArrayList<>();

            if (peek().type() == TokenType.LPAREN) {
                advance();
                if (peek().type() == TokenType.VARIABLE) {
                    args.add(new VariableNode(advance().value()));

                    while (peek().type() == TokenType.COMMA) {
                        advance();
                        Token varToken = consume(TokenType.VARIABLE, " Variable expected after ',' ");
                        args.add(new VariableNode(varToken.value()));
                    }
                }
                consume(TokenType.RPAREN, " ')' expected after the Predicate's arguments ");
            }

            return new PredicateNode(name, args);
        }

        if (peek().type() == TokenType.LPAREN) {
            advance();
            LogicNode expr = parseExpression();
            consume(TokenType.RPAREN, " ')' expected for closing the group ");
            return expr;
        }

        throw new RuntimeException("Syntax error: unexpected token type at primary level - " + peek().type());
    }

    private LogicNode parseUnary() {
        if (peek().type() == TokenType.NOT) {
            advance();
            LogicNode operand = parseUnary();
            return new NotNode(operand);
        }

        if (peek().type() == TokenType.FORALL || peek().type() == TokenType.EXISTS) {
            String quantifier = advance().value();
            Token varToken = consume(TokenType.VARIABLE, " Variable expected after a Quantifier ");
            VariableNode variable = new VariableNode(varToken.value());
            LogicNode body = parseUnary();
            return new QuantifierNode(quantifier, variable, body);
        }

        return parsePrimary();
    }

    private int getPrecedence(TokenType type) {
        if (type == TokenType.AND) return 3;
        if (type == TokenType.OR || type == TokenType.XOR) return 2;
        if (type == TokenType.IMPLIES || type == TokenType.IFF) return 1;
        return 0;
    }

    private LogicNode parseBinary(int minPrecedence) {
        LogicNode left = parseUnary();

        while (true) {
            int currentPrecedence = getPrecedence(peek().type());
            if (currentPrecedence < minPrecedence || currentPrecedence == 0) {
                break;
            }

            String operator = advance().value();
            if (operator.equals("^")) operator = "AND";
            if (operator.equals("v")) operator = "OR";
            if (operator.equals("->")) operator = "IMPLIES";
            LogicNode right = parseBinary(currentPrecedence + 1);
            left = new BinaryOpNode(operator, left, right);
        }

        return left;
    }

    private LogicNode parseExpression() {
        return parseBinary(1);
    }

    public LogicNode parse() {
        LogicNode ast = parseExpression();
        consume(TokenType.EOF, " Syntax error: residual elements after the expression ");
        return ast;
    }
}
