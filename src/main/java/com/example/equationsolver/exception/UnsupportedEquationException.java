package com.example.equationsolver.exception;

/**
 * CUSTOM EXCEPTION — Thrown when equation type is recognized but not solvable
 */
public class UnsupportedEquationException extends RuntimeException {

    private String equationType;
    private String errorCode;

    public UnsupportedEquationException(String message) {
        super(message);
        this.errorCode = "UNSUPPORTED_EQUATION";
    }

    public UnsupportedEquationException(String message, String equationType) {
        super(message);
        this.equationType = equationType;
        this.errorCode = "UNSUPPORTED_EQUATION";
    }

    public UnsupportedEquationException(String message, String equationType, Throwable cause) {
        super(message, cause);
        this.equationType = equationType;
        this.errorCode = "UNSUPPORTED_EQUATION";
    }

    public String getEquationType() { return equationType; }
    public String getErrorCode() { return errorCode; }
}
