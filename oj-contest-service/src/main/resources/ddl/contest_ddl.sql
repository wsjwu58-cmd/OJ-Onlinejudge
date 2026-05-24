-- 竞赛服务数据库DDL
-- 数据库: exercise (暂共享，后续独立拆分)

-- 竞赛表
CREATE TABLE IF NOT EXISTS `contests` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL COMMENT '竞赛名称',
    `description` TEXT COMMENT '竞赛描述',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `type` VARCHAR(50) DEFAULT 'Weekly Contest' COMMENT '竞赛类型: Weekly Contest/Biweekly Contest/Mock Interview/Company Contest',
    `status` VARCHAR(20) DEFAULT 'Upcoming' COMMENT '竞赛状态: Upcoming/Running/Ended',
    `created_by` BIGINT COMMENT '创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛表';

-- 竞赛题目关联表
CREATE TABLE IF NOT EXISTS `contest_problems` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `contest_id` INT NOT NULL COMMENT '竞赛ID',
    `problem_id` INT NOT NULL COMMENT '题目ID',
    `score` INT DEFAULT 100 COMMENT '该题分数',
    `sort_order` INT DEFAULT 0 COMMENT '题目在比赛中的顺序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_contest_id` (`contest_id`),
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛题目关联表';

-- 竞赛参赛表
CREATE TABLE IF NOT EXISTS `contest_participants` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `contest_id` INT NOT NULL COMMENT '竞赛ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT DEFAULT 0 COMMENT '比赛总得分',
    `solved_count` INT DEFAULT 0 COMMENT '通过题数',
    `registered_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    UNIQUE INDEX `uk_contest_user` (`contest_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛参赛表';

-- 题解评论表
CREATE TABLE IF NOT EXISTS `solution_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` INT NOT NULL DEFAULT 1 COMMENT '类型：1-题解 2-评论',
    `problem_id` BIGINT NOT NULL COMMENT '关联题目ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID（0表示顶级）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复的用户ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题（题解必填）',
    `content` TEXT COMMENT '正文内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `status` INT DEFAULT 1 COMMENT '状态：0-隐藏 1-正常 2-置顶',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_problem_id` (`problem_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题解评论表';
