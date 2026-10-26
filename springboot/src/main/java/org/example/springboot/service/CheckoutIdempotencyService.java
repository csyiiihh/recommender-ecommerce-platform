package org.example.springboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.util.*;

@Service
public class CheckoutIdempotencyService {
    private final UserMapper users;
    private final OrderMapper orders;
    private final ObjectMapper json;
    public CheckoutIdempotencyService(UserMapper users, OrderMapper orders, ObjectMapper json) { this.users = users; this.orders = orders; this.json = json; }
    public record Token(String tradeNo, List<Order> existing) {}
    /** Caller must use READ_COMMITTED, and call before inventory or order reads/writes. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Token prepare(Long buyerId, String scope, String key, List<?> payload) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,128}") || buyerId == null || buyerId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效下单请求标识");
        if (!Set.of("single", "cart", "group-start", "group-join").contains(scope)) throw new IllegalArgumentException("Unknown checkout scope");
        String prefix = digest(Arrays.asList(scope, buyerId, key)).substring(0, 32);
        String tradeNo = prefix + digest(payload).substring(0, 32);
        var buyer = users.selectForUpdate(buyerId);
        if (buyer == null || !Integer.valueOf(1).equals(buyer.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "用户不可用");
        // Buyer lock serializes same-user allocations. Avoid missing-row gap locks here.
        List<Order> existing = orders.selectCheckoutByPrefix(buyerId, prefix);
        for (Order order : existing) {
            if (!buyerId.equals(order.getUserId()) || !Integer.valueOf(scope.startsWith("group-") ? 1 : 0).equals(order.getOrderType()) || !tradeNo.equals(order.getOutTradeNo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该下单请求已用于其他内容，请检查已有订单后重新提交");
            }
        }
        if (!"cart".equals(scope) && existing.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "下单请求记录异常，请检查已有订单");
        return new Token(tradeNo, List.copyOf(existing));
    }
    private String digest(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value))); }
        catch (java.security.GeneralSecurityException | com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Cannot fingerprint checkout request", e); }
    }
}
