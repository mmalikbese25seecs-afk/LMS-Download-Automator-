package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// CourseService fetches the user's enrolled courses from Moodle.
public class CourseService {

    private final MoodleApiClient apiClient;
    private final MoodleConfig    config;
    private final FavoritesService favoritesService;

    // Constructor without favorites
    public CourseService(MoodleApiClient apiClient, MoodleConfig config) {
        this.apiClient        = apiClient;
        this.config           = config;
        this.favoritesService = null;
    }

    // Constructor with favorites
    public CourseService(MoodleApiClient apiClient, MoodleConfig config,
                         FavoritesService favoritesService) {
        this.apiClient        = apiClient;
        this.config           = config;
        this.favoritesService = favoritesService;
    }

    public List<Course> getEnrolledCourses() {
        List<Course> courses = new ArrayList<>();
        System.out.println("Getting enrolled courses...");

        try {
            String params = "userid=" + config.getUserId() + "&returnusercount=0";
            JsonNode response = apiClient.callFunction("core_enrol_get_users_courses", params);

            if (response == null || !response.isArray()) {
                System.err.println("bad api response");
                return courses;
            }

            Set<Integer> favoriteIds = (favoritesService != null)
                    ? favoritesService.getFavoriteIds()
                    : Set.of();

            for (JsonNode node : response) {

                int    id        = node.path("id").asInt();
                String fullName  = node.path("fullname").asText("Unknown Course");
                String shortName = node.path("shortname").asText("");
                String summary   = node.path("summary").asText("");

                String instructor = "";
                JsonNode contacts = node.path("contacts");
                if (contacts.isArray() && contacts.size() > 0) {
                    instructor = contacts.get(0).path("fullname").asText("");
                }

                long lastAccess = node.path("lastaccess").asLong(0L);

                // FIX 3: old code called countNewFiles(node, shortName) which read
                // node.path("overviewfiles") — those are thumbnail/banner images, not
                // lecture files. The badge was counting missing thumbnails.
                // We now count actual course content files not yet on disk.
                int newFiles = countNewCourseFiles(id, shortName);

                boolean isFav = favoriteIds.contains(id);

                courses.add(new Course(id, fullName, shortName, summary,
                        instructor, lastAccess, newFiles, isFav));
            }

            System.out.println("Loaded " + courses.size() + " courses");

        } catch (Exception e) {
            System.err.println("error " + e.getMessage());
        }

        return courses;
    }

    // FIX 3: fetch real course content from Moodle and count files not yet on disk.
    // We reuse FileService so the logic is identical to what FileDownloader uses.
    // If the API call fails we return 0 silently — a badge of 0 is safe, a crash is not.
    private int countNewCourseFiles(int courseId, String shortName) {
        try {
            String safeShort = shortName.replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();

            FileService fs = new FileService(apiClient);
            Course dummy   = new Course(courseId, "", shortName, "");

            return (int) fs.getFilesForCourse(dummy).stream()
                    .filter(f -> {
                        String safeFile = f.getFileName()
                                .replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();
                        return !Files.exists(Paths.get("downloads", safeShort, safeFile));
                    })
                    .count();

        } catch (Exception e) {
            return 0; // never crash the course list for a badge count
        }
    }
}
