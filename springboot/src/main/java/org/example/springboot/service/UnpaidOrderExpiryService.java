package org.example.springboot.service;

import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Only ordinary, unpaid orders expire here. Group orders follow their team lifecycle. */
@Service
public class UnpaidOrderExpiryService {
    private final OrderMapper orders;
    private final int timeoutMinutes;
    private final InventoryReservationService inventory;
    public UnpaidOrderExpiryService(OrderMapper orders, InventoryReservationService inventory,
            @Value("${order.unpaid-timeout-minutes:30}") int timeoutMinutes) {
        if (timeoutMinutes < 1 || timeoutMinutes > 10080) {
            throw new IllegalArgumentException("order.unpaid-timeout-minutes must be between 1 and 10080");
        }
        this.orders = orders;
        this.inventory = inventory;
        this.timeoutMinutes = timeoutMinutes;
    }
    /** Payment entry and background cleanup use the same inclusive cutoff. */
    public boolean isPastPaymentDeadline(Order order, Instant now) {
        return Integer.valueOf(0).equals(order.getOrderType()) && order.getCreatedAt() != null
            && !order.getCreatedAt().after(cutoff(now));
    }
    private Timestamp cutoff(Instant now) {
        return Timestamp.from(now.minus(timeoutMinutes, ChronoUnit.MINUTES));
    }
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        Timestamp cutoff = cutoff(Instant.now());
        if (!inventory.isEnabled()) return orders.closeExpiredUnpaid(cutoff, 200);
        int closed = 0;
        for (Long id : orders.findExpiredUnpaidIds(cutoff)) {
            var order = orders.selectForUpdate(id);
            if (order == null || !Integer.valueOf(0).equals(order.getStatus()) || !Integer.valueOf(0).equals(order.getOrderType())
                    || order.getTradeNo() != null || order.getPayTime() != null || order.getCreatedAt() == null || order.getCreatedAt().after(cutoff)) continue;
            inventory.release(order);
            if (orders.closeOneExpiredUnpaid(id, cutoff) != 1) throw new org.example.springboot.exception.BusinessException("关闭超时订单失败");
            closed++;
        }
        return closed;
    }
}
