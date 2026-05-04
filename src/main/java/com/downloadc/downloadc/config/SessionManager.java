package com.downloadc.downloadc.config;
<<<<<<< HEAD
 
import com.downloadc.downloadc.model.MoodleConfig;
import org.springframework.stereotype.Component;
 
// Holds the active MoodleConfig after a successful login.
// FIX 5: activeConfig declared volatile so the scheduler thread and servlet
// threads always see a consistent value without needing a full synchronized block.
// Also added synchronized to setActiveConfig / clearSession so the write +
// read pair is atomic (volatile alone only guarantees visibility of individual
// reads/writes, not compound check-then-act operations).
@Component
public class SessionManager {
 
    // volatile: any thread that reads activeConfig sees the latest write immediately
    private volatile MoodleConfig activeConfig = null;
 
    public synchronized void setActiveConfig(MoodleConfig config) {
        this.activeConfig = config;
    }
 
    public MoodleConfig getActiveConfig() {
        return activeConfig; // volatile read is safe without lock
    }
 
    public boolean isLoggedIn() {
        MoodleConfig snap = activeConfig; // read once to avoid TOCTOU
        return snap != null && snap.getToken() != null;
    }
 
    public synchronized void clearSession() {
        this.activeConfig = null;
    }
}
 
=======

import com.downloadc.downloadc.model.MoodleConfig;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    // BUG FIX: original field had no synchronization.
    // The @Scheduled auto-sync runs on a background thread while HTTP requests
    // run on servlet threads — both read/write activeConfig concurrently.
    // volatile guarantees every thread sees the latest write immediately.
    // setActiveConfig and clearSession are synchronized so the write is atomic.
    private volatile MoodleConfig activeConfig = null;

    public synchronized void setActiveConfig(MoodleConfig config) {
        this.activeConfig = config;
    }

    public MoodleConfig getActiveConfig() {
        return activeConfig; // volatile read — safe without lock
    }

    public boolean isLoggedIn() {
        MoodleConfig snap = activeConfig; // read once to avoid race
        return snap != null && snap.getToken() != null;
    }

    public synchronized void clearSession() {
        this.activeConfig = null;
    }
}
>>>>>>> 1906425 (jk)
