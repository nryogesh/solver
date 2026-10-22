package com.example.equationsolver.exception;

/**
 * CUSTOM EXCEPTION — Thrown when equation format is invalid
 */
public class InvalidEquationException extends RuntimeException {

    private String errorCode;

    public InvalidEquationException(String message) {
        super(message);
        this.errorCode = "INVALID_EQUATION";
    }

    public InvalidEquationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidEquationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_EQUATION";
    }

    public String getErrorCode() { return errorCode; }
}
