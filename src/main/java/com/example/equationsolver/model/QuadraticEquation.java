package com.example.equationsolver.model;

/**
 * OOP: Inheritance — extends abstract Equation
 * Represents: ax² + bx + c = 0
 */
public class QuadraticEquation extends Equation {

    private double a; // coefficient of x²
    private double b; // coefficient of x
    private double c; // constant term
    private double discriminant;

    public QuadraticEquation() {
        super();
    }

    public QuadraticEquation(String expression) {
        super(expression, "Quadratic Equation", 2, 1,
              "Quadratic Formula: x = (-b ± √(b²-4ac)) / 2a");
        this.a = 0; this.b = 0; this.c = 0;
    }

    @Override
    public String getDescription() {
        return "A Quadratic Equation is a second-degree polynomial. " +
               "Form: ax² + bx + c = 0. Solved using the quadratic formula or factoring.";
    }

    @Override
    public boolean isSolvable() {
        return true;
    }

    public void computeDiscriminant() {
        this.discriminant = (b * b) - (4 * a * c);
    }

    public String discriminantNature() {
        if (discriminant > 0) return "Two distinct real roots";
        if (discriminant == 0) return "One repeated real root";
        return "Two complex (imaginary) roots";
    }

    public double getA() { return a; }
    public void setA(double a) { this.a = a; }

    public double getB() { return b; }
    public void setB(double b) { this.b = b; }

    public double getC() { return c; }
    public void setC(double c) { this.c = c; }

    public double getDiscriminant() { return discriminant; }

    @Override
    public String toString() {
        return "QuadraticEquation{expression='" + getExpression() + "', a=" + a + ", b=" + b + ", c=" + c + "}";
    }
}
