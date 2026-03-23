/**
 * Redis快速使用示例
 * 
 * 展示了如何在现有的Service中使用Redis进行缓存
 */

// ==================== 示例1：商品查询缓存 ====================

@Service
public class ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private RedisUtil redisUtil;  // 使用工具类，简化操作
    
    /**
     * 带缓存的商品查询
     */
    public Result<?> getProductById(Long id) {
        String cacheKey = "product:" + id;
        
        // 方式1：使用工具类的 getOrSet 方法（最简单）
        Product product = redisUtil.getOrSet(
            cacheKey,
            () -> productMapper.selectById(id),  // 数据库查询逻辑
            30,  // 缓存30分钟
            Product.class
        );
        
        if (product == null) {
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        return ResultUtils.success(product);
    }
    
    /**
     * 手动缓存（更灵活的控制）
     */
    public Result<?> getProductByIdManual(Long id) {
        String cacheKey = "product:" + id;
        
        // 1. 先从缓存获取
        Product product = redisUtil.get(cacheKey, Product.class);
        if (product != null) {
            return ResultUtils.success(product);
        }
        
        // 2. 查询数据库
        product = productMapper.selectById(id);
        if (product == null) {
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        // 3. 存入缓存
        redisUtil.set(cacheKey, product, 30, TimeUnit.MINUTES);
        
        return ResultUtils.success(product);
    }
    
    /**
     * 更新商品时清除缓存
     */
    public Result<?> updateProduct(Long id, Product product) {
        // ... 更新逻辑 ...
        productMapper.updateById(product);
        
        // 清除缓存
        redisUtil.delete("product:" + id);
        redisUtil.delete("product:list:*");  // 如果有列表缓存，也清除
        
        return ResultUtils.success(product);
    }
}


// ==================== 示例2：推荐结果缓存 ====================

@Service
public class RecommendService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    /**
     * 带缓存的推荐生成
     */
    public Result<RecommendationResponse> generateRecommendations(Long userId) {
        String cacheKey = "recommend:user:" + userId;
        
        // 使用缓存，10分钟过期
        RecommendationResponse response = redisUtil.getOrSet(
            cacheKey,
            () -> {
                // 这是耗时的推荐计算逻辑
                return doGenerateRecommendations(userId);
            },
            10,  // 10分钟过期
            RecommendationResponse.class
        );
        
        return ResultUtils.success(response);
    }
    
    /**
     * 当用户有新的行为时，清除推荐缓存
     */
    public void onUserBehaviorChanged(Long userId) {
        redisUtil.delete("recommend:user:" + userId);
    }
}


// ==================== 示例3：浏览量统计 ====================

@Service
public class ProductService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    /**
     * 记录商品浏览量（原子操作）
     */
    public void recordProductView(Long productId) {
        String key = "product:views:" + productId;
        
        // 原子递增
        long views = redisUtil.increment(key);
        
        // 如果是第一次访问，设置过期时间（每天重置）
        if (views == 1) {
            redisUtil.expire(key, 1, TimeUnit.DAYS);
        }
        
        // 可选：每100次访问同步到数据库
        if (views % 100 == 0) {
            asyncUpdateViewsToDatabase(productId, views);
        }
    }
    
    /**
     * 获取商品浏览量
     */
    public Long getProductViews(Long productId) {
        String key = "product:views:" + productId;
        Object views = redisUtil.get(key);
        
        if (views instanceof Number) {
            return ((Number) views).longValue();
        }
        
        return 0L;
    }
}


// ==================== 示例4：防止重复提交 ====================

@Service
public class OrderService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    /**
     * 创建订单（防止重复提交）
     */
    public Result<?> createOrder(Order order) {
        String lockKey = "order:lock:user:" + order.getUserId();
        String lockValue = UUID.randomUUID().toString();
        
        // 尝试获取锁（10秒过期）
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            lockKey, 
            lockValue, 
            10, 
            TimeUnit.SECONDS
        );
        
        if (Boolean.FALSE.equals(locked)) {
            return ResultUtils.error(ResultCode.OPERATION_FAILED, "操作过于频繁，请稍后再试");
        }
        
        try {
            // 执行下单逻辑
            return doCreateOrder(order);
        } finally {
            // 释放锁（只有持有锁的线程才能释放）
            String currentValue = (String) redisUtil.get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisUtil.delete(lockKey);
            }
        }
    }
}


// ==================== 示例5：收藏列表缓存 ====================

@Service
public class FavoriteService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    /**
     * 获取用户收藏列表（带缓存）
     */
    public Result<?> getFavoritesByUserId(Long userId) {
        String cacheKey = "favorites:user:" + userId;
        
        // 使用缓存，5分钟过期
        List<Favorite> favorites = redisUtil.getOrSet(
            cacheKey,
            () -> {
                // 数据库查询逻辑（已优化的批量查询）
                return getFavoritesFromDatabase(userId);
            },
            5,  // 5分钟过期
            new TypeReference<List<Favorite>>() {}.getClass()
        );
        
        return ResultUtils.success(favorites);
    }
    
    /**
     * 添加收藏时清除缓存
     */
    public Result<?> addFavorite(Favorite favorite) {
        // ... 添加逻辑 ...
        
        // 清除该用户的收藏缓存
        redisUtil.delete("favorites:user:" + favorite.getUserId());
        
        return ResultUtils.success();
    }
}


// ==================== 使用说明 ====================

/*
 * 1. 注入RedisUtil
 *    @Autowired
 *    private RedisUtil redisUtil;
 * 
 * 2. 最常用的方法：getOrSet
 *    - 自动处理缓存查询、数据库查询、缓存写入
 *    - 防止缓存穿透（空结果也会缓存）
 * 
 * 3. 手动操作（更灵活）：
 *    - redisUtil.get(key, clazz)     // 获取
 *    - redisUtil.set(key, value, timeout, unit)  // 设置
 *    - redisUtil.delete(key)         // 删除
 *    - redisUtil.hasKey(key)         // 判断是否存在
 * 
 * 4. 原子操作：
 *    - redisUtil.increment(key)      // 递增
 *    - redisUtil.decrement(key)      // 递减
 * 
 * 5. 缓存键命名规范：
 *    - 使用冒号分隔：product:1, favorites:user:123
 *    - 保持统一和清晰
 * 
 * 6. 过期时间设置：
 *    - 商品信息：30分钟
 *    - 推荐结果：10分钟
 *    - 收藏列表：5分钟
 *    - 浏览量：1天
 * 
 * 7. 清除缓存：
 *    - 更新数据时记得清除相关缓存
 *    - 可以使用通配符批量清除：redisUtil.delete("product:list:*")
 */



