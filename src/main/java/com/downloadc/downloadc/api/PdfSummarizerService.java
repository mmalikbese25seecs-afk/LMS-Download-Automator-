package com.downloadc.downloadc.api;

import java.io.InputStream;
import com.downloadc.downloadc.model.SummaryResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfSummarizerService {

    // Max characters to process from extracted text
    private static final int MAX_CHARS = 20000;

    // Number of sentences to include in summary
    private static final int SUMMARY_SENTENCES = 15;

    // Extracts text from PDF, cleans it, and returns a summary result
    public SummaryResult summarize(MultipartFile file) throws Exception {

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        System.out.println("Processing file: " + fileName);

        String fullText = "";
        int pageCount;

        // Load and extract text from the PDF
        try (InputStream is = file.getInputStream();
             PDDocument doc = Loader.loadPDF(is.readAllBytes())) {

            pageCount = doc.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Preserve reading order
            fullText = stripper.getText(doc);
            if (fullText == null || fullText.trim().isEmpty()) {
                throw new Exception("No readable text found in PDF (maybe scanned PDF)");
            }
        }

        // Clean raw text before processing
        String cleanedText = cleanText(fullText);

        // shorten if text exceeds limit
        String processText = cleanedText.length() > MAX_CHARS
                ? cleanedText.substring(0, MAX_CHARS)
                : cleanedText;

        // Generate extractive summary
        String summary = extractiveSummary(processText, SUMMARY_SENTENCES);

        // Create a short preview of the text
        String preview = processText.length() > 500
                ? processText.substring(0, 500) + "..."
                : processText;

        return new SummaryResult(
                fileName,
                pageCount,
                fullText,
                preview,
                summary
        );
    }

    // Scores and selects the most important sentences from the text
    private String extractiveSummary(String text, int maxSentences) {

        // Split text into sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");

        if (sentences.length == 0)
            return "Could not generate summary";

        // Return as-is if already within limit
        if (sentences.length <= maxSentences)
            return String.join(" ", sentences);

        // Keywords that indicate important sentences
        String[] keywords = {"therefore", "however", "conclusion", "result", "important",
                "define", "concept", "method", "approach", "algorithm",
                "example", "objective", "purpose", "summary", "analysis",
                "function", "class", "object", "interface", "input", "output"
        };

        double[] scores = new double[sentences.length];

        for (int i = 0; i < sentences.length; i++) {
            String s = sentences[i];
            String lower = s.toLowerCase();

            // Boost first and last sentences
            if (i == 0 || i == sentences.length - 1)
                scores[i] += 2;

            // Boost sentences containing keywords
            for (String kw : keywords) {
                if (lower.contains(kw))
                    scores[i] += 3;
            }

            String[] words = s.trim().split("\\s+");

            // Penalize very short sentences
            if (words.length < 6)
                scores[i] -= 5;

            // Reward longer, more informative sentences
            if (words.length > 10)
                scores[i] += Math.min(words.length * 0.1, 2);
        }

        // Sort sentence indices by score descending
        Integer[] indices = new Integer[sentences.length];
        for (int i = 0; i < indices.length; i++)
            indices[i] = i;

        java.util.Arrays.sort(indices,
                (a, b) -> Double.compare(scores[b], scores[a]));

        // Pick top-scoring sentences, ignoring very low scores
        java.util.List<Integer> top = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(maxSentences, indices.length); i++) {
            if (scores[indices[i]] > -3)
                top.add(indices[i]);
        }

        // Restore original sentence order
        java.util.Collections.sort(top);

        StringBuilder sb = new StringBuilder();
        for (int idx : top) {
            sb.append(sentences[idx].trim()).append(" ");
        }

        return sb.toString().trim();
    }

    // Normalizes whitespace and removes stray page numbers from extracted text
    private String cleanText(String raw) {
        return raw
                .replaceAll("\\r\\n|\\r", "\n")        // Normalize line endings
                .replaceAll("\\n{3,}", "\n\n")          // Collapse excess blank lines
                .replaceAll("[ \\t]{2,}", " ")          // Collapse extra spaces/tabs
                .replaceAll("(?m)^\\s*\\d+\\s*$", "")  // Remove lone page numbers
                .trim();
    }
}