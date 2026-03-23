package org.example.springboot.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 通过用户账号（username）查询用户ID
     */
    @Select("SELECT id FROM user WHERE username = #{username} LIMIT 1")
    Long selectIdByUsername(@Param("username") String username);
}
