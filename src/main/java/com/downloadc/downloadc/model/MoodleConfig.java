package com.downloadc.downloadc.model;

// Holds LMS username, password, and the token we get

public class MoodleConfig {

    // Fields are private to protect the data (Encapsulation)
    // We'll get token after login
    private String username;
    private String password;
    private String token;
    private int userId;
    private String timezone;

    // The URL of the LMS that is same for every student
    private static final String BASE_URL = "https://lms.nust.edu.pk/portal";

    // Constructor
    // Token will be there after the LogIN
    public MoodleConfig(String username, String password) {
        this.username = username;
        this.password = password;
        this.token = null;
    }

    //GETTERS
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getToken()    { return token; }
    public String getBaseUrl()  { return BASE_URL; }
    public int getUserId() { return userId; }
    public String getTimezone() { return timezone; }
    
    //SETTER
    public void setToken(String token) {
        this.token = token;
    }
    public void setUserId(int userId) { this.userId = userId; }

    public void setTimezone(String timezone) { 
        this.timezone = timezone; 
    }
    // Prints the object for debugging
    @Override
    public String toString() {
        return "MoodleConfig\nUser=" + username + "\nToken= " +
                (token != null ? "Received" : "Not received");
    }
}
