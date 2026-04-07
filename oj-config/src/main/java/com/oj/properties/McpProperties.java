package com.oj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.mcp")
@Data
public class McpProperties {

    private BraveSearch braveSearch = new BraveSearch();
    private QianfanSearch qianfanSearch = new QianfanSearch();

    @Data
    public static class BraveSearch {
        private Boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://api.search.brave.com/mcp";
    }

    @Data
    public static class QianfanSearch {
        private Boolean enabled = false;
        private String apiKey;
        private String secretKey;
        private String baseUrl = "https://aip.baidubce.com";
    }
}
