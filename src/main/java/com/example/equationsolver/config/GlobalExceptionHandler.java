package com.example.equationsolver.config;

import com.example.equationsolver.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER — OOP: Encapsulation + Spring AOP
 *
 * Catches all exceptions from controllers and returns proper HTTP responses.
 * The application never crashes unexpectedly.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OCRException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleOCRException(OCRException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", ex.getErrorCode());
        response.put("message", "OCR Extraction Failed: " + ex.getMessage());
        response.put("type", "OCRException");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidEquationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleInvalidEquationException(InvalidEquationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", ex.getErrorCode());
        response.put("message", "Invalid Equation: " + ex.getMessage());
        response.put("type", "InvalidEquationException");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UnsupportedEquationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Object>> handleUnsupportedEquationException(UnsupportedEquationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("errorCode", ex.getErrorCode());
        response.put("message", ex.getMessage());
        response.put("equationType", ex.getEquationType());
        response.put("type", "UnsupportedEquationException");
        response.put("note", "Equation recognized but solving is not supported in current version");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(SolverException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleSolverException(SolverException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", ex.getErrorCode());
        response.put("message", "Solver Error: " + ex.getMessage());
        response.put("solverType", ex.getSolverType());
        response.put("type", "SolverException");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "FILE_TOO_LARGE");
        response.put("message", "File size exceeds maximum allowed size (5MB)");
        response.put("type", "MaxUploadSizeExceededException");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        logger.error("Unhandled request failure", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "INTERNAL_ERROR");
        response.put("message", "An unexpected server error occurred. Please check the server logs.");
        response.put("type", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
