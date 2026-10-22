package com.example.equationsolver.math;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AffineExpressionParser {

    private AffineExpressionParser() {}

    public static AffineForm parseEquation(String equation) {
        String[] sides = equation.split("=", -1);
        if (sides.length != 2 || sides[0].isBlank() || sides[1].isBlank()) {
            throw new IllegalArgumentException("Each equation must contain exactly one '=' and two non-empty sides.");
        }
        AffineForm left = new Parser(sides[0]).parse();
        AffineForm right = new Parser(sides[1]).parse();
        return left.add(right.scale(-1));
    }

    public record AffineForm(Map<String, Double> coefficients, double constant) {
        public AffineForm {
            coefficients = new LinkedHashMap<>(coefficients);
        }

        public AffineForm add(AffineForm other) {
            Map<String, Double> result = new LinkedHashMap<>(coefficients);
            other.coefficients.forEach((variable, coefficient) ->
                    result.merge(variable, coefficient, Double::sum));
            result.values().removeIf(value -> Math.abs(value) < 1e-12);
            return new AffineForm(result, constant + other.constant);
        }

        public AffineForm scale(double factor) {
            Map<String, Double> result = new LinkedHashMap<>();
            coefficients.forEach((variable, coefficient) -> result.put(variable, coefficient * factor));
            return new AffineForm(result, constant * factor);
        }

        public AffineForm multiply(AffineForm other) {
            boolean leftConstant = coefficients.isEmpty();
            boolean rightConstant = other.coefficients.isEmpty();
            if (!leftConstant && !rightConstant) {
                throw new IllegalArgumentException("Nonlinear products are not supported by the linear solver.");
            }
            if (leftConstant) return other.scale(constant);
            return scale(other.constant);
        }
    }

    private static final class Parser {
        private final List<String> tokens;
        private int position;

        private Parser(String expression) {
            tokens = tokenize(expression);
        }

        private AffineForm parse() {
            AffineForm result = expression();
            if (position != tokens.size()) {
                throw new IllegalArgumentException("Unexpected token '" + tokens.get(position) + "'.");
            }
            return result;
        }

        private AffineForm expression() {
            AffineForm result = term();
            while (match("+") || match("-")) {
                String operator = previous();
                AffineForm next = term();
                result = result.add(next.scale(operator.equals("+") ? 1 : -1));
            }
            return result;
        }

        private AffineForm term() {
            AffineForm result = unary();
            while (position < tokens.size()) {
                if (match("*")) {
                    result = result.multiply(unary());
                } else if (match("/")) {
                    AffineForm divisor = unary();
                    if (!divisor.coefficients().isEmpty()) {
                        throw new IllegalArgumentException("Division by a variable expression is not linear.");
                    }
                    if (Math.abs(divisor.constant()) < 1e-12) throw new IllegalArgumentException("Division by zero.");
                    result = result.scale(1 / divisor.constant());
                } else if (startsPrimary(peek())) {
                    result = result.multiply(unary());
                } else {
                    break;
                }
            }
            return result;
        }

        private AffineForm unary() {
            if (match("+")) return unary();
            if (match("-")) return unary().scale(-1);
            AffineForm value = primary();
            if (match("^")) {
                if (position >= tokens.size() || !tokens.get(position).matches("\\d")) {
                    throw new IllegalArgumentException("Only powers zero and one are supported in a linear equation.");
                }
                int exponent = Integer.parseInt(tokens.get(position++));
                if (exponent == 0) return constant(1);
                if (exponent != 1) throw new IllegalArgumentException("Nonlinear powers are not supported by the linear solver.");
            }
            return value;
        }

        private AffineForm primary() {
            if (match("(")) {
                AffineForm result = expression();
                if (!match(")")) throw new IllegalArgumentException("Expected ')'.");
                return result;
            }
            if (position >= tokens.size()) throw new IllegalArgumentException("Expected a number, variable, or parenthesis.");
            String token = tokens.get(position++);
            if (token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
                return constant(Double.parseDouble(token));
            }
            if (token.matches("[A-Za-z]")) {
                return new AffineForm(Map.of(token.toLowerCase(), 1.0), 0);
            }
            throw new IllegalArgumentException("Unexpected token '" + token + "'.");
        }

        private boolean startsPrimary(String token) {
            return token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+|[A-Za-z])") || token.equals("(");
        }

        private boolean match(String token) {
            if (position < tokens.size() && tokens.get(position).equals(token)) {
                position++;
                return true;
            }
            return false;
        }

        private String previous() {
            return tokens.get(position - 1);
        }

        private String peek() {
            return tokens.get(position);
        }

        private static List<String> tokenize(String expression) {
            List<String> tokens = new ArrayList<>();
            int index = 0;
            while (index < expression.length()) {
                char character = expression.charAt(index);
                if (Character.isWhitespace(character)) {
                    index++;
                } else if (Character.isDigit(character) || character == '.') {
                    int end = index + 1;
                    while (end < expression.length() && (Character.isDigit(expression.charAt(end))
                            || expression.charAt(end) == '.')) end++;
                    String number = expression.substring(index, end);
                    if (!number.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
                        throw new IllegalArgumentException("Invalid number '" + number + "'.");
                    }
                    tokens.add(number);
                    index = end;
                } else if (Character.isLetter(character)) {
                    tokens.add(String.valueOf(character));
                    index++;
                } else if ("+-*/^()".indexOf(character) >= 0) {
                    tokens.add(String.valueOf(character));
                    index++;
                } else {
                    throw new IllegalArgumentException("Unsupported character '" + character + "'.");
                }
            }
            if (tokens.isEmpty()) throw new IllegalArgumentException("Expression is empty.");
            return tokens;
        }
    }

    private static AffineForm constant(double value) {
        return new AffineForm(Map.of(), value);
    }
}
