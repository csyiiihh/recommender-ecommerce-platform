package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Review;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    @org.apache.ibatis.annotations.Select("SELECT rating, COUNT(*) AS count FROM review WHERE product_id = #{productId} AND status = 1 AND rating BETWEEN 1 AND 5 GROUP BY rating")
    java.util.List<org.example.springboot.dto.ReviewRatingCount> countPublicRatings(@org.apache.ibatis.annotations.Param("productId") Long productId);
} 