package com.course.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class FileParserService {

    @Value("${tesseract.datapath:./tessdata}")
    private String tessDataPath;

    @Value("${tesseract.language:chi_sim+eng}")
    private String language;

    /**
     * 从上传文件中提取文字（支持 PNG/JPG/PDF/DOCX/DOC）
     * @return 提取到的文字内容
     */
    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名为空");
        }

        String lower = filename.toLowerCase();
        System.out.println("[FileParser] 解析文件: " + filename + ", 大小: " + file.getSize());

        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp") || lower.endsWith(".gif")) {
            return extractTextFromImage(file);
        } else if (lower.endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (lower.endsWith(".docx")) {
            return extractTextFromDocx(file);
        } else if (lower.endsWith(".doc")) {
            return extractTextFromDoc(file);
        } else {
            throw new RuntimeException("不支持的文件格式: " + lower + "，支持 PNG/JPG/PDF/DOCX/DOC");
        }
    }

    // ==================== 图片 OCR ====================

    private String extractTextFromImage(MultipartFile file) throws Exception {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new RuntimeException("无法读取图片文件");
        }

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(language);

        String text = tesseract.doOCR(image);
        System.out.println("[FileParser] 图片 OCR 完成, 文字长度: " + text.length());
        return text.trim();
    }

    // ==================== PDF 解析 ====================

    private String extractTextFromPdf(MultipartFile file) throws Exception {
        File tempFile = File.createTempFile("pdf_", ".pdf");
        try {
            file.transferTo(tempFile);
            try (PDDocument document = Loader.loadPDF(tempFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                System.out.println("[FileParser] PDF 解析完成, 文字长度: " + text.length());
                return text.trim();
            }
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ==================== Word .docx 解析 ====================

    private String extractTextFromDocx(MultipartFile file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder text = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
                String pText = paragraph.getText();
                if (pText != null && !pText.isEmpty()) {
                    text.append(pText).append("\n");
                }
            }
            System.out.println("[FileParser] DOCX 解析完成, 文字长度: " + text.length());
            return text.toString().trim();
        }
    }

    // ==================== Word .doc 解析（旧版）====================

    private String extractTextFromDoc(MultipartFile file) throws Exception {
        File tempFile = File.createTempFile("doc_", ".doc");
        try {
            file.transferTo(tempFile);
            org.apache.poi.hwpf.HWPFDocument document = 
                new org.apache.poi.hwpf.HWPFDocument(new java.io.FileInputStream(tempFile));
            
            org.apache.poi.hwpf.extractor.WordExtractor extractor = 
                new org.apache.poi.hwpf.extractor.WordExtractor(document);
            
            String[] paragraphs = extractor.getParagraphText();
            StringBuilder text = new StringBuilder();
            for (String para : paragraphs) {
                if (para != null && !para.isEmpty()) {
                    text.append(para).append("\n");
                }
            }
            
            extractor.close();
            document.close();
            
            System.out.println("[FileParser] DOC 解析完成, 文字长度: " + text.length());
            return text.toString().trim();
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

}
