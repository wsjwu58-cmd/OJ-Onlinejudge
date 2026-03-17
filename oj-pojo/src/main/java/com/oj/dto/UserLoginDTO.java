package com.oj.dto;

import lombok.Data;

@Data
public class UserLoginDTO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;
    /**
     * 昵称
     */
    private String nickName;
}
