package com.downloadc.downloadc.downloader;

import com.downloadc.downloadc.api.DownloadHistoryService;
<<<<<<< HEAD
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.DownloadRecord;
import com.downloadc.downloadc.model.DownloadStatus;
import com.downloadc.downloadc.model.MoodleConfig;
import com.downloadc.downloadc.model.Downloadable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

// Downloads files from Moodle and saves them locally.
//
// FIX (Download): The original code opened the .part file with
// StandardOpenOption.CREATE (which silently truncates if it already exists
// when not combined with APPEND). On a resumed download this meant the
// already-downloaded bytes were thrown away and the file started from scratch,
// but the Range header told the server to start mid-file — so the saved bytes
// were wrong and the final file was corrupt.
//
// Fixed open-option logic:
//   fresh start  → CREATE_NEW  (fail fast if something is already there)
//   resume       → APPEND      (add to existing bytes, no truncation)
public class FileDownloader {

    private final HttpClient            httpClient;
    private final MoodleConfig          config;
=======
import com.downloadc.downloadc.model.DownloadRecord;
import com.downloadc.downloadc.model.DownloadStatus;
import com.downloadc.downloadc.model.Downloadable;
import com.downloadc.downloadc.model.MoodleConfig;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.Optional;

public class FileDownloader {

    private final HttpClient             httpClient;
    private final MoodleConfig           config;
>>>>>>> 1906425 (jk)
    private final DownloadHistoryService historyService;

    private static final String DOWNLOAD_ROOT = "downloads";

    public FileDownloader(MoodleConfig config, DownloadHistoryService historyService) {
        this.config         = config;
        this.historyService = historyService;
<<<<<<< HEAD

        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    }
            };

            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, trustAll, new SecureRandom());
            this.httpClient = HttpClient.newBuilder().sslContext(ssl).build();

=======
        try {
            TrustManager[] trustAll = { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
            }};
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, trustAll, new SecureRandom());
            this.httpClient = HttpClient.newBuilder().sslContext(ssl).build();
>>>>>>> 1906425 (jk)
        } catch (Exception e) {
            throw new RuntimeException("SSL setup failed: " + e.getMessage(), e);
        }
    }

