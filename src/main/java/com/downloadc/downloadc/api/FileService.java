package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// parses Moodle's course content tree and pulls out downloadable file metadata.
// also reads timemodified for smart sync, so we can skip unchanged files
public class FileService {

    private final MoodleApiClient apiClient;

    public FileService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<CourseFile> getFilesForCourse(Course course) {
        List<CourseFile> files = new ArrayList<>();
        System.out.println("[FileService] Fetching files for: " + course.getShortName());

        // Moodle tree: sections, modules, contents
        // we only want modname=resource nodes that have an actual file url
        try {
            JsonNode sections = apiClient.callFunction(
                    "core_course_get_contents",
                    "courseid=" + course.getId()
            );

            if (sections == null || !sections.isArray()) {
                System.err.println("[FileService] Invalid response for " + course.getShortName());
                return files;
            }

            for (JsonNode section : sections) {
                JsonNode modules = section.get("modules");
                if (modules == null || !modules.isArray()) continue;

                for (JsonNode module : modules) {
                    if (!"resource".equals(module.path("modname").asText(""))) continue;

                    JsonNode contents = module.get("contents");
                    if (contents == null || !contents.isArray()) continue;

                    for (JsonNode content : contents) {
                        if (!"file".equals(content.path("type").asText(""))) continue;
                        if (!content.has("fileurl")) continue;

                        String fileName = content.path("filename").asText("unknown_file");
                        String fileUrl  = content.path("fileurl").asText();
                        long   fileSize = content.path("filesize").asLong(0L);
                        String mimeType = content.path("mimetype").asText("application/octet-stream");
                        long   moodleTs = content.path("timemodified").asLong(0L);

                        files.add(new CourseFile(
                                fileName, fileUrl, fileSize, mimeType,
                                course.getShortName(), moodleTs
                        ));
                    }
                }
            }

            System.out.println("[FileService] Found " + files.size()
                    + " files in " + course.getShortName());

        } catch (Exception e) {
            System.err.println("[FileService] Error for " + course.getShortName()
                    + ": " + e.getMessage());
        }

        return files;
    }
}