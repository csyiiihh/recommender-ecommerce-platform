package org.example.springboot.service;

import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.example.springboot.config.AlipayConfig;
import com.alipay.api.internal.util.AlipaySignature;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class PaymentService {
    @Autowired private OrderMapper orders;
    @Autowired private UnpaidOrderExpiryService expiry;
    @Autowired private InventoryReservationService inventory;
    @Autowired private ProductMapper products;
    @Autowired private CartMapper carts;
    @Autowired private AccessService access;
    @Autowired private AlipayService alipay;
    @Autowired private AlipayConfig config;
    @Autowired private GroupBuyService groups;
    @Autowired private GroupBuyActivityMapper activities;
    @Autowired private GroupBuyTeamMapper teams;

    @Transactional
    public String create(String tradeNo) {
        if(tradeNo == null || tradeNo.isBlank()) throw new BusinessException("订单号不能为空");
        List<Order> list = orders.selectTradeForUpdate(tradeNo);
        if(list.isEmpty()) throw new BusinessException("订单不存在");
        BigDecimal total = BigDecimal.ZERO;
        java.time.Instant now = java.time.Instant.now();
        for(Order order : list) {
            access.owner(order.getUserId());
            if(!Integer.valueOf(0).equals(order.getStatus())) throw new BusinessException("订单状态不允许支付");
            if (order.getTradeNo() != null || order.getPayTime() != null) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "订单已记录支付信息，请刷新订单查看");
            }
            if (expiry.isPastPaymentDeadline(order, now)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "订单已超过付款期限，请取消后重新下单");
            }
            validateAmount(order);
            total = total.add(order.getTotalPrice());
        }
        return alipay.create(tradeNo, "商品订单", total.toPlainString());
    }

    // Notifications must be signed and belong to this application before any database changes.
    public void verifyNotification(Map<String,String> params) {
        try {
            if(config.getPublicKey().isBlank() || !AlipaySignature.rsaCheckV1(params, config.getPublicKey(), AlipayConfig.CHARSET, AlipayConfig.SIGNTYPE))
                throw new BusinessException("支付通知验签失败");
            if(!config.getAppId().equals(params.get("app_id"))) throw new BusinessException("支付应用不匹配");
            if(!config.getSellerId().isBlank() && !config.getSellerId().equals(params.get("seller_id"))) throw new BusinessException("收款方不匹配");
            if(!config.getGateway().contains("sandbox") && config.getSellerId().isBlank()) throw new BusinessException("请配置生产收款方ID");
            if(!Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(params.get("trade_status"))) throw new BusinessException("交易尚未成功");
            if(params.get("out_trade_no") == null || params.get("trade_no") == null || params.get("total_amount") == null) throw new BusinessException("支付通知参数缺失");
        } catch(com.alipay.api.AlipayApiException e) { throw new BusinessException("支付通知验签失败"); }
    }

    @Transactional(rollbackFor = Exception.class)
    public void notifyPaid(Map<String,String> params) {
        verifyNotification(params);
        String tradeNo = params.get("out_trade_no");
        List<Order> list = orders.selectTradeForUpdate(tradeNo);
        if(list.isEmpty()) throw new BusinessException("支付订单不存在");
        BigDecimal expected = BigDecimal.ZERO;
        for(Order order : list) { validateAmount(order); expected = expected.add(order.getTotalPrice()); }
        BigDecimal actual = new BigDecimal(params.get("total_amount"));
        if(actual.compareTo(expected) != 0) throw new BusinessException("支付金额不匹配");
        // Keep a consistent product lock order for multi-product checkouts.
        list.sort(Comparator.comparing(Order::getProductId).thenComparing(Order::getId));
        Map<Long, Product> locked = new HashMap<>();
        Map<Long, Integer> quantities = new HashMap<>();
        List<Order> unpaid = new ArrayList<>();
        boolean fulfillable = true;
        for (Order order : list) {
            if (!Integer.valueOf(0).equals(order.getStatus())) {
                if (Integer.valueOf(4).equals(order.getStatus()) && order.getTradeNo() == null) {
                    fulfillable = false; // Signed late payment after cancellation is returned, never shipped.
                } else {
                    if (!params.get("trade_no").equals(order.getTradeNo())) throw new BusinessException("订单交易号不匹配");
                    continue;
                }
            }
            unpaid.add(order);
            if (!locked.containsKey(order.getProductId())) locked.put(order.getProductId(), products.selectForUpdate(order.getProductId()));
            if (!inventory.hasHeld(order)) quantities.merge(order.getProductId(), order.getQuantity(), Integer::sum);
            Product product = locked.get(order.getProductId());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus())) fulfillable = false;
        }
        for (var entry : quantities.entrySet()) {
            Product product = locked.get(entry.getKey());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus()) || product.getStock() == null || product.getStock() < entry.getValue()) fulfillable = false;
        }
        for (Order order : unpaid) {
            if (Integer.valueOf(1).equals(order.getOrderType())) {
                GroupBuyActivity activity = activities.selectForUpdate(order.getActivityId());
                if (activity == null || !Integer.valueOf(1).equals(activity.getStatus()) || activity.getEndTime() == null || activity.getEndTime().before(new java.util.Date())) fulfillable = false;
                if (order.getGroupBuyTeamId() != null) {
                    GroupBuyTeam team = teams.selectForUpdate(order.getGroupBuyTeamId());
                    if (team == null || !Integer.valueOf(0).equals(team.getStatus()) || team.getCurrentPeople() >= team.getRequiredPeople() || team.getExpireTime() == null || team.getExpireTime().before(new java.util.Date())) fulfillable = false;
                }
            }
        }
        for (Order order : unpaid) {
            order.setStatus(fulfillable ? 1 : 5);
            order.setLastStatus(0);
            order.setPayMethod("alipay");
            order.setTradeNo(params.get("trade_no"));
            order.setPayAmount(order.getTotalPrice());
            order.setPayTime(new Timestamp(System.currentTimeMillis()));
            order.setUpdatedAt(order.getPayTime());
            if (!fulfillable) {
                inventory.release(order);
                order.setRefundStatus(AutomaticRefundService.AUTO_PENDING);
                order.setRefundReason(AutomaticRefundService.UNFULFILLABLE);
            }
            if (orders.updateById(order) != 1) throw new BusinessException("更新支付状态失败");
            if (!fulfillable) continue; // Any reservation was released; compensation must not replenish again.
            if (Integer.valueOf(1).equals(order.getOrderType())) groups.handlePaymentSuccess(order.getId());
            else if (!inventory.consume(order) && products.deductStock(order.getProductId(), order.getQuantity()) != 1) throw new BusinessException("库存扣减失败");
            carts.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, order.getUserId()).eq(Cart::getProductId, order.getProductId()));
        }
    }
    private void validateAmount(Order order) {
        if(order.getQuantity() == null || order.getQuantity() < 1 || order.getTotalPrice() == null || order.getTotalPrice().signum() <= 0) throw new BusinessException("订单金额或数量无效");
    }
}
