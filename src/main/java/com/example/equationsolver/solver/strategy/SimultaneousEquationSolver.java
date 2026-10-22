package com.example.equationsolver.solver.strategy;

import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationSolver;
import com.example.equationsolver.math.AffineExpressionParser;
import com.example.equationsolver.math.AffineExpressionParser.AffineForm;
import com.example.equationsolver.model.Equation;
import com.example.equationsolver.model.SimultaneousEquation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SimultaneousEquationSolver implements EquationSolver {

    private static final Pattern SEPARATOR = Pattern.compile("[\\r\\n;]+");
    private static final double EPSILON = 1e-10;

    @Override
    public String getSolverType() {
        return "Simultaneous Linear Equations";
    }

    @Override
    public boolean canSolve(Equation equation) {
        return equation instanceof SimultaneousEquation;
    }

    @Override
    public SolutionResponse solve(Equation equation) {
        String expression = equation.getExpression().trim();
        List<String> lines = SEPARATOR.splitAsStream(expression).map(String::trim)
                .filter(line -> !line.isEmpty()).toList();
        if (lines.size() < 2) {
            return SolutionResponse.error("Enter at least two linear equations separated by new lines or semicolons.");
        }
        try {
            List<AffineForm> forms = lines.stream().map(AffineExpressionParser::parseEquation).toList();
            Set<String> variableSet = new LinkedHashSet<>();
            forms.forEach(form -> variableSet.addAll(form.coefficients().keySet()));
            List<String> variables = new ArrayList<>(variableSet);
            if (variables.isEmpty() || variables.size() > 20 || forms.size() > 100) {
                return SolutionResponse.error("The linear system must contain 1–20 variables and no more than 100 equations.");
            }

            double[][] matrix = new double[forms.size()][variables.size() + 1];
            for (int row = 0; row < forms.size(); row++) {
                AffineForm form = forms.get(row);
                for (int column = 0; column < variables.size(); column++) {
                    matrix[row][column] = form.coefficients().getOrDefault(variables.get(column), 0.0);
                }
                matrix[row][variables.size()] = -form.constant();
            }

            int[] pivotRowForColumn = new int[variables.size()];
            java.util.Arrays.fill(pivotRowForColumn, -1);
            int pivotRow = 0;
            for (int column = 0; column < variables.size() && pivotRow < matrix.length; column++) {
                int best = pivotRow;
                for (int row = pivotRow + 1; row < matrix.length; row++) {
                    if (Math.abs(matrix[row][column]) > Math.abs(matrix[best][column])) best = row;
                }
                if (Math.abs(matrix[best][column]) < EPSILON) continue;
                double[] swap = matrix[pivotRow];
                matrix[pivotRow] = matrix[best];
                matrix[best] = swap;

                double divisor = matrix[pivotRow][column];
                for (int c = column; c <= variables.size(); c++) matrix[pivotRow][c] /= divisor;
                for (int row = 0; row < matrix.length; row++) {
                    if (row == pivotRow) continue;
                    double factor = matrix[row][column];
                    if (Math.abs(factor) < EPSILON) continue;
                    for (int c = column; c <= variables.size(); c++) {
                        matrix[row][c] -= factor * matrix[pivotRow][c];
                    }
                }
                pivotRowForColumn[column] = pivotRow++;
            }

            for (double[] row : matrix) {
                boolean allZero = true;
                for (int column = 0; column < variables.size(); column++) {
                    allZero &= Math.abs(row[column]) < EPSILON;
                }
                if (allZero && Math.abs(row[variables.size()]) >= EPSILON) {
                    return new SolutionResponse(expression, equation.getEquationType(),
                            List.of("Gaussian elimination produced a contradictory row."),
                            "No solution (inconsistent system).", true);
                }
            }

            List<Integer> freeColumns = new ArrayList<>();
            for (int column = 0; column < variables.size(); column++) {
                if (pivotRowForColumn[column] < 0) freeColumns.add(column);
            }
            List<String> steps = List.of(
                    "Normalize each equation and apply Gaussian elimination to the augmented matrix.",
                    "System size: " + forms.size() + " equation(s), " + variables.size() + " variable(s).");
            if (!freeColumns.isEmpty()) {
                String parameterized = describeFamily(variables, matrix, pivotRowForColumn, freeColumns);
                return new SolutionResponse(expression, equation.getEquationType(), steps,
                        "Infinitely many solutions (free parameters): " + parameterized, true);
            }

            List<String> assignments = new ArrayList<>();
            double[] solution = new double[variables.size()];
            for (int column = 0; column < variables.size(); column++) {
                solution[column] = matrix[pivotRowForColumn[column]][variables.size()];
                if (!Double.isFinite(solution[column])) return SolutionResponse.error("Solution exceeds the supported numeric range.");
                assignments.add(variables.get(column) + " = " + fmt(solution[column]));
            }
            for (AffineForm form : forms) {
                double residual = form.constant();
                for (int column = 0; column < variables.size(); column++) {
                    residual += form.coefficients().getOrDefault(variables.get(column), 0.0) * solution[column];
                }
                if (Math.abs(residual) > 1e-7) {
                    return SolutionResponse.error("The computed solution failed verification against the complete system.");
                }
            }
            return new SolutionResponse(expression, equation.getEquationType(), steps,
                    String.join(",  ", assignments), true);
        } catch (IllegalArgumentException e) {
            return SolutionResponse.error("Could not parse the linear system: " + e.getMessage());
        }
    }

    private String describeFamily(List<String> variables, double[][] matrix, int[] pivotRows,
                                  List<Integer> freeColumns) {
        List<String> parameters = new ArrayList<>();
        for (int free : freeColumns) parameters.add("t" + (parameters.size() + 1));
        List<String> assignments = new ArrayList<>();
        int parameterIndex;
        for (int column = 0; column < variables.size(); column++) {
            int row = pivotRows[column];
            if (row < 0) {
                assignments.add(variables.get(column) + " = t" + (freeColumns.indexOf(column) + 1));
                continue;
            }
            StringBuilder value = new StringBuilder(fmt(matrix[row][variables.size()]));
            parameterIndex = 0;
            for (int free : freeColumns) {
                double coefficient = -matrix[row][free];
                if (Math.abs(coefficient) > EPSILON) {
                    value.append(coefficient < 0 ? " - " : " + ")
                            .append(fmt(Math.abs(coefficient))).append(parameters.get(parameterIndex));
                }
                parameterIndex++;
            }
            assignments.add(variables.get(column) + " = " + value);
        }
        return String.join(", ", assignments) + ", parameters ∈ ℝ";
    }

    private String fmt(double value) {
        double rounded = Math.rint(value * 10000) / 10000;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-10) return String.valueOf((long) Math.rint(rounded));
        return String.format(Locale.ROOT, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
