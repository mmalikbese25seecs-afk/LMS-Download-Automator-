package com.downloadc.downloadc.api;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Handles Google Drive: OAuth URL generation, callback, uploading files.
//
// FIX (Drive): The original code used GoogleCredential (deprecated in
// google-api-client 2.x). The correct modern approach is UserCredentials
// from google-auth-library-oauth2-http, wrapped in HttpCredentialsAdapter
// when building the Drive service. This also fixes token refresh — the old
// GoogleCredential.Builder approach silently skipped refresh token storage,
// so the access token expired after ~1 hour with no way to renew it.
//
// Required extra dependency in pom.xml:
//   <dependency>
//     <groupId>com.google.auth</groupId>
//     <artifactId>google-auth-library-oauth2-http</artifactId>
//     <version>1.23.0</version>
//   </dependency>

@Service
public class GoogleDriveService {

    private static final JsonFactory   JSON_FACTORY     = GsonFactory.getDefaultInstance();
    private static final List<String>  SCOPES           = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String        APP_NAME         = "LMS Download Automator";
    private static final String        ROOT_FOLDER_NAME = "LMS Downloads";
    private static final String        REDIRECT_URI     = "http://localhost:8080/api/drive/callback";

    @Value("${google.drive.credentials.path:src/main/resources/credentials.json}")
    private String credentialsPath;

    // FIX: store UserCredentials (supports refresh) instead of deprecated GoogleCredential
    private final Map<String, UserCredentials>      userCredentials = new ConcurrentHashMap<>();
    private final Map<String, String>               rootFolderIds   = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>>  folderCaches    = new ConcurrentHashMap<>();

    public boolean isConfigured() {
        return new java.io.File(credentialsPath).exists();
    }

    public boolean isAuthorized(String sessionId) {
        return userCredentials.containsKey(sessionId);
    }

