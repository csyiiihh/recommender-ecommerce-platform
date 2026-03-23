package org.example.springboot.common;

/**
 * Result 工具类
 * 简化 Result 的使用，统一错误码管理
 */
public class ResultUtils {
    
    /**
     * 成功返回（无数据）
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success() {
        return (Result<T>) Result.success();
    }
    
    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.success(data);
    }
    
    /**
     * 使用 ResultCode 返回错误
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return Result.error(resultCode.getCode(), resultCode.getMessage());
    }
    
    /**
     * 使用 ResultCode 返回错误（自定义消息）
     */
    public static <T> Result<T> error(ResultCode resultCode, String customMessage) {
        return Result.error(resultCode.getCode(), customMessage);
    }
    
    /**
     * 使用 ResultCode 返回错误（带数据）
     */
    public static <T> Result<T> error(ResultCode resultCode, String customMessage, T data) {
        return Result.error(resultCode.getCode(), customMessage, data);
    }
    
    /**
     * 操作失败
     */
    public static <T> Result<T> operationFailed() {
        return error(ResultCode.OPERATION_FAILED);
    }
    
    /**
     * 操作失败（自定义消息）
     */
    public static <T> Result<T> operationFailed(String message) {
        return error(ResultCode.OPERATION_FAILED, message);
    }
    
    /**
     * 数据不存在
     */
    public static <T> Result<T> dataNotFound() {
        return error(ResultCode.DATA_NOT_FOUND);
    }
    
    /**
     * 数据不存在（自定义消息）
     */
    public static <T> Result<T> dataNotFound(String message) {
        return error(ResultCode.DATA_NOT_FOUND, message);
    }
    
    /**
     * 数据已存在
     */
    public static <T> Result<T> dataAlreadyExists() {
        return error(ResultCode.DATA_ALREADY_EXISTS);
    }
    
    /**
     * 数据已存在（自定义消息）
     */
    public static <T> Result<T> dataAlreadyExists(String message) {
        return error(ResultCode.DATA_ALREADY_EXISTS, message);
    }
    
    /**
     * 参数错误
     */
    public static <T> Result<T> paramError(String message) {
        return error(ResultCode.PARAM_ERROR, message);
    }
    
    /**
     * 库存不足
     */
    public static <T> Result<T> stockInsufficient() {
        return error(ResultCode.STOCK_INSUFFICIENT);
    }
    
    /**
     * 库存不足（自定义消息）
     */
    public static <T> Result<T> stockInsufficient(String message) {
        return error(ResultCode.STOCK_INSUFFICIENT, message);
    }
    
    /**
     * 订单状态异常
     */
    public static <T> Result<T> orderStatusError(String message) {
        return error(ResultCode.ORDER_STATUS_ERROR, message);
    }
    
    /**
     * 支付失败
     */
    public static <T> Result<T> paymentFailed(String message) {
        return error(ResultCode.PAYMENT_FAILED, message);
    }
    
    /**
     * 业务错误
     */
    public static <T> Result<T> businessError(String message) {
        return error(ResultCode.BUSINESS_ERROR, message);
    }
}

