package com.theorem_solver.backend.compiler.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private final Pattern pattern;

    public Lexer(){
        StringBuilder regexBuilder = new StringBuilder();
        for(TokenType type : TokenType.values()) {
            regexBuilder.append("(?<")
                    .append(type.name())
                    .append(">")
                    .append(type.getPattern())
                    .append(")|");
        }
        regexBuilder.deleteCharAt(regexBuilder.length() - 1);
        pattern = Pattern.compile(regexBuilder.toString());
    }

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            for(TokenType type : TokenType.values()) {
                String matchValue = matcher.group(type.name());

                if (matchValue != null) {
                    if (type == TokenType.UNKNOWN) {
                        throw new IllegalArgumentException(matchValue);
                    } else if (type != TokenType.WHITESPACE) {
                        tokens.add(new Token(type, matchValue));
                    }
                    break;
                }
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}