    public String getAuthUrl(String sessionId) throws Exception {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        return flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URI)
                .setState(sessionId)
                .setAccessType("offline")
                .build();
    }

    // FIX: exchange code for token and store as UserCredentials with refresh support
    public void handleCallback(String code, String sessionId) throws Exception {
        GoogleClientSecrets secrets   = loadSecrets();
        NetHttpTransport    transport = GoogleNetHttpTransport.newTrustedTransport();

        TokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                transport,
                JSON_FACTORY,
                secrets.getDetails().getClientId(),
                secrets.getDetails().getClientSecret(),
                code,
                REDIRECT_URI
        ).execute();

        // FIX: build UserCredentials — supports automatic token refresh via refreshToken
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(secrets.getDetails().getClientId())
                .setClientSecret(secrets.getDetails().getClientSecret())
                .setRefreshToken(tokenResponse.getRefreshToken())
                .setAccessToken(new AccessToken(
                        tokenResponse.getAccessToken(),
                        tokenResponse.getExpiresInSeconds() != null
                                ? new Date(System.currentTimeMillis()
                                    + tokenResponse.getExpiresInSeconds() * 1000)
                                : null
                ))
                .build();

        userCredentials.put(sessionId, credentials);
        System.out.println("[GoogleDriveService] Token saved for session: " + sessionId);
    }

    public void disconnect(String sessionId) {
        userCredentials.remove(sessionId);
        rootFolderIds.remove(sessionId);
        folderCaches.remove(sessionId);
        System.out.println("[GoogleDriveService] Disconnected session: " + sessionId);
    }

    public UploadResult uploadCourse(String sessionId, String courseName) throws Exception {

        Drive  drive          = buildDriveForSession(sessionId);
        String rootId         = getOrCreateRootFolder(drive, sessionId);
        String courseFolderId = getOrCreateCourseFolder(drive, sessionId, rootId, courseName);
        Set<String> existing  = listFileNamesInFolder(drive, courseFolderId);

        Path courseDir = Paths.get("downloads", sanitize(courseName));
        if (!Files.exists(courseDir))
            throw new IOException("No local downloads found for: " + courseName);

        int uploaded = 0, skipped = 0, failed = 0;

        try (var stream = Files.list(courseDir)) {
            for (Path filePath : stream.toList()) {
                if (!Files.isRegularFile(filePath)) continue;

                String fileName = filePath.getFileName().toString();

                if (existing.contains(fileName)) {
                    System.out.println("[Drive] Skipped (already in Drive): " + fileName);
                    skipped++;
                    continue;
                }

                try {
                    String mimeType = Files.probeContentType(filePath);
                    if (mimeType == null) mimeType = "application/octet-stream";

                    File fileMeta = new File();
                    fileMeta.setName(fileName);
                    fileMeta.setParents(Collections.singletonList(courseFolderId));

                    FileContent content = new FileContent(mimeType, filePath.toFile());
                    drive.files().create(fileMeta, content).setFields("id").execute();

                    System.out.println("[Drive] Uploaded: " + fileName);
                    uploaded++;

                } catch (Exception e) {
                    System.err.println("[Drive] Failed: " + fileName + " — " + e.getMessage());
                    failed++;
                }
            }
        }

        return new UploadResult(courseName, uploaded, skipped, failed);
    }

    public List<DriveFileInfo> listCourseFiles(String sessionId, String courseName) throws Exception {
        Drive  drive          = buildDriveForSession(sessionId);
        String rootId         = getOrCreateRootFolder(drive, sessionId);
        String courseFolderId = getOrCreateCourseFolder(drive, sessionId, rootId, courseName);

        FileList result = drive.files().list()
                .setQ("'" + courseFolderId + "' in parents and trashed = false")
                .setFields("files(id, name, size, webViewLink, mimeType)")
                .execute();

        List<DriveFileInfo> files = new ArrayList<>();
        for (File f : result.getFiles()) {
            files.add(new DriveFileInfo(
                    f.getId(), f.getName(),
                    f.getSize() != null ? f.getSize() : 0L,
                    f.getWebViewLink(), f.getMimeType()
            ));
        }
        return files;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    // FIX: use HttpCredentialsAdapter to wrap UserCredentials for the Drive builder
    private Drive buildDriveForSession(String sessionId) throws Exception {
        UserCredentials credentials = userCredentials.get(sessionId);
        if (credentials == null)
            throw new IllegalStateException("Drive not connected. Please authorize first.");

        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(transport, JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APP_NAME)
                .build();
    }

    private GoogleAuthorizationCodeFlow buildFlow() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        return new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, loadSecrets(), SCOPES)
                .setAccessType("offline")
                .build();
    }

    private GoogleClientSecrets loadSecrets() throws Exception {
        java.io.File credFile = new java.io.File(credentialsPath);
        if (!credFile.exists())
            throw new FileNotFoundException(
                    "credentials.json not found at: " + credentialsPath);
        return GoogleClientSecrets.load(JSON_FACTORY, new FileReader(credFile));
    }

    private String getOrCreateRootFolder(Drive drive, String sessionId) throws Exception {
        if (rootFolderIds.containsKey(sessionId))
            return rootFolderIds.get(sessionId);

        FileList existing = drive.files().list()
                .setQ("name = '" + ROOT_FOLDER_NAME + "' "
                        + "and mimeType = 'application/vnd.google-apps.folder' "
                        + "and trashed = false")
                .setFields("files(id)").execute();

        String id;
        if (!existing.getFiles().isEmpty()) {
            id = existing.getFiles().get(0).getId();
        } else {
            File meta = new File();
            meta.setName(ROOT_FOLDER_NAME);
            meta.setMimeType("application/vnd.google-apps.folder");
            id = drive.files().create(meta).setFields("id").execute().getId();
            System.out.println("[Drive] Created root folder: " + ROOT_FOLDER_NAME);
        }

        rootFolderIds.put(sessionId, id);
        return id;
    }

    private String getOrCreateCourseFolder(Drive drive, String sessionId,
                                           String rootId, String courseName) throws Exception {
        String safe  = sanitize(courseName);
        Map<String, String> cache = folderCaches.computeIfAbsent(sessionId, k -> new HashMap<>());
        if (cache.containsKey(safe)) return cache.get(safe);

        FileList existing = drive.files().list()
                .setQ("name = '" + safe + "' "
                        + "and '" + rootId + "' in parents "
                        + "and mimeType = 'application/vnd.google-apps.folder' "
                        + "and trashed = false")
                .setFields("files(id)").execute();

        String id;
        if (!existing.getFiles().isEmpty()) {
            id = existing.getFiles().get(0).getId();
        } else {
            File meta = new File();
            meta.setName(safe);
            meta.setMimeType("application/vnd.google-apps.folder");
            meta.setParents(Collections.singletonList(rootId));
            id = drive.files().create(meta).setFields("id").execute().getId();
            System.out.println("[Drive] Created course folder: " + safe);
        }

        cache.put(safe, id);
        return id;
    }

    private Set<String> listFileNamesInFolder(Drive drive, String folderId) throws Exception {
        FileList result = drive.files().list()
                .setQ("'" + folderId + "' in parents and trashed = false")
                .setFields("files(name)").execute();
        Set<String> names = new HashSet<>();
        for (File f : result.getFiles()) names.add(f.getName());
        return names;
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();
    }

    public record UploadResult(String courseName, int uploaded, int skipped, int failed) {}
    public record DriveFileInfo(String id, String name, long size, String webViewLink, String mimeType) {}
}
