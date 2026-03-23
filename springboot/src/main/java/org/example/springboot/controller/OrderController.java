package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserMapper userMapper;
    @PostMapping
    public Result<?> createOrder(@Valid @RequestBody Order order) {
        return orderService.createOrder(order);
    }
    @PutMapping("/{id}/status")
    public Result<?> updateOrderStatus(
            @PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return orderService.updateOrderStatus(id, status);
    }
    @PutMapping("/{id}/pay")
    public Result<?> pay(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.payOrder(id);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteOrder(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.deleteOrder(id);
    }
    @GetMapping("/{id}")
    public Result<?> getOrderById(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.getOrderById(id);
    }
    @GetMapping("/user/{userId}")
    public Result<?> getOrdersByUserId(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return orderService.getOrdersByUserId(userId);
    }
    @GetMapping("/page")
    public Result<?> getOrdersByPage(
            @RequestParam(required = false) String userId,  // 改为 String 类型
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        
        // 验证 userId 是否为有效数字
        Long userIdLong = null;
        if (StringUtils.isNotBlank(userId)) {
            try {
                userIdLong = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                // 如果解析失败，说明是用户名，需要查询用户ID
                userIdLong = userMapper.selectIdByUsername(userId);
                if (userIdLong == null) {
                    // 如果用户名不存在，返回空结果
                    Page<Order> page = new Page<>(currentPage, size);
                    page.setTotal(0);
                    return Result.success(page);
                }
            }
        }
        
        // 调用 service 方法时使用转换后的 userIdLong
        return orderService.getOrdersByPage(userIdLong, id, status, merchantId, currentPage, size);
    }
    @PostMapping("/{id}/refund")
    public Result<?> refundOrder(
            @PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id, 
            @RequestParam @NotBlank(message = "退款原因不能为空") String reason) {
        return orderService.refundOrder(id, reason);
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "订单ID列表不能为空") List<Long> ids) {
        return orderService.deleteBatch(ids);
    }
    @PutMapping("/{id}/address")
    public Result<?> updateOrderAddress(
            @PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id,
            @RequestParam @NotBlank(message = "收货人姓名不能为空") String name,
            @RequestParam @NotBlank(message = "收货地址不能为空") String address,
            @RequestParam @NotBlank(message = "联系电话不能为空") String phone) {
        return orderService.updateOrderAddress(name,id, address, phone);
    }
    // 更新订单信息
    @PutMapping("/{id}")
    public Result<?> updateOrder(
            @PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id,
            @Valid @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }
    @GetMapping("/{id}/logistics")
    public Result<?> getOrderLogistics(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.getOrderLogistics(id);
    }
    @PutMapping("/{id}/handle-refund")
    public Result<?> handleRefund(
            @PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id,
            @RequestParam @NotNull(message = "状态不能为空") Integer status,
            @RequestParam @NotBlank(message = "备注不能为空") String remark) {
        return orderService.handleRefund(id, status, remark);
    }

    /**
     * 获取订单状态
     */
    @GetMapping("/{id}/status")
    public Result<Integer> getOrderStatus(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.getOrderStatus(id);
    }
    
    /**
     * 从购物车创建订单（支付宝支付）
     */
    @PostMapping("/cart/create")
    public Result<?> createOrdersFromCart(@RequestBody Map<String, Object> params) {
        return orderService.createOrdersFromCart(params);
    }
    
    /**
     * 准备单个订单支付（支付宝支付）
     */
    @PostMapping("/{id}/prepare-pay")
    public Result<?> prepareOrderPay(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.prepareOrderPay(id);
    }
    
    /**
     * 余额支付（一键支付）- 单个订单
     */
    @PostMapping("/{id}/pay-balance")
    public Result<?> payByBalance(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long id) {
        return orderService.payByBalance(id);
    }
    
    /**
     * 余额支付（一键支付）- 批量支付购物车订单
     */
    @PostMapping("/cart/pay-balance")
    public Result<?> payCartByBalance(@RequestBody Map<String, Object> params) {
        return orderService.payCartByBalance(params);
    }
} 