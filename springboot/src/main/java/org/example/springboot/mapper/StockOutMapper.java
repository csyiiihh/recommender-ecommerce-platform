package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.StockOut;

@Mapper
public interface StockOutMapper extends BaseMapper<StockOut> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM stock_out WHERE id = #{id} FOR UPDATE")
    StockOut selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
} 