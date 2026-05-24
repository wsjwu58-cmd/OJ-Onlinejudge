package com.oj.judge.config;

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

    private static final Map<String, Integer> LANGUAGE_MAP = new HashMap<>();

    static {
        LANGUAGE_MAP.put("Java", 62);
        LANGUAGE_MAP.put("Python", 71);
        LANGUAGE_MAP.put("C++", 54);
        LANGUAGE_MAP.put("JavaScript", 63);
        LANGUAGE_MAP.put("C", 50);
        LANGUAGE_MAP.put("Go", 60);
        LANGUAGE_MAP.put("Rust", 73);
        LANGUAGE_MAP.put("TypeScript", 74);
    }

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    public int getLanguageId(String language) {
        Integer id = LANGUAGE_MAP.get(language);
        if (id == null) {
            throw new IllegalArgumentException("不支持的编程语言: " + language);
        }
        return id;
    }

    public JSONObject submitAndWait(String sourceCode, int languageId, String stdin,
                                    String expectedOutput, Float timeLimitSec, Integer memoryLimitKb) {
        JSONObject body = new JSONObject();
        body.put("source_code", base64Encode(sourceCode));
        body.put("language_id", languageId);
        if (stdin != null) body.put("stdin", base64Encode(stdin));
        if (expectedOutput != null) body.put("expected_output", base64Encode(expectedOutput));
        if (timeLimitSec != null) body.put("cpu_time_limit", timeLimitSec);
        if (memoryLimitKb != null) body.put("memory_limit", memoryLimitKb);

        String submitUrl = judge0Config.getApiUrl() + "/submissions?base64_encoded=true&wait=false";
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> submitResp = restTemplate.postForEntity(submitUrl, request, String.class);
        JSONObject submitResult = JSON.parseObject(submitResp.getBody());
        String token = submitResult.getString("token");
        log.info("Judge0 token: {}", token);

        return pollResult(token);
    }

    private JSONObject pollResult(String token) {
        String getUrl = judge0Config.getApiUrl() + "/submissions/" + token + "?base64_encoded=true&fields=*";
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        int maxAttempts = judge0Config.getMaxWaitSeconds() * 1000 / judge0Config.getPollIntervalMs();
        for (int i = 0; i < maxAttempts; i++) {
            try { Thread.sleep(judge0Config.getPollIntervalMs()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

            ResponseEntity<String> resp = restTemplate.exchange(getUrl, HttpMethod.GET, request, String.class);
            JSONObject result = JSON.parseObject(resp.getBody());
            JSONObject status = result.getJSONObject("status");
            if (status != null && status.getIntValue("id") >= 3) {
                return result;
            }
        }
        throw new RuntimeException("Judge0 判题超时");
    }

    public String parseStatus(int statusId) {
        return switch (statusId) {
            case 3 -> "Accepted";
            case 4 -> "Wrong Answer";
            case 5 -> "Time Limit Exceeded";
            case 6 -> "Compile Error";
            case 13 -> "Internal Error";
            case 14 -> "Exec Format Error";
            default -> "Runtime Error";
        };
    }

    public String decodeField(JSONObject result, String field) {
        String val = result.getString(field);
        if (val == null || val.isEmpty()) return null;
        String current = val;
        for (int i = 0; i < 5; i++) {
            try {
                String cleanBase64 = current.replaceAll("\\s+", "");
                if (!isValidBase64(cleanBase64)) return current;
                byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
                String decodedString = new String(decodedBytes);
                if (decodedString.equals(current) || !isValidBase64(decodedString)) return decodedString;
                current = decodedString;
            } catch (IllegalArgumentException e) { return current; }
        }
        return current;
    }

    private boolean isValidBase64(String str) {
        if (str == null || str.length() == 0 || str.length() % 4 != 0) return false;
        return str.matches("^[a-zA-Z0-9+/]*={0,2}$");
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = judge0Config.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-RapidAPI-Key", apiKey);
            headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
        }
        return headers;
    }

    private String base64Encode(String text) {
        return text != null ? Base64.getEncoder().encodeToString(text.getBytes()) : null;
    }
}
