package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.GroupBuyMember;

/**
 * 拼团成员Mapper接口
 */
@Mapper
public interface GroupBuyMemberMapper extends BaseMapper<GroupBuyMember> {
}

