package com.oj.service.tools;

import com.oj.properties.McpProperties;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSearchMcpTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchMcpTool.class);

    @Autowired
    private McpProperties mcpProperties;

    @Tool("使用Brave搜索引擎搜索互联网获取相关信息")
    public String braveSearch(
            @P("搜索关键词") String query,
            @P(value = "搜索类型，可选值: web, news") String searchType,
            @P(value = "返回结果数量") Integer count) {

        log.info("执行Brave搜索 - 查询: {}, 类型: {}, 数量: {}", query, searchType, count);

        if (!mcpProperties.getBraveSearch().getEnabled()) {
            return "Brave Search MCP客户端未启用，请在配置中启用并设置API Key";
        }

        String apiKey = mcpProperties.getBraveSearch().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "Brave Search API Key未配置";
        }

        try {
            String searchApiUrl = searchType != null && searchType.equals("news")
                    ? "https://api.search.brave.com/res/v1/news/search"
                    : "https://api.search.brave.com/res/v1/web/search";

            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = searchApiUrl + "?q=" + encodedQuery + "&count=" + Math.min(count != null ? count : 10, 20);

            java.net.URL requestUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Subscription-Token", apiKey);

            StringBuilder response = new StringBuilder();
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return formatBraveSearchResults(response.toString(), query);
            } else {
                return "Brave Search API调用失败，响应码: " + responseCode;
            }

        } catch (Exception e) {
            log.error("Brave搜索执行失败: ", e);
            return "搜索执行失败: " + e.getMessage();
        }
    }

    @Tool("使用DuckDuckGo搜索引擎搜索互联网")
    public String duckDuckGoSearch(
            @P("搜索关键词") String query,
            @P(value = "返回结果数量") Integer count) {

        log.info("执行DuckDuckGo搜索 - 查询: {}, 数量: {}", query, count);

        try {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String apiUrl = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_redirect=1";

            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            log.info("DuckDuckGo搜索成功，结果长度: {}", response.length());
            return formatDuckDuckGoResults(response.toString(), query, count);

        } catch (Exception e) {
            log.error("DuckDuckGo搜索执行失败: ", e);
            return "搜索执行失败: " + e.getMessage();
        }
    }

    @Tool("使用百度千帆AI搜索引擎搜索互联网获取相关信息")
    public String qianfanSearch(
            @P("搜索关键词") String query,
            @P(value = "返回结果数量") Integer count) {

        log.info("执行百度千帆搜索 - 查询: {}, 数量: {}", query, count);

        if (!mcpProperties.getQianfanSearch().getEnabled()) {
            return "百度千帆搜索未启用，请在配置中启用并设置API Key和Secret Key";
        }

        String apiKey = mcpProperties.getQianfanSearch().getApiKey();
        String secretKey = mcpProperties.getQianfanSearch().getSecretKey();
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return "百度千帆API Key或Secret Key未配置";
        }

        try {
            String accessToken = getQianfanAccessToken(apiKey, secretKey);
            if (accessToken == null) {
                return "获取百度千帆Access Token失败，请检查API Key和Secret Key";
            }

            return searchWithQianfan(query, accessToken, count);
        } catch (Exception e) {
            log.error("百度千帆搜索执行失败: ", e);
            return "搜索执行失败: " + e.getMessage();
        }
    }

    private String getQianfanAccessToken(String apiKey, String secretKey) {
        try {
            String tokenUrl = mcpProperties.getQianfanSearch().getBaseUrl()
                    + "/oauth/2.0/token?grant_type=client_credentials&client_id="
                    + apiKey + "&client_secret=" + secretKey;

            java.net.URL url = new java.net.URL(tokenUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            StringBuilder response = new StringBuilder();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response.toString());
                return json.getString("access_token");
            } else {
                log.error("获取Access Token失败，响应码: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            log.error("获取Access Token异常: ", e);
            return null;
        }
    }

    private String searchWithQianfan(String query, String accessToken, Integer count) {
        try {
            String searchUrl = "https://wenxin.baidu.com/developer/aiSearch/v1/query";
            java.net.URL url = new java.net.URL(searchUrl + "?access_token=" + accessToken);

            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json");

            String requestBody = com.alibaba.fastjson.JSON.toJSONString(
                    java.util.Map.of(
                            "query", query,
                            "page_num", 1,
                            "page_size", Math.min(count != null ? count : 10, 20)
                    )
            );

            connection.setDoOutput(true);
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(connection.getOutputStream())) {
                writer.write(requestBody);
            }

            StringBuilder response = new StringBuilder();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return formatQianfanSearchResults(response.toString(), query);
            } else {
                return "百度千帆搜索API调用失败，响应码: " + responseCode;
            }
        } catch (Exception e) {
            log.error("百度千帆搜索异常: ", e);
            return "搜索执行失败: " + e.getMessage();
        }
    }

    private String formatQianfanSearchResults(String jsonResponse, String query) {
        StringBuilder result = new StringBuilder();
        result.append("【百度千帆搜索结果】\n");
        result.append("查询: ").append(query).append("\n\n");

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(jsonResponse);

            if (json.containsKey("data") && json.containsKey("results")) {
                var results = json.getJSONArray("results");
                if (results != null && !results.isEmpty()) {
                    result.append("相关结果:\n");
                    int shownCount = 0;
                    for (int i = 0; i < results.size() && shownCount < 10; i++) {
                        var item = results.getJSONObject(i);
                        String title = item.getString("title");
                        String url = item.getString("url");
                        String description = item.getString("abstract");

                        if (title != null) {
                            result.append(String.format("%d. %s\n", shownCount + 1, title));
                            if (url != null) {
                                result.append("   URL: ").append(url).append("\n");
                            }
                            if (description != null && !description.isEmpty()) {
                                result.append("   摘要: ").append(description).append("\n");
                            }
                            result.append("\n");
                            shownCount++;
                        }
                    }
                } else {
                    result.append("未找到相关结果");
                }
            } else {
                result.append("未找到相关结果");
                result.append("\n原始响应: ").append(jsonResponse);
            }
        } catch (Exception e) {
            log.error("解析百度千帆搜索结果失败: ", e);
            result.append("解析结果失败: ").append(e.getMessage());
            result.append("\n原始响应: ").append(jsonResponse);
        }

        return result.toString();
    }

    private String formatBraveSearchResults(String jsonResponse, String query) {
        StringBuilder result = new StringBuilder();
        result.append("【Brave Search 搜索结果】\n");
        result.append("查询: ").append(query).append("\n\n");

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(jsonResponse);

            if (json.containsKey("web")) {
                var webResults = json.getJSONObject("web");
                if (webResults != null && webResults.containsKey("results")) {
                    var results = webResults.getJSONArray("results");
                    if (results != null) {
                        result.append("相关结果:\n");
                        int shownCount = 0;
                        for (int i = 0; i < results.size() && shownCount < 10; i++) {
                            var item = results.getJSONObject(i);
                            String title = item.getString("title");
                            String url = item.getString("url");
                            String description = item.getString("description");

                            if (title != null && url != null) {
                                result.append(String.format("%d. %s\n   URL: %s\n", shownCount + 1, title, url));
                                if (description != null) {
                                    result.append("   摘要: ").append(description).append("\n");
                                }
                                result.append("\n");
                                shownCount++;
                            }
                        }
                    }
                }
            }

            if (result.length() == 0 || result.toString().equals("【Brave Search 搜索结果】\n查询: " + query + "\n\n")) {
                result.append("未找到相关结果");
            }

        } catch (Exception e) {
            log.error("解析Brave Search结果失败: ", e);
            result.append("原始响应: ").append(jsonResponse);
        }

        return result.toString();
    }

    private String formatDuckDuckGoResults(String jsonResponse, String query, int count) {
        StringBuilder result = new StringBuilder();
        result.append("【DuckDuckGo搜索结果】\n");
        result.append("查询: ").append(query).append("\n\n");

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(jsonResponse);

            if (json.containsKey("RelatedTopics")) {
                result.append("相关结果:\n");
                var topics = json.getJSONArray("RelatedTopics");
                int shownCount = 0;
                for (int i = 0; i < topics.size() && shownCount < Math.min(count!=0 ? count : 10, 10); i++) {
                    var topic = topics.getJSONObject(i);
                    if (topic.containsKey("Text") && topic.containsKey("FirstURL")) {
                        result.append(String.format("%d. %s\n   链接: %s\n\n",
                                shownCount + 1,
                                topic.getString("Text"),
                                topic.getString("FirstURL")));
                        shownCount++;
                    }
                }
            }

            if (result.length() == 0) {
                result.append("未找到相关结果");
            }

        } catch (Exception e) {
            log.error("解析DuckDuckGo结果失败: ", e);
            result.append("原始响应: ").append(jsonResponse);
        }

        return result.toString();
    }
}
