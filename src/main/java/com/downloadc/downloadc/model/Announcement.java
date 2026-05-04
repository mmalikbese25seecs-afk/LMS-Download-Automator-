package com.downloadc.downloadc.model;

// Unchangeable snapshot of a single Moodle notification shown on the dashboard.
public class Announcement {

    private final String id;
    private final String subject;
    private final String text;          // Short preview pulled from small message field.
    private final String senderName;
    private final String formattedTime;
    private final String icon;          // Emoji chosen by resolveIcon() based on subject keywords.
    private final long   timeSent;      // Unix timestamp kept as a long so we can sort without parsing.

    public Announcement(String id, String subject, String text,
                        String senderName, String formattedTime,
                        String icon, long timeSent) {
        this.id            = id;
        this.subject       = subject;
        this.text          = text;
        this.senderName    = senderName;
        this.formattedTime = formattedTime;
        this.icon          = icon;
        this.timeSent      = timeSent;
    }

    public String getId()            { return id; }
    public String getSubject()       { return subject; }
    public String getText()          { return text; }
    public String getSenderName()    { return senderName; }
    public String getFormattedTime() { return formattedTime; }
    public String getIcon()          { return icon; }
    public long   getTimeSent()      { return timeSent; }
}