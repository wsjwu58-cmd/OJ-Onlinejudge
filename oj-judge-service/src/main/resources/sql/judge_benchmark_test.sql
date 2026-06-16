-- =============================================
-- 判题服务 - 性能测试数据生成与基准测试
-- =============================================

-- 1. 插入10000+提交记录测试数据
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS generate_submissions()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO submissions (user_id, problem_id, contest_id, code, language, status, runtime_ms, memory_kb,
                                 test_cases_passed, test_cases_total, submit_time)
        VALUES (
            (i % 1000) + 1,
            (i % 2000) + 1,
            IF(i % 3 = 0, (i % 100) + 1, NULL),
            CONCAT('print("Hello ', i, '")'),
            CASE (i % 4)
                WHEN 0 THEN 'Python'
                WHEN 1 THEN 'Java'
                WHEN 2 THEN 'C++'
                ELSE 'JavaScript'
            END,
            CASE (i % 6)
                WHEN 0 THEN 'Accepted'
                WHEN 1 THEN 'Wrong Answer'
                WHEN 2 THEN 'Time Limit Exceeded'
                WHEN 3 THEN 'Memory Limit Exceeded'
                WHEN 4 THEN 'Runtime Error'
                ELSE 'Compile Error'
            END,
            FLOOR(RAND() * 500),
            FLOOR(RAND() * 256000),
            FLOOR(RAND() * 20),
            20,
            DATE_ADD('2024-01-01', INTERVAL i SECOND)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 2. 性能对比：Feign N+1 优化
-- 优化前 pageQuery(20条/页):
--   1次: SELECT * FROM submissions WHERE ... LIMIT 20
--   20次: GET /internal/user/{id}          (userClient.getUserById)
--   20次: GET /internal/problem/{id}       (problemClient.getProblemById)
--   总计: 41次远程调用
--
-- 优化后 pageQuery(20条/页):
--   1次: SELECT * FROM submissions WHERE ... LIMIT 20
--   1次: POST /internal/user/batch          (userClient.getUsersByIds)
--   1次: POST /internal/problem/batch       (problemClient.getProblemsByIds)
--   总计: 3次远程调用（减少93%）
