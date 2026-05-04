package com.downloadc.downloadc.model;

public class CalendarEvent {

    private final String id;
    private final String title;
    private final String start;
    private final String color;
    private final String courseName;
    private final String eventType;
    private final String description;
    private final long   timeUntilDue;

    public CalendarEvent(String id, String title, String start, String color,
                         String courseName, String eventType,
                         String description, long timeUntilDue) {

        this.id = (id == null || id.isBlank())
                ? String.valueOf(System.nanoTime())
                : id;
        this.title        = (title == null || title.isBlank()) ? "Event" : title;
        this.start        = start;
        this.color        = color;
        this.courseName   = courseName;
        this.eventType    = eventType;
        this.description  = description;
        this.timeUntilDue = timeUntilDue;
    }

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public String getStart()       { return start; }
    public String getColor()       { return color; }
    public String getCourseName()  { return courseName; }
    public String getEventType()   { return eventType; }
    public String getDescription() { return description; }
    public long   getTimeUntilDue(){ return timeUntilDue; }
}