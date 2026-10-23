package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Update("UPDATE product SET stock = stock - #{quantity}, sales_count = COALESCE(sales_count, 0) + #{quantity} WHERE id = #{id} AND status = 1 AND stock >= #{quantity} AND #{quantity} > 0")
    int deductStock(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("quantity") Integer quantity);
    @org.apache.ibatis.annotations.Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{id} AND status = 1 AND stock >= #{quantity} AND #{quantity} > 0")
    int reserveStock(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("quantity") int quantity);
    @org.apache.ibatis.annotations.Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{id} AND #{quantity} > 0")
    int releaseReservedStock(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("quantity") int quantity);
    @org.apache.ibatis.annotations.Update("UPDATE product SET sales_count = COALESCE(sales_count, 0) + #{quantity} WHERE id = #{id} AND #{quantity} > 0")
    int recordReservedSale(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("quantity") int quantity);
} 