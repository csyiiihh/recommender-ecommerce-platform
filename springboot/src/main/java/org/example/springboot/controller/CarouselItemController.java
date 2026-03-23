package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.CarouselItem;
import org.example.springboot.service.CarouselItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/carousel")
@Validated
public class CarouselItemController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarouselItemController.class);

    @Autowired
    private CarouselItemService carouselItemService;
    @PostMapping
    public Result<?> createCarouselItem(@Valid @RequestBody CarouselItem carouselItem) {
        return carouselItemService.createCarouselItem(carouselItem);
    }
    @PutMapping("/{id}")
    public Result<?> updateCarouselItem(
            @PathVariable @NotNull(message = "轮播图ID不能为空") @Min(value = 1, message = "轮播图ID必须大于0") Long id, 
            @Valid @RequestBody CarouselItem carouselItem) {
        return carouselItemService.updateCarouselItem(id, carouselItem);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteCarouselItem(@PathVariable @NotNull(message = "轮播图ID不能为空") @Min(value = 1, message = "轮播图ID必须大于0") Long id) {
        return carouselItemService.deleteCarouselItem(id);
    }
    @GetMapping("/{id}")
    public Result<?> getCarouselItemById(@PathVariable @NotNull(message = "轮播图ID不能为空") @Min(value = 1, message = "轮播图ID必须大于0") Long id) {
        return carouselItemService.getCarouselItemById(id);
    }
    @GetMapping("/active")
    public Result<?> getActiveCarouselItems() {
        return carouselItemService.getActiveCarouselItems();
    }
    @GetMapping("/page")
    public Result<?> getCarouselItemsByPage(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return carouselItemService.getCarouselItemsByPage(currentPage, size);
    }
} 