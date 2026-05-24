-- submit_and_update_v3.lua
-- KEYS[1]: user:{userId}:problem:{problemId}:status
-- KEYS[2]: problem:{problemId}:solved_count
-- KEYS[3]: submission:{token}
-- KEYS[4]: user:{userId}:problem:{problemId}:processing
-- KEYS[5]: user:{userId}:problem:{problemId}:submit_window
-- ARGV[1]: token
-- ARGV[2]: current_timestamp
-- ARGV[3]: window_size
-- ARGV[4]: max_submits

local status_key = KEYS[1]
local count_key = KEYS[2]
local submission_key = KEYS[3]
local processing_key = KEYS[4]
local submit_window_key = KEYS[5]
local token = ARGV[1]
local current_time = tonumber(ARGV[2])
local window_size = tonumber(ARGV[3])
local max_submits = tonumber(ARGV[4])

redis.call('ZREMRANGEBYSCORE', submit_window_key, 0, current_time - window_size)
local window_count = redis.call('ZCARD', submit_window_key)
if window_count >= max_submits then
    local earliest = redis.call('ZRANGE', submit_window_key, 0, 0, 'WITHSCORES')
    local wait_time = 0
    if earliest and type(earliest) == 'table' and #earliest >= 2 then
        local earliest_time = tonumber(earliest[2])
        if earliest_time then
            wait_time = math.ceil(earliest_time - (current_time - window_size))
            if wait_time < 0 then wait_time = 0 end
        end
    end
    return {0, 'rate_limited', '', window_count, wait_time}
end

redis.call('ZADD', submit_window_key, current_time, token)
redis.call('EXPIRE', submit_window_key, window_size + 10)

local processing_token = redis.call('GET', processing_key)
if processing_token then
    local processing_status = redis.call('GET', 'submission:' .. processing_token)
    if processing_status == 'pending' then
        return {0, 'already_processing', processing_token, '', 0}
    end
end

local is_solved = redis.call('GET', status_key)
redis.call('SETEX', processing_key, 300, token)
redis.call('SETEX', submission_key, 3600, 'pending')
return {1, 'success', token, is_solved or 'null', 0}
