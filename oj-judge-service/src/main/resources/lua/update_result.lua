-- update_result_v2.lua
-- KEYS[1]: user:{userId}:problem:{problemId}:status
-- KEYS[2]: problem:{problemId}:solved_count
-- KEYS[3]: submission:{token}
-- KEYS[4]: user:{userId}:solved_count
-- KEYS[5]: user:{userId}:problem:{problemId}:processing
-- KEYS[6]: user:{userId}:problem:{problemId}:last_result
-- ARGV[1]: token
-- ARGV[2]: result
-- ARGV[3]: execute_time
-- ARGV[4]: memory_kb

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

local current_status = redis.call('GET', submission_key)
if not current_status then
    return {0, 'token_not_found', 0, 0}
end
if current_status ~= 'pending' then
    return {0, 'already_processed', 0, 0}
end

redis.call('SETEX', submission_key, 86400, result)

local result_info = result
if execute_time and memory_kb then
    result_info = result .. ':' .. execute_time .. ':' .. memory_kb
end
redis.call('SETEX', last_result_key, 86400, result_info)

redis.call('DEL', processing_key)

if result == 'Accepted' then
    local was_solved = redis.call('GET', status_key)
    if not was_solved then
        redis.call('SET', status_key, '1')
        local new_problem_count = redis.call('INCR', count_key)
        local new_user_count = redis.call('INCR', user_solved_key)
        return {1, 'first_ac', new_problem_count, new_user_count}
    end
    local problem_count = redis.call('GET', count_key) or 0
    local user_count = redis.call('GET', user_solved_key) or 0
    return {0, 'already_solved', problem_count, user_count}
end

local problem_count = redis.call('GET', count_key) or 0
local user_count = redis.call('GET', user_solved_key) or 0
return {0, 'not_ac', problem_count, user_count}
