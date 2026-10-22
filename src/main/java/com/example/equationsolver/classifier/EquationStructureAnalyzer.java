package com.example.equationsolver.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class EquationStructureAnalyzer {

    private static final int MAX_POLYNOMIAL_TERMS = 10_000;
    private static final Pattern INTEGER_INTENT =
            Pattern.compile("(?i)\\b(integer|integers|integer-valued|in\\s+z)\\b");

    private EquationStructureAnalyzer() {}

    static Result analyze(String input) {
        boolean diophantineIntent = INTEGER_INTENT.matcher(input).find();
        String expression = input.replaceAll("(?i)\\b(find|solve)\\s+(for\\s+)?(integer|integers)\\s+(solutions?\\s+)?(to\\s+)?", "")
                .replaceAll("(?i)\\b(integer|integers)\\s+solutions?\\s+(to\\s+)?", "").trim();
        List<String> equations = splitEquations(expression);
        if (equations.isEmpty()) return Result.invalid();

        List<Value> values = new ArrayList<>();
        for (String equation : equations) {
            int equals = equation.indexOf('=');
            if (equals < 0 || equals != equation.lastIndexOf('=')) return Result.invalid();
            String left = equation.substring(0, equals).trim();
            String right = equation.substring(equals + 1).trim();
            if (left.isEmpty() || right.isEmpty()) return Result.invalid();
            try {
                Value lhs = new Parser(left).parse();
                Value rhs = new Parser(right).parse();
                values.add(subtract(lhs, rhs));
            } catch (IllegalArgumentException e) {
                return Result.invalid();
            }
        }

        Set<String> variables = new HashSet<>();
        int degree = 0;
        boolean polynomial = true;
        boolean rational = false;
        boolean radical = false;
        boolean absolute = false;
        List<Map<Monomial, Double>> polynomials = new ArrayList<>();
        for (Value value : values) {
            variables.addAll(value.variables);
            rational |= value.rational;
            radical |= value.radical;
            absolute |= value.absolute;
            polynomial &= value.terms != null;
            polynomials.add(value.terms);
            if (value.terms != null) {
                for (Monomial monomial : value.terms.keySet()) degree = Math.max(degree, monomial.degree());
            }
        }
        List<String> sortedVariables = variables.stream().sorted().toList();
        boolean system = equations.size() > 1;
        boolean allLinear = polynomial && degree <= 1;
        boolean allPolynomial = polynomial;
        boolean homogeneous = !system && polynomial && isHomogeneous(polynomials.get(0)) && variables.size() > 1;
        boolean symmetric = !system && polynomial && isSymmetric(polynomials.get(0), sortedVariables);
        boolean factored = !system && Pattern.compile("\\)\\s*\\(").matcher(expression).find();
        boolean reciprocal = !system && isReciprocal(polynomials.isEmpty() ? null : polynomials.get(0), expression, sortedVariables);

        return new Result(true, sortedVariables, degree, equations.size(), system, allLinear,
                allPolynomial, rational, radical, absolute, homogeneous, symmetric,
                factored, reciprocal, diophantineIntent);
    }

    private static List<String> splitEquations(String input) {
        String[] parts = input.split("(?i)\\s*(?:\\r?\\n|;|\\band\\b)\\s*");
        List<String> equations = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) equations.add(part.trim());
        }
        return equations;
    }

    private static Value subtract(Value left, Value right) {
        Value negated = scale(right, -1);
        return combine(left, negated, 1);
    }

    private static Value combine(Value left, Value right, double rightScale) {
        Map<Monomial, Double> terms = null;
        if (left.terms != null && right.terms != null) {
            terms = new HashMap<>(left.terms);
            for (Map.Entry<Monomial, Double> term : right.terms.entrySet()) {
                terms.merge(term.getKey(), rightScale * term.getValue(), Double::sum);
            }
            terms.values().removeIf(value -> Math.abs(value) < 1e-12);
        }
        Set<String> variables = new HashSet<>(left.variables);
        variables.addAll(right.variables);
        return new Value(terms, variables, left.rational || right.rational,
                left.radical || right.radical, left.absolute || right.absolute);
    }

    private static Value scale(Value value, double scalar) {
        if (value.terms == null) {
            return new Value(null, value.variables, value.rational, value.radical, value.absolute);
        }
        Map<Monomial, Double> terms = new HashMap<>();
        value.terms.forEach((key, coefficient) -> terms.put(key, coefficient * scalar));
        return new Value(terms, value.variables, value.rational, value.radical, value.absolute);
    }

    private static Value multiply(Value left, Value right) {
        Map<Monomial, Double> terms = null;
        if (left.terms != null && right.terms != null) {
            terms = new HashMap<>();
            for (Map.Entry<Monomial, Double> leftTerm : left.terms.entrySet()) {
                for (Map.Entry<Monomial, Double> rightTerm : right.terms.entrySet()) {
                    terms.merge(leftTerm.getKey().multiply(rightTerm.getKey()),
                            leftTerm.getValue() * rightTerm.getValue(), Double::sum);
                    if (terms.size() > MAX_POLYNOMIAL_TERMS) {
                        throw new IllegalArgumentException("Polynomial expansion is too large");
                    }
                }
            }
            terms.values().removeIf(value -> Math.abs(value) < 1e-12);
        }
        Set<String> variables = new HashSet<>(left.variables);
        variables.addAll(right.variables);
        return new Value(terms, variables, left.rational || right.rational,
                left.radical || right.radical, left.absolute || right.absolute);
    }

    private static Value divide(Value numerator, Value denominator) {
        boolean variableDenominator = denominator.terms == null || denominator.variables.size() > 0;
        Set<String> variables = new HashSet<>(numerator.variables);
        variables.addAll(denominator.variables);
        if (variableDenominator) {
            return new Value(null, variables, true,
                    numerator.radical || denominator.radical, numerator.absolute || denominator.absolute);
        }
        double divisor = denominator.terms.getOrDefault(Monomial.CONSTANT, 0.0);
        if (Math.abs(divisor) < 1e-12) throw new IllegalArgumentException("Division by zero");
        Value quotient = scale(numerator, 1 / divisor);
        return new Value(quotient.terms, variables, numerator.rational || denominator.rational,
                numerator.radical || denominator.radical, numerator.absolute || denominator.absolute);
    }

    private static Value power(Value base, int exponent) {
        if (exponent < 0) {
            return new Value(null, base.variables, true, base.radical, base.absolute);
        }
        if (base.terms == null) return new Value(null, base.variables, base.rational, base.radical, base.absolute);
        Value result = constant(1);
        Value factor = base;
        int remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) == 1) result = multiply(result, factor);
            remaining >>>= 1;
            if (remaining > 0) factor = multiply(factor, factor);
        }
        return new Value(result.terms, base.variables, base.rational, base.radical, base.absolute);
    }

    private static Value constant(double value) {
        return new Value(new HashMap<>(Map.of(Monomial.CONSTANT, value)), Set.of(), false, false, false);
    }

    private static boolean isHomogeneous(Map<Monomial, Double> terms) {
        if (terms == null || terms.isEmpty()) return false;
        return terms.keySet().stream().map(Monomial::degree).distinct().count() == 1;
    }

    private static boolean isSymmetric(Map<Monomial, Double> terms, List<String> variables) {
        if (terms == null || variables.size() != 2) return false;
        Map<Monomial, Double> swapped = new HashMap<>();
        terms.forEach((monomial, coefficient) -> swapped.put(monomial.swap(variables.get(0), variables.get(1)), coefficient));
        return equalPolynomials(terms, swapped);
    }

    private static boolean isReciprocal(Map<Monomial, Double> terms, String expression, List<String> variables) {
        if (variables.size() == 1 && terms != null && !terms.isEmpty()) {
            int degree = terms.keySet().stream().mapToInt(monomial -> monomial.powers.getOrDefault(variables.get(0), 0)).max().orElse(0);
            if (degree >= 2) {
                for (int exponent = 0; exponent <= degree; exponent++) {
                    double low = coefficientAt(terms, variables.get(0), exponent);
                    double high = coefficientAt(terms, variables.get(0), degree - exponent);
                    if (Math.abs(low - high) > 1e-12) return false;
                }
                return true;
            }
        }
        return Pattern.compile("(?i)\\b([a-z])\\b\\s*\\+\\s*1\\s*/\\s*\\1\\b|1\\s*/\\s*([a-z])\\b\\s*\\+\\s*\\2\\b")
                .matcher(expression.replaceAll("\\s+", "")).find();
    }

    private static double coefficientAt(Map<Monomial, Double> terms, String variable, int exponent) {
        return terms.entrySet().stream()
                .filter(entry -> entry.getKey().powers.size() == (exponent == 0 ? 0 : 1)
                        && entry.getKey().powers.getOrDefault(variable, 0) == exponent)
                .mapToDouble(Map.Entry::getValue).sum();
    }

    private static boolean equalPolynomials(Map<Monomial, Double> left, Map<Monomial, Double> right) {
        Set<Monomial> keys = new HashSet<>(left.keySet());
        keys.addAll(right.keySet());
        return keys.stream().allMatch(key ->
                Math.abs(left.getOrDefault(key, 0.0) - right.getOrDefault(key, 0.0)) < 1e-12);
    }

    record Result(boolean valid, List<String> variables, int degree, int equationCount, boolean system,
                  boolean linear, boolean polynomial, boolean rational, boolean radical, boolean absolute,
                  boolean homogeneous, boolean symmetric, boolean factored, boolean reciprocal,
                  boolean diophantine) {
        private static Result invalid() {
            return new Result(false, List.of(), 0, 0, false, false, false,
                    false, false, false, false, false, false, false, false);
        }
    }

    private record Value(Map<Monomial, Double> terms, Set<String> variables,
                         boolean rational, boolean radical, boolean absolute) {}

    private record Monomial(Map<String, Integer> powers) {
        private static final Monomial CONSTANT = new Monomial(Map.of());

        private Monomial {
            powers = Collections.unmodifiableMap(new TreeMap<>(powers));
        }

        private int degree() {
            return powers.values().stream().mapToInt(Integer::intValue).sum();
        }

        private Monomial multiply(Monomial other) {
            Map<String, Integer> result = new TreeMap<>(powers);
            other.powers.forEach((variable, exponent) -> result.merge(variable, exponent, Integer::sum));
            return new Monomial(result);
        }

        private Monomial swap(String first, String second) {
            Map<String, Integer> result = new TreeMap<>();
            powers.forEach((variable, exponent) -> {
                String key = variable.equals(first) ? second : variable.equals(second) ? first : variable;
                result.put(key, exponent);
            });
            return new Monomial(result);
        }
    }

    private static final class Parser {
        private final List<String> tokens;
        private int position;

        private Parser(String expression) {
            this.tokens = tokenize(expression.replace("²", "^2").replace("³", "^3")
                    .replace("⁴", "^4").replace("⁵", "^5"));
        }

        private Value parse() {
            Value value = expression();
            if (position != tokens.size()) throw new IllegalArgumentException("Unexpected token");
            return value;
        }

        private Value expression() {
            Value value = term();
            while (match("+") || match("-")) {
                String operator = previous();
                value = combine(value, term(), operator.equals("+") ? 1 : -1);
            }
            return value;
        }

        private Value term() {
            Value value = unary();
            while (position < tokens.size()) {
                if (match("*")) {
                    value = multiply(value, unary());
                } else if (match("/")) {
                    value = divide(value, unary());
                } else if (startsPrimary(peek())) {
                    value = multiply(value, unary());
                } else {
                    break;
                }
            }
            return value;
        }

        private Value unary() {
            if (match("+")) return unary();
            if (match("-")) return scale(unary(), -1);
            Value value = primary();
            if (match("^")) {
                boolean negative = match("-");
                match("+");
                if (position >= tokens.size() || !tokens.get(position).matches("\\d+")) {
                    if (position < tokens.size()) position++;
                    return new Value(null, value.variables, value.rational || !value.variables.isEmpty(),
                            value.radical, value.absolute);
                }
                int exponent = Integer.parseInt(tokens.get(position++)) * (negative ? -1 : 1);
                return power(value, exponent);
            }
            return value;
        }

        private Value primary() {
            if (match("(")) {
                Value value = expression();
                expect(")");
                return value;
            }
            if (match("|")) {
                Value inner = expression();
                expect("|");
                return new Value(null, inner.variables, inner.rational, inner.radical, true);
            }
            if (match("√")) {
                Value inner = primary();
                return new Value(null, inner.variables, inner.rational, true, inner.absolute);
            }
            if (position >= tokens.size()) throw new IllegalArgumentException("Expected expression");
            String token = tokens.get(position++);
            if (token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
                return constant(Double.parseDouble(token));
            }
            if (token.matches("[A-Za-z]+")) {
                String name = token.toLowerCase();
                if (name.equals("pi") || name.equals("e")) return constant(1);
                if (Set.of("sqrt", "abs", "sin", "cos", "tan", "log", "ln").contains(name)) {
                    expect("(");
                    Value inner = expression();
                    expect(")");
                    return new Value(null, inner.variables, inner.rational,
                            inner.radical || name.equals("sqrt"),
                            inner.absolute || name.equals("abs"));
                }
                return new Value(new HashMap<>(Map.of(new Monomial(Map.of(name, 1)), 1.0)),
                        Set.of(name), false, false, false);
            }
            throw new IllegalArgumentException("Unexpected token");
        }

        private boolean startsPrimary(String token) {
            return token.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+|[A-Za-z]+)") ||
                    token.equals("(") || token.equals("√");
        }

        private void expect(String expected) {
            if (!match(expected)) throw new IllegalArgumentException("Expected " + expected);
        }

        private boolean match(String expected) {
            if (position < tokens.size() && tokens.get(position).equals(expected)) {
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
                    while (end < expression.length() &&
                            (Character.isDigit(expression.charAt(end)) || expression.charAt(end) == '.')) end++;
                    String number = expression.substring(index, end);
                    if (!number.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) throw new IllegalArgumentException("Invalid number");
                    tokens.add(number);
                    index = end;
                } else if (Character.isLetter(current)) {
                    int end = index + 1;
                    while (end < expression.length() && Character.isLetter(expression.charAt(end))) end++;
                    String identifier = expression.substring(index, end);
                    if (Set.of("pi", "e", "sqrt", "abs", "sin", "cos", "tan", "log", "ln").contains(identifier.toLowerCase())) {
                        tokens.add(identifier);
                    } else {
                        for (int letter = 0; letter < identifier.length(); letter++) {
                            tokens.add(identifier.substring(letter, letter + 1));
                        }
                    }
                    index = end;
                } else if ("+-*/^()|√".indexOf(current) >= 0) {
                    tokens.add(String.valueOf(current));
                    index++;
                } else {
                    throw new IllegalArgumentException("Unsupported character");
                }
            }
            return tokens;
        }
    }
}
