package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.api.WeeklyFileService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.DownloadStatus;
import com.downloadc.downloadc.model.WeeklySection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Shows course content by week
@RestController
@RequestMapping("/api/courses")
public class WeeklyDownloadController {

    @Autowired private SessionManager sessionManager;
    @Autowired private DownloadHistoryService historyService;

    // get all weeks with file counts
    @GetMapping("/{courseId}/weeks")
    public ResponseEntity<?> getWeeks(
            @PathVariable int courseId,
            @RequestParam String shortName) {

        // Check if user is logged in first
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        try {
            // Make api client and fetch weekly sections
            MoodleApiClient   client  = new MoodleApiClient(sessionManager.getActiveConfig());
            WeeklyFileService service = new WeeklyFileService(client);
            List<WeeklySection> weeks = service.getWeeklySections(courseId, shortName);
            return ResponseEntity.ok(weeks);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // download selected weeks
    @PostMapping("/{courseId}/weeks/download")
    public ResponseEntity<?> downloadWeeks(
            @PathVariable int courseId,
            @RequestBody Map<String, Object> body) {

        // login check
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        String shortName = (String) body.getOrDefault("shortName", String.valueOf(courseId));

        // week list
        @SuppressWarnings("unchecked")
        List<Integer> requested = (List<Integer>) body.get("sectionNumbers");

        // validate
        if (requested == null || requested.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "no weeks"));

        try {
            MoodleApiClient   client   = new MoodleApiClient(sessionManager.getActiveConfig());
            WeeklyFileService wService = new WeeklyFileService(client);
            FileDownloader    dl       = new FileDownloader(sessionManager.getActiveConfig(), historyService);

            // Get all weeks
            List<WeeklySection> allWeeks = wService.getWeeklySections(courseId, shortName);

            int downloaded = 0, updated = 0, resumed = 0, skipped = 0, failed = 0;

            for (WeeklySection week : allWeeks) {

                // Skip unselected
                if (!requested.contains(week.getSectionNumber())) continue;

                // Loop files
                for (CourseFile file : week.getFiles()) {
                    try {
                        switch (dl.download(file)) {
                            case DOWNLOADED -> downloaded++;
                            case UPDATED    -> updated++;
                            case RESUMED    -> resumed++;
                            case SKIPPED    -> skipped++;
                            case FAILED     -> failed++;
                        }
                    } catch (Exception e) {
                        failed++;
                        System.out.println("fail " + file.getFileName() + " " + e.getMessage());
                    }
                }
            }

            // result map
            return ResponseEntity.ok(Map.of(
                    "downloaded", downloaded,
                    "updated",    updated,
                    "resumed",    resumed,
                    "skipped",    skipped,
                    "failed",     failed
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}