package org.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutIdempotencyTest {
    static final String KEY = "checkout-test-request-0001";
    @Mock UserMapper users; @Mock OrderMapper orders; @Mock ProductMapper products; @Mock CartMapper carts;
    @Mock InventoryReservationService inventory; @Mock AccessService access; @Mock CheckoutIdempotencyService requests;
    @InjectMocks OrderService checkout;
    CheckoutIdempotencyService worker() { return new CheckoutIdempotencyService(users, orders, new ObjectMapper()); }
    User buyer(long id) { User u = new User(); u.setId(id); u.setStatus(1); return u; }
    Order existing(String tradeNo) { Order o = new Order(); o.setId(10L); o.setUserId(1L); o.setOrderType(0); o.setStatus(0); o.setQuantity(2); o.setProductId(2L); o.setTotalPrice(new BigDecimal("50")); o.setOutTradeNo(tradeNo); return o; }
    Order input() { Order o = new Order(); o.setUserId(1L); o.setProductId(2L); o.setQuantity(2); return o; }
    void enabledBuyer() { when(users.selectForUpdate(1L)).thenReturn(buyer(1)); }
    @Test void sameKeyAndPayloadProduceStableSixtyFourCharacterTradeNumber() {
        enabledBuyer(); var worker = worker(); var one = worker.prepare(1L, "single", KEY, Arrays.asList(2L, 2, "收货地址", null)); var two = worker.prepare(1L, "single", KEY, Arrays.asList(2L, 2, "收货地址", null));
        assertEquals(one.tradeNo(), two.tradeNo()); assertTrue(one.tradeNo().matches("[a-f0-9]{64}"));
        var sequence = inOrder(users, orders); sequence.verify(users).selectForUpdate(1L); sequence.verify(orders).selectCheckoutByPrefix(1L, one.tradeNo().substring(0, 32));
    }
    @Test void keysAreScopedToBuyerAndCheckoutKind() {
        enabledBuyer(); when(users.selectForUpdate(2L)).thenReturn(buyer(2)); var w = worker();
        String first = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); assertNotEquals(first, w.prepare(2L, "single", KEY, List.of(2)).tradeNo()); assertNotEquals(first, w.prepare(1L, "cart", KEY, List.of(2)).tradeNo());
    }
    @Test void changedBodyKeepsLookupPrefixButChangesFingerprint() {
        enabledBuyer(); var w = worker(); String first = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); String second = w.prepare(1L, "single", KEY, List.of(3)).tradeNo(); assertEquals(first.substring(0, 32), second.substring(0, 32)); assertNotEquals(first, second);
    }
    @Test void invalidKeysNeverAcquireDatabaseLocks() {
        for (String key : Arrays.asList(null, "", "short", "abcdefghijklmnop!", "a".repeat(129))) assertEquals(400, assertThrows(ResponseStatusException.class, () -> worker().prepare(1L, "single", key, List.of())).getStatusCode().value());
        verifyNoInteractions(users, orders);
    }
    @Test void invalidBuyerNeverQueriesDatabase() { assertEquals(400, assertThrows(ResponseStatusException.class, () -> worker().prepare(null, "single", KEY, List.of())).getStatusCode().value()); verifyNoInteractions(users, orders); }
    @Test void missingBuyerIsRejectedBeforeOrderLookup() { assertEquals(403, assertThrows(ResponseStatusException.class, () -> worker().prepare(1L, "single", KEY, List.of())).getStatusCode().value()); verifyNoInteractions(orders); }
    @Test void disabledBuyerCannotReplayOrCreateOrders() { User u = buyer(1); u.setStatus(0); when(users.selectForUpdate(1L)).thenReturn(u); assertEquals(403, assertThrows(ResponseStatusException.class, () -> worker().prepare(1L, "single", KEY, List.of())).getStatusCode().value()); verifyNoInteractions(orders); }
    @Test void sameKeyReplaysOriginalEvenAfterItsStateChanges() {
        enabledBuyer(); var w = worker(); String trade = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); Order saved = existing(trade); saved.setStatus(4); when(orders.selectCheckoutByPrefix(1L, trade.substring(0, 32))).thenReturn(List.of(saved)); assertSame(saved, w.prepare(1L, "single", KEY, List.of(2)).existing().get(0));
    }
    @Test void sameKeyWithChangedContentIsHttpConflict() {
        enabledBuyer(); var w = worker(); String trade = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); when(orders.selectCheckoutByPrefix(1L, trade.substring(0, 32))).thenReturn(List.of(existing(trade))); assertEquals(409, assertThrows(ResponseStatusException.class, () -> w.prepare(1L, "single", KEY, List.of(3))).getStatusCode().value());
    }
    @Test void wrongOwnerOrGroupRowCannotBeReturnedAsCheckoutReplay() {
        enabledBuyer(); var w = worker(); String trade = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); Order wrong = existing(trade); wrong.setUserId(2L); when(orders.selectCheckoutByPrefix(1L, trade.substring(0, 32))).thenReturn(List.of(wrong)); assertEquals(409, assertThrows(ResponseStatusException.class, () -> w.prepare(1L, "single", KEY, List.of(2))).getStatusCode().value()); wrong.setUserId(1L); wrong.setOrderType(1); assertThrows(ResponseStatusException.class, () -> w.prepare(1L, "single", KEY, List.of(2)));
    }
    @Test void multipleRowsAreOnlyValidForCartScope() {
        enabledBuyer(); var w = worker(); String trade = w.prepare(1L, "single", KEY, List.of(2)).tradeNo(); when(orders.selectCheckoutByPrefix(1L, trade.substring(0, 32))).thenReturn(List.of(existing(trade), existing(trade))); assertThrows(ResponseStatusException.class, () -> w.prepare(1L, "single", KEY, List.of(2)));
    }
    @Test void repeatedSingleCreationNeverReadsProductsOrReservesAgain() {
        when(access.current()).thenReturn(buyer(1)); Order original = existing("a".repeat(64)); when(requests.prepare(eq(1L), eq("single"), eq(KEY), anyList())).thenReturn(new CheckoutIdempotencyService.Token(original.getOutTradeNo(), List.of(original)));
        assertSame(original, checkout.createOrder(input(), KEY).getData()); verifyNoInteractions(products, inventory, orders, carts);
    }
    @Test void newSingleCreationUsesPreparedTradeNumber() {
        when(access.current()).thenReturn(buyer(1)); String trade = "a".repeat(64); when(requests.prepare(eq(1L), eq("single"), eq(KEY), anyList())).thenReturn(new CheckoutIdempotencyService.Token(trade, List.of()));
        Product p = new Product(); p.setId(2L); p.setStatus(1); p.setStock(4); p.setPrice(new BigDecimal("25")); when(products.selectById(2L)).thenReturn(p); when(orders.insert(any(Order.class))).thenReturn(1);
        Order saved = (Order) checkout.createOrder(input(), KEY).getData(); assertEquals(trade, saved.getOutTradeNo()); verify(inventory).reserve(saved);
        var sequence = inOrder(requests, inventory, products); sequence.verify(requests).prepare(eq(1L), eq("single"), eq(KEY), anyList()); sequence.verify(inventory).lockBuyer(1L); sequence.verify(products).selectById(2L);
    }
    @Test void cartReplayWorksAfterCartRowsHaveDisappeared() {
        Order original = existing("b".repeat(64)); when(requests.prepare(eq(1L), eq("cart"), eq(KEY), anyList())).thenReturn(new CheckoutIdempotencyService.Token(original.getOutTradeNo(), List.of(original)));
        @SuppressWarnings("unchecked") Map<String,Object> data = (Map<String,Object>) checkout.createOrdersFromCart(Map.of("cartIds", List.of(4), "userId", 1), KEY).getData();
        assertEquals(new BigDecimal("50"), data.get("totalAmount")); assertEquals(1, data.get("orderCount")); assertEquals(true, data.get("replayed")); assertEquals(true, data.get("paymentAllowed")); verifyNoInteractions(carts, products, orders, inventory);
    }
    @Test void processedCartReplayDoesNotInviteAnotherPayment() {
        Order original = existing("b".repeat(64)); original.setStatus(1); when(requests.prepare(eq(1L), eq("cart"), eq(KEY), anyList())).thenReturn(new CheckoutIdempotencyService.Token(original.getOutTradeNo(), List.of(original)));
        @SuppressWarnings("unchecked") Map<String,Object> data = (Map<String,Object>) checkout.createOrdersFromCart(Map.of("cartIds", List.of(4), "userId", 1), KEY).getData(); assertEquals(false, data.get("paymentAllowed")); verifyNoInteractions(carts, products, inventory);
    }
    @Test void reorderedAndDuplicateCartIdsShareCanonicalRequestBody() {
        Order original = existing("b".repeat(64)); when(requests.prepare(eq(1L), eq("cart"), eq(KEY), anyList())).thenReturn(new CheckoutIdempotencyService.Token(original.getOutTradeNo(), List.of(original)));
        checkout.createOrdersFromCart(Map.of("cartIds", List.of(2, 1.0, 1), "userId", 1), KEY);
        @SuppressWarnings("unchecked") ArgumentCaptor<List<?>> payload = ArgumentCaptor.forClass(List.class); verify(requests).prepare(eq(1L), eq("cart"), eq(KEY), payload.capture()); assertEquals(List.of(1L, 2L), payload.getValue().get(0));
    }
    @Test void fractionalOrNonPositiveCartIdsCannotCreateOrReplayOrders() {
        for (Number value : List.of(1.5, 0, -1, new BigDecimal("9223372036854775808"))) assertThrows(BusinessException.class, () -> checkout.createOrdersFromCart(Map.of("cartIds", List.of(value), "userId", 1), KEY)); verifyNoInteractions(requests, orders, products, carts, inventory);
    }
    @Test void cartRequestConflictPreservesHttpStatus() {
        when(requests.prepare(eq(1L), eq("cart"), eq(KEY), anyList())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "changed")); assertEquals(409, assertThrows(ResponseStatusException.class, () -> checkout.createOrdersFromCart(Map.of("cartIds", List.of(1), "userId", 1), KEY)).getStatusCode().value()); verifyNoInteractions(products, carts, inventory);
    }
}
