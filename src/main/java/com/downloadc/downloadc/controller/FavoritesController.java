package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.FavoritesService;
import com.downloadc.downloadc.config.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// get all favorite course ids
// POST /api/favorites/{id} will add to favorites
// DELETE /api/favorites/{id} will remove from favorites

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    @Autowired private SessionManager sessionManager;
    @Autowired private FavoritesService favoritesService;

    // returns list of fav course ids, 401 if not logged in
    @GetMapping
    public ResponseEntity<?> list() {
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));
        return ResponseEntity.ok(favoritesService.getFavoriteIds());
    }

    // pins a course to favorites
    @PostMapping("/{courseId}")
    public ResponseEntity<?> pin(@PathVariable int courseId) {
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));
        favoritesService.addFavorite(courseId);
        return ResponseEntity.ok(Map.of("message", "Course pinned.", "courseId", courseId));
    }

    // removes course from favorites
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> unpin(@PathVariable int courseId) {
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));
        favoritesService.removeFavorite(courseId);
        return ResponseEntity.ok(Map.of("message", "Course unpinned.", "courseId", courseId));
    }
}