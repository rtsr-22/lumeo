package com.lumeo.stream.lumeo.controller;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lumeo.stream.lumeo.entity.Video;
import com.lumeo.stream.lumeo.service.VideoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    @PostMapping(
    value = "/upload",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam String description) throws Exception {

                 System.out.println("UPLOAD ENDPOINT HIT ✅ fileSize=" + file.getSize());

        Video video = videoService.uploadVideo(file, title, description);
        return ResponseEntity.ok(video);
    }
}
