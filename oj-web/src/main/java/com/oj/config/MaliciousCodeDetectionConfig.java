package com.oj.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
public class MaliciousCodeDetectionConfig {

    private boolean enabled = true;

    private boolean logMaliciousSubmissions = true;

    private int maxCodeLength = 100000;

    private List<String> allowedPatterns;

    private Map<String, LanguageRules> languageRules = new HashMap<>();

    @Data
    public static class LanguageRules {
        private List<DetectionRule> rules;
        private List<String> whitelist;
    }

    @Data
    public static class DetectionRule {
        private String name;
        private String pattern;
        private String description;
        private int severity;
        private boolean enabled = true;
    }
}
