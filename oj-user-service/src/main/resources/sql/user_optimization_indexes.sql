-- =============================================
-- 用户服务 - 索引优化
-- 微服务优化方向文档 2.3节
-- =============================================

-- user表缺失的关键索引
-- 管理端按角色筛选
ALTER TABLE `user` ADD INDEX `idx_role` (`role`);

-- 账户状态筛选
ALTER TABLE `user` ADD INDEX `idx_status` (`status`);

-- 邮箱登录/找回密码
ALTER TABLE `user` ADD INDEX `idx_email` (`email`);
