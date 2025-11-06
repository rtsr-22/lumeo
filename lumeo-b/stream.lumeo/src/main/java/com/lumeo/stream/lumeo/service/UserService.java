package com.lumeo.stream.lumeo.service;



import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;


import com.lumeo.stream.lumeo.dto.RegisterRequest;
import com.lumeo.stream.lumeo.dto.AuthenticationResponse;
import com.lumeo.stream.lumeo.dto.LoginRequest;
import com.lumeo.stream.lumeo.entity.User;
import com.lumeo.stream.lumeo.repository.UserRepo;
import com.lumeo.stream.lumeo.security.JwtUtil;

@Service
public class UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private StorjService storjService;

    @Autowired
    private PasswordEncoder passwordEncoder;

   public AuthenticationResponse registerUser(RegisterRequest request) {

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            return new AuthenticationResponse("Email already registered", false, null);
        }

        String profileKey = null;
        String bannerKey = null;

        try {
            profileKey = storjService.upload(request.getProfilePicture(), "users/profile/");
            bannerKey  = storjService.upload(request.getBannerPicture() , "users/banner/");
        } catch (Exception e) {
            e.printStackTrace();
            return new AuthenticationResponse("File upload failed", false, null);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .description(request.getDescription())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .profilePictureUrl(profileKey)  
                .bannerPictureUrl(bannerKey)
                .build();

        userRepo.save(user);

        return new AuthenticationResponse("User registered successfully", true, profileKey);
    }

    public AuthenticationResponse loginUser(LoginRequest request) {
        var userOpt = userRepo.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return new AuthenticationResponse("Invalid email or password", false, null);
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthenticationResponse("Invalid email or password", false, null);
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthenticationResponse("Login successful", true, token);
    }

    public Optional<User> getLoggedInUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
        return Optional.empty();
    }
    return userRepo.findByEmail(auth.getName());
}


}
