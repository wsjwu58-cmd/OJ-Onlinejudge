package com.oj.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sky.judge0")
public class Judge0Config {
    private String apiUrl = "http://192.168.141.128:2358";
    private String apiKey = "";
    private int maxWaitSeconds = 15;
    private int pollIntervalMs = 500;
}
