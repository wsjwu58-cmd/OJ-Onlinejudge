package com.oj.service.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * LangChain4j 对话窗口的 Redis 持久化，按 memoryId（通常为 sessionId）隔离。
 * 与 {@link LongTermMemoryService} 配合：本类存完整多轮 ChatMessage，长期服务存用户级摘要与画像。
 */
@Component
@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "agent:lc4j:chat:";
    /** 与 LongTermMemoryService 中 SESSION_TTL 对齐：7 天 */
    private static final long TTL_SECONDS = 86400L * 7;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = toKey(memoryId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<?> raw = ChatMessageDeserializer.messagesFromJson(json);
            if (raw == null) {
                return new ArrayList<>();
            }
            List<ChatMessage> messages = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o instanceof ChatMessage) {
                    messages.add((ChatMessage) o);
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("反序列化对话记忆失败，将视为空会话: key={}", key, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = toKey(memoryId);
        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        String json = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(key, json, TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(toKey(memoryId));
    }

    private static String toKey(Object memoryId) {
        String id = memoryId == null ? "null" : memoryId.toString();
        // 避免与 Redis 分层键冲突
        String safe = id.replace(':', '_');
        return KEY_PREFIX + safe;
    }
}
