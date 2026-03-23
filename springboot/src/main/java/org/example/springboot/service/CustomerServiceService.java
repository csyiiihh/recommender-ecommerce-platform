package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.ResultCode;
import org.example.springboot.entity.CustomerServiceMessage;
import org.example.springboot.entity.CustomerServiceSession;
import org.example.springboot.entity.User;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.CustomerServiceMessageMapper;
import org.example.springboot.mapper.CustomerServiceSessionMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.List;

/**
 * 客服功能服务类
 */
@Service
public class CustomerServiceService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceService.class);
    
    @Autowired
    private CustomerServiceSessionMapper sessionMapper;
    
    @Autowired
    private CustomerServiceMessageMapper messageMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private AICustomerService aiCustomerService;
    
    /**
     * 创建或获取用户的客服会话
     */
    @Transactional
    public CustomerServiceSession getOrCreateSession(Long userId) {
        if (userId == null) {
            LOGGER.warn("创建客服会话失败，userId 为空");
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            LOGGER.warn("创建客服会话失败，用户不存在，userId={}", userId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 查找用户是否已有进行中的会话
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerServiceSession::getUserId, userId)
               .in(CustomerServiceSession::getStatus, 0, 1)
               .orderByDesc(CustomerServiceSession::getCreatedAt)
               .last("LIMIT 1");

        CustomerServiceSession session = sessionMapper.selectOne(wrapper);

        if (session == null) {
            // 创建新会话
            session = CustomerServiceSession.builder()
                    .userId(userId)
                    .status(0) // 待接入
                    .unreadUser(0)
                    .unreadService(0)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();
            sessionMapper.insert(session);
            LOGGER.info("创建新的客服会话成功，userId={}，sessionId={}", userId, session.getId());
        } else {
            LOGGER.info("获取已有客服会话，userId={}，sessionId={}", userId, session.getId());
        }

        // 加载用户信息
        session.setUser(user);

        if (session.getServiceId() != null) {
            User service = userMapper.selectById(session.getServiceId());
            session.setService(service);
        }

        return session;
    }
    
    /**
     * 客服接入会话
     */
    @Transactional
    public boolean acceptSession(Long sessionId, Long serviceId) {
        if (sessionId == null || serviceId == null) {
            LOGGER.warn("接入会话失败，参数为空，sessionId={}, serviceId={}", sessionId, serviceId);
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID和客服ID不能为空");
        }

        CustomerServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            LOGGER.warn("接入会话失败，会话不存在，sessionId={}", sessionId);
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        session.setServiceId(serviceId);
        session.setStatus(1); // 进行中

        boolean updated = sessionMapper.updateById(session) > 0;
        if (!updated) {
            LOGGER.error("接入会话失败，更新数据库失败，sessionId={}", sessionId);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "接入会话失败");
        }

        LOGGER.info("客服接入会话成功，sessionId={}，serviceId={}", sessionId, serviceId);
        return true;
    }
    
    /**
     * 结束会话
     */
    @Transactional
    public boolean endSession(Long sessionId) {
        if (sessionId == null) {
            LOGGER.warn("结束会话失败，sessionId 为空");
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }

        CustomerServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            LOGGER.warn("结束会话失败，会话不存在，sessionId={}", sessionId);
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        session.setStatus(2); // 已结束
        boolean updated = sessionMapper.updateById(session) > 0;
        if (!updated) {
            LOGGER.error("结束会话失败，更新数据库失败，sessionId={}", sessionId);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "结束会话失败");
        }

        LOGGER.info("结束会话成功，sessionId={}", sessionId);
        return true;
    }
    
    /**
     * 发送消息
     */
    @Transactional
    public CustomerServiceMessage sendMessage(Long sessionId, Long senderId, String senderType,
                                             String content, String messageType) {
        if (sessionId == null || senderId == null || !StringUtils.hasText(senderType) || !StringUtils.hasText(content)) {
            LOGGER.warn("发送消息失败，参数不完整，sessionId={}, senderId={}, senderType={}", sessionId, senderId, senderType);
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID、发送者、发送者类型和内容不能为空");
        }

        CustomerServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            LOGGER.warn("发送消息失败，会话不存在，sessionId={}", sessionId);
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        User sender = userMapper.selectById(senderId);
        if (sender == null) {
            LOGGER.warn("发送消息失败，发送者不存在，senderId={}", senderId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "发送者不存在");
        }

        CustomerServiceMessage message = CustomerServiceMessage.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .senderType(senderType)
                .content(content)
                .messageType(messageType)
                .isRead(0)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        int inserted = messageMapper.insert(message);
        if (inserted <= 0) {
            LOGGER.error("发送消息失败，写入数据库失败，sessionId={}, senderId={}", sessionId, senderId);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "发送消息失败");
        }

        // 更新会话信息
        session.setLastMessage(content);
        session.setLastMessageTime(message.getCreatedAt());

        // 更新未读数
        if ("USER".equalsIgnoreCase(senderType)) {
            session.setUnreadService(session.getUnreadService() + 1);
        } else {
            session.setUnreadUser(session.getUnreadUser() + 1);
        }

        sessionMapper.updateById(session);

        // 加载发送者信息
        message.setSender(sender);

        // 如果用户发送消息且AI客服已启用，异步生成AI回复
        if ("USER".equalsIgnoreCase(senderType) && aiCustomerService != null) {
            try {
                aiCustomerService.sendAIReplyAsync(sessionId, senderId, content);
            } catch (Exception e) {
                LOGGER.error("触发AI客服回复失败，sessionId={}, senderId={}", sessionId, senderId, e);
            }
        }

        LOGGER.info("发送客服消息成功，sessionId={}，senderId={}，senderType={}", sessionId, senderId, senderType);
        return message;
    }
    
    /**
     * 获取会话的消息列表
     */
    public List<CustomerServiceMessage> getMessages(Long sessionId, Long currentUserId, String userType) {
        if (sessionId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }

        List<CustomerServiceMessage> messages = messageMapper.selectMessagesBySessionId(sessionId);

        // 构建用户对象（从查询结果中提取）
        for (CustomerServiceMessage message : messages) {
            User sender = new User();
            sender.setId(message.getSenderId());
            message.setSender(sender);
        }

        // 标记消息为已读
        markMessagesAsRead(sessionId, currentUserId, userType);

        return messages;
    }
    
    /**
     * 标记消息为已读
     */
    @Transactional
    public void markMessagesAsRead(Long sessionId, Long currentUserId, String userType) {
        if (sessionId == null || currentUserId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID和当前用户ID不能为空");
        }

        CustomerServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        // 标记消息为已读
        LambdaUpdateWrapper<CustomerServiceMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CustomerServiceMessage::getSessionId, sessionId)
               .ne(CustomerServiceMessage::getSenderId, currentUserId)
               .eq(CustomerServiceMessage::getIsRead, 0)
               .set(CustomerServiceMessage::getIsRead, 1);

        messageMapper.update(null, wrapper);

        // 更新会话未读数
        if ("USER".equalsIgnoreCase(userType)) {
            session.setUnreadUser(0);
        } else {
            session.setUnreadService(0);
        }
        sessionMapper.updateById(session);
    }
    
    /**
     * 获取用户的会话列表
     */
    public List<CustomerServiceSession> getUserSessions(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }

        List<CustomerServiceSession> sessions = sessionMapper.selectSessionsByUserId(userId);

        // 加载完整的用户信息（包括头像）
        for (CustomerServiceSession session : sessions) {
            if (session.getUserId() != null) {
                User user = userMapper.selectById(session.getUserId());
                session.setUser(user);
            }
            if (session.getServiceId() != null) {
                User service = userMapper.selectById(session.getServiceId());
                session.setService(service);
            }
        }

        return sessions;
    }
    
    /**
     * 获取客服的会话列表
     */
    public List<CustomerServiceSession> getServiceSessions(Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "客服ID不能为空");
        }

        List<CustomerServiceSession> sessions = sessionMapper.selectSessionsByServiceId(serviceId);

        // 加载完整的用户信息（包括头像）
        for (CustomerServiceSession session : sessions) {
            if (session.getUserId() != null) {
                User user = userMapper.selectById(session.getUserId());
                session.setUser(user);
            }
            if (session.getServiceId() != null) {
                User service = userMapper.selectById(session.getServiceId());
                session.setService(service);
            }
        }

        return sessions;
    }
    
    /**
     * 获取所有会话（管理员）
     */
    public List<CustomerServiceSession> getAllSessions() {
        List<CustomerServiceSession> sessions = sessionMapper.selectAllSessions();

        // 加载完整的用户信息（包括头像）
        for (CustomerServiceSession session : sessions) {
            if (session.getUserId() != null) {
                User user = userMapper.selectById(session.getUserId());
                session.setUser(user);
            }
            if (session.getServiceId() != null) {
                User service = userMapper.selectById(session.getServiceId());
                session.setService(service);
            }
        }

        return sessions;
    }
    
    /**
     * 分页查询会话历史
     */
    public Page<CustomerServiceSession> getSessionHistory(Integer status, Long userId,
                                                          Integer currentPage, Integer pageSize) {
        if (currentPage == null || currentPage <= 0 || pageSize == null || pageSize <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分页参数不合法");
        }

        Page<CustomerServiceSession> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(CustomerServiceSession::getStatus, status);
        }
        if (userId != null) {
            wrapper.and(w -> w.eq(CustomerServiceSession::getUserId, userId)
                             .or()
                             .eq(CustomerServiceSession::getServiceId, userId));
        }

        wrapper.orderByDesc(CustomerServiceSession::getLastMessageTime);

        Page<CustomerServiceSession> result = sessionMapper.selectPage(page, wrapper);

        // 加载用户和客服信息
        for (CustomerServiceSession session : result.getRecords()) {
            if (session.getUserId() != null) {
                User user = userMapper.selectById(session.getUserId());
                session.setUser(user);
            }
            if (session.getServiceId() != null) {
                User service = userMapper.selectById(session.getServiceId());
                session.setService(service);
            }
        }

        return result;
    }
    
    /**
     * 获取未读消息统计
     */
    public Integer getUnreadCount(Long userId, String userType) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }

        LambdaQueryWrapper<CustomerServiceSession> wrapper = new LambdaQueryWrapper<>();

        if ("USER".equalsIgnoreCase(userType)) {
            wrapper.eq(CustomerServiceSession::getUserId, userId)
                   .gt(CustomerServiceSession::getUnreadUser, 0);
        } else {
            wrapper.and(w -> w.eq(CustomerServiceSession::getServiceId, userId)
                             .or()
                             .isNull(CustomerServiceSession::getServiceId))
                   .gt(CustomerServiceSession::getUnreadService, 0);
        }

        return sessionMapper.selectCount(wrapper).intValue();
    }
    
    /**
     * 删除会话记录（级联删除相关消息）
     */
    @Transactional
    public boolean deleteSession(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }

        CustomerServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        // 由于数据库设置了外键级联删除，删除会话时会自动删除相关消息
        int result = sessionMapper.deleteById(sessionId);
        if (result <= 0) {
            throw new BusinessException(ResultCode.CANNOT_DELETE, "删除会话失败");
        }

        LOGGER.info("删除客服会话成功，sessionId={}", sessionId);
        return true;
    }
}

