package com.example.equationsolver.interfaces;

import com.example.equationsolver.model.Equation;

/**
 * INTERFACE — OOP: Abstraction
 *
 * Classifies a raw expression string into the correct Equation subclass.
 * Implemented by RegexEquationClassifier.
 */
public interface EquationClassifier {

    /**
     * Analyze the expression and return a typed Equation object.
     * @param expression raw equation string (e.g., "2x + 5 = 10")
     * @return an appropriate Equation subclass instance
     */
    Equation classify(String expression);

    /**
     * Detect the equation type as a readable string label.
     * @param expression raw equation string
     * @return type label e.g. "Linear Equation"
     */
    String detectType(String expression);

    /**
     * Count the number of unique variables in the expression.
     */
    int countVariables(String expression);

    /**
     * Determine the degree (highest power) in the expression.
     */
    int detectDegree(String expression);
}
