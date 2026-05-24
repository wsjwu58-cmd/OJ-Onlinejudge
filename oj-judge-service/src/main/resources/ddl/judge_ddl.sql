-- 判题服务 DDL
-- 数据库: exercise
-- 表: submissions, malicious_code_log

-- 提交记录表
CREATE TABLE IF NOT EXISTS `submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `problem_id` INT NOT NULL COMMENT '题目ID',
    `contest_id` INT DEFAULT NULL COMMENT '比赛ID（非比赛提交为空）',
    `code` TEXT NOT NULL COMMENT '提交的代码',
    `language` VARCHAR(50) NOT NULL COMMENT '编程语言',
    `status` VARCHAR(50) NOT NULL COMMENT '判题状态（Pending/Accepted/Wrong Answer/Compile Error/Time Limit Exceeded/Memory Limit Exceeded/Runtime Error/System Error）',
    `runtime_ms` INT DEFAULT NULL COMMENT '运行时间（毫秒）',
    `memory_kb` INT DEFAULT NULL COMMENT '内存使用（KB）',
    `test_cases_passed` INT DEFAULT NULL COMMENT '通过的测试用例数',
    `test_cases_total` INT DEFAULT NULL COMMENT '总测试用例数',
    `error_info` TEXT DEFAULT NULL COMMENT '错误信息',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '提交者IP地址',
    `submit_time` DATETIME NOT NULL COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_problem_id` (`problem_id`),
    KEY `idx_contest_id` (`contest_id`),
    KEY `idx_status` (`status`),
    KEY `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';

-- 恶意代码日志表
CREATE TABLE IF NOT EXISTS `malicious_code_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `code` TEXT NOT NULL COMMENT '检测到的代码',
    `language` VARCHAR(50) NOT NULL COMMENT '编程语言',
    `detection_type` VARCHAR(100) NOT NULL COMMENT '检测类型',
    `risk_level` VARCHAR(20) NOT NULL COMMENT '风险等级（LOW/MEDIUM/HIGH/CRITICAL）',
    `description` TEXT DEFAULT NULL COMMENT '检测描述',
    `problem_id` INT DEFAULT NULL COMMENT '相关题目ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_language` (`language`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='恶意代码检测日志表';
