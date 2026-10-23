package org.example.springboot;

import org.example.springboot.controller.OrderController;
import org.example.springboot.dto.OrderSummary;
import org.example.springboot.entity.User;
import org.example.springboot.exception.GlobalExceptionHandler;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.service.AccessService;
import org.example.springboot.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderSummaryTest {
    @Mock OrderMapper orders;
    @Mock AccessService access;
    @InjectMocks OrderService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        OrderController controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    void login(String role) {
        User buyer = new User(); buyer.setId(42L); buyer.setRole(role);
        when(access.current()).thenReturn(buyer);
    }
    @Test void summaryUsesAuthenticatedBuyerAndOneAggregateQuery() {
        login("USER"); var counts = new OrderSummary(103, 27, 41);
        when(orders.selectBuyerSummary(42L)).thenReturn(counts);
        assertSame(counts, service.getMyOrderSummary().getData());
        verify(orders).selectBuyerSummary(42L); verifyNoMoreInteractions(orders);
    }
    @Test void clientCannotSelectAnotherBuyersSummary() throws Exception {
        login("USER"); when(orders.selectBuyerSummary(42L)).thenReturn(new OrderSummary(103, 27, 41));
        mvc.perform(get("/order/my-summary").param("userId", "99").param("merchantId", "77"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(103))
            .andExpect(jsonPath("$.data.unpaid").value(27)).andExpect(jsonPath("$.data.pending").value(41));
        verify(orders).selectBuyerSummary(42L); verifyNoMoreInteractions(orders);
    }
    @Test void administratorStillReceivesOwnBuyerSummary() {
        login("ADMIN"); when(orders.selectBuyerSummary(42L)).thenReturn(new OrderSummary(4, 1, 2));
        assertEquals(4, service.getMyOrderSummary().getData().total()); verify(orders).selectBuyerSummary(42L);
    }
    @Test void merchantReceivesOwnPurchasesRatherThanStoreSales() {
        login("MERCHANT"); when(orders.selectBuyerSummary(42L)).thenReturn(new OrderSummary(3, 0, 1));
        assertEquals(3, service.getMyOrderSummary().getData().total()); verify(orders).selectBuyerSummary(42L);
    }
    @Test void unauthenticatedRequestCannotReadAnyCounts() throws Exception {
        when(access.current()).thenThrow(new AccessDeniedException("login required"));
        mvc.perform(get("/order/my-summary")).andExpect(status().isForbidden()); verifyNoInteractions(orders);
    }
    @Test void emptyBuyerReturnsNumericZeroCounts() throws Exception {
        login("USER"); when(orders.selectBuyerSummary(42L)).thenReturn(new OrderSummary(0, 0, 0));
        mvc.perform(get("/order/my-summary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0)).andExpect(jsonPath("$.data.unpaid").value(0))
            .andExpect(jsonPath("$.data.pending").value(0));
    }
    @Test void queryFailureIsNotReplacedWithInventedZeroCounts() {
        login("USER"); when(orders.selectBuyerSummary(42L)).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, service::getMyOrderSummary);
        verify(orders).selectBuyerSummary(42L); verifyNoMoreInteractions(orders);
    }
}
