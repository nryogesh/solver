package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.UnivariateEquationParser;
import com.example.equationsolver.math.UnivariateEquationParser.EquationForm;
import com.example.equationsolver.math.UnivariateEquationParser.Polynomial;
import com.example.equationsolver.model.Equation;
import com.example.equationsolver.model.LinearEquation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class LinearSolver implements EquationSolver {

    @Override
    public String getSolverType() {
        return "Linear Equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof LinearEquation;
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        try {
            EquationForm form = UnivariateEquationParser.parse(expression);
            Polynomial polynomial = form.numerator();
            if (form.isRational() || polynomial.degree() > 1) {
                return SolutionResponse.error("The equation is not a supported linear polynomial.");
            }

            double coefficient = polynomial.coefficient(1);
            double constant = polynomial.coefficient(0);
            List<String> steps = List.of(
                    "Step 1: Identify the equation type → Linear Equation (degree 1)",
                    "Step 2: Normalize both sides to left − right = 0 and expand parentheses.",
                    "Step 3: Collect terms: " + format(coefficient) + form.variable() + " + "
                            + format(constant) + " = 0");
            if (Math.abs(coefficient) < 1e-12) {
                if (Math.abs(constant) < 1e-12) {
                    return new SolutionResponse(expression, equation.getEquationType(), steps,
                            "Infinitely many solutions (identity)", true);
                }
                return new SolutionResponse(expression, equation.getEquationType(), steps,
                        "No solution (contradiction)", true);
            }

            double root = -constant / coefficient;
            if (!Double.isFinite(root)) return SolutionResponse.error("The solution is outside the supported numeric range.");
            Polynomial originalLeft = UnivariateEquationParser.parse(sides[0] + " = 0").numerator();
            Polynomial originalRight = UnivariateEquationParser.parse(sides[1] + " = 0").numerator();
            double leftValue = originalLeft.evaluate(root);
            double rightValue = originalRight.evaluate(root);
            if (Math.abs(leftValue - rightValue) > 1e-7 * Math.max(1, Math.max(Math.abs(leftValue), Math.abs(rightValue)))) {
                return SolutionResponse.error("The candidate solution failed verification in the original equation.");
            }
            List<String> verifiedSteps = new java.util.ArrayList<>(steps);
            verifiedSteps.add("Step 4: Verify in the original equation: left side = " + format(leftValue)
                    + ", right side = " + format(rightValue) + ".");
            return new SolutionResponse(expression, equation.getEquationType(), verifiedSteps,
                    form.variable() + " = " + format(root), true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Invalid linear equation: " + e.getMessage());
        }
    }

    private String format(double value) {
        double rounded = Math.rint(value * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
