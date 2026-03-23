package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 拼团活动实体类
 * 对应表：group_buy_activity
 */
@Data
@TableName("group_buy_activity")
public class GroupBuyActivity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private Long merchantId;
    
    private String activityName;
    
    private String activityDesc;
    
    private BigDecimal originalPrice;
    
    private BigDecimal groupPrice;
    
    private Integer requiredMembers;
    
    private Integer maxMembers;
    
    private Integer validityHours;
    
    private Integer stock;
    
    private Integer salesCount;
    
    /**
     * 状态：0-未开始 1-进行中 2-已结束 3-已下架
     */
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp endTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp updatedAt;
    
    /**
     * 关联商品信息（不映射到数据库）
     */
    @TableField(exist = false)
    private Product product;
    
    /**
     * 关联商家信息（不映射到数据库）
     */
    @TableField(exist = false)
    private User merchant;
}

