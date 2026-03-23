package org.example.springboot.common;

/**
 * 统一状态码枚举
 * 用于统一管理系统返回的状态码
 */
public enum ResultCode {
    
    /**
     * 成功
     */
    SUCCESS("0", "成功"),
    
    /**
     * 系统错误
     */
    ERROR("-1", "系统错误"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR("400", "参数错误"),
    
    /**
     * 未授权
     */
    UNAUTHORIZED("401", "未授权"),
    
    /**
     * 禁止访问
     */
    FORBIDDEN("403", "禁止访问"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND("404", "资源不存在"),
    
    /**
     * 服务器错误
     */
    INTERNAL_ERROR("500", "服务器错误"),
    
    /**
     * 业务异常
     */
    BUSINESS_ERROR("1000", "业务处理失败"),
    
    /**
     * 数据不存在
     */
    DATA_NOT_FOUND("1001", "数据不存在"),
    
    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS("1002", "数据已存在"),
    
    /**
     * 操作失败
     */
    OPERATION_FAILED("1003", "操作失败"),
    
    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT("2001", "库存不足"),
    
    /**
     * 订单状态异常
     */
    ORDER_STATUS_ERROR("2002", "订单状态异常"),
    
    /**
     * 支付失败
     */
    PAYMENT_FAILED("2003", "支付失败"),
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND("3001", "用户不存在"),
    
    /**
     * 用户名或密码错误
     */
    LOGIN_FAILED("3002", "用户名或密码错误"),
    
    /**
     * 账号被禁用
     */
    ACCOUNT_DISABLED("3003", "账号被禁用"),
    
    /**
     * 商品不存在
     */
    PRODUCT_NOT_FOUND("4001", "商品不存在"),
    
    /**
     * 分类不存在
     */
    CATEGORY_NOT_FOUND("4002", "分类不存在"),
    
    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND("5001", "订单不存在"),
    
    /**
     * 无法删除（存在关联数据）
     */
    CANNOT_DELETE("6001", "无法删除，存在关联数据");
    
    private final String code;
    private final String message;
    
    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}

