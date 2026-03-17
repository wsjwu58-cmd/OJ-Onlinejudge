package com.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * Redis Lua 脚本配置
 */
@Configuration
public class RedisLuaConfig {

    /**
     * 提交代码时的 Lua 脚本（设置 pending 状态）
     */
    @Bean
    public DefaultRedisScript<List> submitAndUpdateScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/submit_and_update.lua")));
        script.setResultType(List.class);
        return script;
    }

    /**
     * 判题完成后更新结果的 Lua 脚本
     */
    @Bean
    public DefaultRedisScript<List> updateResultScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/update_result.lua")));
        script.setResultType(List.class);
        return script;
    }
}
