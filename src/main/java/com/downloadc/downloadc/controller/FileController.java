package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.api.FileService;
import com.downloadc.downloadc.api.GoogleDriveService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.DownloadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
import java.util.List;
import java.util.Map;
=======
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
>>>>>>> 1906425 (jk)

@RestController
@RequestMapping("/api")
public class FileController {

<<<<<<< HEAD
    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private DownloadHistoryService historyService;

    @Autowired
    private GoogleDriveService driveService;

    // Get all files for a course.
    // FIX 4: the old code built Course with shortName = String.valueOf(courseId),
    // a bare number like "12345". FileDownloader uses shortName as the folder name,
    // so it would save into downloads/12345/ while the download endpoint uses the
    // real shortName (e.g. "CS101"). Files could never be found after downloading.
    // The listing endpoint now requires the caller to pass shortName as a query param
    // (the frontend already has it — it's in every course card).
=======
    @Autowired private SessionManager         sessionManager;
    @Autowired private DownloadHistoryService  historyService;
    @Autowired private GoogleDriveService      driveService;

    // ── GET /api/courses/{courseId}/files?shortName=CS101 ─────────────────────
    // BUG FIX: original endpoint signature was getFilesForCourse(@PathVariable int courseId)
    // with no shortName parameter.  It built Course with shortName = String.valueOf(courseId),
    // e.g. "98765".  FileService uses shortName as the folder name, so the file list
    // showed files mapped to downloads/98765/ while all real files lived in
    // downloads/CS101/. The panel always appeared empty on the dashboard.
    // FIX: accept shortName as a query param and pass it into the Course object.
>>>>>>> 1906425 (jk)
    @GetMapping("/courses/{courseId}/files")
    public ResponseEntity<?> getFilesForCourse(
            @PathVariable int courseId,
            @RequestParam(defaultValue = "") String shortName) {

<<<<<<< HEAD
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in."));
        }

