package com.example.equationsolver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * ABSTRACT BASE CLASS — OOP: Abstraction + Inheritance
 *
 * This is the root of the equation hierarchy.
 * All concrete equation types extend this class.
 * Common properties are defined here to avoid duplication (DRY principle).
 *
 * OOP Principles demonstrated:
 *  - Abstraction: abstract method getDescription() forces each subclass to define itself
 *  - Encapsulation: all fields are private with getters/setters
 *  - Inheritance: LinearEquation, QuadraticEquation, etc. extend this
 */
public abstract class Equation {

    // ─── Encapsulation: private fields ───────────────────────────────────────
    private String expression;
    private String equationType;
    private int degree;
    private int variables;
    private String solvingMethod;
    private List<String> variableNames = new ArrayList<>();
    private List<String> secondaryProperties = new ArrayList<>();
    private int equationCount = 1;
    private boolean system;
    private String classificationStatus = "recognized";

    // ─── Constructors ─────────────────────────────────────────────────────────
    protected Equation() {}

    protected Equation(String expression, String equationType, int degree,
                       int variables, String solvingMethod) {
        this.expression    = expression;
        this.equationType  = equationType;
        this.degree        = degree;
        this.variables     = variables;
        this.solvingMethod = solvingMethod;
    }

    // ─── Abstraction: subclasses MUST provide a human-readable description ────
    public abstract String getDescription();

    /**
     * Returns true if this equation type can be fully solved
     * by the current system. Subclasses may override.
     */
    public boolean isSolvable() {
        return false; // default: not solvable unless subclass says otherwise
    }

    // ─── Getters & Setters (Encapsulation) ───────────────────────────────────
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getEquationType() { return equationType; }
    public void setEquationType(String equationType) { this.equationType = equationType; }

    public int getDegree() { return degree; }
    public void setDegree(int degree) { this.degree = degree; }

    public int getVariables() { return variables; }
    public void setVariables(int variables) { this.variables = variables; }

    public String getSolvingMethod() { return solvingMethod; }
    public void setSolvingMethod(String solvingMethod) { this.solvingMethod = solvingMethod; }

    public List<String> getVariableNames() { return variableNames; }
    public void setVariableNames(List<String> variableNames) { this.variableNames = new ArrayList<>(variableNames); }

    public List<String> getSecondaryProperties() { return secondaryProperties; }
    public void setSecondaryProperties(List<String> secondaryProperties) {
        this.secondaryProperties = new ArrayList<>(secondaryProperties);
    }

    public int getEquationCount() { return equationCount; }
    public void setEquationCount(int equationCount) { this.equationCount = equationCount; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public String getClassificationStatus() { return classificationStatus; }
    public void setClassificationStatus(String classificationStatus) { this.classificationStatus = classificationStatus; }

    // ─── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Equation{" +
               "expression='" + expression + '\'' +
               ", equationType='" + equationType + '\'' +
               ", degree=" + degree +
               ", variables=" + variables +
               ", solvingMethod='" + solvingMethod + '\'' +
               '}';
    }
}
