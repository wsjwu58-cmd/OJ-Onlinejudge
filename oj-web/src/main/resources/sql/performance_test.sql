-- =====================================================
-- 判题链路性能测试脚本
-- =====================================================

-- 1. EXPLAIN 分析关键查询
-- =====================================================

-- 分析 selectCountProblem 查询计划
EXPLAIN SELECT COUNT(id) FROM submissions
WHERE problem_id = 1 AND status = 'Accepted';

-- 分析 countSubmission 查询计划
EXPLAIN SELECT COUNT(id) FROM submissions
WHERE status = 'Pending' AND submit_time >= '2024-01-01';

-- 分析 selectSubmission 查询计划
EXPLAIN SELECT * FROM submissions
WHERE user_id = 1 AND problem_id = 1;

-- 分析 test_cases 查询计划
EXPLAIN SELECT * FROM test_case
WHERE problem_id = 1 ORDER BY order_num;


-- 2. 性能基准测试（优化前后对比）
-- =====================================================

-- 清空查询缓存（MySQL 8.0+ 已移除查询缓存）
RESET QUERY CACHE;

-- 测试1: 统计某题目的提交总数（优化后应该使用 idx_problem_status）
SELECT SQL_NO_CACHE COUNT(id) FROM submissions
WHERE problem_id = 1;

-- 测试2: 统计某题目的AC数量（优化后应该使用 idx_problem_status）
SELECT SQL_NO_CACHE COUNT(id) FROM submissions
WHERE problem_id = 1 AND status = 'Accepted';

-- 测试3: 查询用户的某题目提交历史
SELECT SQL_NO_CACHE id, user_id, problem_id, status, submit_time
FROM submissions
WHERE user_id = 1 AND problem_id = 1
ORDER BY submit_time DESC
LIMIT 10;

-- 测试4: 统计时间范围内的Pending提交
SELECT SQL_NO_CACHE COUNT(id) FROM submissions
WHERE status = 'Pending'
AND submit_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 测试5: 查询测试用例
SELECT SQL_NO_CACHE id, problem_id, is_sample, order_num
FROM test_case
WHERE problem_id = 1
ORDER BY order_num;


-- 3. 使用 SHOW PROFILE 分析执行时间
-- =====================================================

SET profiling = 1;

-- 执行查询
SELECT COUNT(id) FROM submissions WHERE problem_id = 1 AND status = 'Accepted';
SELECT COUNT(id) FROM submissions WHERE problem_id = 1;




-- 4. 批量插入测试数据（模拟高并发场景）
-- =====================================================

-- 生成测试数据的存储过程
DELIMITER //
CREATE PROCEDURE generate_test_submissions(
    IN p_problem_id INT,
    IN p_count INT
)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE rand_status VARCHAR(50);
    DECLARE statuses VARCHAR(200) DEFAULT 'Pending,Judging,Accepted,Wrong Answer,Time Limit Exceeded,Runtime Error';

    WHILE i < p_count DO
        SET rand_status = ELT(FLOOR(1 + RAND() * 6), 'Pending', 'Judging', 'Accepted', 'Wrong Answer', 'Time Limit Exceeded', 'Runtime Error');

        INSERT INTO submissions (user_id, problem_id, code, language, status, runtime_ms, memory_kb, test_cases_passed, test_cases_total, submit_time)
        VALUES (
            FLOOR(1 + RAND() * 1000),
            p_problem_id,
            'test code',
            ELT(FLOOR(1 + RAND() * 5), 'Java', 'Python', 'C++', 'JavaScript', 'Go'),
            rand_status,
            FLOOR(RAND() * 1000),
            FLOOR(RAND() * 50000),
            FLOOR(RAND() * 10),
            10,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 86400) SECOND)
        );

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 调用存储过程生成测试数据
CALL generate_test_submissions(1, 10000);


-- 5. 使用 sys schema 进行性能分析
-- =====================================================

-- 查看最慢的查询（需要 MySQL 5.7+）
SELECT * FROM sys.statements_with_sorting
WHERE db = 'exercise'
ORDER BY exec_count DESC
LIMIT 10;


