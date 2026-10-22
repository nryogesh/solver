package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.UnivariateEquationParser;
import com.example.equationsolver.math.UnivariateEquationParser.EquationForm;
import com.example.equationsolver.math.UnivariateEquationParser.Polynomial;
import com.example.equationsolver.model.AlgebraicEquation;
import com.example.equationsolver.model.Equation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AbsoluteValueEquationSolver implements EquationSolver {

    @Override
    public String getSolverType() {
        return "Absolute-value equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof AlgebraicEquation
                && "Absolute-Value Equation".equals(equation.getEquationType())
                && equation.isSolvable();
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        try {
            String[] sides = expression.split("=", -1);
            if (sides.length != 2) return SolutionResponse.error("Enter one absolute-value equation with a single '='.");
            String absoluteSide;
            String otherSide;
            if (isSingleAbsolute(sides[0])) {
                absoluteSide = sides[0].trim();
                otherSide = sides[1].trim();
            } else if (isSingleAbsolute(sides[1])) {
                absoluteSide = sides[1].trim();
                otherSide = sides[0].trim();
            } else {
                return SolutionResponse.unsupported(expression, "absolute-value equation (one absolute expression on one side required)");
            }

            String inner = absoluteSide.substring(1, absoluteSide.length() - 1);
            EquationForm right = UnivariateEquationParser.parse(otherSide + " = 0");
            EquationForm innerForm = UnivariateEquationParser.parse(inner + " = 0");
            if (right.isRational() || innerForm.isRational()
                    || right.numerator().degree() > 1 || innerForm.numerator().degree() > 1) {
                return SolutionResponse.unsupported(expression, "absolute-value equation (linear expressions only)");
            }

            List<Double> candidates = new ArrayList<>();
            for (int sign : new int[]{1, -1}) {
                String branchRight = sign < 0 ? "-(" + otherSide + ")" : "(" + otherSide + ")";
                EquationForm branch = UnivariateEquationParser.parse("(" + inner + ") = " + branchRight);
                Polynomial polynomial = branch.numerator();
                if (!branch.isRational() && polynomial.degree() == 1) {
                    addUnique(candidates, -polynomial.coefficient(0) / polynomial.coefficient(1));
                }
            }

            List<Double> verified = candidates.stream()
                    .filter(Double::isFinite)
                    .filter(root -> Math.abs(Math.abs(innerForm.numerator().evaluate(root))
                            - right.numerator().evaluate(root)) < 1e-7)
                    .filter(root -> right.numerator().evaluate(root) >= -1e-9)
                    .sorted()
                    .toList();
            List<String> steps = List.of(
                    "Split |u| = v into the cases u = v and u = −v.",
                    "Solve each resulting linear equation and verify candidates in the original equation.");
            String answer = verified.isEmpty() ? "No real solutions."
                    : String.join("; ", verified.stream()
                            .map(root -> innerForm.variable() + " = " + fmt(root)).toList());
            return new SolutionResponse(expression, equation.getEquationType(), steps, answer, true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Invalid absolute-value equation: " + e.getMessage());
        }
    }

    public static boolean supports(String expression) {
        String[] sides = expression.split("=", -1);
        return sides.length == 2 && (isSingleAbsolute(sides[0]) || isSingleAbsolute(sides[1]));
    }

    private static boolean isSingleAbsolute(String side) {
        String trimmed = side.trim();
        return trimmed.length() >= 2 && trimmed.startsWith("|") && trimmed.endsWith("|")
                && trimmed.substring(1, trimmed.length() - 1).indexOf('|') < 0;
    }

    private void addUnique(List<Double> candidates, double candidate) {
        if (candidates.stream().noneMatch(root -> Math.abs(root - candidate) < 1e-7)) candidates.add(candidate);
    }

    private String fmt(double value) {
        double rounded = Math.rint(value * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
