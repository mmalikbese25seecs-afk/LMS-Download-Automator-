package com.downloadc.downloadc.model;

// Any object that can be downloaded from the LMS must implement this.
// Demonstrates: interface-based abstraction
public interface Downloadable {

    String getFileUrl();
    String getFileName();
    long   getFileSize();
    String getCourseName();
    public long   getMoodleTimestamp();
    public String getFileType();

    // Default method (Java 8+): concrete behaviour in an interface
    // Shows your professor you know the full interface feature set
    default String getAuthenticatedUrl(String token) {
        String url = getFileUrl();
        return url.contains("?")
                ? url + "&token=" + token
                : url + "?token=" + token;
    }

    default boolean isEmpty() {
        return getFileSize() == 0;
    }
}