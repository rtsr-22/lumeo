package com.lumeo.stream.lumeo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lumeo.stream.lumeo.dto.RegisterRequest;
import com.lumeo.stream.lumeo.dto.AuthenticationResponse;
import com.lumeo.stream.lumeo.dto.LoginRequest;
import com.lumeo.stream.lumeo.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping(value = "/register", consumes = "multipart/form-data", produces = "application/json")

 
    public AuthenticationResponse register(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String description,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "bannerPicture", required = false) MultipartFile bannerPicture) {

        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .username(username)
                .description(description)
                .profilePicture(profilePicture)
                .bannerPicture(bannerPicture)
                .build();

        return userService.registerUser(request);
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody LoginRequest request) {
        return userService.loginUser(request);
    }

    @GetMapping("/login")
    public String helloWorld() {
        return "Hello World";
    }

}
