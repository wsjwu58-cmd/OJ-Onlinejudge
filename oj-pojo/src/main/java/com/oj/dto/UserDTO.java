package com.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserDTO {
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

    /**
     * 邮箱
     */
    private String email;
    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 用户角色: student-学生, teacher-教师, admin-管理员
     */
    private String role;

    /**
     * 账号状态: 1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 竞赛评分
     */
    private Integer rating;
    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginTime;



}
