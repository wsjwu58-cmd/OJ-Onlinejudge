-- update_result_v2.lua
-- 判题完成后调用，增强token验证和状态追踪
-- KEYS[1]: user:{userId}:problem:{problemId}:status
-- KEYS[2]: problem:{problemId}:solved_count
-- KEYS[3]: submission:{token}
-- KEYS[4]: user:{userId}:solved_count
-- KEYS[5]: user:{userId}:problem:{problemId}:processing (正在处理的token)
-- KEYS[6]: user:{userId}:problem:{problemId}:last_result (上次结果，用于统计)
-- ARGV[1]: token
-- ARGV[2]: result (Accepted/Wrong Answer/TLE等)
-- ARGV[3]: execute_time (执行时间ms，可选)
-- ARGV[4]: memory_kb (内存占用KB，可选)

local status_key = KEYS[1]
local count_key = KEYS[2]
local submission_key = KEYS[3]
local user_solved_key = KEYS[4]
local processing_key = KEYS[5]
local last_result_key = KEYS[6]
local token = ARGV[1]
local result = ARGV[2]
local execute_time = ARGV[3]
local memory_kb = ARGV[4]

-- 1. 检查token是否存在且为pending状态
local current_status = redis.call('GET', submission_key)
if not current_status then
    return {0, 'token_not_found', 0, 0}
end
if current_status ~= 'pending' then
    return {0, 'already_processed', 0, 0}
end

-- ❌ 删除：检查token是否被覆盖的逻辑
-- local processing_token = redis.call('GET', processing_key)
-- if processing_token and processing_token ~= token then
--     redis.call('SETEX', submission_key, 3600, 'overridden')
--     return {0, 'overridden', 0, 0}
-- end

-- 2. 更新提交状态（保存24小时）
redis.call('SETEX', submission_key, 86400, result)

-- 3. 保存上次结果（用于前端展示或统计）
local result_info = result
if execute_time and memory_kb then
    result_info = result .. ':' .. execute_time .. ':' .. memory_kb
end
redis.call('SETEX', last_result_key, 86400, result_info)

-- 4. 清除processing标记（允许新提交）
redis.call('DEL', processing_key)

-- 5. 如果是Accepted且用户之前没AC过
if result == 'Accepted' then
    local was_solved = redis.call('GET', status_key)
    if not was_solved then
        -- 标记用户已AC该题（永久保存）
        redis.call('SET', status_key, '1')
        -- 原子性增加题目解题数
        local new_problem_count = redis.call('INCR', count_key)
        -- 原子性增加用户个人解题数
        local new_user_count = redis.call('INCR', user_solved_key)
        -- 返回：[是否首次AC, 状态, 题目新解题数, 用户新解题数]
        return {1, 'first_ac', new_problem_count, new_user_count}
    end
    -- 已AC过，但仍返回当前统计数据
    local problem_count = redis.call('GET', count_key) or 0
    local user_count = redis.call('GET', user_solved_key) or 0
    return {0, 'already_solved', problem_count, user_count}
end

-- 6. 非AC结果
local problem_count = redis.call('GET', count_key) or 0
local user_count = redis.call('GET', user_solved_key) or 0
return {0, 'not_ac', problem_count, user_count}