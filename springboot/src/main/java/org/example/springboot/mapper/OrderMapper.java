package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Order;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM \u0060order\u0060 WHERE id = #{id} FOR UPDATE")
    Order selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT * FROM \u0060order\u0060 WHERE out_trade_no = #{tradeNo} ORDER BY id FOR UPDATE")
    java.util.List<Order> selectTradeForUpdate(@org.apache.ibatis.annotations.Param("tradeNo") String tradeNo);
    @org.apache.ibatis.annotations.Update("UPDATE `order` SET recv_name = #{name}, recv_address = #{address}, recv_phone = #{phone}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status IN (0, 1)")
    int updateDeliveryIfUnshipped(@org.apache.ibatis.annotations.Param("id") Long id,
        @org.apache.ibatis.annotations.Param("name") String name, @org.apache.ibatis.annotations.Param("address") String address,
        @org.apache.ibatis.annotations.Param("phone") String phone);
    // The conditional update competes atomically with payment's order row locks.
    @org.apache.ibatis.annotations.Update("UPDATE `order` SET last_status = 0, status = 4, updated_at = CURRENT_TIMESTAMP "
        + "WHERE status = 0 AND order_type = 0 AND trade_no IS NULL AND pay_time IS NULL "
        + "AND created_at <= #{cutoff} ORDER BY created_at, id LIMIT #{batchSize}")
    int closeExpiredUnpaid(@org.apache.ibatis.annotations.Param("cutoff") java.sql.Timestamp cutoff,
        @org.apache.ibatis.annotations.Param("batchSize") int batchSize);
} 