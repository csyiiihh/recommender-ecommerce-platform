package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.springboot.entity.Logistics;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogisticsMapper extends BaseMapper<Logistics> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM logistics WHERE id = #{id} FOR UPDATE")
    Logistics selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
} 