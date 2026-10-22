package com.example.equationsolver.service.impl;

import com.example.equationsolver.exception.OCRException;
import com.example.equationsolver.interfaces.OCRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OCR SERVICE IMPLEMENTATION — OOP: Encapsulation + Interface Implementation
 *
 * Extracts equation text from images using the separately installed Tesseract CLI.
 *
 * Design Pattern: Strategy — can be swapped with another OCR provider.
 *
 * Tesseract and its trained language data must be installed outside Maven.
 */
@Service
public class Tess4JOCRService implements OCRService {

    private static final Logger logger = LoggerFactory.getLogger(Tess4JOCRService.class);
    private static final Set<String> ALLOWED_FORMATS = Set.of("png", "jpg", "jpeg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Duration OCR_TIMEOUT = Duration.ofSeconds(30);

    @Value("${tesseract.datapath:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tesseractDataPath;

    @Value("${tesseract.executable:tesseract}")
    private String tesseractExecutable;

    @Value("${tesseract.language:eng}")
    private String tesseractLanguage;

    /**
     * Extracts equation text from an uploaded image.
     *
     * @param imageFile the uploaded image (PNG, JPG, JPEG)
     * @return extracted text string
     * @throws OCRException if extraction fails
     */
    @Override
    public String extractText(MultipartFile imageFile) {
        Path imagePath = null;
        Path outputPath = null;
        Path errorPath = null;
        try {
            validateImageFile(imageFile);

            BufferedImage bufferedImage;
            try (InputStream input = imageFile.getInputStream()) {
                bufferedImage = ImageIO.read(input);
            }
            if (bufferedImage == null) {
                throw new OCRException("The uploaded file is not a readable PNG or JPEG image.", "INVALID_IMAGE");
            }

            validateTesseractConfiguration();
            imagePath = Files.createTempFile("equation-ocr-", imageSuffix(imageFile));
            outputPath = Files.createTempFile("equation-ocr-", ".txt");
            errorPath = Files.createTempFile("equation-ocr-", ".log");
            Files.write(imagePath, imageFile.getBytes());
            runTesseract(imagePath, outputPath, errorPath);

            String result = Files.readString(outputPath, StandardCharsets.UTF_8).trim();
            if (result.isEmpty()) {
                throw new OCRException("No text was detected. Use a clear, well-lit image and try again.", "NO_TEXT_DETECTED");
            }
            return result;
        } catch (OCRException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("OCR processing was interrupted", e);
            throw new OCRException("OCR processing was interrupted. Please try again.", "OCR_INTERRUPTED");
        } catch (IOException e) {
            logger.error("OCR file processing failed", e);
            throw new OCRException("The image could not be processed. Please try another PNG or JPEG image.", "FILE_IO_ERROR");
        } catch (Exception e) {
            logger.error("Unexpected OCR processing failure", e);
            throw new OCRException("OCR processing failed. Check the server configuration and logs for details.", "OCR_PROCESSING_ERROR");
        } finally {
            deleteTemporaryFile(imagePath);
            deleteTemporaryFile(outputPath);
            deleteTemporaryFile(errorPath);
        }
    }

    protected void runTesseract(Path imagePath, Path outputPath, Path errorPath) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(
                    tesseractExecutable, imagePath.toString(), "stdout",
                    "--tessdata-dir", tesseractDataPath,
                    "-l", tesseractLanguage,
                    "--psm", "6")
                    .redirectOutput(outputPath.toFile())
                    .redirectError(errorPath.toFile())
                    .start();
        } catch (IOException e) {
            logger.error("Could not start configured Tesseract executable '{}'", tesseractExecutable, e);
            throw new OCRException(
                    "Tesseract was not found. Install it or set tesseract.executable to its full path.",
                    "TESSERACT_NOT_FOUND");
        }

        boolean completed;
        try {
            completed = process.waitFor(OCR_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            process.waitFor();
            throw e;
        }
        if (!completed) {
            logger.warn("Tesseract exceeded the {} second OCR timeout", OCR_TIMEOUT.toSeconds());
            process.destroyForcibly().waitFor();
            throw new OCRException("Tesseract timed out while processing the image. Try a smaller image.", "OCR_TIMEOUT");
        }
        if (process.exitValue() != 0) {
            String diagnostic = Files.readString(errorPath, StandardCharsets.UTF_8).trim();
            logger.error("Tesseract exited with status {}: {}", process.exitValue(), diagnostic);
            throw new OCRException(
                    "Tesseract could not process the image. Verify the installation and trained-data configuration.",
                    "TESSERACT_ERROR");
        }
    }

    /**
     * Validates if the extracted text contains a valid equation.
     */
    @Override
    public boolean isValidEquation(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("=") && text.matches(".*[a-zA-Z0-9].*");
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null) {
            throw new OCRException("Please select an image file.", "EMPTY_FILE");
        }
        if (imageFile.isEmpty()) {
            throw new OCRException("File is empty", "EMPTY_FILE");
        }
        if (imageFile.getSize() > MAX_FILE_SIZE) {
            throw new OCRException("File size exceeds 5MB limit", "FILE_TOO_LARGE");
        }
        String filename = imageFile.getOriginalFilename();
        if (filename == null || !isValidImageFormat(filename)) {
            throw new OCRException("Unsupported image format. Allowed: PNG, JPG, JPEG", "INVALID_FORMAT");
        }
    }

    private void validateTesseractConfiguration() {
        Path dataDirectory;
        try {
            dataDirectory = Path.of(tesseractDataPath);
        } catch (RuntimeException e) {
            throw new OCRException("The configured Tesseract trained-data path is invalid. Update tesseract.datapath.", "INVALID_TESSDATA_PATH");
        }
        if (!Files.isDirectory(dataDirectory)) {
            logger.error("Configured Tesseract trained-data directory does not exist: {}", dataDirectory);
            throw new OCRException(
                    "Tesseract trained data was not found. Install the language data and set tesseract.datapath to its tessdata folder.",
                    "TESSDATA_NOT_FOUND");
        }

        Path languageData = dataDirectory.resolve(tesseractLanguage + ".traineddata");
        if (!Files.isRegularFile(languageData)) {
            logger.error("Tesseract language data is missing for language '{}' in {}", tesseractLanguage, dataDirectory);
            throw new OCRException(
                    "Tesseract language data for '" + tesseractLanguage + "' is missing. Install that language pack or update tesseract.language.",
                    "TESSDATA_NOT_FOUND");
        }
        if (tesseractExecutable == null || tesseractExecutable.isBlank()) {
            throw new OCRException("Set tesseract.executable to the Tesseract executable or its PATH name.", "TESSERACT_NOT_CONFIGURED");
        }
    }

    private boolean isValidImageFormat(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex < 0) return false;
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_FORMATS.contains(extension);
    }

    private String imageSuffix(MultipartFile imageFile) {
        String filename = imageFile.getOriginalFilename();
        return "." + filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteTemporaryFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Could not remove temporary OCR file", e);
        }
    }
}
