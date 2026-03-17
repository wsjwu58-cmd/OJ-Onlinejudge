package com.oj.config;

import org.commonmark.Extension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
@Configuration
public class MarkdownConfig {

    /**
     * 定义 Markdown 解析器 Bean
     */
    @Bean
    public Parser markdownParser() {
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),       // 表格支持
                TaskListItemsExtension.create() // 任务列表支持
                // ... 你可以在这里添加更多扩展
        );

        return Parser.builder()
                .extensions(extensions)
                .build();
    }

    /**
     * 定义 HTML 渲染器 Bean
     * 这是最关键的部分，确保安全配置
     */
    @Bean
    public HtmlRenderer htmlRenderer() {
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                TaskListItemsExtension.create()
        );

        return HtmlRenderer.builder()
                .extensions(extensions)
                // **至关重要**：防止XSS攻击，对原始HTML进行转义
                .escapeHtml(true)
                // 可选：如果你想允许解析和渲染原始HTML标签（不推荐用于用户输入），则设为 false
                // .escapeHtml(false)
                .build();
    }
}