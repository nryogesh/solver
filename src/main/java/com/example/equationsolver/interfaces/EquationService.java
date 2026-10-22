package com.example.equationsolver.interfaces;

import com.example.equationsolver.dto.ClassificationResponse;
import com.example.equationsolver.dto.SolutionResponse;

/**
 * INTERFACE — OOP: Abstraction + Service Layer Pattern
 *
 * Defines the contract for the equation service layer.
 * The controller depends on this interface, not the concrete implementation.
 * This ensures loose coupling (Dependency Inversion Principle).
 */
public interface EquationService {

    /**
     * Classify the given expression into an equation type.
     */
    ClassificationResponse classify(String expression);

    /**
     * Solve the given expression. Returns steps and final answer.
     */
    SolutionResponse solve(String expression);
}
