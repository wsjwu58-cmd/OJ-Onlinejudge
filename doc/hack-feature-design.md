# Hack 功能设计文档

## 1. 功能概述

竞赛期间，选手在正确提交（AC）题目后可将该题目**锁定**（Lock），锁定后：
- **不可再提交**该题目的答案
- **可查看**同赛场其他 AC 选手的代码
- **可发起 Hack**：针对某个 AC 选手的代码，提交一组特定的测试数据，证明其代码存在逻辑漏洞或边界处理错误

Hack 成功则：发起者得分（+题目分数），被攻击者扣分（-题目分数），排行榜更新，测试用例加入题目题库。

## 2. 架构设计

### 2.1 涉及服务

| 服务 | 职责 |
|------|------|
| **oj-problem-service** | Problem 表新增 `validator_path`、`validator_exe_path`、`reference_path`、`reference_language`；管理 Hack 产生的测试用例；Validator 编译与缓存 |
| **oj-contest-service** | 锁定/解锁、AC 代码列表、Hack 记录管理、排行榜原子更新（Lua 脚本） |
| **oj-judge-service** | Hack 异步判题（Validator + Target + Reference 三方运行），结果回传 |
| **oj-common-api** | 新增 Hack 相关 Feign 接口 |

### 2.2 数据流总览

```
选手A锁定题目 → 查看选手B的AC代码 → 构造Hack数据 → 提交Hack
  │
  ▼
[judge-service] Hack判题流程:
  ① 检查 Validator .exe 编译缓存 → 无缓存或源码变更则通过 Judge0 编译
  ② 在 Judge0 沙箱运行 C++ Validator，校验 Hack 数据合法性
  ③ 若合法 → 运行目标代码 (选手B) + Hack数据 → 记录结果
  ④ 运行标准解答 (C++优先) + Hack数据 → 获取预期输出
  ⑤ 判定: 目标代码失败 且 标准解答通过 → HackSuccess
  ⑥ 结果通过 MQ 回传 contest-service
  │
  ▼
[contest-service] 处理结果:
  ⑥ 写 Hack 记录到 MySQL
  ⑦ 更新 Redis 排行榜 (ZINCRBY hacker +score, hacked -score)
  ⑧ 通知 problem-service 添加测试用例
  ⑨ WebSocket 广播 Hack 结果
```

### 2.3 新增 RocketMQ Topic

```java
// 在 oj-common MqConstant 中新增
public static final String HACK_TASK_TOPIC              = "hack-task-topic";
public static final String HACK_TASK_DEAD_LETTER_TOPIC  = "hack-task-dead-letter-topic";
public static final String HACK_RESULT_TOPIC            = "hack-result-topic";
public static final String HACK_TASK_CONSUMER_GROUP     = "hack-task-consumer-group";
public static final String HACK_TASK_DLQ_CONSUMER_GROUP = "hack-task-dead-letter-consumer-group";
public static final String HACK_RESULT_CONSUMER_GROUP   = "hack-result-consumer-group";
```

**死信策略**：HackTaskConsumer 重试 3 次耗尽后进入 DLQ，由 `HackTaskDeadLetterConsumer` 消费并更新记录状态为 `SystemError`。

## 3. 数据库设计

### 3.1 Problem 表新增字段 (oj-problem-service)

```sql
ALTER TABLE problems ADD COLUMN validator_path     VARCHAR(512) COMMENT 'C++校验器源码磁盘路径';
ALTER TABLE problems ADD COLUMN validator_exe_path VARCHAR(512) COMMENT 'C++校验器编译产物路径(首次编译生成，后续复用)';
ALTER TABLE problems ADD COLUMN validator_src_hash VARCHAR(64)  COMMENT 'validator.cpp 的 SHA-256 值，用于缓存失效判断';
ALTER TABLE problems ADD COLUMN reference_path     VARCHAR(512) COMMENT '标准解答文件磁盘路径';
ALTER TABLE problems ADD COLUMN reference_language VARCHAR(32)  DEFAULT 'C++' COMMENT '标准解答语言(C++/Java/Python)';
```

> C++ 文件落盘存储，不在 DB 存源码。文件按 `hack-data/contest-{contestId}/problem-{problemId}/` 目录组织：
>
> ```
> oj-microservice/
> └── hack-data/
>     └── contest-{contestId}/
>         └── problem-{problemId}/
>             ├── validator.cpp       # C++ 校验器源码（管理员上传）
>             ├── validator.exe       # 预编译产物（首次编译生成，Hash 不变则复用）
>             └── reference.cpp       # 标准解答源码（扩展名按实际语言）
> ```
>
> **编译缓存**：管理员上传 validator.cpp 时触发编译，产物 validator.exe 持久化到同目录。
> 每次 Hack 判题前校验源码 SHA-256 与 `validator_src_hash` 是否一致；一致则跳过编译直接用 .exe，不一致则重新编译并更新 Hash。

