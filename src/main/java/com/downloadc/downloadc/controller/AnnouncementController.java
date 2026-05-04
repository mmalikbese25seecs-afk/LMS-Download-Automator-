package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.AnnouncementService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.Announcement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Exposes GET /api/announcements?limit=N — returns latest notifications as JSON.
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private SessionManager sessionManager;

    @GetMapping
    public ResponseEntity<?> getAnnouncements(
            @RequestParam(defaultValue = "10") int limit) {

        // Reject the request immediately if no active session exists.
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in."));
        }

        // TODO: return actual announcements here
        return ResponseEntity.ok(List.of());

    }

}