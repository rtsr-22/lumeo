package com.lumeo.stream.lumeo.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
     private String name;
    private String email;
    private String password;
     private MultipartFile profilePicture;
    private MultipartFile bannerPicture;
    private String description;
    private String username;
}
