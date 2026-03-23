package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Category;
import org.example.springboot.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/category")
@Validated
public class CategoryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;
    @PostMapping
    public Result<?> createCategory(@Valid @RequestBody Category category) {
        return categoryService.createCategory(category);
    }
    @PutMapping("/{id}")
    public Result<?> updateCategory(
            @PathVariable @NotNull(message = "分类ID不能为空") @Min(value = 1, message = "分类ID必须大于0") Long id, 
            @Valid @RequestBody Category category) {
        return categoryService.updateCategory(id, category);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable @NotNull(message = "分类ID不能为空") @Min(value = 1, message = "分类ID必须大于0") Long id) {
        return categoryService.deleteCategory(id);
    }
    @GetMapping("/{id}")
    public Result<?> getCategoryById(@PathVariable @NotNull(message = "分类ID不能为空") @Min(value = 1, message = "分类ID必须大于0") Long id) {
        return categoryService.getCategoryById(id);
    }
    @GetMapping("/page")
    public Result<?> getCategoriesByPage(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return categoryService.getCategoriesByPage(name, currentPage, size);
    }
    @GetMapping("/all")
    public Result<?> getAllCategories() {
        return categoryService.getAllCategories();
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "分类ID列表不能为空") List<Long> ids) {
        return categoryService.deleteBatch(ids);
    }
} 