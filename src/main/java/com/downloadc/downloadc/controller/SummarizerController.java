package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.GeminiSummarizerService;
import com.downloadc.downloadc.api.PdfSummarizerService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.SummaryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/summarize")
public class SummarizerController {

    @Autowired private SessionManager sessionManager;
    @Autowired private PdfSummarizerService localSummarizer;
    @Autowired private GeminiSummarizerService geminiSummarizer;

    // Check which summarizers are available
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "localAvailable", true,
                "geminiAvailable", geminiSummarizer.isConfigured()
        ));
    }

    // Upload PDF and summarize
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> summarize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "engine", defaultValue = "gemini") String engine,
            @RequestParam(value = "style", defaultValue = "concise") String style,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "customPrompt", defaultValue = "") String customPrompt) {

        // Check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));

        if (file.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No file uploaded"));

        String name = (file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "").toLowerCase();

        if (!name.endsWith(".pdf"))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files allowed."));

        if (file.getSize() > 20L * 1024 * 1024)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File too large (max 20MB)."));

        try {
            // Extract text from PDF first using local summarizer
            System.out.println("[SummarizerController] Extracting text from PDF: " + name);
            SummaryResult extracted = localSummarizer.summarize(file);
            String extractedText = extracted.getFullText();

            System.out.println("[SummarizerController] Extracted " + extractedText.length() + " characters");

            if ("gemini".equalsIgnoreCase(engine)) {

                if (!geminiSummarizer.isConfigured()) {
                    System.out.println("[SummarizerController] Gemini not configured, falling back to local");
                    return ResponseEntity.ok(Map.of(
                            "fileName", extracted.getFileName(),
                            "pageCount", extracted.getPageCount(),
                            "fullText", extracted.getFullText(),
                            "preview", extracted.getExtractedText(),
                            "summary", extracted.getSummary(),
                            "engine", "local",
                            "warning", "Gemini not configured, used local summarizer instead"
                    ));
                }

                System.out.println("[SummarizerController] Calling Gemini API with style=" + style + ", lang=" + language);

                // Build prompt with user preferences
                String fullPrompt = buildPrompt(extractedText, name, style, language, customPrompt);

                // Call Gemini with the full prompt
                String aiSummary = geminiSummarizer.summarizeWithPrompt(fullPrompt);

                System.out.println("[SummarizerController] Gemini returned summary of " + aiSummary.length() + " chars");

                // Return result with Gemini summary
                return ResponseEntity.ok(Map.of(
                        "fileName", extracted.getFileName(),
                        "pageCount", extracted.getPageCount(),
                        "fullText", extracted.getFullText(),
                        "preview", extracted.getExtractedText(),
                        "summary", aiSummary,
                        "engine", "gemini",
                        "style", style,
                        "language", language
                ));

            } else {
                // Default: local summarizer
                System.out.println("[SummarizerController] Using local summarizer");
                return ResponseEntity.ok(Map.of(
                        "fileName", extracted.getFileName(),
                        "pageCount", extracted.getPageCount(),
                        "fullText", extracted.getFullText(),
                        "preview", extracted.getExtractedText(),
                        "summary", extracted.getSummary(),
                        "engine", "local"
                ));
            }

        } catch (Exception e) {
            System.out.println("[SummarizerController] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Build prompt with user preferences
    private String buildPrompt(String text, String fileName, String style, String language, String customPrompt) {
        String safeName = fileName != null
                ? fileName.replaceAll("[\"'\\\\]", "")
                : "document";

        String styleInstructions = switch (style.toLowerCase()) {
            case "detailed" -> "Provide a detailed, comprehensive summary with all important points.";
            case "bullet" -> "Format the summary as bullet points with key takeaways.";
            case "exam" -> "Create exam-style notes with key concepts, definitions, and important facts to memorize.";
            default -> "Provide a concise, brief summary of the main points."; // concise
        };

        String langInstructions = switch (language.toLowerCase()) {
            case "ur" -> "Write the summary in Urdu.";
            case "simple" -> "Write the summary using simple, easy-to-understand English.";
            default -> "Write the summary in English."; // en
        };

        String customPart = customPrompt != null && !customPrompt.isBlank()
                ? "\n\nAdditional instructions: " + customPrompt
                : "";

        return "You are a helpful academic assistant. Summarize the document '" + safeName + "' using these sections:\n\n" +
                "**Key Topics** — What main subjects does this cover?\n" +
                "**Core Concepts** — What are the key ideas explained?\n" +
                "**Key Takeaways** — What should the reader remember?\n" +
                "**Overview** — A brief 2-3 sentence summary.\n\n" +
                styleInstructions + " " + langInstructions + " Be clear and student-friendly." +
                customPart + "\n\n" +
                "DOCUMENT TEXT:\n" + text;
    }
}
