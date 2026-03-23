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
 * 客服消息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_service_message")
public class CustomerServiceMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者类型：USER-用户，SERVICE-客服
     */
    private String senderType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：TEXT-文本，IMAGE-图片
     */
    private String messageType;
    
    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;
    
    /**
     * 发送时间
     */
    private Timestamp createdAt;
    
    // 扩展字段，不对应数据库
    /**
     * 发送者信息
     */
    @TableField(exist = false)
    private User sender;
}

