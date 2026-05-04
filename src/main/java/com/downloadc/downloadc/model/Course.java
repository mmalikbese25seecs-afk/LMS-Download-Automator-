package com.downloadc.downloadc.model;

// course model, enriched with instructor, last access, new file count, and favorite flag
// extra fields default to safe values so the 4-arg constructor still works fine
public class Course extends LmsResource {

    private final String shortName;
    private final String summary;   // populated by CourseService after the base course is fetched.
    private String  instructorName; // first teacher found in Moodle response
    private long    lastAccess;     // Unix epoch, when student last opened this course
    private int     newFilesCount;  // files on lms not yet downloaded locally
    private boolean favorite;       // pinned by user, persisted by FavoritesService

    public Course(int id, String fullName, String shortName, String summary) {
        super(id, fullName);
        this.shortName = shortName;
        this.summary = summary;
        this.instructorName = "Unknown";
        this.lastAccess = 0;
        this.newFilesCount = 0;
        this.favorite = false;
    }

    public Course(int id, String fullName, String shortName, String summary, String instructorName, long lastAccess, int newFilesCount, boolean favorite) {
        super(id, fullName);
        this.shortName      = shortName;
        this.summary        = summary;
        this.instructorName = instructorName;
        this.lastAccess     = lastAccess;
        this.newFilesCount  = newFilesCount;
        this.favorite       = favorite;
    }

    // getFullName() now delegates to parent's getName()
    public String getFullName()  { return getName(); }
    public String getShortName() { return shortName; }
    public String getSummary()   { return summary; }
    public String getInstructorName() { return instructorName; }
    public long   getLastAccess()     { return lastAccess; }
    public int    getNewFilesCount()  { return newFilesCount; }
    public boolean isFavorite()       { return favorite; }

    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public void setLastAccess(long lastAccess)           { this.lastAccess     = lastAccess; }
    public void setNewFilesCount(int count)              { this.newFilesCount  = count; }
    public void setFavorite(boolean favorite)            { this.favorite       = favorite; }

    // Implementing the abstract method from LmsResource (polymorphism)
    @Override
    public String getSummaryLine() {
        return String.format("%s (%s) — instructor: %s, newFiles: %d",
                getName(), shortName, instructorName, newFilesCount);
    }
}