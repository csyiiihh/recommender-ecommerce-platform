package org.example.springboot.task;

import org.example.springboot.service.UnpaidOrderExpiryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "order.unpaid-expiry.enabled", havingValue = "true", matchIfMissing = true)
public class UnpaidOrderExpiryTask {
    private static final Logger log = LoggerFactory.getLogger(UnpaidOrderExpiryTask.class);
    private final UnpaidOrderExpiryService expiry;
    public UnpaidOrderExpiryTask(UnpaidOrderExpiryService expiry) { this.expiry = expiry; }
    @Scheduled(fixedDelayString = "${order.unpaid-expiry.delay-ms:60000}",
               initialDelayString = "${order.unpaid-expiry.delay-ms:60000}")
    public void closeExpiredOrders() {
        try {
            int count = expiry.closeExpiredOrders();
            if (count > 0) log.info("Closed {} expired unpaid ordinary orders", count);
        } catch (Exception e) {
            log.error("Failed to close expired unpaid orders; will retry on next scan", e);
        }
    }
}
