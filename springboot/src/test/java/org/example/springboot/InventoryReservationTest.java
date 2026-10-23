package org.example.springboot;

import org.example.springboot.entity.Order;
import org.example.springboot.entity.InventoryReservation;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryReservationTest {
    final InventoryReservationMapper rows = mock(InventoryReservationMapper.class);
    final ProductMapper products = mock(ProductMapper.class);
    final InventoryReservationService service = new InventoryReservationService(rows, products, true);
    Order order() { Order o = new Order(); o.setId(1L); o.setProductId(2L); o.setQuantity(3); o.setOrderType(0); o.setStatus(0); o.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(7200))); return o; }
    InventoryReservation row(int state) { var r = new InventoryReservation(); r.setOrderId(1L); r.setProductId(2L); r.setQuantity(3); r.setState(state); return r; }
    @Test void disabledFeatureWorksWithoutMigration() {
        when(rows.tableExists()).thenReturn(0);
        var off = new InventoryReservationService(rows, products, false); assertDoesNotThrow(off::validateSchemaAndSwitch);
        off.reserve(order()); off.release(order()); assertFalse(off.consume(order())); assertFalse(off.hasHeld(order()));
        verify(rows).tableExists(); verifyNoMoreInteractions(rows); verifyNoInteractions(products);
    }
    @Test void cannotEnableBeforeMigration() { assertThrows(IllegalStateException.class, service::validateSchemaAndSwitch); }
    @Test void cannotDisableWithHeldStock() {
        when(rows.tableExists()).thenReturn(1); when(rows.countHeld()).thenReturn(1L);
        assertThrows(IllegalStateException.class, new InventoryReservationService(rows, products, false)::validateSchemaAndSwitch);
    }
    @Test void enabledFeatureAcceptsInstalledSchema() { when(rows.tableExists()).thenReturn(1); assertDoesNotThrow(service::validateSchemaAndSwitch); }
    @Test void canDisableAfterAllReservationsSettle() { when(rows.tableExists()).thenReturn(1); assertDoesNotThrow(new InventoryReservationService(rows, products, false)::validateSchemaAndSwitch); }
    @Test void reserveDecreasesAvailabilityWithoutRecordingSales() {
        when(products.reserveStock(2L, 3)).thenReturn(1); when(rows.insert(1L, 2L, 3)).thenReturn(1);
        service.reserve(order()); verify(products).reserveStock(2L, 3); verifyNoMoreInteractions(products);
    }
    @Test void repeatedReservationDoesNotDeductAgain() {
        when(rows.find(1L)).thenReturn(row(1)); service.reserve(order()); verifyNoInteractions(products); verify(rows, never()).insert(anyLong(), anyLong(), anyInt());
    }
    @Test void insufficientAvailabilityCannotCreateHold() { assertThrows(BusinessException.class, () -> service.reserve(order())); verify(rows, never()).insert(anyLong(), anyLong(), anyInt()); }
    @Test void failedLedgerInsertThrowsForOuterTransactionRollback() {
        when(products.reserveStock(2L, 3)).thenReturn(1); assertThrows(BusinessException.class, () -> service.reserve(order()));
    }
    @Test void invalidQuantityCannotTouchStock() { Order o = order(); o.setQuantity(-1); assertThrows(BusinessException.class, () -> service.reserve(o)); verifyNoInteractions(rows, products); }
    @Test void inconsistentReservationCannotTouchStock() { var r = row(1); r.setQuantity(4); when(rows.find(1L)).thenReturn(r); assertThrows(BusinessException.class, () -> service.release(order())); verifyNoInteractions(products); }
    @Test void settledReservationCannotBeReservedAgain() { when(rows.find(1L)).thenReturn(row(3)); assertThrows(BusinessException.class, () -> service.reserve(order())); verifyNoInteractions(products); }
    @Test void consumptionRecordsSalesExactlyOnceWithoutAnotherStockDeduction() {
        var r = row(1); when(rows.find(1L)).thenReturn(r); when(rows.transition(1L, 2)).thenAnswer(i -> { r.setState(2); return 1; }); when(products.recordReservedSale(2L, 3)).thenReturn(1);
        assertTrue(service.consume(order())); assertTrue(service.consume(order())); verify(products).recordReservedSale(2L, 3); verifyNoMoreInteractions(products);
    }
    @Test void repeatedReleaseRestoresAvailabilityExactlyOnce() {
        var r = row(1); when(rows.find(1L)).thenReturn(r); when(rows.transition(1L, 3)).thenAnswer(i -> { r.setState(3); return 1; }); when(products.releaseReservedStock(2L, 3)).thenReturn(1);
        service.release(order()); service.release(order()); verify(products).releaseReservedStock(2L, 3); verifyNoMoreInteractions(products);
    }
    @Test void consumedHoldCannotBeReleasedAsUnpaidStock() { when(rows.find(1L)).thenReturn(row(2)); service.release(order()); verifyNoInteractions(products); }
    @Test void releasedHoldCannotBeConsumed() { when(rows.find(1L)).thenReturn(row(3)); assertThrows(BusinessException.class, () -> service.consume(order())); verifyNoInteractions(products); }
    @Test void legacyOrderWithoutLedgerUsesExistingPaymentPath() { assertFalse(service.consume(order())); service.release(order()); assertFalse(service.hasHeld(order())); verifyNoInteractions(products); }
    @Test void groupOrdersKeepExistingLifecycle() { Order o = order(); o.setOrderType(1); service.reserve(o); service.release(o); assertFalse(service.consume(o)); assertFalse(service.hasHeld(o)); verifyNoInteractions(rows, products); }
    @Test void failedTransitionCannotRestoreStock() { when(rows.find(1L)).thenReturn(row(1)); assertThrows(BusinessException.class, () -> service.release(order())); verifyNoInteractions(products); }
    @Test void failedStockRestorationThrowsForOuterTransactionRollback() { when(rows.find(1L)).thenReturn(row(1)); when(rows.transition(1L, 3)).thenReturn(1); assertThrows(BusinessException.class, () -> service.release(order())); }
    @Test void expiryReleasesBeforeClosingUnderOrderLock() {
        OrderMapper orders = mock(OrderMapper.class); InventoryReservationService inventory = mock(InventoryReservationService.class); Order o = order();
        when(inventory.isEnabled()).thenReturn(true); when(orders.findExpiredUnpaidIds(any())).thenReturn(List.of(1L)); when(orders.selectForUpdate(1L)).thenReturn(o); when(orders.closeOneExpiredUnpaid(eq(1L), any())).thenReturn(1);
        assertEquals(1, new UnpaidOrderExpiryService(orders, inventory, 30).closeExpiredOrders());
        var seq = inOrder(orders, inventory); seq.verify(inventory).isEnabled(); seq.verify(orders).findExpiredUnpaidIds(any()); seq.verify(orders).selectForUpdate(1L); seq.verify(inventory).release(o); seq.verify(orders).closeOneExpiredUnpaid(eq(1L), any());
        verify(orders, never()).closeExpiredUnpaid(any(), anyInt());
    }
    @Test void expirySkipsCandidatePaidBeforeLockAcquired() {
        OrderMapper orders = mock(OrderMapper.class); InventoryReservationService inventory = mock(InventoryReservationService.class); Order o = order(); o.setStatus(1);
        when(inventory.isEnabled()).thenReturn(true); when(orders.findExpiredUnpaidIds(any())).thenReturn(List.of(1L)); when(orders.selectForUpdate(1L)).thenReturn(o);
        assertEquals(0, new UnpaidOrderExpiryService(orders, inventory, 30).closeExpiredOrders()); verify(inventory, never()).release(any()); verify(orders, never()).closeOneExpiredUnpaid(anyLong(), any());
    }
    @Test void failedExpiryClosureThrowsForReleaseRollback() {
        OrderMapper orders = mock(OrderMapper.class); InventoryReservationService inventory = mock(InventoryReservationService.class);
        when(inventory.isEnabled()).thenReturn(true); when(orders.findExpiredUnpaidIds(any())).thenReturn(List.of(1L)); when(orders.selectForUpdate(1L)).thenReturn(order());
        assertThrows(BusinessException.class, () -> new UnpaidOrderExpiryService(orders, inventory, 30).closeExpiredOrders());
    }
}