### 3.2 新增 HackRecord 表 (oj-contest-service)

```sql
CREATE TABLE hack_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id      INT NOT NULL COMMENT '比赛ID',
    problem_id      INT NOT NULL COMMENT '题目ID',
    hacker_id       BIGINT NOT NULL COMMENT '发起Hack的用户ID',
    target_user_id  BIGINT NOT NULL COMMENT '被Hack的目标用户ID',
    target_submission_id BIGINT NOT NULL COMMENT '被攻击的提交记录ID',
    hack_input      MEDIUMTEXT NOT NULL COMMENT 'Hack测试数据(输入)',
    hack_output     MEDIUMTEXT COMMENT '标准解答的输出结果',
    status          VARCHAR(32) NOT NULL DEFAULT 'Pending' COMMENT 'Pending/Validating/HackSuccess/HackFailed/InvalidData/SystemError',
    error_info      TEXT COMMENT '错误信息(InvalidData原因/Runtime Error等)',
    target_result   VARCHAR(32) COMMENT '目标代码运行结果(Accepted/Wrong Answer/TLE等)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contest_hacker (contest_id, hacker_id),
    INDEX idx_contest_target (contest_id, target_user_id),
    INDEX idx_contest_problem (contest_id, problem_id)
);
```

### 3.3 TestCase 表新增字段 (oj-problem-service)

```sql
ALTER TABLE test_cases ADD COLUMN source_hack_id BIGINT DEFAULT NULL COMMENT '来源Hack记录ID(NULL=原始测试用例)';
```

## 4. Redis Key 设计

| Key | 类型 | 说明 |
|-----|------|------|
| `contest:lock:{contestId}:{userId}:{problemId}` | String | 锁定标记，TTL=比赛结束时间 |
| `contest:hack:{contestId}:{hackerId}:{targetUserId}:{problemId}` | String | 防止重复Hack同一人的同一题 |
| `contest:hack:pending:{hackId}` | String | Hack判题令牌(Pending状态) |
| `contest:{contestId}:user:{userId}:solved_count` | String | 用户解题数(已有，需支持扣减) |

### 4.1 Lock 机制

锁定条件：用户在该比赛的该题目上已有 AC 记录。锁定后通过 Redis String 标记，TTL 设为比赛结束时间，防止赛后继续查看代码。

```java
// 锁定
stringRedisTemplate.opsForValue().set(
    "contest:lock:" + contestId + ":" + userId + ":" + problemId, "1",
    Duration.between(LocalDateTime.now(), contestEndTime).getSeconds(), TimeUnit.SECONDS);
```

## 5. API 设计

### 5.1 用户端 (oj-contest-service /user/contest/)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/contest/{contestId}/problem/{problemId}/lock` | 锁定题目 |
| POST | `/user/contest/{contestId}/problem/{problemId}/unlock` | 解锁题目(手动) |
| GET | `/user/contest/{contestId}/problem/{problemId}/ac-submissions` | 获取AC提交列表(含代码) |
| POST | `/user/contest/{contestId}/hack` | 提交Hack |
| GET | `/user/contest/{contestId}/hack/{hackId}/result` | 查询Hack结果 |
| GET | `/user/contest/{contestId}/hack/records` | 查询用户Hack记录列表 |

### 5.2 内部调用 (oj-common-api Feign)

**ProblemClient 新增** — 获取 Hack 资源 + 添加测试用例：

```java
@GetMapping("/internal/problem/{problemId}/hack-assets")
Result<HackAssetsDTO> getHackAssets(@PathVariable("problemId") Integer problemId);

@PostMapping("/internal/problem/test-case/hack")
Result<Void> addHackTestCase(@RequestParam("problemId") Integer problemId,
                              @RequestParam("sourceHackId") Long sourceHackId,
                              @RequestParam("inputData") String inputData,
                              @RequestParam("outputData") String outputData);
```

**HackAssetsDTO** (存放于 `oj-common-api` `dto/`):

```java
@Data
public class HackAssetsDTO implements Serializable {
    private String validatorPath;      // C++ 校验器源码路径
    private String validatorExePath;   // C++ 校验器编译产物路径(预编译缓存)
    private String validatorSrcHash;   // 源码 SHA-256，判题时比对决定是否重编译
    private String referencePath;      // 标准解答磁盘路径
    private String referenceLanguage;  // C++ / Java / Python
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
}
```

> **Judge 服务读取 HackAssetsDTO 时的逻辑**：
> 1. 若 `validatorExePath` 存在且 `validatorSrcHash` 与当前源码 SHA-256 一致 → 直接 `ProcessBuilder ./validator.exe` 运行
> 2. 否则 → 通过 Judge0 沙箱编译 `validatorPath` → 保存产物到 `validatorExePath` → 更新 `validatorSrcHash`

