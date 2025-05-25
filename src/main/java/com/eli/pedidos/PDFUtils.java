package com.eli.pedidos;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PDFUtils {

    public static String extraerTexto(File archivoPdf) throws IOException {
        try (PDDocument document = PDDocument.load(archivoPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}