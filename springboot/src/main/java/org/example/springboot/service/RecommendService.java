package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.dto.RecommendationExplanation;
import org.example.springboot.dto.RecommendationResponse;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Favorite;
import org.example.springboot.entity.RecommendationFeedback;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.FavoriteMapper;
import org.example.springboot.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendService.class);

    // 反馈类型权重配置
    private static final Map<String, Double> FEEDBACK_WEIGHTS;
    private static final Set<String> ALLOWED_FEEDBACK_TYPES;
    static {
        Map<String, Double> weights = new HashMap<>();
        weights.put("LIKE", 1.1);           // 喜欢：小幅加权
        weights.put("DISLIKE", 0.1);        // 不喜欢：降权90%
        weights.put("ALREADY_OWN", 0.0);    // 已拥有：直接过滤
        weights.put("NOT_INTERESTED", 0.3); // 不感兴趣：大幅降权
        weights.put("TOO_EXPENSIVE", 0.5);  // 太贵：中等降权
        weights.put("WILL_BUY_LATER", 0.7); // 稍后购买：轻降权
        FEEDBACK_WEIGHTS = Collections.unmodifiableMap(weights);
        ALLOWED_FEEDBACK_TYPES = Collections.unmodifiableSet(weights.keySet());
    }

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private FavoriteMapper favoriteMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private org.example.springboot.mapper.RecommendationFeedbackMapper feedbackMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 计算用户相似度矩?
    private Map<Long, Map<Long, Double>> calculateUserSimilarity() {
        // 构建用户-商品行为矩阵
        Map<Long, Set<Long>> userProductMap = new HashMap<>();
        
        // 获取所有订单数据（已完成的订单?
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, 3); // 已完成状?
        List<Order> orders = orderMapper.selectList(orderWrapper);
        
        // 获取所有收藏数据（有效的收藏）
        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getStatus, 1); // 收藏状态为1
        List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);

        // 构建用户-商品映射，购买行为权重为2，收藏行为权重为1
        for (Order order : orders) {
            Set<Long> products = userProductMap.computeIfAbsent(order.getUserId(), k -> new HashSet<>());
            products.add(order.getProductId());
            products.add(order.getProductId()); // 添加两次表示更高权重
        }
        
        for (Favorite favorite : favorites) {
            userProductMap.computeIfAbsent(favorite.getUserId(), k -> new HashSet<>())
                         .add(favorite.getProductId());
        }

        // 计算用户相似?
        Map<Long, Map<Long, Double>> similarityMatrix = new HashMap<>();
        List<Long> userIds = new ArrayList<>(userProductMap.keySet());

        for (int i = 0; i < userIds.size(); i++) {
            Long user1 = userIds.get(i);
            Map<Long, Double> userSimilarities = new HashMap<>();
            similarityMatrix.put(user1, userSimilarities);

            for (int j = i + 1; j < userIds.size(); j++) {
                Long user2 = userIds.get(j);
                double similarity = calculateCosineSimilarity(
                    userProductMap.get(user1), 
                    userProductMap.get(user2)
                );
                userSimilarities.put(user2, similarity);
                similarityMatrix.computeIfAbsent(user2, k -> new HashMap<>())
                               .put(user1, similarity);
            }
        }

        return similarityMatrix;
    }

    // 计算余弦相似?
    private double calculateCosineSimilarity(Set<Long> set1, Set<Long> set2) {
        if (set1 == null || set2 == null || set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        // 计算交集
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // 添加最小阈?
        if (intersection.isEmpty()) {
            return 0.0;
        }

        // 计算余弦相似?- 交集大小作为点积，除以两个集合大小的平方根乘?
        double numerator = intersection.size();
        double denominator = Math.sqrt(set1.size()) * Math.sqrt(set2.size());
        double similarity = numerator / denominator;
        
        LOGGER.debug("计算相似? set1={}, set2={}, similarity={}", set1, set2, similarity);
        return similarity;
    }

    /**
     * 优化版本：只计算与指定用户相关的相似度，而不是所有用户
     * 这样可以大大减少计算量和数据库查询
     */
    private Map<Long, Double> calculateUserSimilarityForUser(Long userId, Set<Long> userProducts) {
        if (userProducts == null || userProducts.isEmpty()) {
            return new HashMap<>();
        }

        // 只获取与当前用户有共同商品的用户（优化：只查询相关订单和收藏）
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, 3)
                   .in(Order::getProductId, userProducts)
                   .ne(Order::getUserId, userId); // 排除当前用户
        List<Order> relevantOrders = orderMapper.selectList(orderWrapper);

        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getStatus, 1)
                      .in(Favorite::getProductId, userProducts)
                      .ne(Favorite::getUserId, userId); // 排除当前用户
        List<Favorite> relevantFavorites = favoriteMapper.selectList(favoriteWrapper);

        // 构建其他用户-商品映射
        Map<Long, Set<Long>> otherUserProductMap = new HashMap<>();
        for (Order order : relevantOrders) {
            Set<Long> products = otherUserProductMap.computeIfAbsent(order.getUserId(), k -> new HashSet<>());
            products.add(order.getProductId());
            products.add(order.getProductId()); // 购买权重为2
        }
        for (Favorite favorite : relevantFavorites) {
            otherUserProductMap.computeIfAbsent(favorite.getUserId(), k -> new HashSet<>())
                             .add(favorite.getProductId());
        }

        // 只计算与当前用户的相似度
        Map<Long, Double> similarUsers = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : otherUserProductMap.entrySet()) {
            double similarity = calculateCosineSimilarity(userProducts, entry.getValue());
            if (similarity > 0.0) { // 只保留有相似度的用户
                similarUsers.put(entry.getKey(), similarity);
            }
        }

        LOGGER.debug("为用户 {} 找到 {} 个相似用户", userId, similarUsers.size());
        return similarUsers;
    }

    // 为指定用户生成推?
    public Result<?> generateRecommendations(Long userId) {
        try {
            // 获取用户的订单和收藏数据
            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getUserId, userId)
                       .eq(Order::getStatus, 3); // 已完成的订单
            List<Order> userOrders = orderMapper.selectList(orderWrapper);

            LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
            favoriteWrapper.eq(Favorite::getUserId, userId)
                          .eq(Favorite::getStatus, 1); // 有效的收?
            List<Favorite> userFavorites = favoriteMapper.selectList(favoriteWrapper);

            // 获取用户的商品集?
            Set<Long> userProducts = new HashSet<>();
            userOrders.forEach(order -> userProducts.add(order.getProductId()));
            userFavorites.forEach(favorite -> userProducts.add(favorite.getProductId()));

            if (userProducts.isEmpty()) {
                LOGGER.warn("用户 {} 没有任何订单或收藏记录", userId);
            }

            // 获取用户相似度矩?
            Map<Long, Map<Long, Double>> similarityMatrix = calculateUserSimilarity();

            // 获取相似用户
            Map<Long, Double> similarUsers = new HashMap<>();
            // 获取当前用户与其他用户的相似?
            if (similarityMatrix.containsKey(userId)) {
                similarUsers.putAll(similarityMatrix.get(userId));
            }
            // 获取其他用户与当前用户的相似?
            for (Map.Entry<Long, Map<Long, Double>> entry : similarityMatrix.entrySet()) {
                if (entry.getValue().containsKey(userId)) {
                    similarUsers.put(entry.getKey(), entry.getValue().get(userId));
                }
            }

            // 动态调整相似度阈?
            double similarityThreshold;
            if (userProducts.size() < 3) { // 新用?
                similarityThreshold = 0.2;
            } else if (userProducts.size() > 10) { // 活跃用户
                similarityThreshold = 0.4;
            } else {
                similarityThreshold = 0.3;
            }

            // 过滤和排序相似用?
            similarUsers = similarUsers.entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= similarityThreshold)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // 收集推荐商品及其得分
            Map<Long, Double> productScores = new HashMap<>();
            for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
                Long similarUserId = entry.getKey();
                double similarity = entry.getValue();

                // 获取相似用户的订单和收藏
                List<Order> similarUserOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, similarUserId)
                        .eq(Order::getStatus, 3)
                );
                
                List<Favorite> similarUserFavorites = favoriteMapper.selectList(
                    new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, similarUserId)
                        .eq(Favorite::getStatus, 1)
                );

                // 计算推荐分数（订单权?, 收藏权???
                for (Order order : similarUserOrders) {
                    if (!userProducts.contains(order.getProductId())) {
                        double newScore = productScores.getOrDefault(order.getProductId(), 0.0) + similarity * 2;
                        productScores.put(order.getProductId(), newScore);
                    }
                }
                
                for (Favorite favorite : similarUserFavorites) {
                    if (!userProducts.contains(favorite.getProductId())) {
                        double newScore = productScores.getOrDefault(favorite.getProductId(), 0.0) + similarity;
                        productScores.put(favorite.getProductId(), newScore);
                    }
                }
            }

            List<Product> recommendations;
            if (productScores.isEmpty()) {
                LOGGER.info("没有找到相似用户，使用基于销量的推荐");
                recommendations = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                        .orderByDesc(Product::getSalesCount)
                        .last("LIMIT 12")
                );
            } else {
                List<Map.Entry<Long, Double>> sortedProducts = new ArrayList<>(productScores.entrySet());
                sortedProducts.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                List<Long> recommendedIds = sortedProducts.stream()
                    .limit(12)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

                recommendations = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                        .in(Product::getId, recommendedIds)
                );
            }

            LOGGER.info("为用户 {} 生成推荐成功，推荐数量：{}", userId, recommendations.size());
            return ResultUtils.success(recommendations);
        } catch (Exception e) {
            LOGGER.error("生成推荐异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "生成推荐失败：" + e.getMessage());
        }
    }

    // 定时更新推荐
    public void updateRecommendations() {
        try {
            // 获取所有用户ID
            List<Long> userIds = orderMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .map(Order::getUserId)
                .distinct()
                .toList();

            // 为每个用户生成推?
            for (Long userId : userIds) {
                generateRecommendations(userId);
            }

            LOGGER.info("成功更新所有用户推");
        } catch (Exception e) {
            LOGGER.error("更新推荐失败: {}", e.getMessage());
        }
    }

    /**
     * 获取用户分类匹配度（基于订单+收藏的分类分布，趋势基于销量占比）
     */
    public Result<?> getCategoryMatches(Long userId) {
        try {
            // 用户订单与收藏
            List<Order> userOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getUserId, userId)
                            .eq(Order::getStatus, 3)
            );
            List<Favorite> userFavorites = favoriteMapper.selectList(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, userId)
                            .eq(Favorite::getStatus, 1)
            );

            // 聚合用户行为涉及的商品
            Set<Long> productIds = new HashSet<>();
            userOrders.forEach(o -> productIds.add(o.getProductId()));
            userFavorites.forEach(f -> productIds.add(f.getProductId()));

            if (productIds.isEmpty()) {
                return Result.success(Collections.emptyMap());
            }

            List<Product> userProducts = productMapper.selectBatchIds(productIds);
            Map<Long, Long> categoryCount = userProducts.stream()
                    .filter(p -> p.getCategoryId() != null)
                    .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

            long total = categoryCount.values().stream().mapToLong(Long::longValue).sum();

            // 全局销量趋势
            List<Product> allProducts = productMapper.selectList(new LambdaQueryWrapper<Product>());
            Map<Long, Long> categorySales = allProducts.stream()
                    .filter(p -> p.getCategoryId() != null)
                    .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.summingLong(p -> p.getSalesCount() == null ? 0 : p.getSalesCount())));
            long totalSales = categorySales.values().stream().mapToLong(Long::longValue).sum();

            Map<Long, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<Long, Long> entry : categoryCount.entrySet()) {
                Long cid = entry.getKey();
                double match = total == 0 ? 0.0 : (entry.getValue() * 100.0 / total);
                double trend = 0.0;
                if (totalSales > 0 && categorySales.containsKey(cid)) {
                    trend = categorySales.get(cid) * 100.0 / totalSales;
                }
                Map<String, Object> info = new HashMap<>();
                info.put("match", match);
                info.put("trend", trend);
                result.put(cid, info);
            }

            LOGGER.debug("获取用户分类匹配度成功，用户ID：{}，分类数：{}", userId, result.size());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("获取分类匹配度异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取分类匹配度失败：" + e.getMessage());
        }
    }

    /**
     * 生成带解释的推荐（前台可感知版本）
     */
    public Result<RecommendationResponse> generateRecommendationsWithExplanation(Long userId, String algorithmType, String weightsJson) {
        long startTime = System.currentTimeMillis();
        try {
            // 解析算法类型与权重
            String algoType = (algorithmType == null || algorithmType.isBlank())
                    ? "COLLABORATIVE"
                    : algorithmType.trim().toUpperCase(Locale.ROOT);
            Map<String, Integer> weightMap = parseWeights(weightsJson);
            double collaborativeWeight = weightMap.getOrDefault("collaborative", 60) / 100.0;
            double contentWeight = weightMap.getOrDefault("content", 30) / 100.0;
            double trendingWeight = weightMap.getOrDefault("trending", 10) / 100.0;

            // 获取用户的订单和收藏数据
            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getUserId, userId)
                       .eq(Order::getStatus, 3);
            List<Order> userOrders = orderMapper.selectList(orderWrapper);

            LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
            favoriteWrapper.eq(Favorite::getUserId, userId)
                          .eq(Favorite::getStatus, 1);
            List<Favorite> userFavorites = favoriteMapper.selectList(favoriteWrapper);

            // 获取用户的商品集合
            Set<Long> userProducts = new HashSet<>();
            userOrders.forEach(order -> userProducts.add(order.getProductId()));
            userFavorites.forEach(favorite -> userProducts.add(favorite.getProductId()));

            int userBehaviorCount = userOrders.size() + userFavorites.size();
            boolean fallbackUsed = false;
            String fallbackReason = null;

            // 非协同过滤算法的快速分支（单一算法）
            if ("TRENDING".equalsIgnoreCase(algoType)) {
                RecommendationResponse response = buildTrendingResponse(trendingWeight, startTime);
                return Result.success(response);
            }

            if ("SERENDIPITY".equalsIgnoreCase(algoType)) {
                RecommendationResponse response = buildSerendipityResponse(startTime);
                return Result.success(response);
            }

            if ("CONTENT".equalsIgnoreCase(algoType)) {
                RecommendationResponse response = buildContentResponse(contentWeight, startTime);
                return Result.success(response);
            }

            // 优化：只计算与当前用户相关的相似度，而不是所有用户
            Map<Long, Double> similarUsers = calculateUserSimilarityForUser(userId, userProducts);

            // 动态调整相似度阈值
            double similarityThreshold;
            if (userProducts.size() < 3) {
                similarityThreshold = 0.2;
            } else if (userProducts.size() > 10) {
                similarityThreshold = 0.4;
            } else {
                similarityThreshold = 0.3;
            }

            // 过滤和排序相似用户
            similarUsers = similarUsers.entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= similarityThreshold)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            int similarUserCount = similarUsers.size();

            // 获取用户的历史反馈（用于权重/过滤）
            LambdaQueryWrapper<RecommendationFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
            feedbackWrapper.eq(RecommendationFeedback::getUserId, userId);
            List<RecommendationFeedback> userFeedbacks = feedbackMapper.selectList(feedbackWrapper);

            Map<Long, Double> feedbackWeightMap = new HashMap<>();
            Set<Long> blockedProducts = new HashSet<>();
            for (RecommendationFeedback feedback : userFeedbacks) {
                String type = feedback.getFeedbackType() == null ? "" : feedback.getFeedbackType().trim().toUpperCase();
                double weight = FEEDBACK_WEIGHTS.getOrDefault(type, 1.0);
                Long pid = feedback.getProductId();
                if (pid == null) {
                    continue;
                }
                if (weight == 0.0) {
                    blockedProducts.add(pid);
                }
                // 取最小权重（保守降权）
                double current = feedbackWeightMap.getOrDefault(pid, 1.0);
                feedbackWeightMap.put(pid, Math.min(current, weight));
            }
            
            // 收集推荐商品及其得分和来源信息
            Map<Long, ProductScoreInfo> productScoreMap = new HashMap<>();
            
            // 记录每个商品的来源（哪些相似用户推荐了它，以及来源商品）
            Map<Long, Map<Long, Double>> productSimilarUserMap = new HashMap<>(); // 商品 -> 相似用户ID -> 相似度
            Map<Long, Set<Long>> productSourceMap = new HashMap<>(); // 商品 -> 来源商品ID集合

            // 优化：批量查询所有相似用户的订单和收藏，避免 N+1 查询
            Set<Long> similarUserIds = similarUsers.keySet();
            Map<Long, List<Order>> similarUserOrdersMap = new HashMap<>();
            Map<Long, List<Favorite>> similarUserFavoritesMap = new HashMap<>();
            
            if (!similarUserIds.isEmpty()) {
                // 批量查询所有相似用户的订单
                List<Order> allSimilarUserOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                        .in(Order::getUserId, similarUserIds)
                        .eq(Order::getStatus, 3)
                );
                // 按用户ID分组
                for (Order order : allSimilarUserOrders) {
                    similarUserOrdersMap.computeIfAbsent(order.getUserId(), k -> new ArrayList<>())
                                       .add(order);
                }
                
                // 批量查询所有相似用户的收藏
                List<Favorite> allSimilarUserFavorites = favoriteMapper.selectList(
                    new LambdaQueryWrapper<Favorite>()
                        .in(Favorite::getUserId, similarUserIds)
                        .eq(Favorite::getStatus, 1)
                );
                // 按用户ID分组
                for (Favorite favorite : allSimilarUserFavorites) {
                    similarUserFavoritesMap.computeIfAbsent(favorite.getUserId(), k -> new ArrayList<>())
                                           .add(favorite);
                }
            }

            for (Map.Entry<Long, Double> entry : similarUsers.entrySet()) {
                Long similarUserId = entry.getKey();
                double similarity = entry.getValue();

                // 从批量查询结果中获取
                List<Order> similarUserOrders = similarUserOrdersMap.getOrDefault(similarUserId, new ArrayList<>());
                List<Favorite> similarUserFavorites = similarUserFavoritesMap.getOrDefault(similarUserId, new ArrayList<>());

                // 获取相似用户购买/收藏的商品（作为来源商品）
                Set<Long> similarUserProductIds = new HashSet<>();
                similarUserOrders.forEach(order -> similarUserProductIds.add(order.getProductId()));
                similarUserFavorites.forEach(fav -> similarUserProductIds.add(fav.getProductId()));

                // 计算推荐分数
                for (Order order : similarUserOrders) {
                    Long productId = order.getProductId();
                    if (productId == null || userProducts.contains(productId) || blockedProducts.contains(productId)) {
                        continue;
                    }
                    double scoreMultiplier = feedbackWeightMap.getOrDefault(productId, 1.0);

                    ProductScoreInfo info = productScoreMap.computeIfAbsent(productId,
                        k -> new ProductScoreInfo());
                    info.cfScore += similarity * 2 * scoreMultiplier;
                    info.trendScore = computeTrendScore(productId);
                    info.contentScore = computeContentScore(productId);

                    productSimilarUserMap.computeIfAbsent(productId, k -> new HashMap<>())
                        .put(similarUserId, similarity);
                    productSourceMap.computeIfAbsent(productId, k -> new HashSet<>())
                        .addAll(similarUserProductIds);
                }
                
                for (Favorite favorite : similarUserFavorites) {
                    Long productId = favorite.getProductId();
                    if (productId == null || userProducts.contains(productId) || blockedProducts.contains(productId)) {
                        continue;
                    }
                    double scoreMultiplier = feedbackWeightMap.getOrDefault(productId, 1.0);

                    ProductScoreInfo info = productScoreMap.computeIfAbsent(productId,
                        k -> new ProductScoreInfo());
                    info.cfScore += similarity * scoreMultiplier;
                    info.trendScore = computeTrendScore(productId);
                    info.contentScore = computeContentScore(productId);

                    productSimilarUserMap.computeIfAbsent(productId, k -> new HashMap<>())
                        .put(similarUserId, similarity);
                    productSourceMap.computeIfAbsent(productId, k -> new HashSet<>())
                        .addAll(similarUserProductIds);
                }
            }

            List<Product> recommendations;
            List<RecommendationExplanation> explanations = new ArrayList<>();

            if (productScoreMap.isEmpty()) {
                // 降级策略：基于销量推荐
                fallbackUsed = true;
                // 根据用户行为和相似用户情况生成降级原因
                boolean insufficientBehavior = userBehaviorCount < 5;
                boolean insufficientSimilarUsers = similarUserCount < 3;
                if (insufficientBehavior) {
                    fallbackReason = "您的历史订单和收藏行为较少，目前优先采用热门/销量推荐作为兜底结果。";
                } else if (insufficientSimilarUsers) {
                    fallbackReason = "当前与您兴趣相似的用户数量不足或相似度偏低，暂时采用热门/销量推荐为主。";
                } else {
                    fallbackReason = "当前个性化信号不足，暂时采用热门/销量推荐作为兜底结果。";
                }
                recommendations = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                        .orderByDesc(Product::getSalesCount)
                        .last("LIMIT 20")
                );
                // 过滤被屏蔽的商品（例如 ALREADY_OWN）
                recommendations = recommendations.stream()
                        .filter(p -> !blockedProducts.contains(p.getId()))
                        .limit(12)
                        .collect(Collectors.toList());
                
                for (Product product : recommendations) {
                    RecommendationExplanation explanation = new RecommendationExplanation();
                    explanation.setProductId(product.getId());
                    explanation.setScore((double) (product.getSalesCount() == null ? 0 : product.getSalesCount()));
                    explanation.setReasonType("POPULAR");
                    explanation.setReasonDescription("该商品销量较高，受到众多用户喜爱");
                    explanation.setPrimaryReason("热销商品推荐");
                    explanation.setSimilarUserCount(0);
                    explanation.setSimilarUserHints(Collections.emptyList());
                    explanation.setSourceProductIds(Collections.emptyList());
                    explanation.setConfidence(0.6);
                    explanation.setTimestamp(System.currentTimeMillis());

                    // 多算法得分占位：热门推荐权重高，其他为0
                    Map<String, Double> algorithmScores = new HashMap<>();
                    algorithmScores.put("trending", (double) product.getSalesCount());
                    algorithmScores.put("collaborativeFiltering", 0.0);
                    algorithmScores.put("contentBased", 0.0);
                    algorithmScores.put("portrait", 0.0);
                    explanation.setAlgorithmScores(algorithmScores);

                    // 支撑因素
                    List<RecommendationExplanation.SupportingFactor> supportingFactors = new ArrayList<>();
                    RecommendationExplanation.SupportingFactor trend = new RecommendationExplanation.SupportingFactor();
                    trend.setType("TREND");
                    trend.setDescription("近期销量高，用户关注度高");
                    trend.setScore(Math.min(100.0, product.getSalesCount() * 1.0));
                    supportingFactors.add(trend);
                    explanation.setSupportingFactors(supportingFactors);

                    explanations.add(explanation);
                }
            } else {
                // 协同过滤推荐
                List<Map.Entry<Long, ProductScoreInfo>> sortedProducts = new ArrayList<>(
                    productScoreMap.entrySet());
                sortedProducts.sort((e1, e2) -> Double.compare(
                        e2.getValue().totalScore(collaborativeWeight, contentWeight, trendingWeight),
                        e1.getValue().totalScore(collaborativeWeight, contentWeight, trendingWeight)
                ));

                List<Long> recommendedIds = sortedProducts.stream()
                    .limit(12)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

                recommendations = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                        .in(Product::getId, recommendedIds)
                );

                // 按推荐分数排序
                Map<Long, Product> productMap = recommendations.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
                
                int index = 0;
                for (Map.Entry<Long, ProductScoreInfo> entry : sortedProducts) {
                    if (index >= 12) break;
                    Long productId = entry.getKey();
                    ProductScoreInfo info = entry.getValue();
                    Product product = productMap.get(productId);
                    if (product == null) continue;

                    RecommendationExplanation explanation = new RecommendationExplanation();
                    explanation.setProductId(productId);
                    double totalScore = info.totalScore(collaborativeWeight, contentWeight, trendingWeight);
                    explanation.setScore(totalScore);
                    explanation.setReasonType("COLLABORATIVE");
                    
                    // 生成推荐理由描述
                    Map<Long, Double> similarUsersForProduct = productSimilarUserMap.get(productId);
                    int count = similarUsersForProduct != null ? similarUsersForProduct.size() : 0;
                    Set<Long> sourceProducts = productSourceMap.get(productId);
                    
                    StringBuilder reasonDesc = new StringBuilder();
                    if (count > 0) {
                        reasonDesc.append("有 ").append(count).append(" 位与您兴趣相似的用户也喜欢此商品");
                        if (sourceProducts != null && !sourceProducts.isEmpty()) {
                            reasonDesc.append("，基于您对相关商品的偏好推荐");
                        }
                    } else {
                        reasonDesc.append("基于协同过滤算法推荐");
                    }
                    explanation.setReasonDescription(reasonDesc.toString());
                    explanation.setPrimaryReason(reasonDesc.toString());
                    
                    explanation.setSimilarUserCount(count);
                    
                    // 生成相似用户提示（脱敏）
                    List<String> userHints = new ArrayList<>();
                    if (similarUsersForProduct != null) {
                        int hintIndex = 1;
                        for (Map.Entry<Long, Double> userEntry : similarUsersForProduct.entrySet()) {
                            double sim = userEntry.getValue();
                            userHints.add(String.format("用户%d (相似度: %.1f%%)", hintIndex++, sim * 100));
                            if (userHints.size() >= 3) break; // 最多显示3个
                        }
                    }
                    explanation.setSimilarUserHints(userHints);
                    
                    // 设置来源商品（取交集，即用户也购买/收藏过的商品）
                    Set<Long> sourceProductIds = new HashSet<>();
                    if (sourceProducts != null) {
                        Set<Long> copySources = new HashSet<>(sourceProducts);
                        copySources.retainAll(userProducts); // 取交集
                        sourceProductIds.addAll(copySources);
                        if (sourceProductIds.size() > 5) {
                            sourceProductIds = sourceProductIds.stream().limit(5).collect(Collectors.toSet());
                        }
                    }
                    explanation.setSourceProductIds(new ArrayList<>(sourceProductIds));
                    
                    // 计算置信度
                    double confidence = Math.min(0.95, 0.5 + (totalScore / 10.0));
                    explanation.setConfidence(confidence);
                    explanation.setTimestamp(System.currentTimeMillis());

                    // 多算法得分（示例：协同过滤得分为主，热门/内容/画像为占位或简易估算）
                    Map<String, Double> algorithmScores = new HashMap<>();
                    algorithmScores.put("collaborativeFiltering", info.cfScore);
                    algorithmScores.put("contentBased", info.contentScore);
                    algorithmScores.put("trending", info.trendScore);
                    algorithmScores.put("portrait", Math.min(1.0, confidence)); // 暂用置信度占位
                    explanation.setAlgorithmScores(algorithmScores);

                    // 支撑因素列表
                    List<RecommendationExplanation.SupportingFactor> factors = new ArrayList<>();
                    if (count > 0) {
                        RecommendationExplanation.SupportingFactor factor = new RecommendationExplanation.SupportingFactor();
                        factor.setType("SIMILAR_USER");
                        factor.setDescription("与您相似的用户也购买/收藏了该商品");
                        factor.setScore(Math.min(100.0, count * 10.0));
                        factors.add(factor);
                    }
                    if (!sourceProductIds.isEmpty()) {
                        RecommendationExplanation.SupportingFactor factor = new RecommendationExplanation.SupportingFactor();
                        factor.setType("HISTORY");
                        factor.setDescription("基于您历史的相关商品偏好");
                        factor.setScore(20.0);
                        factors.add(factor);
                    }
                    RecommendationExplanation.SupportingFactor trend = new RecommendationExplanation.SupportingFactor();
                    trend.setType("TREND");
                    trend.setDescription("销量/热度加权推荐");
                    trend.setScore(Math.min(100.0, product.getSalesCount() * 1.0));
                    factors.add(trend);
                    explanation.setSupportingFactors(factors);
                    
                    explanations.add(explanation);
                    index++;
                }
            }

            // 构建响应
            RecommendationResponse response = new RecommendationResponse();
            response.setProducts(recommendations);
            response.setExplanations(explanations);
            response.setAlgorithmType(fallbackUsed ? "POPULAR" : algoType);
            
            RecommendationResponse.RecommendationStats stats = new RecommendationResponse.RecommendationStats();
            stats.setSimilarUserCount(similarUserCount);
            stats.setUserBehaviorCount(userBehaviorCount);
            stats.setExecutionTime(System.currentTimeMillis() - startTime);
            stats.setFallbackUsed(fallbackUsed);
            stats.setFallbackReason(fallbackReason);
            stats.setLastUpdateTime(System.currentTimeMillis());
            response.setStats(stats);

            LOGGER.info("为用户 {} 生成推荐解释成功，算法类型：{}，推荐数量：{}，执行时间：{}ms", 
                userId, response.getAlgorithmType(), recommendations.size(), 
                response.getStats().getExecutionTime());
            return ResultUtils.success(response);
        } catch (Exception e) {
            LOGGER.error("生成推荐解释异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "生成推荐解释失败：" + e.getMessage());
        }
    }

    /**
     * 解析前端传来的权重 JSON
     */
    private Map<String, Integer> parseWeights(String weightsJson) {
        if (weightsJson == null || weightsJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(weightsJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            LOGGER.warn("解析算法权重失败，使用默认值: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 构建热门趋势推荐
     */
    private RecommendationResponse buildTrendingResponse(double trendingWeight, long startTime) {
        List<Product> recommendations = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSalesCount)
                        .last("LIMIT 12")
        );
        List<RecommendationExplanation> explanations = new ArrayList<>();
        for (Product product : recommendations) {
            RecommendationExplanation exp = new RecommendationExplanation();
            exp.setProductId(product.getId());
            exp.setReasonType("TRENDING");
            exp.setPrimaryReason("热度/销量推荐");
            exp.setReasonDescription("该商品近期销量较高，热度上升");
            exp.setScore((product.getSalesCount() == null ? 0 : product.getSalesCount()) * trendingWeight);
            exp.setConfidence(0.6);
            Map<String, Double> scores = new HashMap<>();
            scores.put("trending", (double)(product.getSalesCount() == null ? 0 : product.getSalesCount()));
            exp.setAlgorithmScores(scores);
            explanations.add(exp);
        }
        RecommendationResponse response = new RecommendationResponse();
        response.setProducts(recommendations);
        response.setExplanations(explanations);
        response.setAlgorithmType("TRENDING");
        RecommendationResponse.RecommendationStats stats = new RecommendationResponse.RecommendationStats();
        stats.setExecutionTime(System.currentTimeMillis() - startTime);
        stats.setFallbackUsed(false);
        stats.setLastUpdateTime(System.currentTimeMillis());
        response.setStats(stats);
        return response;
    }

    /**
     * 计算趋势得分（按销量）
     */
    private double computeTrendScore(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) return 0.0;
        return p.getSalesCount() == null ? 0.0 : p.getSalesCount();
    }

    /**
     * 计算内容/上新得分（按更新时间，越新得分越高）
     */
    private double computeContentScore(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null || p.getUpdatedAt() == null) return 0.0;
        long now = System.currentTimeMillis();
        long diff = now - p.getUpdatedAt().getTime();
        // 30天半衰，越近越高
        double days = diff / (1000.0 * 60 * 60 * 24);
        double score = Math.max(0.0, 100.0 * Math.exp(-days / 30.0));
        return score;
    }

    /**
     * 构建内容相似/更新驱动的推荐（简化版，用最新商品近似）
     */
    private RecommendationResponse buildContentResponse(double contentWeight, long startTime) {
        List<Product> recommendations = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getUpdatedAt)
                        .last("LIMIT 12")
        );
        List<RecommendationExplanation> explanations = new ArrayList<>();
        for (Product product : recommendations) {
            RecommendationExplanation exp = new RecommendationExplanation();
            exp.setProductId(product.getId());
            exp.setReasonType("CONTENT");
            exp.setPrimaryReason("基于内容/上新推荐");
            exp.setReasonDescription("为您推荐近期更新/上新的相关内容");
            exp.setScore(contentWeight);
            exp.setConfidence(0.55);
            Map<String, Double> scores = new HashMap<>();
            scores.put("contentBased", contentWeight);
            exp.setAlgorithmScores(scores);
            explanations.add(exp);
        }
        RecommendationResponse response = new RecommendationResponse();
        response.setProducts(recommendations);
        response.setExplanations(explanations);
        response.setAlgorithmType("CONTENT");
        RecommendationResponse.RecommendationStats stats = new RecommendationResponse.RecommendationStats();
        stats.setExecutionTime(System.currentTimeMillis() - startTime);
        stats.setFallbackUsed(false);
        stats.setLastUpdateTime(System.currentTimeMillis());
        response.setStats(stats);
        return response;
    }

    /**
     * 构建探索/随机推荐
     */
    private RecommendationResponse buildSerendipityResponse(long startTime) {
        List<Product> recommendations = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .last("ORDER BY RAND() LIMIT 12")
        );
        List<RecommendationExplanation> explanations = new ArrayList<>();
        for (Product product : recommendations) {
            RecommendationExplanation exp = new RecommendationExplanation();
            exp.setProductId(product.getId());
            exp.setReasonType("SERENDIPITY");
            exp.setPrimaryReason("为您探索不同领域的惊喜");
            exp.setReasonDescription("随机探索，帮助您发现新领域的好书");
            exp.setScore(1.0);
            exp.setConfidence(0.5);
            explanations.add(exp);
        }
        RecommendationResponse response = new RecommendationResponse();
        response.setProducts(recommendations);
        response.setExplanations(explanations);
        response.setAlgorithmType("SERENDIPITY");
        RecommendationResponse.RecommendationStats stats = new RecommendationResponse.RecommendationStats();
        stats.setExecutionTime(System.currentTimeMillis() - startTime);
        stats.setFallbackUsed(false);
        stats.setLastUpdateTime(System.currentTimeMillis());
        response.setStats(stats);
        return response;
    }

    /**
     * 保存用户反馈
     */
    public Result<?> saveFeedback(Long userId, Long productId, String feedbackType, String remark) {
        try {
            String normalizedType = feedbackType == null ? "" : feedbackType.trim().toUpperCase();
            if (!ALLOWED_FEEDBACK_TYPES.contains(normalizedType)) {
                LOGGER.warn("保存反馈失败，不支持的反馈类型：{}", feedbackType);
                return ResultUtils.paramError("不支持的反馈类型，支持的类型：" + ALLOWED_FEEDBACK_TYPES);
            }
            
            RecommendationFeedback feedback = new RecommendationFeedback();
            feedback.setUserId(userId);
            feedback.setProductId(productId);
            feedback.setFeedbackType(normalizedType);
            feedback.setRemark(remark);
            feedback.setFeedbackTime(new java.sql.Timestamp(System.currentTimeMillis()));
            
            int result = feedbackMapper.insert(feedback);
            if (result > 0) {
                LOGGER.info("用户 {} 对商品 {} 的反馈已保存，反馈类型：{}", userId, productId, normalizedType);
                return ResultUtils.success();
            }
            LOGGER.warn("保存反馈失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "保存反馈失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("保存反馈异常，用户ID：{}，商品ID：{}，错误：{}", userId, productId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "保存反馈失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户反馈历史（最近 20 条）
     */
    public Result<?> getFeedbackHistory(Long userId) {
        try {
            LambdaQueryWrapper<RecommendationFeedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecommendationFeedback::getUserId, userId)
                   .orderByDesc(RecommendationFeedback::getFeedbackTime)
                   .last("LIMIT 20");
            List<RecommendationFeedback> list = feedbackMapper.selectList(wrapper);
            LOGGER.debug("获取用户反馈历史成功，用户ID：{}，反馈数量：{}", userId, list.size());
            return ResultUtils.success(list);
        } catch (Exception e) {
            LOGGER.error("获取反馈历史异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取反馈历史失败：" + e.getMessage());
        }
    }

    /**
     * 撤销一条反馈
     */
    public Result<?> undoFeedback(Long userId, Long feedbackId) {
        try {
            // 先检查反馈是否存在
            RecommendationFeedback feedback = feedbackMapper.selectById(feedbackId);
            if (feedback == null || !feedback.getUserId().equals(userId)) {
                LOGGER.warn("撤销反馈失败，反馈ID：{}，用户ID：{}，反馈不存在或不属于该用户", feedbackId, userId);
                return ResultUtils.dataNotFound("反馈记录不存在或已被删除");
            }
            
            LambdaQueryWrapper<RecommendationFeedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecommendationFeedback::getId, feedbackId)
                   .eq(RecommendationFeedback::getUserId, userId);
            int deleted = feedbackMapper.delete(wrapper);
            if (deleted > 0) {
                LOGGER.info("用户 {} 撤销反馈 {} 成功", userId, feedbackId);
                return ResultUtils.success();
            }
            LOGGER.warn("撤销反馈失败，反馈ID：{}，数据库删除返回 0", feedbackId);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "撤销反馈失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("撤销反馈异常，用户ID：{}，反馈ID：{}，错误：{}", userId, feedbackId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "撤销反馈失败：" + e.getMessage());
        }
    }
    
    /**
     * 商品推荐分数信息（内部类）
     * 支持多维得分，便于按权重混合
     */
    private static class ProductScoreInfo {
        double cfScore = 0.0;      // 协同过滤得分
        double contentScore = 0.0; // 内容/上新得分
        double trendScore = 0.0;   // 热度/销量得分

        double totalScore(double wCf, double wContent, double wTrend) {
            return cfScore * wCf + contentScore * wContent + trendScore * wTrend;
        }
    }
}
