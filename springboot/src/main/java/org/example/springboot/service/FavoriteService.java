package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Favorite;
import org.example.springboot.entity.User;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.FavoriteMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteService.class);

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;


    public Result<?> createFavorite(Favorite favorite) {
        try {
            // 检查用户是否存在
            User user = userMapper.selectById(favorite.getUserId());
            if (user == null) {
                LOGGER.warn("创建收藏失败，用户ID：{} 不存在", favorite.getUserId());
                return ResultUtils.error(ResultCode.USER_NOT_FOUND, "用户不存在");
            }

            // 检查商品是否存在
            Product product = productMapper.selectById(favorite.getProductId());
            if (product == null) {
                LOGGER.warn("创建收藏失败，商品ID：{} 不存在", favorite.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }

            // 检查是否已经收藏，已收藏直接更新状态
            LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Favorite::getUserId, favorite.getUserId())
                       .eq(Favorite::getProductId, favorite.getProductId());
            if (favoriteMapper.selectCount(queryWrapper) > 0) {
                favorite.setStatus(favorite.getStatus() == 0 ? 1 : 0); // 切换状态
                return updateFavoriteStatus(favorite.getUserId(), favorite.getProductId(), favorite.getStatus());
            }

            favorite.setStatus(1); // 默认状态为收藏
            int result = favoriteMapper.insert(favorite);
            if (result > 0) {
                LOGGER.info("创建收藏成功，收藏ID：{}，用户ID：{}，商品ID：{}", 
                    favorite.getId(), favorite.getUserId(), favorite.getProductId());
                return ResultUtils.success(favorite);
            }
            LOGGER.warn("创建收藏失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建收藏失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建收藏异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建收藏失败：" + e.getMessage());
        }
    }

    public Result<?> updateFavoriteStatus(Long userId, Long productId, Integer status) {
        try {
            Favorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
            if (favorite == null) {
                LOGGER.warn("更新收藏状态失败，用户ID：{}，商品ID：{}，收藏记录不存在", userId, productId);
                return ResultUtils.dataNotFound("收藏记录不存在");
            }

            // 检查状态是否合法
            if (status == null || status < 0 || status > 1) {
                LOGGER.warn("更新收藏状态失败，状态不合法：{}", status);
                return ResultUtils.paramError("收藏状态不合法，必须为0或1");
            }

            favorite.setStatus(status);
            int result = favoriteMapper.updateById(favorite);
            if (result > 0) {
                LOGGER.info("更新收藏状态成功，收藏ID：{}，用户ID：{}，商品ID：{}，新状态：{}", 
                    favorite.getId(), userId, productId, status);
                return ResultUtils.success(favorite);
            }
            LOGGER.warn("更新收藏状态失败，收藏ID：{}，数据库更新返回 0", favorite.getId());
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新收藏状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新收藏状态异常，用户ID：{}，商品ID：{}，错误：{}", userId, productId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新收藏状态失败：" + e.getMessage());
        }
    }

    public Result<?> deleteFavorite(Long id) {
        // 检查收藏是否存在
        Favorite favorite = favoriteMapper.selectById(id);
        if (favorite == null) {
            LOGGER.warn("删除收藏失败，收藏ID：{} 不存在", id);
            return ResultUtils.dataNotFound("收藏记录不存在");
        }
        
        try {
            int result = favoriteMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除收藏成功，收藏ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除收藏失败，收藏ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除收藏失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除收藏异常，收藏ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除收藏失败：" + e.getMessage());
        }
    }

    public Result<?> getFavoriteById(Long id) {
        Favorite favorite = favoriteMapper.selectById(id);
        if (favorite == null) {
            LOGGER.warn("查询收藏失败，收藏ID：{} 不存在", id);
            return ResultUtils.dataNotFound("收藏记录不存在");
        }
        
        // 填充关联信息
        favorite.setUser(userMapper.selectById(favorite.getUserId()));
        favorite.setProduct(productMapper.selectById(favorite.getProductId()));
        return ResultUtils.success(favorite);
    }

    public Result<?> getFavoritesByUserId(Long userId) {
        try {
            LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Favorite::getUserId, userId)
                       .eq(Favorite::getStatus, 1); // 只查询状态为收藏的记录
            List<Favorite> favorites = favoriteMapper.selectList(queryWrapper);
            
            if (favorites == null || favorites.isEmpty()) {
                return Result.success(new java.util.ArrayList<>());
            }
            
            // 优化：批量查询避免 N+1 问题
            // 收集所有需要查询的 ID
            java.util.Set<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .collect(java.util.stream.Collectors.toSet());
            
            // 批量查询商品信息（1次查询替代 N 次查询）
            List<Product> products = productIds.isEmpty() ? 
                new java.util.ArrayList<>() : 
                productMapper.selectBatchIds(productIds);
            
            // 将商品列表转换为 Map，方便快速查找
            java.util.Map<Long, Product> productMap = products.stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));
            
            // 用户信息只需要查询一次（同一个用户）
            User user = favorites.isEmpty() ? null : userMapper.selectById(userId);
            
            // 填充关联信息（内存操作，不涉及数据库查询）
            favorites.forEach(favorite -> {
                favorite.setUser(user);
                favorite.setProduct(productMap.get(favorite.getProductId()));
            });
            
            LOGGER.debug("获取用户收藏成功，用户ID: {}, 收藏数量: {}", userId, favorites.size());
            return ResultUtils.success(favorites);
        } catch (Exception e) {
            LOGGER.error("获取用户收藏异常，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取收藏记录失败：" + e.getMessage());
        }
    }

    public Result<?> getFavoritesByPage(Long userId, Integer currentPage, Integer size) {
        try {
            LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
            if (userId != null) {
                queryWrapper.eq(Favorite::getUserId, userId)
                           .eq(Favorite::getStatus, 1); // 只查询状态为收藏的记录
            }

            Page<Favorite> page = new Page<>(currentPage, size);
            Page<Favorite> result = favoriteMapper.selectPage(page, queryWrapper);

            if (result.getRecords().isEmpty()) {
                return Result.success(result);
            }

            // 优化：批量查询避免 N+1 问题
            Set<Long> userIds = result.getRecords().stream()
                .map(Favorite::getUserId)
                .collect(Collectors.toSet());
            Set<Long> productIds = result.getRecords().stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toSet());

            // 批量查询用户和商品信息
            Map<Long, User> userMap = userIds.isEmpty() ? 
                new java.util.HashMap<>() : 
                userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
            Map<Long, Product> productMap = productIds.isEmpty() ? 
                new java.util.HashMap<>() : 
                productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            // 填充关联信息（内存操作）
            result.getRecords().forEach(favorite -> {
                favorite.setUser(userMap.get(favorite.getUserId()));
                favorite.setProduct(productMap.get(favorite.getProductId()));
            });

            LOGGER.debug("分页查询收藏，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询收藏异常，用户ID: {}, 页码: {}, 错误: {}", userId, currentPage, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "获取收藏记录失败：" + e.getMessage());
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除收藏失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个收藏是否存在
            for (Long id : ids) {
                Favorite favorite = favoriteMapper.selectById(id);
                if (favorite == null) {
                    LOGGER.warn("批量删除收藏，收藏ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("收藏ID：" + id + " 不存在");
                }
            }
            
            LambdaQueryWrapper<Favorite> wrapper = Wrappers.lambdaQuery();
            wrapper.in(Favorite::getId, ids);
            int result = favoriteMapper.delete(wrapper);
            
            if (result > 0) {
                LOGGER.info("批量删除收藏成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除收藏失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除收藏失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除收藏异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除收藏失败：" + e.getMessage());
        }
    }
} 