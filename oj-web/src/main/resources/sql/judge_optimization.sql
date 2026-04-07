-- =====================================================
-- 判题链路MySQL索引优化脚本
-- 执行前请先备份数据库！
-- =====================================================

-- 1. submissions 表优化（最关键）

-- 添加 problem_id + status 复合索引（用于 updateAccept 计算通过率）
-- 在 DatabaseUpdateConsumer.onMessage 中被频繁调用
ALTER TABLE submissions ADD INDEX idx_problem_status (problem_id, status);

-- 添加 submit_time + status 复合索引（用于统计查询）
-- 在统计接口中被使用
ALTER TABLE submissions ADD INDEX idx_time_status (submit_time, status);

-- 添加 covering index 避免回表（用户提交历史查询）
-- 在查询用户提交记录时被使用
ALTER TABLE submissions ADD INDEX idx_user_status_time (user_id, status, submit_time);


-- 2. test_cases 表优化

-- 添加 covering index 避免回表
-- 在 JudgeServiceImpl.submit 和 JudgeTaskConsumer.onMessage 中被频繁调用
ALTER TABLE test_case ADD INDEX idx_problem_covering (problem_id, is_sample, order_num);


-- 3. problems 表优化（可选，根据查询模式添加）

-- 如果经常按 difficulty + status 查询题目
ALTER TABLE problems ADD INDEX idx_difficulty_status (difficulty, status);


-- 4. 验证索引是否生效
SHOW INDEX FROM submissions;
SHOW INDEX FROM test_case;
SHOW INDEX FROM problems;


-- =====================================================
-- 性能监控：慢查询日志配置建议
-- =====================================================

-- 查看当前慢查询配置
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';

-- 建议配置（临时生效）
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 超过1秒记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 永久配置需要在 my.cnf 中添加：
-- [mysqld]
-- slow_query_log = 1
-- long_query_time = 1
-- slow_query_log_file = /var/log/mysql/slow.log
