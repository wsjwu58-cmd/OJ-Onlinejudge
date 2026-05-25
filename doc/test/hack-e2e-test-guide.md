# Hack 功能端到端测试手册

## 环境信息

| 组件 | 地址/值 | 说明 |
|------|---------|------|
| MySQL | localhost:3306 / root / qwer1234 | 4个库 |
| Redis | 192.168.141.128:6378 / qwer1234 | |
| RocketMQ | 192.168.141.128:9876 | |
| Judge0 | 192.168.141.128:2358 | |
| Gateway | localhost:8080 | |
| user-service | localhost:8081 | 库 oj_user_db |
| problem-service | localhost:8082 | 库 oj_problem_db |
| contest-service | localhost:8083 | 库 oj_contest_db |
| judge-service | localhost:8084 | 库 oj_judge_db |

## 测试场景：A+B 问题 Hack

- **选手A（hacker）**：提交正确的AC代码 → 锁定题目
- **选手B（target）**：提交有bug的代码（能通过样例但特定输入会错）→ 也能AC（因为原题只有简单样例）→ 被Hack

---

## 第一步：环境准备 — 创建磁盘文件

创建目录和文件：

```
E:\oj-microservice\hack-data\problem-1\
├── validator.cpp       # C++ 校验器源码
├── validator.exe       # 预编译（首次运行时由 ValidatorRunner 自动通过 Judge0 编译）
└── reference.cpp       # 标准解答源码
```

### validator.cpp — 校验 Hack 输入合法性

```cpp
#include <iostream>
using namespace std;
int main() {
    int a, b;
    if (!(cin >> a >> b)) return 1;              // 输入格式错误
    if (a < -1000 || a > 1000) return 2;         // 超出范围
    if (b < -1000 || b > 1000) return 3;
    char c;
    if (cin >> c) return 4;                      // 多余字符
    return 0;                                    // 合法
}
```

### reference.cpp — 标准解答（本题正确答案）

```cpp
#include <iostream>
using namespace std;
int main() {
    int a, b;
    cin >> a >> b;
    cout << a + b << endl;
    return 0;
}
```

> **注意**：先用 g++ 手动编译验证 reference.cpp 正确性：
> ```
> g++ -O2 -std=c++17 -o reference reference.cpp
> echo "3 7" | ./reference    # 应输出: 10
> ```

---

## 第二步：数据库初始化

### 2.1 oj_user_db.user 表 — 创建2个测试用户

```sql
USE oj_user_db;

-- 选手A (hacker, id=1001)
INSERT INTO `user` (`id`, `username`, `password_hash`, `nickname`, `role`, `status`)
VALUES (1001, 'hacker1001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'HackerA', 'student', 1);

-- 选手B (target, id=1002)
INSERT INTO `user` (`id`, `username`, `password_hash`, `nickname`, `role`, `status`)
VALUES (1002, 'target1002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'TargetB', 'student', 1);
```

> 密码哈希为 `123456` 的 BCrypt 值（实际测试请用登录接口注册或使用已知哈希值）

### 2.2 oj_problem_db — 创建题目 + 测试用例 + Hack 字段

```sql
USE oj_problem_db;

-- 先执行 DDL（如果尚未执行）
ALTER TABLE problems ADD COLUMN validator_path     VARCHAR(512) COMMENT 'C++校验器源码磁盘路径';
ALTER TABLE problems ADD COLUMN validator_exe_path VARCHAR(512) COMMENT 'C++校验器编译产物路径';
ALTER TABLE problems ADD COLUMN validator_src_hash VARCHAR(64)  COMMENT 'validator.cpp SHA-256值';
ALTER TABLE problems ADD COLUMN reference_path     VARCHAR(512) COMMENT '标准解答文件磁盘路径';
ALTER TABLE problems ADD COLUMN reference_language VARCHAR(32)  DEFAULT 'C++' COMMENT '标准解答语言';
ALTER TABLE test_cases ADD COLUMN source_hack_id   BIGINT DEFAULT NULL COMMENT '来源Hack记录ID';

-- 创建测试题目 (id=1, A+B Problem)
INSERT INTO `problems` (`id`, `title`, `content`, `difficulty`, `problem_type`,
    `time_limit_ms`, `memory_limit_mb`, `status`,
    `validator_path`, `validator_exe_path`, `validator_src_hash`,
    `reference_path`, `reference_language`)
VALUES (1,
    'A+B Problem (Hack Test)',
    '## 题目描述\n\n给定两个整数 a 和 b，输出 a+b 的值。\n\n## 输入\n\n一行两个整数 a, b (-1000 <= a, b <= 1000)\n\n## 输出\n\na+b 的值',
    'Easy', 'Algorithm',
    1000, 256, 1,
    'E:\\oj-microservice\\hack-data\\problem-1\\validator.cpp',
    'E:\\oj-microservice\\hack-data\\problem-1\\validator.exe',
    '',
    'E:\\oj-microservice\\hack-data\\problem-1\\reference.cpp',
    'C++'
);

-- 插入2个简单测试用例（让有bug的代码也能通过，从而"AC"）
INSERT INTO `test_cases` (`problem_id`, `input_data`, `output_data`, `is_sample`, `order_num`, `score_weight`, `status`)
VALUES
(1, '3 7',    '10', 1, 1, 1.0, 1),   -- 样例1
(1, '100 200', '300', 0, 2, 1.0, 1);  -- 样例2
```

