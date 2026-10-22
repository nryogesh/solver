package com.example.equationsolver.exception;

/**
 * CUSTOM EXCEPTION — Thrown when solver encounters an error
 */
public class SolverException extends RuntimeException {

    private String errorCode;
    private String solverType;

    public SolverException(String message) {
        super(message);
        this.errorCode = "SOLVER_ERROR";
    }

    public SolverException(String message, String solverType) {
        super(message);
        this.solverType = solverType;
        this.errorCode = "SOLVER_ERROR";
    }

    public SolverException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SOLVER_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public String getSolverType() { return solverType; }
}
