package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.*;
import org.example.springboot.service.AccessService;
import org.example.springboot.service.email.VerificationCodeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessAndVerificationTest {
    @Mock OrderMapper orders;
    @Mock ProductMapper products;
    @InjectMocks AccessService access;
    User user;
    @BeforeEach void setup() {
        user = new User(); user.setId(1L); user.setRole("USER");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void userCannotChooseAnotherIdentity() { assertThrows(AccessDeniedException.class, () -> access.owner(2L)); }
    @Test void unscopedListsAreRestrictedToCurrentUser() { assertEquals(1L, access.scope(null)); }
    @Test void userCannotAccessAnotherOrder() {
        Order order = new Order(); order.setUserId(2L); when(orders.selectById(9L)).thenReturn(order);
        assertThrows(AccessDeniedException.class, () -> access.order(9L, true));
    }
    @Test void merchantCannotManageAnotherMerchantsProduct() {
        user.setRole("MERCHANT"); Product product = new Product(); product.setMerchantId(2L);
        when(products.selectById(9L)).thenReturn(product);
        assertThrows(AccessDeniedException.class, () -> access.product(9L));
    }
    @Test void adminCanManageResources() { user.setRole("ADMIN"); assertDoesNotThrow(() -> access.owner(2L)); }
    @Test void verificationCodeIsSingleUseAndPurposeBound() {
        VerificationCodeService codes = new VerificationCodeService(); codes.storeCode("register", "a@example.com", "123456", 10);
        assertFalse(codes.verifyCode("reset", "a@example.com", "123456"));
        assertTrue(codes.verifyCode("register", "a@example.com", "123456"));
        assertFalse(codes.verifyCode("register", "a@example.com", "123456"));
    }
    @Test void verificationAttemptsAreBounded() {
        VerificationCodeService codes = new VerificationCodeService(); codes.storeCode("reset", "a@example.com", "123456", 10);
        for(int i=0;i<5;i++) assertFalse(codes.verifyCode("reset", "a@example.com", "wrong"));
        assertFalse(codes.verifyCode("reset", "a@example.com", "123456"));
    }
    @Test void resendHasCooldown() {
        VerificationCodeService codes = new VerificationCodeService(); codes.storeCode("reset", "a@example.com", "123456", 10);
        assertThrows(IllegalArgumentException.class, () -> codes.storeCode("reset", "a@example.com", "999999", 10));
    }
    @Test void expiredVerificationCodeIsRejected() {
        VerificationCodeService codes = new VerificationCodeService(); codes.storeCode("reset", "a@example.com", "123456", -1);
        assertFalse(codes.verifyCode("reset", "a@example.com", "123456"));
    }
}
