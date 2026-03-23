package org.example.springboot.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/recommend")
@Validated
public class RecommendController {
    @Autowired
    private RecommendService recommendService;
    @GetMapping("/user/{userId}")
    public Result<?> getRecommendations(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return recommendService.generateRecommendations(userId);
    }
    
    /**
     * 获取带解释的推荐（前台可感知版本）
     */
    @GetMapping("/user/{userId}/explanation")
    public Result<?> getRecommendationsWithExplanation(
            @PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) String algorithmType,
            @RequestParam(required = false) String weights) {
        return recommendService.generateRecommendationsWithExplanation(userId, algorithmType, weights);
    }
    
    /**
     * 保存用户反馈
     */
    @PostMapping("/feedback")
    public Result<?> saveFeedback(
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId,
            @RequestParam @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long productId,
            @RequestParam @NotBlank(message = "反馈类型不能为空") String feedbackType,
            @RequestParam(required = false) String remark) {
        return recommendService.saveFeedback(userId, productId, feedbackType, remark);
    }

    /**
     * 获取用户反馈历史
     */
    @GetMapping("/feedback/history")
    public Result<?> getFeedbackHistory(@RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return recommendService.getFeedbackHistory(userId);
    }

    /**
     * 撤销一条反馈
     */
    @DeleteMapping("/feedback/{id}")
    public Result<?> undoFeedback(
            @PathVariable("id") @NotNull(message = "反馈ID不能为空") @Min(value = 1, message = "反馈ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return recommendService.undoFeedback(userId, id);
    }

    /**
     * 获取用户分类匹配度
     */
    @GetMapping("/category-matches/{userId}")
    public Result<?> getCategoryMatches(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return recommendService.getCategoryMatches(userId);
    }

    @PostMapping("/update")
    public Result<?> updateRecommendations() {
        recommendService.updateRecommendations();
        return Result.success();
    }
} 