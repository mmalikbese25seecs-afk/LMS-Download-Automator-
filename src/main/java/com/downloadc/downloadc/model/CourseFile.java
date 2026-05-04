package com.downloadc.downloadc.model;

// a single downloadable file from Moodle
// moodleTimestamp = "timemodified" from lms, used by smart sync to skip unchanged files.
public class CourseFile implements Downloadable{

    private final String fileName;
    private final String fileUrl;         // needs auth token appended before downloading
    private final long   fileSize;
    private final String fileType;
    private final String courseName;
    private final long   moodleTimestamp; // Unix epoch seconds, 0 if unknown

    public CourseFile(String fileName, String fileUrl,
                      long fileSize, String fileType,
                      String courseName, long moodleTimestamp) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.courseName = courseName;
        this.moodleTimestamp = moodleTimestamp;
    }

    // Constructor
    public CourseFile(String fileName, String fileUrl,
                      long fileSize, String fileType, String courseName) {
        this(fileName, fileUrl, fileSize, fileType, courseName, 0L);
    }
    
@Override
    public String getFileName() {
        return fileName;
    }
    @Override
    public String getFileUrl(){
        return fileUrl; 
    }
    @Override
    public long   getFileSize(){
        return fileSize;
    }
    @Override
    public String getFileType(){
        return fileType; 
    }
    @Override
    public String getCourseName(){
        return courseName; 
    }
    @Override
    public long   getMoodleTimestamp() { 
        return moodleTimestamp; 
    }

    @Override
    public String toString() {
        return String.format("CourseFile[%s, %.1f KB, %s, ts=%d]",
                fileName, fileSize / 1024.0, courseName, moodleTimestamp);
    }
}
