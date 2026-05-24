package com.oj.ai.service;

import com.oj.ai.dto.AiChatDTO;
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
    private static final int DIALOG_TTL = 3600;
    private static final int MAX_HISTORY_LENGTH = 10;

    public void saveDialogHistory(Long userId, Integer problemId, String role, String content) {
        String key = getDialogKey(userId, problemId);
        try {
            var messageJson = new com.alibaba.fastjson.JSONObject();
            messageJson.put("role", role);
            messageJson.put("content", content);
            messageJson.put("timestamp", System.currentTimeMillis());

            String existing = redisTemplate.opsForValue().get(key);
            var dialogs = existing != null ? com.alibaba.fastjson.JSON.parseArray(existing) : new com.alibaba.fastjson.JSONArray();
            if (dialogs.size() >= MAX_HISTORY_LENGTH) dialogs.remove(0);
            dialogs.add(messageJson);

            redisTemplate.opsForValue().set(key, dialogs.toJSONString());
            redisTemplate.expire(key, DIALOG_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("保存对话历史失败", e);
        }
    }

    public List<AiChatDTO.MessageHistory> getDialogHistory(Long userId, Integer problemId) {
        String key = getDialogKey(userId, problemId);
        String dialogsJson = redisTemplate.opsForValue().get(key);
        if (dialogsJson == null) return Collections.emptyList();

        List<AiChatDTO.MessageHistory> history = new ArrayList<>();
        var dialogs = com.alibaba.fastjson.JSON.parseArray(dialogsJson);
        for (Object obj : dialogs) {
            var msg = (com.alibaba.fastjson.JSONObject) obj;
            AiChatDTO.MessageHistory message = new AiChatDTO.MessageHistory();
            message.setRole(msg.getString("role"));
            message.setContent(msg.getString("content"));
            history.add(message);
        }
        return history;
    }

    public void clearDialogHistory(Long userId, Integer problemId) {
        redisTemplate.delete(getDialogKey(userId, problemId));
    }

    private String getDialogKey(Long userId, Integer problemId) {
        return DIALOG_KEY_PREFIX + userId + ":" + (problemId != null ? problemId : "general");
    }
}
