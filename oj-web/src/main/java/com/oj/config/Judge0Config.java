package com.oj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Judge0 API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sky.judge0")
public class Judge0Config {
    /**
     * Judge0 API 基地址，例如 http://localhost:2358
     * 如果用官方 SaaS 版则为 https://judge0-ce.p.rapidapi.com
     */
    private String apiUrl = "http://192.168.141.128:2358";

    /**
     * RapidAPI Key（使用官方SaaS时需要，自建可留空）
     */
    private String apiKey = "";

    /**
     * 提交后轮询结果的最大等待秒数
     */
    private int maxWaitSeconds = 15;

    /**
     * 轮询间隔（毫秒）
     */
    private int pollIntervalMs = 500;
}