### 2.3 oj_contest_db — 创建比赛 + 关联题目

```sql
USE oj_contest_db;

-- 创建 HackRecord 表（如果尚未执行）
CREATE TABLE IF NOT EXISTS hack_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id      INT NOT NULL,
    problem_id      INT NOT NULL,
    hacker_id       BIGINT NOT NULL,
    target_user_id  BIGINT NOT NULL,
    target_submission_id BIGINT NOT NULL,
    hack_input      MEDIUMTEXT NOT NULL,
    hack_output     MEDIUMTEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'Pending',
    error_info      TEXT,
    target_result   VARCHAR(32),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contest_hacker (contest_id, hacker_id),
    INDEX idx_contest_target (contest_id, target_user_id),
    INDEX idx_contest_problem (contest_id, problem_id)
);

-- 创建测试比赛 (id=1, Running状态, 持续7天)
INSERT INTO `contests` (`id`, `title`, `description`, `start_time`, `end_time`, `type`, `status`, `created_by`)
VALUES (1,
    'Hack 测试赛',
    '用于端到端测试 Hack 功能',
    NOW() - INTERVAL 1 HOUR,      -- 1小时前开始
    NOW() + INTERVAL 7 DAY,       -- 7天后结束
    'Weekly Contest',
    'Running',
    1001   -- created_by = hackerA
);

-- 关联题目 (题目1, 分数100)
INSERT INTO `contest_problems` (`contest_id`, `problem_id`, `score`, `sort_order`)
VALUES (1, 1, 100, 1);
```

### 2.4 选手报名比赛

```sql
USE oj_contest_db;

-- 选手A报名
INSERT INTO `contest_participants` (`contest_id`, `user_id`, `score`, `solved_count`)
VALUES (1, 1001, 0, 0);

-- 选手B报名
INSERT INTO `contest_participants` (`contest_id`, `user_id`, `score`, `solved_count`)
VALUES (1, 1002, 0, 0);
```

### 2.5 初始化 Redis 排行榜

```bash
# 通过 redis-cli 或 API——参赛时 UserContestService.joinContest() 会自动添加
# 如果手动插入 DB，需要手动初始化 Redis ZSet:
redis-cli -h 192.168.141.128 -p 6378 -a qwer1234
ZADD contest:rank:1 0 1001
ZADD contest:rank:1 0 1002
```

---

## 第三步：模拟判题 — 插入选手的 AC 提交记录

> 实际测试时通过前端提交代码会更真实。这里提供 SQL 模拟：选手A提交正确代码、选手B提交有bug代码，"手动标记为AC"

### 选手B的 Buggy 代码（能过样例但负数会错）

```java
// 选手B提交的代码 — BUG: 未处理负数
class Solution {
    public int solve(int a, int b) {
        // BUG: 负数计算错误（但在样例数据 {3,7} 和 {100,200} 下能通过）
        if (a < 0 || b < 0) {
            return a + b + 1;  // 故意 +1，制造错误
        }
        return a + b;
    }
}
```

### 选手A的正确代码

```java
// 选手A的正确代码
class Solution {
    public int solve(int a, int b) {
        return a + b;
    }
}
```

### 插入到 submissions 表

```sql
USE oj_judge_db;

-- 选手B的AC提交 (id=2001, 带bug但过了样例)
INSERT INTO `submissions` (`id`, `user_id`, `problem_id`, `contest_id`, `code`, `language`, `status`,
    `runtime_ms`, `memory_kb`, `test_cases_passed`, `test_cases_total`, `submit_time`)
VALUES (2001, 1002, 1, 1,
    'class Solution {\n    public int solve(int a, int b) {\n        if (a < 0 || b < 0) {\n            return a + b + 1;\n        }\n        return a + b;\n    }\n}',
    'Java', 'Accepted',
    15, 45000, 2, 2, NOW()
);

-- 选手A的AC提交 (id=2002, 正确代码)
INSERT INTO `submissions` (`id`, `user_id`, `problem_id`, `contest_id`, `code`, `language`, `status`,
    `runtime_ms`, `memory_kb`, `test_cases_passed`, `test_cases_total`, `submit_time`)
VALUES (2002, 1001, 1, 1,
    'class Solution {\n    public int solve(int a, int b) {\n        return a + b;\n    }\n}',
    'Java', 'Accepted',
    12, 44000, 2, 2, NOW()
);
```

