package com.downloadc.downloadc.api;

import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// handles auto sync stuff, runs on a schedule
// modes: manual (default), daily, weekly
// driveAutoSync flag is saved but actual Drive upload must be
// triggered manually by the user from the frontend (requires user session)

@Service
public class SchedulerService {

    private static final Path SCHEDULE_FILE = Paths.get("downloads", "schedule.json");
    private static final DateTimeFormatter LOG_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired private SessionManager sessionManager;
    @Autowired private DownloadHistoryService historyService;
    @Autowired private ChangeDetectionService changeService;

    // Drive upload requires a user session which the scheduler doesn't have.
    // After sync completes, user can manually trigger Drive upload from frontend.

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // config class, saved to schedule.json
    public static class ScheduleConfig {
        public String  mode          = "manual";  // manual | daily | weekly
        public int     hourOfDay     = 2;          // what hour to run (0-23)
        public boolean driveAutoSync = false;      // reminder flag for frontend
        public String  lastRunAt     = "";         // when did it last run
        public String  lastRunResult = "";         // what happened last time
    }

    // runs every hour, checks if it's time to sync
    @Scheduled(cron = "0 0 * * * *")
    public void hourlyCheck() {
        ScheduleConfig cfg = loadConfig();

        if ("manual".equals(cfg.mode)) return;
        if (!sessionManager.isLoggedIn()) {
            System.out.println("[Scheduler] Skipping — no active session.");
            return;
        }

        int currentHour = LocalDateTime.now().getHour();
        int currentDay  = LocalDateTime.now().getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        // check if we should run based on mode
        boolean shouldRun = switch (cfg.mode) {
            case "daily"  -> currentHour == cfg.hourOfDay;
            case "weekly" -> currentDay == 7 && currentHour == cfg.hourOfDay; // Sunday only
            default       -> false;
        };

        if (shouldRun) {
            System.out.println("[Scheduler] Triggered auto-sync (" + cfg.mode + ")");
            runSync(cfg);
        }
    }

    // manually trigger sync right now (called from controller)
    public SyncResult triggerNow() {
        if (!sessionManager.isLoggedIn())
            return new SyncResult(false, "No active session. Please log in first.", 0, 0, 0, 0);

        ScheduleConfig cfg = loadConfig();
        return runSync(cfg);
    }

    // actual sync logic, downloads everything
    private SyncResult runSync(ScheduleConfig cfg) {
        String startTime = LocalDateTime.now().format(LOG_FMT);
        System.out.println("[Scheduler] Sync started at " + startTime);

        int downloaded = 0, updated = 0, skipped = 0, failed = 0;

        try {
            MoodleConfig    config    = sessionManager.getActiveConfig();
            MoodleApiClient apiClient = new MoodleApiClient(config);
            CourseService   cs        = new CourseService(apiClient, config);
            FileService     fs        = new FileService(apiClient);
            FileDownloader  dl        = new FileDownloader(config, historyService);

            List<Course> courses = cs.getEnrolledCourses();

            for (Course course : courses) {
                List<CourseFile> files = fs.getFilesForCourse(course);
                for (CourseFile file : files) {
                    try {
                        switch (dl.download(file)) {
                            case DOWNLOADED -> downloaded++;
                            case UPDATED    -> updated++;
                            case RESUMED    -> downloaded++;
                            case SKIPPED    -> skipped++;
                            case FAILED     -> failed++;
                        }
                    } catch (Exception e) {
                        failed++;
                        System.err.println("[Scheduler] Failed: "
                                + file.getFileName() + " — " + e.getMessage());
                    }
                }

                // Drive auto-sync skipped here — no user session available
                // User can upload to Drive manually from the frontend after sync
                if (cfg.driveAutoSync && (downloaded > 0 || updated > 0)) {
                    System.out.println("[Scheduler] Drive auto-sync for "
                            + course.getShortName()
                            + " — please trigger manually from dashboard.");
                }
            }

            // save snapshot for change detection next time
            try { changeService.takeSnapshot(config); } catch (Exception ignored) {}

            String result = String.format(
                    "Sync at %s: %d new, %d updated, %d skipped, %d failed",
                    startTime, downloaded, updated, skipped, failed);

            cfg.lastRunAt     = startTime;
            cfg.lastRunResult = result;
            saveConfig(cfg);

            System.out.println("[Scheduler] " + result);
            return new SyncResult(true, result, downloaded, updated, skipped, failed);

        } catch (Exception e) {
            String err = "Sync failed: " + e.getMessage();
            cfg.lastRunAt     = startTime;
            cfg.lastRunResult = err;
            try { saveConfig(cfg); } catch (IOException ignored) {}
            System.err.println("[Scheduler] " + err);
            return new SyncResult(false, err, 0, 0, 0, 0);
        }
    }

    // load config from JSON file, return default if not found
    public ScheduleConfig loadConfig() {
        if (!Files.exists(SCHEDULE_FILE)) return new ScheduleConfig();
        try {
            return objectMapper.readValue(SCHEDULE_FILE.toFile(), ScheduleConfig.class);
        } catch (IOException e) {
            return new ScheduleConfig();
        }
    }

    // save config to JSON
    public void saveConfig(ScheduleConfig cfg) throws IOException {
        Files.createDirectories(SCHEDULE_FILE.getParent());
        objectMapper.writeValue(SCHEDULE_FILE.toFile(), cfg);
    }

    // result of a sync run
    public record SyncResult(
            boolean success,
            String  message,
            int     downloaded,
            int     updated,
            int     skipped,
            int     failed
    ) {}
}