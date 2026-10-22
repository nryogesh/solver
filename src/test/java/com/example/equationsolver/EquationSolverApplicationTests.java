package com.example.equationsolver;

import com.example.equationsolver.classifier.RegexEquationClassifier;
import com.example.equationsolver.model.*;
import com.example.equationsolver.service.impl.EquationServiceImpl;
import com.example.equationsolver.dto.ClassificationResponse;
import com.example.equationsolver.dto.SolutionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for the Equation Solver
 * Tests OOP hierarchy, classification, and solving accuracy
 */
class EquationSolverApplicationTests {

    private RegexEquationClassifier classifier;
    private EquationServiceImpl service;

    @BeforeEach
    void setUp() {
        classifier = new RegexEquationClassifier();
        service = new EquationServiceImpl();
    }

    // ─── Classifier Tests ─────────────────────────────────────────

    @Test
    void testLinearClassification() {
        Equation eq = classifier.classify("2x + 5 = 15");
        assertInstanceOf(LinearEquation.class, eq);
        assertEquals("Linear Equation", eq.getEquationType());
        assertTrue(eq.isSolvable());
    }

    @Test
    void testQuadraticClassification() {
        Equation eq = classifier.classify("x^2 + 5x + 6 = 0");
        assertInstanceOf(QuadraticEquation.class, eq);
        assertEquals("Quadratic Equation", eq.getEquationType());
        assertTrue(eq.isSolvable());
    }

    @Test
    void testTrigClassification() {
        Equation eq = classifier.classify("sin(x) = 0.5");
        assertInstanceOf(TrigonometricEquation.class, eq);
        assertFalse(eq.isSolvable());
    }

    @Test
    void testLogClassification() {
        Equation eq = classifier.classify("log(x) + 2 = 5");
        assertInstanceOf(LogarithmicEquation.class, eq);
        assertFalse(eq.isSolvable());
    }

    @Test
    void testSimultaneousClassification() {
        Equation eq = classifier.classify("x + y = 10\nx - y = 4");
        assertInstanceOf(SimultaneousEquation.class, eq);
        assertTrue(eq.isSolvable());
    }

    // ─── Solver Tests ──────────────────────────────────────────────

    @Test
    void testLinearSolve() {
        SolutionResponse resp = service.solve("2x + 5 = 15");
        assertTrue(resp.isSolved());
        assertTrue(resp.getFinalAnswer().contains("5"));
    }

    @Test
    void testCompleteLinearEquationsWithParentheses() {
        assertLinearAnswer("5 + 30x = 4(3 + 4x)", "x = 0.5");
        assertLinearAnswer("2(x + 3) = 5x - 4", "x = 3.3333");
        assertLinearAnswer("3(x - 2) + 5 = 2(x + 1)", "x = 3");
        assertLinearAnswer("2x + 3 = 7", "x = 2");
    }

    @Test
    void completeLinearFormsAreClassifiedBeforeSolving() {
        assertInstanceOf(LinearEquation.class, classifier.classify("5 + 30x = 4(3 + 4x)"));
        assertInstanceOf(LinearEquation.class, classifier.classify("2(x + 3) = 5x - 4"));
        assertInstanceOf(LinearEquation.class, classifier.classify("3(x - 2) + 5 = 2(x + 1)"));
        assertInstanceOf(QuadraticEquation.class, classifier.classify("x^2 + 5x + 6 = 0"));
    }

    @Test
    void malformedLinearEquationReturnsClearError() {
        SolutionResponse response = service.solve("2(x + ) = 5");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid equation"));
        assertFalse(response.getMessage().contains("x ="));
    }

    private void assertLinearAnswer(String expression, String expected) {
        SolutionResponse response = service.solve(expression);
        assertTrue(response.isSolved(), response.getMessage());
        assertEquals(expected, response.getFinalAnswer());
    }

    @Test
    void testQuadraticSolve() {
        SolutionResponse resp = service.solve("x^2 - 5x + 6 = 0");
        assertTrue(resp.isSolved());
        assertNotNull(resp.getFinalAnswer());
    }

