package org.example.springboot.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.example.springboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理系统异常，避免敏感信息泄露
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 是否为生产环境
     * 生产环境不暴露详细错误信息
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * 判断是否为生产环境
     */
    private boolean isProduction() {
        return "prod".equals(activeProfile) || "production".equals(activeProfile);
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("JSON解析失败", e);
        // 生产环境不暴露详细错误信息
        String message = isProduction() ? "请求数据格式错误" : "JSON解析失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        return Result.error("400", message);
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验失败", e);
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return Result.error("400", "参数校验失败: " + errors);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        log.error("参数绑定失败", e);
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return Result.error("400", "参数绑定失败: " + errors);
    }
    
    /**
     * 处理客户端断开连接异常（不影响使用，无需记录详细堆栈）
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public void handleClientAbortException(Exception e) {
        // 客户端主动断开连接（如刷新页面、取消请求），只记录简单日志
        log.debug("客户端断开连接: {}", e.getMessage());
        // 不返回任何内容，因为客户端已经断开
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        // 检查是否是客户端断开连接导致的异常
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ClientAbortException || 
                cause instanceof AsyncRequestNotUsableException ||
                cause.getClass().getName().contains("ClientAbortException")) {
                log.debug("客户端断开连接: {}", e.getMessage());
                return null;
            }
            cause = cause.getCause();
        }
        
        // 记录详细错误日志（包含堆栈信息）
        log.error("系统异常: {}", e.getMessage(), e);
        
        // 生产环境不暴露详细错误信息，避免泄露系统内部信息
        String message = isProduction() ? "系统内部错误，请联系管理员" : "系统错误: " + e.getMessage();
        return Result.error("500", message);
    }
}

