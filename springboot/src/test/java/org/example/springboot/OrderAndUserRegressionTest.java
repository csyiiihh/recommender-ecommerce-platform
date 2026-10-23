package org.example.springboot;

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
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAndUserRegressionTest {
    @Mock ProductMapper productMapper;
    @Mock CartMapper cartMapper;
    @Mock OrderMapper orderMapper;
    @Mock UserMapper userMapper;
    @Mock StockInMapper stockInMapper;
    @Mock AddressMapper addressMapper;
    @Mock AccessService access;
    @Mock FulfillmentPolicy fulfillment;
    @Mock org.example.springboot.service.email.VerificationCodeService codes;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @org.mockito.Mock org.example.springboot.service.InventoryReservationService inventory;
    @InjectMocks OrderService orders;
    @InjectMocks UserService users;
    @Test void submittedPriceAndPaidStateAreIgnoredOnCreate() {
        User current = new User(); current.setId(1L); when(access.current()).thenReturn(current);
        Product product = new Product(); product.setId(2L); product.setStock(10); product.setStatus(1); product.setPrice(new BigDecimal("25.00"));
        when(productMapper.selectById(2L)).thenReturn(product); when(orderMapper.insert(any(Order.class))).thenReturn(1);
        Order input = new Order(); input.setUserId(1L); input.setProductId(2L); input.setPrice(new BigDecimal("0.01")); input.setQuantity(2); input.setStatus(1);
        orders.createOrder(input);
        ArgumentCaptor<Order> inserted=ArgumentCaptor.forClass(Order.class); verify(orderMapper).insert(inserted.capture());
        assertEquals(new BigDecimal("50.00"), inserted.getValue().getTotalPrice()); assertEquals(0,inserted.getValue().getStatus());
        verify(inventory).reserve(inserted.getValue());
    }
    @Test void negativeQuantityIsRejected() {
        Order input = new Order(); input.setUserId(1L); input.setQuantity(-1);
        assertThrows(BusinessException.class, () -> orders.createOrder(input)); verifyNoInteractions(orderMapper, productMapper);
    }
    @Test void disabledBalanceEntryCannotChangeInventory() {
        assertNotEquals("0", orders.payByBalance(1L).getCode()); assertNotEquals("0",orders.payOrder(1L).getCode());
        verifyNoInteractions(orderMapper,productMapper);
    }
    @Test void blockedUserDeletionDoesNotDeleteRelations() {
        when(stockInMapper.selectList(any())).thenReturn(List.of(new StockIn()));
        assertEquals(-2, users.deleteUserById(1)); verifyNoInteractions(addressMapper, orderMapper); verify(userMapper,never()).deleteById(any(java.io.Serializable.class));
    }
    @Test void publicRegistrationCannotAssignAdminRole() {
        User input = new User(); input.setUsername("测试用户"); input.setEmail("buyer@example.com");
        input.setPassword("password123"); input.setRole("ADMIN"); input.setCode("123456");
        when(codes.verifyCode("register", "buyer@example.com", "123456")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        assertEquals("0", users.register(input).getCode());
        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class); verify(userMapper).insert(inserted.capture());
        assertEquals("USER", inserted.getValue().getRole()); assertNull(inserted.getValue().getId());
        assertEquals("encoded-password", inserted.getValue().getPassword());
    }
    @Test void unverifiedRegistrationCannotWriteUser() {
        User input = new User(); input.setUsername("buyer"); input.setEmail("buyer@example.com"); input.setPassword("password123");
        assertNotEquals("0", users.register(input).getCode()); verifyNoInteractions(userMapper);
    }
    @Test void passwordResetWithoutVerifiedCodeCannotTouchAccount() {
        assertFalse(users.resetPassword("buyer@example.com", "new-password", "wrong")); verifyNoInteractions(userMapper);
    }
    @Test void rejectedRefundCanContinueAuthorizedFulfillment() {
        Order order = new Order(); order.setId(1L); order.setStatus(7); order.setLastStatus(1);
        when(orderMapper.selectForUpdate(1L)).thenReturn(order);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        when(access.isMerchant()).thenReturn(true);
        assertEquals("0", orders.updateOrderStatus(1L, 2).getCode());
        assertEquals(2, order.getStatus()); verify(access).sellerOrder(1L);
    }
    @Test void manualUnpaidCancellationReleasesReservation() {
        Order order = new Order(); order.setId(1L); order.setStatus(0);
        when(orderMapper.selectForUpdate(1L)).thenReturn(order); when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        orders.updateOrderStatus(1L, 4); verify(inventory).release(order); assertEquals(4, order.getStatus());
    }
    @Test void cartCheckoutReservesServerPricedOrderAfterSuccessfulInsert() {
        Cart cart = new Cart(); cart.setId(4L); cart.setUserId(1L); cart.setProductId(2L); cart.setQuantity(2);
        Product product = new Product(); product.setId(2L); product.setStatus(1); product.setStock(5); product.setPrice(new BigDecimal("25"));
        when(cartMapper.selectById(4L)).thenReturn(cart); when(productMapper.selectById(2L)).thenReturn(product);
        when(orderMapper.insert(any(Order.class))).thenAnswer(i -> { i.<Order>getArgument(0).setId(10L); return 1; });
        assertEquals("0", orders.createOrdersFromCart(java.util.Map.of("cartIds", List.of(4L), "userId", 1L)).getCode());
        ArgumentCaptor<Order> created = ArgumentCaptor.forClass(Order.class); verify(inventory).reserve(created.capture());
        assertEquals(10L, created.getValue().getId()); assertEquals(new BigDecimal("50"), created.getValue().getTotalPrice());
        verify(cartMapper, never()).delete(any());
    }
}
