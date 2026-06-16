-- =============================================
-- 题目服务 - 索引优化
-- 微服务优化方向文档 2.3节
-- =============================================

-- problems表缺失的关键索引
-- 按状态筛选是高频操作
ALTER TABLE `problems` ADD INDEX `idx_status` (`status`);

-- 按难度筛选
ALTER TABLE `problems` ADD INDEX `idx_difficulty` (`difficulty`);

-- 列表默认按创建时间排序
ALTER TABLE `problems` ADD INDEX `idx_created_at` (`created_at`);
