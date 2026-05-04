package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.GoogleDriveService;
import com.downloadc.downloadc.config.SessionManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Google Drive endpoints for multi-user web OAuth flow
//
// Flow:
//   1. Frontend calls GET /api/drive/auth-url
//   2. Gets back a Google login URL
//   3. Frontend redirects user to that URL
//   4. User logs in + approves
//   5. Google redirects to GET /api/drive/callback?code=xxx&state=sessionId
//   6. Backend saves token for that session
//   7. Drive is now connected for that user ✅

@RestController
@RequestMapping("/api/drive")
public class DriveController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private GoogleDriveService driveService;

    // returns current drive connection status for this session
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        return ResponseEntity.ok(Map.of(
                "configured", driveService.isConfigured(),
                "authorized", driveService.isAuthorized(session.getId())
        ));
    }

    // returns the Google OAuth URL the frontend should redirect the user to
    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl(HttpSession session) {
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        if (!driveService.isConfigured())
            return ResponseEntity.status(400).body(Map.of(
                    "error", "credentials.json not found. " +
                            "Download it from Google Cloud Console and place it in " +
                            "src/main/resources/credentials.json"));
        try {
            String url = driveService.getAuthUrl(session.getId());
            return ResponseEntity.ok(Map.of("authUrl", url));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Google redirects here after user approves — exchanges code for token
    // then redirects user back to the frontend dashboard
    @GetMapping("/callback")
    public void callback(
            @RequestParam String code,
            @RequestParam String state,      // state = sessionId we passed in auth-url
            HttpServletResponse response) throws Exception {

        try {
            driveService.handleCallback(code, state);
            System.out.println("[DriveController] OAuth callback success for session: " + state);
            // redirect back to frontend after successful auth
            response.sendRedirect("/?driveConnected=true");
        } catch (Exception e) {
            System.err.println("[DriveController] Callback failed: " + e.getMessage());
            response.sendRedirect("/?driveError=true");
        }
    }

    // disconnects Drive for this session (clears token)
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect(HttpSession session) {
        driveService.disconnect(session.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Google Drive disconnected."
        ));
    }

    // uploads all locally downloaded files for a course to this user's Drive
    @PostMapping("/upload/{courseName}")
    public ResponseEntity<Map<String, Object>> uploadCourse(
            @PathVariable String courseName,
            HttpSession session) {

        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        if (!driveService.isAuthorized(session.getId()))
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Google Drive not connected. Call /api/drive/auth-url first."));

        try {
            GoogleDriveService.UploadResult result =
                    driveService.uploadCourse(session.getId(), courseName);

            String message = String.format(
                    "Uploaded %d file(s) to Drive. Skipped %d (already there). Failed %d.",
                    result.uploaded(), result.skipped(), result.failed());

            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "message",    message,
                    "uploaded",   result.uploaded(),
                    "skipped",    result.skipped(),
                    "failed",     result.failed(),
                    "courseName", result.courseName()
            ));

        } catch (Exception e) {
            System.err.println("[DriveController] Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // lists files already uploaded to Drive for a course
    @GetMapping("/files/{courseName}")
    public ResponseEntity<?> listFiles(
            @PathVariable String courseName,
            HttpSession session) {

        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        if (!driveService.isAuthorized(session.getId()))
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Google Drive not connected."));

        try {
            return ResponseEntity.ok(driveService.listCourseFiles(session.getId(), courseName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}