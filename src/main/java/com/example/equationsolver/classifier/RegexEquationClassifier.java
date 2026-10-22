package com.example.equationsolver.classifier;

import com.example.equationsolver.interfaces.EquationClassifier;
import com.example.equationsolver.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexEquationClassifier implements EquationClassifier {

    private static final Pattern TRIG_PATTERN = Pattern.compile("\\b(sin|cos|tan|cot|sec|csc)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_PATTERN = Pattern.compile("\\b(log|ln)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPONENTIAL_PATTERN = Pattern.compile("(\\d+|e)\\s*\\^\\s*[a-zA-Z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIFFERENTIAL_PATTERN = Pattern.compile("d[²2]?[a-zA-Z]/d[a-zA-Z][²2]?|d/d[a-zA-Z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEGRAL_PATTERN = Pattern.compile("∫|\\bint\\s*\\(|\\bintegral\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(?<![a-zA-Z])([a-zA-Z]+)(?![a-zA-Z(])", Pattern.CASE_INSENSITIVE);

    @Override
    public Equation classify(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }
        String trimmed = expression.trim();

        Equation special = classifyNonAlgebraic(trimmed);
        if (special != null) return withMetadata(special, trimmed, List.of(), 1, false, "recognized");

        EquationStructureAnalyzer.Result result = EquationStructureAnalyzer.analyze(trimmed);
        if (!result.valid()) {
            AlgebraicEquation invalid = algebraic(trimmed, "Invalid Equation", 0, 0,
                    "The input is malformed or is not a supported equation.");
            invalid.setClassificationStatus("invalid");
            invalid.setSolvingMethod("Not available for invalid input");
            return withMetadata(invalid, trimmed, List.of(), 0, false, "invalid");
        }

        List<String> properties = new ArrayList<>();
        Equation equation;
        int degree = result.degree();
        int variables = result.variables().size();

        if (result.system()) {
            if (variables > 1) properties.add("Multiple Variables");
            if (result.polynomial()) {
                properties.add("Simultaneous Polynomial Equations");
                properties.add("Polynomial System of Equations");
            }
            if (result.linear()) {
                SimultaneousEquation system = new SimultaneousEquation(trimmed, variables);
                system.setEquationCount(result.equationCount());
                equation = system;
                properties.add("Linear System");
            } else {
                String type = result.polynomial() ? "Nonlinear Algebraic System" : "Algebraic System of Equations";
                equation = algebraic(trimmed, type, degree, variables,
                        result.polynomial()
                                ? "A system containing polynomial equations, including at least one nonlinear equation."
                                : "A system of algebraic equations requiring structural or symbolic solving.");
                if (result.rational()) properties.add("Rational System");
            }
            if (result.diophantine()) properties.add("Diophantine System");
            return withMetadata(equation, trimmed, properties, result.equationCount(), true, "recognized");
        }

        if (result.rational()) {
            equation = algebraic(trimmed, "Rational Equation", degree, variables,
                    "An equation containing a variable in a denominator.");
            ((AlgebraicEquation) equation).setSolverAvailable(variables == 1 && degree <= 20);
            properties.add("Rational");
            if (result.reciprocal()) properties.add("Reciprocal");
        } else if (result.radical()) {
            equation = algebraic(trimmed, "Radical Equation", degree, variables,
                    "An equation containing a variable under a root.");
            ((AlgebraicEquation) equation).setSolverAvailable(variables == 1
                    && com.example.equationsolver.solver.strategy.RadicalEquationSolver.supports(trimmed));
            properties.add("Radical");
        } else if (result.absolute()) {
            equation = algebraic(trimmed, "Absolute-Value Equation", degree, variables,
                    "An equation containing an absolute-value expression.");
            ((AlgebraicEquation) equation).setSolverAvailable(variables == 1
                    && com.example.equationsolver.solver.strategy.AbsoluteValueEquationSolver.supports(trimmed));
            properties.add("Absolute Value");
        } else if (result.diophantine()) {
            equation = algebraic(trimmed, "Diophantine Equation", degree, variables,
                    "An equation explicitly asking for integer solutions.");
            ((AlgebraicEquation) equation).setSolverAvailable(degree == 1 && variables <= 2);
            properties.add("Integer-solution intent");
        } else if (result.reciprocal()) {
            equation = algebraic(trimmed, "Reciprocal Equation", degree, variables,
                    "A reciprocal equation with symmetric powers or reciprocal variable terms.");
            ((AlgebraicEquation) equation).setSolverAvailable(variables == 1 && degree <= 20);
            properties.add("Reciprocal");
        } else if (isBiquadratic(trimmed, degree, variables)) {
            equation = algebraic(trimmed, "Biquadratic Equation", degree, variables,
                    "A quartic polynomial involving only even powers of one variable.");
            ((AlgebraicEquation) equation).setSolverAvailable(true);
            properties.add("Biquadratic");
        } else if (result.homogeneous()) {
            equation = algebraic(trimmed, "Homogeneous Polynomial Equation", degree, variables,
                    "A multivariable polynomial whose nonzero terms all have the same total degree.");
            properties.add("Homogeneous");
        } else if (result.symmetric()) {
            equation = algebraic(trimmed, "Symmetric Polynomial Equation", degree, variables,
                    "A polynomial unchanged when its two variables are exchanged.");
            properties.add("Symmetric");
        } else if (variables > 1) {
            equation = algebraic(trimmed, "Multivariable Algebraic Equation", degree, variables,
                    "A single algebraic equation containing multiple variables, not a system.");
            properties.add(degreeName(degree));
            if (result.polynomial()) properties.add("Polynomial");
        } else if (result.polynomial()) {
            if (degree == 1) {
                LinearEquation linear = new LinearEquation(trimmed);
                extractLinearCoefficients(trimmed, linear);
                equation = linear;
            } else if (degree == 2) {
                QuadraticEquation quadratic = new QuadraticEquation(trimmed);
                extractQuadraticCoefficients(trimmed, quadratic);
                equation = quadratic;
            } else if (degree == 3) {
                PolynomialEquation cubic = new PolynomialEquation(trimmed, degree);
                cubic.setEquationType("Cubic Equation");
                equation = cubic;
            } else {
                equation = algebraic(trimmed, degreeName(degree), degree, variables,
                        "A polynomial equation of degree " + degree + "; solving is not implemented for this degree.");
                ((AlgebraicEquation) equation).setSolverAvailable(degree <= 20);
            }
        } else {
            equation = algebraic(trimmed, "Algebraic Equation", degree, variables,
                    "A valid algebraic equation that does not match a supported polynomial form.");
        }

        if (result.factored()) properties.add("Factored Polynomial");
        if (variables > 1) properties.add("Multiple Variables");
        if (result.homogeneous()) properties.add("Homogeneous");
        if (result.symmetric()) properties.add("Symmetric");
        if (result.diophantine()) properties.add("Diophantine");
        if (result.reciprocal()) properties.add("Reciprocal");
        return withMetadata(equation, trimmed, properties, 1, false, "recognized");
    }

    private Equation classifyNonAlgebraic(String expression) {
        if (DIFFERENTIAL_PATTERN.matcher(expression).find()) return new DifferentialEquation(expression);
        if (INTEGRAL_PATTERN.matcher(expression).find()) return new IntegralEquation(expression);
        if (TRIG_PATTERN.matcher(expression).find()) return new TrigonometricEquation(expression);
        if (LOG_PATTERN.matcher(expression).find()) return new LogarithmicEquation(expression);
        if (EXPONENTIAL_PATTERN.matcher(expression).find()) return new ExponentialEquation(expression);
        return null;
    }

    private AlgebraicEquation algebraic(String expression, String type, int degree, int variables, String description) {
        return new AlgebraicEquation(expression, type, degree, variables, description);
    }

    private Equation withMetadata(Equation equation, String expression, List<String> properties,
                                  int equationCount, boolean system, String status) {
        EquationStructureAnalyzer.Result result = EquationStructureAnalyzer.analyze(expression);
        if (result.valid()) {
            equation.setVariableNames(result.variables());
            equation.setVariables(result.variables().size());
        }
        equation.setSecondaryProperties(properties);
        equation.setEquationCount(equationCount);
        equation.setSystem(system);
        equation.setClassificationStatus(status);
        return equation;
    }

    private boolean isBiquadratic(String expression, int degree, int variables) {
        if (degree != 4 || variables != 1) return false;
        Matcher matcher = Pattern.compile("[a-zA-Z]+\\s*(?:\\^\\s*(\\d+)|([²³⁴⁵]))?").matcher(expression);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String exponent = matcher.group(1);
            String superscript = matcher.group(2);
            int power = exponent != null ? Integer.parseInt(exponent)
                    : superscript == null ? 1 : "²³⁴⁵".indexOf(superscript) + 2;
            if (power % 2 != 0) return false;
        }
        return found;
    }

    private String degreeName(int degree) {
        return switch (degree) {
            case 0 -> "Constant Equation";
            case 1 -> "Linear Equation";
            case 2 -> "Quadratic Equation";
            case 3 -> "Cubic Equation";
            case 4 -> "Quartic Equation";
            case 5 -> "Quintic Equation";
            default -> "Higher-Degree Polynomial Equation";
        };
    }

    @Override
    public String detectType(String expression) {
        return classify(expression).getEquationType();
    }

    @Override
    public int countVariables(String expression) {
        Set<String> vars = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            String variable = matcher.group(1).toLowerCase();
            if (!Set.of("e", "pi", "sqrt", "abs", "sin", "cos", "tan", "cot", "sec", "csc", "log", "ln").contains(variable)) {
                vars.add(variable);
            }
        }
        return vars.size();
    }

    @Override
    public int detectDegree(String expression) {
        Equation equation = classify(expression);
        return equation.getDegree();
    }

    private void extractQuadraticCoefficients(String expression, QuadraticEquation quadratic) {
        try {
            String normalized = normalizeToLeftSide(expression);
            Matcher a = Pattern.compile("([+-]?\\d*\\.?\\d*)\\s*[xX]\\s*(?:\\^\\s*2|²)").matcher(normalized);
            Matcher b = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*[xX](?!\\s*(?:\\^|²))").matcher(normalized);
            if (a.find()) quadratic.setA(parseCoefficient(a.group(1)));
            if (b.find()) quadratic.setB(parseCoefficient(b.group(1)));
        } catch (RuntimeException ignored) {
            // Coefficients remain at their defaults when the expression is outside the legacy solver's format.
        }
    }

    private void extractLinearCoefficients(String expression, LinearEquation linear) {
        try {
            String[] sides = expression.split("=", -1);
            if (sides.length == 2) {
                linear.setConstantC(Double.parseDouble(sides[1].trim()));
                Matcher coefficient = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*[xX]").matcher(sides[0]);
                if (coefficient.find()) linear.setCoefficientA(parseCoefficient(coefficient.group(1)));
            }
        } catch (RuntimeException ignored) {
            // Coefficients remain at their defaults when the expression is outside the legacy solver's format.
        }
    }

    private double parseCoefficient(String value) {
        String coefficient = value.replaceAll("\\s", "");
        if (coefficient.isEmpty() || coefficient.equals("+")) return 1;
        if (coefficient.equals("-")) return -1;
        return Double.parseDouble(coefficient);
    }

    private String normalizeToLeftSide(String expression) {
        String[] sides = expression.split("=", -1);
        if (sides.length != 2) return expression;
        try {
            double right = Double.parseDouble(sides[1].trim());
            return sides[0].trim() + " - " + right;
        } catch (NumberFormatException e) {
            return sides[0].trim();
        }
    }
}
