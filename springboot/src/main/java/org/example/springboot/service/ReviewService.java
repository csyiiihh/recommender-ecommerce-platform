package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Review;
import org.example.springboot.entity.User;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.ReviewMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired private org.example.springboot.mapper.OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<?> createReview(Review review) {
        try {
            // 检查用户是否存在
            User user = userMapper.selectForUpdate(review.getUserId());
            if (user == null) {
                LOGGER.warn("创建评价失败，用户ID：{} 不存在", review.getUserId());
                return ResultUtils.error(ResultCode.USER_NOT_FOUND, "用户不存在");
            }

            // 检查商品是否存在
            Product product = productMapper.selectById(review.getProductId());
            if (product == null) {
                LOGGER.warn("创建评价失败，商品ID：{} 不存在", review.getProductId());
                return ResultUtils.dataNotFound("商品不存在");
            }

            // 检查评分是否合法
            if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
                LOGGER.warn("创建评价失败，评分不合法：{}", review.getRating());
                return ResultUtils.paramError("评分必须在1-5之间");
            }

            if (review.getContent() == null || review.getContent().isBlank() || review.getContent().length() > 2000) return ResultUtils.paramError("评价内容须为1至2000字");
            Long purchases = orderMapper.selectCount(new LambdaQueryWrapper<org.example.springboot.entity.Order>()
                .eq(org.example.springboot.entity.Order::getUserId, review.getUserId()).eq(org.example.springboot.entity.Order::getProductId, review.getProductId()).eq(org.example.springboot.entity.Order::getStatus, 3));
            if (purchases == null || purchases == 0) return ResultUtils.businessError("完成购买并签收后才能评价");
            Long existing = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getUserId, review.getUserId()).eq(Review::getProductId, review.getProductId()).in(Review::getStatus, 0, 1));
            if (existing != null && existing > 0) return ResultUtils.businessError("已提交评价，请勿重复发布");
            review.setId(null);
            review.setStatus(0);
            review.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            review.setContent(review.getContent().strip());
            review.setUser(null); review.setProduct(null);
            int result = reviewMapper.insert(review);
            if (result > 0) {
                LOGGER.info("创建评价成功，评价ID：{}，用户ID：{}，商品ID：{}，评分：{}", 
                    review.getId(), review.getUserId(), review.getProductId(), review.getRating());
                return ResultUtils.success(review);
            }
            LOGGER.warn("创建评价失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建评价失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建评价异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建评价失败：" + e.getMessage());
        }
    }

    public Result<?> updateReviewStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 2) return ResultUtils.paramError("无效审核状态");
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            LOGGER.warn("更新评价状态失败，评价ID：{} 不存在", id);
            return ResultUtils.dataNotFound("评价不存在");
        }
        
        try {
            review.setStatus(status);
            int result = reviewMapper.updateById(review);
            if (result > 0) {
                LOGGER.info("更新评价状态成功，评价ID：{}，新状态：{}", id, status);
                return ResultUtils.success(review);
            }
            LOGGER.warn("更新评价状态失败，评价ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新评价状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新评价状态异常，评价ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新评价状态失败：" + e.getMessage());
        }
    }

    public Result<?> deleteReview(Long id) {
        // 检查评价是否存在
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            LOGGER.warn("删除评价失败，评价ID：{} 不存在", id);
            return ResultUtils.dataNotFound("评价不存在");
        }
        
        try {
            int result = reviewMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除评价成功，评价ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除评价失败，评价ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除评价失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除评价异常，评价ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除评价失败：" + e.getMessage());
        }
    }

    public Result<?> getReviewById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            LOGGER.warn("查询评价失败，评价ID：{} 不存在", id);
            return ResultUtils.dataNotFound("评价不存在");
        }
        
        // 填充关联信息
        review.setUser(userMapper.selectById(review.getUserId()));
        review.setProduct(productMapper.selectById(review.getProductId()));
        return ResultUtils.success(review);
    }

    public Result<?> getReviewsByProductId(Long productId, Integer status) {
        Product product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) return ResultUtils.dataNotFound("图书不存在或已下架");
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>().eq(Review::getProductId, productId)
            .eq(Review::getStatus, 1).orderByDesc(Review::getCreatedAt));
        var ids = reviews.stream().map(Review::getUserId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        var users = ids.isEmpty() ? java.util.Map.<Long, User>of() : userMapper.selectBatchIds(ids).stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        var response = reviews.stream().map(review -> {
            User user = users.get(review.getUserId());
            String nickname = user == null || user.getName() == null || user.getName().isBlank() ? "书友" : user.getName();
            String avatar = user != null && "USER".equals(user.getRole()) && user.getBusinessLicense() != null && user.getBusinessLicense().startsWith("/img/") ? "/api" + user.getBusinessLicense() : null;
            return new org.example.springboot.dto.PublicReview(review.getId(), review.getProductId(), review.getRating(), review.getContent(), review.getCreatedAt(), new org.example.springboot.dto.PublicReview.Reviewer(nickname, avatar));
        }).toList();
        return ResultUtils.success(response);
    }

    public Result<?> getReviewsByPage(Long productId, String productName, Integer userId, String username, 
                                       Long merchantId, Integer status, Integer currentPage, Integer size) {
        try {
            LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
            if (productId != null) {
                queryWrapper.eq(Review::getProductId, productId);
            }
            if (StringUtils.isNotBlank(productName)) {
                List<Long> productIds = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().like(Product::getName, productName))
                    .stream().map(Product::getId).toList();
                if (!productIds.isEmpty()) {
                    queryWrapper.in(Review::getProductId, productIds);
                } else {
                    // 如果没有匹配的商品，返回空结果
                    Page<Review> emptyPage = new Page<>(currentPage, size);
                    emptyPage.setTotal(0);
                    return ResultUtils.success(emptyPage);
                }
            }
            if (StringUtils.isNotBlank(username)) {
                List<Long> userIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>().like(User::getUsername, username))
                    .stream().map(User::getId).toList();
                if (!userIds.isEmpty()) {
                    queryWrapper.in(Review::getUserId, userIds);
                } else {
                    // 如果没有匹配的用户，返回空结果
                    Page<Review> emptyPage = new Page<>(currentPage, size);
                    emptyPage.setTotal(0);
                    return ResultUtils.success(emptyPage);
                }
            }
            if (userId != null) {
                queryWrapper.eq(Review::getUserId, userId);
            }
            if (merchantId != null) {
                List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
                List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
                if (!productIds.isEmpty()) {
                    queryWrapper.in(Review::getProductId, productIds);
                } else {
                    // 如果没有匹配的商品，返回空结果
                    Page<Review> emptyPage = new Page<>(currentPage, size);
                    emptyPage.setTotal(0);
                    return ResultUtils.success(emptyPage);
                }
            }

            if (status != null) {
                queryWrapper.eq(Review::getStatus, status);
            }

            Page<Review> page = new Page<>(currentPage, size);
            Page<Review> result = reviewMapper.selectPage(page, queryWrapper);

            // 填充关联信息
            result.getRecords().forEach(review -> {
                review.setUser(userMapper.selectById(review.getUserId()));
                review.setProduct(productMapper.selectById(review.getProductId()));
            });

            LOGGER.debug("分页查询评价，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询评价异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询评价列表失败");
        }
    }

} 