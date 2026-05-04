package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// Service to detect file changes
@Service
public class ChangeDetectionService {

private static final Path SNAPSHOT_FILE=Paths.get("downloads","snapshot.json");

// Time format
private static final DateTimeFormatter FMT=
DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
.withZone(ZoneId.of("Asia/Karachi"));

// Json mapper
private final ObjectMapper objectMapper=new ObjectMapper()
.enable(SerializationFeature.INDENT_OUTPUT);


public ChangeReport detectChanges(MoodleConfig config) throws Exception{

System.out.println("starting change detect...");

// Setup api and services
MoodleApiClient apiClient=new MoodleApiClient(config);
CourseService cs=new CourseService(apiClient,config);
FileService fs=new FileService(apiClient);

// Get courses
List<Course> courses=cs.getEnrolledCourses();

// Fresh snapshot
Map<String,List<FileEntry>> fresh=new LinkedHashMap<>();

for(Course course:courses){

List<CourseFile> files=fs.getFilesForCourse(course);

// Map entries
List<FileEntry> entries=files.stream()
.map(f->new FileEntry(
f.getFileName(),
f.getFileUrl(),
f.getFileSize(),
f.getMoodleTimestamp(),
course.getShortName(),
course.getFullName()
))
.collect(Collectors.toList());

fresh.put(String.valueOf(course.getId()),entries);
}

// Load old snapshot
Map<String,List<FileEntry>> stored=loadSnapshot();

// Do diff
ChangeReport report=diff(stored,fresh,courses);

// save new snapshot
saveSnapshot(fresh);

System.out.printf("done %d new %d upd %d rem%n",
report.newFiles().size(),
report.updatedFiles().size(),
report.removedFiles().size());

return report;

}


// take snapshot only
public void takeSnapshot(MoodleConfig config) throws Exception{

System.out.println("Taking snapshot...");

MoodleApiClient apiClient=new MoodleApiClient(config);
CourseService cs=new CourseService(apiClient,config);
FileService fs=new FileService(apiClient);

List<Course> courses=cs.getEnrolledCourses();

Map<String,List<FileEntry>> snapshot=new LinkedHashMap<>();

for(Course course:courses){

List<CourseFile> files=fs.getFilesForCourse(course);

snapshot.put(String.valueOf(course.getId()),
files.stream()
.map(f->new FileEntry(
f.getFileName(),f.getFileUrl(),
f.getFileSize(),f.getMoodleTimestamp(),
course.getShortName(),course.getFullName()))
.collect(Collectors.toList())
);
}

// Save
saveSnapshot(snapshot);

System.out.println("snapshot saved "+courses.size());
}


// check snapshot exist
public boolean hasSnapshot(){
return Files.exists(SNAPSHOT_FILE);
}


// Get last snapshot time
public String getLastSnapshotTime(){
try{
return FMT.format(Instant.ofEpochMilli(
Files.getLastModifiedTime(SNAPSHOT_FILE).toMillis()));
}catch(Exception e){
return "never";
}
}


// Difference checking
private ChangeReport diff(Map<String,List<FileEntry>> stored,
Map<String,List<FileEntry>> fresh,
List<Course> courses){

List<ChangeItem> newFiles=new ArrayList<>();
List<ChangeItem> updatedFiles=new ArrayList<>();
List<ChangeItem> removedFiles=new ArrayList<>();

// Map course id 
Map<String,String> courseNames=new HashMap<>();
for(Course c:courses)
courseNames.put(String.valueOf(c.getId()),c.getFullName());

for(Map.Entry<String,List<FileEntry>> entry:fresh.entrySet()){

String courseId=entry.getKey();
List<FileEntry> freshFiles=entry.getValue();
List<FileEntry> storedFiles=stored.getOrDefault(courseId,List.of());
String courseName=courseNames.getOrDefault(courseId,courseId);

// Index old files
Map<String,FileEntry> storedByName=new HashMap<>();
for(FileEntry f:storedFiles) storedByName.put(f.fileName(),f);

for(FileEntry f:freshFiles){

FileEntry old=storedByName.get(f.fileName());

// New file
if(old==null){
newFiles.add(new ChangeItem(f.fileName(),courseName,
f.courseShortName(),f.fileSize(),f.moodleTimestamp(),"NEW"));
}

// Updated file
else if(f.moodleTimestamp()>0 && old.moodleTimestamp()>0
&& f.moodleTimestamp()>old.moodleTimestamp()){
updatedFiles.add(new ChangeItem(f.fileName(),courseName,
f.courseShortName(),f.fileSize(),f.moodleTimestamp(),"UPDATED"));
}
}

// Removed files
Set<String> freshNames=freshFiles.stream()
.map(FileEntry::fileName).collect(Collectors.toSet());

for(FileEntry old:storedFiles){
if(!freshNames.contains(old.fileName())){
removedFiles.add(new ChangeItem(old.fileName(),courseName,
old.courseShortName(),old.fileSize(),old.moodleTimestamp(),"REMOVED"));
}
}
}

// time now
String checkedAt=FMT.format(Instant.now());

return new ChangeReport(newFiles,updatedFiles,removedFiles,checkedAt);
}


// load snapshot
private Map<String,List<FileEntry>> loadSnapshot(){

if(!Files.exists(SNAPSHOT_FILE)) return new HashMap<>();

try{
return objectMapper.readValue(
SNAPSHOT_FILE.toFile(),
new TypeReference<Map<String,List<FileEntry>>>() {}
);
}catch(IOException e){
System.out.println("snapshot read fail "+e.getMessage());
return new HashMap<>();
}
}


// save snapshot
private void saveSnapshot(Map<String,List<FileEntry>> snapshot) throws IOException{

Files.createDirectories(SNAPSHOT_FILE.getParent());
objectMapper.writeValue(SNAPSHOT_FILE.toFile(),snapshot);
}


// File entry
public record FileEntry(
String fileName,
String fileUrl,
long fileSize,
long moodleTimestamp,
String courseShortName,
String courseFullName
){}


// Change item
public record ChangeItem(
String fileName,
String courseName,
String courseShortName,
long fileSize,
long moodleTimestamp,
String changeType
){}


// Report
public record ChangeReport(
List<ChangeItem> newFiles,
List<ChangeItem> updatedFiles,
List<ChangeItem> removedFiles,
String checkedAt
){

// Total changes
public int totalChanges(){
return newFiles.size()+updatedFiles.size()+removedFiles.size();
}

// Sees for changes
public boolean hasChanges(){
return totalChanges()>0;
}
}
}
