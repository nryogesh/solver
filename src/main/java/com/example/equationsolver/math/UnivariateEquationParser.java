package com.example.equationsolver.math;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UnivariateEquationParser {

    private UnivariateEquationParser() {}

    public static EquationForm parse(String expression) {
        String[] sides = expression.split("=", -1);
        if (sides.length != 2 || sides[0].isBlank() || sides[1].isBlank()) {
            throw new IllegalArgumentException("Enter exactly one '=' with an expression on each side.");
        }
        Parser left = new Parser(sides[0]);
        Parser right = new Parser(sides[1]);
        RationalFunction normalized = left.parse().subtract(right.parse());
        Set<String> variables = new HashSet<>(left.variables);
        variables.addAll(right.variables);
        if (variables.size() > 1) {
            throw new IllegalArgumentException("This solver supports only one variable.");
        }
        return new EquationForm(normalized.numerator, normalized.denominator,
                variables.stream().findFirst().orElse("x"));
    }

    public record EquationForm(Polynomial numerator, Polynomial denominator, String variable) {
        public int degree() {
            return numerator.degree();
        }

        public boolean isRational() {
            return denominator.degree() > 0;
        }
    }

    public static final class Polynomial {
        private static final double ZERO_EPSILON = 1e-14;
        private final double[] coefficients;

        private Polynomial(double... coefficients) {
            int degree = coefficients.length - 1;
            while (degree > 0 && Math.abs(coefficients[degree]) < ZERO_EPSILON) degree--;
            this.coefficients = new double[degree + 1];
            System.arraycopy(coefficients, 0, this.coefficients, 0, degree + 1);
        }

        public int degree() {
            return coefficients.length - 1;
        }

        public double coefficient(int exponent) {
            return exponent < coefficients.length ? coefficients[exponent] : 0;
        }

        public double[] coefficients() {
            return coefficients.clone();
        }

        public double evaluate(double x) {
            double result = 0;
            for (int index = coefficients.length - 1; index >= 0; index--) {
                result = result * x + coefficients[index];
            }
            return result;
        }

        private Polynomial add(Polynomial other) {
            double[] result = new double[Math.max(coefficients.length, other.coefficients.length)];
            for (int index = 0; index < result.length; index++) {
                result[index] = coefficient(index) + other.coefficient(index);
            }
            return new Polynomial(result);
        }

        private Polynomial scale(double factor) {
            double[] result = coefficients.clone();
            for (int index = 0; index < result.length; index++) result[index] *= factor;
            return new Polynomial(result);
        }

        private Polynomial multiply(Polynomial other) {
            double[] result = new double[coefficients.length + other.coefficients.length - 1];
            for (int left = 0; left < coefficients.length; left++) {
                for (int right = 0; right < other.coefficients.length; right++) {
                    result[left + right] += coefficients[left] * other.coefficients[right];
                }
            }
            return new Polynomial(result);
        }

        private Polynomial divideByConstant(double divisor) {
            if (Math.abs(divisor) < ZERO_EPSILON) throw new IllegalArgumentException("Division by zero.");
            return scale(1 / divisor);
        }

        private static Polynomial constant(double value) {
            return new Polynomial(value);
        }

        private static Polynomial variable(String name) {
            return new Polynomial(0, 1);
        }
    }

    private record RationalFunction(Polynomial numerator, Polynomial denominator) {
        private RationalFunction add(RationalFunction other) {
            return new RationalFunction(numerator.multiply(other.denominator)
                    .add(other.numerator.multiply(denominator)),
                    denominator.multiply(other.denominator));
        }

        private RationalFunction subtract(RationalFunction other) {
            return new RationalFunction(numerator.multiply(other.denominator)
                    .add(other.numerator.multiply(denominator).scale(-1)),
                    denominator.multiply(other.denominator));
        }

        private RationalFunction multiply(RationalFunction other) {
            return new RationalFunction(numerator.multiply(other.numerator),
                    denominator.multiply(other.denominator));
        }

        private RationalFunction divide(RationalFunction other) {
            if (other.numerator.degree() == 0 && Math.abs(other.numerator.coefficient(0)) < 1e-14) {
                throw new IllegalArgumentException("Division by zero.");
            }
            return new RationalFunction(numerator.multiply(other.denominator),
                    denominator.multiply(other.numerator));
        }

        private RationalFunction power(int exponent) {
            if (exponent < 0) {
                if (numerator.degree() == 0 && Math.abs(numerator.coefficient(0)) < 1e-14) {
                    throw new IllegalArgumentException("Division by zero.");
                }
                return new RationalFunction(polynomialPower(denominator, -exponent),
                        polynomialPower(numerator, -exponent));
            }
            return new RationalFunction(polynomialPower(numerator, exponent),
                    polynomialPower(denominator, exponent));
        }

        private static Polynomial polynomialPower(Polynomial polynomial, int exponent) {
            Polynomial result = Polynomial.constant(1);
            for (int index = 0; index < exponent; index++) result = result.multiply(polynomial);
            return result;
        }
    }

    private static final class Parser {
        private final List<String> tokens;
        private final Set<String> variables = new HashSet<>();
        private int position;

        private Parser(String expression) {
            tokens = tokenize(expression);
        }

        private RationalFunction parse() {
            RationalFunction result = expression();
            if (position != tokens.size()) {
                throw new IllegalArgumentException("Unexpected token '" + tokens.get(position) + "'.");
            }
            return result;
        }

        private RationalFunction expression() {
            RationalFunction result = term();
            while (match("+") || match("-")) {
                String operator = previous();
                RationalFunction next = term();
                result = operator.equals("+") ? result.add(next) : result.subtract(next);
            }
            return result;
        }

        private RationalFunction term() {
            RationalFunction result = unary();
            while (position < tokens.size()) {
                if (match("*")) {
                    result = result.multiply(unary());
                } else if (match("/")) {
                    result = result.divide(unary());
                } else if (startsPrimary(peek())) {
                    result = result.multiply(unary());
                } else {
                    break;
                }
            }
            return result;
        }

        private RationalFunction unary() {
            if (match("+")) return unary();
            if (match("-")) {
                RationalFunction value = unary();
                return new RationalFunction(value.numerator.scale(-1), value.denominator);
            }
            RationalFunction value = primary();
            if (match("^")) {
                boolean negative = match("-");
                match("+");
                if (position >= tokens.size() || !tokens.get(position).matches("\\d+")) {
                    throw new IllegalArgumentException("Exponents must be integers.");
                }
                int exponent = Integer.parseInt(tokens.get(position++)) * (negative ? -1 : 1);
                if (Math.abs(exponent) > 100) throw new IllegalArgumentException("Exponent exceeds the supported limit of 100.");
                return value.power(exponent);
            }
            return value;
        }

        private RationalFunction primary() {
            if (match("(")) {
                RationalFunction value = expression();
                expect(")");
                return value;
            }
            if (position >= tokens.size()) throw new IllegalArgumentException("Expected a number, variable, or parenthesis.");
            String token = tokens.get(position++);
            if (token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
                return new RationalFunction(Polynomial.constant(Double.parseDouble(token)), Polynomial.constant(1));
            }
            if (token.matches("[A-Za-z]")) {
                variables.add(token.toLowerCase());
                return new RationalFunction(Polynomial.variable(token), Polynomial.constant(1));
            }
            throw new IllegalArgumentException("Unexpected token '" + token + "'.");
        }

        private boolean startsPrimary(String token) {
            return token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+|[A-Za-z])") || token.equals("(");
        }

        private void expect(String token) {
            if (!match(token)) throw new IllegalArgumentException("Expected '" + token + "'.");
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
                char current = expression.charAt(index);
                if (Character.isWhitespace(current)) {
                    index++;
                } else if (Character.isDigit(current) || current == '.') {
                    int end = index + 1;
                    while (end < expression.length()
                            && (Character.isDigit(expression.charAt(end)) || expression.charAt(end) == '.')) end++;
                    String number = expression.substring(index, end);
                    if (!number.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
                        throw new IllegalArgumentException("Invalid number '" + number + "'.");
                    }
                    tokens.add(number);
                    index = end;
                } else if (Character.isLetter(current)) {
                    tokens.add(String.valueOf(current));
                    index++;
                } else if ("+-*/^()".indexOf(current) >= 0) {
                    tokens.add(String.valueOf(current));
                    index++;
                } else {
                    throw new IllegalArgumentException("Unsupported character '" + current + "'.");
                }
            }
            if (tokens.isEmpty()) throw new IllegalArgumentException("Expression is empty.");
            return tokens;
        }
    }
}
