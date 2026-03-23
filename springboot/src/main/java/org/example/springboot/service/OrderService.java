package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Cart;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.Logistics;
import org.example.springboot.entity.GroupBuyMember;
import org.example.springboot.entity.GroupBuyTeam;
import org.example.springboot.entity.GroupBuyActivity;
import org.example.springboot.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private AddressMapper addressMapper;
    
    @Autowired
    private CartMapper cartMapper;
    
    @Autowired
    private GroupBuyMemberMapper groupBuyMemberMapper;
    
    @Autowired
    private GroupBuyTeamMapper groupBuyTeamMapper;
    
    @Autowired
    private GroupBuyActivityMapper groupBuyActivityMapper;
    
    @Autowired
    private GroupBuyService groupBuyService;



    public Result<?> createOrder(Order order) {
        try {
            // 检查商品库存
            Product product = productMapper.selectById(order.getProductId());
            if (product == null) {
                LOGGER.warn("创建订单失败，商品ID：{} 不存在", order.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }
            if (product.getStock() < order.getQuantity()) {
                LOGGER.warn("创建订单失败，商品ID：{}，库存不足，需要：{}，库存：{}", 
                    order.getProductId(), order.getQuantity(), product.getStock());
                return ResultUtils.stockInsufficient();
            }

            // 计算总价
            order.setTotalPrice(order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));

            int result = orderMapper.insert(order);

            if (result > 0) {
                LOGGER.info("创建订单成功，订单ID：{}，用户ID：{}，商品ID：{}，数量：{}", 
                    order.getId(), order.getUserId(), order.getProductId(), order.getQuantity());
                return ResultUtils.success(order);
            }
            LOGGER.warn("创建订单失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建订单失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建订单异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建订单失败：" + e.getMessage());
        }
    }

    public Result<?> updateOrderStatus(Long id, Integer status) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("更新订单状态失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            order.setLastStatus(order.getStatus());
            order.setStatus(status);
            int result = orderMapper.updateById(order);
            if (result > 0) {
                // 查找该订单的物流信息
                LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Logistics::getOrderId, id);
                Logistics logistics = logisticsMapper.selectOne(queryWrapper);
                
                if (logistics != null) {
                    // 如果订单状态变为已退款，更新物流状态为已取消
                    if (status == 6) { // 6表示已退款
                        logistics.setStatus(3); // 3表示已取消
                        logisticsMapper.updateById(logistics);
                        LOGGER.info("订单退款成功，同步更新物流状态为已取消，物流ID：{}", logistics.getId());
                    }
                    // 如果订单状态变为已完成，更新物流状态为已签收
                    else if (status == 3) { // 3表示已完成
                        logistics.setStatus(2); // 2表示已签收
                        logisticsMapper.updateById(logistics);
                        LOGGER.info("订单已完成，同步更新物流状态为已签收，物流ID：{}", logistics.getId());
                    }
                }

                LOGGER.info("更新订单状态成功，订单ID：{}，新状态：{}", id, status);
                return ResultUtils.success(order);
            }
            LOGGER.warn("更新订单状态失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新订单状态异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单状态失败：" + e.getMessage());
        }
    }

    public Result<?> deleteOrder(Long id) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            LOGGER.warn("删除订单失败，订单ID：{} 不存在", id);
            return ResultUtils.dataNotFound("订单不存在");
        }
        
        try {
            deleteRelation(id);
            int result = orderMapper.deleteById(id);

            if (result > 0) {
                LOGGER.info("删除订单成功，订单ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除订单失败，订单ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除订单失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除订单异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除订单失败：" + e.getMessage());
        }
    }

    public void deleteRelation(Long id){
        // 删除物流记录
        logisticsMapper.delete(new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId,id));
        
        // 检查是否是拼团订单
        Order order = orderMapper.selectById(id);
        if (order != null && order.getOrderType() != null && order.getOrderType() == 1) {
            // 查询该订单对应的成员记录
            LambdaQueryWrapper<GroupBuyMember> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(GroupBuyMember::getOrderId, id);
            GroupBuyMember member = groupBuyMemberMapper.selectOne(memberQuery);
            
            if (member != null) {
                Long teamId = member.getTeamId();
                Integer isLeader = member.getIsLeader();
                
                // 删除成员记录
                groupBuyMemberMapper.delete(memberQuery);
                
                if (isLeader != null && isLeader == 1) {
                    // 如果是团长订单，删除整个团和所有成员
                    LOGGER.info("删除团长订单，同时删除整个拼团，团ID：{}", teamId);
                    
                    // 删除该团的所有成员记录
                    groupBuyMemberMapper.delete(
                        new LambdaQueryWrapper<GroupBuyMember>().eq(GroupBuyMember::getTeamId, teamId)
                    );
                    
                    // 删除团记录
                    groupBuyTeamMapper.deleteById(teamId);
                    
                } else {
                    // 如果是团员订单，更新团的人数
                    GroupBuyTeam team = groupBuyTeamMapper.selectById(teamId);
                    if (team != null && team.getCurrentPeople() > 0) {
                        team.setCurrentPeople(team.getCurrentPeople() - 1);
                        groupBuyTeamMapper.updateById(team);
                        LOGGER.info("删除团员订单，更新团人数，团ID：{}，剩余人数：{}", teamId, team.getCurrentPeople());
                    }
                }
            }
        }
        
        LOGGER.info("删除订单关联数据成功，订单ID：{}", id);
    }

    public Result<?> getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            LOGGER.warn("查询订单失败，订单ID：{} 不存在", id);
            return ResultUtils.dataNotFound("订单不存在");
        }
        
        // 填充关联信息
        order.setUser(userMapper.selectById(order.getUserId()));
        order.setProduct(productMapper.selectById(order.getProductId()));
        return ResultUtils.success(order);
    }

    public Result<?> getOrdersByUserId(Long userId) {
        try {
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getUserId, userId);
            queryWrapper.orderByDesc(Order::getCreatedAt);
            List<Order> orders = orderMapper.selectList(queryWrapper);
            
            if (orders != null && !orders.isEmpty()) {
                // 填充关联信息
                orders.forEach(order -> {
                    order.setUser(userMapper.selectById(order.getUserId()));
                    order.setProduct(productMapper.selectById(order.getProductId()));
                });
                LOGGER.debug("查询用户订单，用户ID：{}，订单数：{}", userId, orders.size());
                return ResultUtils.success(orders);
            }
            LOGGER.info("查询用户订单，用户ID：{}，结果为空", userId);
            return ResultUtils.success(orders); // 返回空列表而不是错误
        } catch (Exception e) {
            LOGGER.error("查询用户订单异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询订单列表失败");
        }
    }

    /**
     * 获取订单状态
     */
    public Result<Integer> getOrderStatus(Long id) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("获取订单状态失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }
            return ResultUtils.success(order.getStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("获取订单状态异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取订单状态失败：" + e.getMessage());
        }
    }

    public Result<?> getOrdersByPage(Long userId,Long id,String status, Long merchantId,Integer currentPage, Integer size) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        if (id != null) {
            queryWrapper.eq(Order::getId, id);
        }
        if(StringUtils.isNotBlank(status)){
            queryWrapper.eq(Order::getStatus,status);
        }
        if (merchantId != null) {
            List<Product> product = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));

            if(!product.isEmpty()){
            List<Long> productIds = product.stream().map(Product::getId).collect(Collectors.toList());
            queryWrapper.in(Order::getProductId, productIds);
            }else{
                Page<Order> page = new Page<>(currentPage, size);
                page.setTotal(0);
                page.setRecords(null);
                return ResultUtils.success(page);
            }
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(currentPage, size);
        Page<Order> result = orderMapper.selectPage(page, queryWrapper);

        // 填充关联信息
        result.getRecords().forEach(order -> {
            order.setUser(userMapper.selectById(order.getUserId()));
            Product product = productMapper.selectById(order.getProductId());
            if (product != null) {
                order.setProduct(product);
                order.setMerchant(userMapper.selectById(product.getMerchantId()));
            } else {
                order.setProduct(null);
                order.setMerchant(null);
            }
        });

        LOGGER.debug("分页查询订单，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
        return ResultUtils.success(result);
    }

    public Result<?> refundOrder(Long id, String reason) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("申请退款失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单状态是否允许退款
            if (order.getStatus() != 1 && order.getStatus() != 2) {
                LOGGER.warn("申请退款失败，订单ID：{}，当前状态：{} 不允许退款", id, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许退款");
            }

            order.setLastStatus(order.getStatus());  // 保存当前状态
            order.setStatus(5);  // 设为退款中
            order.setRefundStatus(1); // 申请退款
            order.setRefundReason(reason);
            int result = orderMapper.updateById(order);
            if (result > 0) {
                LOGGER.info("申请退款成功，订单ID：{}，退款原因：{}", id, reason);
                return ResultUtils.success(order);
            }
            LOGGER.warn("申请退款失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "申请退款失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("申请退款异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "申请退款失败：" + e.getMessage());
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除订单失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个订单是否存在
            for (Long id : ids) {
                Order order = orderMapper.selectById(id);
                if (order == null) {
                    LOGGER.warn("批量删除订单，订单ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("订单ID：" + id + " 不存在");
                }
                // 删除关联数据
                deleteRelation(id);
            }

            // 使用新的批量删除方法
            int result = 0;
            for (Long id : ids) {
                result += orderMapper.deleteById(id);
            }
            
            if (result > 0) {
                LOGGER.info("批量删除订单成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除订单失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除订单失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除订单异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除订单失败：" + e.getMessage());
        }
    }
    @Transactional
    public Result<?> payOrder(Long id) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("支付订单失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            Product product = productMapper.selectById(order.getProductId());
            if (product == null) {
                LOGGER.warn("支付订单失败，订单ID：{}，商品ID：{} 不存在", id, order.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }
            
            if (product.getStock() < order.getQuantity()) {
                LOGGER.warn("支付订单失败，订单ID：{}，库存不足，需要：{}，库存：{}", 
                    id, order.getQuantity(), product.getStock());
                return ResultUtils.stockInsufficient();
            }
            
            product.setSalesCount(product.getSalesCount() + order.getQuantity());
            product.setStock(product.getStock() - order.getQuantity());
            order.setStatus(1);
            int res = productMapper.updateById(product);

            if (res <= 0) {
                LOGGER.error("支付订单失败，订单ID：{}，更新商品库存失败", id);
                throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "支付异常：更新商品库存失败");
            }
            updateOrder(order.getId(), order);
            
            LOGGER.info("支付订单成功，订单ID：{}，用户ID：{}", id, order.getUserId());
            return ResultUtils.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("支付订单异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "支付失败：" + e.getMessage());
        }
    }

    public Result<?> updateOrderAddress(String name, Long id, String address, String phone) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("更新订单收货信息失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单状态，只有未发货的订单才能修改地址
            if (order.getStatus() > 1) {
                LOGGER.warn("更新订单收货信息失败，订单ID：{}，当前状态：{} 已发货，无法修改", id, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单已发货，无法修改收货地址");
            }
            
            order.setRecvName(name);
            order.setRecvAddress(address);
            order.setRecvPhone(phone);
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                LOGGER.info("更新订单收货信息成功，订单ID：{}", id);
                return ResultUtils.success(order);
            }
            LOGGER.warn("更新订单收货信息失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单收货信息失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新订单收货信息异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单收货信息失败：" + e.getMessage());
        }
    }
    public Result<?> updateOrder(Long id, Order order) {
        try {
            Order existingOrder = orderMapper.selectById(id);
            if (existingOrder == null) {
                LOGGER.warn("更新订单失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 设置ID确保更新正确的订单
            order.setId(id);
            
            // 保持原有的不可修改字段
            order.setCreatedAt(existingOrder.getCreatedAt());
            order.setUserId(existingOrder.getUserId());
            order.setProductId(existingOrder.getProductId());
            order.setTotalPrice(existingOrder.getTotalPrice());
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                // 查找该订单的物流信息
                LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Logistics::getOrderId, id);
                Logistics logistics = logisticsMapper.selectOne(queryWrapper);
                
                if (logistics != null) {
                    // 如果订单状态变为已退款，更新物流状态为已取消
                    if (order.getStatus() == 6 && existingOrder.getStatus() != 6) {
                        logistics.setStatus(3); // 3表示已取消
                        logisticsMapper.updateById(logistics);
                        LOGGER.info("订单退款成功，同步更新物流状态为已取消，物流ID：{}", logistics.getId());
                    }
                    // 如果订单状态变为已完成，更新物流状态为已签收
                    else if (order.getStatus() == 3 && existingOrder.getStatus() != 3) {
                        logistics.setStatus(2); // 2表示已签收
                        logisticsMapper.updateById(logistics);
                        LOGGER.info("订单已完成，同步更新物流状态为已签收，物流ID：{}", logistics.getId());
                    }
                }

                LOGGER.info("更新订单成功，订单ID：{}", id);
                return ResultUtils.success(order);
            }
            LOGGER.warn("更新订单失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单信息失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新订单异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新订单失败：" + e.getMessage());
        }
    }

    public Result<?> getOrderLogistics(Long orderId) {
        try {
            // 检查订单是否存在
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                LOGGER.warn("查询订单物流信息失败，订单ID：{} 不存在", orderId);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 查询物流信息
            LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Logistics::getOrderId, orderId);
            Logistics logistics = logisticsMapper.selectOne(queryWrapper);
            
            if (logistics != null) {
                // 填充关联信息
                logistics.setOrder(order);
                return ResultUtils.success(logistics);
            }
            LOGGER.info("查询订单物流信息，订单ID：{}，物流信息不存在", orderId);
            return ResultUtils.dataNotFound("未找到物流信息");
        } catch (Exception e) {
            LOGGER.error("查询订单物流信息异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询订单物流信息失败：" + e.getMessage());
        }
    }

    /**
     * 处理退款申请
     * @param id 订单ID
     * @param status 退款状态：6-同意退款 7-拒绝退款
     * @param remark 处理备注
     * @return 处理结果
     */
    public Result<?> handleRefund(Long id, Integer status, String remark) {
        try {
            Order order = orderMapper.selectById(id);
            if (order == null) {
                LOGGER.warn("处理退款失败，订单ID：{} 不存在", id);
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单是否处于退款中状态
            if (order.getStatus() != 5) {
                LOGGER.warn("处理退款失败，订单ID：{}，当前状态：{} 不是退款中", id, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不是退款中");
            }

            // 保存原始状态
            order.setLastStatus(order.getStatus());
            // 更新状态
            order.setStatus(status);
            order.setRefundStatus(status == 6 ? 3 : 4); // 3-已退款 4-退款失败
            order.setRefundTime(Timestamp.valueOf(LocalDateTime.now()));
            order.setRemark(remark);
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                // 如果同意退款，恢复商品库存
                if (status == 6) {
                    Product product = productMapper.selectById(order.getProductId());
                    if (product != null) {
                        // 增加库存
                        product.setStock(product.getStock() + order.getQuantity());
                        
                        // ⭐ 对于拼团订单，销量的处理逻辑不同
                        if (order.getOrderType() != null && order.getOrderType() == 1) {
                            // 拼团订单：先不处理销量，交给 handleGroupBuyRefund 处理
                            LOGGER.info("拼团订单退款，商品库存已恢复，销量将在拼团逻辑中处理");
                        } else {
                            // 普通订单：直接减少销量
                            if (product.getSalesCount() >= order.getQuantity()) {
                                product.setSalesCount(product.getSalesCount() - order.getQuantity());
                            }
                        }
                        
                        productMapper.updateById(product);
                        LOGGER.info("退款成功，已恢复商品库存，商品ID：{}，数量：{}", product.getId(), order.getQuantity());
                    }
                    
                    // ⭐ 如果是拼团订单，需要处理拼团相关数据
                    if (order.getOrderType() != null && order.getOrderType() == 1) {
                        handleGroupBuyRefund(order);
                    }
                }

                // 同步更新物流状态
                LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Logistics::getOrderId, id);
                Logistics logistics = logisticsMapper.selectOne(queryWrapper);
                if (logistics != null && status == 6) { // 如果同意退款
                    logistics.setStatus(3); // 设置物流状态为已取消
                    logisticsMapper.updateById(logistics);
                    LOGGER.info("订单退款成功，同步更新物流状态为已取消，物流ID：{}", logistics.getId());
                }
                
                LOGGER.info("处理退款成功，订单ID：{}，处理结果：{}", id, status == 6 ? "已退款" : "拒绝退款");
                return ResultUtils.success(order);
            }
            LOGGER.warn("处理退款失败，订单ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "处理退款失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("处理退款异常，订单ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "处理退款失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理拼团订单退款的特殊逻辑
     * @param order 拼团订单
     */
    private void handleGroupBuyRefund(Order order) {
        try {
            LOGGER.info("开始处理拼团订单退款，订单ID：{}", order.getId());
            
            // 1. 查找该订单对应的拼团成员记录
            LambdaQueryWrapper<GroupBuyMember> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(GroupBuyMember::getOrderId, order.getId());
            GroupBuyMember member = groupBuyMemberMapper.selectOne(memberQuery);
            
            if (member == null) {
                LOGGER.warn("未找到拼团成员记录，订单ID：{}", order.getId());
                return;
            }
            
            // 2. 更新成员状态为已退款
            member.setStatus(2);  // 2-已退款
            member.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            groupBuyMemberMapper.updateById(member);
            LOGGER.info("更新拼团成员状态为已退款，成员ID：{}", member.getId());
            
            // 3. 查找对应的拼团
            GroupBuyTeam team = groupBuyTeamMapper.selectById(member.getTeamId());
            if (team == null) {
                LOGGER.warn("未找到拼团记录，团ID：{}", member.getTeamId());
                return;
            }
            
            // 4. 判断是团长还是团员
            if (member.getIsLeader() != null && member.getIsLeader() == 1) {
                // 团长退款 - 整个团失败
                LOGGER.info("团长申请退款，整个拼团将失败，团ID：{}", team.getId());
                
                // 更新团状态为失败
                team.setStatus(2);  // 2-拼团失败
                team.setFailReason("leader_refund");  // 团长退款
                team.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                groupBuyTeamMapper.updateById(team);
                
                // 查询该团的所有其他成员（不包括当前退款的团长）
                LambdaQueryWrapper<GroupBuyMember> otherMembersQuery = new LambdaQueryWrapper<>();
                otherMembersQuery.eq(GroupBuyMember::getTeamId, team.getId())
                                .ne(GroupBuyMember::getId, member.getId())
                                .eq(GroupBuyMember::getStatus, 1);  // 已支付的成员
                
                List<GroupBuyMember> otherMembers = groupBuyMemberMapper.selectList(otherMembersQuery);
                
                if (!otherMembers.isEmpty()) {
                    LOGGER.warn("团长退款导致拼团失败，需要为其他 {} 个成员退款", otherMembers.size());
                    // 注意：这里只是更新成员状态，实际退款由定时任务处理
                    // 或者可以手动触发退款流程
                }
                
            } else {
                // 团员退款 - 减少当前人数
                LOGGER.info("团员申请退款，更新拼团人数，团ID：{}", team.getId());
                
                if (team.getCurrentPeople() > 0) {
                    team.setCurrentPeople(team.getCurrentPeople() - 1);
                    
                    // ⭐ 如果团已经成功（status=1），但退款后人数不足，需要更新状态
                    if (team.getStatus() == 1 && team.getCurrentPeople() < team.getRequiredPeople()) {
                        // 人数不足，团失败
                        team.setStatus(2); // 2-拼团失败
                        team.setFailReason("member_refund"); // 成员退款导致失败
                        LOGGER.warn("团员退款导致团失败，团ID：{}，当前人数：{}/{}，状态更新为失败", 
                                team.getId(), team.getCurrentPeople(), team.getRequiredPeople());
                    }
                    
                    team.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    groupBuyTeamMapper.updateById(team);
                    LOGGER.info("拼团人数已减少，团ID：{}，剩余人数：{}/{}，状态：{}", 
                            team.getId(), team.getCurrentPeople(), team.getRequiredPeople(), team.getStatus());
                }
            }
            
            // 5. 恢复活动库存（和销量，如果团已经成功）
            if (order.getActivityId() != null) {
                GroupBuyActivity activity = groupBuyActivityMapper.selectById(order.getActivityId());
                if (activity != null) {
                    activity.setStock(activity.getStock() + order.getQuantity());
                    
                    // ⭐ 只有团已经成功(status=1)，才需要减少销量
                    // 如果团还在进行中或已失败，销量从未增加，不需要减少
                    if (team != null && team.getStatus() == 1) {
                        // 团已成功，需要减少活动销量
                        if (activity.getSalesCount() >= order.getQuantity()) {
                            activity.setSalesCount(activity.getSalesCount() - order.getQuantity());
                            LOGGER.info("拼团已成功，恢复活动销量，活动ID：{}，销量：-{}", 
                                    activity.getId(), order.getQuantity());
                        }
                        
                        // 同时减少商品销量
                        Product product = productMapper.selectById(activity.getProductId());
                        if (product != null && product.getSalesCount() >= order.getQuantity()) {
                            product.setSalesCount(product.getSalesCount() - order.getQuantity());
                            productMapper.updateById(product);
                            LOGGER.info("拼团已成功，恢复商品销量，商品ID：{}，销量：-{}", 
                                    product.getId(), order.getQuantity());
                        }
                    } else {
                        LOGGER.info("拼团未成功，不需要恢复销量，团ID：{}，团状态：{}", 
                                team != null ? team.getId() : "null", 
                                team != null ? team.getStatus() : "null");
                    }
                    
                    groupBuyActivityMapper.updateById(activity);
                    LOGGER.info("恢复拼团活动库存，活动ID：{}，数量：{}，剩余库存：{}", 
                            activity.getId(), order.getQuantity(), activity.getStock());
                }
            }
            
            LOGGER.info("拼团订单退款处理完成，订单ID：{}", order.getId());
            
        } catch (Exception e) {
            LOGGER.error("处理拼团订单退款失败，订单ID：" + order.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 从购物车创建订单（支付宝支付）
     * @param params 包含购物车ID列表、收货信息等
     * @return 返回商户订单号用于支付
     */
    @Transactional
    public Result<?> createOrdersFromCart(Map<String, Object> params) {
        try {
            // 解析参数
            @SuppressWarnings("unchecked")
            List<Integer> cartIds = (List<Integer>) params.get("cartIds");
            Long userId = Long.parseLong(params.get("userId").toString());
            String recvName = (String) params.get("recvName");
            String recvAddress = (String) params.get("recvAddress");
            String recvPhone = (String) params.get("recvPhone");
            
            if (cartIds == null || cartIds.isEmpty()) {
                LOGGER.warn("从购物车创建订单失败，购物车ID列表为空");
                return ResultUtils.paramError("购物车为空");
            }
            
            // 生成商户订单号（时间戳 + 随机数）
            String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(10000);
            
            int orderCount = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            // 遍历购物车项创建订单
            for (Integer cartId : cartIds) {
                Cart cart = cartMapper.selectById(cartId.longValue());
                if (cart == null) {
                    continue;
                }
                
                // 检查商品和库存
                Product product = productMapper.selectById(cart.getProductId());
                if (product == null) {
                    LOGGER.warn("从购物车创建订单失败，商品ID：{} 不存在", cart.getProductId());
                    return ResultUtils.dataNotFound("商品不存在：" + cart.getProductId());
                }
                if (product.getStock() < cart.getQuantity()) {
                    LOGGER.warn("从购物车创建订单失败，商品ID：{}，库存不足，需要：{}，库存：{}", 
                        cart.getProductId(), cart.getQuantity(), product.getStock());
                    return ResultUtils.stockInsufficient("商品库存不足：" + product.getName());
                }
                
                // 创建订单
                Order order = new Order();
                order.setUserId(userId);
                order.setProductId(cart.getProductId());
                order.setQuantity(cart.getQuantity());
                
                // 判断是否有折扣
                BigDecimal unitPrice;
                if (product.getIsDiscount() != null && product.getIsDiscount() == 1 
                    && product.getDiscountPrice() != null) {
                    unitPrice = product.getDiscountPrice();
                } else {
                    unitPrice = product.getPrice();
                }
                
                order.setPrice(unitPrice);
                order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
                order.setStatus(0);  // 待支付
                order.setLastStatus(0);
                order.setRecvName(recvName);
                order.setRecvAddress(recvAddress);
                order.setRecvPhone(recvPhone);
                order.setPayMethod("alipay");
                order.setOutTradeNo(outTradeNo);  // 所有订单使用同一个outTradeNo
                order.setRefundStatus(0);
                
                orderMapper.insert(order);
                
                orderCount++;
                totalAmount = totalAmount.add(order.getTotalPrice());
            }
            
            LOGGER.info("创建购物车订单成功，商户订单号：{}，订单数量：{}，总金额：{}", 
                    outTradeNo, orderCount, totalAmount);
            
            // 返回商户订单号和总金额
            Map<String, Object> result = new HashMap<>();
            result.put("outTradeNo", outTradeNo);
            result.put("totalAmount", totalAmount);
            result.put("orderCount", orderCount);
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("创建购物车订单异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 准备单个订单支付（支付宝支付）
     * @param orderId 订单ID
     * @return 返回商户订单号和支付金额
     */
    @Transactional
    public Result<?> prepareOrderPay(Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                LOGGER.warn("准备订单支付失败，订单ID：{} 不存在", orderId);
                return ResultUtils.dataNotFound("订单不存在");
            }
            
            // 检查订单状态，只有待支付的订单才能支付
            if (order.getStatus() != 0) {
                LOGGER.warn("准备订单支付失败，订单ID：{}，当前状态：{} 不允许支付", orderId, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
            }
            
            // 生成商户订单号（如果还没有）
            String outTradeNo = order.getOutTradeNo();
            if (StringUtils.isBlank(outTradeNo)) {
                outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(10000);
                order.setOutTradeNo(outTradeNo);
                orderMapper.updateById(order);
            }
            
            LOGGER.info("准备订单支付，订单ID：{}，商户订单号：{}，支付金额：{}", 
                    orderId, outTradeNo, order.getTotalPrice());
            
            // 返回商户订单号和支付金额
            Map<String, Object> result = new HashMap<>();
            result.put("outTradeNo", outTradeNo);
            result.put("totalAmount", order.getTotalPrice());
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("准备订单支付异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "准备支付失败：" + e.getMessage());
        }
    }
    
    /**
     * 余额支付（一键支付）- 直接完成支付，无需余额检查
     * 适用于普通订单
     * @param orderId 订单ID
     * @return 支付结果
     */
    @Transactional
    public Result<?> payByBalance(Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                LOGGER.warn("余额支付失败，订单ID：{} 不存在", orderId);
                return ResultUtils.dataNotFound("订单不存在");
            }
            
            // 检查订单状态，只有待支付的订单才能支付
            if (order.getStatus() != 0) {
                LOGGER.warn("余额支付失败，订单ID：{}，当前状态：{} 不允许支付", orderId, order.getStatus());
                return ResultUtils.error(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
            }
            
            // 检查是否为拼团订单
            boolean isGroupBuyOrder = (order.getOrderType() != null && order.getOrderType() == 1);
            
            if (isGroupBuyOrder) {
                // 拼团订单：更新订单状态和支付信息
                order.setStatus(1);  // 已支付
                order.setLastStatus(0);
                order.setPayMethod("balance");
                order.setPayTime(Timestamp.valueOf(LocalDateTime.now()));
                orderMapper.updateById(order);
                
                // 调用拼团服务处理拼团逻辑（创建团或加入团）
                groupBuyService.handlePaymentSuccess(order.getId());
                
                LOGGER.info("[拼团订单余额支付成功] 订单ID：{}，状态已更新", order.getId());
            } else {
                // 普通订单：更新订单状态并扣减库存
                order.setStatus(1);  // 已支付
                order.setLastStatus(0);
                order.setPayMethod("balance");
                order.setPayTime(Timestamp.valueOf(LocalDateTime.now()));
                
                orderMapper.updateById(order);
                
                // 扣减库存，增加销量
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    if (product.getStock() >= order.getQuantity()) {
                        product.setStock(product.getStock() - order.getQuantity());
                        product.setSalesCount(product.getSalesCount() + order.getQuantity());
                        productMapper.updateById(product);
                        
                        LOGGER.info("[库存扣减] 商品ID：{}，扣减数量：{}，剩余库存：{}", 
                                product.getId(), order.getQuantity(), product.getStock());
                    } else {
                        LOGGER.warn("余额支付失败，订单ID：{}，商品ID：{}，库存不足", orderId, order.getProductId());
                        return ResultUtils.stockInsufficient();
                    }
                }
                
                // 清理购物车（通过用户ID和商品ID查找）
                LambdaQueryWrapper<Cart> cartQueryWrapper = new LambdaQueryWrapper<>();
                cartQueryWrapper.eq(Cart::getUserId, order.getUserId())
                               .eq(Cart::getProductId, order.getProductId());
                cartMapper.delete(cartQueryWrapper);
                
                LOGGER.info("[普通订单余额支付成功] 订单ID：{}", order.getId());
            }
            
            return ResultUtils.success(order);
        } catch (Exception e) {
            LOGGER.error("余额支付异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "支付失败：" + e.getMessage());
        }
    }
    
    /**
     * 余额支付（一键支付）- 批量支付购物车订单
     * @param params 包含商户订单号的参数
     * @return 支付结果
     */
    @Transactional
    public Result<?> payCartByBalance(Map<String, Object> params) {
        String outTradeNo = null;
        try {
            outTradeNo = (String) params.get("outTradeNo");
            if (StringUtils.isBlank(outTradeNo)) {
                LOGGER.warn("批量余额支付失败，商户订单号为空");
                return ResultUtils.paramError("商户订单号不能为空");
            }
            
            // 通过outTradeNo查询所有相关订单
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOutTradeNo, outTradeNo);
            List<Order> orders = orderMapper.selectList(queryWrapper);
            
            if (orders == null || orders.isEmpty()) {
                LOGGER.warn("批量余额支付失败，商户订单号：{}，未找到相关订单", outTradeNo);
                return ResultUtils.dataNotFound("未找到相关订单");
            }
            
            int successCount = 0;
            for (Order order : orders) {
                if (order.getStatus() == 0) {  // 只处理待支付状态的订单
                    // 更新订单状态
                    order.setStatus(1);  // 已支付
                    order.setLastStatus(0);
                    order.setPayMethod("balance");
                    order.setPayTime(Timestamp.valueOf(LocalDateTime.now()));
                    orderMapper.updateById(order);
                    
                    // 扣减库存，增加销量
                    Product product = productMapper.selectById(order.getProductId());
                    if (product != null) {
                        if (product.getStock() >= order.getQuantity()) {
                            product.setStock(product.getStock() - order.getQuantity());
                            product.setSalesCount(product.getSalesCount() + order.getQuantity());
                            productMapper.updateById(product);
                            
                            LOGGER.info("[库存扣减] 商品ID：{}，扣减数量：{}，剩余库存：{}", 
                                    product.getId(), order.getQuantity(), product.getStock());
                        } else {
                            LOGGER.error("[库存不足] 商品ID：{}，需要：{}，库存：{}", 
                                    product.getId(), order.getQuantity(), product.getStock());
                            continue;
                        }
                    }
                    
                    // 清理购物车
                    LambdaQueryWrapper<Cart> cartQueryWrapper = new LambdaQueryWrapper<>();
                    cartQueryWrapper.eq(Cart::getUserId, order.getUserId())
                                   .eq(Cart::getProductId, order.getProductId());
                    cartMapper.delete(cartQueryWrapper);
                    
                    successCount++;
                }
            }
            
            LOGGER.info("[购物车余额支付成功] 商户订单号：{}，支付订单数：{}", outTradeNo, successCount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", orders.size());
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("购物车余额支付异常，商户订单号：{}，错误：{}", outTradeNo, e.getMessage(), e);
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "支付失败：" + e.getMessage());
        }
    }
} 