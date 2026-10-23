package org.example.springboot;

import com.alipay.api.internal.util.AlipaySignature;
import org.example.springboot.config.AlipayConfig;
import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.security.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRegressionTest {
    @Mock OrderMapper orders;
    @Mock ProductMapper products;
    @Mock CartMapper carts;
    @Mock AccessService access;
    @Mock AlipayService alipay;
    @Mock AlipayConfig config;
    @Mock GroupBuyService groups;
    @org.mockito.Mock org.example.springboot.service.InventoryReservationService inventory;
    @InjectMocks PaymentService payments;
    static KeyPair keys;
    Order order;
    Product product;
    @BeforeAll static void keys() throws Exception { KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); keys = generator.generateKeyPair(); }
    @BeforeEach void setup() {
        lenient().when(config.getPublicKey()).thenReturn(Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        lenient().when(config.getAppId()).thenReturn("test-app");
        lenient().when(config.getSellerId()).thenReturn("test-seller");
        lenient().when(config.getGateway()).thenReturn("https://sandbox.example.com");
        order = new Order(); order.setId(1L); order.setUserId(1L); order.setProductId(2L); order.setStatus(0); order.setOrderType(0); order.setQuantity(2); order.setTotalPrice(new BigDecimal("50.00"));
        product = new Product(); product.setId(2L); product.setStock(5); product.setStatus(1);
    }
    Map<String,String> signed(String amount) throws Exception {
        Map<String,String> params = new HashMap<>();
        params.put("app_id", "test-app"); params.put("seller_id", "test-seller"); params.put("out_trade_no", "trade-1");
        params.put("trade_no", "alipay-1"); params.put("total_amount", amount); params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", AlipaySignature.rsaSign(AlipaySignature.getSignContent(params), Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded()), "UTF-8", "RSA2"));
        params.put("sign_type", "RSA2"); return params;
    }
    @Test void unsignedNotificationCannotUpdateOrders() {
        assertThrows(Exception.class, () -> payments.notifyPaid(Map.of("trade_status", "TRADE_SUCCESS")));
        verifyNoInteractions(orders, products);
    }
    @Test void incorrectSignedAmountIsRejected() throws Exception {
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        var params = signed("0.01"); assertThrows(BusinessException.class, () -> payments.notifyPaid(params));
        verify(orders, never()).updateById(any(Order.class)); verifyNoInteractions(products);
    }
    @Test void tamperedSignedNotificationIsRejected() throws Exception {
        var params = signed("50.00"); params.put("total_amount", "0.01");
        assertThrows(BusinessException.class, () -> payments.notifyPaid(params)); verifyNoInteractions(orders);
    }
    @Test void signedNotificationUpdatesOnce() throws Exception {
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        when(products.selectForUpdate(2L)).thenReturn(product);
        when(products.deductStock(2L, 2)).thenReturn(1); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("50.00")); payments.notifyPaid(signed("50.00"));
        assertEquals(1, order.getStatus()); assertEquals(new BigDecimal("50.00"), order.getPayAmount());
        verify(products, times(1)).deductStock(2L,2); verify(orders, times(1)).updateById(any(Order.class));
    }
    @Test void insufficientStockQueuesDurableCompensationWithoutDeducting() throws Exception {
        product.setStock(1); when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        when(products.selectForUpdate(2L)).thenReturn(product); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("50.00")); payments.notifyPaid(signed("50.00"));
        assertEquals(5, order.getStatus()); assertEquals("alipay-1", order.getTradeNo());
        assertEquals(AutomaticRefundService.UNFULFILLABLE, order.getRefundReason());
        verify(orders,times(1)).updateById(any(Order.class)); verify(products,never()).deductStock(anyLong(),anyInt());
        verifyNoInteractions(alipay);
    }
    @Test void combinedCheckoutValidatesAggregateStockBeforeAnyDeduction() throws Exception {
        Order second = new Order(); second.setId(2L); second.setUserId(1L); second.setProductId(2L); second.setQuantity(4); second.setTotalPrice(new BigDecimal("100.00")); second.setStatus(0);
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order,second)));
        when(products.selectForUpdate(2L)).thenReturn(product); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("150.00"));
        assertEquals(5,order.getStatus()); assertEquals(5,second.getStatus()); verify(products,never()).deductStock(anyLong(),anyInt());
    }
    @Test void lateSignedPaymentForCanceledOrderIsCompensated() throws Exception {
        order.setStatus(4); when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        when(products.selectForUpdate(2L)).thenReturn(product); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("50.00")); payments.notifyPaid(signed("50.00"));
        assertEquals(5, order.getStatus());
        assertEquals(AutomaticRefundService.AUTO_PENDING, order.getRefundStatus());
        assertEquals(AutomaticRefundService.UNFULFILLABLE, order.getRefundReason());
        verify(orders, times(1)).updateById(any(Order.class));
        verify(products, never()).deductStock(anyLong(), anyInt());
        verifyNoInteractions(alipay, carts);
    }
    @Test void paymentCreationRejectsClosedOrders() {
        order.setStatus(4);
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        assertThrows(BusinessException.class, () -> payments.create("trade-1"));
        verifyNoInteractions(alipay);
    }
    @Test void paymentCreationUsesDatabaseTotal() {
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        payments.create("trade-1"); verify(alipay).create("trade-1", "商品订单", "50.00");
    }
    @Test void paymentCreationChecksOwnership() {
        when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(access).owner(1L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> payments.create("trade-1")); verifyNoInteractions(alipay);
    }
    @Test void heldStockCanBePaidWhenRemainingAvailabilityIsZero() throws Exception {
        product.setStock(0); when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        when(products.selectForUpdate(2L)).thenReturn(product); when(inventory.hasHeld(order)).thenReturn(true); when(inventory.consume(order)).thenReturn(true); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("50.00")); payments.notifyPaid(signed("50.00"));
        assertEquals(1, order.getStatus()); verify(inventory).consume(order); verify(products, never()).deductStock(anyLong(), anyInt());
    }
    @Test void delistedReservedProductReleasesBeforeCompensation() throws Exception {
        product.setStatus(0); when(orders.selectTradeForUpdate("trade-1")).thenReturn(new ArrayList<>(List.of(order)));
        when(products.selectForUpdate(2L)).thenReturn(product); when(inventory.hasHeld(order)).thenReturn(true); when(orders.updateById(any(Order.class))).thenReturn(1);
        payments.notifyPaid(signed("50.00")); payments.notifyPaid(signed("50.00"));
        assertEquals(5, order.getStatus()); verify(inventory).release(order); verify(inventory, never()).consume(any()); verify(products, never()).deductStock(anyLong(), anyInt());
    }
}
