package com.downloadc.downloadc.model;

public class SummaryResult {

    private final String fileName;
    private final int pageCount;
    private final String extractedText;
    private final String summary;
    private final String fullText;

    // Used by PdfSummarizerService (local summarizer)
    // Full constructor with 5 parameters
    public SummaryResult(String fileName, int pageCount,
                         String fullText, String extractedText, String summary) {
        this.fileName = fileName;
        this.pageCount = pageCount;
        this.fullText = fullText;
        this.extractedText = extractedText;
        this.summary = summary;
    }

    // Used by SummarizerController (Gemini path)
    // 4 parameter constructor
    public SummaryResult(String fileName, int pageCount,
                         String fullText, String summary) {
        this.fileName = fileName;
        this.pageCount = pageCount;
        this.fullText = fullText;
        this.extractedText = fullText.length() > 500
                ? fullText.substring(0, 500) + "..."
                : fullText;
        this.summary = summary;
    }

    public String getFileName()      { return fileName; }
    public int getPageCount()        { return pageCount; }
    public String getExtractedText() { return extractedText; }
    public String getFullText()      { return fullText; }
    public String getSummary()       { return summary; }
}