        try {
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService = new FileService(apiClient);

            // Use real shortName so the folder path matches what FileDownloader will use.
            // Fall back to the courseId string only if the caller genuinely didn't send one.
            String effectiveShortName = (shortName == null || shortName.isBlank())
                    ? String.valueOf(courseId)
                    : shortName;

            Course course = new Course(courseId, "", effectiveShortName, "");
            return ResponseEntity.ok(fileService.getFilesForCourse(course));
=======
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        try {
            MoodleApiClient apiClient   = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService     fileService = new FileService(apiClient);

            // fall back to courseId string only if caller genuinely didn't send shortName
            String sn = (shortName == null || shortName.isBlank())
                    ? String.valueOf(courseId) : shortName;

            return ResponseEntity.ok(
                    fileService.getFilesForCourse(new Course(courseId, "", sn, "")));
>>>>>>> 1906425 (jk)

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

<<<<<<< HEAD
    // Download all files of a course.
    // FIX 2: Drive upload was called inside the per-file loop, so for a course
    // with N files the entire folder was re-uploaded N times. Moved the Drive
    // upload to a single call after all files are downloaded.
    @PostMapping("/download/{courseId}")
    public ResponseEntity<?> downloadCourse(
            @PathVariable int courseId,
            @RequestBody Map<String, String> body) {
=======
    // ── POST /api/download/{courseId} ─────────────────────────────────────────
    // BUG FIX 1 (Drive re-upload N times): original code called
    //   driveService.uploadCourse(sessionId, shortName)
    // inside the per-file for-loop.  For a course with 20 files that triggered
    // 20 full Drive re-uploads of the same folder.
    // FIX: move the Drive upload to a single call AFTER the download loop.
    //
    // BUG FIX 2 (selective download ignored): original body was Map<String,String>
    // so Jackson couldn't deserialise a List<String> fileNames field — it was
    // silently dropped and every "Download Selected" click downloaded everything.
    // FIX: change body type to Map<String,Object> and read the optional fileNames list.
    @PostMapping("/download/{courseId}")
    public ResponseEntity<?> downloadCourse(
            @PathVariable int courseId,
            @RequestBody Map<String, Object> body) {   // was Map<String,String> — can't hold List
>>>>>>> 1906425 (jk)

        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

<<<<<<< HEAD
        String saveOption  = body.getOrDefault("saveOption", "device").toLowerCase();
        boolean uploadToDrive = saveOption.equals("drive") || saveOption.equals("both");

        String sessionId = "default";
        if (uploadToDrive && !driveService.isAuthorized(sessionId)) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Google Drive not connected. Click 'Connect Drive' first."));
        }

        try {
            MoodleApiClient apiClient  = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService     fileService = new FileService(apiClient);
            FileDownloader  downloader  = new FileDownloader(sessionManager.getActiveConfig(), historyService);

            String shortName = body.getOrDefault("shortName", String.valueOf(courseId));
            Course course    = new Course(courseId, "", shortName, "");

            List<CourseFile> files = fileService.getFilesForCourse(course);

            int downloaded = 0, updated = 0, resumed = 0, skipped = 0, failed = 0;

            // ── download loop (no Drive calls here) ─────────────────────────
            for (CourseFile file : files) {
                try {
                    DownloadStatus result = downloader.download(file);
                    switch (result) {
=======
        String  saveOption    = body.getOrDefault("saveOption", "device").toString().toLowerCase();
        boolean uploadToDrive = saveOption.equals("drive") || saveOption.equals("both");
        String  sessionId     = "default";

        if (uploadToDrive && !driveService.isAuthorized(sessionId))
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Google Drive not connected. Click 'Connect Drive' first."));

        try {
            MoodleApiClient  apiClient  = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService      fileSvc    = new FileService(apiClient);
            FileDownloader   downloader = new FileDownloader(sessionManager.getActiveConfig(), historyService);

            String shortName = body.getOrDefault("shortName", String.valueOf(courseId)).toString();
            List<CourseFile> allFiles   = fileSvc.getFilesForCourse(new Course(courseId, "", shortName, ""));

            // BUG FIX 2: read optional fileNames list for selective download
            @SuppressWarnings("unchecked")
            List<String> requested = (List<String>) body.get("fileNames");
            List<CourseFile> files;
            if (requested != null && !requested.isEmpty()) {
                Set<String> wanted = new HashSet<>(requested);
                files = allFiles.stream()
                        .filter(f -> wanted.contains(f.getFileName()))
                        .toList();
                System.out.println("[FileController] Selective: " + files.size()
                        + " of " + allFiles.size());
            } else {
                files = allFiles;
            }

            int downloaded = 0, updated = 0, resumed = 0, skipped = 0, failed = 0;

            // ── download loop — NO Drive calls inside here ────────────────────
            for (CourseFile file : files) {
                try {
                    switch (downloader.download(file)) {
>>>>>>> 1906425 (jk)
                        case DOWNLOADED -> downloaded++;
                        case UPDATED    -> updated++;
                        case RESUMED    -> resumed++;
                        case SKIPPED    -> skipped++;
                        case FAILED     -> failed++;
                    }
                } catch (Exception e) {
                    failed++;
<<<<<<< HEAD
                    System.out.println("Failed: " + file.getFileName() + " — " + e.getMessage());
                }
            }

            // ── FIX 2: single Drive upload AFTER all files are downloaded ────
            int driveUploaded = 0, driveFailed = 0;
            if (uploadToDrive) {
                try {
                    GoogleDriveService.UploadResult uploadResult =
                            driveService.uploadCourse(sessionId, shortName);
                    driveUploaded = uploadResult.uploaded();
                    driveFailed   = uploadResult.failed();
                } catch (Exception e) {
                    System.out.println("Drive upload failed: " + e.getMessage());
=======
                    System.out.println("[FileController] Failed: "
                            + file.getFileName() + " — " + e.getMessage());
                }
            }

            // BUG FIX 1: single Drive upload after ALL files are done
            int driveUploaded = 0, driveFailed = 0;
            if (uploadToDrive) {
                try {
                    GoogleDriveService.UploadResult r =
                            driveService.uploadCourse(sessionId, shortName);
                    driveUploaded = r.uploaded();
                    driveFailed   = r.failed();
                } catch (Exception e) {
                    System.out.println("[FileController] Drive failed: " + e.getMessage());
>>>>>>> 1906425 (jk)
                    driveFailed = files.size();
                }
            }

<<<<<<< HEAD
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("courseId",    courseId);
            response.put("totalFiles",  files.size());
            response.put("downloaded",  downloaded);
            response.put("updated",     updated);
            response.put("resumed",     resumed);
            response.put("skipped",     skipped);
            response.put("failed",      failed);
            response.put("saveOption",  saveOption);

            if (uploadToDrive) {
                response.put("driveUploaded", driveUploaded);
                response.put("driveFailed",   driveFailed);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();
    }
}
=======
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("courseId",   courseId);
            resp.put("totalFiles", files.size());
            resp.put("downloaded", downloaded);
            resp.put("updated",    updated);
            resp.put("resumed",    resumed);
            resp.put("skipped",    skipped);
            resp.put("failed",     failed);
            resp.put("saveOption", saveOption);
            if (uploadToDrive) {
                resp.put("driveUploaded", driveUploaded);
                resp.put("driveFailed",   driveFailed);
            }
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            // BUG FIX: original had "Error" (capital E) as the key — frontend
            // checked for "error" (lowercase) so it never showed the message.
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
>>>>>>> 1906425 (jk)
