package com.oj.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.ai")
@Data
public class AiProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    private String embeddingModel;
    private long timeout = 120000;
    private boolean stream = true;
}
