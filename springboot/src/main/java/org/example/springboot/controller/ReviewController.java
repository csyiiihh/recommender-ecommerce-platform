package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Review;
import org.example.springboot.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/review")
@Validated
public class ReviewController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;
    @PostMapping
    public Result<?> createReview(@Valid @RequestBody Review review) {
        return reviewService.createReview(review);
    }
    @PutMapping("/{id}/status")
    public Result<?> updateReviewStatus(
            @PathVariable @NotNull(message = "评论ID不能为空") @Min(value = 1, message = "评论ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return reviewService.updateReviewStatus(id, status);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteReview(@PathVariable @NotNull(message = "评论ID不能为空") @Min(value = 1, message = "评论ID必须大于0") Long id) {
        return reviewService.deleteReview(id);
    }
    @GetMapping("/{id}")
    public Result<?> getReviewById(@PathVariable @NotNull(message = "评论ID不能为空") @Min(value = 1, message = "评论ID必须大于0") Long id) {
        return reviewService.getReviewById(id);
    }
    @GetMapping("/product/{productId}")
    public Result<?> getReviewsByProductId(
            @PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long productId, 
            @RequestParam(required = false) Integer status) {
        return reviewService.getReviewsByProductId(productId, status);
    }
    @GetMapping("/page")
    public Result<?> getReviewsByPage(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return reviewService.getReviewsByPage(productId, productName,userId,username,merchantId, status, currentPage, size);
    }
}

 