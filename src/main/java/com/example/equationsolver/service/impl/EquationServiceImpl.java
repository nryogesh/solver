package com.example.equationsolver.service.impl;

import com.example.equationsolver.classifier.RegexEquationClassifier;
import com.example.equationsolver.dto.ClassificationResponse;
import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.factory.SolverFactory;
import com.example.equationsolver.interfaces.EquationService;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.model.*;
import org.springframework.stereotype.Service;

/**
 * SERVICE LAYER — OOP: Encapsulation + Service Pattern
 *
 * Business logic for equation classification and solving.
 * Controllers depend on EquationService interface, not this implementation.
 * Strict separation of concerns:
 *   Controller → Service Layer → Classifier/Solver/Factory
 */
@Service
public class EquationServiceImpl implements EquationService {

    private final RegexEquationClassifier classifier;

    public EquationServiceImpl() {
        this.classifier = new RegexEquationClassifier();
    }

    /**
     * POLYMORPHISM in action:
     * Calls classify() on the interface; the concrete RegexEquationClassifier executes.
     */
    @Override
    public ClassificationResponse classify(String expression) {
        try {
            if (expression == null || expression.trim().isEmpty()) {
                return ClassificationResponse.error("Expression cannot be empty");
            }

            String trimmed = expression.trim();
            Equation equation = classifier.classify(trimmed);

            ClassificationResponse response = new ClassificationResponse(
                trimmed,
                equation.getEquationType(),
                equation.getDegree(),
                equation.getVariables(),
                equation.getSolvingMethod(),
                equation.getDescription(),
                equation.isSolvable());
            response.setSubtype(equation.getSecondaryProperties().isEmpty()
                    ? null : equation.getSecondaryProperties().get(0));
            response.setSecondaryProperties(equation.getSecondaryProperties());
            response.setVariableNames(equation.getVariableNames());
            response.setEquationCount(equation.getEquationCount());
            response.setSystem(equation.isSystem());
            response.setClassificationStatus(equation.getClassificationStatus());
            if (!equation.isSolvable() && equation.getClassificationStatus().equals("recognized")) {
                response.setMessage("Equation recognized; solving is not implemented for this type.");
            }
            return response;

        } catch (IllegalArgumentException e) {
            return ClassificationResponse.error("Parsing error: " + e.getMessage());
        } catch (Exception e) {
            return ClassificationResponse.error("Classification error: " + e.getMessage());
        }
    }

    /**
     * Solves the given equation with step-by-step explanation.
     * Uses Factory Pattern for runtime polymorphism.
     */
    @Override
    public SolutionResponse solve(String expression) {
        try {
            // Step 1: Classify
            Equation equation = classifier.classify(expression);

            // Step 2: Check if solvable
            if ("invalid".equals(equation.getClassificationStatus())) {
                return SolutionResponse.error("Invalid equation: " + equation.getDescription());
            }
            if (!equation.isSolvable()) {
                if (equation.isSystem()) {
                    return SolutionResponse.error("Unsupported or non-linear term in system; only linear systems are currently solved.");
                }
                SolutionResponse unsupported = SolutionResponse.unsupported(expression, equation.getEquationType());
                if ("Multivariable Algebraic Equation".equals(equation.getEquationType())) {
                    unsupported.setMessage("One equation with multiple variables generally describes a solution set, not unique values. A requested variable or additional independent equations are needed.");
                } else if ("Diophantine Equation".equals(equation.getEquationType())) {
                    unsupported.setMessage("Only linear Diophantine equations in one or two variables are currently solved; this equation is outside that supported form.");
                } else {
                    unsupported.setMessage("Equation type recognized, but its form is outside the implemented solver's supported syntax.");
                }
                return unsupported;
            }

            // Step 3: Get solver via Factory Pattern
            EquationSolver solver = SolverFactory.getSolver(equation);

            // Step 4: Solve and return
            return solver.solve(equation);

        } catch (IllegalArgumentException e) {
            return SolutionResponse.error(e.getMessage());
        } catch (Exception e) {
            return SolutionResponse.error("Error solving equation: " + e.getMessage());
        }
    }

    public RegexEquationClassifier getClassifier() {
        return classifier;
    }
}
