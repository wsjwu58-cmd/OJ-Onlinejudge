CREATE TABLE IF NOT EXISTS `malicious_code_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `problem_id` int DEFAULT NULL COMMENT '题目ID',
  `language` varchar(50) DEFAULT NULL COMMENT '编程语言',
  `code` text COMMENT '恶意代码内容',
  `detection_reason` varchar(500) DEFAULT NULL COMMENT '检测原因',
  `matched_pattern` varchar(500) DEFAULT NULL COMMENT '匹配的模式',
  `severity` int DEFAULT NULL COMMENT '严重程度(1-低,2-中,3-高)',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='恶意代码检测日志表';
