package com.example.equationsolver.model;

/** OOP: Inheritance — extends abstract Equation. Recognized only. */
public class TrigonometricEquation extends Equation {

    private String trigFunction; // sin, cos, tan, etc.

    public TrigonometricEquation() { super(); }

    public TrigonometricEquation(String expression) {
        super(expression, "Trigonometric Equation", 1, 1,
              "Recognition Only — Solving not supported in current version");
    }

    @Override
    public String getDescription() {
        return "A Trigonometric Equation involves sin, cos, tan, or their inverses. " +
               "Example: sin(x) = 0.5. Requires trigonometric identities to solve.";
    }

    @Override
    public boolean isSolvable() { return false; }

    public String getTrigFunction() { return trigFunction; }
    public void setTrigFunction(String trigFunction) { this.trigFunction = trigFunction; }

    @Override
    public String toString() {
        return "TrigonometricEquation{expression='" + getExpression() + "'}";
    }
}
