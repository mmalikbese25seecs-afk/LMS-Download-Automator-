package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.DownloadRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Handles download history APIs
@RestController
@RequestMapping("/api/history")
public class DownloadHistoryController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private DownloadHistoryService historyService;

    //Get all download history
    @GetMapping
    public ResponseEntity<?> getHistory() {

        // Check if user logged in
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<DownloadRecord> records = historyService.readAll();

        // Show newest first
        java.util.Collections.reverse(records);

        return ResponseEntity.ok(records);
    }

    // Gets. stats 
   @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String course,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(defaultValue = "newest") String sort) {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        List<DownloadRecord> results = historyService.search(q, course, tag, sort);

        return ResponseEntity.ok(results);
    }

    // get stats (total files, size, etc)
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        return ResponseEntity.ok(historyService.getStats());
    }

    // get all tags
    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags() {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        return ResponseEntity.ok(historyService.getAllTags());
    }

    // add tag to a file
    @PostMapping("/tag")
    public ResponseEntity<?> addTag(@RequestBody Map<String, String> body) {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        String fileName = body.get("fileName");
        String courseName = body.get("courseName");
        String tag = body.get("tag");

        // validate input
        if (fileName == null || courseName == null || tag == null || tag.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "fileName, courseName and tag are required"));

        boolean ok = historyService.addTag(fileName, courseName, tag);

        return ok
                ? ResponseEntity.ok(Map.of("message", "Tag added."))
                : ResponseEntity.status(404).body(Map.of("error", "Record not found"));
    }

    // remove tag
    @DeleteMapping("/tag")
    public ResponseEntity<?> removeTag(@RequestBody Map<String, String> body) {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in "));

        String fileName = body.get("fileName");
        String courseName = body.get("courseName");
        String tag = body.get("tag");

        // validate input
        if (fileName == null || courseName == null || tag == null)
            return ResponseEntity.badRequest().body(Map.of("error", "fileName, courseName and tag are required"));

        boolean ok = historyService.removeTag(fileName, courseName, tag);

        return ok
                ? ResponseEntity.ok(Map.of("message", "Tag removed"))
                : ResponseEntity.status(404).body(Map.of("error", "Record not found"));
    }

    // Delete all history
    @DeleteMapping
    public ResponseEntity<?> clearHistory() {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        try {
            historyService.clearAll();
            return ResponseEntity.ok(Map.of("message", "History cleared."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}  

