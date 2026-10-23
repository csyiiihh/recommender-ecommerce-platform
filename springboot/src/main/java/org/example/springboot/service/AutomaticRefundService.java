package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.List;

/** Durable refund intent lives on the order; the payment request number is stable per order. */
@Service
public class AutomaticRefundService {
    public static final int AUTO_PENDING = 5;
    public static final String TEAM_FAILED = "拼团失败自动退款";
    public static final String UNFULFILLABLE = "支付后无法履约自动退款";
    private final OrderMapper orders;
    public AutomaticRefundService(OrderMapper orders) { this.orders = orders; }
    public void queueTeam(Long teamId) {
        List<Order> paid = orders.selectList(new LambdaQueryWrapper<Order>()
            .eq(Order::getGroupBuyTeamId, teamId).and(q -> q.eq(Order::getStatus, 1)
                .or(legacy -> legacy.eq(Order::getStatus, 5).eq(Order::getRefundStatus, 1).likeRight(Order::getRefundReason, TEAM_FAILED))));
        for (Order order : paid) {
            orders.update(null, new LambdaUpdateWrapper<Order>().eq(Order::getId, order.getId()).eq(Order::getStatus, order.getStatus())
                .set(Order::getLastStatus, 1).set(Order::getStatus, 5).set(Order::getRefundStatus, AUTO_PENDING)
                .set(Order::getRefundReason, TEAM_FAILED).set(Order::getUpdatedAt, new Timestamp(System.currentTimeMillis())));
        }
    }
    public static boolean isAutomatic(Order order) {
        return Integer.valueOf(AUTO_PENDING).equals(order.getRefundStatus()) && order.getRefundReason() != null && (order.getRefundReason().startsWith(TEAM_FAILED) || UNFULFILLABLE.equals(order.getRefundReason()));
    }
}
