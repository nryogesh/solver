package com.example.equationsolver.dto;

/**
 * DTO — Request body for classify and solve endpoints.
 */
public class EquationRequest {

    private String expression;

    public EquationRequest() {}

    public EquationRequest(String expression) {
        this.expression = expression;
    }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    @Override
    public String toString() {
        return "EquationRequest{expression='" + expression + "'}";
    }
}
