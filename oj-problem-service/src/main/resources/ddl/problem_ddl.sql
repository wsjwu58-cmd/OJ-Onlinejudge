-- =============================================
-- OJ 题目服务数据库建表语句
-- 数据库: exercise (暂共享，后续拆分为 oj_problem)
-- =============================================

-- 题目表
CREATE TABLE IF NOT EXISTS `problems` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `title` VARCHAR(200) NOT NULL COMMENT '题目标题',
    `content` TEXT COMMENT '题目描述(Markdown格式)',
    `difficulty` VARCHAR(20) DEFAULT 'Easy' COMMENT '难度: Easy/Medium/Hard',
    `acceptance` DECIMAL(5,2) DEFAULT 0.00 COMMENT '通过率(%)',
    `frequency` VARCHAR(20) DEFAULT 'Low' COMMENT '出现频率: Low/Medium/High',
    `problem_type` VARCHAR(50) DEFAULT 'Algorithm' COMMENT '题目大类: Algorithm/Database/Shell/Concurrency',
    `time_limit_ms` INT DEFAULT 1000 COMMENT '时间限制(毫秒)',
    `memory_limit_mb` INT DEFAULT 256 COMMENT '内存限制(MB)',
    `likes_count` INT DEFAULT 0 COMMENT '点赞数',
    `dislikes_count` INT DEFAULT 0 COMMENT '点踩数',
    `status` INT DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
    `template_code` JSON COMMENT '代码模板(JSON)',
    `db_schema` TEXT COMMENT '数据库题表结构SQL',
    `db_init_data` TEXT COMMENT '数据库题初始化数据(JSON)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- 题目类型表
CREATE TABLE IF NOT EXISTS `problem_types` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '类型ID',
    `name` VARCHAR(50) NOT NULL COMMENT '类型名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '类型描述',
    `is_active` INT DEFAULT 1 COMMENT '是否激活: 1-是, 0-否',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型表';

-- 题目类型关联表
CREATE TABLE IF NOT EXISTS `problem_types_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `problem_id` INT NOT NULL COMMENT '题目ID',
    `type_id` INT NOT NULL COMMENT '类型ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_problem_id` (`problem_id`),
    KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型关联表';

-- 测试用例表
CREATE TABLE IF NOT EXISTS `test_cases` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用例ID',
    `problem_id` INT NOT NULL COMMENT '关联题目ID',
    `input_data` TEXT COMMENT '输入数据',
    `output_data` TEXT COMMENT '期望输出数据',
    `is_sample` TINYINT(1) DEFAULT 0 COMMENT '是否示例用例',
    `order_num` SMALLINT DEFAULT 0 COMMENT '顺序',
    `time_limit_ms` INT DEFAULT NULL COMMENT '时间限制(可选)',
    `memory_limit_mb` INT DEFAULT NULL COMMENT '内存限制(可选)',
    `score_weight` DOUBLE DEFAULT 1.00 COMMENT '分数权重',
    `status` INT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- 题组(题单)表
CREATE TABLE IF NOT EXISTS `problem_groups` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '题组ID',
    `title` VARCHAR(200) NOT NULL COMMENT '题组标题',
    `description` TEXT COMMENT '题组描述',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `difficulty_range` VARCHAR(50) DEFAULT 'All' COMMENT '难度范围',
    `estimated_duration_minutes` INT DEFAULT NULL COMMENT '预计完成时长(分钟)',
    `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
    `status` INT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `join_count` INT DEFAULT 0 COMMENT '加入/练习人数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组(题单)表';

-- 题组类型关联表
CREATE TABLE IF NOT EXISTS `group_types_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `group_id` INT NOT NULL COMMENT '题组ID',
    `type_id` INT NOT NULL COMMENT '类型ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组类型关联表';

-- 题组题目关联表
CREATE TABLE IF NOT EXISTS `group_problems` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `group_id` INT NOT NULL COMMENT '题组ID',
    `problem_id` INT NOT NULL COMMENT '题目ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `score` INT DEFAULT 10 COMMENT '分数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组题目关联表';