**ContestClient 新增** — Hack 排名更新：

```java
@PostMapping("/internal/contest/rank/hack")
Result<Void> updateRankOnHackSuccess(@RequestParam("contestId") Integer contestId,
                                      @RequestParam("hackerId") Long hackerId,
                                      @RequestParam("targetUserId") Long targetUserId,
                                      @RequestParam("problemId") Integer problemId,
                                      @RequestParam("score") Integer score);
```

### 5.3 HackSubmitDTO（用户端请求）

```java
@Data
public class HackSubmitDTO {
    private Integer contestId;
    private Integer problemId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String hackInput;          // 测试数据(输入)
}
```

## 6. C++ Validator 与标准解答规范

### 6.1 Validator 规范

Validator 是一个 C++ 程序，从 **stdin** 读取 Hack 测试数据，验证其是否符合题目输入约束。

- **输入**: stdin — Hack 测试数据（即攻击者构造的输入）
- **输出**: stdout — 可选错误描述（仅用于日志）
- **退出码**: `0` = 数据合法，`非0` = 数据非法

> **约定**: 服务端使用 GCC 编译，Validator 中不得引入 `fork()`、网络、文件系统操作。必须使用标准 C++11/14/17 库。编译命令: `g++ -O2 -std=c++17 -o validator validator.cpp`。

**示例（A+B Problem 校验器）**:
```cpp
#include <iostream>
using namespace std;
int main() {
    long long a, b;
    if (!(cin >> a >> b)) return 1;
    // 检查数据范围
    if (a < -1e9 || a > 1e9 || b < -1e9 || b > 1e9) return 2;
    // 检查输入流是否干净
    char c;
    if (cin >> c) return 3;
    return 0;
}
```

### 6.2 标准解答规范

标准解答（Reference Solution）是出题人提供的正确题解，**支持 C++ / Java / Python**。Hack 判定时用标准解答运行 Hack 数据作为正确答案基准。

- **推荐 C++** 作为标答语言（编译型，启动快，单次 Hack 判题百毫秒级）
- **不推荐 Java**（JVM 冷启动慢，增加延迟；仅在题目原本就是纯 Java 体系时使用）
- 必须通过题目所有现有测试用例（已在出题时验证）
- 语言由 `reference_language` 字段动态指定，Judge0 自动识别

## 7. Hack 判题流程（详细）

### 7.1 判题入口

```
HackTaskConsumer 接收 HackTaskMessage(scoped，不含代码)
  │
  ├─ 阶段①: 编译缓存检查 + Validator 校验
  │    ①检查 validatorExePath 是否存在 && 源码 SHA-256 == validatorSrcHash
  │       → 命中缓存: 直接 ProcessBuilder ./validator.exe < hackInput
  │       → 缓存失效: Judge0 编译 → 保存 .exe → 更新 DB Hash
  │    ②Judge0 沙箱运行 Validator(status 3=合法, 6=编译错, 其他=非法)
  │    exitCode != 0 → InvalidData, 结束
  │
  ├─ 阶段②: Feign 拉取目标代码 → Judge0 沙箱运行目标代码
  │    通过 Feign 从 judge-service 按 submissionId 查代码
  │    Judge0.submitAndWait(targetCode, targetLangId, hackInput, ...)
  │    记录 targetStatus
  │
  ├─ 阶段③: 读取标准解答文件 → Judge0 沙箱运行（C++/Java/Python 自适应）
  │    Files.readString(referencePath) → Judge0.submitAndWait(...)
  │    记录 referenceOutput
  │
  └─ 阶段④: 判定 + DLQ 兜底
       targetStatus != Accepted && referenceStatus == Accepted → HackSuccess
       重试耗尽(RocketMQ maxReconsumeTimes=3) → 进入 DLQ → SystemError
```

### 7.2 判定规则

| 目标代码结果 | 标准解答结果 | 判定 |
|-------------|-------------|------|
| Wrong Answer / TLE / Runtime Error | Accepted | **Hack Success** |
| Accepted | Accepted | Hack Failed (目标代码通过) |
| Accepted | 非Accepted | Hack Failed (标准解答异常) |
| — (Validator返回非0) | — | **Invalid Data** |

### 7.3 ValidatorRunner 实现（judge-service）— 编译缓存

C++ Validator 的编译和运行核心类，放置在 `oj-judge-service/src/main/java/com/oj/judge/hack/ValidatorRunner.java`：

