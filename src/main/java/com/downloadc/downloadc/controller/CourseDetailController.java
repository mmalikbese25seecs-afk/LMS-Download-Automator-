package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.api.WeeklyFileService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.MoodleConfig;
import com.downloadc.downloadc.model.WeeklySection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// Endpoint to fetch detailed course information for the course detail page
@RestController
@RequestMapping("/api/courses")
public class CourseDetailController {

    @Autowired
    private SessionManager sessionManager;

    // Get detailed course info with weeks and files
    @GetMapping("/{courseId}/detail")
    public ResponseEntity<?> getCourseDetail(
            @PathVariable int courseId,
            @RequestParam String shortName) {

        // Check login
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));
        }

        try {
            MoodleConfig config = sessionManager.getActiveConfig();
            MoodleApiClient apiClient = new MoodleApiClient(config);
            WeeklyFileService service = new WeeklyFileService(apiClient);

            // Fetch weekly sections with files
            List<WeeklySection> weeks = service.getWeeklySections(courseId, shortName);

            // Calculate statistics
            int totalResources = weeks.stream()
                    .mapToInt(w -> w.getFiles().size())
                    .sum();
            int totalAssignments = (int) weeks.stream()
                    .flatMap(w -> w.getFiles().stream())
                    .filter(f -> f.getFileType().toLowerCase().contains("assignment"))
                    .count();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("courseId", courseId);
            response.put("shortName", shortName);
            response.put("weeks", weeks);
            response.put("totalFiles", totalResources);
            response.put("totalAssignments", totalAssignments);
            response.put("completedCount", 0);
            response.put("totalCount", totalResources);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[CourseDetailController] Error: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}