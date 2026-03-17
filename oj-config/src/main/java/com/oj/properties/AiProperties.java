package com.oj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.ai")
@Data
public class AiProperties {
    /**
     * 模型服务基础地址
     */
    private String baseUrl;

    /**
     * API Key（云端API需要，本地Ollama可不填）
     */
    private String apiKey;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 是否启用流式响应
     */
    private Boolean stream;
}
