package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Cart;
import org.example.springboot.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ✅ 解决 StringUtils 无法解析：本项目普遍使用 MyBatis-Plus 的工具类
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
// ✅ 为了返回空分页，需要用到 MyBatis-Plus 的 Page
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.example.springboot.mapper.UserMapper;
@RestController
@RequestMapping("/cart")
@Validated
public class CartController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private UserMapper userMapper;
    @PostMapping
    public Result<?> addToCart(@Valid @RequestBody Cart cart) {
        return cartService.addToCart(cart);
    }
    @PutMapping("/{id}")
    public Result<?> updateCartItem(
            @PathVariable @NotNull(message = "购物车ID不能为空") @Min(value = 1, message = "购物车ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量必须大于0") Integer quantity) {
        return cartService.updateCartItem(id, quantity);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteCartItem(@PathVariable @NotNull(message = "购物车ID不能为空") @Min(value = 1, message = "购物车ID必须大于0") Long id) {
        return cartService.deleteCartItem(id);
    }
    @GetMapping("/user/{userId}")
    public Result<?> getCartByUserId(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return cartService.getCartByUserId(userId);
    }
    @DeleteMapping("/clear/{userId}")
    public Result<?> clearCart(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return cartService.clearCart(userId);
    }
    @GetMapping("/page")
    public Result<?> getCartByPage(
            @RequestParam(required = false) String userId,  // 改为 String 类型
            @RequestParam(required = false) String productName,
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
                    Page<Cart> page = new Page<>(currentPage, size);
                    page.setTotal(0);
                    return Result.success(page);
                }
            }
        }
        
        // 调用 service 方法时使用转换后的 userIdLong
        return cartService.getCartByPage(userIdLong, productName, currentPage, size);
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "购物车ID列表不能为空") List<Long> ids) {
        return cartService.deleteBatch(ids);
    }
} 