### 设置 Redis AC 标记（模拟判题服务完成的AC标记）

```bash
redis-cli -h 192.168.141.128 -p 6378 -a qwer1234
SET contest:ac:1:1001:1 "1"
SET contest:ac:1:1002:1 "1"
SET contest:1:user:1001:solved_count "1"
SET contest:1:user:1002:solved_count "1"
ZADD contest:rank:1 100 1001
ZADD contest:rank:1 100 1002
```

---

## 第四步：执行测试流程

### 全部通过 Gateway (localhost:8080) 调用

> 鉴权方式：需先登录获取 token，每次请求携带 `Authorization: Bearer <token>` 头。
> 或者直接在 Header 中传 `X-User-Id: 1001` / `X-User-Role: user` 绕过鉴权（仅开发环境）。

---

### 步骤 1 — 选手A锁定题目

```http
POST http://localhost:8080/api/user/contest/1/problem/1/lock
Header: X-User-Id: 1001
Header: X-User-Role: user
```

**预期响应**：
```json
{ "code": 1, "msg": null, "data": null }
```

**验证**：
```bash
redis-cli -h 192.168.141.128 -p 6378 -a qwer1234
EXISTS contest:lock:1:1001:1
# 应返回: 1
TTL contest:lock:1:1001:1
# 应返回剩余秒数（比赛结束前）
```

---

### 步骤 2 — 选手A查看AC提交列表

```http
GET http://localhost:8080/api/user/contest/1/problem/1/ac-submissions
Header: X-User-Id: 1001
Header: X-User-Role: user
```

**预期响应**（排除自己，只看到选手B的提交）：
```json
{
  "code": 1,
  "data": [
    {
      "id": 2001,
      "userId": 1002,
      "code": "class Solution { ... }",
      "language": "Java",
      "runtimeMs": 15,
      "memoryKb": 45000,
      "submitTime": "2026-05-25T17:00:00"
    }
  ]
}
```

---

### 步骤 3 — 选手A发起Hack

```http
POST http://localhost:8080/api/user/contest/1/hack
Header: X-User-Id: 1001
Header: X-User-Role: user
Content-Type: application/json

{
  "contestId": 1,
  "problemId": 1,
  "targetUserId": 1002,
  "targetSubmissionId": 2001,
  "hackInput": "-5 -3"
}
```

**预期响应**：
```json
{
  "code": 1,
  "msg": null,
  "data": 1        // ← hackId (HackRecord的id)
}
```

**后台发生了什么**：
1. Redis 去重检查 `contest:hack:1:1001:1002:1`
2. 创建 `hack_records` 记录 (status=Pending)
3. Feign 调用 problem-service 获取 `HackAssetsDTO`
4. 发送 `HackTaskMessage` 到 RocketMQ `hack-task-topic`

---

### 步骤 4 — 轮询 Hack 结果（或等5秒后查询）

```http
GET http://localhost:8080/api/user/contest/1/hack/1/result
Header: X-User-Id: 1001
Header: X-User-Role: user
```

**预期响应 — HackSuccess**：
```json
{
  "code": 1,
  "data": {
    "id": 1,
    "contestId": 1,
    "problemId": 1,
    "hackerId": 1001,
    "targetUserId": 1002,
    "targetSubmissionId": 2001,
    "hackInput": "-5 -3",
    "hackOutput": "-8",
    "status": "HackSuccess",
    "targetResult": "Wrong Answer",
    "errorInfo": null,
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

---

## 第五步：验证结果

### 5.1 验证 Redis 排行榜变化

```bash
# 选手A加分 (+100), 选手B扣分 (-100, 保底0)
redis-cli -h 192.168.141.128 -p 6378 -a qwer1234
ZSCORE contest:rank:1 1001       # 应返回: 200 (初始100 + 100)
ZSCORE contest:rank:1 1002       # 应返回: 0   (初始100 - 100, 保底0)
```

### 5.2 验证被攻击者AC标记被清除

```bash
EXISTS contest:ac:1:1002:1       # 应返回: 0 (已清除)
```

### 5.3 验证解题数扣减

```bash
GET contest:1:user:1002:solved_count  # 应返回: 0
```

### 5.4 验证防重复Hack标记

```bash
EXISTS contest:hack:1:1001:1002:1    # 应返回: 1
```

### 5.5 验证 test_cases 表新增记录

```sql
USE oj_problem_db;
SELECT * FROM test_cases WHERE source_hack_id IS NOT NULL;

