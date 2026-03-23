package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.sql.Timestamp;

/**
 * 推荐反馈实体
 * 用于记录用户对推荐结果的反馈
 */
@Data
@TableName("recommendation_feedback")
public class RecommendationFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 推荐的商品ID
     */
    private Long productId;
    
    /**
     * 反馈类型（统一大写）：
     * LIKE, DISLIKE, ALREADY_OWN, NOT_INTERESTED, TOO_EXPENSIVE, WILL_BUY_LATER
     */
    private String feedbackType;
    
    /**
     * 反馈时间
     */
    private Timestamp feedbackTime;
    
    /**
     * 备注
     */
    private String remark;
}

