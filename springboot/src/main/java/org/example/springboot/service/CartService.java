package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Cart;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.CartMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    public Result<?> addToCart(Cart cart) {
        try {
            // 检查商品是否存在
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                LOGGER.warn("添加购物车失败，商品ID：{} 不存在", cart.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }

            // 检查库存是否足够
            if (product.getStock() < cart.getQuantity()) {
                LOGGER.warn("添加购物车失败，商品ID：{}，库存不足，需要：{}，库存：{}", 
                    cart.getProductId(), cart.getQuantity(), product.getStock());
                return ResultUtils.stockInsufficient();
            }

            // 检查是否已经存在相同商品
            LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Cart::getUserId, cart.getUserId())
                       .eq(Cart::getProductId, cart.getProductId());
            Cart existingCart = cartMapper.selectOne(queryWrapper);

            if (existingCart != null) {
                // 更新数量
                existingCart.setQuantity(existingCart.getQuantity() + cart.getQuantity());
                int result = cartMapper.updateById(existingCart);
                if (result > 0) {
                    LOGGER.info("更新购物车成功，购物车ID：{}，用户ID：{}，商品ID：{}，数量：{}", 
                        existingCart.getId(), cart.getUserId(), cart.getProductId(), existingCart.getQuantity());
                    return ResultUtils.success(existingCart);
                }
            } else {
                // 新增记录
                int result = cartMapper.insert(cart);
                if (result > 0) {
                    LOGGER.info("添加购物车成功，购物车ID：{}，用户ID：{}，商品ID：{}，数量：{}", 
                        cart.getId(), cart.getUserId(), cart.getProductId(), cart.getQuantity());
                    return ResultUtils.success(cart);
                }
            }
            LOGGER.warn("操作购物车失败，数据库操作返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作购物车失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("操作购物车异常，用户ID：{}，商品ID：{}，错误：{}", 
                cart.getUserId(), cart.getProductId(), e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作购物车失败：" + e.getMessage());
        }
    }

    public Result<?> updateCartItem(Long id, Integer quantity) {
        try {
            Cart cart = cartMapper.selectById(id);
            if (cart == null) {
                LOGGER.warn("更新购物车失败，购物车ID：{} 不存在", id);
                return ResultUtils.dataNotFound("购物车记录不存在");
            }

            // 检查商品库存
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                LOGGER.warn("更新购物车失败，商品ID：{} 不存在", cart.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }
            if (product.getStock() < quantity) {
                LOGGER.warn("更新购物车失败，商品ID：{}，库存不足，需要：{}，库存：{}", 
                    cart.getProductId(), quantity, product.getStock());
                return ResultUtils.stockInsufficient();
            }

            cart.setQuantity(quantity);
            int result = cartMapper.updateById(cart);
            if (result > 0) {
                LOGGER.info("更新购物车成功，购物车ID：{}，新数量：{}", id, quantity);
                return ResultUtils.success(cart);
            }
            LOGGER.warn("更新购物车失败，购物车ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新购物车失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新购物车异常，购物车ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新购物车失败：" + e.getMessage());
        }
    }

    public Result<?> deleteCartItem(Long id) {
        // 检查购物车记录是否存在
        Cart cart = cartMapper.selectById(id);
        if (cart == null) {
            LOGGER.warn("删除购物车失败，购物车ID：{} 不存在", id);
            return ResultUtils.dataNotFound("购物车记录不存在");
        }
        
        try {
            int result = cartMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除购物车成功，购物车ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除购物车失败，购物车ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除购物车失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除购物车异常，购物车ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除购物车失败：" + e.getMessage());
        }
    }

    public Result<?> getCartByUserId(Long userId) {
        try {
            LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Cart::getUserId, userId);
            List<Cart> carts = cartMapper.selectList(queryWrapper);
            
            if (carts != null && !carts.isEmpty()) {
                // 填充关联信息
                carts.forEach(cart -> {
                    cart.setUser(userMapper.selectById(cart.getUserId()));
                    cart.setProduct(productMapper.selectById(cart.getProductId()));
                });
                LOGGER.debug("查询用户购物车，用户ID：{}，购物车项数：{}", userId, carts.size());
                return ResultUtils.success(carts);
            }
            LOGGER.info("查询用户购物车，用户ID：{}，结果为空", userId);
            return ResultUtils.success(carts); // 返回空列表而不是错误
        } catch (Exception e) {
            LOGGER.error("查询用户购物车异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询购物车失败");
        }
    }

    public Result<?> clearCart(Long userId) {
        try {
            LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Cart::getUserId, userId);
            int result = cartMapper.delete(queryWrapper);
            if (result > 0) {
                LOGGER.info("清空购物车成功，用户ID：{}，删除数量：{}", userId, result);
                return ResultUtils.success();
            }
            LOGGER.info("清空购物车，用户ID：{}，购物车为空", userId);
            return ResultUtils.success(); // 即使没有数据也返回成功
        } catch (Exception e) {
            LOGGER.error("清空购物车异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "清空购物车失败：" + e.getMessage());
        }
    }

    public Result<?> getCartByPage(Long userId, String productName, Integer currentPage, Integer size) {
        try {
            LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
            if (userId != null) {
                queryWrapper.eq(Cart::getUserId, userId);
            }
            if (StringUtils.isNotEmpty(productName)) {
                List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().like(Product::getName, productName));
                List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
                if (!productIds.isEmpty()) {
                    queryWrapper.in(Cart::getProductId, productIds);
                } else {
                    // 如果没有匹配的商品，返回空结果
                    Page<Cart> emptyPage = new Page<>(currentPage, size);
                    emptyPage.setTotal(0);
                    return ResultUtils.success(emptyPage);
                }
            }

            Page<Cart> page = new Page<>(currentPage, size);
            Page<Cart> result = cartMapper.selectPage(page, queryWrapper);

            // 填充关联信息
            result.getRecords().forEach(cart -> {
                cart.setUser(userMapper.selectById(cart.getUserId()));
                cart.setProduct(productMapper.selectById(cart.getProductId()));
            });

            LOGGER.debug("分页查询购物车，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询购物车异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询购物车列表失败");
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除购物车项失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 使用新的批量删除方法
            int result = 0;
            for (Long id : ids) {
                result += cartMapper.deleteById(id);
            }
            
            if (result > 0) {
                LOGGER.info("批量删除购物车项成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除购物车项失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除购物车项失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除购物车项异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除购物车项失败：" + e.getMessage());
        }
    }
} 