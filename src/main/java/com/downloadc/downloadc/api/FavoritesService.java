package com.downloadc.downloadc.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

// saves pinned course ids to downloads/favorites.json as a simple int array e.g. [12,47,93]
// used by CourseService, FavoritesController, and DashboardService
@Service
public class FavoritesService extends BaseStorageService  {

    public Path getStorageFile() {
        return storagePath("favorites.json");
    }

        public FavoritesService() {
        super();
    }

    public synchronized Set<Integer> getFavoriteIds() {
        if (!Files.exists(getStorageFile())) return new HashSet<>();
        try {
            return objectMapper.readValue(
                    getStorageFile().toFile(),
                    new TypeReference<Set<Integer>>() {}
            );
        } catch (IOException e) {
            System.out.println("[FavoritesService] Read error: " + e.getMessage());
            return new HashSet<>();
        }
    }

    public boolean isFavorite(int courseId) {
        return getFavoriteIds().contains(courseId);
    }

    // returns true if newly added, false if already in the list
    public synchronized boolean addFavorite(int courseId) {
        try {
            Set<Integer> ids = getFavoriteIds();
            if (ids.add(courseId)) {
                save(ids);
                System.out.println("[FavoritesService] Pinned course " + courseId);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println("[FavoritesService] Save error: " + e.getMessage());
            return false;
        }
    }

    // returns true if removed, false if wasnt there
    public synchronized boolean removeFavorite(int courseId) {
        try {
            Set<Integer> ids = getFavoriteIds();
            if (ids.remove(courseId)) {
                save(ids);
                System.out.println("[FavoritesService] Unpinned course " + courseId);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println("[FavoritesService] Save error: " + e.getMessage());
            return false;
        }
    }

    private void save(Set<Integer> ids) throws IOException {
        Files.createDirectories(getStorageFile().getParent());
        objectMapper.writeValue(getStorageFile().toFile(), ids);
    }

    @Override
    public synchronized void clearAll() throws IOException {
        save(new HashSet<>());
        System.out.println("Favorites cleared");
    }
}
