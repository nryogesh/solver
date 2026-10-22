package com.example.equationsolver.model;

/**
 * Recognized algebraic forms that do not have a dedicated solver implementation.
 */
public class AlgebraicEquation extends Equation {

    private boolean solverAvailable;

    public AlgebraicEquation(String expression, String equationType, int degree, int variables,
                              String description) {
        super(expression, equationType, degree, variables,
                "Recognition only — solving is not implemented for this equation type");
        setDescription(description);
    }

    private String description;

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSolverAvailable(boolean solverAvailable) {
        this.solverAvailable = solverAvailable;
    }

    @Override
    public boolean isSolvable() {
        return solverAvailable;
    }
}
