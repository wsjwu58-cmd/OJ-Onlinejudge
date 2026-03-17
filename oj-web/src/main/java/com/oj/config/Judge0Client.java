package com.oj.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class Judge0Client {
    @Autowired
    private Judge0Config judge0Config;

    private RestTemplate restTemplate;

    /**
     * Judge0 语言ID映射
     * 完整列表: GET /languages
     */
    private static final Map<String, Integer> LANGUAGE_MAP = new HashMap<>();

    static {
        LANGUAGE_MAP.put("Java", 62);           // Java (OpenJDK 13.0.1)
        LANGUAGE_MAP.put("Python", 71);          // Python (3.8.1)
        LANGUAGE_MAP.put("C++", 54);             // C++ (GCC 9.2.0)
        LANGUAGE_MAP.put("JavaScript", 63);      // JavaScript (Node.js 12.14.0)
        LANGUAGE_MAP.put("C", 50);               // C (GCC 9.2.0)
        LANGUAGE_MAP.put("Go", 60);              // Go (1.13.5)
        LANGUAGE_MAP.put("Rust", 73);            // Rust (1.40.0)
        LANGUAGE_MAP.put("TypeScript", 74);      // TypeScript (3.7.4)
    }

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 获取 Judge0 语言ID
     */
    public int getLanguageId(String language) {
        Integer id = LANGUAGE_MAP.get(language);
        if (id == null) {
            throw new IllegalArgumentException("不支持的编程语言: " + language + "，支持: " + LANGUAGE_MAP.keySet());
        }
        return id;
    }

    /**
     * 提交代码到 Judge0，同步等待结果返回
     *
     * @param sourceCode     源代码
     * @param languageId     Judge0 语言ID
     * @param stdin          标准输入
     * @param expectedOutput 期望输出（可为null）
     * @param timeLimitSec   时间限制（秒），null则用默认
     * @param memoryLimitKb  内存限制（KB），null则用默认
     * @return Judge0 返回的完整 JSON
     */
    public JSONObject submitAndWait(String sourceCode, int languageId, String stdin,
                                    String expectedOutput, Float timeLimitSec, Integer memoryLimitKb) {
        // 1. 构造请求体（Base64编码）
        JSONObject body = new JSONObject();
        body.put("source_code", base64Encode(sourceCode));
        body.put("language_id", languageId);
        if (stdin != null) {
            body.put("stdin", base64Encode(stdin));
        }
        if (expectedOutput != null) {
            body.put("expected_output", base64Encode(expectedOutput));
        }
        if (timeLimitSec != null) {
            body.put("cpu_time_limit", timeLimitSec);
        }
        if (memoryLimitKb != null) {
            body.put("memory_limit", memoryLimitKb);
        }

        // 2. 提交
        String submitUrl = judge0Config.getApiUrl() + "/submissions?base64_encoded=true&wait=false";
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        log.info("Judge0 提交: languageId={}, stdinLen={}", languageId, stdin == null ? 0 : stdin.length());
        ResponseEntity<String> submitResp = restTemplate.postForEntity(submitUrl, request, String.class);

        if (submitResp.getStatusCode() != HttpStatus.CREATED && submitResp.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Judge0 提交失败: " + submitResp.getStatusCode() + " " + submitResp.getBody());
        }

        JSONObject submitResult = JSON.parseObject(submitResp.getBody());
        String token = submitResult.getString("token");
        log.info("Judge0 token: {}", token);

        // 3. 轮询结果
        return pollResult(token);
    }

    /**
     * 轮询 Judge0 获取判题结果
     */
    private JSONObject pollResult(String token) {
        String getUrl = judge0Config.getApiUrl() + "/submissions/" + token + "?base64_encoded=true&fields=*";
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        int maxAttempts = judge0Config.getMaxWaitSeconds() * 1000 / judge0Config.getPollIntervalMs();

        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(judge0Config.getPollIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            ResponseEntity<String> resp = restTemplate.exchange(getUrl, HttpMethod.GET, request, String.class);
            JSONObject result = JSON.parseObject(resp.getBody());

            JSONObject status = result.getJSONObject("status");
            if (status != null) {
                int statusId = status.getIntValue("id");
                // status.id: 1=In Queue, 2=Processing, 3+=已完成
                if (statusId >= 3) {
                    log.info("Judge0 完成: statusId={}, description={}", statusId, status.getString("description"));
                    return result;
                }
            }
        }

        throw new RuntimeException("Judge0 判题超时，请稍后重试");
    }

    /**
     * 解析 Judge0 状态ID 为项目状态字符串
     */
    public String parseStatus(int statusId) {
        switch (statusId) {
            case 3: return "Accepted";
            case 4: return "Wrong Answer";
            case 5: return "Time Limit Exceeded";
            case 6: return "Compile Error";
            case 7: case 8: case 9: case 10: case 11: case 12:
                return "Runtime Error";
            case 13: return "Internal Error";
            case 14: return "Exec Format Error";
            default: return "Runtime Error";
        }
    }

    /**
     * 从 Judge0 结果中安全解码 Base64 字段
     */
    public String decodeField(JSONObject result, String field) {
        String val = result.getString(field);
        if (val == null || val.isEmpty()) return null;

        String current = val;
        // 最多尝试解码 5 次，防止死循环
        int maxAttempts = 5;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                // 预处理：移除空白字符
                String cleanBase64 = current.replaceAll("\\s+", "");

                // 检查是否是有效的Base64字符串（长度是4的倍数，且只包含Base64字符）
                if (!isValidBase64(cleanBase64)) {
                    // 如果不是有效的Base64，说明 current 已经是明文了
                    return current;
                }

                // 尝试解码
                byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
                String decodedString = new String(decodedBytes);

                // 如果解码后的字符串和原字符串一样（且长度>1），说明已经解码到最底层了
                // 或者如果解码后的内容不再符合Base64格式，也说明是明文了
                if (decodedString.equals(current) || !isValidBase64(decodedString)) {
                    return decodedString;
                }

                // 如果解码成功且结果仍是Base64，则继续循环解码
                current = decodedString;

            } catch (IllegalArgumentException e) {
                // 如果解码失败，说明 current 是明文
                log.debug("Base64解码终止，已到达明文层: field={}, value={}", field, current);
                return current;
            }
        }
        // 防止极端情况下的死循环
        return current;
    }

    // 辅助方法：检查字符串是否符合Base64基本格式
    private boolean isValidBase64(String str) {
        if (str == null || str.length() == 0) return false;
        // Base64长度必须是4的倍数
        if (str.length() % 4 != 0) return false;
        // 简单检查字符集（忽略末尾的=）
        return str.matches("^[a-zA-Z0-9+/]*={0,2}$");
    }

    /**
     * 构造请求头（兼容自建 & RapidAPI SaaS）
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = judge0Config.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            // RapidAPI SaaS 模式
            headers.set("X-RapidAPI-Key", apiKey);
            headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
        }
        return headers;
    }

    private String base64Encode(String text) {
        if (text == null) return null;
        return Base64.getEncoder().encodeToString(text.getBytes());
    }
}
