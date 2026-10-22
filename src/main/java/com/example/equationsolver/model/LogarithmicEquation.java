package com.example.equationsolver.model;

/** OOP: Inheritance — extends abstract Equation. Recognized only. */
public class LogarithmicEquation extends Equation {

    private double base; // log base, default 10

    public LogarithmicEquation() { super(); }

    public LogarithmicEquation(String expression) {
        super(expression, "Logarithmic Equation", 1, 1,
              "Recognition Only — Solving not supported in current version");
        this.base = 10;
    }

    @Override
    public String getDescription() {
        return "A Logarithmic Equation contains log or ln terms. " +
               "Example: log(x) + 2 = 5. Requires log laws to solve.";
    }

    @Override
    public boolean isSolvable() { return false; }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }

    @Override
    public String toString() {
        return "LogarithmicEquation{expression='" + getExpression() + "', base=" + base + "}";
    }
}
