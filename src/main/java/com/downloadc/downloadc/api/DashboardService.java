package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Announcement;
import com.downloadc.downloadc.model.CalendarEvent;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// Aggregates user info, courses, events, and announcements into one response so the frontend makes a single HTTP call.
public class DashboardService {

    private final MoodleApiClient apiClient;
    private final MoodleConfig    config;

    public DashboardService(MoodleApiClient apiClient, MoodleConfig config) {
        this.apiClient = apiClient;
        this.config    = config;
    }

    // Runs four Moodle API calls in sequence and packs all results into a DashboardSummary.
    public DashboardSummary getSummary() throws Exception {

        // Call 1: grab the user's display name and the site name for the navbar.
        JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");
        String fullName   = siteInfo.has("fullname")
                ? siteInfo.get("fullname").asText() : "Unknown";
        String siteName   = siteInfo.has("sitename")
                ? siteInfo.get("sitename").asText() : "NUST LMS";

        // Call 2: fetch every course the user is enrolled in.
        CourseService courseService = new CourseService(apiClient, config);
        List<Course>  courses       = courseService.getEnrolledCourses();

        // Call 3: pull calendar events for the next 14 days.
        CalendarService calendarService = new CalendarService(apiClient, config);
        List<CalendarEvent> allEvents   = calendarService.getUpcomingEvents(14);

        // Show only the five nearest deadlines in the sidebar preview panel.
        List<CalendarEvent> upcomingFive = allEvents.stream()
                .filter(e -> e.getTimeUntilDue() > 0)
                .sorted((a, b) -> Long.compare(
                        a.getTimeUntilDue(), b.getTimeUntilDue()))
                .limit(5)
                .toList();

        // Count events within the next 72 hours to drive the urgent-deadline alert badge.
        long urgentCount = allEvents.stream()
                .filter(e -> e.getTimeUntilDue() > 0
                        && e.getTimeUntilDue() <= 3 * 86_400L)
                .count();

        // Call 4: fetch announcements — wrapped in its own try/catch so a failure here doesn't kill the whole dashboard.
        AnnouncementService announcementService =
                new AnnouncementService(apiClient);

        List<Announcement> announcements = List.of();
        try {
            announcements = announcementService
                    .getRecentAnnouncements(config.getUserId(), 5);
        } catch (Exception e) {
            // Some Moodle instances restrict this endpoint — log and continue with an empty list.
            System.out.println("[DashboardService] Announcements unavailable: "
                    + e.getMessage());
        }

        System.out.printf("[DashboardService] Summary built: %d courses, " +
                        "%d events, %d urgent, %d announcements%n",
                courses.size(), upcomingFive.size(),
                urgentCount, announcements.size());

        return new DashboardSummary(
                fullName, siteName,
                courses,
                upcomingFive,
                urgentCount,
                announcements
        );
    }

    // Unchangeable data container serialized directly to JSON by Jackson and consumed by the frontend.
    public record DashboardSummary(
            String              fullName,
            String              siteName,
            List<Course>        courses,
            List<CalendarEvent> upcomingEvents,
            long                urgentCount,
            List<Announcement>  announcements
    ) {}
}