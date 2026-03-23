package org.example.springboot.controller;

import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.CartMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.service.AlipayService;
import org.example.springboot.service.GroupBuyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付宝支付控制器
 */
@Controller
@CrossOrigin
@RequestMapping("/alipay")
public class AlipayController {
    
    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);
    
    @Autowired
    private AlipayService alipayService;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private CartMapper cartMapper;
    
    @Autowired
    private GroupBuyService groupBuyService;
    
    /**
     * 创建支付订单
     */
    @ResponseBody
    @PostMapping(value = "/create", produces = "text/html;charset=utf-8")
    public String create(@RequestParam("orderNo") String orderNo,
                         @RequestParam("orderName") String orderName,
                         @RequestParam("payPrice") String payPrice) {
        log.info("[支付宝] 创建支付订单，订单号：{}，订单名称：{}，支付金额：{}", orderNo, orderName, payPrice);
        return alipayService.create(orderNo, orderName, payPrice);
    }
    
    /**
     * 支付退款
     */
    @ResponseBody
    @PostMapping(value = "/refund")
    public Result<?> refund(@RequestParam("orderNo") String orderNo,
                            @RequestParam("payPrice") String payPrice) {
        try {
            log.info("[支付宝] 申请退款，订单号：{}，退款金额：{}", orderNo, payPrice);
            alipayService.refund(orderNo, payPrice);
            return Result.success("退款成功");
        } catch (AlipayApiException e) {
            log.error("【支付宝支付】退款失败", e);
            return Result.error("-1", "退款失败：" + e.getMessage());
        }
    }
    
    /**
     * 支付成功同步回调
     */
    @GetMapping(value = "/success")
    public void success(@RequestParam Map<String, String> map, HttpServletResponse response) {
        try {
            String outTradeNo = map.get("out_trade_no");
            String tradeNo = map.get("trade_no");
            String totalAmount = map.get("total_amount");
            
            log.info("[支付宝] 同步回调成功，商户订单号：{}，支付宝交易号：{}，支付金额：{}", outTradeNo, tradeNo, totalAmount);
            
            // 更新订单支付状态
            updateOrderPaymentStatus(outTradeNo, tradeNo, totalAmount);
            
            // 重定向到前端订单页面
            response.sendRedirect("http://localhost:8080/#/order");
        } catch (Exception e) {
            log.error("[支付宝] 同步回调处理失败", e);
        }
    }
    
    /**
     * 支付成功异步回调
     */
    @ResponseBody
    @PostMapping(value = "/notify")
    public String payNotify(@RequestParam Map<String, String> map) {
        String tradeStatus = map.get("trade_status");
        
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            String payTime = map.get("gmt_payment");
            String outTradeNo = map.get("out_trade_no");
            String tradeNo = map.get("trade_no");
            String tradeName = map.get("subject");
            String payAmount = map.get("buyer_pay_amount");
            
            log.info("[支付成功] 交易时间：{}，商户订单号：{}，支付宝交易号：{}，订单名称：{}，交易金额：{}",
                    payTime, outTradeNo, tradeNo, tradeName, payAmount);
            
            // 更新订单支付状态
            updateOrderPaymentStatus(outTradeNo, tradeNo, payAmount);
            
            return "success";
        }
        
        return "failure";
    }
    
    /**
     * 更新订单支付状态和库存
     */
    @Transactional
    public void updateOrderPaymentStatus(String outTradeNo, String tradeNo, String payAmount) {
        try {
            // 通过outTradeNo查询所有相关订单
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("out_trade_no", outTradeNo);
            List<Order> orders = orderMapper.selectList(queryWrapper);
            
            if (orders != null && !orders.isEmpty()) {
                for (Order order : orders) {
                    if (order.getStatus() == 0) {  // 只更新待支付状态的订单
                        // 检查是否为拼团订单
                        boolean isGroupBuyOrder = (order.getOrderType() != null && order.getOrderType() == 1);
                        
                        if (isGroupBuyOrder) {
                            // 拼团订单：更新订单状态和支付信息
                            order.setStatus(1);  // 已支付
                            order.setLastStatus(0);
                            order.setPayMethod("alipay");
                            order.setTradeNo(tradeNo);
                            order.setPayTime(Timestamp.valueOf(LocalDateTime.now()));
                            orderMapper.updateById(order);
                            
                            // 调用拼团服务处理拼团逻辑（创建团或加入团）
                            groupBuyService.handlePaymentSuccess(order.getId());
                            
                            log.info("[拼团订单支付成功] 订单ID：{}，状态已更新", order.getId());
                        } else {
                            // 普通订单：更新订单状态并扣减库存
                            order.setStatus(1);  // 已支付
                            order.setLastStatus(0);
                            order.setPayMethod("alipay");
                            order.setTradeNo(tradeNo);
                            order.setPayTime(Timestamp.valueOf(LocalDateTime.now()));
                            
                            orderMapper.updateById(order);
                            
                            // 扣减库存，增加销量
                            Product product = productMapper.selectById(order.getProductId());
                            if (product != null) {
                                if (product.getStock() >= order.getQuantity()) {
                                    product.setStock(product.getStock() - order.getQuantity());
                                    product.setSalesCount(product.getSalesCount() + order.getQuantity());
                                    productMapper.updateById(product);
                                    
                                    log.info("[库存扣减] 商品ID：{}，扣减数量：{}，剩余库存：{}", 
                                            product.getId(), order.getQuantity(), product.getStock());
                                } else {
                                    log.error("[库存不足] 商品ID：{}，需要：{}，库存：{}", 
                                            product.getId(), order.getQuantity(), product.getStock());
                                }
                            }
                            
                            // 清理购物车（通过用户ID和商品ID查找）
                            QueryWrapper<org.example.springboot.entity.Cart> cartQueryWrapper = new QueryWrapper<>();
                            cartQueryWrapper.eq("user_id", order.getUserId())
                                           .eq("product_id", order.getProductId());
                            cartMapper.delete(cartQueryWrapper);
                        }
                    }
                }
                
                log.info("[支付成功] 订单号：{}，更新订单数：{}", outTradeNo, orders.size());
            }
            
        } catch (Exception e) {
            log.error("[支付回调] 更新订单状态失败", e);
        }
    }
}

