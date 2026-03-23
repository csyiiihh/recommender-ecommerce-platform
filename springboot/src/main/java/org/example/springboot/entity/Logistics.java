package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class Logistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String companyName;
    private String trackingNumber;
    // 预计到达时间（使用字符串保存，格式建议为 yyyy-MM-dd HH:mm:ss）
    private String expectedArrivalTime;
    private Integer status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @TableField(exist = false)
    private Order order;
} 