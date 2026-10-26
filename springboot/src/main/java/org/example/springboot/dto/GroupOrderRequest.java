package org.example.springboot.dto;

import jakarta.validation.constraints.*;

public record GroupOrderRequest(
    @Min(1) Long activityId,
    @Min(1) Long teamId,
    @NotNull @Min(1) Long userId,
    @NotNull @Min(1) Long addressId,
    @NotNull @Min(1) @Max(1000) Integer quantity
) {}
