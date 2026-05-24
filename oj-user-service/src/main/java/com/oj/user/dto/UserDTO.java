package com.oj.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickName;
    private String email;
    private String avatarUrl;
    private String role;
    private Integer status;
    private Integer points;
    private Integer rating;
    private LocalDateTime lastLoginTime;
}
