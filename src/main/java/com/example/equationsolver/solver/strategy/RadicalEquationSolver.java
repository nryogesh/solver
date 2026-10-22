package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.UnivariateEquationParser;
import com.example.equationsolver.math.UnivariateEquationParser.EquationForm;
import com.example.equationsolver.model.AlgebraicEquation;
import com.example.equationsolver.model.Equation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RadicalEquationSolver implements EquationSolver {

    @Override
    public String getSolverType() {
        return "Square-root equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof AlgebraicEquation
                && "Radical Equation".equals(equation.getEquationType())
                && equation.isSolvable();
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        try {
            String[] sides = expression.split("=", -1);
            if (sides.length != 2) return SolutionResponse.error("Enter one square-root equation with a single '='.");
            String radicand = singleSquareRootArgument(sides[0]);
            String otherSide = sides[1].trim();
            if (radicand == null) {
                radicand = singleSquareRootArgument(sides[1]);
                otherSide = sides[0].trim();
            }
            if (radicand == null) {
                return SolutionResponse.unsupported(expression, "radical equation (one square root on one side required)");
            }

            EquationForm radicandForm = UnivariateEquationParser.parse(radicand + " = 0");
            EquationForm otherForm = UnivariateEquationParser.parse(otherSide + " = 0");
            if (radicandForm.isRational() || otherForm.isRational()) {
                return SolutionResponse.unsupported(expression, "radical equation with rational subexpressions");
            }
            EquationForm squared = UnivariateEquationParser.parse(
                    "(" + otherSide + ")^2 = (" + radicand + ")");
            if (squared.isRational()) return SolutionResponse.unsupported(expression, "rational squared equation");

            List<Double> candidates = PolynomialSolver.findVerifiedRealRoots(squared.numerator());
            List<Double> verified = candidates.stream()
                    .filter(root -> radicandForm.numerator().evaluate(root) >= -1e-9)
                    .filter(root -> otherForm.numerator().evaluate(root) >= -1e-9)
                    .filter(root -> Math.abs(Math.sqrt(Math.max(0, radicandForm.numerator().evaluate(root)))
                            - otherForm.numerator().evaluate(root)) <= 1e-7)
                    .toList();
            String answer = verified.isEmpty() ? "No real solutions."
                    : String.join("; ", verified.stream()
                            .map(root -> radicandForm.variable() + " ≈ " + fmt(root)).toList());
            return new SolutionResponse(expression, equation.getEquationType(),
                    List.of("Square both sides to form a polynomial equation.",
                            "Check the principal-root domain and substitute each candidate in the original equation; extraneous roots are discarded.",
                            "Numerical root candidates are approximate and residual-checked."),
                    answer, true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Invalid radical equation: " + e.getMessage());
        }
    }

    public static boolean supports(String expression) {
        String[] sides = expression.split("=", -1);
        return sides.length == 2
                && (singleSquareRootArgument(sides[0]) != null || singleSquareRootArgument(sides[1]) != null);
    }

    private static String singleSquareRootArgument(String side) {
        String value = side.trim();
        if (!value.regionMatches(true, 0, "sqrt(", 0, 5) || !value.endsWith(")")) return null;
        int depth = 0;
        for (int index = 4; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '(') depth++;
            if (character == ')') {
                depth--;
                if (depth == 0 && index != value.length() - 1) return null;
                if (depth < 0) return null;
            }
        }
        return depth == 0 ? value.substring(5, value.length() - 1) : null;
    }

    private String fmt(double value) {
        double rounded = Math.rint(value * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
