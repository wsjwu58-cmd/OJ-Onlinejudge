package com.oj.ai.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class McpClientConfiguration {

    private final List<McpClient> mcpClients = new ArrayList<>();

    @Value("${oj.mcp.bing-search.enabled:false}")
    private boolean bingSearchEnabled;

    @Bean
    @ConditionalOnProperty(name = "oj.mcp.bing-search.enabled", havingValue = "true")
    public McpClient bingSearchMcpClient() {
        log.info("Initializing Bing Search MCP Client...");

        List<String> command = List.of("npx.cmd", "bing-cn-mcp-enhanced");

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .logEvents(true)
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        mcpClients.add(mcpClient);

        log.info("必应搜索MCP已启动");
        return mcpClient;
    }

    @Bean
    public ToolProvider mcpToolProvider(List<McpClient> mcpClients) {
        log.info("创建MCP ToolProvider, 当前MCP客户端数: {}", mcpClients.size());
        return new McpToolProvider.Builder()
                .mcpClients(mcpClients)
                .failIfOneServerFails(false)
                .build();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Closing MCP clients...");
        for (McpClient client : mcpClients) {
            try {
                client.close();
            } catch (Exception e) {
                log.error("Error closing MCP client", e);
            }
        }
    }
}
