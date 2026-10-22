package com.example.equationsolver;

import com.example.equationsolver.classifier.RegexEquationClassifier;
import com.example.equationsolver.model.Equation;
import com.example.equationsolver.service.impl.EquationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquationClassificationCoverageTest {

    private RegexEquationClassifier classifier;
    private EquationServiceImpl service;

    @BeforeEach
    void setUp() {
        classifier = new RegexEquationClassifier();
        service = new EquationServiceImpl();
    }

    @Test
    void recognizesPolynomialDegreesOneThroughSix() {
        assertEquals("Linear Equation", classifier.classify("2x + 3 = 7").getEquationType());
        assertEquals("Quadratic Equation", classifier.classify("x^2 - 5x + 6 = 0").getEquationType());
        assertEquals("Cubic Equation", classifier.classify("x^3 - 6x^2 + 11x - 6 = 0").getEquationType());
        assertEquals("Quartic Equation", classifier.classify("x^4 + x + 1 = 0").getEquationType());
        assertEquals("Quintic Equation", classifier.classify("x^5 - 2x + 1 = 0").getEquationType());
        assertEquals("Higher-Degree Polynomial Equation", classifier.classify("x^6 - 1 = 0").getEquationType());
        assertEquals("Higher-Degree Polynomial Equation", classifier.classify("x^100 - 1 = 0").getEquationType());
    }

    @Test
    void distinguishesLinearSystemsAndNonlinearPolynomialSystems() {
        Equation linear = classifier.classify("x + y = 5\nx - y = 1");
        assertEquals("Simultaneous Linear Equations", linear.getEquationType());
        assertTrue(linear.isSystem());
        assertEquals(2, linear.getEquationCount());
        assertTrue(linear.isSolvable());

        Equation nonlinear = classifier.classify("x^2 + y = 4\nx + y^2 = 5");
        assertEquals("Nonlinear Algebraic System", nonlinear.getEquationType());
        assertTrue(nonlinear.getSecondaryProperties().contains("Simultaneous Polynomial Equations"));
        assertTrue(nonlinear.getSecondaryProperties().contains("Polynomial System of Equations"));
        assertFalse(nonlinear.isSolvable());
        assertEquals(2, nonlinear.getEquationCount());
    }

    @Test
    void recognizesRationalRadicalAndAbsoluteValueEquations() {
        assertEquals("Rational Equation", classifier.classify("1/x + 1 = 3").getEquationType());
        assertEquals("Radical Equation", classifier.classify("sqrt(x + 1) = 3").getEquationType());
        assertEquals("Absolute-Value Equation", classifier.classify("|x - 2| = 5").getEquationType());
    }

    @Test
    void prioritizesBiquadraticReciprocalAndFactoredForms() {
        Equation biquadratic = classifier.classify("x^4 - 5x^2 + 4 = 0");
        assertEquals("Biquadratic Equation", biquadratic.getEquationType());
        assertFalse(biquadratic.isSolvable());

        Equation reciprocal = classifier.classify("x^4 + 3x^2 + 1 = 0");
        assertEquals("Reciprocal Equation", reciprocal.getEquationType());

        Equation factored = classifier.classify("(x - 2)(x + 3) = 0");
        assertEquals("Quadratic Equation", factored.getEquationType());
        assertTrue(factored.getSecondaryProperties().contains("Factored Polynomial"));
    }

    @Test
    void identifiesHomogeneousAndSymmetricPolynomialsFromTheirTerms() {
        Equation homogeneous = classifier.classify("x^2 + 2xy + y^2 = 0");
        assertEquals("Homogeneous Polynomial Equation", homogeneous.getEquationType());
        assertTrue(homogeneous.getSecondaryProperties().contains("Homogeneous"));

        Equation symmetric = classifier.classify("x^2 + y^2 = 1");
        assertEquals("Symmetric Polynomial Equation", symmetric.getEquationType());
        assertTrue(symmetric.getSecondaryProperties().contains("Symmetric"));
    }

    @Test
    void aSingleMultivariableEquationIsNotReportedAsASystem() {
        Equation equation = classifier.classify("x + y = 5");
        assertFalse(equation.isSystem());
        assertEquals(1, equation.getEquationCount());
        assertEquals(2, equation.getVariables());
        assertTrue(equation.getSecondaryProperties().contains("Multiple Variables"));
    }

    @Test
    void classifiesDiophantineOnlyWhenIntegerIntentIsExplicit() {
        Equation explicit = classifier.classify("Find integer solutions to x^2 + y^2 = 25");
        assertEquals("Diophantine Equation", explicit.getEquationType());
        assertFalse(classifier.classify("x^2 + y^2 = 25").getEquationType().contains("Diophantine"));
    }

    @Test
    void malformedInputHasAnInvalidStatusInsteadOfAnInventedType() {
        Equation equation = classifier.classify("x^2 + = 3");
        assertEquals("Invalid Equation", equation.getEquationType());
        assertEquals("invalid", equation.getClassificationStatus());
        assertFalse(equation.isSolvable());
    }

    @Test
    void classificationResponseExposesStructureAndDoesNotClaimUnsupportedSolving() {
        var response = service.classify("x^5 - 2x + 1 = 0");
        assertTrue(response.isSuccess());
        assertEquals("Quintic Equation", response.getEquationType());
        assertEquals(5, response.getDegree());
        assertEquals(1, response.getEquationCount());
        assertFalse(response.isSystem());
        assertEquals("recognized", response.getClassificationStatus());
        assertFalse(response.isSolvable());
        assertTrue(response.getMessage().contains("solving is not implemented"));
    }
}
