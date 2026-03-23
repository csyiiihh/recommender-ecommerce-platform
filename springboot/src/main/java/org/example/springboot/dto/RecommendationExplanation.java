package org.example.springboot.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 推荐解释信息DTO
 * 在基础协同过滤解释上，增加多算法得分与支撑因素，便于前端可视化
 */
@Data
public class RecommendationExplanation {
    /**
     * 推荐商品ID
     */
    private Long productId;

    /**
     * 综合推荐分数（总分）
     */
    private Double score;

    /**
     * 推荐理由类型：COLLABORATIVE / POPULAR / SIMILAR_USER / HYBRID 等
     */
    private String reasonType;

    /**
     * 推荐理由描述
     */
    private String reasonDescription;

    /**
     * 相似用户数量（协同过滤场景）
     */
    private Integer similarUserCount;

    /**
     * 相似用户提示（脱敏展示）
     */
    private List<String> similarUserHints;

    /**
     * 来源商品ID列表（与用户历史行为相关）
     */
    private List<Long> sourceProductIds;

    /**
     * 算法置信度（0-1）
     */
    private Double confidence;

    /**
     * 推荐生成时间戳
     */
    private Long timestamp;

    /**
     * 主推荐理由摘要（用于前端优先展示）
     */
    private String primaryReason;

    /**
     * 多算法得分（用于雷达图/解释）
     * key 例如：collaborativeFiltering / contentBased / trending / knowledgeGraph / portrait
     */
    private Map<String, Double> algorithmScores;

    /**
     * 支撑因素列表（用于时间线/标签展示）
     */
    private List<SupportingFactor> supportingFactors;

    /**
     * 支撑因素（细粒度解释项）
     */
    @Data
    public static class SupportingFactor {
        /**
         * 类型：AUTHOR_MATCH / TREND / PRICE / INVENTORY / TAG_MATCH 等
         */
        private String type;
        /**
         * 描述：面向用户的可读文案
         */
        private String description;
        /**
         * 分值贡献（正向加分）
         */
        private Double score;
    }
}

















