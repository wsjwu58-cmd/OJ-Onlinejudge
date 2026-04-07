package com.oj.config;

import com.oj.properties.McpProperties;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class McpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientConfig.class);

    @Autowired
    private McpProperties mcpProperties;

    private final Map<String, McpClient> mcpClients = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            initializeBraveSearchClient();
        } catch (Exception e) {
            log.warn("初始化MCP客户端失败: {}", e.getMessage());
        }
    }

    private void initializeBraveSearchClient() {
        if (!mcpProperties.getBraveSearch().getEnabled()) {
            log.info("Brave Search MCP客户端未启用");
            return;
        }

        String apiKey = mcpProperties.getBraveSearch().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Brave Search API Key未配置，无法启用MCP客户端");
            return;
        }

        try {
            log.info("正在初始化Brave Search MCP客户端...");

            var transport = new StdioMcpTransport.Builder()
                    .command(List.of(
                            "npx",
                            "-y",
                            "@modelcontextprotocol/server-brave-search",
                            apiKey
                    ))
                    .logEvents(true)
                    .build();

            var braveSearchClient = new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();

            mcpClients.put("brave-search", braveSearchClient);

            List<ToolSpecification> tools = braveSearchClient.listTools();
            log.info("Brave Search MCP客户端初始化成功，可用工具数量: {}", tools.size());
            tools.forEach(tool -> log.info("  - 工具: {}", tool.name()));

        } catch (Exception e) {
            log.error("初始化Brave Search MCP客户端失败: ", e);
        }
    }

    public McpClient getClient(String name) {
        return mcpClients.get(name);
    }

    public List<ToolSpecification> getToolSpecifications(String clientName) {
        McpClient client = mcpClients.get(clientName);
        if (client == null) {
            log.warn("MCP客户端 '{}' 不存在", clientName);
            return List.of();
        }

        try {
            return client.listTools();
        } catch (Exception e) {
            log.error("获取工具规格失败: ", e);
            return List.of();
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("关闭所有MCP客户端连接...");
        mcpClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭MCP客户端时出错: ", e);
            }
        });
        mcpClients.clear();
    }
}
