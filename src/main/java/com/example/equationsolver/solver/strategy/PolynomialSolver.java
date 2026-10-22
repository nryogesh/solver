package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.UnivariateEquationParser;
import com.example.equationsolver.math.UnivariateEquationParser.EquationForm;
import com.example.equationsolver.math.UnivariateEquationParser.Polynomial;
import com.example.equationsolver.model.AlgebraicEquation;
import com.example.equationsolver.model.Equation;
import com.example.equationsolver.model.PolynomialEquation;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class PolynomialSolver implements EquationSolver {

    private static final int MAX_DEGREE = 20;
    private static final int MAX_ROOT_EVALUATIONS = 2_000;
    private static final double ROOT_TOLERANCE = 1e-7;
    private static final double DENOMINATOR_TOLERANCE = 1e-9;

    static List<Double> findVerifiedRealRoots(Polynomial polynomial) {
        if (polynomial.degree() == 0 || polynomial.degree() > MAX_DEGREE) {
            throw new IllegalArgumentException("Real root search supports polynomial degrees 1 through " + MAX_DEGREE + ".");
        }
        Complex[] roots = new LaguerreSolver().solveAllComplex(polynomial.coefficients(), 0.37, MAX_ROOT_EVALUATIONS);
        List<Double> realRoots = new ArrayList<>();
        for (Complex root : roots) {
            if (!isVerifiedRoot(polynomial, root)) {
                throw new IllegalArgumentException("A numerical root did not pass the residual check.");
            }
            if (Math.abs(root.getImaginary()) <= ROOT_TOLERANCE * (1 + Math.abs(root.getReal()))) {
                double value = root.getReal();
                if (realRoots.stream().noneMatch(existing -> Math.abs(existing - value) <= ROOT_TOLERANCE)) {
                    realRoots.add(value);
                }
            }
        }
        realRoots.sort(Double::compareTo);
        return realRoots;
    }

    @Override
    public String getSolverType() {
        return "Polynomial / rational equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof PolynomialEquation
                || equation instanceof AlgebraicEquation algebraic && algebraic.isSolvable();
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        try {
            EquationForm form = UnivariateEquationParser.parse(expression);
            Polynomial numerator = form.numerator();
            int degree = numerator.degree();
            List<String> steps = new ArrayList<>();
            steps.add("Normalize the equation to a univariate polynomial numerator equal to zero.");
            if (form.isRational()) {
                steps.add("Preserve denominator restrictions and discard candidates where the original denominator is zero.");
            }

            if (degree == 0) {
                double constant = numerator.coefficient(0);
                if (Math.abs(constant) < ROOT_TOLERANCE) {
                    return new SolutionResponse(expression, equation.getEquationType(), steps,
                            "Infinitely many solutions, excluding any denominator zeros.", true);
                }
                return new SolutionResponse(expression, equation.getEquationType(), steps,
                        "No solution.", true);
            }
            if (degree > MAX_DEGREE) {
                return SolutionResponse.unsupported(expression,
                        equation.getEquationType() + " (numerical solver supports degree up to " + MAX_DEGREE + ")");
            }

            Complex[] roots = new LaguerreSolver().solveAllComplex(
                    numerator.coefficients(), 0.37, MAX_ROOT_EVALUATIONS);
            List<Complex> accepted = new ArrayList<>();
            List<Complex> excluded = new ArrayList<>();
            for (Complex root : roots) {
                if (!isVerifiedRoot(numerator, root)) {
                    return SolutionResponse.error("Numerical root did not pass the residual check; no result was reported.");
                }
                if (form.isRational() && isDenominatorZero(form.denominator(), root)) {
                    excluded.add(root);
                } else {
                    accepted.add(root);
                }
            }
            accepted.sort(Comparator.comparingDouble(Complex::getReal).thenComparingDouble(Complex::getImaginary));
            steps.add("Numerical method: Laguerre's method over the complex plane; roots are approximations.");
            steps.add("Residual tolerance: " + ROOT_TOLERANCE + "; polynomial degree: " + degree + ".");
            if (!excluded.isEmpty()) {
                steps.add("Excluded " + excluded.size() + " candidate root(s) that violate denominator restrictions.");
            }

            if (accepted.isEmpty()) {
                return new SolutionResponse(expression, equation.getEquationType(), steps, "No valid roots.", true);
            }
            List<String> rootLabels = new ArrayList<>();
            for (Complex root : accepted) {
                if (Math.abs(root.getImaginary()) <= ROOT_TOLERANCE * (1 + Math.abs(root.getReal()))) {
                    rootLabels.add(form.variable() + " ≈ " + fmt(root.getReal()));
                } else {
                    rootLabels.add(form.variable() + " ≈ " + fmt(root.getReal())
                            + (root.getImaginary() < 0 ? " - " : " + ")
                            + fmt(Math.abs(root.getImaginary())) + "i");
                }
            }
            steps.add("Verified normalized-polynomial residuals for all reported roots.");
            return new SolutionResponse(expression, equation.getEquationType(), steps,
                    String.join("; ", rootLabels), true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Could not parse equation: " + e.getMessage());
        } catch (RuntimeException e) {
            return SolutionResponse.error("Numerical solver could not establish a verified root set: " + e.getMessage());
        }
    }

    private static boolean isVerifiedRoot(Polynomial polynomial, Complex root) {
        Complex value = evaluate(polynomial, root);
        double scale = 1;
        double magnitude = root.abs();
        for (double coefficient : polynomial.coefficients()) {
            scale = scale * magnitude + Math.abs(coefficient);
            if (!Double.isFinite(scale)) return false;
        }
        return value.abs() <= ROOT_TOLERANCE * Math.max(1, scale);
    }

    private static boolean isDenominatorZero(Polynomial denominator, Complex root) {
        double scale = 1;
        double magnitude = root.abs();
        for (double coefficient : denominator.coefficients()) {
            scale = scale * magnitude + Math.abs(coefficient);
            if (!Double.isFinite(scale)) return false;
        }
        return evaluate(denominator, root).abs() <= DENOMINATOR_TOLERANCE * Math.max(1, scale);
    }

    private static Complex evaluate(Polynomial polynomial, Complex x) {
        double[] coefficients = polynomial.coefficients();
        Complex result = Complex.ZERO;
        for (int index = coefficients.length - 1; index >= 0; index--) {
            result = result.multiply(x).add(coefficients[index]);
        }
        return result;
    }

    private String fmt(double number) {
        double rounded = Math.rint(number * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
