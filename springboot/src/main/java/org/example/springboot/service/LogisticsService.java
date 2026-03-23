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
    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsService.class);

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProductMapper productMapper;

    public Result<?> createLogistics(Logistics logistics) {
        try {
            // 检查订单是否存在
            Order order = orderMapper.selectById(logistics.getOrderId());
            if (order == null) {
                LOGGER.warn("创建物流信息失败，订单ID：{} 不存在", logistics.getOrderId());
                return ResultUtils.dataNotFound("订单不存在");
            }

            // 检查订单状态是否为已支付
            if (order.getStatus() == null || order.getStatus() != 1) {
                LOGGER.warn("创建物流信息失败，订单ID：{}，订单状态：{}，只能为已支付订单创建物流", 
                    logistics.getOrderId(), order.getStatus());
                return ResultUtils.businessError("订单状态不正确，只能为已支付订单创建物流");
            }

            // 检查必填字段
            if (logistics.getCompanyName() == null || logistics.getCompanyName().trim().isEmpty()) {
                LOGGER.warn("创建物流信息失败，物流公司名称不能为空");
                return ResultUtils.paramError("物流公司名称不能为空");
            }
            if (logistics.getReceiverName() == null || logistics.getReceiverName().trim().isEmpty()) {
                LOGGER.warn("创建物流信息失败，收货人姓名不能为空");
                return ResultUtils.paramError("收货人姓名不能为空");
            }
            if (logistics.getReceiverPhone() == null || logistics.getReceiverPhone().trim().isEmpty()) {
                LOGGER.warn("创建物流信息失败，收货人电话不能为空");
                return ResultUtils.paramError("收货人电话不能为空");
            }
            if (logistics.getReceiverAddress() == null || logistics.getReceiverAddress().trim().isEmpty()) {
                LOGGER.warn("创建物流信息失败，收货地址不能为空");
                return ResultUtils.paramError("收货地址不能为空");
            }
            logistics.setStatus(1);

            int result = logisticsMapper.insert(logistics);
            if (result > 0) {
                // 更新订单状态为已发货
                order.setStatus(2); // 2表示已发货
                orderMapper.updateById(order);
                
                LOGGER.info("创建物流信息成功，物流ID：{}，订单ID：{}，物流公司：{}", 
                    logistics.getId(), logistics.getOrderId(), logistics.getCompanyName());
                return ResultUtils.success(logistics);
            }
            LOGGER.warn("创建物流信息失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建物流信息失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建物流信息异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建物流信息失败：" + e.getMessage());
        }
    }

    public Result<?> updateLogisticsStatus(Long id, Integer status) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            LOGGER.warn("更新物流状态失败，物流ID：{} 不存在", id);
            return ResultUtils.dataNotFound("物流信息不存在");
        }
        
        if (status == null) {
            LOGGER.warn("更新物流状态失败，状态不能为空");
            return ResultUtils.paramError("状态不能为空");
        }
        
        try {
            logistics.setStatus(status);
            int result = logisticsMapper.updateById(logistics);
            if (result > 0) {
                // 根据物流状态更新订单状态
                Order order = orderMapper.selectById(logistics.getOrderId());
                if (order != null) {
                    switch (status) {
                        case 0: // 待发货
                            order.setStatus(1); // 已支付，待发货
                            break;
                        case 1: // 已发货
                            order.setStatus(2); // 已发货
                            break;
                        case 2: // 已签收
                            order.setStatus(3); // 已完成
                            break;
                        case 3: // 已取消
                            order.setStatus(4); // 已取消
                            break;
                    }
                    orderMapper.updateById(order);
                    LOGGER.debug("同步更新订单状态，订单ID：{}，新状态：{}", order.getId(), order.getStatus());
                }

                LOGGER.info("更新物流状态成功，物流ID：{}，订单ID：{}，新状态：{}", 
                    id, logistics.getOrderId(), status);
                return ResultUtils.success(logistics);
            }
            LOGGER.warn("更新物流状态失败，物流ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新物流状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新物流状态异常，物流ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新物流状态失败：" + e.getMessage());
        }
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

    public Result<?> signLogistics(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            LOGGER.warn("物流签收失败，物流ID：{} 不存在", id);
            return ResultUtils.dataNotFound("物流信息不存在");
        }

        // 检查当前状态是否为已发货
        if (logistics.getStatus() == null || logistics.getStatus() != 1) {
            LOGGER.warn("物流签收失败，物流ID：{}，当前状态：{}，不允许签收", id, logistics.getStatus());
            return ResultUtils.businessError("当前物流状态不允许签收，只能签收已发货的物流");
        }
        
        try {
            // 更新物流状态为已签收
            logistics.setStatus(2); // 2表示已签收
            int result = logisticsMapper.updateById(logistics);
            
            if (result > 0) {
                // 更新订单状态为已完成
                Order order = orderMapper.selectById(logistics.getOrderId());
                if (order != null) {
                    order.setStatus(3); // 3表示已完成
                    orderMapper.updateById(order);
                    LOGGER.debug("同步更新订单状态为已完成，订单ID：{}", order.getId());
                }

                LOGGER.info("物流签收成功，物流ID：{}，订单ID：{}", id, logistics.getOrderId());
                return ResultUtils.success(logistics);
            }
            LOGGER.warn("物流签收失败，物流ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "物流签收失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("物流签收异常，物流ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "物流签收失败：" + e.getMessage());
        }
    }
} 