package com.oj.ai.service.memory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LongTermMemoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String USER_PROFILE_KEY_PREFIX = "agent:user:profile:";
    private static final String USER_PREFERENCES_KEY_PREFIX = "agent:user:preferences:";
    private static final String USER_LEARNING_HISTORY_PREFIX = "agent:user:learning:";
    private static final String SESSION_SUMMARY_PREFIX = "agent:session:summary:";
    private static final int PROFILE_TTL = 86400 * 30;
    private static final int SESSION_TTL = 86400 * 7;
    private static final int MAX_SUMMARY_ITEMS = 50;

    public void saveUserInteraction(Long userId, String sessionId, String userMessage, String assistantResponse) {
        if (userId == null) return;
        String key = USER_LEARNING_HISTORY_PREFIX + userId;

        JSONObject interaction = new JSONObject();
        interaction.put("timestamp", System.currentTimeMillis());
        interaction.put("sessionId", sessionId);
        interaction.put("userMessage", userMessage);
        interaction.put("assistantResponse", assistantResponse);
        interaction.put("messageLength", userMessage.length());

        String existing = redisTemplate.opsForValue().get(key);
        JSONArray history = existing != null ? JSON.parseArray(existing) : new JSONArray();
        if (history.size() >= MAX_SUMMARY_ITEMS) {
            history.remove(0);
        }
        history.add(interaction);

        redisTemplate.opsForValue().set(key, history.toJSONString());
        redisTemplate.expire(key, SESSION_TTL, TimeUnit.SECONDS);
    }

    public List<Map<String, Object>> getRecentInteractions(Long userId, int limit) {
        String key = USER_LEARNING_HISTORY_PREFIX + userId;
        String historyJson = redisTemplate.opsForValue().get(key);
        if (historyJson == null) return Collections.emptyList();

        JSONArray history = JSON.parseArray(historyJson);
        List<Map<String, Object>> interactions = new ArrayList<>();
        int start = Math.max(0, history.size() - limit);
        for (int i = start; i < history.size(); i++) {
            JSONObject obj = history.getJSONObject(i);
            Map<String, Object> interaction = new HashMap<>();
            interaction.put("timestamp", obj.getLong("timestamp"));
            interaction.put("sessionId", obj.getString("sessionId"));
            interaction.put("userMessage", obj.getString("userMessage"));
            interaction.put("assistantResponse", obj.getString("assistantResponse"));
            interactions.add(interaction);
        }
        return interactions;
    }

    public Map<String, Object> buildContextForUser(Long userId, String currentTopic) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> profile = getUserProfile(userId);
        if (!profile.isEmpty()) {
            context.put("userProfile", profile);
        }
        List<Map<String, Object>> recentInteractions = getRecentInteractions(userId, 5);
        if (!recentInteractions.isEmpty()) {
            context.put("recentInteractions", recentInteractions);
            List<String> recentTopics = recentInteractions.stream()
                    .map(interaction -> (String) interaction.get("userMessage"))
                    .limit(3)
                    .collect(Collectors.toList());
            context.put("recentTopics", recentTopics);
        }
        return context;
    }

    public String formatContextForPrompt(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) context.get("userProfile");
        if (profile != null && !profile.isEmpty()) {
            sb.append("- 用户画像：").append(JSON.toJSONString(profile)).append('\n');
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) context.get("recentInteractions");
        if (recent != null && !recent.isEmpty()) {
            sb.append("- 近期对话：\n");
            int n = Math.min(3, recent.size());
            for (int i = Math.max(0, recent.size() - n); i < recent.size(); i++) {
                Map<String, Object> turn = recent.get(i);
                String um = String.valueOf(turn.get("userMessage"));
                sb.append("  ").append(i + 1).append(") 用户：").append(um).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private Map<String, Object> getUserProfile(Long userId) {
        String key = USER_PROFILE_KEY_PREFIX + userId;
        String profileJson = redisTemplate.opsForValue().get(key);
        if (profileJson == null) return new HashMap<>();
        return JSON.parseObject(profileJson);
    }

    public void clearUserMemory(Long userId) {
        redisTemplate.delete(USER_PROFILE_KEY_PREFIX + userId);
        redisTemplate.delete(USER_PREFERENCES_KEY_PREFIX + userId);
        redisTemplate.delete(USER_LEARNING_HISTORY_PREFIX + userId);
        redisTemplate.delete("agent:user:stats:" + userId);
        log.info("清除用户长期记忆: userId={}", userId);
    }
}
