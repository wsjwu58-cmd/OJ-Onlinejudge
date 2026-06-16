-- =============================================
-- 用户服务 - 性能测试数据生成与基准测试
-- =============================================

-- 1. 执行索引创建
SOURCE oj-user-service/src/main/resources/sql/user_optimization_indexes.sql;

-- 2. 插入10000+用户测试数据
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS generate_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
        INSERT INTO `user` (username, password_hash, nickname, email, role, status, created_at, updated_at)
        VALUES (
            CONCAT('testuser_', i),
            CONCAT('hash_', SHA2(CONCAT('password_', i), 256)),
            CONCAT('测试用户', i),
            CONCAT('user', i, '@test.com'),
            CASE (i % 10)
                WHEN 0 THEN 'admin'
                WHEN 1 THEN 'teacher'
                ELSE 'student'
            END,
            CASE (i % 20) WHEN 0 THEN 0 ELSE 1 END,
            DATE_ADD('2024-01-01', INTERVAL i MINUTE),
            DATE_ADD('2024-01-01', INTERVAL i MINUTE)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 3. 性能对比测试

-- 测试1：按角色筛选（应该使用 idx_role）
SELECT SQL_NO_CACHE COUNT(*) FROM `user` WHERE role = 'admin'; -- 391
SELECT SQL_NO_CACHE COUNT(*) FROM `user` WHERE role = 'student';

-- 测试2：按状态筛选（应该使用 idx_status）
SELECT SQL_NO_CACHE COUNT(*) FROM `user` WHERE status = 1;
SELECT SQL_NO_CACHE COUNT(*) FROM `user` WHERE status = 0;

-- 测试3：邮箱查询（应该使用 idx_email）
SELECT SQL_NO_CACHE * FROM `user` WHERE email = 'user5000@test.com';

-- 测试4：组合查询（idx_role + idx_status）
SELECT SQL_NO_CACHE COUNT(*) FROM `user` WHERE role = 'student' AND status = 1;
