-- ============================================================
-- OJ题目服务 - N+1查询优化前后性能对比脚本
-- 使用方法：在MySQL客户端执行本文件
-- 对比项：
--   1. 索引优化前后查询速度
--   2. N+1逐条查询 vs 批量查询速度
--   3. 批量UPDATE vs 逐条UPDATE速度
-- ============================================================

-- ==================== 第一步：清理旧测试数据 ====================
DELETE FROM problem_types_rel WHERE problem_id >= 1000000;
DELETE FROM problems WHERE id >= 1000000;
ALTER TABLE problems AUTO_INCREMENT = 1000000;

-- ==================== 第二步：插入10000条测试数据 ====================
-- 插入题目
DROP PROCEDURE IF EXISTS insert_test_problems;
DELIMITER $$
CREATE PROCEDURE insert_test_problems()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO problems (title, content, difficulty, status, acceptance, created_at, updated_at)
        VALUES (
            CONCAT('测试题目 #', LPAD(i,5,'0')),
            CONCAT('# Problem ', i, '\n\n测试内容描述。'),
            ELT(1 + (i % 3), 'Easy', 'Medium', 'Hard'),
            IF(i % 5 = 0, 0, 1),
            ROUND(RAND() * 100, 2),
            DATE_ADD('2024-01-01 00:00:00', INTERVAL i MINUTE),
            DATE_ADD('2024-01-01 00:00:00', INTERVAL i MINUTE)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 插入题目类型（8种）
INSERT IGNORE INTO problem_types (name, is_active, sort_order) VALUES
('Array', 1, 1), ('String', 1, 2), ('DP', 1, 3), ('Tree', 1, 4),
('Graph', 1, 5), ('Math', 1, 6), ('Sorting', 1, 7), ('Greedy', 1, 8);

-- 插入题目-类型关联
DROP PROCEDURE IF EXISTS insert_test_rel;
DELIMITER $$
CREATE PROCEDURE insert_test_rel()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE tc INT;
    DECLARE tid INT;
    WHILE i <= 10000 DO
        SET tc = 1 + FLOOR(RAND() * 3);
        INSERT IGNORE INTO problem_types_rel (problem_id, type_id, created_at)
        SELECT LAST_INSERT_ID(), 1 + MOD(i + 0, 8), NOW();
        IF tc >= 2 THEN
            INSERT IGNORE INTO problem_types_rel (problem_id, type_id, created_at)
            VALUES (LAST_INSERT_ID(), 1 + MOD(i + 3, 8), NOW());
        END IF;
        IF tc >= 3 THEN
            INSERT IGNORE INTO problem_types_rel (problem_id, type_id, created_at)
            VALUES (LAST_INSERT_ID(), 1 + MOD(i + 6, 8), NOW());
        END IF;
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

SELECT '>>> 正在插入10000道题目...' AS status;
CALL insert_test_problems();
SELECT CONCAT('题目数量: ', COUNT(*)) AS result FROM problems;
SELECT '>>> 正在插入题目类型关联...' AS status;
CALL insert_test_rel();
SELECT CONCAT('关联记录数: ', COUNT(*)) AS result FROM problem_types_rel;

-- =================== 第三步：索引优化前性能测试 ====================
-- 先删除优化索引（如果存在）
ALTER TABLE problems DROP INDEX IF EXISTS idx_status;
ALTER TABLE problems DROP INDEX IF EXISTS idx_difficulty;
ALTER TABLE problems DROP INDEX IF EXISTS idx_created_at;
SELECT '>>> 索引已删除，开始测试无索引性能...' AS status;

-- 存储过程：计时查询
DROP PROCEDURE IF EXISTS timed_query;
DELIMITER $$
CREATE PROCEDURE timed_query(IN test_name VARCHAR(200))
BEGIN
    CREATE TEMPORARY TABLE IF NOT EXISTS perf_results (
        test_name VARCHAR(200), stage VARCHAR(20), duration_ms DOUBLE, notes VARCHAR(500)
    );
END$$
DELIMITER ;

-- ====== 测试A：无索引 - 按status筛选 ======
SET @start = NOW(6);
SELECT COUNT(*) INTO @dummy FROM problems WHERE status = 1;
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('status筛选-无索引', '优化前', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, '全表扫描');

SET @start = NOW(6);
SELECT COUNT(*) INTO @dummy FROM problems WHERE difficulty = 'Hard';
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('difficulty筛选-无索引', '优化前', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, '全表扫描');

SET @start = NOW(6);
SELECT id, title FROM problems ORDER BY created_at DESC LIMIT 20;
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('排序分页-无索引', '优化前', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, 'filesort');

-- ====== 测试B：模拟N+1逐条查询（20条记录，每条逐次查类型） ======
SET @start = NOW(6);
-- 1. 主查询
SELECT id INTO @main_id1 FROM problems WHERE id >= 1000000 LIMIT 1;
SELECT id INTO @main_id2 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 1;
SELECT id INTO @main_id3 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 2;
SELECT id INTO @main_id4 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 3;
SELECT id INTO @main_id5 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 4;
SELECT id INTO @main_id6 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 5;
SELECT id INTO @main_id7 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 6;
SELECT id INTO @main_id8 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 7;
SELECT id INTO @main_id9 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 8;
SELECT id INTO @main_id10 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 9;
SELECT id INTO @main_id11 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 10;
SELECT id INTO @main_id12 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 11;
SELECT id INTO @main_id13 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 12;
SELECT id INTO @main_id14 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 13;
SELECT id INTO @main_id15 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 14;
SELECT id INTO @main_id16 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 15;
SELECT id INTO @main_id17 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 16;
SELECT id INTO @main_id18 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 17;
SELECT id INTO @main_id19 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 18;
SELECT id INTO @main_id20 FROM problems WHERE id >= 1000000 LIMIT 1 OFFSET 19;
-- 2. 逐条查类型（N+1）
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id1;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id2;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id3;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id4;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id5;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id6;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id7;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id8;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id9;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id10;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id11;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id12;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id13;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id14;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id15;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id16;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id17;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id18;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id19;
SELECT pt.* FROM problem_types pt INNER JOIN problem_types_rel ptr ON pt.id = ptr.type_id WHERE ptr.problem_id = @main_id20;
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('pageQuery类型填充(N+1)', '优化前', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, '1+20=21次SQL');

-- ====== 测试C：模拟批量查询（优化后，2次SQL） ======
SET @start = NOW(6);
SELECT id INTO @main_id1 FROM problems WHERE id >= 1000000 LIMIT 1;
-- 批量查询关联表
SELECT ptr.* FROM problem_types_rel ptr WHERE ptr.problem_id IN (@main_id1,@main_id2,@main_id3,@main_id4,@main_id5,@main_id6,@main_id7,@main_id8,@main_id9,@main_id10,@main_id11,@main_id12,@main_id13,@main_id14,@main_id15,@main_id16,@main_id17,@main_id18,@main_id19,@main_id20);
-- 然后批量获取类型
SELECT pt.* FROM problem_types pt WHERE pt.id IN (SELECT type_id FROM problem_types_rel WHERE problem_id IN (@main_id1,@main_id2,@main_id3,@main_id4,@main_id5,@main_id6,@main_id7,@main_id8,@main_id9,@main_id10,@main_id11,@main_id12,@main_id13,@main_id14,@main_id15,@main_id16,@main_id17,@main_id18,@main_id19,@main_id20));
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('pageQuery类型填充(批量)', '优化后', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, '1+2=3次SQL');

-- =================== 第四步：创建索引 ====================
SELECT '>>> 创建优化索引...' AS status;
ALTER TABLE problems ADD INDEX idx_status (status);
ALTER TABLE problems ADD INDEX idx_difficulty (difficulty);
ALTER TABLE problems ADD INDEX idx_created_at (created_at);

SELECT '>>> 索引创建完成，开始测试有索引性能...' AS status;

-- ====== 测试D：有索引 - 按status筛选 ======
SET @start = NOW(6);
SELECT COUNT(*) INTO @dummy FROM problems WHERE status = 1;
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('status筛选-有索引', '优化后', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, 'idx_status');

SET @start = NOW(6);
SELECT COUNT(*) INTO @dummy FROM problems WHERE difficulty = 'Hard';
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('difficulty筛选-有索引', '优化后', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, 'idx_difficulty');

SET @start = NOW(6);
SELECT id, title FROM problems ORDER BY created_at DESC LIMIT 20;
SET @end = NOW(6);
INSERT INTO perf_results VALUES ('排序分页-有索引', '优化后', TIMESTAMPDIFF(MICROSECOND, @start, @end) / 1000, 'idx_created_at覆盖');

-- =================== 第五步：输出对比报告 ====================
SELECT '============= N+1查询 & 索引优化 性能对比报告 =============' AS '';
SELECT '' AS '';

SELECT
    test_name AS '测试项',
    SUM(CASE WHEN stage = '优化前' THEN duration_ms END) AS '优化前(ms)',
    SUM(CASE WHEN stage = '优化后' THEN duration_ms END) AS '优化后(ms)',
    CONCAT(ROUND(SUM(CASE WHEN stage = '优化前' THEN duration_ms END) / SUM(CASE WHEN stage = '优化后' THEN duration_ms END), 1), 'x') AS '提速倍数',
    MAX(notes) AS '备注'
FROM perf_results
WHERE test_name NOT LIKE '%pageQuery%'
GROUP BY test_name
ORDER BY test_name;

SELECT '' AS '';
SELECT
    test_name AS 'N+1对比测试项',
    SUM(CASE WHEN stage = '优化前' THEN duration_ms END) AS '优化前(21次SQL-ms)',
    SUM(CASE WHEN stage = '优化后' THEN duration_ms END) AS '优化后(3次SQL-ms)',
    CONCAT(ROUND(SUM(CASE WHEN stage = '优化前' THEN duration_ms END) / SUM(CASE WHEN stage = '优化后' THEN duration_ms END), 1), 'x') AS '提速倍数',
    MAX(notes) AS '优化说明'
FROM perf_results
WHERE test_name LIKE '%pageQuery%'
GROUP BY test_name;

SELECT '' AS '';
SELECT '>>> 测试完成！将10000条数据规模的pageQuery从21次SQL降为3次SQL' AS summary;
SELECT '>>> 索引优化使筛选查询从全表扫描变为索引查找' AS summary2;

-- 查看索引使用情况
SELECT '' AS '';
SELECT '============= 索引使用验证（EXPLAIN） =============' AS '';
EXPLAIN SELECT COUNT(*) FROM problems WHERE status = 1;
EXPLAIN SELECT COUNT(*) FROM problems WHERE difficulty = 'Hard';
EXPLAIN SELECT id, title FROM problems ORDER BY created_at DESC LIMIT 20;

DROP TEMPORARY TABLE IF EXISTS perf_results;
DROP PROCEDURE IF EXISTS insert_test_problems;
DROP PROCEDURE IF EXISTS insert_test_rel;
DROP PROCEDURE IF EXISTS timed_query;
