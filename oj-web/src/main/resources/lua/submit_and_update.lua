-- submit_and_update_v2.lua
-- 用户提交代码时调用，增强防重复提交和并发控制
-- KEYS[1]: user:{userId}:problem:{problemId}:status (AC状态)
-- KEYS[2]: problem:{problemId}:solved_count (题目解题数)
-- KEYS[3]: submission:{token} (本次提交状态)
-- KEYS[4]: user:{userId}:problem:{problemId}:processing (正在处理的token)
-- KEYS[5]: user:{userId}:problem:{problemId}:submit_count (提交次数，用于限流)
-- ARGV[1]: token
-- ARGV[2]: current_timestamp (当前时间戳，秒)

local status_key = KEYS[1]
local count_key = KEYS[2]
local submission_key = KEYS[3]
local processing_key = KEYS[4]
local submit_count_key = KEYS[5]
local token = ARGV[1]
local current_time = tonumber(ARGV[2])

-- 1. 限流检查：同一题目60秒内最多提交5次
local submit_count = redis.call('INCR', submit_count_key)
if submit_count == 1 then
    redis.call('EXPIRE', submit_count_key, 60)  -- 60秒窗口
end
if submit_count > 5 then
    return {0, 'rate_limited', nil, submit_count}
end

-- 2. 检查是否有正在处理的提交
local processing_token = redis.call('GET', processing_key)
if processing_token then
    -- 检查该token是否还在pending状态
    local processing_status = redis.call('GET', 'submission:' .. processing_token)
    if processing_status == 'pending' then
        -- 有提交正在判题，返回正在处理的token
        return {0, 'already_processing', processing_token, nil}
    end
end

-- 3. 检查是否已AC
local is_solved = redis.call('GET', status_key)

-- 4. 设置当前token为正在处理的提交（5分钟超时）
redis.call('SETEX', processing_key, 300, token)

-- 5. 设置提交状态为pending
redis.call('SETEX', submission_key, 3600, 'pending')

-- 6. 返回：[成功标志, 状态, token, 已AC标志]
return {1, 'success', token, is_solved or 'null'}