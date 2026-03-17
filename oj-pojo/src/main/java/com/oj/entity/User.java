package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码哈希值
     */
    private String passwordHash;

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
     * 连续刷题天数
     */
    private Integer dailyQuestionStreak;

    /**
     * 总提交次数
     */
    private Integer totalSubmissions;

    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * VIP过期时间
     */
    private LocalDateTime vipExpireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /*
    验证码
     */
    @TableField(exist = false)
    private String nonceStr;
    @TableField(exist = false)
    private String value;

}