<<<<<<< HEAD
    public DownloadStatus download(Downloadable courseFile) throws Exception {

        String safeCourse = sanitize(courseFile.getCourseName());
        String safeFile   = sanitize(courseFile.getFileName());

        Path courseFolder = Paths.get(DOWNLOAD_ROOT, safeCourse);
        Path destination  = courseFolder.resolve(safeFile);
        Path partFile     = courseFolder.resolve(safeFile + ".part");

        Optional<DownloadRecord> prior = historyService.findRecord(
                safeFile, courseFile.getCourseName()
        );

        // ── skip check ───────────────────────────────────────────────────────
        if (Files.exists(destination)) {
            if (prior.isPresent()) {
                long lmsTs   = courseFile.getMoodleTimestamp();
                long localTs = prior.get().getMoodleTimestamp();

                if (lmsTs > 0 && localTs > 0 && lmsTs <= localTs) {
                    System.out.println("SKIP: " + safeFile);
                    return DownloadStatus.SKIPPED;
                }

                long localSize = Files.size(destination);
                if (lmsTs == 0 && courseFile.getFileSize() > 0
                        && localSize == courseFile.getFileSize()) {
                    System.out.println("SKIP size: " + safeFile);
                    return DownloadStatus.SKIPPED;
                }

                System.out.println("UPDATE: " + safeFile);
                Files.delete(destination);

            } else {
                long localSize = Files.size(destination);
                if (courseFile.getFileSize() > 0 && localSize == courseFile.getFileSize()) {
                    System.out.println("SKIP no history: " + safeFile);
                    return DownloadStatus.SKIPPED;
                }
                System.out.println("Re-download: " + safeFile);
=======
    public DownloadStatus download(Downloadable file) throws Exception {

        String safeCourse = sanitize(file.getCourseName());
        String safeFile   = sanitize(file.getFileName());

        Path folder      = Paths.get(DOWNLOAD_ROOT, safeCourse);
        Path destination = folder.resolve(safeFile);
        Path partFile    = folder.resolve(safeFile + ".part");

        Optional<DownloadRecord> prior =
                historyService.findRecord(safeFile, file.getCourseName());

        // ── skip / re-download logic ──────────────────────────────────────────
        if (Files.exists(destination)) {
            if (prior.isPresent()) {
                long lmsTs   = file.getMoodleTimestamp();
                long localTs = prior.get().getMoodleTimestamp();
                if (lmsTs > 0 && localTs > 0 && lmsTs <= localTs) {
                    System.out.println("[DL] SKIP " + safeFile);
                    return DownloadStatus.SKIPPED;
                }
                long localSize = Files.size(destination);
                if (lmsTs == 0 && file.getFileSize() > 0
                        && localSize == file.getFileSize()) {
                    System.out.println("[DL] SKIP(size) " + safeFile);
                    return DownloadStatus.SKIPPED;
                }
                System.out.println("[DL] UPDATE " + safeFile);
                Files.delete(destination);
            } else {
                long localSize = Files.size(destination);
                if (file.getFileSize() > 0 && localSize == file.getFileSize()) {
                    System.out.println("[DL] SKIP(no-history) " + safeFile);
                    return DownloadStatus.SKIPPED;
                }
                System.out.println("[DL] Re-download " + safeFile);
>>>>>>> 1906425 (jk)
                Files.delete(destination);
            }
        }

<<<<<<< HEAD
        Files.createDirectories(courseFolder);

        // ── resume check ─────────────────────────────────────────────────────
        long resumeFrom = 0;
        if (Files.exists(partFile)) {
            resumeFrom = Files.size(partFile);
            System.out.println("Resuming " + safeFile + " from byte " + resumeFrom);
        }

        String authUrl = courseFile.getAuthenticatedUrl(config.getToken());

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .GET();

        if (resumeFrom > 0) {
            reqBuilder.header("Range", "bytes=" + resumeFrom + "-");
        }

        HttpResponse<InputStream> response = httpClient.send(
                reqBuilder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );

=======
        Files.createDirectories(folder);

        // ── resume check ──────────────────────────────────────────────────────
        long resumeFrom = 0;
        if (Files.exists(partFile)) {
            resumeFrom = Files.size(partFile);
            System.out.println("[DL] Resuming " + safeFile + " from byte " + resumeFrom);
        }

        String authUrl = file.getAuthenticatedUrl(config.getToken());

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(authUrl)).GET();
        if (resumeFrom > 0)
            req.header("Range", "bytes=" + resumeFrom + "-");

        HttpResponse<InputStream> response = httpClient.send(
                req.build(), HttpResponse.BodyHandlers.ofInputStream());
>>>>>>> 1906425 (jk)
        int status = response.statusCode();

        if (status != 200 && status != 206) {
            if (resumeFrom > 0 && status == 416) {
<<<<<<< HEAD
                // server says the range is unsatisfiable — our .part is already complete
                Files.deleteIfExists(partFile);
                System.out.println("Restarting: " + safeFile);
                return download(courseFile);
            }
            throw new Exception("HTTP " + status + " for " + safeFile);
        }

        long          bytesWritten  = resumeFrom;
        MessageDigest md5           = MessageDigest.getInstance("MD5");
        boolean       freshDownload = (resumeFrom == 0);

        // FIX: use CREATE_NEW for a fresh start (no silent truncation) and
        //      APPEND for a resume (adds to existing bytes, not overwrites).
        StandardOpenOption openOption = freshDownload
=======
                // server says range unsatisfiable — .part already complete
                Files.move(partFile, destination, StandardCopyOption.REPLACE_EXISTING);
                return DownloadStatus.RESUMED;
            }
            throw new Exception("HTTP " + status + " downloading " + safeFile);
        }

        boolean       fresh = (resumeFrom == 0);
        long          bytes = resumeFrom;
        MessageDigest md5   = MessageDigest.getInstance("MD5");

        // BUG FIX: original code used StandardOpenOption.CREATE for both fresh and
        // resume cases.  CREATE silently TRUNCATES an existing file when not combined
        // with APPEND.  So on a resume the already-downloaded bytes were wiped, but
        // the Range header told the server to start mid-file — resulting in a corrupt
        // file that begins at offset X but contains bytes from byte 0 of the response.
        //
        // FIX:
        //   fresh download → CREATE_NEW  (fails loudly if stale .part already exists)
        //   resume         → APPEND      (appends to existing bytes, never truncates)
        StandardOpenOption openOpt = fresh
>>>>>>> 1906425 (jk)
                ? StandardOpenOption.CREATE_NEW
                : StandardOpenOption.APPEND;

        try (InputStream  in  = response.body();
<<<<<<< HEAD
             OutputStream out = Files.newOutputStream(partFile, openOption)) {

            byte[] buffer = new byte[65_536];
            int    read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (freshDownload) md5.update(buffer, 0, read);
                bytesWritten += read;
            }
        }

        Files.move(partFile, destination);

        String hashHex = freshDownload
                ? HexFormat.of().formatHex(md5.digest())
                : null;

        System.out.println("Saved: " + destination + " (" + bytesWritten + " bytes)");

        prior.ifPresent(r -> historyService.removeRecord(safeFile, courseFile.getCourseName()));

        DownloadRecord record = new DownloadRecord(
                safeFile,
                courseFile.getCourseName(),
                safeCourse,
                bytesWritten,
                destination.toString(),
                hashHex,
                courseFile.getMoodleTimestamp()
        );

        historyService.addRecord(record);

        if (resumeFrom > 0)    return DownloadStatus.RESUMED;
=======
             OutputStream out = Files.newOutputStream(partFile, openOpt)) {
            byte[] buf = new byte[65_536];
            int    n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                if (fresh) md5.update(buf, 0, n);
                bytes += n;
            }
        }

        Files.move(partFile, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[DL] Saved " + destination + " (" + bytes + " B)");

        prior.ifPresent(r -> historyService.removeRecord(safeFile, file.getCourseName()));
        historyService.addRecord(new DownloadRecord(
                safeFile,
                file.getCourseName(),
                safeCourse,
                bytes,
                destination.toString(),
                fresh ? HexFormat.of().formatHex(md5.digest()) : null,
                file.getMoodleTimestamp()
        ));

        if (!fresh)            return DownloadStatus.RESUMED;
>>>>>>> 1906425 (jk)
        if (prior.isPresent()) return DownloadStatus.UPDATED;
        return DownloadStatus.DOWNLOADED;
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 1906425 (jk)
