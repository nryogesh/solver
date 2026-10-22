package com.example.equationsolver.exception;

/**
 * CUSTOM EXCEPTION — Thrown when OCR extraction fails
 */
public class OCRException extends RuntimeException {

    private String errorCode;

    public OCRException(String message) {
        super(message);
        this.errorCode = "OCR_ERROR";
    }

    public OCRException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OCRException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "OCR_ERROR";
    }

    public String getErrorCode() { return errorCode; }
}
