package org.example.springboot.dto;

import lombok.Data;
import org.example.springboot.entity.Product;
import java.util.List;

/**
 * 推荐响应DTO
 * 包含推荐商品和解释信息
 */
@Data
public class RecommendationResponse {
    /**
     * 推荐商品列表
     */
    private List<Product> products;
    
    /**
     * 推荐解释信息列表（与products一一对应）
     */
    private List<RecommendationExplanation> explanations;
    
    /**
     * 推荐算法类型
     */
    private String algorithmType;
    
    /**
     * 推荐统计信息
     */
    private RecommendationStats stats;
    
    /**
     * 推荐统计信息
     */
    @Data
    public static class RecommendationStats {
        /**
         * 找到的相似用户数量
         */
        private Integer similarUserCount;
        
        /**
         * 用户历史行为数量（订单+收藏）
         */
        private Integer userBehaviorCount;
        
        /**
         * 推荐算法执行时间（毫秒）
         */
        private Long executionTime;
        
        /**
         * 是否使用了降级策略
         */
        private Boolean fallbackUsed;

        /**
         * 触发降级为热门推荐的原因说明（例如：用户行为不足 / 相似用户不足）
         */
        private String fallbackReason;

        /**
         * 推荐结果最近一次生成时间（时间戳毫秒）
         */
        private Long lastUpdateTime;
    }
}










