package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 拼团成员实体类
 * 对应表：group_buy_member
 */
@Data
@TableName("group_buy_member")
public class GroupBuyMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    
    private Long userId;
    
    private Long orderId;
    
    /**
     * 是否团长：0-否 1-是
     */
    private Integer isLeader;
    
    /**
     * 支付状态：0-待支付 1-已支付 2-已退款
     */
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp joinTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp payTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp updatedAt;
    
    /**
     * 用户信息（不映射到数据库）
     */
    @TableField(exist = false)
    private User user;
    
    /**
     * 订单信息（不映射到数据库）
     */
    @TableField(exist = false)
    private Order order;
}

