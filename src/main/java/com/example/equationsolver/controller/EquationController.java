package com.example.equationsolver.controller;

import com.example.equationsolver.dto.ClassificationResponse;
import com.example.equationsolver.dto.EquationRequest;
import com.example.equationsolver.dto.SolutionResponse;
import com.example.equationsolver.interfaces.EquationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST CONTROLLER — OOP: Encapsulation + REST API Design
 *
 * Handles HTTP requests for equation classification and solving.
 * Business logic is fully delegated to EquationService (Service Layer Pattern).
 * Controllers do NOT contain any business logic.
 */
@RestController
@RequestMapping("/api/equation")
@CrossOrigin(origins = "*")
public class EquationController {

    @Autowired
    private EquationService equationService;

    /**
     * POST /api/equation/classify
     * Classifies an equation and returns its type, degree, variables, etc.
     */
    @PostMapping("/classify")
    public ResponseEntity<ClassificationResponse> classify(@RequestBody EquationRequest request) {
        ClassificationResponse response = equationService.classify(request.getExpression());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/equation/solve
     * Solves the given equation and returns step-by-step solution.
     */
    @PostMapping("/solve")
    public ResponseEntity<SolutionResponse> solve(@RequestBody EquationRequest request) {
        SolutionResponse response = equationService.solve(request.getExpression());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * GET /api/equation/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Equation Solver API is running ✓");
    }
}
