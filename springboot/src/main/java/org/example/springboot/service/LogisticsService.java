package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Logistics;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogisticsService {
    @Autowired private FulfillmentPolicy fulfillment;

    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsService.class);

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProductMapper productMapper;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<?> createLogistics(Logistics logistics) {
        if (logistics.getOrderId() == null) throw new BusinessException("订单不能为空");
        Order order = orderMapper.selectForUpdate(logistics.getOrderId());
        fulfillment.requireShipping(order);
        Long existing = logisticsMapper.selectCount(new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId, order.getId()));
        if (existing != null && existing > 0) throw new BusinessException("订单已有物流信息，请勿重复发货");
        if (blank(logistics.getCompanyName()) || blank(logistics.getReceiverName()) || blank(logistics.getReceiverPhone()) || blank(logistics.getReceiverAddress())) {
            throw new BusinessException("物流公司和收货信息不能为空");
        }
        logistics.setId(null);
        logistics.setStatus(1);
        if (logisticsMapper.insert(logistics) != 1) throw new BusinessException("创建物流信息失败");
        changeOrder(order, 2);
        return ResultUtils.success(logistics);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<?> updateLogisticsStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 3) throw new BusinessException("无效物流状态");
        LockedShipment locked = lockShipment(id);
        Order order = locked.order();
        Logistics logistics = locked.logistics();
        int state = fulfillment.fulfillmentStatus(order);
        if (state != 1 && state != 2 && state != 3) throw new BusinessException("订单已关闭或正在退款，不能修改物流状态");
        Integer old = logistics.getStatus();
        if (status.equals(old)) {
            boolean consistent = (status == 0 && state == 1) || (status == 1 && state == 2) || (status == 2 && state == 3);
            if (!consistent) throw new BusinessException("订单与物流状态不一致，请人工核对");
            return ResultUtils.success(logistics);
        }
        int orderStatus;
        if (Integer.valueOf(0).equals(old) && status == 1) {
            fulfillment.requireShipping(order);
            orderStatus = 2;
        } else if (Integer.valueOf(1).equals(old) && status == 2) {
            fulfillment.requireReceipt(order);
            orderStatus = 3;
        } else {
            throw new BusinessException("不允许回退或跳过物流状态，取消交易请使用退款或售后流程");
        }
        logistics.setStatus(status);
        if (logisticsMapper.updateById(logistics) != 1) throw new BusinessException("更新物流状态失败");
        changeOrder(order, orderStatus);
        return ResultUtils.success(logistics);
    }

    public Result<?> deleteLogistics(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            LOGGER.warn("删除物流信息失败，物流ID：{} 不存在", id);
            return ResultUtils.dataNotFound("物流信息不存在");
        }

        // 检查物流状态，只有未发货或已取消的物流信息才能删除
        if (logistics.getStatus() != null && logistics.getStatus() != 0 && logistics.getStatus() != 3) {
            LOGGER.warn("删除物流信息失败，物流ID：{}，当前状态：{}，不允许删除", id, logistics.getStatus());
            return ResultUtils.businessError("当前物流状态不允许删除，只能删除未发货或已取消的物流信息");
        }
        
        try {
            int result = logisticsMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除物流信息成功，物流ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除物流信息失败，物流ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除物流信息失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除物流信息异常，物流ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除物流信息失败：" + e.getMessage());
        }
    }

    public Result<?> getLogisticsById(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            LOGGER.warn("查询物流信息失败，物流ID：{} 不存在", id);
            return ResultUtils.dataNotFound("物流信息不存在");
        }
        
        // 填充关联信息
        logistics.setOrder(orderMapper.selectById(logistics.getOrderId()));
        return ResultUtils.success(logistics);
    }

    public Result<?> getLogisticsByOrderId(Long orderId) {
        try {
            LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Logistics::getOrderId, orderId);
            Logistics logistics = logisticsMapper.selectOne(queryWrapper);
            if (logistics == null) {
                LOGGER.info("查询订单物流信息，订单ID：{}，结果为空", orderId);
                return ResultUtils.dataNotFound("该订单暂无物流信息");
            }
            
            // 填充关联信息
            logistics.setOrder(orderMapper.selectById(logistics.getOrderId()));
            return ResultUtils.success(logistics);
        } catch (Exception e) {
            LOGGER.error("查询订单物流信息异常，订单ID：{}，错误：{}", orderId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询物流信息失败");
        }
    }

    public Result<?> getLogisticsByPage(Long orderId, Long merchantId, Integer status, Integer currentPage, Integer size) {
        try {
            // 创建分页对象
            Page<Logistics> page = new Page<>(currentPage, size);
            
            // 创建查询构造器
            LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
            
            // 基础条件
            queryWrapper.eq(orderId != null, Logistics::getOrderId, orderId);
            queryWrapper.eq(status != null, Logistics::getStatus, status);
            
            if (merchantId != null) {
                // 先查询该商户的所有商品ID
                LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
                productWrapper.eq(Product::getMerchantId, merchantId);
                List<Product> products = productMapper.selectList(productWrapper);
                List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
                
                if (!productIds.isEmpty()) {
                    // 查询包含这些商品的订单ID
                    LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
                    orderWrapper.in(Order::getProductId, productIds);
                    List<Order> orders = orderMapper.selectList(orderWrapper);
                    List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
                    
                    if (!orderIds.isEmpty()) {
                        queryWrapper.in(Logistics::getOrderId, orderIds);
                    } else {
                        // 如果没有找到相关订单，直接返回空结果
                        Page<Logistics> emptyPage = new Page<>(currentPage, size);
                        emptyPage.setTotal(0);
                        return ResultUtils.success(emptyPage);
                    }
                } else {
                    // 如果没有找到该商户的商品，直接返回空结果
                    Page<Logistics> emptyPage = new Page<>(currentPage, size);
                    emptyPage.setTotal(0);
                    return ResultUtils.success(emptyPage);
                }
            }

            // 按创建时间倒序排序
            queryWrapper.orderByDesc(Logistics::getCreatedAt);

            // 执行分页查询
            Page<Logistics> result = logisticsMapper.selectPage(page, queryWrapper);

            // 填充关联信息
            result.getRecords().forEach(logistics -> {
                Order order = orderMapper.selectById(logistics.getOrderId());
                if (order != null) {
                    logistics.setOrder(order);
                    // 可以选择性地填充产品信息
                    order.setProduct(productMapper.selectById(order.getProductId()));
                }
            });

            LOGGER.debug("分页查询物流信息，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询物流信息异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "分页查询物流信息失败");
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除物流信息失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个物流信息的状态
            for (Long id : ids) {
                Logistics logistics = logisticsMapper.selectById(id);
                if (logistics == null) {
                    LOGGER.warn("批量删除物流信息，物流ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("物流ID：" + id + " 不存在");
                }
                if (logistics.getStatus() != null && logistics.getStatus() != 0 && logistics.getStatus() != 3) {
                    LOGGER.warn("批量删除物流信息失败，物流ID：{}，当前状态：{}，不允许删除", id, logistics.getStatus());
                    return ResultUtils.businessError("物流ID：" + id + " 当前状态不允许删除，只能删除未发货或已取消的物流信息");
                }
            }

            LambdaQueryWrapper<Logistics> wrapper = Wrappers.lambdaQuery();
            wrapper.in(Logistics::getId, ids);
            int result = logisticsMapper.delete(wrapper);
            if (result > 0) {
                LOGGER.info("批量删除物流信息成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除物流信息失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除物流信息失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除物流信息异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除物流信息失败：" + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<?> signLogistics(Long id) {
        return updateLogisticsStatus(id, 2);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private record LockedShipment(Order order, Logistics logistics) { }
    private LockedShipment lockShipment(Long id) {
        Logistics snapshot = logisticsMapper.selectById(id);
        if (snapshot == null) throw new BusinessException("物流信息不存在");
        // All writers lock the order before the logistics row, including refund and receipt.
        Order order = orderMapper.selectForUpdate(snapshot.getOrderId());
        if (order == null) throw new BusinessException("订单不存在");
        Logistics current = logisticsMapper.selectForUpdate(id);
        if (current == null || !java.util.Objects.equals(current.getOrderId(), order.getId())) throw new BusinessException("物流信息已变化，请刷新重试");
        return new LockedShipment(order, current);
    }
    private void changeOrder(Order order, int status) {
        order.setLastStatus(order.getStatus());
        order.setStatus(status);
        if (orderMapper.updateById(order) != 1) throw new BusinessException("同步订单状态失败");
    }
} 