package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.GroupBuyActivity;

/**
 * 拼团活动Mapper接口
 */
@Mapper
public interface GroupBuyActivityMapper extends BaseMapper<GroupBuyActivity> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM group_buy_activity WHERE id = #{id} FOR UPDATE")
    GroupBuyActivity selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}

