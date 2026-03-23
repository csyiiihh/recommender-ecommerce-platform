package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.CarouselItem;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.CarouselItemMapper;
import org.example.springboot.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarouselItemService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarouselItemService.class);

    @Resource
    private CarouselItemMapper carouselItemMapper;

    @Resource
    private ProductMapper productMapper;

    public Result<?> createCarouselItem(CarouselItem carouselItem) {
        // 检查商品是否存在
        if (carouselItem.getProductId() != null && carouselItem.getProductId() != 0) {
            Product product = productMapper.selectById(carouselItem.getProductId());
            if (product == null) {
                LOGGER.warn("创建轮播图失败，商品ID：{} 不存在", carouselItem.getProductId());
                return ResultUtils.dataNotFound("关联的商品不存在");
            }
        }
        
        try {
            int result = carouselItemMapper.insert(carouselItem);
            if (result > 0) {
                LOGGER.info("创建轮播图成功，轮播图ID：{}，商品ID：{}", 
                    carouselItem.getId(), carouselItem.getProductId());
                return ResultUtils.success(carouselItem);
            }
            LOGGER.warn("创建轮播图失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建轮播图失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建轮播图异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建轮播图失败：" + e.getMessage());
        }
    }

    public Result<?> updateCarouselItem(Long id, CarouselItem carouselItem) {
        CarouselItem existing = carouselItemMapper.selectById(id);
        if (existing == null) {
            LOGGER.warn("更新轮播图失败，轮播图ID：{} 不存在", id);
            return ResultUtils.dataNotFound("轮播图不存在");
        }

        // 检查商品是否存在
        if (carouselItem.getProductId() != null && carouselItem.getProductId() != 0) {
            Product product = productMapper.selectById(carouselItem.getProductId());
            if (product == null) {
                LOGGER.warn("更新轮播图失败，商品ID：{} 不存在", carouselItem.getProductId());
                return ResultUtils.dataNotFound("关联的商品不存在");
            }
        }

        carouselItem.setId(id);
        try {
            int result = carouselItemMapper.updateById(carouselItem);
            if (result > 0) {
                LOGGER.info("更新轮播图成功，轮播图ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("更新轮播图失败，轮播图ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新轮播图失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新轮播图异常，轮播图ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新轮播图失败：" + e.getMessage());
        }
    }

    public Result<?> deleteCarouselItem(Long id) {
        // 检查轮播图是否存在
        CarouselItem existing = carouselItemMapper.selectById(id);
        if (existing == null) {
            LOGGER.warn("删除轮播图失败，轮播图ID：{} 不存在", id);
            return ResultUtils.dataNotFound("轮播图不存在");
        }
        
        try {
            int result = carouselItemMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除轮播图成功，轮播图ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除轮播图失败，轮播图ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除轮播图失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除轮播图异常，轮播图ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除轮播图失败：" + e.getMessage());
        }
    }

    public Result<?> getCarouselItemById(Long id) {
        CarouselItem item = carouselItemMapper.selectById(id);
        if (item == null) {
            LOGGER.warn("查询轮播图失败，轮播图ID：{} 不存在", id);
            return ResultUtils.dataNotFound("轮播图不存在");
        }
        
        if (item.getProductId() != null && item.getProductId() != 0) {
            item.setProduct(productMapper.selectById(item.getProductId()));
        }
        return ResultUtils.success(item);
    }

    public Result<?> getActiveCarouselItems() {
        try {
            LambdaQueryWrapper<CarouselItem> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CarouselItem::getStatus, 1)
                       .orderByAsc(CarouselItem::getSortOrder);
            List<CarouselItem> items = carouselItemMapper.selectList(queryWrapper);
            
            if (items == null || items.isEmpty()) {
                return Result.success(items);
            }
            
            // 优化：批量查询商品信息，避免 N+1 查询
            Set<Long> productIds = items.stream()
                .filter(item -> item.getProductId() != null && item.getProductId() != 0)
                .map(CarouselItem::getProductId)
                .collect(Collectors.toSet());
            
            Map<Long, Product> productMap = productIds.isEmpty() ? 
                new java.util.HashMap<>() : 
                productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));
            
            // 填充商品信息（内存操作）
            for (CarouselItem item : items) {
                if (item.getProductId() != null && item.getProductId() != 0) {
                    item.setProduct(productMap.get(item.getProductId()));
                }
            }
            
            LOGGER.debug("获取活跃轮播图，数量：{}", items.size());
            return ResultUtils.success(items);
        } catch (Exception e) {
            LOGGER.error("获取轮播图异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取轮播图失败：" + e.getMessage());
        }
    }

    public Result<?> getCarouselItemsByPage(Integer currentPage, Integer size) {
        try {
            Page<CarouselItem> page = new Page<>(currentPage, size);
            Page<CarouselItem> result = carouselItemMapper.selectPage(page, 
                new LambdaQueryWrapper<CarouselItem>().orderByAsc(CarouselItem::getSortOrder));
            
            if (result.getRecords().isEmpty()) {
                return Result.success(result);
            }
            
            // 优化：批量查询商品信息，避免 N+1 查询
            Set<Long> productIds = result.getRecords().stream()
                .filter(item -> item.getProductId() != null && item.getProductId() != 0)
                .map(CarouselItem::getProductId)
                .collect(Collectors.toSet());
            
            Map<Long, Product> productMap = productIds.isEmpty() ? 
                new java.util.HashMap<>() : 
                productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));
            
            // 填充商品信息（内存操作）
            for (CarouselItem item : result.getRecords()) {
                if (item.getProductId() != null && item.getProductId() != 0) {
                    item.setProduct(productMap.get(item.getProductId()));
                }
            }
            
            LOGGER.debug("分页查询轮播图，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询轮播图异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取轮播图失败：" + e.getMessage());
        }
    }
} 