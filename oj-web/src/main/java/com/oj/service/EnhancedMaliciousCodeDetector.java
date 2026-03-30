package com.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oj.config.MaliciousCodeDetectionConfig;
import com.oj.dto.MaliciousCodeDetectionResult;
import com.oj.entity.MaliciousCodeLog;
import com.oj.mapper.MaliciousCodeLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EnhancedMaliciousCodeDetector {

    @Autowired
    private MaliciousCodeDetectionConfig config;

    @Autowired
    private MaliciousCodeLogMapper maliciousCodeLogMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    public MaliciousCodeDetectionResult detect(String code, String language) {
        if (!config.isEnabled()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(true)
                    .message("恶意代码检测已禁用")
                    .build();
        }

        if (code == null || code.isEmpty()) {
            return MaliciousCodeDetectionResult.builder()
                    .safe(true)
                    .message("代码为空")
                    .build();
        }

        if (code.length() > config.getMaxCodeLength()) {
            log.warn("代码长度超过限制: {} 字符", code.length());
            if (config.isLogMaliciousSubmissions()) {
                logMaliciousSubmission(code, language, "代码长度超过限制");
            }
            return MaliciousCodeDetectionResult.builder()
                    .safe(false)
                    .message("代码长度超过限制（最大 " + config.getMaxCodeLength() + " 字符）")
                    .build();
        }

        String lowerCode = code.toLowerCase();
        String lowerLanguage = language.toLowerCase();

        MaliciousCodeDetectionConfig.LanguageRules rules = config.getLanguageRules().get(lowerLanguage);
        
        if (rules != null && rules.getRules() != null) {
            for (MaliciousCodeDetectionConfig.DetectionRule rule : rules.getRules()) {
                if (!rule.isEnabled()) {
                    continue;
                }

                try {
                    Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(code);

                    if (matcher.find()) {
                        String matchedText = matcher.group();
                        
                        if (isWhitelisted(matchedText, rules.getWhitelist())) {
                            log.debug("匹配到白名单规则: {}", matchedText);
                            continue;
                        }

                        String message = String.format("检测到%s: %s", rule.getName(), rule.getDescription());
                        
                        if (config.isLogMaliciousSubmissions()) {
                            logMaliciousSubmission(code, language, message);
                        }

                        return MaliciousCodeDetectionResult.builder()
                                .safe(false)
                                .message(message)
                                .build();
                    }
                } catch (Exception e) {
                    log.error("正则表达式执行失败: rule={}, error={}", rule.getName(), e.getMessage());
                }
            }
        }

        return MaliciousCodeDetectionResult.builder()
                .safe(true)
                .message("代码安全")
                .build();
    }

    private boolean isWhitelisted(String matchedText, List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }

        for (String whitelistPattern : whitelist) {
            try {
                Pattern pattern = Pattern.compile(whitelistPattern, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(matchedText).find()) {
                    return true;
                }
            } catch (Exception e) {
                log.error("白名单正则表达式执行失败: pattern={}, error={}", whitelistPattern, e.getMessage());
            }
        }

        return false;
    }

    private void logMaliciousSubmission(String code, String language, String reason) {
        try {
            String codePreview = code.length() > 200 ? code.substring(0, 200) + "..." : code;
            log.warn("恶意代码检测触发 - 语言: {}, 原因: {}, 代码预览: {}", language, reason, codePreview);

            MaliciousCodeLog logEntity = new MaliciousCodeLog();
            logEntity.setCode(code.length() > 5000 ? code.substring(0, 5000) : code);
            logEntity.setLanguage(language);
            logEntity.setDetectionReason(reason);
            logEntity.setCreateTime(LocalDateTime.now());

            if (request != null) {
                logEntity.setIpAddress(getClientIpAddress(request));
                logEntity.setUserAgent(request.getHeader("User-Agent"));
            }

            maliciousCodeLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("记录恶意代码提交日志失败: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public List<String> getAllSupportedLanguages() {
        return new ArrayList<>(config.getLanguageRules().keySet());
    }
}
