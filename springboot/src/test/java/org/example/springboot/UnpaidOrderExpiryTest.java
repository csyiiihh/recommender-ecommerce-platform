package org.example.springboot;

import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.service.UnpaidOrderExpiryService;
import org.example.springboot.task.UnpaidOrderExpiryTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnpaidOrderExpiryTest {
    @Test void cutoffUsesConfiguredWindowAndBoundedBatch() {
        OrderMapper orders = mock(OrderMapper.class);
        Instant before = Instant.now().minusSeconds(45 * 60L);
        when(orders.closeExpiredUnpaid(any(), eq(200))).thenReturn(3);
        assertEquals(3, new UnpaidOrderExpiryService(orders, mock(org.example.springboot.service.InventoryReservationService.class), 45).closeExpiredOrders());
        Instant after = Instant.now().minusSeconds(45 * 60L);
        var cutoff = ArgumentCaptor.forClass(Timestamp.class);
        verify(orders).closeExpiredUnpaid(cutoff.capture(), eq(200));
        assertFalse(cutoff.getValue().toInstant().isBefore(before.minusMillis(1)));
        assertFalse(cutoff.getValue().toInstant().isAfter(after));
        verifyNoMoreInteractions(orders);
    }
    @Test void databaseFailurePropagatesForTransactionRollback() {
        OrderMapper orders = mock(OrderMapper.class);
        when(orders.closeExpiredUnpaid(any(), anyInt())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> new UnpaidOrderExpiryService(orders, mock(org.example.springboot.service.InventoryReservationService.class), 30).closeExpiredOrders());
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 10081, Integer.MAX_VALUE})
    void invalidTimeoutFailsBeforeAnyDatabaseChange(int timeout) {
        OrderMapper orders = mock(OrderMapper.class);
        assertThrows(IllegalArgumentException.class, () -> new UnpaidOrderExpiryService(orders, mock(org.example.springboot.service.InventoryReservationService.class), timeout));
        verifyNoInteractions(orders);
    }
    @Test void schedulerCanContinueAfterFailedScan() {
        var expiry = mock(UnpaidOrderExpiryService.class);
        when(expiry.closeExpiredOrders()).thenThrow(new IllegalStateException("temporary failure")).thenReturn(1);
        var task = new UnpaidOrderExpiryTask(expiry);
        assertDoesNotThrow(task::closeExpiredOrders);
        assertDoesNotThrow(task::closeExpiredOrders);
        verify(expiry, times(2)).closeExpiredOrders();
    }
    @Test void emptyScanDoesNotRetryInALoop() {
        var expiry = mock(UnpaidOrderExpiryService.class);
        new UnpaidOrderExpiryTask(expiry).closeExpiredOrders();
        verify(expiry).closeExpiredOrders();
        verifyNoMoreInteractions(expiry);
    }
}
