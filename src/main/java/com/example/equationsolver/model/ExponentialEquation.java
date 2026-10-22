package com.example.equationsolver.model;

/** OOP: Inheritance — extends abstract Equation. Recognized only. */
public class ExponentialEquation extends Equation {

    public ExponentialEquation() { super(); }

    public ExponentialEquation(String expression) {
        super(expression, "Exponential Equation", 1, 1,
              "Recognition Only — Solving not supported in current version");
    }

    @Override
    public String getDescription() {
        return "An Exponential Equation has the variable in the exponent. " +
               "Example: 2^x = 8. Solved using logarithms.";
    }

    @Override
    public boolean isSolvable() { return false; }

    @Override
    public String toString() {
        return "ExponentialEquation{expression='" + getExpression() + "'}";
    }
}
