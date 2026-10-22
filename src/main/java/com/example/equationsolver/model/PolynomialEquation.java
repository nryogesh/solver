package com.example.equationsolver.model;

import java.util.List;
import java.util.ArrayList;

/**
 * OOP: Inheritance — extends abstract Equation
 * Represents a general polynomial: aₙxⁿ + ... + a₁x + a₀ = 0
 */
public class PolynomialEquation extends Equation {

    private List<Double> coefficients; // index 0 = constant term, index n = leading coefficient

    public PolynomialEquation() {
        super();
        this.coefficients = new ArrayList<>();
    }

    public PolynomialEquation(String expression, int degree) {
        super(expression, "Polynomial Equation", degree, 1,
              "Numerical methods (Newton-Raphson) or factoring");
        this.coefficients = new ArrayList<>();
    }

    @Override
    public String getDescription() {
        return "A Polynomial Equation has degree " + getDegree() +
               ". Form: aₙxⁿ + ... + a₁x + a₀ = 0. " +
               "Solved by factoring or numerical approximation.";
    }

    @Override
    public boolean isSolvable() {
        return getDegree() <= 20;
    }

    public List<Double> getCoefficients() { return coefficients; }
    public void setCoefficients(List<Double> coefficients) { this.coefficients = coefficients; }

    @Override
    public String toString() {
        return "PolynomialEquation{expression='" + getExpression() + "', degree=" + getDegree() + "}";
    }
}
