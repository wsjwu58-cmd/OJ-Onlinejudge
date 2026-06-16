-- =============================================
-- 竞赛服务 - 索引优化
-- 微服务优化方向文档 2.3节
-- =============================================

-- contests表缺失的关键索引
-- 按竞赛状态筛选
ALTER TABLE `contests` ADD INDEX `idx_status` (`status`);

-- 按时间范围查询
ALTER TABLE `contests` ADD INDEX `idx_start_time` (`start_time`);
