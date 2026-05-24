package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserFeignDTO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String role;
    private Integer status;
    private Integer points;
    private Integer rating;
}
