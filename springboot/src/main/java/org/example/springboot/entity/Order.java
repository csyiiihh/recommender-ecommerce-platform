package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal totalPrice;
    private Integer status;
    private Integer lastStatus;
    private String remark;
    private Timestamp refundTime;
    private Integer refundStatus;
    private String refundReason;
    private String recvAddress;
    private String recvPhone;
    private String recvName;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // 支付相关字段
    private String payMethod;       // 支付方式:alipay-支付宝,wechat-微信,cash-现金
    private String tradeNo;         // 支付流水号(支付宝交易号)
    private String outTradeNo;      // 商户订单号
    private Timestamp payTime;      // 支付时间
    private BigDecimal payAmount;   // 实际支付金额
    
    // 拼团相关字段
    private Integer orderType;      // 订单类型：0-普通订单 1-拼团订单
    private Long groupBuyTeamId;    // 拼团ID（拼团订单专用）
    private Long activityId;        // 拼团活动ID（拼团订单专用）

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Product product;

    @TableField(exist = false)
    private User merchant;

} 