package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.CalendarEvent;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarService {

    private final MoodleApiClient apiClient;
    private final MoodleConfig    config;

    private static final long DAY = 86_400L;

    // Formatter uses config timezone — defaults to Asia/Karachi if invalid
    private final DateTimeFormatter formatter;

    public CalendarService(MoodleApiClient apiClient, MoodleConfig config) {
        this.apiClient = apiClient;
        this.config    = config;

        // Fall back to Asia/Karachi if timezone in config is invalid
        ZoneId zone;
        try {
            zone = ZoneId.of(config.getTimezone());
        } catch (Exception e) {
            System.out.println("[CalendarService] Invalid timezone, using Asia/Karachi");
            zone = ZoneId.of("Asia/Karachi");
        }

        this.formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(zone);
    }

    public List<CalendarEvent> getUpcomingEvents(int days) throws Exception {

        // Clamp days between 1 and 365
        int clampedDays = Math.max(1, Math.min(days, 365));
        long now        = Instant.now().getEpochSecond();
        long future     = now + (clampedDays * DAY);
        int limit       = 50;

        System.out.println("[CalendarService] now="    + now);
        System.out.println("[CalendarService] future=" + future);
        System.out.println("[CalendarService] limit="  + limit);
        System.out.println("[CalendarService] days="   + clampedDays);

        JsonNode response = apiClient.callFunction(
                "core_calendar_get_action_events_by_timesort",
                "timesortfrom=" + now
                        + "&timesortto=" + future
                        + "&limitnum="   + limit
        );

        System.out.println("[CalendarService] Raw response: " + response);

        List<CalendarEvent> events = new ArrayList<>();

        if (response == null) {
            System.out.println("[CalendarService] Response is null");
            return events;
        }

        // Moodle returns an exception object when token is expired or function is disabled
        if (response.has("exception")) {
            String msg = response.has("message")
                    ? response.get("message").asText()
                    : response.toString();
            throw new Exception("Moodle error: " + msg);
        }

        if (!response.has("events")) {
            System.out.println("[CalendarService] No 'events' key in response");
            System.out.println("[CalendarService] Response keys: " + response.fieldNames());
            return events;
        }

        JsonNode eventsArray = response.get("events");
        System.out.println("[CalendarService] Events array size: " + eventsArray.size());

        for (JsonNode e : eventsArray) {

            String id        = e.has("id")       ? e.get("id").asText()        : null;
            String title     = e.has("name")      ? e.get("name").asText()      : "Untitled";
            long   timeStart = e.has("timestart") ? e.get("timestart").asLong() : now;
            long   due       = timeStart - now;

            System.out.println("[CalendarService] Event: " + title
                    + " | type=" + (e.has("modulename") ? e.get("modulename").asText() : "?")
                    + " | due=" + due + "s"
                    + " | timeStart=" + timeStart);

            // Use modulename as event type, fall back to "event"
            String type = e.has("modulename")
                    ? e.get("modulename").asText().toLowerCase()
                    : "event";

            // Extract course name if present
            String course = "";
            if (e.has("course") && !e.get("course").isNull()
                    && e.get("course").has("fullname")) {
                course = e.get("course").get("fullname").asText();
            }

            // Use action name as description, fall back to type
            String desc = e.has("action") && !e.get("action").isNull()
                    && e.get("action").has("name")
                    ? e.get("action").get("name").asText()
                    : type;

            String iso   = formatter.format(Instant.ofEpochSecond(timeStart));
            String color = resolveColor(type, due);

            events.add(new CalendarEvent(
                    id, title, iso, color, course, type, desc, due
            ));
        }

        System.out.println("[CalendarService] Returning " + events.size() + " events");
        return events;
    }

    // Color by type and urgency — red if due soon, purple for quizzes
    private String resolveColor(String type, long due) {
        if ("quiz".equals(type))   return "#a78bfa";
        if (due <= 3 * DAY)        return "#e74c3c";
        if (due <= 7 * DAY)        return "#e67e22";
        if ("assign".equals(type)) return "#3b82f6";
        return "#2dd4bf";
    }
}