-- 秒杀下单 Lua 脚本
-- KEYS[1]: 库存 key (seckill:stock:{voucherId})
-- KEYS[2]: 订单用户集合 key (seckill:order:{voucherId})
-- ARGV[1]: 用户 ID

-- 1. 判断库存是否充足
local stock = tonumber(redis.call("get", KEYS[1]))
if stock == nil or stock <= 0 then
    -- 库存不足，返回 1
    return 1
end

-- 2. 判断用户是否已经下单
local userId = ARGV[1]
local exists = redis.call("sismember", KEYS[2], userId)
if exists == 1 then
    -- 用户已下单，返回 2
    return 2
end

-- 3. 扣减库存
redis.call("decr", KEYS[1])

-- 4. 将 userId 存入当前优惠券的 set 集合
redis.call("sadd", KEYS[2], userId)

-- 5. 下单成功，返回 0
return 0
