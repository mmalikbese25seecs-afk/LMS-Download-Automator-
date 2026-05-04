package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.WeeklySection;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// Course files week by week from moodle
public class WeeklyFileService {

    private final MoodleApiClient apiClient;

    // Constructor
    public WeeklyFileService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // get all weeks with their files
    public List<WeeklySection> getWeeklySections(int courseId,String courseName) throws Exception {

        System.out.println("fetching sections for courseId=" + courseId);

        // call moodle api
        JsonNode sections = apiClient.callFunction(
                "core_course_get_contents",
                "courseid=" + courseId
        );

        List<WeeklySection> result = new ArrayList<>();

        // if nothing returned
        if (sections == null || !sections.isArray()) {
            return result;
        }

        // loop all sections
        for (JsonNode section : sections) {

            int sectionNum = section.has("section") ? section.get("section").asInt() : 0;
            String sectionName = section.has("name") ? section.get("name").asText() : "Section " + sectionNum;

            JsonNode modules = section.get("modules");

            if (modules == null || !modules.isArray()) continue;

            // get files from this section
            List<CourseFile> files = extractFilesFromModules(modules, courseName);

            // skip if no files
            if (files.isEmpty()) continue;

            // get readable name/date
            String dateRange = resolveDateRange(sectionName, sectionNum);

            result.add(new WeeklySection(sectionNum, sectionName, dateRange, files));
        }

        System.out.println("found " + result.size() + " sections with files");
        return result;
    }

    // Extract only downloadable files from modules
    private List<CourseFile> extractFilesFromModules(JsonNode modules,String courseName) {
        List<CourseFile> files = new ArrayList<>();

        for (JsonNode module : modules) {

            String modName = module.has("modname") ? module.get("modname").asText() : "";

            // only take resource type
            if (!"resource".equals(modName)) {
                continue;

            }

            JsonNode contents = module.get("contents");
            if (contents == null || !contents.isArray()) continue;

            for (JsonNode content : contents) {

                String type = content.has("type") ? content.get("type").asText() : "";

                // Only actual files
                if (!"file".equals(type)) continue;

                String fileName = content.has("filename")
                        ? content.get("filename").asText() : "unknown";
                String fileUrl  = content.has("fileurl")
                        ? content.get("fileurl").asText()  : null;

                // skip if no url
                if (fileUrl == null || fileUrl.isEmpty()) continue;

                long fileSize = content.has("filesize")
                        ? content.get("filesize").asLong() : 0L;
                String mimeType = content.has("mimetype")
                        ? content.get("mimetype").asText() : "application/octet-stream";

                files.add(new CourseFile(fileName, fileUrl, fileSize, mimeType, courseName));
            }
        }
        return files;
    }

    // Clean
    private String resolveDateRange(String sectionName, int sectionNum) {

        // If empty name
        if (sectionName == null || sectionName.isBlank()) {
            return "Section " + sectionNum;
        }

        String trimmed = sectionName.trim();

        // If just a number
        if (trimmed.matches("\\d+")) {
            return "Week " + trimmed;
        }

        // Return name as it is
        return trimmed;
    }
}