package org.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 封装常用的Redis操作，简化使用
 */
@Component
public class RedisUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtil.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // ==================== 基础操作 ====================
    
    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            LOGGER.error("Redis设置缓存失败，key: {}", key, e);
        }
    }
    
    /**
     * 设置缓存（无过期时间）
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            LOGGER.error("Redis设置缓存失败，key: {}", key, e);
        }
    }
    
    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? (T) value : null;
        } catch (Exception e) {
            LOGGER.error("Redis获取缓存失败，key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 获取缓存（返回Object）
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOGGER.error("Redis获取缓存失败，key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            LOGGER.error("Redis删除缓存失败，key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 批量删除缓存
     */
    public long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            LOGGER.error("Redis批量删除缓存失败，keys: {}", keys, e);
            return 0;
        }
    }
    
    /**
     * 判断键是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            LOGGER.error("Redis检查键是否存在失败，key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            LOGGER.error("Redis设置过期时间失败，key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 获取过期时间（秒）
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            LOGGER.error("Redis获取过期时间失败，key: {}", key, e);
            return -1;
        }
    }
    
    // ==================== 原子操作 ====================
    
    /**
     * 递增
     */
    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            return value != null ? value : 0;
        } catch (Exception e) {
            LOGGER.error("Redis递增失败，key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * 递增指定值
     */
    public long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            return value != null ? value : 0;
        } catch (Exception e) {
            LOGGER.error("Redis递增失败，key: {}, delta: {}", key, delta, e);
            return 0;
        }
    }
    
    /**
     * 递减
     */
    public long decrement(String key) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key);
            return value != null ? value : 0;
        } catch (Exception e) {
            LOGGER.error("Redis递减失败，key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * 递减指定值
     */
    public long decrement(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key, delta);
            return value != null ? value : 0;
        } catch (Exception e) {
            LOGGER.error("Redis递减失败，key: {}, delta: {}", key, delta, e);
            return 0;
        }
    }
    
    // ==================== Hash操作 ====================
    
    /**
     * Hash设置
     */
    public void hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            LOGGER.error("Redis Hash设置失败，key: {}, hashKey: {}", key, hashKey, e);
        }
    }
    
    /**
     * Hash获取
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hashKey);
            return value != null ? (T) value : null;
        } catch (Exception e) {
            LOGGER.error("Redis Hash获取失败，key: {}, hashKey: {}", key, hashKey, e);
            return null;
        }
    }
    
    /**
     * Hash删除
     */
    public void hDelete(String key, String hashKey) {
        try {
            redisTemplate.opsForHash().delete(key, hashKey);
        } catch (Exception e) {
            LOGGER.error("Redis Hash删除失败，key: {}, hashKey: {}", key, hashKey, e);
        }
    }
    
    // ==================== Set操作 ====================
    
    /**
     * Set添加
     */
    public long sAdd(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null ? count : 0;
        } catch (Exception e) {
            LOGGER.error("Redis Set添加失败，key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Set获取所有成员
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if (members == null) {
                return null;
            }
            return (Set<T>) (Set<?>) members;
        } catch (Exception e) {
            LOGGER.error("Redis Set获取成员失败，key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Set判断成员是否存在
     */
    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            LOGGER.error("Redis Set判断成员失败，key: {}, value: {}", key, value, e);
            return false;
        }
    }
    
    // ==================== List操作 ====================
    
    /**
     * List左推（头部添加）
     */
    public long lPush(String key, Object value) {
        try {
            Long count = redisTemplate.opsForList().leftPush(key, value);
            return count != null ? count : 0;
        } catch (Exception e) {
            LOGGER.error("Redis List左推失败，key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * List右推（尾部添加）
     */
    public long rPush(String key, Object value) {
        try {
            Long count = redisTemplate.opsForList().rightPush(key, value);
            return count != null ? count : 0;
        } catch (Exception e) {
            LOGGER.error("Redis List右推失败，key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * List获取范围
     */
    @SuppressWarnings("unchecked")
    public <T> java.util.List<T> lRange(String key, long start, long end, Class<T> clazz) {
        try {
            java.util.List<Object> list = redisTemplate.opsForList().range(key, start, end);
            if (list == null) {
                return null;
            }
            return (java.util.List<T>) (java.util.List<?>) list;
        } catch (Exception e) {
            LOGGER.error("Redis List获取范围失败，key: {}", key, e);
            return null;
        }
    }
    
    // ==================== 业务常用方法 ====================
    
    /**
     * 缓存查询模式：先查缓存，再查数据库，最后写回缓存
     * @param key 缓存键
     * @param supplier 数据提供者（数据库查询逻辑）
     * @param timeout 过期时间（分钟）
     * @param clazz 数据类型
     * @return 数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrSet(String key, java.util.function.Supplier<T> supplier, int timeout, Class<T> clazz) {
        // 1. 先查缓存
        T cached = get(key, clazz);
        if (cached != null && !cached.equals("")) {
            LOGGER.debug("从缓存获取，key: {}", key);
            return cached;
        }
        
        // 2. 缓存未命中，查询数据库
        LOGGER.debug("缓存未命中，查询数据库，key: {}", key);
        T value = supplier.get();
        
        // 3. 如果查询到数据，写入缓存
        if (value != null) {
            set(key, value, timeout, TimeUnit.MINUTES);
            LOGGER.debug("数据已写入缓存，key: {}", key);
        } else {
            // 缓存空结果，防止缓存穿透（短时间过期）
            set(key, "", 5, TimeUnit.MINUTES);
        }
        
        return value;
    }
}

