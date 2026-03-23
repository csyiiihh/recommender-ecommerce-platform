package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.StockIn;
import org.example.springboot.entity.StockOut;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.mapper.StockInMapper;
import org.example.springboot.mapper.StockOutMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockService.class);

    @Autowired
    private StockInMapper stockInMapper;

    @Autowired
    private StockOutMapper stockOutMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public Result<?> createStockIn(StockIn stockIn) {
        try {
            // 检查商品是否存在
            Product product = productMapper.selectById(stockIn.getProductId());
            if (product == null) {
                LOGGER.warn("创建入库记录失败，商品ID：{} 不存在", stockIn.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }

            // 验证数量
            if (stockIn.getQuantity() == null || stockIn.getQuantity() <= 0) {
                LOGGER.warn("创建入库记录失败，数量不合法：{}", stockIn.getQuantity());
                return ResultUtils.paramError("入库数量必须大于0");
            }

            // 验证单价
            if (stockIn.getUnitPrice() == null || stockIn.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                LOGGER.warn("创建入库记录失败，单价不合法：{}", stockIn.getUnitPrice());
                return ResultUtils.paramError("单价必须大于0");
            }

            // 计算总价
            stockIn.setTotalPrice(stockIn.getUnitPrice().multiply(new BigDecimal(stockIn.getQuantity())));

            // 更新商品库存
            product.setStock(product.getStock() + stockIn.getQuantity());
            productMapper.updateById(product);

            // 保存入库记录
            int result = stockInMapper.insert(stockIn);
            if (result > 0) {
                LOGGER.info("创建入库记录成功，入库ID：{}，商品ID：{}，数量：{}，单价：{}", 
                    stockIn.getId(), stockIn.getProductId(), stockIn.getQuantity(), stockIn.getUnitPrice());
                return ResultUtils.success(stockIn);
            }
            LOGGER.warn("创建入库记录失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建入库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建入库记录异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建入库记录失败：" + e.getMessage());
        }
    }

    @Transactional
    public Result<?> createStockOut(StockOut stockOut) {
        try {
            // 检查商品是否存在
            Product product = productMapper.selectById(stockOut.getProductId());
            if (product == null) {
                LOGGER.warn("创建出库记录失败，商品ID：{} 不存在", stockOut.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }

            // 验证数量
            if (stockOut.getQuantity() == null || stockOut.getQuantity() <= 0) {
                LOGGER.warn("创建出库记录失败，数量不合法：{}", stockOut.getQuantity());
                return ResultUtils.paramError("出库数量必须大于0");
            }

            // 检查库存是否足够
            if (product.getStock() == null || product.getStock() < stockOut.getQuantity()) {
                LOGGER.warn("创建出库记录失败，商品ID：{}，当前库存：{}，出库数量：{}，库存不足", 
                    stockOut.getProductId(), product.getStock(), stockOut.getQuantity());
                return ResultUtils.stockInsufficient("库存不足，当前库存：" + product.getStock() + "，需要：" + stockOut.getQuantity());
            }

            // 验证单价
            if (stockOut.getUnitPrice() == null || stockOut.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                LOGGER.warn("创建出库记录失败，单价不合法：{}", stockOut.getUnitPrice());
                return ResultUtils.paramError("单价必须大于0");
            }

            // 计算总价
            stockOut.setTotalPrice(stockOut.getUnitPrice().multiply(new BigDecimal(stockOut.getQuantity())));

            // 更新商品库存
            product.setStock(product.getStock() - stockOut.getQuantity());
            productMapper.updateById(product);

            // 保存出库记录
            int result = stockOutMapper.insert(stockOut);
            if (result > 0) {
                LOGGER.info("创建出库记录成功，出库ID：{}，商品ID：{}，数量：{}，单价：{}", 
                    stockOut.getId(), stockOut.getProductId(), stockOut.getQuantity(), stockOut.getUnitPrice());
                return ResultUtils.success(stockOut);
            }
            LOGGER.warn("创建出库记录失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建出库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建出库记录异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建出库记录失败：" + e.getMessage());
        }
    }

    // 获取入库记录列表
    public Page<StockIn> getStockInList(Long productId, String supplier, Integer status, Long operatorId,
                                      Integer currentPage, Integer size) {
        LambdaQueryWrapper<StockIn> queryWrapper = new LambdaQueryWrapper<>();
        
        if (productId != null) {
            queryWrapper.eq(StockIn::getProductId, productId);
        }
        if (supplier != null) {
            queryWrapper.like(StockIn::getSupplier, supplier);
        }
        if (status != null) {
            queryWrapper.eq(StockIn::getStatus, status);
        }
        if (operatorId != null) {
            queryWrapper.eq(StockIn::getOperatorId, operatorId);
        }

        queryWrapper.orderByDesc(StockIn::getId);

        Page<StockIn> page = new Page<>(currentPage, size);
        Page<StockIn> result = stockInMapper.selectPage(page, queryWrapper);

        // 填充关联信息
        result.getRecords().forEach(stockIn -> {
            stockIn.setProduct(productMapper.selectById(stockIn.getProductId()));
            stockIn.setOperator(userMapper.selectById(stockIn.getOperatorId()));
        });

        return result;
    }

    // 获取出库记录列表
    public Page<StockOut> getStockOutList(Long productId, Integer type, Integer status, Long operatorId,
                                        String customerName, String orderNo,
                                        Integer currentPage, Integer size) {
        LambdaQueryWrapper<StockOut> queryWrapper = new LambdaQueryWrapper<>();
        
        if (productId != null) {
            queryWrapper.eq(StockOut::getProductId, productId);
        }
        if (type != null) {
            queryWrapper.eq(StockOut::getType, type);
        }
        if (status != null) {
            queryWrapper.eq(StockOut::getStatus, status);
        }
        if (operatorId != null) {
            queryWrapper.eq(StockOut::getOperatorId, operatorId);
        }
        if (customerName != null) {
            queryWrapper.like(StockOut::getCustomerName, customerName);
        }
        if (orderNo != null) {
            queryWrapper.eq(StockOut::getOrderNo, orderNo);
        }

        queryWrapper.orderByDesc(StockOut::getId);

        Page<StockOut> page = new Page<>(currentPage, size);
        Page<StockOut> result = stockOutMapper.selectPage(page, queryWrapper);

        // 填充关联信息
        result.getRecords().forEach(stockOut -> {
            stockOut.setProduct(productMapper.selectById(stockOut.getProductId()));
            stockOut.setOperator(userMapper.selectById(stockOut.getOperatorId()));
        });

        return result;
    }

    // 作废入库记录
    @Transactional
    public Result<?> invalidateStockIn(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            LOGGER.warn("作废入库记录失败，入库ID：{} 不存在", id);
            return ResultUtils.dataNotFound("入库记录不存在");
        }

        if (stockIn.getStatus() != null && stockIn.getStatus() == 0) {
            LOGGER.warn("作废入库记录失败，入库ID：{}，该记录已作废", id);
            return ResultUtils.businessError("该记录已作废");
        }
        
        try {
            // 更新商品库存
            Product product = productMapper.selectById(stockIn.getProductId());
            if (product != null) {
                int newStock = product.getStock() - stockIn.getQuantity();
                if (newStock < 0) {
                    LOGGER.warn("作废入库记录失败，入库ID：{}，商品ID：{}，当前库存：{}，入库数量：{}，库存不足", 
                        id, stockIn.getProductId(), product.getStock(), stockIn.getQuantity());
                    return ResultUtils.stockInsufficient("作废入库记录会导致库存为负数");
                }
                product.setStock(newStock);
                productMapper.updateById(product);
                LOGGER.debug("作废入库记录，更新商品库存，商品ID：{}，原库存：{}，新库存：{}", 
                    stockIn.getProductId(), product.getStock() + stockIn.getQuantity(), newStock);
            }

            // 更新入库记录状态
            stockIn.setStatus(0);
            int result = stockInMapper.updateById(stockIn);
            if (result > 0) {
                LOGGER.info("作废入库记录成功，入库ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("作废入库记录失败，入库ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "作废入库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("作废入库记录异常，入库ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "作废入库记录失败：" + e.getMessage());
        }
    }

    // 作废出库记录
    @Transactional
    public Result<?> invalidateStockOut(Long id) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            LOGGER.warn("作废出库记录失败，出库ID：{} 不存在", id);
            return ResultUtils.dataNotFound("出库记录不存在");
        }

        if (stockOut.getStatus() != null && stockOut.getStatus() == 0) {
            LOGGER.warn("作废出库记录失败，出库ID：{}，该记录已作废", id);
            return ResultUtils.businessError("该记录已作废");
        }
        
        try {
            // 更新商品库存
            Product product = productMapper.selectById(stockOut.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + stockOut.getQuantity());
                productMapper.updateById(product);
                LOGGER.debug("作废出库记录，恢复商品库存，商品ID：{}，原库存：{}，新库存：{}", 
                    stockOut.getProductId(), product.getStock() - stockOut.getQuantity(), product.getStock());
            }

            // 更新出库记录状态
            stockOut.setStatus(0);
            int result = stockOutMapper.updateById(stockOut);
            if (result > 0) {
                LOGGER.info("作废出库记录成功，出库ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("作废出库记录失败，出库ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "作废出库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("作废出库记录异常，出库ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "作废出库记录失败：" + e.getMessage());
        }
    }

    // 删除入库记录
    @Transactional
    public Result<?> deleteStockIn(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            LOGGER.warn("删除入库记录失败，入库ID：{} 不存在", id);
            return ResultUtils.dataNotFound("入库记录不存在");
        }
        
        try {
            // 如果记录状态为正常，需要先作废（减少库存）
            if (stockIn.getStatus() != null && stockIn.getStatus() == 1) {
                Result<?> invalidateResult = invalidateStockIn(id);
                if (!ResultCode.SUCCESS.getCode().equals(invalidateResult.getCode())) {
                    return invalidateResult;
                }
            }

            // 删除记录
            int result = stockInMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除入库记录成功，入库ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除入库记录失败，入库ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除入库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除入库记录异常，入库ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除入库记录失败：" + e.getMessage());
        }
    }

    // 删除出库记录
    @Transactional
    public Result<?> deleteStockOut(Long id) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            LOGGER.warn("删除出库记录失败，出库ID：{} 不存在", id);
            return ResultUtils.dataNotFound("出库记录不存在");
        }
        
        try {
            // 如果记录状态为正常，需要先作废（恢复库存）
            if (stockOut.getStatus() != null && stockOut.getStatus() == 1) {
                Result<?> invalidateResult = invalidateStockOut(id);
                if (!ResultCode.SUCCESS.getCode().equals(invalidateResult.getCode())) {
                    return invalidateResult;
                }
            }

            // 删除记录
            int result = stockOutMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除出库记录成功，出库ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除出库记录失败，出库ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除出库记录失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除出库记录异常，出库ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除出库记录失败：" + e.getMessage());
        }
    }
} 