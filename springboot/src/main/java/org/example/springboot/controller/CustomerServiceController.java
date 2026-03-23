package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.springboot.common.Result;
import org.example.springboot.entity.CustomerServiceMessage;
import org.example.springboot.entity.CustomerServiceSession;
import org.example.springboot.service.CustomerServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客服功能控制器
 */
@RestController
@RequestMapping("/customerService")
@Validated
public class CustomerServiceController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceController.class);
    
    @Autowired
    private CustomerServiceService customerServiceService;
    
    /**
     * 用户获取或创建客服会话
     * @param userId 用户ID
     * @return 会话信息
     */
    @PostMapping("/session/create")
    public Result<?> createSession(@RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        try {
            CustomerServiceSession session = customerServiceService.getOrCreateSession(userId);
            return Result.success(session);
        } catch (Exception e) {
            LOGGER.error("创建客服会话失败", e);
            return Result.error("-1", "创建客服会话失败：" + e.getMessage());
        }
    }
    
    /**
     * 客服接入会话
     * @param sessionId 会话ID
     * @param serviceId 客服ID
     * @return 操作结果
     */
    @PostMapping("/session/accept")
    public Result<?> acceptSession(
            @RequestParam @NotNull(message = "会话ID不能为空") @Min(value = 1, message = "会话ID必须大于0") Long sessionId, 
            @RequestParam @NotNull(message = "客服ID不能为空") @Min(value = 1, message = "客服ID必须大于0") Long serviceId) {
        try {
            boolean success = customerServiceService.acceptSession(sessionId, serviceId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("-1", "接入会话失败");
            }
        } catch (Exception e) {
            LOGGER.error("接入会话失败", e);
            return Result.error("-1", "接入会话失败：" + e.getMessage());
        }
    }
    
    /**
     * 结束会话
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @PostMapping("/session/end")
    public Result<?> endSession(@RequestParam @NotNull(message = "会话ID不能为空") @Min(value = 1, message = "会话ID必须大于0") Long sessionId) {
        try {
            boolean success = customerServiceService.endSession(sessionId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("-1", "结束会话失败");
            }
        } catch (Exception e) {
            LOGGER.error("结束会话失败", e);
            return Result.error("-1", "结束会话失败：" + e.getMessage());
        }
    }
    
    /**
     * 发送消息
     * @param params 消息参数（sessionId, senderId, senderType, content, messageType）
     * @return 消息信息
     */
    @PostMapping("/message/send")
    public Result<?> sendMessage(@RequestBody Map<String, Object> params) {
        try {
            Long sessionId = Long.valueOf(params.get("sessionId").toString());
            Long senderId = Long.valueOf(params.get("senderId").toString());
            String senderType = params.get("senderType").toString();
            String content = params.get("content").toString();
            String messageType = params.getOrDefault("messageType", "TEXT").toString();
            
            CustomerServiceMessage message = customerServiceService.sendMessage(
                    sessionId, senderId, senderType, content, messageType);
            return Result.success(message);
        } catch (Exception e) {
            LOGGER.error("发送消息失败", e);
            return Result.error("-1", "发送消息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取会话消息列表
     * @param sessionId 会话ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型（USER/SERVICE）
     * @return 消息列表
     */
    @GetMapping("/message/list")
    public Result<?> getMessages(
            @RequestParam @NotNull(message = "会话ID不能为空") @Min(value = 1, message = "会话ID必须大于0") Long sessionId,
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long currentUserId,
            @RequestParam @NotBlank(message = "用户类型不能为空") String userType) {
        try {
            List<CustomerServiceMessage> messages = customerServiceService.getMessages(
                    sessionId, currentUserId, userType);
            return Result.success(messages);
        } catch (Exception e) {
            LOGGER.error("获取消息列表失败", e);
            return Result.error("-1", "获取消息列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户的会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping("/session/user")
    public Result<?> getUserSessions(@RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId) {
        try {
            List<CustomerServiceSession> sessions = customerServiceService.getUserSessions(userId);
            return Result.success(sessions);
        } catch (Exception e) {
            LOGGER.error("获取用户会话列表失败", e);
            return Result.error("-1", "获取用户会话列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取客服的会话列表
     * @param serviceId 客服ID
     * @return 会话列表
     */
    @GetMapping("/session/service")
    public Result<?> getServiceSessions(@RequestParam @NotNull(message = "客服ID不能为空") @Min(value = 1, message = "客服ID必须大于0") Long serviceId) {
        try {
            List<CustomerServiceSession> sessions = customerServiceService.getServiceSessions(serviceId);
            return Result.success(sessions);
        } catch (Exception e) {
            LOGGER.error("获取客服会话列表失败", e);
            return Result.error("-1", "获取客服会话列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有会话（管理员）
     * @return 会话列表
     */
    @GetMapping("/session/all")
    public Result<?> getAllSessions() {
        try {
            List<CustomerServiceSession> sessions = customerServiceService.getAllSessions();
            return Result.success(sessions);
        } catch (Exception e) {
            LOGGER.error("获取所有会话列表失败", e);
            return Result.error("-1", "获取所有会话列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询会话历史
     * @param status 会话状态（可选）
     * @param userId 用户ID（可选）
     * @param currentPage 当前页
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/session/history")
    public Result<?> getSessionHistory(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "当前页必须大于0") Integer currentPage,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于0") Integer pageSize) {
        try {
            Page<CustomerServiceSession> page = customerServiceService.getSessionHistory(
                    status, userId, currentPage, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            LOGGER.error("获取会话历史失败", e);
            return Result.error("-1", "获取会话历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取未读消息统计
     * @param userId 用户ID
     * @param userType 用户类型（USER/SERVICE）
     * @return 未读消息数
     */
    @GetMapping("/message/unread")
    public Result<?> getUnreadCount(
            @RequestParam @NotNull(message = "用户ID不能为空") @Min(value = 1, message = "用户ID必须大于0") Long userId,
            @RequestParam @NotBlank(message = "用户类型不能为空") String userType) {
        try {
            Integer count = customerServiceService.getUnreadCount(userId, userType);
            return Result.success(count);
        } catch (Exception e) {
            LOGGER.error("获取未读消息数失败", e);
            return Result.error("-1", "获取未读消息数失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除会话记录（管理员）
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<?> deleteSession(@PathVariable @NotNull(message = "会话ID不能为空") @Min(value = 1, message = "会话ID必须大于0") Long sessionId) {
        try {
            boolean success = customerServiceService.deleteSession(sessionId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("-1", "删除会话失败");
            }
        } catch (Exception e) {
            LOGGER.error("删除会话失败", e);
            return Result.error("-1", "删除会话失败：" + e.getMessage());
        }
    }
}

