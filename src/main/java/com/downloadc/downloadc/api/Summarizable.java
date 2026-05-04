package com.downloadc.downloadc.api;

// Interface that any summarizer must implement.
// Demonstrates: abstraction + interface + polymorphism
public interface Summarizable {

    // Returns true when this summarizer is ready to use.
    boolean isConfigured();

    // Summarize a body of text and return the summary string.
    // fileName is used for context in the prompt.
    String summarize(String extractedText, String fileName) throws Exception;
}