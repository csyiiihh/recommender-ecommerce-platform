package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.entity.Category;
import org.example.springboot.entity.Product;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.CategoryMapper;
import org.example.springboot.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    public Result<?> createCategory(Category category) {
        try {
            int result = categoryMapper.insert(category);
            if (result > 0) {
                LOGGER.info("创建分类成功，分类ID：{}，分类名称：{}", category.getId(), category.getName());
                return ResultUtils.success(category);
            }
            LOGGER.warn("创建分类失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建分类失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建分类异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建分类失败：" + e.getMessage());
        }
    }

    public Result<?> updateCategory(Long id, Category category) {
        // 检查分类是否存在
        Category existingCategory = categoryMapper.selectById(id);
        if (existingCategory == null) {
            LOGGER.warn("更新分类失败，分类ID：{} 不存在", id);
            return ResultUtils.dataNotFound("分类不存在");
        }
        
        category.setId(id);
        try {
            int result = categoryMapper.updateById(category);
            if (result > 0) {
                LOGGER.info("更新分类成功，分类ID：{}，分类名称：{}", id, category.getName());
                return ResultUtils.success(category);
            }
            LOGGER.warn("更新分类失败，分类ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新分类失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新分类异常，分类ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新分类失败：" + e.getMessage());
        }
    }

    public Result<?> deleteCategory(Long id) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            LOGGER.warn("删除分类失败，分类ID：{} 不存在", id);
            return ResultUtils.dataNotFound("分类不存在");
        }
        
        try {
            // 检查是否存在关联商品
            LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
            productQuery.eq(Product::getCategoryId, id);
            Long productCount = productMapper.selectCount(productQuery);
            if (productCount > 0) {
                LOGGER.warn("删除分类失败，分类ID：{}，存在 {} 个关联商品", id, productCount);
                return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除分类，存在关联商品");
            }

            int result = categoryMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除分类成功，分类ID：{}，分类名称：{}", id, category.getName());
                return ResultUtils.success();
            }
            LOGGER.warn("删除分类失败，分类ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除分类失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除分类异常，分类ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除分类失败：" + e.getMessage());
        }
    }

    public Result<?> getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            LOGGER.warn("查询分类失败，分类ID：{} 不存在", id);
            return ResultUtils.dataNotFound("分类不存在");
        }
        
        // 填充商品数量
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getCategoryId, id);
        category.setProductCount(Math.toIntExact(productMapper.selectCount(productQuery)));
        return ResultUtils.success(category);
    }

    public Result<?> getCategoriesByPage(String name, Integer currentPage, Integer size) {
        try {
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Category::getName, name);
            }

            Page<Category> page = new Page<>(currentPage, size);
            Page<Category> result = categoryMapper.selectPage(page, queryWrapper);

            // 填充每个分类的商品数量
            result.getRecords().forEach(category -> {
                LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
                productQuery.eq(Product::getCategoryId, category.getId());
                category.setProductCount(Math.toIntExact(productMapper.selectCount(productQuery)));
            });

            LOGGER.debug("分页查询分类，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询分类异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询分类列表失败");
        }
    }

    public Result<?> getAllCategories() {
        try {
            List<Category> categories = categoryMapper.selectList(null);
            if (categories != null && !categories.isEmpty()) {
                // 填充每个分类的商品数量
                categories.forEach(category -> {
                    LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
                    productQuery.eq(Product::getCategoryId, category.getId());
                    category.setProductCount(Math.toIntExact(productMapper.selectCount(productQuery)));
                });
                LOGGER.debug("查询所有分类，共 {} 个", categories.size());
                return ResultUtils.success(categories);
            }
            LOGGER.info("查询所有分类，结果为空");
            return ResultUtils.success(categories); // 返回空列表而不是错误
        } catch (Exception e) {
            LOGGER.error("查询所有分类异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询分类列表失败");
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除分类失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个分类是否存在关联商品
            for (Long id : ids) {
                Category category = categoryMapper.selectById(id);
                if (category == null) {
                    LOGGER.warn("批量删除分类，分类ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("分类ID：" + id + " 不存在");
                }
                
                LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
                productQuery.eq(Product::getCategoryId, id);
                Long productCount = productMapper.selectCount(productQuery);
                if (productCount > 0) {
                    LOGGER.warn("批量删除分类失败，分类ID：{}，存在 {} 个关联商品", id, productCount);
                    return ResultUtils.error(ResultCode.CANNOT_DELETE, "无法删除分类ID：" + id + "，存在关联商品");
                }
            }

            // 使用新的批量删除方法
            int result = 0;
            for (Long id : ids) {
                result += categoryMapper.deleteById(id);
            }
            if (result > 0) {
                LOGGER.info("批量删除分类成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除分类失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除分类失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除分类异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除分类失败：" + e.getMessage());
        }
    }
} 