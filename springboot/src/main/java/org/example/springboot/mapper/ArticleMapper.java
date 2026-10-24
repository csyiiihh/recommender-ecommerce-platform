package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT * FROM article WHERE id = #{id} AND status = 1 FOR UPDATE")
    Article selectPublishedForUpdate(@Param("id") Long id);

    @Update("UPDATE article SET view_count = GREATEST(COALESCE(view_count, 0), 0) + 1 WHERE id = #{id} AND status = 1 AND COALESCE(view_count, 0) < 2147483647")
    int incrementPublishedViews(@Param("id") Long id);

} 