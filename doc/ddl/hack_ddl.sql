-- ============================================
-- Hack功能 DDL 脚本
-- ============================================

-- 1. Problem表新增字段 (oj-problem-service)
ALTER TABLE problems ADD COLUMN validator_path     VARCHAR(512) COMMENT 'C++校验器源码磁盘路径';
ALTER TABLE problems ADD COLUMN validator_exe_path VARCHAR(512) COMMENT 'C++校验器编译产物路径';
ALTER TABLE problems ADD COLUMN validator_src_hash VARCHAR(64)  COMMENT 'validator.cpp SHA-256值';
ALTER TABLE problems ADD COLUMN reference_path     VARCHAR(512) COMMENT '标准解答文件磁盘路径';
ALTER TABLE problems ADD COLUMN reference_language VARCHAR(32)  DEFAULT 'C++' COMMENT '标准解答语言';

-- 2. TestCase表新增字段 (oj-problem-service)
ALTER TABLE test_cases ADD COLUMN source_hack_id BIGINT DEFAULT NULL COMMENT '来源Hack记录ID';

-- 3. HackRecord表 (oj-contest-service)
CREATE TABLE IF NOT EXISTS hack_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id      INT NOT NULL COMMENT '比赛ID',
    problem_id      INT NOT NULL COMMENT '题目ID',
    hacker_id       BIGINT NOT NULL COMMENT '发起Hack的用户ID',
    target_user_id  BIGINT NOT NULL COMMENT '被Hack的目标用户ID',
    target_submission_id BIGINT NOT NULL COMMENT '被攻击的提交记录ID',
    hack_input      MEDIUMTEXT NOT NULL COMMENT 'Hack测试数据(输入)',
    hack_output     MEDIUMTEXT COMMENT '标准解答的输出结果',
    status          VARCHAR(32) NOT NULL DEFAULT 'Pending' COMMENT 'Pending/Validating/HackSuccess/HackFailed/InvalidData/SystemError',
    error_info      TEXT COMMENT '错误信息',
    target_result   VARCHAR(32) COMMENT '目标代码运行结果',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contest_hacker (contest_id, hacker_id),
    INDEX idx_contest_target (contest_id, target_user_id),
    INDEX idx_contest_problem (contest_id, problem_id)
);
