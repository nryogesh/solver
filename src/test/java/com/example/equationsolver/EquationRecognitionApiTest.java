package com.example.equationsolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class EquationRecognitionApiTest {

    private static final List<RecognitionCase> CASES = List.of(
            new RecognitionCase("1. Linear equation in one variable", "2x + 3 = 7",
                    "Linear Equation", null),
            new RecognitionCase("2. Quadratic equation", "x^2 - 5x + 6 = 0",
                    "Quadratic Equation", null),
            new RecognitionCase("3. Cubic equation", "x^3 - 6x^2 + 11x - 6 = 0",
                    "Cubic Equation", null),
            new RecognitionCase("4. Quartic equation (not biquadratic)", "x^4 + x + 1 = 0",
                    "Quartic Equation", null),
            new RecognitionCase("5. Quintic equation", "x^5 - 2x + 1 = 0",
                    "Quintic Equation", null),
            new RecognitionCase("6. Higher-degree polynomial equation", "x^6 - 1 = 0",
                    "Higher-Degree Polynomial Equation", null),
            new RecognitionCase("7. Simultaneous linear equations", "x + y = 5\nx - y = 1",
                    "Simultaneous Linear Equations", "Linear System"),
            new RecognitionCase("8. Simultaneous polynomial equations", "x^2 + y = 4\nx + y^2 = 5",
                    "Nonlinear Algebraic System", "Simultaneous Polynomial Equations"),
            new RecognitionCase("9. Rational equation", "1/x + 1 = 3",
                    "Rational Equation", null),
            new RecognitionCase("10. Radical equation", "sqrt(x + 1) = 3",
                    "Radical Equation", null),
            new RecognitionCase("11. Absolute-value equation", "|x - 2| = 5",
                    "Absolute-Value Equation", null),
            new RecognitionCase("12. Biquadratic equation (special quartic)", "x^4 - 5x^2 + 4 = 0",
                    "Biquadratic Equation", null),
            new RecognitionCase("13. Reciprocal equation (also polynomial)", "x^4 + 3x^2 + 1 = 0",
                    "Reciprocal Equation", null),
            new RecognitionCase("14. Factored polynomial equation (also quadratic)", "(x - 2)(x + 3) = 0",
                    "Quadratic Equation", "Factored Polynomial"),
            new RecognitionCase("15. Homogeneous polynomial equation", "x^2 + 2xy + y^2 = 0",
                    "Homogeneous Polynomial Equation", "Homogeneous"),
            new RecognitionCase("16. Symmetric polynomial equation (not homogeneous)", "x^2 + y^2 = 1",
                    "Symmetric Polynomial Equation", "Symmetric"),
            new RecognitionCase("17. Multivariable algebraic equation (not a system)", "x + 2y = 5",
                    "Multivariable Algebraic Equation", "Multiple Variables"),
            new RecognitionCase("18. Diophantine equation (explicit integer intent)",
                    "Find integer solutions to x^2 + y^2 = 25", "Diophantine Equation", null),
            new RecognitionCase("19. Polynomial system of equations", "x^2 + y^2 = 4\nx^2 - y = 1",
                    "Nonlinear Algebraic System", "Polynomial System of Equations"),
            new RecognitionCase("20. Nonlinear algebraic system", "x^2 + y = 4\nx + y^2 = 5",
                    "Nonlinear Algebraic System", null)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestFactory
    Stream<DynamicTest> classifiesAllTwentyEquationTypesThroughTheApi() {
        return CASES.stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.name() + ": " + testCase.equation(),
                () -> assertClassification(testCase)));
    }

    @org.junit.jupiter.api.Test
    void classificationPriorityAndMetadataDistinguishOverlappingForms() throws Exception {
        assertClassification(new RecognitionCase("Reciprocal rational overlap", "x + 1/x = 3",
                "Rational Equation", "Reciprocal"));
        assertClassification(new RecognitionCase("Homogeneous and symmetric overlap", "x^2 + 2xy + y^2 = 0",
                "Homogeneous Polynomial Equation", "Symmetric"));

        JsonNode multivariable = classify("x + 2y = 5");
        assertEquals("Multivariable Algebraic Equation", multivariable.path("equationType").asText(),
                "A single multi-variable equation must not be classified as a system.");
        assertTrue(!multivariable.path("system").asBoolean(),
                "A single multi-variable equation must have system=false.");

        JsonNode polynomialSystem = classify("x^2 + y = 4\nx + y^2 = 5");
        assertTrue(polynomialSystem.path("system").asBoolean(),
                "Multiple equations must be reported as a system.");
    }

    private void assertClassification(RecognitionCase testCase) throws Exception {
        JsonNode response = classify(testCase.equation());
        String actual = response.path("equationType").asText();
        String context = "Equation: \"" + testCase.equation() + "\"; expected classification: \""
                + testCase.expectedType() + "\"; actual classification: \"" + actual + "\"";

        assertEquals(testCase.expectedType(), actual, context);
        assertTrue(response.path("success").asBoolean(), context + "; response message: "
                + response.path("message").asText());
        if (testCase.expectedProperty() != null) {
            boolean propertyPresent = false;
            for (JsonNode property : response.path("secondaryProperties")) {
                if (testCase.expectedProperty().equals(property.asText())) {
                    propertyPresent = true;
                    break;
                }
            }
            assertTrue(propertyPresent, context + "; expected secondary property: \""
                    + testCase.expectedProperty() + "\"; actual properties: "
                    + response.path("secondaryProperties"));
        }
    }

    private JsonNode classify(String equation) throws Exception {
        String request = objectMapper.writeValueAsString(new EquationInput(equation));
        MvcResult result = mockMvc.perform(post("/api/equation/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, result.getResponse().getStatus(),
                "Equation: \"" + equation + "\"; classification endpoint response: "
                        + result.getResponse().getContentAsString());
        return response;
    }

    private record RecognitionCase(String name, String equation, String expectedType,
                                   String expectedProperty) {}

    private record EquationInput(String expression) {}
}