核心优化：
- **缓存检查**：优先使用预编译的 `.exe`，校验源码 Hash 一致则跳过编译
- **Judge0 沙箱**：编译和运行均托管给 Judge0 沙箱，安全隔离
- **Hash 比对**：`MessageDigest SHA-256` 计算当前 validator.cpp 的 Hash 与 DB 中 `validator_src_hash` 比对

```java
package com.oj.judge.hack;

import com.alibaba.fastjson.JSONObject;
import com.oj.judge.config.Judge0Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

@Slf4j
@Component
public class ValidatorRunner {

    @Autowired
    private Judge0Client judge0Client;

    private static final int CXX_LANG_ID = 54;
    private static final float VALIDATOR_TIMEOUT_SEC = 5.0f;
    private static final int VALIDATOR_MEMORY_KB = 256 * 1024;

    /**
     * 校验 Hack 数据合法性（优先使用预编译产物）
     */
    public ValidatorResult validate(String validatorPath, String validatorExePath,
                                     String cachedHash, String hackInput) {
        Path sourceFile = Paths.get(validatorPath);
        if (!Files.exists(sourceFile)) {
            return ValidatorResult.invalid("Validator 源码不存在: " + validatorPath);
        }

        String currentHash = sha256(sourceFile);
        Path exeFile = Paths.get(validatorExePath);

        // 缓存命中：exe 存在 且 Hash 未变 → 直接运行
        if (Files.exists(exeFile) && currentHash.equals(cachedHash)) {
            log.info("Validator 编译缓存命中: {}", validatorExePath);
            return runInJudge0Sandbox(Paths.get(validatorPath), hackInput, true);
        }

        // 缓存失效：通过 Judge0 沙箱编译 + 运行
        log.info("Validator 编译缓存未命中，通过 Judge0 编译");
        return runInJudge0Sandbox(Paths.get(validatorPath), hackInput, false);
    }

    private ValidatorResult runInJudge0Sandbox(Path sourceFile, String hackInput, boolean skipCompile) {
        try {
            String cppCode = Files.readString(sourceFile);
            JSONObject result;
            if (skipCompile) {
                // 已有预编译 .exe，但 Judge0 只接受源码。用 compile_only 参数无效时
                // 仍需提交源码让 Judge0 编译运行（编译很快，且沙箱安全）
                result = judge0Client.submitAndWait(cppCode, CXX_LANG_ID,
                        hackInput, null, VALIDATOR_TIMEOUT_SEC, VALIDATOR_MEMORY_KB);
            } else {
                result = judge0Client.submitAndWait(cppCode, CXX_LANG_ID,
                        hackInput, null, VALIDATOR_TIMEOUT_SEC, VALIDATOR_MEMORY_KB);
            }

            JSONObject status = result.getJSONObject("status");
            int statusId = status.getIntValue("id");

            if (statusId == 3) {
                return ValidatorResult.valid(judge0Client.decodeField(result, "stdout"));
            } else if (statusId == 6) {
                String compileErr = judge0Client.decodeField(result, "compile_output");
                return ValidatorResult.invalid("Validator 编译失败: " + compileErr);
            } else {
                String reason = judge0Client.decodeField(result, "stderr");
                if (reason == null || reason.isEmpty()) {
                    reason = judge0Client.decodeField(result, "stdout");
                }
                return ValidatorResult.invalid(
                        "Validator 校验不通过(status=" + statusId + "): " + reason);
            }
        } catch (Exception e) {
            log.error("Validator Judge0 运行异常", e);
            return ValidatorResult.invalid("Validator 运行异常: " + e.getMessage());
        }
    }

    public static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ... ValidatorResult 内部类同上
}
```

**Hash 更新时机**：首次提交 Hack 时若缓存未命中，在编译成功后通过 Feign 回调 `ProblemClient.updateValidatorHash(problemId, exePath, newHash)` 更新 DB 缓存值。

### 7.4 HackTaskConsumer 实现（judge-service）

放置在 `oj-judge-service/src/main/java/com/oj/judge/mq/HackTaskConsumer.java`：

