package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockIntegrityTest {
    @Mock ProductMapper products;
    @Mock StockInMapper incoming;
    @Mock StockOutMapper outgoing;
    @Mock AccessService access;
    @InjectMocks StockService service;
    Product product(Integer stock) { Product p = new Product(); p.setId(2L); p.setStock(stock); when(products.selectForUpdate(2L)).thenReturn(p); return p; }
    StockIn in() { StockIn r = new StockIn(); r.setId(1L); r.setProductId(2L); r.setQuantity(2); r.setUnitPrice(BigDecimal.TEN); r.setStatus(1); return r; }
    StockOut out() { StockOut r = new StockOut(); r.setId(1L); r.setProductId(2L); r.setQuantity(2); r.setUnitPrice(BigDecimal.TEN); r.setStatus(1); return r; }
    void actor() { User u = new User(); u.setId(7L); when(access.current()).thenReturn(u); }
    void stockWriteSucceeds() { when(products.updateById(any(Product.class))).thenReturn(1); }
    @Test void failedStockInWriteCannotInsertLedgerRecord() {
        product(4); assertThrows(BusinessException.class, () -> service.createStockIn(in())); verifyNoInteractions(incoming, access);
    }
    @Test void failedStockOutWriteCannotInsertLedgerRecord() {
        product(4); assertThrows(BusinessException.class, () -> service.createStockOut(out())); verifyNoInteractions(outgoing, access);
    }
    @Test void stockInOverflowNeverWritesNegativeInventory() {
        Product p = product(Integer.MAX_VALUE); assertThrows(BusinessException.class, () -> service.createStockIn(in()));
        assertEquals(Integer.MAX_VALUE, p.getStock()); verify(products, never()).updateById(any(Product.class)); verifyNoInteractions(incoming);
    }
    @Test void largestRepresentableStockIsAllowedAndServerSetsOperatorAndTotal() {
        Product p = product(Integer.MAX_VALUE - 2); stockWriteSucceeds(); actor(); when(incoming.insert(any(StockIn.class))).thenReturn(1);
        StockIn r = in(); r.setOperatorId(999L); r.setTotalPrice(BigDecimal.ONE);
        assertEquals("0", service.createStockIn(r).getCode()); assertEquals(Integer.MAX_VALUE, p.getStock());
        assertEquals(7L, r.getOperatorId()); assertNull(r.getId()); assertEquals(new BigDecimal("20"), r.getTotalPrice());
    }
    @Test void normalStockOutUsesLockedBalanceAndAuthenticatedOperator() {
        Product p = product(4); stockWriteSucceeds(); actor(); when(outgoing.insert(any(StockOut.class))).thenReturn(1);
        StockOut r = out(); r.setOperatorId(999L); assertEquals("0", service.createStockOut(r).getCode());
        assertEquals(2, p.getStock()); assertEquals(7L, r.getOperatorId()); assertNull(r.getId()); assertEquals(new BigDecimal("20"),r.getTotalPrice());
        var sequence = inOrder(products, outgoing); sequence.verify(products).selectForUpdate(2L); sequence.verify(products).updateById(p); sequence.verify(outgoing).insert(r);
    }
    @Test void insufficientStockNeverWritesOrCreatesLedger() {
        Product p = product(1); assertNotEquals("0", service.createStockOut(out()).getCode()); assertEquals(1,p.getStock());
        verify(products,never()).updateById(any(Product.class)); verifyNoInteractions(outgoing);
    }
    @Test void nullOrNegativeBalanceCannotBeIncreased() {
        for (Integer stock : Arrays.asList(null,-1)) { product(stock); assertThrows(BusinessException.class,()->service.createStockIn(in())); }
        verify(products,never()).updateById(any(Product.class)); verifyNoInteractions(incoming);
    }
    @Test void nullOrNegativeBalanceCannotBeDecreased() {
        for (Integer stock : Arrays.asList(null,-1)) { product(stock); assertThrows(BusinessException.class,()->service.createStockOut(out())); }
        verify(products,never()).updateById(any(Product.class)); verifyNoInteractions(outgoing);
    }
    @Test void missingProductCannotVoidIncomingLedger() {
        StockIn r=in(); when(incoming.selectForUpdate(1L)).thenReturn(r); assertThrows(BusinessException.class,()->service.invalidateStockIn(1L));
        assertEquals(1,r.getStatus()); verify(incoming,never()).updateById(any(StockIn.class));
    }
    @Test void missingProductCannotVoidOutgoingLedger() {
        StockOut r=out(); when(outgoing.selectForUpdate(1L)).thenReturn(r); assertThrows(BusinessException.class,()->service.invalidateStockOut(1L));
        assertEquals(1,r.getStatus()); verify(outgoing,never()).updateById(any(StockOut.class));
    }
    @Test void stockWriteFailureKeepsIncomingLedgerActive() {
        product(4); StockIn r=in(); when(incoming.selectForUpdate(1L)).thenReturn(r); assertThrows(BusinessException.class,()->service.invalidateStockIn(1L));
        assertEquals(1,r.getStatus()); verify(incoming,never()).updateById(any(StockIn.class));
    }
    @Test void stockWriteFailureKeepsOutgoingLedgerActive() {
        product(4); StockOut r=out(); when(outgoing.selectForUpdate(1L)).thenReturn(r); assertThrows(BusinessException.class,()->service.invalidateStockOut(1L));
        assertEquals(1,r.getStatus()); verify(outgoing,never()).updateById(any(StockOut.class));
    }
    @Test void reversalCannotOverflowOutgoingStock() {
        Product p=product(Integer.MAX_VALUE); StockOut r=out(); when(outgoing.selectForUpdate(1L)).thenReturn(r);
        assertThrows(BusinessException.class,()->service.invalidateStockOut(1L)); assertEquals(Integer.MAX_VALUE,p.getStock()); assertEquals(1,r.getStatus());
        verify(products,never()).updateById(any(Product.class)); verify(outgoing,never()).updateById(any(StockOut.class));
    }
    @Test void incomingReversalNeedsEnoughAvailableStock() {
        Product p=product(1); StockIn r=in(); when(incoming.selectForUpdate(1L)).thenReturn(r); assertNotEquals("0",service.invalidateStockIn(1L).getCode());
        assertEquals(1,p.getStock()); assertEquals(1,r.getStatus()); verify(products,never()).updateById(any(Product.class)); verify(incoming,never()).updateById(any(StockIn.class));
    }
    @Test void validIncomingReversalUpdatesStockBeforeLedger() {
        Product p=product(4); stockWriteSucceeds(); StockIn r=in(); when(incoming.selectForUpdate(1L)).thenReturn(r); when(incoming.updateById(r)).thenReturn(1);
        assertEquals("0",service.invalidateStockIn(1L).getCode()); assertEquals(2,p.getStock()); assertEquals(0,r.getStatus());
        var sequence=inOrder(products,incoming); sequence.verify(incoming).selectForUpdate(1L); sequence.verify(products).selectForUpdate(2L); sequence.verify(products).updateById(p); sequence.verify(incoming).updateById(r);
    }
    @Test void validOutgoingReversalRestoresStockOnce() {
        Product p=product(4); stockWriteSucceeds(); StockOut r=out(); when(outgoing.selectForUpdate(1L)).thenReturn(r); when(outgoing.updateById(r)).thenReturn(1);
        assertEquals("0",service.invalidateStockOut(1L).getCode()); assertNotEquals("0",service.invalidateStockOut(1L).getCode());
        assertEquals(6,p.getStock()); verify(products,times(1)).updateById(p); verify(outgoing,times(1)).updateById(r);
    }
    @Test void alreadyVoidedIncomingRecordNeverTouchesStock() {
        StockIn r=in();r.setStatus(0);when(incoming.selectForUpdate(1L)).thenReturn(r);assertNotEquals("0",service.invalidateStockIn(1L).getCode());verifyNoInteractions(products);
    }
    @Test void corruptIncomingQuantityCannotReverseStock() {
        for(Integer q:Arrays.asList(null,0,-2)){StockIn r=in();r.setQuantity(q);when(incoming.selectForUpdate(1L)).thenReturn(r);assertThrows(BusinessException.class,()->service.invalidateStockIn(1L));}
        verifyNoInteractions(products);verify(incoming,never()).updateById(any(StockIn.class));
    }
    @Test void corruptOutgoingQuantityCannotReverseStock() {
        for(Integer q:Arrays.asList(null,0,-2)){StockOut r=out();r.setQuantity(q);when(outgoing.selectForUpdate(1L)).thenReturn(r);assertThrows(BusinessException.class,()->service.invalidateStockOut(1L));}
        verifyNoInteractions(products);verify(outgoing,never()).updateById(any(StockOut.class));
    }
    @Test void unknownLedgerStatesCannotBeReversed() {
        for(Integer status:Arrays.asList(null,2)){StockIn a=in();a.setStatus(status);StockOut b=out();b.setStatus(status);when(incoming.selectForUpdate(1L)).thenReturn(a);when(outgoing.selectForUpdate(1L)).thenReturn(b);assertThrows(BusinessException.class,()->service.invalidateStockIn(1L));assertThrows(BusinessException.class,()->service.invalidateStockOut(1L));}
        verifyNoInteractions(products);
    }
    @Test void failedIncomingReversalPreventsPhysicalDeletion() {
        product(4);when(incoming.selectForUpdate(1L)).thenReturn(in());assertThrows(BusinessException.class,()->service.deleteStockIn(1L));
        verify(incoming,never()).updateById(any(StockIn.class));verify(incoming,never()).deleteById(any(java.io.Serializable.class));
    }
    @Test void failedOutgoingReversalPreventsPhysicalDeletion() {
        product(4);when(outgoing.selectForUpdate(1L)).thenReturn(out());assertThrows(BusinessException.class,()->service.deleteStockOut(1L));
        verify(outgoing,never()).updateById(any(StockOut.class));verify(outgoing,never()).deleteById(any(java.io.Serializable.class));
    }
    @Test void unknownLedgerStatesCannotBeDeleted() {
        StockIn a=in();a.setStatus(null);StockOut b=out();b.setStatus(2);when(incoming.selectForUpdate(1L)).thenReturn(a);when(outgoing.selectForUpdate(1L)).thenReturn(b);
        assertThrows(BusinessException.class,()->service.deleteStockIn(1L));assertThrows(BusinessException.class,()->service.deleteStockOut(1L));
        verifyNoInteractions(products);verify(incoming,never()).deleteById(any(java.io.Serializable.class));verify(outgoing,never()).deleteById(any(java.io.Serializable.class));
    }
    @Test void voidedRecordsCanBeDeletedWithoutReversingStockAgain() {
        StockIn a=in();a.setStatus(0);StockOut b=out();b.setStatus(0);when(incoming.selectForUpdate(1L)).thenReturn(a);when(outgoing.selectForUpdate(1L)).thenReturn(b);
        when(incoming.deleteById(1L)).thenReturn(1);when(outgoing.deleteById(1L)).thenReturn(1);assertEquals("0",service.deleteStockIn(1L).getCode());assertEquals("0",service.deleteStockOut(1L).getCode());verifyNoInteractions(products);
    }
    @Test void ledgerInsertFailurePropagatesToTransactionRollback() {
        product(4);stockWriteSucceeds();actor();assertThrows(BusinessException.class,()->service.createStockIn(in()));verify(incoming).insert(any(StockIn.class));
    }
    @Test void ledgerReversalFailureCannotReturnSuccess() {
        product(4);stockWriteSucceeds();StockOut r=out();when(outgoing.selectForUpdate(1L)).thenReturn(r);assertThrows(BusinessException.class,()->service.invalidateStockOut(1L));verify(outgoing).updateById(r);
    }
}
