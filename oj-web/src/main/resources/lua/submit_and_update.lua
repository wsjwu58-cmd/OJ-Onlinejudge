-- submit_and_update_v3.lua
-- 用户提交代码时调用，滑动窗口限流（不同题目独立限流）
-- KEYS[1]: user:{userId}:problem:{problemId}:status (AC状态)
-- KEYS[2]: problem:{problemId}:solved_count (题目解题数)
-- KEYS[3]: submission:{token} (本次提交状态)
-- KEYS[4]: user:{userId}:problem:{problemId}:processing (正在处理的token)
-- KEYS[5]: user:{userId}:problem:{problemId}:submit_window (ZSET，滑动窗口)
-- ARGV[1]: token
-- ARGV[2]: current_timestamp (当前时间戳，秒)
-- ARGV[3]: window_size (窗口大小，秒)
-- ARGV[4]: max_submits (最大提交次数)

local status_key = KEYS[1]
local count_key = KEYS[2]
local submission_key = KEYS[3]
local processing_key = KEYS[4]
local submit_window_key = KEYS[5]
local token = ARGV[1]
local current_time = tonumber(ARGV[2])
local window_size = tonumber(ARGV[3])
local max_submits = tonumber(ARGV[4])

-- 1. 滑动窗口限流检查
-- 删除窗口外的过期记录（保留最近 window_size 秒的记录）
redis.call('ZREMRANGEBYSCORE', submit_window_key, 0, current_time - window_size)
-- 统计窗口内的提交次数
local window_count = redis.call('ZCARD', submit_window_key)
-- 检查是否超过限制
if window_count >= max_submits then
    -- 获取窗口内最早的时间戳，计算需要等待的时间
    local earliest = redis.call('ZRANGE', submit_window_key, 0, 0, 'WITHSCORES')
    local wait_time = 0
    if earliest and type(earliest) == 'table' and #earliest >= 2 then
        local earliest_time = tonumber(earliest[2])
        if earliest_time then
            wait_time = math.ceil(earliest_time - (current_time - window_size))
            if wait_time < 0 then
                wait_time = 0
            end
        end
    end
    return {0, 'rate_limited', '', window_count, wait_time}
end

-- 未超限，添加当前提交记录到窗口
redis.call('ZADD', submit_window_key, current_time, token)
-- 设置过期时间，多留10秒缓冲
redis.call('EXPIRE', submit_window_key, window_size + 10)

-- 2. 检查是否有正在处理的提交
local processing_token = redis.call('GET', processing_key)
if processing_token then
    local processing_status = redis.call('GET', 'submission:' .. processing_token)
    if processing_status == 'pending' then
        return {0, 'already_processing', processing_token, '', 0}
    end
end

-- 3. 检查是否已AC
local is_solved = redis.call('GET', status_key)

-- 4. 设置当前token为正在处理的提交（5分钟超时）
redis.call('SETEX', processing_key, 300, token)

-- 5. 设置提交状态为pending
redis.call('SETEX', submission_key, 3600, 'pending')

-- 6. 返回：[成功标志, 状态, token, 已AC标志, 额外信息]
return {1, 'success', token, is_solved or 'null', 0}