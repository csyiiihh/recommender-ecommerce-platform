package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.StockIn;
import org.example.springboot.entity.StockOut;
import org.example.springboot.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/stock")
@Validated
public class StockController {

    @Autowired
    private StockService stockService;
    @PostMapping("/in")
    public Result<?> createStockIn(@Valid @RequestBody StockIn stockIn) {
        return stockService.createStockIn(stockIn);
    }
    @PostMapping("/out")
    public Result<?> createStockOut(@Valid @RequestBody StockOut stockOut) {
        return stockService.createStockOut(stockOut);
    }
    @GetMapping("/in/list")
    public Result<?> getStockInList(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return Result.success(stockService.getStockInList(productId, supplier, status, operatorId, currentPage, size));
    }
    @GetMapping("/out/list")
    public Result<?> getStockOutList(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return Result.success(stockService.getStockOutList(productId, type, status, operatorId, customerName, orderNo, currentPage, size));
    }
    @PutMapping("/in/{id}/invalidate")
    public Result<?> invalidateStockIn(@PathVariable @NotNull(message = "入库ID不能为空") @Min(value = 1, message = "入库ID必须大于0") Long id) {
        return stockService.invalidateStockIn(id);
    }
    @PutMapping("/out/{id}/invalidate")
    public Result<?> invalidateStockOut(@PathVariable @NotNull(message = "出库ID不能为空") @Min(value = 1, message = "出库ID必须大于0") Long id) {
        return stockService.invalidateStockOut(id);
    }
    @DeleteMapping("/in/{id}")
    public Result<?> deleteStockIn(@PathVariable @NotNull(message = "入库ID不能为空") @Min(value = 1, message = "入库ID必须大于0") Long id) {
        return stockService.deleteStockIn(id);
    }
    @DeleteMapping("/out/{id}")
    public Result<?> deleteStockOut(@PathVariable @NotNull(message = "出库ID不能为空") @Min(value = 1, message = "出库ID必须大于0") Long id) {
        return stockService.deleteStockOut(id);
    }
} 