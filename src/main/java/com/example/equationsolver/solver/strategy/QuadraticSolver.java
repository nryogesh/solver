package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.UnivariateEquationParser;
import com.example.equationsolver.math.UnivariateEquationParser.EquationForm;
import com.example.equationsolver.math.UnivariateEquationParser.Polynomial;
import com.example.equationsolver.model.Equation;
import com.example.equationsolver.model.QuadraticEquation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class QuadraticSolver implements EquationSolver {

    @Override
    public String getSolverType() {
        return "Quadratic Equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof QuadraticEquation;
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        try {
            EquationForm form = UnivariateEquationParser.parse(expression);
            Polynomial polynomial = form.numerator();
            if (form.isRational() || polynomial.degree() != 2) {
                return SolutionResponse.error("The parsed equation is not a quadratic polynomial.");
            }
            double a = polynomial.coefficient(2);
            double b = polynomial.coefficient(1);
            double c = polynomial.coefficient(0);
            if (Math.abs(a) < 1e-14) return SolutionResponse.error("The quadratic coefficient must not be zero.");

            double discriminant = b * b - 4 * a * c;
            List<String> steps = new ArrayList<>(List.of(
                    "Step 1: Normalize both sides to a quadratic polynomial equal to zero.",
                    "Step 2: Coefficients: a = " + fmt(a) + ", b = " + fmt(b) + ", c = " + fmt(c),
                    "Step 3: Discriminant Δ = b² − 4ac = " + fmt(discriminant)));

            String answer;
            if (discriminant < 0) {
                double real = -b / (2 * a);
                double imaginary = Math.sqrt(-discriminant) / (2 * Math.abs(a));
                answer = form.variable() + " = " + fmt(real) + " ± " + fmt(imaginary) + "i";
                steps.add("Step 4: Δ < 0, so the roots are complex.");
            } else {
                double sqrt = Math.sqrt(discriminant);
                double q = -0.5 * (b + Math.copySign(sqrt, b));
                double first = q / a;
                double second = Math.abs(q) < 1e-14 ? -b / (2 * a) : c / q;
                if (discriminant == 0) {
                    answer = form.variable() + " = " + fmt(first) + " (repeated root)";
                } else {
                    answer = form.variable() + "₁ = " + fmt(first) + ", " + form.variable() + "₂ = " + fmt(second);
                }
                steps.add("Step 4: Apply the quadratic formula using a numerically stable root computation.");
                steps.add("Step 5: Verify candidates in the normalized polynomial; residuals are "
                        + fmt(polynomial.evaluate(first)) + " and " + fmt(polynomial.evaluate(second)) + ".");
            }
            return new SolutionResponse(expression, equation.getEquationType(), steps, answer, true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Invalid quadratic equation: " + e.getMessage());
        }
    }

    private String fmt(double number) {
        double rounded = Math.rint(number * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
