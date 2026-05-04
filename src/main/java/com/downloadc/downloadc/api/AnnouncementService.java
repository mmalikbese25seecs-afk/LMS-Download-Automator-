package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Announcement;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Grabs Announcements from the LMS so we don't have to open Moodle every time
public class AnnouncementService {

    private final MoodleApiClient apiClient;

    // Pakistan Standard Time formatting, dd MMM yyyy hh:mm am/pm
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                    .withZone(ZoneId.of("Asia/Karachi"));

    public AnnouncementService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // fetches recent notifications for the user, not chat messages
    // sorted newest first, max 50 from NUST LMS
    public List<Announcement> getRecentAnnouncements(int userId, int limit) throws Exception {

        System.out.println("[AnnouncementService] Fetching announcements for userId = " + userId);

        // calling core_message_get_messages: type=notifications skips private chats
        // read=0 means unread only, limitnum capped at 50 by the server
        JsonNode response = apiClient.callFunction(
                "core_message_get_messages",
                "useridto=" + userId
                        + "&type=notifications"
                        + "&read=0"
                        + "&limitfrom=0"
                        + "&limitnum=" + Math.min(limit, 50)
        );

        List<Announcement> announcements = new ArrayList<>();

        if (response == null || !response.has("messages")) {
            System.out.println("[AnnouncementService] No messages key in response");
            return announcements;
        }

        JsonNode messages = response.get("messages");
        System.out.println("[AnnouncementService] Received " + messages.size() + " messages");

        for (JsonNode msg : messages) {

            String id       = msg.has("id")          ? msg.get("id").asText()          : "0";
            String subject  = msg.has("subject")      ? msg.get("subject").asText()      : "No subject";
            String text     = msg.has("smallmessage") ? msg.get("smallmessage").asText() : "";
            long   timeSent = msg.has("timecreated")  ? msg.get("timecreated").asLong()  : 0;

            // try to get sender name, Moodle puts it in different fields sometimes
            String sender = "";
            if (msg.has("userfromfullname")) {
                sender = msg.get("userfromfullname").asText();
            } else if (msg.has("fullname")) {
                sender = msg.get("fullname").asText();
            }

            // convert Unix timestamp to readable time
            String formattedTime = timeSent > 0
                    ? FORMATTER.format(Instant.ofEpochSecond(timeSent))
                    : "Unknown time";

            // pick icon based on what the subject says
            String icon = resolveIcon(subject);

            announcements.add(new Announcement(
                    id, subject, text, sender, formattedTime, icon, timeSent
            ));
        }

        // newest first
        announcements.sort((a, b) -> Long.compare(b.getTimeSent(), a.getTimeSent()));

        System.out.println("[AnnouncementService] Returning "
                + announcements.size() + " announcements");

        return announcements;
    }

    // matches keywords in subject to return a relevant emoji, makes list easier to scan
    private String resolveIcon(String subject) {
        String lower = subject.toLowerCase();
        if (lower.contains("quiz") || lower.contains("test"))      return "📋";
        if (lower.contains("assignment") || lower.contains("task")) return "📝";
        if (lower.contains("exam") || lower.contains("final"))     return "📚";
        if (lower.contains("lab"))                                  return "🔬";
        if (lower.contains("cancel") || lower.contains("holiday")) return "🚫";
        if (lower.contains("result") || lower.contains("grade"))   return "🎯";
        if (lower.contains("deadline") || lower.contains("due"))   return "⏰";
        return "📢";
    }
}
