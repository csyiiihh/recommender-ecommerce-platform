package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.StockIn;

@Mapper
public interface StockInMapper extends BaseMapper<StockIn> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM stock_in WHERE id = #{id} FOR UPDATE")
    StockIn selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
} 