package com.oj.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oj.dto.AiChatDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DialogMemoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String DIALOG_KEY_PREFIX = "ai:dialog:";
    private static final int DIALOG_TTL = 3600; // 1小时过期
    private static final int MAX_HISTORY_LENGTH = 10; // 最多保存10条消息

    /**
     * 保存对话历史
     */
    public void saveDialogHistory(Long userId, Integer problemId, String role, String content) {
        String key = getDialogKey(userId, problemId);
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());

        // 获取现有对话
        String existing = redisTemplate.opsForValue().get(key);
        JSONArray dialogs = existing != null ? JSON.parseArray(existing) : new JSONArray();
        
        // 限制对话历史长度
        if (dialogs.size() >= MAX_HISTORY_LENGTH) {
            dialogs.remove(0);
        }
        dialogs.add(message);

        // 保存并设置过期时间
        redisTemplate.opsForValue().set(key, dialogs.toJSONString());
        redisTemplate.expire(key, DIALOG_TTL, TimeUnit.SECONDS);
        
        log.debug("保存对话历史: userId={}, problemId={}, role={}", userId, problemId, role);
    }

    /**
     * 获取对话历史
     */
    public List<AiChatDTO.MessageHistory> getDialogHistory(Long userId, Integer problemId) {
        String key = getDialogKey(userId, problemId);
        String dialogsJson = redisTemplate.opsForValue().get(key);
        
        if (dialogsJson == null) {
            return Collections.emptyList();
        }

        List<AiChatDTO.MessageHistory> history = new ArrayList<>();
        JSONArray dialogs = JSON.parseArray(dialogsJson);
        for (Object obj : dialogs) {
            JSONObject msg = (JSONObject) obj;
            AiChatDTO.MessageHistory message = new AiChatDTO.MessageHistory();
            message.setRole(msg.getString("role"));
            message.setContent(msg.getString("content"));
            history.add(message);
        }
        return history;
    }

    /**
     * 清除对话历史
     */
    public void clearDialogHistory(Long userId, Integer problemId) {
        String key = getDialogKey(userId, problemId);
        redisTemplate.delete(key);
        log.debug("清除对话历史: userId={}, problemId={}", userId, problemId);
    }

    /**
     * 构建对话键
     */
    private String getDialogKey(Long userId, Integer problemId) {
        return DIALOG_KEY_PREFIX + userId + ":" + (problemId != null ? problemId : "general");
    }
}
