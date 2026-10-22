package com.example.equationsolver.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * DTO — Data Transfer Object for solution results.
 *
 * Carries: original equation, step-by-step solution, and final answer.
 */
public class SolutionResponse {

    private String originalExpression;
    private String equationType;
    private List<String> steps;
    private String finalAnswer;
    private boolean solved;
    private boolean success;
    private String message;

    public SolutionResponse() {
        this.steps = new ArrayList<>();
    }

    public SolutionResponse(String originalExpression, String equationType,
                            List<String> steps, String finalAnswer, boolean solved) {
        this.originalExpression = originalExpression;
        this.equationType       = equationType;
        this.steps              = steps != null ? steps : new ArrayList<>();
        this.finalAnswer        = finalAnswer;
        this.solved             = solved;
        this.success            = true;
    }

    public static SolutionResponse unsupported(String expression, String type) {
        SolutionResponse r = new SolutionResponse();
        r.originalExpression = expression;
        r.equationType       = type;
        r.solved             = false;
        r.success            = true;
        r.message            = "Equation recognized successfully, but solving is not supported in the current version.";
        r.steps              = new ArrayList<>();
        r.finalAnswer        = "Not available";
        return r;
    }

    public static SolutionResponse error(String message) {
        SolutionResponse r = new SolutionResponse();
        r.success = false;
        r.solved  = false;
        r.message = message;
        r.steps   = new ArrayList<>();
        return r;
    }

    public void addStep(String step) {
        this.steps.add(step);
    }

    public String getOriginalExpression() { return originalExpression; }
    public void setOriginalExpression(String v) { this.originalExpression = v; }

    public String getEquationType() { return equationType; }
    public void setEquationType(String v) { this.equationType = v; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> v) { this.steps = v; }

    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String v) { this.finalAnswer = v; }

    public boolean isSolved() { return solved; }
    public void setSolved(boolean v) { this.solved = v; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }

    @Override
    public String toString() {
        return "SolutionResponse{equationType='" + equationType + "', solved=" + solved + "}";
    }
}
