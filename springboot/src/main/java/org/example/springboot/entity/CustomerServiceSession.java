package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 客服会话实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_service_session")
public class CustomerServiceSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 客服人员ID（管理员/商家）
     */
    private Long serviceId;
    
    /**
     * 会话状态：0-待接入，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 用户未读消息数
     */
    private Integer unreadUser;
    
    /**
     * 客服未读消息数
     */
    private Integer unreadService;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最后消息时间
     */
    private Timestamp lastMessageTime;
    
    /**
     * 创建时间
     */
    private Timestamp createdAt;
    
    /**
     * 更新时间
     */
    private Timestamp updatedAt;
    
    // 扩展字段，不对应数据库
    /**
     * 用户信息
     */
    @TableField(exist = false)
    private User user;
    
    /**
     * 客服信息
     */
    @TableField(exist = false)
    private User service;
}