-- 应返回1条记录：
-- input_data = "-5 -3"
-- output_data = "-8"
-- is_sample = 0
-- source_hack_id = 1
```

### 5.6 验证 hack_records 表

```sql
USE oj_contest_db;
SELECT * FROM hack_records WHERE id = 1;

-- status = 'HackSuccess'
-- target_result = 'Wrong Answer'
-- hack_input = '-5 -3'
-- hack_output = '-8'
```

### 5.7 验证数据库中参赛记录（比赛结束后持久化）

```sql
USE oj_contest_db;
-- 比赛结束后调用 persistRankToDb 会同步，手动检查：
SELECT * FROM contest_participants WHERE contest_id = 1;
-- 选手A: score 应更新为 200, solved_count = 1
-- 选手B: score 应更新为 0, solved_count = 0
```

---

## 第六步：边界测试用例

### 6.1 未锁定就Hack — 应返回错误

```http
POST http://localhost:8080/api/user/contest/1/problem/1/lock
Header: X-User-Id: 1002     # 选手B尝试Hack但没有AC
```
期望：`{ "code": 0, "msg": "请先AC该题目" }`

```http
POST http://localhost:8080/api/user/contest/1/hack
Header: X-User-Id: 1002     # 选手B未锁定就请求 Hack
{ "contestId": 1, "problemId": 1, "targetUserId": 1001, "targetSubmissionId": 2002, "hackInput": "1 2" }
```
期望：`{ "code": 0, "msg": "请先锁定该题目" }`

### 6.2 重复Hack同一人 — 应返回错误

```http
POST http://localhost:8080/api/user/contest/1/hack
Header: X-User-Id: 1001
{ "contestId": 1, "problemId": 1, "targetUserId": 1002, "targetSubmissionId": 2001, "hackInput": "1 1" }
```
期望：`{ "code": 0, "msg": "已对该选手的此题发起过 Hack" }`

### 6.3 Hack 数据被 Validator 拒绝 — InvalidData

```http
POST http://localhost:8080/api/user/contest/1/hack
Header: X-User-Id: 1001
{ "contestId": 1, "problemId": 1, "targetUserId": 1002, "targetSubmissionId": 2001, "hackInput": "abc xyz" }
```
期望：HackRecord status = `InvalidData`（Validator 校验失败）

### 6.4 目标代码通过 Hack 数据 — HackFailed

```http
POST http://localhost:8080/api/user/contest/1/hack
Header: X-User-Id: 1001
{ "contestId": 1, "problemId": 1, "targetUserId": 1001, "targetSubmissionId": 2002, "hackInput": "10 20" }
```
期望：HackRecord status = `HackFailed`（目标代码正确通过）

### 6.5 前端 Hack 弹窗

访问 `http://localhost:5174/contests/1/problems/1`：
1. 提交正确代码 → 判题通过后出现「Hack 挑战」区域
2. 点击「锁定题目」→ AC 提交列表出现
3. 点击「发起Hack」→ 弹窗输入测试数据 → 确认
4. 等待判题 → 弹窗显示 Hack 结果

---

## 故障排查

### RocketMQ 连接失败
确保 `192.168.141.128:9876` 可达，contest-service 的 application.yml 中已配置 rocketmq 地址。

### Judge0 连接失败
确保 `192.168.141.128:2358` 可达，C++ (id=54) 和 Java (id=62) 编译器可用。

### Validator 文件找不到
确保 `E:\oj-microservice\hack-data\problem-1\validator.cpp` 和 `reference.cpp` 文件存在，且路径与数据库中一致。

### 提交代码获取不到 language
确保 HackTaskMessage 中的 targetLanguage 不为空，Consumer 会 fallback 到 submission 记录的 language。

---

## 清理测试数据

```sql
-- 清理所有测试数据
DELETE FROM oj_judge_db.submissions WHERE id IN (2001, 2002);
DELETE FROM oj_contest_db.hack_records WHERE contest_id = 1;
DELETE FROM oj_contest_db.contest_participants WHERE contest_id = 1;
DELETE FROM oj_contest_db.contest_problems WHERE contest_id = 1;
DELETE FROM oj_contest_db.contests WHERE id = 1;
DELETE FROM oj_problem_db.test_cases WHERE problem_id = 1;
DELETE FROM oj_problem_db.problems WHERE id = 1;
DELETE FROM oj_user_db.user WHERE id IN (1001, 1002);
```

```bash
# 清理 Redis
redis-cli -h 192.168.141.128 -p 6378 -a qwer1234
DEL contest:rank:1
DEL contest:ac:1:1001:1
DEL contest:ac:1:1002:1
DEL contest:lock:1:1001:1
DEL contest:hack:1:1001:1002:1
DEL contest:1:user:1001:solved_count
DEL contest:1:user:1002:solved_count
```
