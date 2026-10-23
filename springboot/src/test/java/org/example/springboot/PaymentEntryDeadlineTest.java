package org.example.springboot;

import org.example.springboot.entity.Order;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEntryDeadlineTest {
    @Mock OrderMapper orders;
    @Mock InventoryReservationService inventory;
    @Mock AccessService access;
    @Mock AlipayService alipay;
    @InjectMocks PaymentService payments;
    UnpaidOrderExpiryService expiry;
    static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @BeforeEach void setup() {
        expiry = new UnpaidOrderExpiryService(orders, inventory, 30);
        ReflectionTestUtils.setField(payments, "expiry", expiry);
    }
    Order order(long id, Instant created) {
        Order o = new Order(); o.setId(id); o.setUserId(42L); o.setOrderType(0); o.setStatus(0);
        o.setQuantity(1); o.setTotalPrice(new BigDecimal("39.00"));
        if (created != null) o.setCreatedAt(Timestamp.from(created));
        return o;
    }
    void batch(Order... rows) { when(orders.selectTradeForUpdate("batch")).thenReturn(List.of(rows)); }
    void assertRejected() {
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> payments.create("batch")).getStatusCode().value());
        verifyNoInteractions(alipay, inventory);
        verify(orders, never()).updateById(any(Order.class));
    }
    @Test void deadlineIsInclusiveToMatchCleanup() {
        assertTrue(expiry.isPastPaymentDeadline(order(1, NOW.minusSeconds(1800)), NOW));
        assertTrue(expiry.isPastPaymentDeadline(order(1, NOW.minusSeconds(1801)), NOW));
    }
    @Test void oneMillisecondBeforeDeadlineIsStillEligible() {
        assertFalse(expiry.isPastPaymentDeadline(order(1, NOW.minusSeconds(1800).plusMillis(1)), NOW));
    }
    @Test void customTimeoutIsUsedByPaymentEntry() {
        var shortWindow = new UnpaidOrderExpiryService(orders, inventory, 1);
        assertTrue(shortWindow.isPastPaymentDeadline(order(1, NOW.minusSeconds(60)), NOW));
        assertFalse(shortWindow.isPastPaymentDeadline(order(1, NOW.minusSeconds(59)), NOW));
    }
    @Test void groupOrdersKeepTheirOwnLifecycle() {
        Order group = order(1, NOW.minusSeconds(86400)); group.setOrderType(1);
        assertFalse(expiry.isPastPaymentDeadline(group, NOW));
    }
    @Test void missingTimestampKeepsLegacyCompatibility() {
        assertFalse(expiry.isPastPaymentDeadline(order(1, null), NOW));
    }
    @Test void expiredUnscannedOrderCannotReachPaymentGateway() {
        Order old = order(1, Instant.now().minusSeconds(1801)); batch(old); assertRejected();
        assertEquals(0, old.getStatus());
    }
    @Test void oneExpiredRowRejectsEntireCartBatch() {
        batch(order(1, Instant.now()), order(2, Instant.now().minusSeconds(1801))); assertRejected();
    }
    @Test void freshOrderCanRequestPaymentWithDatabaseAmount() {
        batch(order(1, Instant.now())); payments.create("batch");
        verify(alipay).create("batch", "商品订单", "39.00");
    }
    @Test void legacyOrderWithoutTimestampCanStillPay() {
        batch(order(1, null)); payments.create("batch"); verify(alipay).create("batch", "商品订单", "39.00");
    }
    @Test void paymentReferenceOnUnpaidRowBlocksAnotherCollection() {
        Order row = order(1, Instant.now()); row.setTradeNo("paid-reference"); batch(row); assertRejected();
    }
    @Test void paymentTimestampOnUnpaidRowBlocksAnotherCollection() {
        Order row = order(1, Instant.now()); row.setPayTime(Timestamp.from(Instant.now())); batch(row); assertRejected();
    }
    @Test void ownershipIsCheckedBeforeDeadlineAndGateway() {
        batch(order(1, Instant.now().minusSeconds(1801)));
        doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(access).owner(42L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> payments.create("batch"));
        verifyNoInteractions(alipay, inventory);
    }
}