    @Test
    void polynomialSolversNormalizeBothEquationSidesAndReturnVerifiedRoots() {
        SolutionResponse quadratic = service.solve("x^2 = 5x - 6");
        assertTrue(quadratic.isSolved(), quadratic.getMessage());
        assertTrue(quadratic.getFinalAnswer().contains("2"));
        assertTrue(quadratic.getFinalAnswer().contains("3"));

        SolutionResponse cubic = service.solve("x^3 - 6x^2 + 11x - 6 = 0");
        assertTrue(cubic.isSolved(), cubic.getMessage());
        assertTrue(cubic.getFinalAnswer().contains("1"));
        assertTrue(cubic.getFinalAnswer().contains("2"));
        assertTrue(cubic.getFinalAnswer().contains("3"));

        SolutionResponse quartic = service.solve("x^4 - 5x^2 + 4 = 0");
        assertTrue(quartic.isSolved(), quartic.getMessage());
        assertTrue(quartic.getFinalAnswer().contains("-2"));
        assertTrue(quartic.getFinalAnswer().contains("-1"));
        assertTrue(quartic.getFinalAnswer().contains("1"));
        assertTrue(quartic.getFinalAnswer().contains("2"));
    }

    @Test
    void rationalSolverExcludesDenominatorRoots() {
        SolutionResponse response = service.solve("(x + 1)/(x - 2) = 3");

        assertTrue(response.isSolved(), response.getMessage());
        assertEquals("x ≈ 3.5", response.getFinalAnswer());
        assertTrue(response.getSteps().stream().anyMatch(step -> step.contains("denominator restrictions")));

        SolutionResponse excludedRoot = service.solve("(x - 2)/(x - 2) = 0");
        assertTrue(excludedRoot.isSolved(), excludedRoot.getMessage());
        assertEquals("No valid roots.", excludedRoot.getFinalAnswer());
    }

    @Test
    void solvesSupportedAbsoluteValueAndRadicalEquationsAndFiltersExtraneousRoots() {
        SolutionResponse absolute = service.solve("|2x - 3| = 7");
        assertTrue(absolute.isSolved(), absolute.getMessage());
        assertEquals("x = -2; x = 5", absolute.getFinalAnswer());

        SolutionResponse radical = service.solve("sqrt(x + 4) = x - 2");
        assertTrue(radical.isSolved(), radical.getMessage());
        assertEquals("x ≈ 5", radical.getFinalAnswer());
    }

    @Test
    void numericalPolynomialSolverHandlesQuinticAndHigherDegreeRoots() {
        SolutionResponse quintic = service.solve("x^5 - 2x + 1 = 0");
        assertTrue(quintic.isSolved(), quintic.getMessage());
        assertTrue(quintic.getFinalAnswer().contains("x ≈ 1"));

        SolutionResponse higher = service.solve("x^6 - 1 = 0");
        assertTrue(higher.isSolved(), higher.getMessage());
        assertTrue(higher.getFinalAnswer().contains("x ≈ 1"));
        assertTrue(higher.getFinalAnswer().contains("i"));
    }

    @Test
    void solvesSupportedLinearDiophantineEquationsExactly() {
        SolutionResponse twoVariable = service.solve("Find integer solutions to 2x + 3y = 7");
        assertTrue(twoVariable.isSolved(), twoVariable.getMessage());
        assertTrue(twoVariable.getFinalAnswer().contains("t ∈ ℤ"));

        SolutionResponse oneVariable = service.solve("Find integer solutions to 3x = 12");
        assertTrue(oneVariable.isSolved(), oneVariable.getMessage());
        assertEquals("x = 4", oneVariable.getFinalAnswer());

        SolutionResponse noIntegerSolution = service.solve("Find integer solutions to 2x = 3");
        assertTrue(noIntegerSolution.isSolved(), noIntegerSolution.getMessage());
        assertEquals("No integer solutions.", noIntegerSolution.getFinalAnswer());
    }

    @Test
    void testSimultaneousSolve() {
        SolutionResponse resp = service.solve("x + y = 10\nx - y = 4");
        assertTrue(resp.isSolved());
        assertEquals("x = 7,  y = 3", resp.getFinalAnswer());
    }

