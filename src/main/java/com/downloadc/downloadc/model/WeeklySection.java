package com.downloadc.downloadc.model;

import java.util.List;

// represents one week/section of a course with its files
public class WeeklySection {

    private final int sectionNumber;
    private final String sectionName;
    private final String dateRange;
    private final List<CourseFile> files;

    public WeeklySection(int sectionNumber, String sectionName,
                         String dateRange, List<CourseFile> files) {
        this.sectionNumber = sectionNumber;
        this.sectionName   = sectionName;
        this.dateRange     = dateRange;
        this.files         = files;
    }

    public int getSectionNumber()      { return sectionNumber; }
    public String getSectionName()     { return sectionName; }
    public String getDateRange()       { return dateRange; }
    public List<CourseFile> getFiles() { return files; }

    @Override
    public String toString() {
        return String.format("WeeklySection[%d: %s, %d files]",
                sectionNumber, sectionName, files.size());
    }
}