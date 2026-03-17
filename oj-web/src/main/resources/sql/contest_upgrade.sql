-- ============================================================
-- 比赛模块升级 SQL
-- ============================================================

-- 1. 比赛参赛表
CREATE TABLE IF NOT EXISTS contest_participants (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    contest_id INT UNSIGNED NOT NULL COMMENT '比赛ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    score INT NOT NULL DEFAULT 0 COMMENT '比赛总得分',
    solved_count INT NOT NULL DEFAULT 0 COMMENT '通过题数',
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',

    UNIQUE KEY uk_contest_user (contest_id, user_id),
    INDEX idx_contest_id (contest_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rank (contest_id, score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛参赛表';

-- 2. submissions 表增加 contest_id 字段
ALTER TABLE submissions
    ADD COLUMN contest_id INT UNSIGNED DEFAULT NULL COMMENT '比赛ID，NULL表示普通练习' AFTER problem_id,
    ADD INDEX idx_contest_id (contest_id),
    ADD INDEX idx_contest_user_problem (contest_id, user_id, problem_id);
