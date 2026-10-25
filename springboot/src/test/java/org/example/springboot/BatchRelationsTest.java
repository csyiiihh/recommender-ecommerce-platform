package org.example.springboot;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BatchRelationsTest {
 @Test void cartListLoadsEachDistinctRelationOnlyOnce() {
  CartMapper carts=mock(CartMapper.class);UserMapper users=mock(UserMapper.class);ProductMapper products=mock(ProductMapper.class);CartService s=new CartService();
  ReflectionTestUtils.setField(s,"cartMapper",carts);ReflectionTestUtils.setField(s,"userMapper",users);ReflectionTestUtils.setField(s,"productMapper",products);
  Cart a=new Cart();a.setUserId(1L);a.setProductId(2L);Cart b=new Cart();b.setUserId(1L);b.setProductId(2L);
  when(carts.selectList(any())).thenReturn(List.of(a,b));User u=new User();u.setId(1L);Product p=new Product();p.setId(2L);
  when(users.selectBatchIds(Set.of(1L))).thenReturn(List.of(u));when(products.selectBatchIds(Set.of(2L))).thenReturn(List.of(p));
  assertEquals("0",s.getCartByUserId(1L).getCode());assertSame(u,a.getUser());assertSame(p,b.getProduct());
  verify(users,times(1)).selectBatchIds(any());verify(products,times(1)).selectBatchIds(any());verify(users,never()).selectById(anyLong());verify(products,never()).selectById(anyLong());
 }
 @Test void reviewPageKeepsMissingRelationsAndAvoidsRowQueries() {
  ReviewMapper reviews=mock(ReviewMapper.class);UserMapper users=mock(UserMapper.class);ProductMapper products=mock(ProductMapper.class);ReviewService s=new ReviewService();
  ReflectionTestUtils.setField(s,"reviewMapper",reviews);ReflectionTestUtils.setField(s,"userMapper",users);ReflectionTestUtils.setField(s,"productMapper",products);
  Review a=new Review();a.setUserId(1L);a.setProductId(2L);Review b=new Review();b.setUserId(1L);b.setProductId(3L);
  when(reviews.selectPage(any(),any())).thenReturn(new Page<Review>(1,10).setRecords(List.of(a,b)));User u=new User();u.setId(1L);when(users.selectBatchIds(any())).thenReturn(List.of(u));
  assertEquals("0",s.getReviewsByPage(null,null,null,null,null,null,1,10).getCode());assertSame(u,a.getUser());assertNull(b.getProduct());
  verify(users,times(1)).selectBatchIds(any());verify(products,times(1)).selectBatchIds(any());verify(products,never()).selectById(anyLong());
 }
}
