package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Favorite;
import org.example.springboot.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/favorite")
@Validated
public class FavoriteController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteController.class);

    @Autowired
    private FavoriteService favoriteService;
    @PostMapping
    public Result<?> createFavorite(@Valid @RequestBody Favorite favorite) {
        return favoriteService.createFavorite(favorite);
    }
    @PutMapping("/{userId}/{productId}/status")
    public Result<?> updateFavoriteStatus(
            @PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId, 
            @PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long productId, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return favoriteService.updateFavoriteStatus(userId, productId, status);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteFavorite(@PathVariable @NotNull(message = "收藏ID不能为空") @Min(value = 1, message = "收藏ID必须大于0") Long id) {
        return favoriteService.deleteFavorite(id);
    }
    @GetMapping("/{id}")
    public Result<?> getFavoriteById(@PathVariable @NotNull(message = "收藏ID不能为空") @Min(value = 1, message = "收藏ID必须大于0") Long id) {
        return favoriteService.getFavoriteById(id);
    }
    @GetMapping("/user/{userId}")
    public Result<?> getFavoritesByUserId(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return favoriteService.getFavoritesByUserId(userId);
    }
    @GetMapping("/page")
    public Result<?> getFavoritesByPage(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return favoriteService.getFavoritesByPage(userId, currentPage, size);
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "收藏ID列表不能为空") List<Long> ids) {
        return favoriteService.deleteBatch(ids);
    }
} 