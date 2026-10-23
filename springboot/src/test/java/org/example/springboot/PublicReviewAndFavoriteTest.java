package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.entity.Order;
import org.example.springboot.mapper.*;
import org.example.springboot.service.*;
import org.example.springboot.dto.PublicReview;
import org.example.springboot.controller.FavoriteController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicReviewAndFavoriteTest {
    @Mock ReviewMapper reviews;
    @Mock UserMapper users;
    @Mock ProductMapper products;
    @Mock OrderMapper orders;
    @Mock FavoriteMapper favorites;
    @Mock AccessService access;
    @InjectMocks ReviewService service;
    @InjectMocks FavoriteService favoriteService;
    @BeforeAll static void metadata() {
        var builder=new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"test");
        for(Class<?> type:List.of(Review.class,Favorite.class,Order.class,Product.class,User.class))com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder,type);
    }
    Product product() { Product p=new Product();p.setId(2L);p.setStatus(1);return p; }
    Review input() { Review r=new Review();r.setId(99L);r.setUserId(1L);r.setProductId(2L);r.setRating(5);r.setStatus(1);r.setContent(" 好书 ");return r; }
    void eligibleUser() { User u=new User();u.setId(1L);when(users.selectForUpdate(1L)).thenReturn(u);when(products.selectById(2L)).thenReturn(product()); }
    @Test void clientCannotApproveOwnReviewOrForgeItsIdentityAndTime() {
        eligibleUser();when(orders.selectCount(any())).thenReturn(1L);when(reviews.selectCount(any())).thenReturn(0L);when(reviews.insert(any(Review.class))).thenReturn(1);
        Review r=input();r.setCreatedAt(new java.sql.Timestamp(0));assertEquals("0",service.createReview(r).getCode());assertEquals(0,r.getStatus());assertNull(r.getId());assertTrue(r.getCreatedAt().getTime()>0);assertEquals("好书",r.getContent());
    }
    @Test void unpurchasedBookCannotBeReviewed() {
        eligibleUser();when(orders.selectCount(any())).thenReturn(0L);assertNotEquals("0",service.createReview(input()).getCode());verify(reviews,never()).insert(any(Review.class));
    }
    @Test void concurrentResubmissionGuardUsesUserLockAndRejectsExistingPendingReview() {
        eligibleUser();when(orders.selectCount(any())).thenReturn(1L);when(reviews.selectCount(any())).thenReturn(1L);
        assertNotEquals("0",service.createReview(input()).getCode());verify(reviews,never()).insert(any(Review.class));verify(users).selectForUpdate(1L);
    }
    @Test void publicQueryIgnoresClientModerationFilterAndReturnsSafeReviewer() throws Exception {
        when(products.selectById(2L)).thenReturn(product());Review review=input();review.setStatus(1);when(reviews.selectList(any())).thenReturn(List.of(review));
        User u=new User();u.setId(1L);u.setUsername("login@example.com");u.setEmail("private@example.com");u.setBusinessLicense("/img/private.png");u.setRole("MERCHANT");when(users.selectBatchIds(anyCollection())).thenReturn(List.of(u));
        var result=service.getReviewsByProductId(2L,0);String json=new ObjectMapper().writeValueAsString(result.getData());
        assertFalse(json.contains("private@example.com"));assertFalse(json.contains("login@example.com"));assertFalse(json.contains("private.png"));assertFalse(json.contains("businessLicense"));assertTrue(json.contains("书友"));
        @SuppressWarnings("unchecked") ArgumentCaptor<LambdaQueryWrapper<Review>> query=ArgumentCaptor.forClass(LambdaQueryWrapper.class);verify(reviews).selectList(query.capture());
        query.getValue().getSqlSegment();assertTrue(query.getValue().getParamNameValuePairs().containsValue(1));assertFalse(query.getValue().getParamNameValuePairs().containsValue(0));
        verify(products,times(1)).selectById(2L);verify(users,never()).selectById(any(java.io.Serializable.class));
    }
    @Test void unpublishedBookDoesNotExposeReviews() {
        Product p=product();p.setStatus(0);when(products.selectById(2L)).thenReturn(p);assertNotEquals("0",service.getReviewsByProductId(2L,null).getCode());verifyNoInteractions(reviews,users);
    }
    @Test void onlyPublicNicknameAndUserAvatarCanBeShown() throws Exception {
        when(products.selectById(2L)).thenReturn(product());when(reviews.selectList(any())).thenReturn(List.of(input()));User u=new User();u.setId(1L);u.setRole("USER");u.setName("读者甲");u.setBusinessLicense("/img/avatar.png");when(users.selectBatchIds(anyCollection())).thenReturn(List.of(u));
        var result=service.getReviewsByProductId(2L,null);var review=(PublicReview)((List<?>)result.getData()).get(0);assertEquals("读者甲",review.user().username());assertEquals("/api/img/avatar.png",review.user().avatar());
    }
    @Test void favoriteStateQueryDoesNotLoadUserOrProductEntities() {
        Favorite one=new Favorite();one.setProductId(2L);Favorite duplicate=new Favorite();duplicate.setProductId(2L);when(favorites.selectList(any())).thenReturn(List.of(one,duplicate));
        assertEquals(Set.of(2L),favoriteService.getActiveProductIds(1L).getData());verifyNoInteractions(users,products);
    }
    @Test void anotherUsersFavoriteStatesRemainProtected() {
        FavoriteService worker=mock(FavoriteService.class);FavoriteController controller=new FavoriteController();org.springframework.test.util.ReflectionTestUtils.setField(controller,"access",access);org.springframework.test.util.ReflectionTestUtils.setField(controller,"favoriteService",worker);
        doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(access).owner(2L);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.getFavoriteStatuses(2L));verifyNoInteractions(worker);
    }
}
