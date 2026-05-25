-- KEYS[1] = contest:rank:{contestId}
-- KEYS[2] = contest:{cid}:user:{tgtUid}:solved_count
-- KEYS[3] = contest:ac:{cid}:{tgtUid}:{pid}
-- KEYS[4] = contest:hack:{cid}:{hackerUid}:{tgtUid}:{pid}
-- ARGV[1] = hackerId
-- ARGV[2] = targetUserId
-- ARGV[3] = problemScore (integer)

local rankKey   = KEYS[1]
local solvedKey = KEYS[2]
local acKey     = KEYS[3]
local hackKey   = KEYS[4]
local hackerId  = ARGV[1]
local targetId  = ARGV[2]
local score     = tonumber(ARGV[3])

redis.call('ZINCRBY', rankKey, score, hackerId)

local currentScore = redis.call('ZSCORE', rankKey, targetId)
currentScore = currentScore and tonumber(currentScore) or 0
local newScore = math.max(0, currentScore - score)
redis.call('ZADD', rankKey, newScore, targetId)

local solved = redis.call('GET', solvedKey)
solved = solved and tonumber(solved) or 0
redis.call('SET', solvedKey, math.max(0, solved - 1))

redis.call('DEL', acKey)

redis.call('SET', hackKey, '1')

return 1
