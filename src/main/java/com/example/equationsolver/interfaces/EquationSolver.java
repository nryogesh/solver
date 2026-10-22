package com.example.equationsolver.interfaces;

import com.example.equationsolver.model.Equation;
import com.example.equationsolver.dto.SolutionResponse;

/**
 * INTERFACE — OOP: Abstraction + Strategy Pattern
 *
 * Every solver (Linear, Quadratic, Polynomial, Simultaneous) implements this.
 * The SolverFactory returns this interface type — enabling Runtime Polymorphism.
 *
 * Runtime Polymorphism example:
 *   EquationSolver solver = SolverFactory.getSolver(equation);
 *   solver.solve(equation);  // calls correct solver at runtime
 */
public interface EquationSolver {

    /**
     * Solves the given equation and returns a detailed SolutionResponse.
     * @param equation the Equation object to solve
     * @return SolutionResponse with steps, final answer, and original expression
     */
    SolutionResponse solve(Equation equation);

    /**
     * Returns the type of equation this solver handles.
     */
    String getSolverType();

    /**
     * Returns true if this solver can handle the given equation.
     */
    boolean canSolve(Equation equation);
}
