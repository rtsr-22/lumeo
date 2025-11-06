package com.lumeo.stream.lumeo.service;

import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorjService {

    private final S3Client s3Client;

    @Value("${storj.s3.bucket}")
    private String bucket;

    private final String prefixBase = "videos/hls";

    // For image/video before HLS conversion
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            log.warn("upload: empty multipart file for folder {}", folder);
            return null;
        }

        String key = folder + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            log.info("Uploaded multipart file to {}/{}", bucket, key);
            return key;
        } catch (IOException e) {
            log.error("Failed to upload multipart file to S3: {}", e.getMessage(), e);
            return null;
        }
    }

    // For uploading a single File with explicit key (full key path in bucket)
    private String uploadFileToS3(File file, String key) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        String contentType = detectContentType(file.getName());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file.toPath()));
        log.info("Uploaded file {} -> {}/{} (contentType={})", file.getAbsolutePath(), bucket, key, contentType);
        return key;
    }

    /**
     * Upload entire HLS folder recursively, preserving relative paths under prefixBase/{folderName}/...
     * Returns the S3 object key of the uploaded master playlist (e.g. videos/hls/hls_9/master.m3u8)
     * Throws RuntimeException on failure.
     */
    public String uploadHlsFolder(String folderPath) {
        Path base = Path.of(folderPath);
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            throw new RuntimeException("HLS folder not found: " + folderPath);
        }

        String folderName = base.getFileName().toString();
        String prefix = prefixBase + "/" + folderName + "/";

        List<Path> files;
        try (Stream<Path> s = Files.walk(base)) {
            files = s.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read HLS folder: " + e.getMessage(), e);
        }

        if (files.isEmpty()) {
            throw new RuntimeException("No files found in HLS folder: " + folderPath);
        }

        String masterKey = null;

        for (Path p : files) {
            File f = p.toFile();
            // compute relative key so we preserve any subfolders
            Path rel = base.relativize(p);
            String key = prefix + rel.toString().replace(File.separatorChar, '/');

            try {
                uploadFileToS3(f, key);
            } catch (IOException e) {
                log.error("Failed to upload {} -> {}/{} : {}", f.getAbsolutePath(), bucket, key, e.getMessage(), e);
                throw new RuntimeException("Upload failed for " + f.getAbsolutePath(), e);
            }

            // track master playlist key
            if (rel.toString().equals("master.m3u8") || f.getName().equalsIgnoreCase("master.m3u8")) {
                masterKey = key;
            }
        }

        if (masterKey == null) {
            // try to fallback to variant playlists if no master exists
            for (Path p : files) {
                String name = p.getFileName().toString().toLowerCase();
                if (name.endsWith(".m3u8")) {
                    Path rel = base.relativize(p);
                    masterKey = prefix + rel.toString().replace(File.separatorChar, '/');
                    log.warn("No master.m3u8 found; falling back to first playlist: {}", masterKey);
                    break;
                }
            }
        }

        if (masterKey == null) {
            throw new RuntimeException("Master playlist (.m3u8) not uploaded or found in folder: " + folderPath);
        }

        log.info("uploadHlsFolder completed. masterKey={}", masterKey);
        return masterKey;
    }

    /**
     * Returns a public URL or gateway URL for the given object key.
     * Implement according to your gateway / CDN / signed-url strategy.
     */
    public String getPublicUrlForKey(String key) {
        // If your Storj S3 gateway exposes HTTPS URL like https://<gateway-host>/<bucket>/<key>
        // configure the gateway base URL in properties and build the URL here.
        // For now return the key as a placeholder.
        return key;
    }

    // simple content type detection for HLS files
    private String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".m3u8")) return "application/vnd.apple.mpegurl"; // or "application/x-mpegurl"
        if (lower.endsWith(".ts")) return "video/MP2T";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    // sanitize filename simple helper
    private String sanitizeFilename(String name) {
        if (name == null) return UUID.randomUUID().toString();
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