```java
package com.oj.judge.mq;

import com.oj.api.ContestClient;
import com.oj.api.ProblemClient;
import com.oj.common.constant.MqConstant;
import com.oj.judge.config.Judge0Client;
import com.oj.judge.dto.HackResultMessage;
import com.oj.judge.dto.HackTaskMessage;
import com.oj.judge.hack.ValidatorRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.HACK_TASK_TOPIC,
        consumerGroup = MqConstant.HACK_TASK_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class HackTaskConsumer implements RocketMQListener<HackTaskMessage> {

    @Autowired
    private ValidatorRunner validatorRunner;

    @Autowired
    private Judge0Client judge0Client;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(HackTaskMessage msg) {
        log.info("收到 Hack 任务: hackId={}, hacker={}, target={}, problem={}",
                msg.getHackId(), msg.getHackerId(), msg.getTargetUserId(), msg.getProblemId());

        // 阶段①: Validator 校验
        ValidatorRunner.ValidatorResult validation = validatorRunner.validate(
                msg.getValidatorPath(), msg.getHackInput());
        if (!validation.isValid()) {
            sendHackResult(msg, "InvalidData", null, null, validation.getStderr());
            return;
        }

        // 阶段②: 运行目标代码
        int targetLangId = judge0Client.getLanguageId(msg.getTargetLanguage());
        String targetResult;
        try {
            var result = judge0Client.submitAndWait(msg.getTargetCode(), targetLangId,
                    msg.getHackInput(), null,
                    msg.getTimeLimitMs() != null ? msg.getTimeLimitMs() / 1000.0f : 2.0f,
                    msg.getMemoryLimitMb() != null ? msg.getMemoryLimitMb() * 1024 : 256 * 1024);
            targetResult = judge0Client.parseStatus(
                    result.getJSONObject("status").getIntValue("id"));
        } catch (Exception e) {
            sendHackResult(msg, "HackFailed", "Runtime Error", null, "目标代码运行异常: " + e.getMessage());
            return;
        }

        // 阶段③: 运行标准解答（Java）
        if (!"Accepted".equals(targetResult)) {
            String referenceCode;
            try {
                referenceCode = Files.readString(Paths.get(msg.getReferencePath()));
            } catch (Exception e) {
                sendHackResult(msg, "HackFailed", targetResult, null,
                        "标准解答文件读取失败: " + e.getMessage());
                return;
            }

            int refLangId = judge0Client.getLanguageId("Java");
            try {
                var refResult = judge0Client.submitAndWait(referenceCode, refLangId,
                        msg.getHackInput(), null,
                        msg.getTimeLimitMs() != null ? msg.getTimeLimitMs() / 1000.0f : 5.0f,
                        msg.getMemoryLimitMb() != null ? msg.getMemoryLimitMb() * 1024 : 512 * 1024);
                String refStatus = judge0Client.parseStatus(
                        refResult.getJSONObject("status").getIntValue("id"));

                if ("Accepted".equals(refStatus)) {
                    String refOutput = judge0Client.decodeField(refResult, "stdout");
                    sendHackResult(msg, "HackSuccess", targetResult, refOutput, null);
                } else {
                    sendHackResult(msg, "HackFailed", targetResult, null,
                            "标准解答未通过 Hack 数据: " + refStatus);
                }
            } catch (Exception e) {
                sendHackResult(msg, "HackFailed", targetResult, null,
                        "标准解答运行异常: " + e.getMessage());
            }
        } else {
            sendHackResult(msg, "HackFailed", targetResult, null, "目标代码通过了 Hack 测试数据");
        }
    }

    private void sendHackResult(HackTaskMessage msg, String status,
                                 String targetResult, String hackOutput, String errorInfo) {
        HackResultMessage result = HackResultMessage.builder()
                .hackId(msg.getHackId())
                .contestId(msg.getContestId())
                .problemId(msg.getProblemId())
                .hackerId(msg.getHackerId())
                .targetUserId(msg.getTargetUserId())
                .targetSubmissionId(msg.getTargetSubmissionId())
                .status(status)
                .targetResult(targetResult)
                .hackOutput(hackOutput)
                .errorInfo(errorInfo)
                .build();
        rocketMQTemplate.convertAndSend(MqConstant.HACK_RESULT_TOPIC, result);
        log.info("Hack 结果已发送: hackId={}, status={}", msg.getHackId(), status);
    }
}
```

### 7.5 HackResultConsumer 实现（contest-service）

放置在 `oj-contest-service/src/main/java/com/oj/contest/mq/HackResultConsumer.java`：

