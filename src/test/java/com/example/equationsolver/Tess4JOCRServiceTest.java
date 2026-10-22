package com.example.equationsolver;

import com.example.equationsolver.exception.OCRException;
import com.example.equationsolver.service.impl.Tess4JOCRService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Tess4JOCRServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void extractsOutputForValidPngAndCleansTemporaryFiles() throws Exception {
        FakeOCRService service = configuredService("x = 2");
        String text = service.extractText(imageFile("equation.png", "image/png", pngBytes()));

        assertEquals("x = 2", text);
        assertNotNull(service.imagePath);
        assertFalse(Files.exists(service.imagePath));
        assertFalse(Files.exists(service.outputPath));
        assertFalse(Files.exists(service.errorPath));
    }

    @Test
    void rejectsUnsupportedTypesAndEmptyUploads() {
        Tess4JOCRService service = configuredService("");
        OCRException unsupported = assertThrows(OCRException.class,
                () -> service.extractText(imageFile("equation.gif", "image/gif", new byte[]{1})));
        assertEquals("INVALID_FORMAT", unsupported.getErrorCode());

        OCRException empty = assertThrows(OCRException.class,
                () -> service.extractText(imageFile("equation.png", "image/png", new byte[0])));
        assertEquals("EMPTY_FILE", empty.getErrorCode());
    }

    @Test
    void rejectsUnreadableImagesAndOversizedFiles() {
        Tess4JOCRService service = configuredService("");
        OCRException unreadable = assertThrows(OCRException.class,
                () -> service.extractText(imageFile("equation.png", "image/png", "not an image".getBytes())));
        assertEquals("INVALID_IMAGE", unreadable.getErrorCode());

        OCRException oversized = assertThrows(OCRException.class,
                () -> service.extractText(imageFile("equation.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
        assertEquals("FILE_TOO_LARGE", oversized.getErrorCode());
    }

    @Test
    void explainsMissingLanguageDataAndExecutable() throws Exception {
        Tess4JOCRService missingData = configuredService("");
        ReflectionTestUtils.setField(missingData, "tesseractDataPath", tempDirectory.resolve("missing").toString());
        OCRException dataError = assertThrows(OCRException.class,
                () -> missingData.extractText(imageFile("equation.png", "image/png", pngBytes())));
        assertEquals("TESSDATA_NOT_FOUND", dataError.getErrorCode());
        assertTrue(dataError.getMessage().contains("tesseract.datapath"));

        Tess4JOCRService missingExecutable = new Tess4JOCRService();
        configure(missingExecutable);
        ReflectionTestUtils.setField(missingExecutable, "tesseractExecutable",
                tempDirectory.resolve("not-installed.exe").toString());
        OCRException executableError = assertThrows(OCRException.class,
                () -> missingExecutable.extractText(imageFile("equation.png", "image/png", pngBytes())));
        assertEquals("TESSERACT_NOT_FOUND", executableError.getErrorCode());
        assertTrue(executableError.getMessage().contains("tesseract.executable"));
    }

    @Test
    void reportsEmptyOcrOutputAndExecutionErrors() {
        FakeOCRService empty = configuredService("  \r\n");
        OCRException noText = assertThrows(OCRException.class,
                () -> empty.extractText(imageFile("equation.png", "image/png", pngBytes())));
        assertEquals("NO_TEXT_DETECTED", noText.getErrorCode());

        FailingOCRService failure = new FailingOCRService();
        configure(failure);
        OCRException executionError = assertThrows(OCRException.class,
                () -> failure.extractText(imageFile("equation.png", "image/png", pngBytes())));
        assertEquals("FILE_IO_ERROR", executionError.getErrorCode());
        assertFalse(executionError.getMessage().contains("internal diagnostic"));
    }

    private FakeOCRService configuredService(String output) {
        FakeOCRService service = new FakeOCRService(output);
        configure(service);
        return service;
    }

    private void configure(Tess4JOCRService service) {
        Path data = tempDirectory.resolve("tessdata");
        try {
            Files.createDirectories(data);
            Files.write(data.resolve("eng.traineddata"), new byte[0]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ReflectionTestUtils.setField(service, "tesseractDataPath", data.toString());
        ReflectionTestUtils.setField(service, "tesseractExecutable", "tesseract");
        ReflectionTestUtils.setField(service, "tesseractLanguage", "eng");
    }

    private MockMultipartFile imageFile(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static class FakeOCRService extends Tess4JOCRService {
        private final String output;
        private Path imagePath;
        private Path outputPath;
        private Path errorPath;

        private FakeOCRService(String output) {
            this.output = output;
        }

        @Override
        protected void runTesseract(Path imagePath, Path outputPath, Path errorPath) throws IOException {
            this.imagePath = imagePath;
            this.outputPath = outputPath;
            this.errorPath = errorPath;
            Files.writeString(outputPath, output == null ? "" : output);
        }
    }

    private static final class FailingOCRService extends Tess4JOCRService {
        @Override
        protected void runTesseract(Path imagePath, Path outputPath, Path errorPath) throws IOException {
            throw new IOException("internal diagnostic details");
        }
    }
}
