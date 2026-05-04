package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.DownloadRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
// Service class to store and manage download history in a JSON file

@Service
public class DownloadHistoryService extends BaseStorageService {

    // path where history is saved
 @Override
    public Path getStorageFile() {
        return storagePath("history.json");
    }
    
// Constructor
    public DownloadHistoryService() {
        super(); // calls baseStorageService constructor which sets up object Mapper
    }
    
    // add a new record to history
    public synchronized void addRecord(DownloadRecord record) {
        try {
            List<DownloadRecord> existing = readAll();
            existing.add(record);
            writeAll(existing);
        } catch (IOException e) {
            // don't crash app if history fails
            System.out.println("Error saving history: " + e.getMessage());
        }
    }

    // get all records from file
    
    public synchronized List<DownloadRecord> readAll() {
   // Returns empty list if file not found
        if (!Files.exists(getStorageFile())) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    getStorageFile().toFile(),
                    new TypeReference<List<DownloadRecord>>() {}
            );
        } 
        catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // delete all history (reset)
    public synchronized void clearAll() throws IOException {
        writeAll(new ArrayList<>());
        System.out.println("History cleared");
    }

    // find record by file + course
    public synchronized Optional<DownloadRecord> findRecord(String fileName, String courseName) {
        return readAll().stream()
                .filter(r -> r.getFileName().equalsIgnoreCase(fileName)&& r.getCourseName().equalsIgnoreCase(courseName))
                .findFirst();
    }

    // remove record
    public synchronized void removeRecord(String fileName, String courseName) {
        try {
            List<DownloadRecord> all = readAll();

            all.removeIf(r -> r.getFileName().equalsIgnoreCase(fileName)
                           && r.getCourseName().equalsIgnoreCase(courseName));

            writeAll(all);
        } 
        catch (IOException e) {
            System.out.println("Remove error : " + e.getMessage());
        }
    }

    // search and filter
    public List<DownloadRecord> search(String query, String course, String tag, String sortBy) {

        List<DownloadRecord> records = readAll();

        // Filter by text
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();

            records = records.stream()
                    .filter(r -> r.getFileName().toLowerCase().contains(q)
                              || r.getCourseName().toLowerCase().contains(q)
                              || r.getCourseShortName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        // Filter by Course
        if (course != null && !course.isBlank()) {
            String c = course.trim().toLowerCase();

            records = records.stream()
                    .filter(r -> r.getCourseShortName().toLowerCase().equals(c)|| r.getCourseName().toLowerCase().contains(c))
                    .collect(Collectors.toList());
        }

        // filter by Tag
        if (tag != null && !tag.isBlank()) {
            String t = tag.trim();

            records = records.stream()
                    .filter(r -> r.getTags() != null && r.getTags().contains(t))
                    .collect(Collectors.toList());
        }

        return sort(records, sortBy);
    }

    // sorting
    public List<DownloadRecord> sort(List<DownloadRecord> records, String sortBy) {

        if (sortBy == null || sortBy.isBlank()) sortBy = "newest";

        return switch (sortBy.toLowerCase()) {

            case "oldest" -> records.stream()
                    .sorted(Comparator.comparing(DownloadRecord::getDownloadedAt))
                    .collect(Collectors.toList());

            case "largest" -> records.stream()
                    .sorted(Comparator.comparingLong(DownloadRecord::getFileSizeBytes).reversed())
                    .collect(Collectors.toList());

            case "smallest" -> records.stream()
                    .sorted(Comparator.comparingLong(DownloadRecord::getFileSizeBytes))
                    .collect(Collectors.toList());

            case "course" -> records.stream()
                    .sorted(Comparator.comparing(DownloadRecord::getCourseShortName,
                            String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(DownloadRecord::getFileName,
                                    String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            case "name" -> records.stream()
                    .sorted(Comparator.comparing(DownloadRecord::getFileName,
                            String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            default -> // newest
                    records.stream()
                    .sorted(Comparator.comparing(DownloadRecord::getDownloadedAt).reversed())
                    .collect(Collectors.toList());
        };
    }

    // add tag to record
    public synchronized boolean addTag(String fileName, String courseName, String tag) {
        try {
            List<DownloadRecord> all = readAll();
            boolean found = false;

            for (DownloadRecord r : all) {
                if (r.getFileName().equalsIgnoreCase(fileName)
                        && r.getCourseName().equalsIgnoreCase(courseName)) {
                    r.addTag(tag);
                    found = true;
                    break;
                }
            }

            if (found) writeAll(all);
            return found;

        } catch (IOException e) {
            System.out.println("Tag add error: " + e.getMessage());
            return false;
        }
    }

    // Remove tag
    public synchronized boolean removeTag(String fileName, String courseName, String tag) {
        try {
            List<DownloadRecord> all = readAll();
            boolean found = false;

            for (DownloadRecord r : all) {
                if (r.getFileName().equalsIgnoreCase(fileName)
                        && r.getCourseName().equalsIgnoreCase(courseName)) {
                    r.removeTag(tag);
                    found = true;
                    break;
                }
            }

            if (found) writeAll(all);
            return found;

        } catch (IOException e) {
            System.out.println("Tag remove error: " + e.getMessage());
            return false;
        }
    }

    // get all unique tags
    public List<String> getAllTags() {
        return readAll().stream()
                .filter(r -> r.getTags() != null)
                .flatMap(r -> r.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // stats 
    public DownloadStats getStats() {
        List<DownloadRecord> records = readAll();

        long totalBytes = records.stream()
                .mapToLong(DownloadRecord::getFileSizeBytes).sum();

        long uniqueCourses = records.stream()
                .map(DownloadRecord::getCourseShortName).distinct().count();

        return new DownloadStats(records.size(), totalBytes, uniqueCourses);
    }

    // write to file
    private void writeAll(List<DownloadRecord> records) throws IOException {
        Files.createDirectories(getStorageFile().getParent());
        objectMapper.writeValue(getStorageFile().toFile(), records);
    }

    // stats record
    public record DownloadStats(long totalFiles, long totalBytes, long uniqueCourses) {

        // convert bytes to readable format
        public String totalSizeFormatted() {
            if (totalBytes < 1024) return totalBytes + " B";
            if (totalBytes < 1024 * 1024) return String.format("%.1f KB", totalBytes / 1024.0);
            if (totalBytes < 1024L * 1024 * 1024) return String.format("%.1f MB", totalBytes / (1024.0 * 1024));
            return String.format("%.1f GB", totalBytes / (1024.0 * 1024 * 1024));
        }
    }
}
