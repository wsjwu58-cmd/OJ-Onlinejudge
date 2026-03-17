package com.oj.controller.User;

import com.oj.properties.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test/ai")
public class AiTestController {
    @Autowired
    private AiProperties aiProperties;

    // 注入 Spring Boot 默认提供的 RestTemplate
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 测试 AI 配置读取和模型调用
     */
    @GetMapping("/call")
    public String testAiCall() {
        System.out.println("读取到的配置: " + aiProperties);

        try {
            // 1. 构建请求 URL
            String url = aiProperties.getBaseUrl() + "/chat/completions";

            // 2. 构建请求体 (Map 形式，RestTemplate 会自动转为 JSON)
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", aiProperties.getModel());

            // 构建消息列表
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "你好，你是谁？请用中文回答");

            requestMap.put("messages", java.util.Collections.singletonList(message));

            // 3. 发送 POST 请求
            // 注意：RestTemplate 默认发送 JSON，接收 JSON
            String response = restTemplate.postForObject(url, requestMap, String.class);

            return "调用成功！响应内容: " + response;

        } catch (Exception e) {
            e.printStackTrace();
            return "调用失败！错误信息: " + e.getMessage() +
                    "<br>请检查 Ollama 是否启动，或模型名称是否正确";
        }
    }
}
