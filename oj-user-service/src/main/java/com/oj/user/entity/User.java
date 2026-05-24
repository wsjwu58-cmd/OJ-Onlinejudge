package com.oj.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    @TableField("nickname")
    private String nickName;

    private String email;

    private String avatarUrl;

    private String role;

    private Integer status;

    private Integer points;

    private Integer rating;

    private Integer dailyQuestionStreak;

    private Integer totalSubmissions;

    private LocalDateTime lastLoginTime;

    private LocalDateTime vipExpireTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String nonceStr;

    @TableField(exist = false)
    private String value;
}
