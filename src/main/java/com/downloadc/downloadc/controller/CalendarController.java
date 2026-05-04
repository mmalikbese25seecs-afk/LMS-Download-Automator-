package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.CalendarService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.CalendarEvent;
import com.downloadc.downloadc.model.MoodleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    @Autowired
    private SessionManager sessionManager;

    // CalendarService is built per-request — it needs the user's token from session
    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(defaultValue = "180") int days) {

        // Reject if no active session
        if (!sessionManager.isLoggedIn()) {
            System.out.println("[CalendarController] Rejected — no active session");
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));
        }

        try {
            MoodleConfig    config    = sessionManager.getActiveConfig();
            MoodleApiClient apiClient = new MoodleApiClient(config);

            // Pass config so CalendarService can read timezone
            CalendarService service = new CalendarService(apiClient, config);

            System.out.println("[CalendarController] Fetching events, days=" + days
                    + ", userId=" + config.getUserId());

            List<CalendarEvent> events = service.getUpcomingEvents(days);

            System.out.println("[CalendarController] Returning " + events.size() + " events");
            return ResponseEntity.ok(events);

        } catch (Exception e) {
            System.out.println("[CalendarController] Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}