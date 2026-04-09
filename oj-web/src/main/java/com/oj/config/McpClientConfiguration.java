package com.oj.config;

import com.oj.properties.McpProperties;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.mcp.McpToolProvider;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class McpClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpClientConfiguration.class);

    private final McpProperties mcpProperties;
    private final List<McpClient> mcpClients = new ArrayList<>();

    @Autowired
    public McpClientConfiguration(McpProperties mcpProperties) {
        this.mcpProperties = mcpProperties;
    }

    @Bean
//    @ConditionalOnProperty(name = "sky.mcp.brave-search.enabled", havingValue = "true")
    public McpClient bingSearchMcpClient() {
        log.info("Initializing Brave Search MCP Client...");
        


        List<String> command = List.of("E:\\nvm\\nvm\\v25.9.0\\npx.cmd","bing-cn-mcp-enhanced");


        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .logEvents(true) // 可选：打印日志
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        mcpClients.add(mcpClient);

        log.info("必应搜素MCP已启动");
        
        return mcpClient;
    }

    @Bean
    public ToolProvider mcpToolProvider(List<McpClient> mcpClients) {
        log.info("创建MCP Tool...");
        log.info("当前mcp Tool:{}",mcpClients);
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
                log.info("MCP client closed: {}", client);
            } catch (Exception e) {
                log.error("Error closing MCP client", e);
            }
        }
    }
}