```java
package com.oj.contest.mq;

import com.oj.api.ProblemClient;
import com.oj.common.constant.MqConstant;
import com.oj.contest.entity.HackRecord;
import com.oj.contest.mapper.HackRecordMapper;
import com.oj.contest.service.UserContestService;
import com.oj.judge.dto.HackResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.HACK_RESULT_TOPIC,
        consumerGroup = MqConstant.HACK_RESULT_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class HackResultConsumer implements RocketMQListener<HackResultMessage> {

    @Autowired
    private HackRecordMapper hackRecordMapper;

    @Autowired
    private UserContestService userContestService;

    @Autowired
    private ProblemClient problemClient;

    @Override
    public void onMessage(HackResultMessage msg) {
        log.info("收到 Hack 结果: hackId={}, status={}", msg.getHackId(), msg.getStatus());

        HackRecord record = HackRecord.builder()
                .id(msg.getHackId())
                .contestId(msg.getContestId())
                .problemId(msg.getProblemId())
                .hackerId(msg.getHackerId())
                .targetUserId(msg.getTargetUserId())
                .targetSubmissionId(msg.getTargetSubmissionId())
                .hackInput(msg.getHackInput())
                .hackOutput(msg.getHackOutput())
                .status(msg.getStatus())
                .errorInfo(msg.getErrorInfo())
                .targetResult(msg.getTargetResult())
                .updatedAt(LocalDateTime.now())
                .build();

        // 更新状态（之前插入时已创建 pending 记录）
        hackRecordMapper.updateById(record);

        if ("HackSuccess".equals(msg.getStatus())) {
            int problemScore = userContestService.getProblemScoreInContest(
                    msg.getContestId(), msg.getProblemId());
            userContestService.updateRankOnHackSuccess(
                    msg.getContestId(), msg.getHackerId(),
                    msg.getTargetUserId(), msg.getProblemId(), problemScore);

            // 通知 problem-service 添加测试用例
            try {
                problemClient.addHackTestCase(msg.getProblemId(), msg.getHackId(),
                        msg.getHackInput(), msg.getHackOutput());
            } catch (Exception e) {
                log.error("添加 Hack 测试用例失败: {}", e.getMessage());
            }
        }

        log.info("Hack 结果处理完成: hackId={}, status={}", msg.getHackId(), msg.getStatus());
    }
}
```

### 7.6 DTO 定义 — 精简化（MQ 只传 ID，不传代码）

**HackTaskMessage** (judge-service) — MQ 消息体（精简，避免代码膨胀 MQ）：

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HackTaskMessage implements Serializable {
    private Long hackId;
    private Integer contestId;
    private Integer problemId;
    private Long hackerId;
    private Long targetUserId;
    private Long targetSubmissionId;  // ← 通过此 ID Feign 拉取目标代码
    private String targetLanguage;    // ← 拉取代码后 Judge0 使用
    private String validatorPath;
    private String validatorExePath;
    private String validatorSrcHash;
    private String referencePath;
    private String referenceLanguage;
    private String hackInput;         // 64KB 上限，可控
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
}
```

> **关键优化**：`targetCode` 不再放入 MQ 消息体。HackTaskConsumer 根据 `targetSubmissionId` 通过 Feign 按需拉取代码，避免大段代码在 MQ 中流转导致消息体膨胀。

**HackResultMessage** (judge-service) — MQ 结果消息体：

```java
package com.oj.judge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HackResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long hackId;
    private Integer contestId;
    private Integer problemId;
    private Long hackerId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String status;        // HackSuccess / HackFailed / InvalidData
    private String targetResult;  // 目标代码运行结果
    private String hackOutput;    // 标准解答输出
    private String hackInput;     // 原始输入（回写记录用）
    private String errorInfo;
}
```

### 7.8 内部 Controller 实现

**ProblemInternalController 新增**（problem-service）：

```java
/**
 * 获取题目 Hack 资源（Validator 路径 + 标准解答路径）
 */
@GetMapping("/{problemId}/hack-assets")
public Result<HackAssetsDTO> getHackAssets(@PathVariable Integer problemId) {
    Problem problem = problemService.getById(problemId);
    if (problem == null) return Result.error("题目不存在");
    HackAssetsDTO dto = new HackAssetsDTO();
    dto.setValidatorPath(problem.getValidatorPath());
    dto.setReferencePath(problem.getReferencePath());
    dto.setReferenceLanguage(problem.getReferenceLanguage());
    dto.setTimeLimitMs(problem.getTimeLimitMs());
    dto.setMemoryLimitMb(problem.getMemoryLimitMb());
    return Result.success(dto);
}

/**
 * Hack 成功后添加测试用例
 */
@PostMapping("/test-case/hack")
public Result<Void> addHackTestCase(@RequestParam Integer problemId,
                                     @RequestParam Long sourceHackId,
                                     @RequestParam String inputData,
                                     @RequestParam String outputData) {
    // 查询最大 orderNum 保证排序
    Integer maxOrder = testCaseMapper.selectMaxOrderNum(problemId);
    TestCase tc = TestCase.builder()
            .problemId(problemId)
            .inputData(inputData)
            .outputData(outputData)
            .isSample(false)
            .orderNum((short) (maxOrder != null ? maxOrder + 1 : 1))
            .sourceHackId(sourceHackId)
            .scoreWeight(1.0)
            .status(1)
            .build();
    testCaseMapper.insert(tc);
    return Result.success();
}
```

**ContestInternalController 新增**（contest-service）：

```java
/**
 * Hack 成功时更新排行榜
 */
