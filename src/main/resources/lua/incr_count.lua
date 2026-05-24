-- 原子增减二进制计数
-- KEYS[1]: key (pcnt:{id} 或 ucnt:{id})
-- ARGV[1]: offset (字节偏移: 0, 4, 8...)
-- ARGV[2]: delta (增量: 1 或 -1)
-- ARGV[3]: total_size (总字节数: 8 或 20)

local key = KEYS[1]
local offset = tonumber(ARGV[1])
local delta = tonumber(ARGV[2])
local total_size = tonumber(ARGV[3])

-- 读取当前值，不存在则初始化
local data = redis.call('GET', key)
if not data then
    data = string.rep('\0', total_size)
end

-- 确保数据长度正确
if #data < total_size then
    data = data .. string.rep('\0', total_size - #data)
end

-- 读取 offset 处的 4 字节大端 int32
local b1 = string.byte(data, offset + 1) or 0
local b2 = string.byte(data, offset + 2) or 0
local b3 = string.byte(data, offset + 3) or 0
local b4 = string.byte(data, offset + 4) or 0
local current = b1 * 16777216 + b2 * 65536 + b3 * 256 + b4

-- 计算新值（防止负数）
local new_val = current + delta
if new_val < 0 then
    new_val = 0
end

-- 转回大端 4 字节
local n1 = math.floor(new_val / 16777216) % 256
local n2 = math.floor(new_val / 65536) % 256
local n3 = math.floor(new_val / 256) % 256
local n4 = new_val % 256

-- 拼接新数据
local prefix = string.sub(data, 1, offset)
local suffix = string.sub(data, offset + 5)
local new_data = prefix .. string.char(n1, n2, n3, n4) .. suffix

redis.call('SET', key, new_data)
return new_val
