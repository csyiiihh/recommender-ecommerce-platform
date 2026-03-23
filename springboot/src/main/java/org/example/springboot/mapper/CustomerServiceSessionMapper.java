package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.CustomerServiceSession;

import java.util.List;

@Mapper
public interface CustomerServiceSessionMapper extends BaseMapper<CustomerServiceSession> {
    
    /**
     * 查询用户的会话列表（包含用户和客服信息）
     */
    @Select("SELECT s.*, " +
            "u.id as user_id_info, u.username as user_username, u.name as user_name, u.role as user_role, u.business_license as user_business_license, " +
            "srv.id as service_id_info, srv.username as service_username, srv.name as service_name, srv.role as service_role, srv.business_license as service_business_license " +
            "FROM customer_service_session s " +
            "LEFT JOIN user u ON s.user_id = u.id " +
            "LEFT JOIN user srv ON s.service_id = srv.id " +
            "WHERE s.user_id = #{userId} " +
            "ORDER BY s.last_message_time DESC")
    List<CustomerServiceSession> selectSessionsByUserId(Long userId);
    
    /**
     * 查询客服的会话列表（包含用户和客服信息）
     */
    @Select("SELECT s.*, " +
            "u.id as user_id_info, u.username as user_username, u.name as user_name, u.role as user_role, u.business_license as user_business_license, " +
            "srv.id as service_id_info, srv.username as service_username, srv.name as service_name, srv.role as service_role, srv.business_license as service_business_license " +
            "FROM customer_service_session s " +
            "LEFT JOIN user u ON s.user_id = u.id " +
            "LEFT JOIN user srv ON s.service_id = srv.id " +
            "WHERE (s.service_id = #{serviceId} OR s.service_id IS NULL) " +
            "ORDER BY s.status ASC, s.last_message_time DESC")
    List<CustomerServiceSession> selectSessionsByServiceId(Long serviceId);
    
    /**
     * 查询所有会话（管理员查看）
     */
    @Select("SELECT s.*, " +
            "u.id as user_id_info, u.username as user_username, u.name as user_name, u.role as user_role, u.business_license as user_business_license, " +
            "srv.id as service_id_info, srv.username as service_username, srv.name as service_name, srv.role as service_role, srv.business_license as service_business_license " +
            "FROM customer_service_session s " +
            "LEFT JOIN user u ON s.user_id = u.id " +
            "LEFT JOIN user srv ON s.service_id = srv.id " +
            "ORDER BY s.last_message_time DESC")
    List<CustomerServiceSession> selectAllSessions();
}

