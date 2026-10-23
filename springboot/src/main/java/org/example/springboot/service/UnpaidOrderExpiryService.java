package org.example.springboot.service;

import org.example.springboot.mapper.OrderMapper;
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
    public UnpaidOrderExpiryService(OrderMapper orders,
            @Value("${order.unpaid-timeout-minutes:30}") int timeoutMinutes) {
        if (timeoutMinutes < 1 || timeoutMinutes > 10080) {
            throw new IllegalArgumentException("order.unpaid-timeout-minutes must be between 1 and 10080");
        }
        this.orders = orders;
        this.timeoutMinutes = timeoutMinutes;
    }
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES));
        // No stock is reserved before payment, so expiry must not replenish inventory.
        return orders.closeExpiredUnpaid(cutoff, 200);
    }
}