@PostMapping("/rank/hack")
public Result<Void> updateRankOnHackSuccess(@RequestParam Integer contestId,
                                             @RequestParam Long hackerId,
                                             @RequestParam Long targetUserId,
                                             @RequestParam Integer problemId,
                                             @RequestParam Integer score) {
    userContestService.updateRankOnHackSuccess(contestId, hackerId, targetUserId, problemId, score);
    return Result.success();
}
```

### 7.9 Hack 提交流程（contest-service 用户端）

```java
@PostMapping("/{contestId}/hack")
public Result<Long> submitHack(@PathVariable Long contestId, @RequestBody HackSubmitDTO dto) {
    Long userId = BaseContext.getCurrentUser().getId();

    // 1. 检查是否已锁定该题
    String lockKey = "contest:lock:" + contestId + ":" + userId + ":" + dto.getProblemId();
    if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
        return Result.error("请先锁定该题目");
    }

    // 2. 防止重复 Hack 同一人的同一题
    String dupKey = "contest:hack:" + contestId + ":" + userId + ":" + dto.getTargetUserId() + ":" + dto.getProblemId();
    if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(dupKey))) {
        return Result.error("已对该选手的此题发起过 Hack");
    }

    // 3. 创建 HackRecord (状态 Pending)
    HackRecord record = HackRecord.builder()
            .contestId(contestId.intValue())
            .problemId(dto.getProblemId())
            .hackerId(userId)
            .targetUserId(dto.getTargetUserId())
            .targetSubmissionId(dto.getTargetSubmissionId())
            .hackInput(dto.getHackInput())
            .status("Pending")
            .build();
    hackRecordMapper.insert(record);

    // 4. 获取题目资源路径
    Result<HackAssetsDTO> assetsResult = problemClient.getHackAssets(dto.getProblemId());
    HackAssetsDTO assets = assetsResult.getData();

    // 5. 获取目标提交代码
    Submission targetSubmission = submissionMapper.selectById(dto.getTargetSubmissionId());

    // 6. 发送 MQ 任务
    HackTaskMessage taskMsg = HackTaskMessage.builder()
            .hackId(record.getId())
            .contestId(contestId.intValue())
            .problemId(dto.getProblemId())
            .hackerId(userId)
            .targetUserId(dto.getTargetUserId())
            .targetSubmissionId(dto.getTargetSubmissionId())
            .targetCode(targetSubmission.getCode())
            .targetLanguage(targetSubmission.getLanguage())
            .validatorPath(assets.getValidatorPath())
            .referencePath(assets.getReferencePath())
            .hackInput(dto.getHackInput())
            .timeLimitMs(assets.getTimeLimitMs())
            .memoryLimitMb(assets.getMemoryLimitMb())
            .build();
    rocketMQTemplate.convertAndSend(MqConstant.HACK_TASK_TOPIC, taskMsg);

    return Result.success(record.getId());
}
```

## 8. 排行榜更新机制 — Lua 原子操作

### 8.1 Hack 成功后的积分变动

```
hacker： 排行榜 + 题目分数
hacked： 排行榜 - 题目分数（保底 0 分）
solved_count（被攻击者）： -1（保底 0）
AC 标记（被攻击者）： 清除，允许重新提交该题
```

### 8.2 Lua 原子脚本 `hack_rank_update.lua`

将多步 Redis 操作封装为原子事务，防止并发 Hack 导致积分错乱：

```lua
-- KEYS[1] = contest:rank:{contestId}        (ZSET)
-- KEYS[2] = contest:{cid}:user:{tgtUid}:solved_count  (String)
-- KEYS[3] = contest:ac:{cid}:{tgtUid}:{pid}  (String, AC 标记)
-- KEYS[4] = contest:hack:{cid}:{hackerUid}:{tgtUid}:{pid}  (String, 去重)
-- ARGV[1] = hackerId (string)
-- ARGV[2] = targetUserId (string)
-- ARGV[3] = problemScore (integer)

local rankKey   = KEYS[1]
local solvedKey = KEYS[2]
local acKey     = KEYS[3]
local hackKey   = KEYS[4]
local hackerId  = ARGV[1]
local targetId  = ARGV[2]
local score     = tonumber(ARGV[3])

-- 1. 加分
redis.call('ZINCRBY', rankKey, score, hackerId)

-- 2. 扣分（保底 0）
local currentScore = redis.call('ZSCORE', rankKey, targetId)
currentScore = currentScore and tonumber(currentScore) or 0
local newScore = math.max(0, currentScore - score)
redis.call('ZADD', rankKey, newScore, targetId)

-- 3. 扣减解题数（保底 0）
local solved = redis.call('GET', solvedKey)
solved = solved and tonumber(solved) or 0
redis.call('SET', solvedKey, math.max(0, solved - 1))

-- 4. 清除 AC 标记
redis.call('DEL', acKey)

-- 5. 标记已 Hack
redis.call('SET', hackKey, '1')

