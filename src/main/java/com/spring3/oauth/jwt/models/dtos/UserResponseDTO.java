package com.spring3.oauth.jwt.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String accountStatus;
    private int chapterReadCount;
    private String imagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tierName;
}
