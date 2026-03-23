package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.GroupBuyTeam;

/**
 * 拼团Mapper接口
 */
@Mapper
public interface GroupBuyTeamMapper extends BaseMapper<GroupBuyTeam> {
    
    /**
     * 行锁查询（防止并发问题）
     * 用于参团时锁定团记录，防止超员
     */
    @Select("SELECT * FROM group_buy_team WHERE id = #{id} FOR UPDATE")
    GroupBuyTeam selectForUpdate(Long id);
}

