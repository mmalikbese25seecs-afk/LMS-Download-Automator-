package com.downloadc.downloadc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// metadata for every downloaded file, also used for smart sync logic
public class DownloadRecord {

    private final String       fileName;
    private final String       courseName;
    private final String       courseShortName;
    private final long         fileSizeBytes;
    private final String       downloadedAt;
    private final String       localPath;

    // md5 hash + lms timestamp, used to skip unchanged files on re-sync
    private final String       fileHash;        // null if not computed yet
    private final long         moodleTimestamp; // 0 if unknown

    private List<String> tags; // changeable so we can add/remove later

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Karachi"));

    // used when saving a fresh download
    public DownloadRecord(String fileName, String courseName,
                          String courseShortName, long fileSizeBytes,
                          String localPath, String fileHash, long moodleTimestamp) {
        this.fileName        = fileName;
        this.courseName      = courseName;
        this.courseShortName = courseShortName;
        this.fileSizeBytes   = fileSizeBytes;
        this.localPath       = localPath;
        this.fileHash        = fileHash;
        this.moodleTimestamp = moodleTimestamp;
        this.downloadedAt    = FORMATTER.format(Instant.now());
        this.tags            = new ArrayList<>();
    }

    // backwards compat, old code that doesnt pass hash/timestamp
    public DownloadRecord(String fileName, String courseName,
                          String courseShortName, long fileSizeBytes,
                          String localPath) {
        this(fileName, courseName, courseShortName, fileSizeBytes, localPath, null, 0L);
    }

    // jackson uses this when loading history.json
    @JsonCreator
    public DownloadRecord(
            @JsonProperty("fileName")        String fileName,
            @JsonProperty("courseName")      String courseName,
            @JsonProperty("courseShortName") String courseShortName,
            @JsonProperty("fileSizeBytes")   long fileSizeBytes,
            @JsonProperty("localPath")       String localPath,
            @JsonProperty("downloadedAt")    String downloadedAt,
            @JsonProperty("fileHash")        String fileHash,
            @JsonProperty("moodleTimestamp") long moodleTimestamp,
            @JsonProperty("tags")            List<String> tags) {
        this.fileName        = fileName;
        this.courseName      = courseName;
        this.courseShortName = courseShortName;
        this.fileSizeBytes   = fileSizeBytes;
        this.localPath       = localPath;
        this.downloadedAt    = downloadedAt != null ? downloadedAt : FORMATTER.format(Instant.now());
        this.fileHash        = fileHash;
        this.moodleTimestamp = moodleTimestamp;
        this.tags            = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public String       getFileName()        { return fileName; }
    public String       getCourseName()      { return courseName; }
    public String       getCourseShortName() { return courseShortName; }
    public long         getFileSizeBytes()   { return fileSizeBytes; }
    public String       getDownloadedAt()    { return downloadedAt; }
    public String       getLocalPath()       { return localPath; }
    public String       getFileHash()        { return fileHash; }
    public long         getMoodleTimestamp() { return moodleTimestamp; }
    public List<String> getTags()            { return tags; }

    // dont add empty / duplicate tags
    public void addTag(String tag) {
        if (tag != null && !tag.isBlank() && !tags.contains(tag.trim()))
            tags.add(tag.trim());
    }

    public void removeTag(String tag) {
        tags.remove(tag != null ? tag.trim() : null);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag != null ? tag.trim() : "");
    }

    // converts bytes to readable format — B, KB, MB, GB
    public String getFileSizeFormatted() {
        if (fileSizeBytes < 1024)                return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024)         return String.format("%.1f KB", fileSizeBytes / 1024.0);
        if (fileSizeBytes < 1024L * 1024 * 1024) return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024));
        return String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
    }
}