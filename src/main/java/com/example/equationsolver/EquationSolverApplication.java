package com.example.equationsolver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * MAIN APPLICATION CLASS — Spring Boot Entry Point
 *
 * Starts the Smart Equation Solver application.
 * Enables component scanning and auto-configuration.
 *
 * OOP Concepts in this project:
 *  1. ENCAPSULATION  — All model fields private with getters/setters
 *  2. INHERITANCE    — Equation hierarchy (LinearEquation, QuadraticEquation, etc.)
 *  3. ABSTRACTION    — Interfaces (EquationSolver, EquationClassifier, etc.)
 *  4. POLYMORPHISM   — SolverFactory returns correct solver at runtime
 *
 * Design Patterns:
 *  1. Factory Pattern   — SolverFactory, EquationFactory
 *  2. Strategy Pattern  — LinearSolver, QuadraticSolver, etc.
 *  3. Service Layer     — EquationServiceImpl, Tess4JOCRService
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.example.equationsolver")
public class EquationSolverApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquationSolverApplication.class, args);

        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║   AI-Powered Smart Formula Recognition                        ║");
        System.out.println("║   & Algebraic Equation Solver                                 ║");
        System.out.println("║                                                               ║");
        System.out.println("║   Access: http://localhost:8080                               ║");
        System.out.println("║   API:    http://localhost:8080/api/equation/health           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }
}
