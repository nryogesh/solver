package com.example.equationsolver.factory;

import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.model.*;
import com.example.equationsolver.solver.strategy.*;
import org.springframework.stereotype.Component;

/**
 * FACTORY PATTERN — OOP: Polymorphism + Design Pattern
 *
 * Runtime Selection: Dynamically returns the correct solver based on equation type.
 * Eliminates if-else chains and enables loose coupling.
 *
 * Example:
 *   EquationSolver solver = SolverFactory.getSolver(equation);
 *   solver.solve(equation);  // calls correct solver at runtime
 */
@Component
public class SolverFactory {

    /**
     * Returns the appropriate EquationSolver for the given equation.
     * POLYMORPHISM IN ACTION: The actual solver class is determined at runtime.
     *
     * @param equation the Equation object
     * @return the concrete EquationSolver implementation
     */
    public static EquationSolver getSolver(Equation equation) {
        if (equation instanceof AlgebraicEquation
                && "Radical Equation".equals(equation.getEquationType())
                && equation.isSolvable()) {
            return new RadicalEquationSolver();
        }
        if (equation instanceof AlgebraicEquation
                && "Absolute-Value Equation".equals(equation.getEquationType())
                && equation.isSolvable()) {
            return new AbsoluteValueEquationSolver();
        }
        if (equation instanceof AlgebraicEquation
                && "Diophantine Equation".equals(equation.getEquationType())
                && equation.isSolvable()) {
            return new DiophantineSolver();
        }
        if (equation instanceof LinearEquation) {
            return new LinearSolver();
        }
        if (equation instanceof QuadraticEquation) {
            return new QuadraticSolver();
        }
        if (equation instanceof PolynomialEquation) {
            return new PolynomialSolver();
        }
        if (equation instanceof AlgebraicEquation algebraic && algebraic.isSolvable()) {
            return new PolynomialSolver();
        }
        if (equation instanceof SimultaneousEquation) {
            return new SimultaneousEquationSolver();
        }
        throw new IllegalArgumentException(
            "No solver available for equation type: " + equation.getEquationType());
    }

    /**
     * Check if a solver exists for the equation type.
     */
    public static boolean hasSolver(Equation equation) {
        return equation instanceof LinearEquation ||
               equation instanceof QuadraticEquation ||
               equation instanceof PolynomialEquation ||
               equation instanceof AlgebraicEquation algebraic && algebraic.isSolvable() ||
               equation instanceof SimultaneousEquation;
    }
}
