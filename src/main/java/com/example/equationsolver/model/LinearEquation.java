package com.example.equationsolver.model;

/**
 * OOP: Inheritance — extends abstract Equation
 * Represents a linear equation of the form: ax + b = c
 */
public class LinearEquation extends Equation {

    private double coefficientA; // coefficient of x
    private double coefficientB; // constant on left side
    private double constantC;    // right-hand side constant

    public LinearEquation() {
        super();
    }

    public LinearEquation(String expression) {
        super(expression, "Linear Equation", 1, 1,
              "Algebraic Isolation (ax + b = c → x = (c - b) / a)");
    }

    // OOP: Polymorphism — overrides abstract method
    @Override
    public String getDescription() {
        return "A Linear Equation is a first-degree polynomial equation with one variable. " +
               "It graphs as a straight line. Form: ax + b = c.";
    }

    @Override
    public boolean isSolvable() {
        return true;
    }

    public double getCoefficientA() { return coefficientA; }
    public void setCoefficientA(double coefficientA) { this.coefficientA = coefficientA; }

    public double getCoefficientB() { return coefficientB; }
    public void setCoefficientB(double coefficientB) { this.coefficientB = coefficientB; }

    public double getConstantC() { return constantC; }
    public void setConstantC(double constantC) { this.constantC = constantC; }

    @Override
    public String toString() {
        return "LinearEquation{expression='" + getExpression() + "', degree=" + getDegree() + "}";
    }
}
