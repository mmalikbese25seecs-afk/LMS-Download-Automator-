package com.downloadc.downloadc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

// This class handles all exceptions globally for controllers

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles file upload size limit errors
    // Happens when user uploads abigger file

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException e) {

        System.out.println("[GlobalExceptionHandler] File too large : " + e.getMessage());

        return ResponseEntity.status(413).body(Map.of("error", "File is too large. Maximum upload size is 20MB."));
    }

    // handles invalid arguments passed to methods
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {

        System.out.println("[GlobalExceptionHandler] Bad argument: " + e.getMessage());

        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }


    //fallback if no other handler matches
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {

        System.out.println("[GlobalExceptionHandler] Unexpected error: "+ e.getClass().getSimpleName() + " - " + e.getMessage());

        e.printStackTrace();

        return ResponseEntity.status(500).body(Map.of("error", "Something went wrong. Please try again."));
    }
}
