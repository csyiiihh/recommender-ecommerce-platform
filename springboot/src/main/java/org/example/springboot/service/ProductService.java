package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.entity.*;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private CarouselItemMapper carouselItemMapper;

    @Resource
    private StockInMapper stockInMapper;
    @Resource
    private StockOutMapper stockOutMapper;
    @Caching(evict = {
            @CacheEvict(value = "productPages",allEntries = true),
            @CacheEvict(value="products",allEntries = true)
    })
    public Result<?> createProduct(Product product) {
        try {
            int result = productMapper.insert(product);
            if (result > 0) {
                LOGGER.info("创建商品成功，商品ID：{}，商品名称：{}", product.getId(), product.getName());
                return ResultUtils.success(product);
            }
            LOGGER.warn("创建商品失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建商品失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建商品异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建商品失败：" + e.getMessage());
        }
    }
@Caching(evict = {
        @CacheEvict(value = "productPages",allEntries = true),
        @CacheEvict(value="products",allEntries = true)
})
    public Result<?> updateProduct(Long id, Product product) {
        // 检查商品是否存在
        Product oldProduct = productMapper.selectById(id);
        if (oldProduct == null) {
            LOGGER.warn("更新商品失败，商品ID：{} 不存在", id);
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        // 处理图片更新
        String oldImg = oldProduct.getImageUrl();
        String newImg = product.getImageUrl();
        // 只有当旧图片和新图片都不为空且不相等时才删除旧图片
        if (oldImg != null && newImg != null && !oldImg.equals(newImg)) {
            fileService.fileRemove(oldImg);
        }
        
        product.setId(id);
        try {
            // 检查库存是否合法
            if (product.getStock() != null && product.getStock() < 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "库存不能为负数");
            }

            int result = productMapper.updateById(product);
            if (result > 0) {
                LOGGER.info("更新商品成功，商品ID：{}，商品名称：{}", id, product.getName());
                return ResultUtils.success(product);
            }
            LOGGER.warn("更新商品失败，商品ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新商品失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新商品异常，商品ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新商品失败：" + e.getMessage());
        }
    }
    @Caching(evict = {
            @CacheEvict(value = "productPages",allEntries = true),
            @CacheEvict(value="products",allEntries = true)
    })
    public Result<?> deleteProduct(Long id) {
        // 检查商品是否存在
        Product product = productMapper.selectById(id);
        if (product == null) {
            LOGGER.warn("删除商品失败，商品ID：{} 不存在", id);
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        try {
            // 检查是否存在关联轮播图
            LambdaQueryWrapper<CarouselItem> carouselQuery = new LambdaQueryWrapper<>();
            carouselQuery.eq(CarouselItem::getProductId, id);
            Long carouselCount = carouselItemMapper.selectCount(carouselQuery);
            if (carouselCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个关联轮播图记录", id, carouselCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在关联轮播图记录");
            }
            
            Long stockInCount = stockInMapper.selectCount(new LambdaQueryWrapper<StockIn>().eq(StockIn::getProductId, id));
            if (stockInCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个入库记录", id, stockInCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在入库记录");
            }
            
            Long stockOutCount = stockOutMapper.selectCount(new LambdaQueryWrapper<StockOut>().eq(StockOut::getProductId, id));
            if (stockOutCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个出库记录", id, stockOutCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在出库记录");
            }
            
            // 检查是否存在关联订单
            LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.eq(Order::getProductId, id);
            Long orderCount = orderMapper.selectCount(orderQuery);
            if (orderCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个关联订单记录", id, orderCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在关联订单记录");
            }

            // 检查是否存在购物车记录
            LambdaQueryWrapper<Cart> cartQuery = new LambdaQueryWrapper<>();
            cartQuery.eq(Cart::getProductId, id);
            Long cartCount = cartMapper.selectCount(cartQuery);
            if (cartCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个购物车记录", id, cartCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在购物车记录");
            }

            // 检查是否存在评价记录
            LambdaQueryWrapper<Review> reviewQuery = new LambdaQueryWrapper<>();
            reviewQuery.eq(Review::getProductId, id);
            Long reviewCount = reviewMapper.selectCount(reviewQuery);
            if (reviewCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个评价记录", id, reviewCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在评价记录");
            }

            // 检查是否存在收藏记录
            LambdaQueryWrapper<Favorite> favoriteQuery = new LambdaQueryWrapper<>();
            favoriteQuery.eq(Favorite::getProductId, id);
            Long favoriteCount = favoriteMapper.selectCount(favoriteQuery);
            if (favoriteCount > 0) {
                LOGGER.warn("删除商品失败，商品ID：{}，存在 {} 个收藏记录", id, favoriteCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品，存在收藏记录");
            }

            int result = productMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除商品成功，商品ID：{}，商品名称：{}", id, product.getName());
                return ResultUtils.success();
            }
            LOGGER.warn("删除商品失败，商品ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除商品失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除商品异常，商品ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除商品失败：" + e.getMessage());
        }
    }

    @Cacheable(value = "products", key = "#productId")
    public Result<?> getProductById(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            LOGGER.warn("查询商品失败，商品ID：{} 不存在", productId);
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        // 填充关联信息
        product.setMerchant(userMapper.selectById(product.getMerchantId()));
        product.setCategory(categoryMapper.selectById(product.getCategoryId()));
        return ResultUtils.success(product);
    }
    @Cacheable(value = "productPages",
            key = "{#currentPage, #size, #sortField, #sortOrder, T(org.example.springboot.util.CacheUtil).generateIdFingerprint(#result)}",
            condition = "#name == null && #categoryId == null && #merchantId == null && #status == null && #minPrice == null && #maxPrice == null && #author == null && #publisher == null && #isbn == null")
    public Page<Product> getProductsByPage(String name, Long categoryId, Long merchantId, Integer status,
                                           Integer currentPage, Integer size, String sortField, String sortOrder,
                                           Double minPrice, Double maxPrice, String author, String publisher, String isbn) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        // 添加基本查询条件
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(Product::getName, name);
        }
        // 添加图书相关搜索条件
        if (StringUtils.isNotBlank(author)) {
            queryWrapper.like(Product::getAuthor, author);
        }
        if (StringUtils.isNotBlank(publisher)) {
            queryWrapper.like(Product::getPublisher, publisher);
        }
        if (StringUtils.isNotBlank(isbn)) {
            queryWrapper.like(Product::getIsbn, isbn);
        }
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        if (merchantId != null) {
            queryWrapper.eq(Product::getMerchantId, merchantId);
        }
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }

        // 处理排序
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        if (StringUtils.isNotBlank(sortField)) {
            switch (sortField) {
                case "sales":
                    queryWrapper.orderBy(true, isAsc, Product::getSalesCount);
                    break;
                case "price":
                    // 对于价格排序，使用自定义SQL考虑折扣价格
                    String orderDirection = isAsc ? "ASC" : "DESC";
                    queryWrapper.apply("1=1")  // 添加一个空条件，防止语法错误
                        .last("ORDER BY CASE WHEN is_discount = 1 THEN discount_price ELSE price END " + orderDirection);
                    break;
                default:
                    queryWrapper.orderByDesc(Product::getCreatedAt);
            }
        } else {
            queryWrapper.orderByDesc(Product::getCreatedAt);
        }

        // 添加价格区间筛选
        if (minPrice != null || maxPrice != null) {
            queryWrapper.and(wrapper -> {
                // 使用CASE WHEN语句根据是否有折扣选择正确的价格字段
                if (minPrice != null) {
                    wrapper.apply("(CASE WHEN is_discount = 1 THEN discount_price ELSE price END) >= {0}", minPrice);
                }
                if (maxPrice != null) {
                    wrapper.apply("(CASE WHEN is_discount = 1 THEN discount_price ELSE price END) <= {0}", maxPrice);
                }
            });
        }

        // 创建分页对象并执行查询
        Page<Product> page = new Page<>(currentPage, size);
        Page<Product> result = productMapper.selectPage(page, queryWrapper);

        // 填充关联信息
        result.getRecords().forEach(product -> {
            product.setMerchant(userMapper.selectById(product.getMerchantId()));
            product.setCategory(categoryMapper.selectById(product.getCategoryId()));
        });

        return result;
    }
    @Caching(evict = {
            @CacheEvict(value = "productPages",allEntries = true),
            @CacheEvict(value="products",allEntries = true)
    })
    public Result<?> updateProductStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            LOGGER.warn("更新商品状态失败，商品ID：{} 不存在", id);
            return ResultUtils.dataNotFound("商品不存在");
        }
        
        product.setStatus(status);
        int result = productMapper.updateById(product);
        if (result > 0) {
            LOGGER.info("更新商品状态成功，商品ID：{}，商品名称：{}，新状态：{}", id, product.getName(), status);
            return ResultUtils.success();
        }
        LOGGER.warn("更新商品状态失败，商品ID：{}，数据库更新返回 0", id);
        throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新商品状态失败");
    }
    @Caching(evict = {
            @CacheEvict(value = "productPages",allEntries = true),
            @CacheEvict(value="products",allEntries = true)
    })
    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除商品失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个商品是否存在关联记录
            for (Long id : ids) {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    LOGGER.warn("批量删除商品，商品ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("商品ID：" + id + " 不存在");
                }
                
                // 检查轮播图
                LambdaQueryWrapper<CarouselItem> carouselQuery = new LambdaQueryWrapper<>();
                carouselQuery.eq(CarouselItem::getProductId, id);
                if (carouselItemMapper.selectCount(carouselQuery) > 0) {
                    LOGGER.warn("批量删除商品失败，商品ID：{}，存在关联轮播图记录", id);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品ID：" + id + "，存在关联轮播图记录");
                }

                // 检查订单
                LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
                orderQuery.eq(Order::getProductId, id);
                if (orderMapper.selectCount(orderQuery) > 0) {
                    LOGGER.warn("批量删除商品失败，商品ID：{}，存在关联订单记录", id);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品ID：" + id + "，存在关联订单记录");
                }

                // 检查购物车
                LambdaQueryWrapper<Cart> cartQuery = new LambdaQueryWrapper<>();
                cartQuery.eq(Cart::getProductId, id);
                if (cartMapper.selectCount(cartQuery) > 0) {
                    LOGGER.warn("批量删除商品失败，商品ID：{}，存在购物车记录", id);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品ID：" + id + "，存在购物车记录");
                }

                // 检查评价
                LambdaQueryWrapper<Review> reviewQuery = new LambdaQueryWrapper<>();
                reviewQuery.eq(Review::getProductId, id);
                if (reviewMapper.selectCount(reviewQuery) > 0) {
                    LOGGER.warn("批量删除商品失败，商品ID：{}，存在评价记录", id);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品ID：" + id + "，存在评价记录");
                }

                // 检查收藏
                LambdaQueryWrapper<Favorite> favoriteQuery = new LambdaQueryWrapper<>();
                favoriteQuery.eq(Favorite::getProductId, id);
                if (favoriteMapper.selectCount(favoriteQuery) > 0) {
                    LOGGER.warn("批量删除商品失败，商品ID：{}，存在收藏记录", id);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除商品ID：" + id + "，存在收藏记录");
                }
            }

            // 使用新的批量删除方法
            int result = 0;
            for (Long id : ids) {
                result += productMapper.deleteById(id);
            }
            
            if (result > 0) {
                LOGGER.info("批量删除商品成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除商品失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除商品失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除商品异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除商品失败：" + e.getMessage());
        }
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productPages",allEntries = true),
            @CacheEvict(value="products",allEntries = true)
    })
    public Result<?> updateBatchStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量更新商品状态失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        // 检查状态值是否有效
        if (status != 0 && status != 1) {
            LOGGER.warn("批量更新商品状态失败，无效的状态值：{}", status);
            return ResultUtils.paramError("无效的商品状态值");
        }
        
        try {
            // 检查每个商品是否存在
            for (Long id : ids) {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    LOGGER.warn("批量更新商品状态，商品ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("商品ID：" + id + " 不存在");
                }
            }

            // 批量更新状态
            int updateCount = 0;
            for (Long id : ids) {
                Product product = new Product();
                product.setId(id);
                product.setStatus(status);
                if (productMapper.updateById(product) > 0) {
                    updateCount++;
                }
            }

            if (updateCount > 0) {
                LOGGER.info("批量更新商品状态成功，更新数量：{}，ID列表：{}", updateCount, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量更新商品状态失败，数据库更新返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量更新商品状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量更新商品状态异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量更新商品状态失败：" + e.getMessage());
        }
    }
} 