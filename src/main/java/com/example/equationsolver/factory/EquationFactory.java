package com.example.equationsolver.factory;

import com.example.equationsolver.model.*;

/**
 * FACTORY PATTERN — OOP: Abstraction + Encapsulation
 *
 * Creates appropriate Equation objects based on type.
 * Centralizes object creation logic in one place.
 */
public class EquationFactory {

    /**
     * Creates an Equation object based on the given type string.
     *
     * @param type       the equation type (e.g., "Linear Equation")
     * @param expression the equation expression string
     * @return an appropriate Equation subclass instance
     */
    public static Equation createEquation(String type, String expression) {
        switch (type) {
            case "Linear Equation":
                return new LinearEquation(expression);
            case "Quadratic Equation":
                return new QuadraticEquation(expression);
            case "Polynomial Equation":
                int degree = detectDegree(expression);
                return new PolynomialEquation(expression, degree);
            case "Simultaneous Linear Equations":
                int vars = countVariables(expression);
                return new SimultaneousEquation(expression, vars);
            case "Trigonometric Equation":
                return new TrigonometricEquation(expression);
            case "Logarithmic Equation":
                return new LogarithmicEquation(expression);
            case "Exponential Equation":
                return new ExponentialEquation(expression);
            case "Differential Equation":
                return new DifferentialEquation(expression);
            case "Integral Expression":
                return new IntegralEquation(expression);
            default:
                return new LinearEquation(expression);
        }
    }

    private static int detectDegree(String expression) {
        int maxDegree = 1;
        if (expression.contains("^3") || expression.contains("³")) maxDegree = 3;
        else if (expression.contains("^2") || expression.contains("²")) maxDegree = 2;
        return maxDegree;
    }

    private static int countVariables(String expression) {
        java.util.Set<Character> vars = new java.util.HashSet<>();
        for (char c : expression.toCharArray()) {
            if (Character.isLetter(c) && c != 'e') {
                vars.add(Character.toLowerCase(c));
            }
        }
        return Math.max(1, vars.size());
    }
}
