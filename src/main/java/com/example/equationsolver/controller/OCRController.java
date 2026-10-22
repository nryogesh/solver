package com.example.equationsolver.controller;

import com.example.equationsolver.interfaces.OCRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR CONTROLLER — OOP: REST API Design
 *
 * Handles image upload and OCR text extraction requests.
 * Business logic is delegated to OCRService.
 */
@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OCRController {

    @Autowired
    private OCRService ocrService;

    /**
     * POST /api/ocr/extract
     * Uploads an image and extracts equation text using OCR.
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String extractedText = ocrService.extractText(file);
        boolean isValid = ocrService.isValidEquation(extractedText);

        response.put("success", true);
        response.put("extractedText", extractedText);
        response.put("isValidEquation", isValid);
        response.put("message", "Text extracted successfully from image");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/ocr/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OCR Service is running ✓");
    }
}
