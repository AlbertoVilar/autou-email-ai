package com.autou.emailai.infrastructure.file;

import com.autou.emailai.application.ports.out.FileTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor implements FileTextExtractor {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return contentType != null && contentType.equalsIgnoreCase("application/pdf");
    }

    @Override
    public String extract(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível extrair texto do PDF.", e);
        }
    }
}
