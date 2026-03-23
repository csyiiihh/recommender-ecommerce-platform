package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * 拼团实体类
 * 对应表：group_buy_team
 */
@Data
@TableName("group_buy_team")
public class GroupBuyTeam {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long activityId;
    
    private Long leaderUserId;
    
    private Long leaderOrderId;
    
    private Integer currentPeople;
    
    private Integer requiredPeople;
    
    /**
     * 状态：0-拼团中 1-拼团成功 2-拼团失败 3-已取消
     */
    private Integer status;
    
    /**
     * 失败原因：
     * timeout - 超时未成团
     * leader_refund - 团长退款
     * member_refund - 成员全部退款
     * cancelled - 主动取消
     */
    private String failReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp expireTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp successTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp updatedAt;
    
    /**
     * 关联活动信息（不映射到数据库）
     */
    @TableField(exist = false)
    private GroupBuyActivity activity;
    
    /**
     * 团长信息（不映射到数据库）
     */
    @TableField(exist = false)
    private User leader;
    
    /**
     * 成员列表（不映射到数据库）
     */
    @TableField(exist = false)
    private List<GroupBuyMember> members;
}

