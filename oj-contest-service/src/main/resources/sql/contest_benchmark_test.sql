-- =============================================
-- 竞赛服务 - 性能测试数据生成与基准测试
-- =============================================

-- 1. 执行索引创建
SOURCE oj-contest-service/src/main/resources/sql/contest_optimization_indexes.sql;

-- 2. 插入10000+竞赛测试数据
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS generate_contests()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO contests (title, description, start_time, end_time, type, status, created_by, created_at, updated_at)
        VALUES (
            CONCAT('测试竞赛 #', i),
            CONCAT('这是第', i, '个测试竞赛。'),
            DATE_ADD(NOW(), INTERVAL (i % 3 - 1) DAY),
            DATE_ADD(NOW(), INTERVAL (i % 3 + 1) DAY),
            CASE (i % 4)
                WHEN 0 THEN 'Weekly Contest'
                WHEN 1 THEN 'Biweekly Contest'
                WHEN 2 THEN 'Mock Interview'
                ELSE 'Company Contest'
            END,
            CASE
                WHEN DATE_ADD(NOW(), INTERVAL (i % 3 - 1) DAY) > NOW() THEN 'Upcoming'
                WHEN DATE_ADD(NOW(), INTERVAL (i % 3 + 1) DAY) < NOW() THEN 'Ended'
                ELSE 'Running'
            END,
            (i % 1000) + 1,
            DATE_ADD('2024-01-01', INTERVAL i MINUTE),
            DATE_ADD('2024-01-01', INTERVAL i MINUTE)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 3. 插入参赛数据（每个竞赛1-10个参赛者）
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS generate_contest_participants()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE participant_count INT;
    DECLARE j INT;
    WHILE i <= 10000 DO
        SET participant_count = 1 + FLOOR(RAND() * 10);
        SET j = 1;
        WHILE j <= participant_count DO
            INSERT INTO contest_participants (contest_id, user_id, score, solved_count, registered_at)
            VALUES (i, (i * 7 + j) % 10000 + 1, FLOOR(RAND() * 500), FLOOR(RAND() * 4), NOW());
            SET j = j + 1;
        END WHILE;
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 4. 性能对比测试

-- 测试1：按状态筛选（应该使用 idx_status）
SELECT SQL_NO_CACHE COUNT(*) FROM contests WHERE status = 'Upcoming';
SELECT SQL_NO_CACHE COUNT(*) FROM contests WHERE status = 'Running';
SELECT SQL_NO_CACHE COUNT(*) FROM contests WHERE status = 'Ended';

-- 测试2：按时间范围查询（应该使用 idx_start_time）
SELECT SQL_NO_CACHE COUNT(*) FROM contests WHERE start_time > NOW();
SELECT SQL_NO_CACHE COUNT(*) FROM contests WHERE start_time BETWEEN '2024-01-01' AND '2024-06-01';

-- 测试3：批量更新比赛状态（优化后：单条CASE WHEN SQL）
-- 优化前需要: SELECT * FROM contests (10000 rows) + N次 UPDATE
-- 优化后只需: 1条 UPDATE ... SET status = CASE WHEN ... END

-- 测试4：批量查询参赛人数（优化后：GROUP BY单次查询）
-- 优化前: SELECT COUNT(*) FROM contest_participants WHERE contest_id = ? （每页20次）
-- 优化后: SELECT contest_id, COUNT(*) FROM contest_participants WHERE contest_id IN (...) GROUP BY contest_id （1次）