return 1
```

### 8.3 Java 调用

```java
private final DefaultRedisScript<Long> hackRankUpdateScript;

public void updateRankOnHackSuccess(Integer contestId, Long hackerId,
                                     Long targetUserId, Integer problemId, int score) {
    List<String> keys = List.of(
            "contest:rank:" + contestId,
            "contest:" + contestId + ":user:" + targetUserId + ":solved_count",
            "contest:ac:" + contestId + ":" + targetUserId + ":" + problemId,
            "contest:hack:" + contestId + ":" + hackerId + ":" + targetUserId + ":" + problemId
    );
    stringRedisTemplate.execute(hackRankUpdateScript, keys,
            String.valueOf(hackerId), String.valueOf(targetUserId), String.valueOf(score));
}
```

### 8.4 持久化

与现有 `persistRankToDb()` 流程一致，在比赛结束时将 Redis 排行榜数据持久化到 `contest_participants` 表。

## 9. 测试用例入库

Hack 成功后，将 Hack 数据作为新的测试用例添加到题目中：

1. `ProblemClient.addHackTestCase()` 调用 problem-service
2. `inputData` = hackInput, `outputData` = hackOutput (标准解答输出)
3. `source_hack_id` = hackId (标记来源，便于溯源)
4. `isSample` = false (非样例，不展示给用户)
5. `scoreWeight` = 1.0

此后该题目的所有新提交都必须通过这个新增的测试用例，逐步提高题目健壮性。

## 10. 实现任务清单

| 序号 | 任务 | 涉及服务 | 优先级 |
|------|------|---------|--------|
| 1 | Problem 表添加 `validator_path`、`validator_exe_path`、`validator_src_hash`、`reference_path`、`reference_language` | problem | P0 |
| 2 | TestCase 表添加 `source_hack_id`；HackRecord 表添加 `SystemError` 状态 | problem+contest | P0 |
| 3 | Admin 端：出题页面上传 validator.cpp + reference.xxx，上传时触发编译缓存，保存 .exe 并写入 Hash | problem | P0 |
| 4 | 创建 `hack_rank_update.lua` Redis 原子脚本 | contest | P0 |
| 5 | 实现 Lock/Unlock API + AC 代码列表接口（锁定时返回含代码的提交列表） | contest | P0 |
| 6 | 实现 Hack 提交接口（Feign 拉取目标代码，MQ 只传 ID 引用） | contest | P0 |
| 7 | 新增 Hack MQ Topic + DLQ Topic，更新 MqConstant | common | P0 |
| 8 | `ValidatorRunner`：编译缓存检查 + SHA-256 比对 + Judge0 沙箱运行 | judge | P0 |
| 9 | `HackTaskConsumer`：Validator → Feign 拉取目标代码 → 目标判题 → 标答判题 → 结果发送 | judge | P0 |
| 10 | `HackTaskDeadLetterConsumer`：重试耗尽 → 写 SystemError 状态 | judge | P0 |
| 11 | `HackResultConsumer`：写记录 → Lua 原子排行更新 → 添加测试用例 | contest | P0 |
| 12 | ProblemClient 新增 `getHackAssets` / `addHackTestCase` / `updateValidatorHash` | common-api | P0 |
| 13 | ContestClient 新增 `updateRankOnHackSuccess`（内部调 Lua） | common-api | P0 |
| 14 | JudgeClient 新增 `getSubmissionCodeById`（Feign 拉取代码） | common-api | P0 |
| 15 | 用户端前端：Hack 操作 UI（锁定/查看AC代码/提交Hack/结果展示） | vue-project1 | P1 |
| 16 | WebSocket 广播 Hack 结果通知 | judge | P1 |
| 17 | 比赛结束自动解锁全部 Lock | contest | P2 |

## 11. 安全与风控

- 单场比赛每个选手对同一目标同一题只能 Hack 一次（Redis key 去重）
- Validator 编译+运行均在 Judge0 沙箱中完成，禁止本地 ProcessBuilder 直接执行 C++ 代码
- C++ 源码存储在 `hack-data/` 磁盘目录，按 `contest-{id}/problem-{id}` 隔离
- 编译缓存：pre-built `.exe` 持久化到磁盘，通过 SHA-256 Hash 校验防止源码被篡改后使用过期缓存
- Hack 输入内容长度限制 64KB
- MQ 消息体精简：只传 ID 和路径，代码/数据由 Consumer 按需通过 Feign 拉取，避免敏感代码在 MQ 中明文流转
- 排行榜扣分通过 Lua 原子脚本执行，score 和 solved_count 保底 0，防止并发写导致数据错乱
- 死信队列兜底：HackTaskConsumer 重试耗尽后进入 DLQ → 更新记录为 SystemError → 通知用户"判题系统异常，本次 Hack 不计分"
