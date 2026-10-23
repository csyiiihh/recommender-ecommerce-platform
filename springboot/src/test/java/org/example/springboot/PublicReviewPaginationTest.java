package org.example.springboot;

import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.service.ReviewService;
import org.example.springboot.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicReviewPaginationTest {
    @Mock ReviewMapper reviews; @Mock ProductMapper products; @Mock UserMapper users;
    @InjectMocks ReviewService service;
    @BeforeAll static void metadata() {
        var builder = new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "pagination-test");
        for (Class<?> type : List.of(Review.class, Product.class, User.class)) com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder, type);
    }
    Review review() { Review r = new Review(); r.setId(8L); r.setProductId(2L); r.setUserId(7L); r.setRating(5); r.setContent("公开评价"); return r; }
    ReviewRatingCount count(int rating, long count) { var r = new ReviewRatingCount(); r.setRating(rating); r.setCount(count); return r; }
    void available(List<Review> records, long total, List<ReviewRatingCount> counts) {
        Product p = new Product(); p.setId(2L); p.setStatus(1); when(products.selectById(2L)).thenReturn(p);
        when(reviews.selectPage(any(Page.class), any())).thenAnswer(i -> { Page<Review> page = i.getArgument(0); page.setRecords(records); page.setTotal(total); return page; });
        when(reviews.countPublicRatings(2L)).thenReturn(counts);
    }
    @Test void pageUsesRequestedBoundsAndSummaryCoversAllPublishedRows() {
        available(List.of(review()), 23, List.of(count(5, 20), count(4, 3)));
        var data = (PublicReviewPage) service.getPublicReviewPage(2L, 2, 5, "latest").getData();
        assertEquals(2, data.current()); assertEquals(5, data.size()); assertEquals(5, data.pages()); assertEquals(23, data.total()); assertEquals(1, data.records().size());
        assertEquals(23, data.summary().total()); assertEquals(new BigDecimal("4.9"), data.summary().average()); assertEquals(20L, data.summary().distribution().get(5)); assertEquals(0L, data.summary().distribution().get(1));
        verify(reviews, never()).selectList(any());
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 51, Integer.MAX_VALUE})
    void invalidSizeNeverQueriesDatabase(int size) { assertNotEquals("0", service.getPublicReviewPage(2L, 1, size, "latest").getCode()); verifyNoInteractions(products, reviews, users); }
    @Test void invalidPageAndProductNeverQueryDatabase() {
        assertNotEquals("0", service.getPublicReviewPage(2L, 0, 10, "latest").getCode()); assertNotEquals("0", service.getPublicReviewPage(null, 1, 10, "latest").getCode()); assertNotEquals("0", service.getPublicReviewPage(-1L, 1, 10, "latest").getCode()); verifyNoInteractions(products, reviews, users);
    }
    @Test void arbitrarySortCannotBecomeSql() { assertNotEquals("0", service.getPublicReviewPage(2L, 1, 10, "rating; DROP TABLE review").getCode()); assertNotEquals("0", service.getPublicReviewPage(2L, 1, 10, null).getCode()); verifyNoInteractions(products, reviews, users); }
    @Test void missingProductCannotRevealReviewExistence() { assertNotEquals("0", service.getPublicReviewPage(2L, 1, 10, "latest").getCode()); verifyNoInteractions(reviews, users); }
    @Test void delistedProductCannotRevealReviewsOrSummary() { Product p = new Product(); p.setStatus(0); when(products.selectById(2L)).thenReturn(p); assertNotEquals("0", service.getPublicReviewPage(2L, 1, 10, "latest").getCode()); verifyNoInteractions(reviews, users); }
    @Test void emptyPublishedReviewsHaveZeroSummaryAndNoUserLookup() {
        available(List.of(), 0, List.of()); var data = (PublicReviewPage) service.getPublicReviewPage(2L, 1, 10, "latest").getData();
        assertTrue(data.records().isEmpty()); assertEquals(BigDecimal.ZERO, data.summary().average()); assertEquals(5, data.summary().distribution().size()); verifyNoInteractions(users);
    }
    @Test void pageDoesNotExposePrivateUserFieldsOrMerchantLicense() throws Exception {
        available(List.of(review()), 1, List.of(count(5, 1))); User u = new User(); u.setId(7L); u.setUsername("secret-login"); u.setEmail("private@example.com"); u.setBusinessLicense("/img/license.png"); u.setRole("MERCHANT"); u.setName("公开昵称"); when(users.selectBatchIds(anyCollection())).thenReturn(List.of(u));
        var data = (PublicReviewPage) service.getPublicReviewPage(2L, 1, 10, "latest").getData(); String json = new ObjectMapper().writeValueAsString(data);
        assertFalse(json.contains("secret-login")); assertFalse(json.contains("private@example.com")); assertFalse(json.contains("license.png")); assertFalse(json.contains("businessLicense")); assertEquals("公开昵称", data.records().get(0).user().username()); verify(users).selectBatchIds(Set.of(7L));
    }
    @ParameterizedTest @ValueSource(strings = {"latest", "rating_desc", "rating_asc"})
    void approvedRatingRangeAndStableTieBreakingApplyToEverySort(String sort) {
        available(List.of(), 0, List.of()); service.getPublicReviewPage(2L, 1, 10, sort);
        @SuppressWarnings("unchecked") ArgumentCaptor<LambdaQueryWrapper<Review>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class); verify(reviews).selectPage(any(Page.class), query.capture());
        String sql = query.getValue().getSqlSegment(); assertTrue(sql.contains("product_id =")); assertTrue(sql.contains("status =")); assertTrue(sql.contains("rating BETWEEN")); assertTrue(sql.contains("created_at DESC,id DESC"));
        if (!"latest".equals(sort)) assertTrue(sql.contains("rating " + (sort.endsWith("desc") ? "DESC" : "ASC")));
    }
    @Test void outOfRangePageStillHasWholeProductSummary() {
        available(List.of(), 2, List.of(count(5, 1), count(4, 1))); var data = (PublicReviewPage) service.getPublicReviewPage(2L, 99, 10, "latest").getData();
        assertTrue(data.records().isEmpty()); assertEquals(2, data.summary().total()); assertEquals(new BigDecimal("4.5"), data.summary().average()); verifyNoInteractions(users);
    }
}
