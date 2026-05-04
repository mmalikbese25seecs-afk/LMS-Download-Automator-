package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.AuthService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
<<<<<<< HEAD
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Username and password are required.")
            );
=======
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username and password are required."));
>>>>>>> 1906425 (jk)
        }

        try {
            MoodleConfig config = new MoodleConfig(username, password);

            AuthService authService = new AuthService();
            String token = authService.getToken(config);

<<<<<<< HEAD
            // FIX 1: getToken() returns null on bad credentials — catch it here
            // before we try to use it, otherwise the next call throws a confusing
            // NullPointerException instead of a clean "invalid credentials" message.
            if (token == null) {
                return ResponseEntity.status(401).body(
                        Map.of("error", "Invalid username or password. Please try again.")
                );
=======
            // BUG FIX: original code called config.setToken(null) then immediately
            // called apiClient.callFunction() which checks for null token and throws
            // "Not logged in!" — that exception message doesn't contain "invalid/wrong/token"
            // so the catch block fell through to the generic message instead of
            // "Invalid username or password".
            // FIX: guard here so we return 401 cleanly before touching apiClient.
            if (token == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid username or password. Please try again."));
>>>>>>> 1906425 (jk)
            }

            config.setToken(token);

            MoodleApiClient apiClient = new MoodleApiClient(config);
            JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");

<<<<<<< HEAD
            // FIX 1b: also guard against a missing userid/fullname in the response
            if (siteInfo == null || !siteInfo.has("userid")) {
                return ResponseEntity.status(401).body(
                        Map.of("error", "Login succeeded but could not read user info from LMS.")
                );
=======
            // BUG FIX: original code called siteInfo.get("userid").asInt() without
            // checking if siteInfo is null or if the field exists — would throw
            // NullPointerException if Moodle returned an error node.
            if (siteInfo == null || !siteInfo.has("userid")) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Login succeeded but could not read user info from LMS."));
>>>>>>> 1906425 (jk)
            }

            config.setUserId(siteInfo.get("userid").asInt());
            String fullName = siteInfo.path("fullname").asText("Student");

            sessionManager.setActiveConfig(config);
<<<<<<< HEAD

            System.out.println("[AuthController] Login successful for: " + fullName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fullName", fullName);
            response.put("userId", config.getUserId());
            response.put("message", "Login successful. Welcome, " + fullName + "!");

=======
            System.out.println("[AuthController] Login OK: " + fullName);

            Map<String, Object> response = new HashMap<>();
            response.put("success",  true);
            response.put("fullName", fullName);
            response.put("userId",   config.getUserId());
            response.put("message",  "Login successful. Welcome, " + fullName + "!");
>>>>>>> 1906425 (jk)
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[AuthController] Login failed: " + e.getMessage());
<<<<<<< HEAD

            String raw = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String userMessage;

            if (raw.contains("invalid") || raw.contains("wrong") || raw.contains("incorrect")
                    || raw.contains("token") || raw.contains("not logged")) {
                userMessage = "Invalid username or password. Please try again.";
            } else if (raw.contains("timeout") || raw.contains("connect")) {
                userMessage = "Cannot reach the LMS server. Please check your connection.";
            } else if (raw.contains("blocked") || raw.contains("locked")) {
                userMessage = "Your account has been locked. Please contact your administrator.";
            } else {
                userMessage = "Login failed. Please check your credentials and try again.";
            }

            return ResponseEntity.status(401).body(Map.of("error", userMessage));
=======
            String raw = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String msg;
            if (raw.contains("invalid") || raw.contains("wrong")
                    || raw.contains("incorrect") || raw.contains("token")
                    || raw.contains("not logged")) {
                msg = "Invalid username or password. Please try again.";
            } else if (raw.contains("timeout") || raw.contains("connect")) {
                msg = "Cannot reach the LMS server. Check your internet connection.";
            } else if (raw.contains("blocked") || raw.contains("locked")) {
                msg = "Your account has been locked. Contact your administrator.";
            } else {
                msg = "Login failed. Please check your credentials and try again.";
            }
            return ResponseEntity.status(401).body(Map.of("error", msg));
>>>>>>> 1906425 (jk)
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        sessionManager.clearSession();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        if (sessionManager.isLoggedIn()) {
            MoodleConfig config = sessionManager.getActiveConfig();
<<<<<<< HEAD
            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "userId", config.getUserId()
            ));
        }
        return ResponseEntity.ok(Map.of("loggedIn", false));
    }
}
=======
            return ResponseEntity.ok(Map.of("loggedIn", true, "userId", config.getUserId()));
        }
        return ResponseEntity.ok(Map.of("loggedIn", false));
    }
}
>>>>>>> 1906425 (jk)
