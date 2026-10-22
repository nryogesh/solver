package com.example.equationsolver.model;

/** OOP: Inheritance — extends abstract Equation. Recognized only. */
public class DifferentialEquation extends Equation {

    private String order; // "first-order", "second-order"

    public DifferentialEquation() { super(); }

    public DifferentialEquation(String expression) {
        super(expression, "Differential Equation", 1, 1,
              "Recognition Only — Solving not supported in current version");
        this.order = "first-order";
    }

    @Override
    public String getDescription() {
        return "A Differential Equation involves derivatives (dy/dx). " +
               "Example: dy/dx = 2x. Requires calculus techniques to solve.";
    }

    @Override
    public boolean isSolvable() { return false; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    @Override
    public String toString() {
        return "DifferentialEquation{expression='" + getExpression() + "', order='" + order + "'}";
    }
}
