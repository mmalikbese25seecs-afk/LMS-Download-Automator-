package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.CourseService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


 //Serves enrolled course data over HTTP

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private SessionManager sessionManager;


    @GetMapping
    public ResponseEntity<?> getCourses() {

        
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not logged in. POST /api/auth/login first.")
            );
        }

        try {
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            CourseService courseService = new CourseService(apiClient, sessionManager.getActiveConfig());
            List<Course> courses = courseService.getEnrolledCourses();

            return ResponseEntity.ok(courses);

        } catch (Exception e) {
            System.out.println("[CourseController] Error fetching courses: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to fetch courses: " + e.getMessage())
            );
        }
    }
}
