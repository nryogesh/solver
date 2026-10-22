package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.model.AlgebraicEquation;
import com.example.equationsolver.model.Equation;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DiophantineSolver implements EquationSolver {

    private static final Pattern TERM =
            Pattern.compile("([+-]?)(?:(\\d+)(?:\\*?)?([a-z])|([a-z])|(\\d+))");

    @Override
    public String getSolverType() {
        return "Linear Diophantine Equation";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof AlgebraicEquation
                && "Diophantine Equation".equals(equation.getEquationType())
                && equation.isSolvable();
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().replaceAll(
                "(?i)\\b(find|solve)\\s+(for\\s+)?(integer|integers)\\s+(solutions?\\s+)?(to\\s+)?", "")
                .replaceAll("(?i)\\b(integer|integers)\\s+solutions?\\s+(to\\s+)?", "").trim();
        try {
            String[] sides = expression.split("=", -1);
            if (sides.length != 2) return SolutionResponse.error("Enter one linear Diophantine equation.");
            LinearForm left = parseSide(sides[0]);
            LinearForm right = parseSide(sides[1]);
            Map<String, BigInteger> coefficients = new LinkedHashMap<>(left.coefficients);
            right.coefficients.forEach((variable, value) ->
                    coefficients.merge(variable, value.negate(), BigInteger::add));
            coefficients.values().removeIf(value -> value.signum() == 0);
            BigInteger target = right.constant.subtract(left.constant);
            if (coefficients.isEmpty()) {
                if (target.signum() == 0) {
                    return new SolutionResponse(equation.getExpression(), equation.getEquationType(),
                            List.of("The equation is an identity over the integers."),
                            "Every integer assignment is a solution.", true);
                }
                return new SolutionResponse(equation.getExpression(), equation.getEquationType(),
                        List.of("The equation reduces to a false constant equality."),
                        "No integer solutions.", true);
            }
            if (coefficients.size() == 1) return solveOneVariable(equation, coefficients, target);
            if (coefficients.size() == 2) return solveTwoVariables(equation, coefficients, target);
            return SolutionResponse.unsupported(equation.getExpression(),
                    "Diophantine equation (only linear equations in at most two variables are supported)");
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Unsupported Diophantine form: " + e.getMessage());
        }
    }

    private SolutionResponse solveOneVariable(Equation equation, Map<String, BigInteger> coefficients,
                                               BigInteger target) {
        Map.Entry<String, BigInteger> term = coefficients.entrySet().iterator().next();
        BigInteger[] division = target.divideAndRemainder(term.getValue());
        if (division[1].signum() != 0) {
            return new SolutionResponse(equation.getExpression(), equation.getEquationType(),
                    List.of("The coefficient must divide the constant for an integer solution."),
                    "No integer solutions.", true);
        }
        return new SolutionResponse(equation.getExpression(), equation.getEquationType(),
                List.of("Solve the linear integer equation using divisibility."),
                term.getKey() + " = " + division[0], true);
    }

    private SolutionResponse solveTwoVariables(Equation equation, Map<String, BigInteger> coefficients,
                                                BigInteger target) {
        List<Map.Entry<String, BigInteger>> terms = new ArrayList<>(coefficients.entrySet());
        BigInteger a = terms.get(0).getValue();
        BigInteger b = terms.get(1).getValue();
        BigInteger gcd = a.abs().gcd(b.abs());
        if (!target.mod(gcd).equals(BigInteger.ZERO)) {
            return new SolutionResponse(equation.getExpression(), equation.getEquationType(),
                    List.of("By Bézout's identity, gcd(" + a + ", " + b + ") must divide " + target + "."),
                    "No integer solutions.", true);
        }
        BigInteger[] bezout = extendedGcd(a, b);
        BigInteger scale = target.divide(gcd);
        BigInteger x0 = bezout[0].multiply(scale);
        BigInteger y0 = bezout[1].multiply(scale);
        BigInteger xStep = b.divide(gcd);
        BigInteger yStep = a.divide(gcd).negate();
        String firstVariable = terms.get(0).getKey();
        String secondVariable = terms.get(1).getKey();
        List<String> steps = List.of(
                "Use the extended Euclidean algorithm; gcd(" + a + ", " + b + ") = " + gcd + ".",
                "All integer solutions are parameterized by t ∈ ℤ.");
        String answer = firstVariable + " = " + x0 + " + (" + xStep + ")t, "
                + secondVariable + " = " + y0 + " + (" + yStep + ")t, t ∈ ℤ";
        return new SolutionResponse(equation.getExpression(), equation.getEquationType(), steps, answer, true);
    }

    private BigInteger[] extendedGcd(BigInteger a, BigInteger b) {
        BigInteger oldR = a.abs();
        BigInteger r = b.abs();
        BigInteger oldS = BigInteger.ONE;
        BigInteger s = BigInteger.ZERO;
        BigInteger oldT = BigInteger.ZERO;
        BigInteger t = BigInteger.ONE;
        while (r.signum() != 0) {
            BigInteger quotient = oldR.divide(r);
            BigInteger next = oldR.subtract(quotient.multiply(r));
            oldR = r;
            r = next;
            next = oldS.subtract(quotient.multiply(s));
            oldS = s;
            s = next;
            next = oldT.subtract(quotient.multiply(t));
            oldT = t;
            t = next;
        }
        return new BigInteger[]{a.signum() < 0 ? oldS.negate() : oldS,
                b.signum() < 0 ? oldT.negate() : oldT, oldR};
    }

    private LinearForm parseSide(String input) {
        String compact = input.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (compact.isEmpty()) throw new IllegalArgumentException("Equation sides cannot be empty.");
        LinearForm result = new LinearForm();
        Matcher matcher = TERM.matcher(compact);
        int position = 0;
        while (position < compact.length()) {
            matcher.region(position, compact.length());
            if (!matcher.lookingAt()) throw new IllegalArgumentException("Use a linear sum of integer constants and variables.");
            int sign = "-".equals(matcher.group(1)) ? -1 : 1;
            String coefficient = matcher.group(2);
            String variable = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            String constant = matcher.group(5);
            if (variable != null) {
                BigInteger value = coefficient == null ? BigInteger.ONE : new BigInteger(coefficient);
                if (sign < 0) value = value.negate();
                result.coefficients.merge(variable, value, BigInteger::add);
            } else {
                BigInteger value = new BigInteger(constant);
                result.constant = result.constant.add(sign < 0 ? value.negate() : value);
            }
            position = matcher.end();
        }
        return result;
    }

    private static final class LinearForm {
        private final Map<String, BigInteger> coefficients = new LinkedHashMap<>();
        private BigInteger constant = BigInteger.ZERO;
    }
}
