package com.example.equationsolver.model;

/** OOP: Inheritance — extends abstract Equation. Recognized only. */
public class IntegralEquation extends Equation {

    private boolean isDefinite; // definite or indefinite integral

    public IntegralEquation() { super(); }

    public IntegralEquation(String expression) {
        super(expression, "Integral Expression", 1, 1,
              "Recognition Only — Solving not supported in current version");
        this.isDefinite = false;
    }

    @Override
    public String getDescription() {
        return "An Integral Expression involves ∫ (integration). " +
               "Example: ∫(2x)dx. Requires integral calculus to evaluate.";
    }

    @Override
    public boolean isSolvable() { return false; }

    public boolean isDefinite() { return isDefinite; }
    public void setDefinite(boolean definite) { isDefinite = definite; }

    @Override
    public String toString() {
        return "IntegralEquation{expression='" + getExpression() + "', isDefinite=" + isDefinite + "}";
    }
}
