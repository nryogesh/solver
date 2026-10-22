package com.example.equationsolver;

import com.example.equationsolver.config.GlobalExceptionHandler;
import com.example.equationsolver.exception.OCRException;
import com.example.equationsolver.interfaces.OCRService;
import com.example.equationsolver.controller.OCRController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OCRController.class)
@Import(GlobalExceptionHandler.class)
class OCRControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OCRService ocrService;

    @Test
    void acceptsFileFieldAndReturnsExtractedText() throws Exception {
        when(ocrService.extractText(any())).thenReturn("x = 2");
        when(ocrService.isValidEquation("x = 2")).thenReturn(true);
        MockMultipartFile image = new MockMultipartFile("file", "equation.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/ocr/extract").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.extractedText").value("x = 2"))
                .andExpect(jsonPath("$.isValidEquation").value(true));
    }

    @Test
    void returnsUsefulOcrConfigurationErrors() throws Exception {
        when(ocrService.extractText(any())).thenThrow(new OCRException(
                "Tesseract trained data was not found. Set tesseract.datapath.", "TESSDATA_NOT_FOUND"));
        MockMultipartFile image = new MockMultipartFile("file", "equation.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/ocr/extract").file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TESSDATA_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(
                        "OCR Extraction Failed: Tesseract trained data was not found. Set tesseract.datapath."));
    }
}
