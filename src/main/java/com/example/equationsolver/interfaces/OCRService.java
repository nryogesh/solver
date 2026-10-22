package com.example.equationsolver.interfaces;

import org.springframework.web.multipart.MultipartFile;

/**
 * INTERFACE — OOP: Abstraction
 *
 * Defines the contract for OCR text extraction.
 * Implemented by Tess4JOCRService using Tesseract.
 */
public interface OCRService {

    /**
     * Extract equation text from an uploaded image file.
     * @param imageFile the uploaded image (PNG, JPG, JPEG)
     * @return extracted text string
     */
    String extractText(MultipartFile imageFile);

    /**
     * Check if the extracted text contains a valid equation.
     */
    boolean isValidEquation(String text);
}
