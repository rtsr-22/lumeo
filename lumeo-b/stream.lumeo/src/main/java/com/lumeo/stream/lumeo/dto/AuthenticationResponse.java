package com.lumeo.stream.lumeo.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    private String message;
    private boolean success;
        private String token;

}
