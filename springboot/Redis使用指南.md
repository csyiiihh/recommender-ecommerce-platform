# Redis 使用指南

## 📚 目录
1. [基础使用方法](#基础使用方法)
2. [实际业务场景示例](#实际业务场景示例)
3. [最佳实践](#最佳实践)
4. [常见问题](#常见问题)

---

## 基础使用方法

### 方法一：使用 RedisTemplate（推荐，最灵活）

在 Service 类中注入 `RedisTemplate`，然后直接使用。

#### 基本操作示例

```java
@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 1. 存储数据（带过期时间）
    public void cacheProduct(Long productId, Product product) {
        String key = "product:" + productId;
        redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
    }
    
    // 2. 获取数据
    public Product getCachedProduct(Long productId) {
        String key = "product:" + productId;
        return (Product) redisTemplate.opsForValue().get(key);
    }
    
    // 3. 检查键是否存在
    public boolean hasProduct(Long productId) {
        String key = "product:" + productId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    // 4. 删除数据
    public void deleteProduct(Long productId) {
        String key = "product:" + productId;
        redisTemplate.delete(key);
    }
    
    // 5. 设置过期时间
    public void expireProduct(Long productId, long timeout, TimeUnit unit) {
        String key = "product:" + productId;
        redisTemplate.expire(key, timeout, unit);
    }
    
    // 6. 批量操作
    public void cacheProducts(Map<Long, Product> products) {
        products.forEach((id, product) -> {
            String key = "product:" + id;
            redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
        });
    }
    
    // 7. Hash 操作（适合存储对象的部分字段）
    public void cacheProductHash(Long productId, Product product) {
        String key = "product:hash:" + productId;
        redisTemplate.opsForHash().put(key, "name", product.getName());
        redisTemplate.opsForHash().put(key, "price", product.getPrice());
        redisTemplate.opsForHash().put(key, "stock", product.getStock());
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }
    
    // 8. List 操作（适合队列、最新列表）
    public void addToRecentProducts(Long productId) {
        String key = "recent:products";
        redisTemplate.opsForList().leftPush(key, productId);
        // 只保留最新的100条
        redisTemplate.opsForList().trim(key, 0, 99);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }
    
    // 9. Set 操作（适合去重、标签）
    public void addProductTag(Long productId, String tag) {
        String key = "product:tags:" + productId;
        redisTemplate.opsForSet().add(key, tag);
    }
    
    // 10. 原子操作（计数器、限流）
    public Long incrementViewCount(Long productId) {
        String key = "product:views:" + productId;
        Long count = redisTemplate.opsForValue().increment(key);
        // 设置过期时间（只在首次设置时）
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
        return count;
    }
}
```

---

## 实际业务场景示例

### 场景1：商品查询缓存

**优化 ProductService.getProductById() 方法**

```java
public Result<?> getProductById(Long id) {
    // 1. 先从缓存查询
    String cacheKey = "product:" + id;
    Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
    
    if (cachedProduct != null) {
        LOGGER.debug("从缓存获取商品: {}", id);
        return ResultUtils.success(cachedProduct);
    }
    
    // 2. 缓存未命中，查询数据库
    Product product = productMapper.selectById(id);
    if (product == null) {
        return ResultUtils.dataNotFound("商品不存在");
    }
    
    // 3. 将结果存入缓存（30分钟过期）
    redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
    LOGGER.debug("商品已存入缓存: {}", id);
    
    return ResultUtils.success(product);
}

// 更新商品时清除缓存
@Caching(evict = {
    @CacheEvict(value = "productPages", allEntries = true),
    @CacheEvict(value = "products", allEntries = true)
})
public Result<?> updateProduct(Long id, Product product) {
    // ... 现有更新逻辑 ...
    
    // 清除该商品的缓存
    String cacheKey = "product:" + id;
    redisTemplate.delete(cacheKey);
    
    return ResultUtils.success(product);
}
```

### 场景2：推荐结果缓存

**优化 RecommendService.generateRecommendations() 方法**

```java
public Result<RecommendationResponse> generateRecommendations(Long userId) {
    // 1. 从缓存获取推荐结果
    String cacheKey = "recommend:user:" + userId;
    RecommendationResponse cached = (RecommendationResponse) redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        LOGGER.info("从缓存获取推荐结果，用户ID: {}", userId);
        return ResultUtils.success(cached);
    }
    
    // 2. 缓存未命中，生成推荐结果（耗时操作）
    RecommendationResponse response = doGenerateRecommendations(userId);
    
    // 3. 存入缓存（10分钟过期，推荐结果不需要太长时间）
    redisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
    LOGGER.info("推荐结果已存入缓存，用户ID: {}", userId);
    
    return ResultUtils.success(response);
}

// 当用户有新的购买或收藏时，清除推荐缓存
public void invalidateRecommendationCache(Long userId) {
    String cacheKey = "recommend:user:" + userId;
    redisTemplate.delete(cacheKey);
}
```

### 场景3：收藏列表缓存

**优化 FavoriteService.getFavoritesByUserId() 方法**

```java
public Result<?> getFavoritesByUserId(Long userId) {
    // 1. 从缓存获取
    String cacheKey = "favorites:user:" + userId;
    List<Favorite> cached = (List<Favorite>) redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        LOGGER.debug("从缓存获取收藏列表，用户ID: {}", userId);
        return ResultUtils.success(cached);
    }
    
    // 2. 查询数据库（已优化的批量查询）
    List<Favorite> favorites = getFavoritesFromDatabase(userId);
    
    // 3. 存入缓存（5分钟过期）
    redisTemplate.opsForValue().set(cacheKey, favorites, 5, TimeUnit.MINUTES);
    
    return ResultUtils.success(favorites);
}

// 添加或更新收藏时清除缓存
public Result<?> updateFavoriteStatus(Long userId, Long productId, Integer status) {
    // ... 更新逻辑 ...
    
    // 清除该用户的收藏缓存
    String cacheKey = "favorites:user:" + userId;
    redisTemplate.delete(cacheKey);
    
    return ResultUtils.success();
}
```

### 场景4：商品浏览计数

```java
public Long recordProductView(Long productId) {
    String key = "product:views:" + productId;
    
    // 原子递增
    Long views = redisTemplate.opsForValue().increment(key);
    
    // 设置过期时间（每天重置）
    if (views == 1) {
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }
    
    // 可选：每100次访问同步到数据库
    if (views % 100 == 0) {
        asyncUpdateProductViews(productId, views);
    }
    
    return views;
}
```

### 场景5：热门商品排行榜

```java
public List<Long> getHotProducts(int limit) {
    String key = "hot:products";
    
    // 使用 Sorted Set (ZSet) 存储，分数为浏览量
    Set<Object> productIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
    
    return productIds.stream()
        .map(id -> (Long) id)
        .collect(Collectors.toList());
}

public void updateProductScore(Long productId, double score) {
    String key = "hot:products";
    redisTemplate.opsForZSet().add(key, productId, score);
    redisTemplate.expire(key, 1, TimeUnit.DAYS);
}
```

### 场景6：防止重复提交（分布式锁）

```java
public boolean tryLock(String lockKey, long timeoutSeconds) {
    String key = "lock:" + lockKey;
    String value = UUID.randomUUID().toString();
    
    // 尝试获取锁
    Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
    
    return Boolean.TRUE.equals(success);
}

public void releaseLock(String lockKey, String lockValue) {
    String key = "lock:" + lockKey;
    String currentValue = (String) redisTemplate.opsForValue().get(key);
    
    // 只有持有锁的线程才能释放
    if (lockValue.equals(currentValue)) {
        redisTemplate.delete(key);
    }
}

// 使用示例：防止重复下单
public Result<?> createOrder(Order order) {
    String lockKey = "order:lock:user:" + order.getUserId();
    
    if (!tryLock(lockKey, 10)) {
        return ResultUtils.error(ResultCode.OPERATION_FAILED, "操作过于频繁，请稍后再试");
    }
    
    try {
        // 执行下单逻辑
        return doCreateOrder(order);
    } finally {
        releaseLock(lockKey, lockValue);
    }
}
```

---

## 最佳实践

### 1. 缓存键命名规范

```java
// ✅ 好的命名
"product:1"                    // 商品
"favorites:user:123"          // 用户收藏
"recommend:user:123"          // 用户推荐
"product:views:1"             // 商品浏览量
"hot:products"                // 热门商品

// ❌ 不好的命名
"p1"                          // 不清楚含义
"data"                        // 太泛化
"user123fav"                  // 格式不统一
```

### 2. 缓存过期时间设置

```java
// 根据数据更新频率设置
- 商品信息：30分钟（商品信息变化不频繁）
- 推荐结果：10分钟（推荐结果需要及时更新）
- 收藏列表：5分钟（用户可能频繁添加收藏）
- 浏览量：1天（每天重置）
- 热点数据：1小时（热门数据更新较快）
```

### 3. 缓存穿透防护

```java
public Product getProductSafely(Long id) {
    String cacheKey = "product:" + id;
    
    // 1. 先查缓存
    Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
    if (product != null) {
        return product;
    }
    
    // 2. 检查是否缓存了空结果（防止穿透）
    String nullKey = "product:null:" + id;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
        return null; // 之前查询过，不存在
    }
    
    // 3. 查询数据库
    product = productMapper.selectById(id);
    
    if (product != null) {
        // 缓存存在的数据
        redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
    } else {
        // 缓存空结果，短时间过期（防止恶意查询）
        redisTemplate.opsForValue().set(nullKey, "", 5, TimeUnit.MINUTES);
    }
    
    return product;
}
```

### 4. 缓存更新策略

```java
// 策略1：写时删除（Write-Through + Delete）
public Result<?> updateProduct(Long id, Product product) {
    // 先更新数据库
    productMapper.updateById(product);
    
    // 删除缓存，下次查询时重新加载
    redisTemplate.delete("product:" + id);
    
    return ResultUtils.success(product);
}

// 策略2：写时更新（Write-Through + Update）
public Result<?> updateProduct(Long id, Product product) {
    // 更新数据库
    productMapper.updateById(product);
    
    // 同时更新缓存
    redisTemplate.opsForValue().set("product:" + id, product, 30, TimeUnit.MINUTES);
    
    return ResultUtils.success(product);
}
```

### 5. 批量操作优化

```java
// ❌ 不好的方式：循环单个操作
for (Long id : productIds) {
    redisTemplate.delete("product:" + id);
}

// ✅ 好的方式：批量删除
Set<String> keys = productIds.stream()
    .map(id -> "product:" + id)
    .collect(Collectors.toSet());
redisTemplate.delete(keys);
```

---

## 常见问题

### Q1: 如何切换使用 Redis 替代 Caffeine？

在 `application.properties` 中修改：

```properties
# 将这行注释掉或删除
# spring.cache.type=caffeine

# 启用 Redis 缓存
spring.cache.type=redis
```

然后现有的 `@Cacheable`、`@CacheEvict` 注解会自动使用 Redis。

### Q2: Redis 和 Caffeine 可以同时使用吗？

可以！它们不冲突：
- **Caffeine**：继续用于 `@Cacheable` 注解（本地缓存）
- **RedisTemplate**：用于需要手动控制的场景（分布式缓存）

### Q3: 如何查看 Redis 中的数据？

可以使用 Redis 客户端工具：
- **命令行**：`redis-cli`（如果已安装）
- **图形工具**：RedisDesktopManager、Another Redis Desktop Manager

```bash
# 查看所有键
KEYS *

# 查看特定键
GET product:1

# 查看键的过期时间
TTL product:1
```

### Q4: 内存满了怎么办？

Redis 有自动淘汰策略，在 `application.properties` 中配置（如果需要）：

```properties
# Redis 最大内存
spring.data.redis.lettuce.pool.max-active=16

# 内存淘汰策略（LRU：最近最少使用）
spring.data.redis.lettuce.pool.max-idle=8
```

### Q5: 如何监控 Redis 使用情况？

可以通过测试接口查看：

```bash
GET http://localhost:1234/api/test/redis/info
```

---

## 📝 总结

1. **灵活使用**：`RedisTemplate` 提供最灵活的操作方式
2. **合理缓存**：缓存热点数据，避免缓存不重要或频繁变化的数据
3. **及时清理**：更新数据时记得清除或更新相关缓存
4. **设置过期**：所有缓存都应该设置合理的过期时间
5. **键名规范**：使用统一、清晰的键名规范

现在你可以在项目中开始使用 Redis 了！🚀



