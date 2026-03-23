package org.example.springboot.controller;

import org.example.springboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis连接测试Controller
 * 用于验证Redis连接是否正常
 */
@RestController
@RequestMapping("/test/redis")
public class RedisTestController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTestController.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 测试Redis连接
     * 访问: GET /test/redis/connection
     */
    @GetMapping("/connection")
    public Result<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查RedisTemplate是否已注入
            if (redisTemplate == null) {
                result.put("status", "未配置");
                result.put("message", "RedisTemplate未注入，请检查Redis配置");
                result.put("connected", false);
                return Result.error("500", "Redis未配置", result);
            }
            
            // 测试写入
            String testKey = "test:connection:" + System.currentTimeMillis();
            String testValue = "success_" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, testValue, 60, TimeUnit.SECONDS);
            
            // 测试读取
            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
            
            // 验证数据
            boolean isCorrect = testValue.equals(retrievedValue);
            
            // 清理测试数据
            redisTemplate.delete(testKey);
            
            if (isCorrect) {
                result.put("status", "正常");
                result.put("message", "Redis连接成功，读写测试通过");
                result.put("connected", true);
                result.put("testKey", testKey);
                result.put("testValue", testValue);
                result.put("retrievedValue", retrievedValue);
                
                LOGGER.info("Redis连接测试成功: {}", result);
                return Result.success(result);
            } else {
                result.put("status", "异常");
                result.put("message", "Redis连接正常，但数据读写不一致");
                result.put("connected", false);
                return Result.error("500", "Redis读写测试失败", result);
            }
            
        } catch (Exception e) {
            LOGGER.error("Redis连接测试失败", e);
            result.put("status", "失败");
            result.put("message", "Redis连接失败: " + e.getMessage());
            result.put("connected", false);
            result.put("error", e.getClass().getSimpleName());
            return Result.error("500", "Redis连接测试失败: " + e.getMessage(), result);
        }
    }
    
    /**
     * 获取Redis基本信息
     * 访问: GET /test/redis/info
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            if (redisTemplate == null) {
                info.put("configured", false);
                info.put("message", "RedisTemplate未注入");
                return Result.error("500", "Redis未配置", info);
            }
            
            info.put("configured", true);
            var connectionFactory = redisTemplate.getConnectionFactory();
            info.put("connectionFactory", connectionFactory != null ? 
                connectionFactory.getClass().getSimpleName() : "null");
            
            // 尝试获取Redis服务器信息
            try {
                if (connectionFactory != null) {
                    String pingResult = connectionFactory
                        .getConnection()
                        .ping();
                    info.put("ping", pingResult);
                    info.put("status", "连接正常");
                } else {
                    info.put("ping", "失败");
                    info.put("status", "连接工厂为空");
                }
            } catch (Exception e) {
                info.put("ping", "失败");
                info.put("status", "连接异常: " + e.getMessage());
            }
            
            return Result.success(info);
            
        } catch (Exception e) {
            LOGGER.error("获取Redis信息失败", e);
            info.put("configured", false);
            info.put("error", e.getMessage());
            return Result.error("500", "获取Redis信息失败", info);
        }
    }
}

