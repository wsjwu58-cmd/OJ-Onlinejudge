-- =============================================
-- OJ 用户服务数据库建表语句
-- 数据库: exercise (暂共享，后续拆分为 oj_user)
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '用户角色: student-学生, teacher-教师, admin-管理员',
    `status` INT NOT NULL DEFAULT 1 COMMENT '账号状态: 1-启用, 0-禁用',
    `points` INT DEFAULT 0 COMMENT '积分',
    `rating` INT DEFAULT 0 COMMENT '竞赛评分',
    `daily_question_streak` INT DEFAULT 0 COMMENT '连续刷题天数',
    `total_submissions` INT DEFAULT 0 COMMENT '总提交次数',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '上次登录时间',
    `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户签到表
CREATE TABLE IF NOT EXISTS `user_attendance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `date` DATE NOT NULL COMMENT '签到日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到表';
