package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.CustomerServiceMessage;

import java.util.List;

@Mapper
public interface CustomerServiceMessageMapper extends BaseMapper<CustomerServiceMessage> {
    
    /**
     * 查询会话的所有消息（包含发送者信息）
     */
    @Select("SELECT m.*, " +
            "u.username as sender_username, u.name as sender_name, u.role as sender_role " +
            "FROM customer_service_message m " +
            "LEFT JOIN user u ON m.sender_id = u.id " +
            "WHERE m.session_id = #{sessionId} " +
            "ORDER BY m.created_at ASC")
    List<CustomerServiceMessage> selectMessagesBySessionId(Long sessionId);
    
    /**
     * 查询会话的最近N条消息（用于AI上下文）
     */
    @Select("SELECT m.* " +
            "FROM customer_service_message m " +
            "WHERE m.session_id = #{sessionId} " +
            "ORDER BY m.created_at DESC " +
            "LIMIT #{limit}")
    List<CustomerServiceMessage> selectRecentMessages(Long sessionId, Integer limit);
}

