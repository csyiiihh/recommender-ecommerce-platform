package org.example.springboot.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Address;
import org.example.springboot.service.AddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
@Validated
public class AddressController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressController.class);

    @Autowired
    private AddressService addressService;

    @PostMapping
    public Result<?> createAddress(@Valid @RequestBody Address address) {
        return addressService.createAddress(address);
    }

    @PutMapping("/{id}")
    public Result<?> updateAddress(
            @PathVariable @NotNull(message = "地址ID不能为空") @Min(value = 1, message = "地址ID必须大于0") Long id, 
            @Valid @RequestBody Address address) {
        return addressService.updateAddress(id, address);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteAddress(@PathVariable @NotNull(message = "地址ID不能为空") @Min(value = 1, message = "地址ID必须大于0") Long id) {
        return addressService.deleteAddress(id);
    }

    @GetMapping("/{id}")
    public Result<?> getAddressById(@PathVariable @NotNull(message = "地址ID不能为空") @Min(value = 1, message = "地址ID必须大于0") Long id) {
        return addressService.getAddressById(id);
    }

    @GetMapping("/user/{userId}")
    public Result<?> getAddressesByUserId(@PathVariable @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        return addressService.getAddressesByUserId(userId);
    }

    @GetMapping("/page")
    public Result<?> getAddressesByPage(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer size) {
        return addressService.getAddressesByPage(userId, currentPage, size);
    }

    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestParam @NotNull(message = "地址ID列表不能为空") List<Long> ids) {
        return addressService.deleteBatch(ids);
    }
} 