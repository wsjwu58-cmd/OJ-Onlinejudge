CREATE TABLE users (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                       password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
                       nickname VARCHAR(100) DEFAULT '' COMMENT '昵称',
                       email VARCHAR(100) UNIQUE COMMENT '邮箱', -- 可选，用于找回密码等
                       avatar_url TEXT COMMENT '头像URL',
                       role ENUM('student', 'teacher', 'admin') NOT NULL DEFAULT 'student' COMMENT '用户角色',
                       status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态: 1-启用, 0-禁用',
                       points INT NOT NULL DEFAULT 0 COMMENT '积分',
                       rating INT NOT NULL DEFAULT 0 COMMENT '竞赛评分',
                       daily_question_streak INT NOT NULL DEFAULT 0 COMMENT '连续刷题天数',
                       total_submissions INT NOT NULL DEFAULT 0 COMMENT '总提交次数',
                       last_login_time TIMESTAMP NULL COMMENT '上次登录时间',
                       vip_expire_time TIMESTAMP NULL COMMENT 'VIP过期时间',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       INDEX idx_username (username),
                       INDEX idx_email (email),
                       INDEX idx_status (status),
                       INDEX idx_last_login (last_login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE problem_types (
                               id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                               name VARCHAR(100) NOT NULL UNIQUE COMMENT '类型名称 (如: 数组, 动态规划, 数据库, 链表)',
                               description TEXT COMMENT '类型描述',
                               is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
                               sort_order INT NOT NULL DEFAULT 999 COMMENT '排序权重，数值越小越靠前',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               INDEX idx_name (name),

                               INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型表';

CREATE TABLE problems (
                          id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(255) NOT NULL COMMENT '题目标题',
                          content LONGTEXT NOT NULL COMMENT '题目描述 (Markdown格式)',
                          difficulty ENUM('Easy', 'Medium', 'Hard') NOT NULL COMMENT '题目难度',
                          acceptance DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '通过率 (%)，缓存字段',
                          frequency ENUM('Low', 'Medium', 'High') DEFAULT 'Medium' COMMENT '出现频率',
                          problem_type ENUM('Algorithm', 'Database', 'Shell', 'Concurrency') NOT NULL DEFAULT 'Algorithm' COMMENT '题目大类',
                          time_limit_ms INT NOT NULL DEFAULT 1000 COMMENT '时间限制 (毫秒)',
                          memory_limit_mb INT NOT NULL DEFAULT 256 COMMENT '内存限制 (MB)',
                          likes_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
                          dislikes_count INT NOT NULL DEFAULT 0 COMMENT '点踩数',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '题目状态: 1-上架, 0-下架',
    -- type_id INT UNSIGNED DEFAULT NULL COMMENT '所属具体类型ID，关联 problem_types.id', -- 移除此行
                          template_code JSON COMMENT '不同语言的代码模板',
                          db_schema TEXT COMMENT '数据库题的表结构SQL',
                          db_init_data JSON COMMENT '数据库题的初始化数据',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          INDEX idx_difficulty (difficulty),
                          INDEX idx_status (status),
                          INDEX idx_acceptance (acceptance),
                          INDEX idx_frequency (frequency),
                          INDEX idx_problem_type (problem_type)
    -- INDEX idx_type_id (type_id) -- 移除此行
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

CREATE TABLE problem_types_rel (
                                   id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                   problem_id INT UNSIGNED NOT NULL COMMENT '题目ID',
                                   type_id INT UNSIGNED NOT NULL COMMENT '类型ID',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 移除外键约束
                                   UNIQUE KEY uk_problem_type (problem_id, type_id), -- 防止同一题目重复添加同一个类型
                                   INDEX idx_problem_id (problem_id),
                                   INDEX idx_type_id (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目类型关联表';

CREATE TABLE submissions (
                             id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                             problem_id INT UNSIGNED NOT NULL COMMENT '题目ID',
                             code TEXT NOT NULL COMMENT '提交的代码',
                             language VARCHAR(20) NOT NULL COMMENT '编程语言',
                             status ENUM('Pending', 'Judging', 'Accepted', 'Wrong Answer', 'Time Limit Exceeded', 'Memory Limit Exceeded', 'Runtime Error', 'Compile Error') NOT NULL DEFAULT 'Pending' COMMENT '判题状态',
                             runtime_ms INT DEFAULT NULL COMMENT '运行时间 (毫秒)',
                             memory_kb INT DEFAULT NULL COMMENT '内存消耗 (KB)',
                             test_cases_passed INT DEFAULT NULL COMMENT '通过的测试用例数',
                             test_cases_total INT DEFAULT NULL COMMENT '总测试用例数',
                             error_info TEXT COMMENT '错误信息',
                             ip_address VARCHAR(45) COMMENT '提交IP地址',
                             submit_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             INDEX idx_user_id (user_id),
                             INDEX idx_problem_id (problem_id),
                             INDEX idx_status (status),
                             INDEX idx_submit_time (submit_time),
                             INDEX idx_user_problem (user_id, problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提交记录表';

CREATE TABLE user_attendances (
                                  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                  date DATE NOT NULL COMMENT '签到日期',
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  UNIQUE KEY uk_user_date (user_id, date),
                                  INDEX idx_user_id (user_id),
                                  INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到表';

CREATE TABLE contests (
                          id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(255) NOT NULL COMMENT '比赛名称',
                          description TEXT COMMENT '比赛描述',
                          start_time TIMESTAMP NOT NULL COMMENT '开始时间',
                          end_time TIMESTAMP NOT NULL COMMENT '结束时间',
                          type ENUM('Weekly Contest', 'Biweekly Contest', 'Mock Interview', 'Company Contest') NOT NULL COMMENT '比赛类型',
                          status ENUM('Upcoming', 'Running', 'Ended') NOT NULL DEFAULT 'Upcoming' COMMENT '比赛状态',
                          created_by BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          INDEX idx_start_time (start_time),
                          INDEX idx_end_time (end_time),
                          INDEX idx_status (status),
                          INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛表';

CREATE TABLE contest_problems (
                                  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                  contest_id INT UNSIGNED NOT NULL COMMENT '比赛ID',
                                  problem_id INT UNSIGNED NOT NULL COMMENT '题目ID',
                                  score INT NOT NULL DEFAULT 100 COMMENT '该题分数',
                                  sort_order TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '题目在比赛中的顺序',
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  UNIQUE KEY uk_contest_problem (contest_id, problem_id),
                                  INDEX idx_contest_id (contest_id),
                                  INDEX idx_problem_id (problem_id),
                                  INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛题目关联表';

CREATE TABLE problem_groups (
                                id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                title VARCHAR(255) NOT NULL COMMENT '题组标题',
                                description TEXT COMMENT '题组描述',

                                creator_id BIGINT UNSIGNED NOT NULL COMMENT '创建者ID，关联 users.id',
    -- 移除 type_id 字段，因为我们使用关联表

                                difficulty_range VARCHAR(50) DEFAULT NULL COMMENT '难度范围 (如: Easy-Medium, All)',
                                estimated_duration_minutes INT DEFAULT NULL COMMENT '预计完成时长 (分钟)',

                                is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开',
                                status TINYINT NOT NULL DEFAULT 1 COMMENT '题组状态: 1-启用, 0-禁用',

                                view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
                                like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
                                join_count INT NOT NULL DEFAULT 0 COMMENT '加入/练习人数',

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 移除外键约束
    -- FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE RESTRICT,

                                INDEX idx_creator_id (creator_id),
    -- 不再需要 idx_type_id
                                INDEX idx_status (status),
                                INDEX idx_is_public (is_public),
                                INDEX idx_view_count (view_count),
                                INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组表';

CREATE TABLE group_types_rel (
                                 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                 group_id INT UNSIGNED NOT NULL COMMENT '题组ID，关联 problem_groups.id',
                                 type_id INT UNSIGNED NOT NULL COMMENT '类型ID，关联 problem_types.id',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 移除外键约束
    -- FOREIGN KEY (group_id) REFERENCES problem_groups(id) ON DELETE CASCADE,
    -- FOREIGN KEY (type_id) REFERENCES problem_types(id) ON DELETE CASCADE,

                                 UNIQUE KEY uk_group_type (group_id, type_id), -- 防止重复关联
                                 INDEX idx_group_id (group_id),
                                 INDEX idx_type_id (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组类型关联表';

CREATE TABLE group_problems (
                                id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                group_id INT UNSIGNED NOT NULL COMMENT '题组ID，关联 problem_groups.id',
                                problem_id INT UNSIGNED NOT NULL COMMENT '题目ID，关联 problems.id',

                                sort_order TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '题目在题组中的顺序，数值越小越靠前',
                                score INT NOT NULL DEFAULT 100 COMMENT '该题在题组中的分数 (可选)',

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 移除外键约束
    -- FOREIGN KEY (group_id) REFERENCES problem_groups(id) ON DELETE CASCADE,
    -- FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,

                                UNIQUE KEY uk_group_problem (group_id, problem_id),
                                INDEX idx_group_id (group_id),
                                INDEX idx_problem_id (problem_id),
                                INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题组题目关联表';

CREATE TABLE test_cases (
                            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                            problem_id INT UNSIGNED NOT NULL COMMENT '关联的题目ID',

                            input_data TEXT NOT NULL COMMENT '测试用例的输入数据 (JSON格式或纯文本)',
                            output_data TEXT NOT NULL COMMENT '测试用例的期望输出数据 (JSON格式或纯文本)',

                            is_sample BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为示例用例 (通常在题目描述中展示给用户)',
                            order_num TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用例执行顺序，数值越小越先执行',

                            time_limit_ms INT DEFAULT NULL COMMENT '该用例的时间限制 (毫秒)，若为空则使用题目的默认值',
                            memory_limit_mb INT DEFAULT NULL COMMENT '该用例的内存限制 (MB)，若为空则使用题目的默认值',

                            score_weight DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '该用例的分数权重，用于计算总分',

                            status TINYINT NOT NULL DEFAULT 1 COMMENT '用例状态: 1-启用, 0-禁用 (可用于临时关闭某个有问题的用例)',

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 移除外键约束 (如前所述)
    -- FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,

                            INDEX idx_problem_id (problem_id),
                            INDEX idx_is_sample (is_sample),
                            INDEX idx_order_num (order_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目测试用例表';
select pt.* from problems p INNER JOIN
                 problem_types_rel ptr ON  ptr.problem_id=2
                            INNER JOIN
                 problem_types pt ON ptr.type_id = pt.id

INSERT INTO test_case (problem_id, input_data, output_data, is_sample, order_num, status)
VALUES
    (2, '4\n2 7 11 15\n9', '0 1', 1, 1, 1),
    (2, '3\n3 2 4\n6', '1 2', 1, 2, 1);

CREATE TABLE `solution_comment` (
                                    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `type`        TINYINT      NOT NULL                COMMENT '类型：1-题解  2-评论',
                                    `problem_id`  BIGINT       NOT NULL                COMMENT '关联题目ID',
                                    `user_id`     BIGINT       NOT NULL                COMMENT '发布者用户ID',
                                    `parent_id`   BIGINT       DEFAULT 0               COMMENT '父级ID（0表示顶级；评论回复时指向被回复的记录ID）',
                                    `reply_to_user_id` BIGINT  DEFAULT NULL            COMMENT '被回复的用户ID（仅评论回复时有值）',
                                    `title`       VARCHAR(200) DEFAULT NULL            COMMENT '标题（题解必填，评论为空）',
                                    `content`     TEXT         NOT NULL                COMMENT '正文内容（题解为富文本/Markdown，评论为纯文本）',
                                    `like_count`  INT          DEFAULT 0               COMMENT '点赞数',
                                    `comment_count` INT        DEFAULT 0               COMMENT '评论数（题解下的评论总数，评论本身为0）',
                                    `view_count`  INT          DEFAULT 0               COMMENT '浏览量（仅题解有意义）',
                                    `status`      TINYINT      DEFAULT 1               COMMENT '状态：0-隐藏/审核中  1-正常  2-置顶',
                                    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
                                    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_problem_type` (`problem_id`, `type`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题解与评论表';