package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Logistics;
import org.example.springboot.service.LogisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/logistics")
@Validated
public class LogisticsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsController.class);

    @Autowired
    private LogisticsService logisticsService;
    @PostMapping
    public Result<?> createLogistics(@Valid @RequestBody Logistics logistics) {
        return logisticsService.createLogistics(logistics);
    }
    @PutMapping("/{id}/status")
    public Result<?> updateLogisticsStatus(
            @PathVariable @NotNull(message = "物流ID不能为空") @Min(value = 1, message = "物流ID必须大于0") Long id, 
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        return logisticsService.updateLogisticsStatus(id, status);
    }
    @DeleteMapping("/{id}")
    public Result<?> deleteLogistics(@PathVariable @NotNull(message = "物流ID不能为空") @Min(value = 1, message = "物流ID必须大于0") Long id) {
        return logisticsService.deleteLogistics(id);
    }
    @GetMapping("/{id}")
    public Result<?> getLogisticsById(@PathVariable @NotNull(message = "物流ID不能为空") @Min(value = 1, message = "物流ID必须大于0") Long id) {
        return logisticsService.getLogisticsById(id);
    }
    @GetMapping("/order/{orderId}")
    public Result<?> getLogisticsByOrderId(@PathVariable @NotNull(message = "订单ID不能为空") @Min(value = 1, message = "订单ID必须大于0") Long orderId) {
        return logisticsService.getLogisticsByOrderId(orderId);
    }
    @GetMapping("/page")
    public Result<?> getLogisticsByPage(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return logisticsService.getLogisticsByPage(orderId, merchantId, status, currentPage, size);
    }
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "物流ID列表不能为空") List<Long> ids) {
        return logisticsService.deleteBatch(ids);
    }
    @PutMapping("/{id}/sign")
    public Result<?> signLogistics(@PathVariable @NotNull(message = "物流ID不能为空") @Min(value = 1, message = "物流ID必须大于0") Long id) {
        return logisticsService.signLogistics(id);
    }
} 