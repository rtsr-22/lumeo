package com.lumeo.stream.lumeo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.util.UUID;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.lumeo.stream.lumeo.entity.ProcessingStatus;
import com.lumeo.stream.lumeo.entity.User;
import com.lumeo.stream.lumeo.entity.Video;
import com.lumeo.stream.lumeo.repository.VideoRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepo videoRepo;
    private final StorjService storjService;
    private final UserService userService;

    @Value("${video.temp-path}")
    private String tempPath;

    public Video uploadVideo(MultipartFile file, String title, String description) throws Exception {
        User loggedUser = userService.getLoggedInUser()
                .orElseThrow(() -> new RuntimeException("User not logged in"));

        File dir = new File(tempPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String localFilePath = dir.getAbsolutePath() + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        File localFile = new File(localFilePath);
        file.transferTo(localFile);

        Video video = Video.builder()
                .title(title)
                .description(description)
                .originalFilename(file.getOriginalFilename())
                .status(ProcessingStatus.PENDING)
                .uploader(loggedUser)
                .build();

        video = videoRepo.save(video);

        log.info("File saved locally: {}", localFilePath);
        log.info("Starting async HLS conversion for video {}", video.getId());
        processHLSAsync(video, localFilePath);

        return video;
    }

    @Async
    public void processHLSAsync(Video video, String inputPath) {
        Long videoId = video.getId();
        log.info("🎬 Starting HLS processing for video {}", videoId);

        try {
            updateStatus(video, ProcessingStatus.PROCESSING);

            String outputDir = tempPath + "/hls_" + video.getId();
            File hlsFolder = new File(outputDir);
            if (!hlsFolder.exists() && !hlsFolder.mkdirs()) {
                throw new RuntimeException("Could not create output directory: " + outputDir);
            }

            String masterPlaylist = outputDir + "/master.m3u8";

            // FFmpeg command (String.format needs %%03d to produce %03d)
            String command = String.format(
                    "ffmpeg -y -i \"%s\" " +
                            "-filter_complex \"[0:v]split=3[v1][v2][v3];" +
                            "[v1]scale=854:480[v1out];" +
                            "[v2]scale=1280:720[v2out];" +
                            "[v3]scale=1920:1080[v3out]\" " +

                            // 480p
                            "-map \"[v1out]\" -map 0:a? -c:v:0 libx264 -b:v:0 1500k -c:a:0 aac -preset veryfast " +
                            "-hls_time 6 -hls_playlist_type vod -hls_segment_filename \"%s/480p_%%03d.ts\" \"%s/480p.m3u8\" " +

                            // 720p
                            "-map \"[v2out]\" -map 0:a? -c:v:1 libx264 -b:v:1 3500k -c:a:1 aac -preset veryfast " +
                            "-hls_time 6 -hls_playlist_type vod -hls_segment_filename \"%s/720p_%%03d.ts\" \"%s/720p.m3u8\" " +

                            // 1080p
                            "-map \"[v3out]\" -map 0:a? -c:v:2 libx264 -b:v:2 6000k -c:a:2 aac -preset veryfast " +
                            "-hls_time 6 -hls_playlist_type vod -hls_segment_filename \"%s/1080p_%%03d.ts\" \"%s/1080p.m3u8\"",
                    inputPath,
                    outputDir, outputDir,
                    outputDir, outputDir,
                    outputDir, outputDir
            );

            log.info("Executing FFmpeg conversion for video {}. Command: {}", videoId, command);

            // Use ProcessBuilder with bash -c (if you prefer fully-argumented ProcessBuilder, change to List<String>)
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[FFmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
            }

            log.info("FFmpeg finished for video {}", videoId);

            // write master playlist (you can choose to generate programmatically or rely on FFmpeg variant playlists)
            String masterContent = "#EXTM3U\n" +
                    "#EXT-X-VERSION:3\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=854x480\n480p.m3u8\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=3500000,RESOLUTION=1280x720\n720p.m3u8\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080\n1080p.m3u8\n";
            Files.write(Path.of(masterPlaylist), masterContent.getBytes());

            // --- list files locally before upload
            List<Path> hlsFiles;
            try (Stream<Path> s = Files.walk(Path.of(outputDir))) {
                hlsFiles = s.filter(Files::isRegularFile).collect(Collectors.toList());
            }

            if (hlsFiles.isEmpty()) {
                log.error("No HLS files present in {} after FFmpeg for video {}", outputDir, videoId);
                updateStatus(video, ProcessingStatus.FAILED);
                return;
            }

            log.info("Found {} HLS files to upload for video {}:", hlsFiles.size(), videoId);
            hlsFiles.forEach(p -> log.info("  -> {} ({} bytes)", p.toString(), p.toFile().length()));

            // ensure there are .ts segments locally (fail early if FFmpeg didn't create them)
            boolean localHasTs = hlsFiles.stream().anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".ts"));
            if (!localHasTs) {
                log.error("No .ts segment files present locally in {} for video {} — aborting upload", outputDir, videoId);
                updateStatus(video, ProcessingStatus.FAILED);
                return;
            }

            // --- upload using StorjService.uploadHlsFolder (returns master key)
            log.info("Uploading HLS folder {} to Storj for video {}", outputDir, videoId);
            String masterKey = null;
            try {
                masterKey = storjService.uploadHlsFolder(outputDir); // matches your StorjService
            } catch (Exception e) {
                log.error("Storj upload failed for video {}: {}", videoId, e.getMessage(), e);
                updateStatus(video, ProcessingStatus.FAILED);
                return;
            }

            if (masterKey == null || masterKey.isBlank()) {
                log.error("StorjService.uploadHlsFolder returned null/empty master key for video {}. Aborting.", videoId);
                updateStatus(video, ProcessingStatus.FAILED);
                return;
            }

            // compute a public URL for the returned key
            String masterUrl = storjService.getPublicUrlForKey(masterKey);
            video.setHlsMasterUrl(masterUrl);
            updateStatus(video, ProcessingStatus.COMPLETED);

            log.info("Video {} processed and uploaded successfully! masterKey={} masterUrl={}", videoId, masterKey, masterUrl);

            // --- delete original input file (log result)
            try {
                boolean deleted = new File(inputPath).delete();
                if (!deleted) log.warn("Could not delete input file: {}", inputPath);
                else log.debug("Deleted input file: {}", inputPath);
            } catch (Exception ex) {
                log.warn("Error deleting input file {}: {}", inputPath, ex.getMessage());
            }

            // --- recursively delete HLS folder only after successful upload
            try {
                Path dirPath = Path.of(outputDir);
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder()) // delete children first
                        .map(Path::toFile)
                        .forEach(f -> {
                            if (!f.delete()) {
                                log.warn("Failed to delete local file: {}", f.getAbsolutePath());
                            } else {
                                log.debug("Deleted local file: {}", f.getAbsolutePath());
                            }
                        });
                log.info("Deleted local HLS folder {}", outputDir);
            } catch (NoSuchFileException nsf) {
                log.warn("HLS folder not found when attempting delete: {}", outputDir);
            } catch (IOException ex) {
                log.warn("Error when deleting HLS folder {}: {}", outputDir, ex.getMessage());
            }

        } catch (Exception e) {
            log.error("HLS Processing Failed for video {}", videoId, e);
            try {
                updateStatus(video, ProcessingStatus.FAILED);
            } catch (Exception ex) {
                log.error("Could not update status to FAILED for video {}", videoId, ex);
            }
        }
    }

    private void updateStatus(Video video, ProcessingStatus status) {
        video.setStatus(status);
        videoRepo.save(video);
    }
}
