package com.example.equationsolver.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO — Data Transfer Object for classification results.
 *
 * Encapsulates the data returned by /api/equation/classify
 */
public class ClassificationResponse {

    private String originalExpression;
    private String equationType;
    private int degree;
    private int variables;
    private String solvingMethod;
    private String description;
    private boolean solvable;
    private boolean success;
    private String message;
    private String subtype;
    private List<String> secondaryProperties = new ArrayList<>();
    private List<String> variableNames = new ArrayList<>();
    private int equationCount;
    private boolean system;
    private String classificationStatus;

    public ClassificationResponse() {}

    public ClassificationResponse(String originalExpression, String equationType,
                                  int degree, int variables, String solvingMethod,
                                  String description, boolean solvable) {
        this.originalExpression = originalExpression;
        this.equationType       = equationType;
        this.degree             = degree;
        this.variables          = variables;
        this.solvingMethod      = solvingMethod;
        this.description        = description;
        this.solvable           = solvable;
        this.success            = true;
    }

    public static ClassificationResponse error(String message) {
        ClassificationResponse r = new ClassificationResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public String getOriginalExpression() { return originalExpression; }
    public void setOriginalExpression(String v) { this.originalExpression = v; }

    public String getEquationType() { return equationType; }
    public void setEquationType(String v) { this.equationType = v; }

    public int getDegree() { return degree; }
    public void setDegree(int v) { this.degree = v; }

    public int getVariables() { return variables; }
    public void setVariables(int v) { this.variables = v; }

    public String getSolvingMethod() { return solvingMethod; }
    public void setSolvingMethod(String v) { this.solvingMethod = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public boolean isSolvable() { return solvable; }
    public void setSolvable(boolean v) { this.solvable = v; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }

    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }

    public List<String> getSecondaryProperties() { return secondaryProperties; }
    public void setSecondaryProperties(List<String> properties) {
        this.secondaryProperties = properties == null ? new ArrayList<>() : new ArrayList<>(properties);
    }

    public List<String> getVariableNames() { return variableNames; }
    public void setVariableNames(List<String> variables) {
        this.variableNames = variables == null ? new ArrayList<>() : new ArrayList<>(variables);
    }

    public int getEquationCount() { return equationCount; }
    public void setEquationCount(int equationCount) { this.equationCount = equationCount; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public String getClassificationStatus() { return classificationStatus; }
    public void setClassificationStatus(String classificationStatus) { this.classificationStatus = classificationStatus; }

    @Override
    public String toString() {
        return "ClassificationResponse{equationType='" + equationType + "', degree=" + degree + "}";
    }
}
