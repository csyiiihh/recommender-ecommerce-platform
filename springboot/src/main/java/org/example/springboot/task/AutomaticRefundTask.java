package org.example.springboot.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.service.AutomaticRefundService;
import org.example.springboot.service.OrderService;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Timestamp;

@Component
public class AutomaticRefundTask {
    private static final Logger LOG = LoggerFactory.getLogger(AutomaticRefundTask.class);
    private final OrderMapper orders;
    private final OrderService service;
    public AutomaticRefundTask(OrderMapper orders, OrderService service) { this.orders = orders; this.service = service; }
    @Scheduled(fixedDelayString = "${refund.retry-delay-ms:60000}")
    public void retryPendingRefunds() {
        var pending = orders.selectList(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 5).eq(Order::getRefundStatus, AutomaticRefundService.AUTO_PENDING)
            .and(q -> q.likeRight(Order::getRefundReason, AutomaticRefundService.TEAM_FAILED)
                .or().eq(Order::getRefundReason, AutomaticRefundService.UNFULFILLABLE))
            .and(q -> q.isNull(Order::getUpdatedAt).or().lt(Order::getUpdatedAt, new Timestamp(System.currentTimeMillis() - 300_000)))
            .orderByAsc(Order::getUpdatedAt).orderByAsc(Order::getId).last("LIMIT 100"));
        for (Order order : pending) {
            try {
                service.handleRefund(order.getId(), 6, "系统自动退款");
            } catch (Exception e) {
                // The failed refund transaction has rolled back; delay this order without losing its intent.
                orders.update(null, new LambdaUpdateWrapper<Order>().eq(Order::getId, order.getId()).eq(Order::getStatus, 5)
                    .set(Order::getUpdatedAt, new Timestamp(System.currentTimeMillis())));
                LOG.error("自动退款待重试，订单ID={}，请在退款管理中核对", order.getId(), e);
            }
        }
    }
}
