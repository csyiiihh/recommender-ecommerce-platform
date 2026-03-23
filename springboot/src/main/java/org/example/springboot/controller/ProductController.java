package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Product;
import org.example.springboot.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/product")
@Validated
public class ProductController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;
    @PostMapping
    public Result<?> createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product);
    }
    @PutMapping("/{id}")
    public Result<?> updateProduct(
            @PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long id, 
            @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteProduct(@PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long id) {
        return productService.deleteProduct(id);
    }
    @GetMapping("/{id}")
    public Result<?> getProductById(@PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long id) {
        return productService.getProductById(id);
    }
    @GetMapping("/page")
    public Result<?> getProductsByPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String isbn) {
        return Result.success(productService.getProductsByPage(name, categoryId, merchantId, status,
                currentPage, size, sortField, sortOrder, minPrice, maxPrice, author, publisher, isbn));
    }
    @PutMapping("/{id}/status")
    public Result<?> updateProductStatus(
            @PathVariable @NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return productService.updateProductStatus(id, status);
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "商品ID列表不能为空") List<Long> ids) {
        return productService.deleteBatch(ids);
    }
    // 获取全部商品
    @GetMapping("/all")
    public Result<?> getAllProducts(@RequestParam(required = false) Long merchantId) {
        return Result.success(productService.getProductsByPage(null, null, merchantId, null, 1, Integer.MAX_VALUE, null, null, null, null, null, null, null).getRecords());
    }
    @PutMapping("/batch/status")
    public Result<?> updateBatchStatus(
            @RequestParam @NotNull(message = "商品ID列表不能为空") List<Long> ids,
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return productService.updateBatchStatus(ids, status);
    }

} 