    @Test
    void testSimultaneousSolveUsesConsistentVariableOrder() {
        SolutionResponse resp = service.solve("y + x = 10\n2x - y = 5");

        assertTrue(resp.isSolved(), resp.getMessage());
        assertEquals("y = 5,  x = 5", resp.getFinalAnswer());
    }

    @Test
    void testSimultaneousSolveMovesTermsFromBothSides() {
        SolutionResponse resp = service.solve("2x + y + 3 = 13\nx - y = 1");

        assertTrue(resp.isSolved(), resp.getMessage());
        assertEquals("x = 3.6667,  y = 2.6667", resp.getFinalAnswer());
    }

    @Test
    void testSimultaneousDependentAndInconsistentSystems() {
        SolutionResponse dependent = service.solve("x + y = 2\n2x + 2y = 4");
        SolutionResponse inconsistent = service.solve("x + y = 2\n2x + 2y = 5");

        assertTrue(dependent.isSolved());
        assertTrue(dependent.getFinalAnswer().contains("Infinitely many"));
        assertTrue(inconsistent.isSolved());
        assertTrue(inconsistent.getFinalAnswer().contains("No solution"));
    }

    @Test
    void testSimultaneousRejectsExtraEquationsAndNonlinearTerms() {
        SolutionResponse extraEquation = service.solve("x + y = 2\nx - y = 0\nx = 1");
        SolutionResponse nonlinear = service.solve("x^2 + y = 2\nx - y = 0");

        assertTrue(extraEquation.isSolved(), extraEquation.getMessage());
        assertEquals("x = 1,  y = 1", extraEquation.getFinalAnswer());
        assertFalse(nonlinear.isSuccess());
        assertTrue(nonlinear.getMessage().contains("Unsupported or non-linear term"));
    }

    @Test
    void gaussianEliminationHandlesUnderdeterminedAndOverdeterminedLinearSystems() {
        SolutionResponse underdetermined = service.solve("x + y + z = 6\nx - y = 0");
        assertTrue(underdetermined.isSolved(), underdetermined.getMessage());
        assertTrue(underdetermined.getFinalAnswer().contains("Infinitely many solutions"));
        assertTrue(underdetermined.getFinalAnswer().contains("z = t1"));

        SolutionResponse inconsistent = service.solve("x + y = 2\nx - y = 0\nx = 3");
        assertTrue(inconsistent.isSolved(), inconsistent.getMessage());
        assertEquals("No solution (inconsistent system).", inconsistent.getFinalAnswer());
    }

    @Test
    void testUnsupportedReturnsGracefully() {
        SolutionResponse resp = service.solve("sin(x) = 0.5");
        assertTrue(resp.isSuccess());
        assertFalse(resp.isSolved());
        assertNotNull(resp.getMessage());
    }

    @Test
    void testEmptyExpressionHandled() {
        ClassificationResponse resp = service.classify("");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getMessage());
    }

    // ─── OOP Hierarchy Tests ───────────────────────────────────────

    @Test
    void testPolymorphism() {
        // All equations are of type Equation (polymorphism)
        Equation linear = new LinearEquation("x = 5");
        Equation quadratic = new QuadraticEquation("x^2 = 4");
        Equation trig = new TrigonometricEquation("sin(x) = 1");

        assertTrue(linear instanceof Equation);
        assertTrue(quadratic instanceof Equation);
        assertTrue(trig instanceof Equation);

        // But they behave differently (polymorphism)
        assertTrue(linear.isSolvable());
        assertTrue(quadratic.isSolvable());
        assertFalse(trig.isSolvable());
    }

    @Test
    void testDescriptions() {
        assertNotNull(new LinearEquation("x=1").getDescription());
        assertNotNull(new QuadraticEquation("x^2=1").getDescription());
        assertNotNull(new TrigonometricEquation("sin(x)=1").getDescription());
        assertNotNull(new DifferentialEquation("dy/dx=x").getDescription());
    }
}
