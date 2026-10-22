package com.example.equationsolver.model;

import java.util.List;
import java.util.ArrayList;

/**
 * OOP: Inheritance — extends abstract Equation
 * Represents a system of linear equations.
 * e.g.:  x + y = 10
 *        x - y = 4
 */
public class SimultaneousEquation extends Equation {

    private List<String> equations; // each raw equation string
    private int equationCount;

    public SimultaneousEquation() {
        super();
        this.equations = new ArrayList<>();
    }

    public SimultaneousEquation(String expression, int variableCount) {
        super(expression, "Simultaneous Linear Equations", 1, variableCount,
              "Elimination Method / Substitution Method / Cramer's Rule");
        this.equations = new ArrayList<>();
        this.equationCount = variableCount;
    }

    @Override
    public String getDescription() {
        return "A system of " + equationCount + " linear equations with " +
               getVariables() + " unknowns. " +
               "Solved using Elimination, Substitution, or Cramer's Rule.";
    }

    @Override
    public boolean isSolvable() {
        return getVariables() > 0 && equationCount >= 2;
    }

    public List<String> getEquations() { return equations; }

    public void setEquations(List<String> equations) {
        this.equations = equations;
        this.equationCount = equations.size();
    }

    public void addEquation(String eq) { this.equations.add(eq); }

    public int getEquationCount() { return equationCount; }

    @Override
    public String toString() {
        return "SimultaneousEquation{equations=" + equations + ", variables=" + getVariables() + "}";
    }
}
