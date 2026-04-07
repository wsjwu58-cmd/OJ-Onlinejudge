package com.oj.service.agent.memory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        if (userId == null) {
            return;
        }
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
        
        log.debug("保存用户交互历史: userId={}, sessionId={}", userId, sessionId);
    }

    public List<Map<String, Object>> getRecentInteractions(Long userId, int limit) {
        String key = USER_LEARNING_HISTORY_PREFIX + userId;
        String historyJson = redisTemplate.opsForValue().get(key);
        
        if (historyJson == null) {
            return Collections.emptyList();
        }
        
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
            interaction.put("messageLength", obj.getInteger("messageLength"));
            interactions.add(interaction);
        }
        
        return interactions;
    }

    public void updateUserProfile(Long userId, String profileType, Map<String, Object> profileData) {
        String key = USER_PROFILE_KEY_PREFIX + userId;
        
        String existing = redisTemplate.opsForValue().get(key);
        JSONObject profile = existing != null ? JSON.parseObject(existing) : new JSONObject();
        
        JSONObject typeProfile = profile.getJSONObject(profileType);
        if (typeProfile == null) {
            typeProfile = new JSONObject();
        }
        
        typeProfile.putAll(profileData);
        typeProfile.put("lastUpdated", System.currentTimeMillis());
        
        profile.put(profileType, typeProfile);
        
        redisTemplate.opsForValue().set(key, profile.toJSONString());
        redisTemplate.expire(key, PROFILE_TTL, TimeUnit.SECONDS);
        
        log.debug("更新用户画像: userId={}, profileType={}", userId, profileType);
    }

    public Map<String, Object> getUserProfile(Long userId) {
        String key = USER_PROFILE_KEY_PREFIX + userId;
        String profileJson = redisTemplate.opsForValue().get(key);
        
        if (profileJson == null) {
            return new HashMap<>();
        }
        
        return JSON.parseObject(profileJson);
    }

    public void saveUserPreference(Long userId, String preferenceKey, Object preferenceValue) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        
        String existing = redisTemplate.opsForValue().get(key);
        JSONObject preferences = existing != null ? JSON.parseObject(existing) : new JSONObject();
        
        preferences.put(preferenceKey, preferenceValue);
        
        redisTemplate.opsForValue().set(key, preferences.toJSONString());
        redisTemplate.expire(key, PROFILE_TTL, TimeUnit.SECONDS);
        
        log.debug("保存用户偏好: userId={}, key={}", userId, preferenceKey);
    }

    public Object getUserPreference(Long userId, String preferenceKey) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        String preferencesJson = redisTemplate.opsForValue().get(key);
        
        if (preferencesJson == null) {
            return null;
        }
        
        JSONObject preferences = JSON.parseObject(preferencesJson);
        return preferences.get(preferenceKey);
    }

    public void saveSessionSummary(String sessionId, String summary) {
        String key = SESSION_SUMMARY_PREFIX + sessionId;
        
        JSONObject sessionData = new JSONObject();
        sessionData.put("summary", summary);
        sessionData.put("createdAt", System.currentTimeMillis());
        
        redisTemplate.opsForValue().set(key, sessionData.toJSONString());
        redisTemplate.expire(key, SESSION_TTL, TimeUnit.SECONDS);
        
        log.debug("保存会话摘要: sessionId={}", sessionId);
    }

    public String getSessionSummary(String sessionId) {
        String key = SESSION_SUMMARY_PREFIX + sessionId;
        String sessionJson = redisTemplate.opsForValue().get(key);
        
        if (sessionJson == null) {
            return null;
        }
        
        JSONObject sessionData = JSON.parseObject(sessionJson);
        return sessionData.getString("summary");
    }

    public Map<String, Object> buildContextForUser(Long userId, String currentTopic) {
        Map<String, Object> context = new HashMap<>();
        
        Map<String, Object> profile = getUserProfile(userId);
        if (!profile.isEmpty()) {
            context.put("userProfile", profile);
        }
        
        Object preferredLanguage = getUserPreference(userId, "preferredLanguage");
        if (preferredLanguage != null) {
            context.put("preferredLanguage", preferredLanguage);
        }
        
        Object learningGoal = getUserPreference(userId, "learningGoal");
        if (learningGoal != null) {
            context.put("learningGoal", learningGoal);
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

    /**
     * 将 {@link #buildContextForUser} 的结果转为简短中文说明，供拼入用户消息前作为系统已知信息。
     */
    public String formatContextForPrompt(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) context.get("userProfile");
        if (profile != null && !profile.isEmpty()) {
            sb.append("- 用户画像：").append(JSON.toJSONString(profile)).append('\n');
        }
        if (context.get("preferredLanguage") != null) {
            sb.append("- 偏好语言：").append(context.get("preferredLanguage")).append('\n');
        }
        if (context.get("learningGoal") != null) {
            sb.append("- 学习目标：").append(context.get("learningGoal")).append('\n');
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) context.get("recentInteractions");
        if (recent != null && !recent.isEmpty()) {
            sb.append("- 近期对话（含助手回复摘要）：\n");
            int n = Math.min(3, recent.size());
            for (int i = Math.max(0, recent.size() - n); i < recent.size(); i++) {
                Map<String, Object> turn = recent.get(i);
                String um = String.valueOf(turn.get("userMessage"));
                String ar = turn.get("assistantResponse") != null
                        ? String.valueOf(turn.get("assistantResponse"))
                        : "";
                if (ar.length() > 200) {
                    ar = ar.substring(0, 200) + "…";
                }
                sb.append("  ").append(i + 1).append(") 用户：").append(um).append('\n');
                if (!ar.isEmpty()) {
                    sb.append("     助手：").append(ar).append('\n');
                }
            }
        }
        return sb.toString().trim();
    }

    public void learnFromInteraction(Long userId, String userMessage, String assistantResponse, boolean wasHelpful) {
        String messageLower = userMessage.toLowerCase();
        
        if (messageLower.contains("题解") || messageLower.contains("解题")) {
            incrementStat(userId, "solutionRequests");
        }
        
        if (messageLower.contains("代码") && messageLower.contains("分析")) {
            incrementStat(userId, "codeAnalysisRequests");
        }
        
        if (messageLower.contains("学习") || messageLower.contains("进度")) {
            incrementStat(userId, "learningRequests");
        }
        
        if (wasHelpful) {
            incrementStat(userId, "successfulInteractions");
        }
        
        if (messageLower.contains("帮助") || messageLower.contains("有用")) {
            savePositiveFeedback(userId, userMessage);
        }
    }

    private void incrementStat(Long userId, String statKey) {
        String key = "agent:user:stats:" + userId;
        redisTemplate.opsForHash().increment(key, statKey, 1);
        redisTemplate.expire(key, PROFILE_TTL, TimeUnit.SECONDS);
    }

    private void savePositiveFeedback(Long userId, String userMessage) {
        String key = "agent:user:positive:" + userId;
        Long size = redisTemplate.opsForList().size(key);
        
        if (size != null && size >= 10) {
            redisTemplate.opsForList().leftPop(key);
        }
        
        redisTemplate.opsForList().rightPush(key, userMessage);
        redisTemplate.expire(key, PROFILE_TTL, TimeUnit.SECONDS);
    }

    public List<String> getPositiveFeedback(Long userId) {
        String key = "agent:user:positive:" + userId;
        List<String> feedback = redisTemplate.opsForList().range(key, 0, -1);
        return feedback != null ? feedback : Collections.emptyList();
    }

    public Map<String, Long> getUserStats(Long userId) {
        String key = "agent:user:stats:" + userId;
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);
        
        Map<String, Long> result = new HashMap<>();
        stats.forEach((k, v) -> result.put(k.toString(), Long.parseLong(v.toString())));
        
        return result;
    }

    public void clearUserMemory(Long userId) {
        redisTemplate.delete(USER_PROFILE_KEY_PREFIX + userId);
        redisTemplate.delete(USER_PREFERENCES_KEY_PREFIX + userId);
        redisTemplate.delete(USER_LEARNING_HISTORY_PREFIX + userId);
        redisTemplate.delete("agent:user:stats:" + userId);
        redisTemplate.delete("agent:user:positive:" + userId);
        
        log.info("清除用户长期记忆: userId={}", userId);
    }
}
