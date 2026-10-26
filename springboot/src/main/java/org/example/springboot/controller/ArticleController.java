package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Article;
import org.example.springboot.service.ArticleService;
import org.example.springboot.service.AccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/article")
@Validated
public class ArticleController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleController.class);

    @Autowired
    private ArticleService articleService;

    @Autowired
    private AccessService access;

    @PostMapping
    public Result<?> createArticle(@Valid @RequestBody Article article) {
        return articleService.createArticle(article);
    }

    @PutMapping("/{id}")
    public Result<?> updateArticle(
            @PathVariable @NotNull(message = "文章ID不能为空") @Min(value = 1, message = "文章ID必须大于0") Long id, 
            @Valid @RequestBody Article article) {
        return articleService.updateArticle(id, article);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteArticle(@PathVariable @NotNull(message = "文章ID不能为空") @Min(value = 1, message = "文章ID必须大于0") Long id) {
        return articleService.deleteArticle(id);
    }

    @GetMapping("/{id}")
    public Result<?> getArticleById(@PathVariable @NotNull(message = "文章ID不能为空") @Min(value = 1, message = "文章ID必须大于0") Long id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/page")
    public Result<?> getArticlesByPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(1) Integer size,
            @RequestParam(required = false) String sort) {
        return sort == null ? getArticlesByPage(title, status, currentPage, size)
            : Result.success(articleService.getArticlesByPage(title, 1, currentPage, size, sort));
    }

    public Result<?> getArticlesByPage(String title, Integer status, Integer currentPage, Integer size) {
        return Result.success(articleService.getArticlesByPage(title, 1, currentPage, size));
    }

    @GetMapping("/manage/page")
    public Result<?> getManagedArticlesByPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        access.admin();
        return Result.success(articleService.getArticlesByPage(title, status, currentPage, size));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateArticleStatus(
            @PathVariable @NotNull(message = "文章ID不能为空") @Min(value = 1, message = "文章ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return articleService.updateArticleStatus(id, status);
    }

    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "文章ID列表不能为空") List<Long> ids) {
        return articleService.deleteBatch(ids);
    }
} 