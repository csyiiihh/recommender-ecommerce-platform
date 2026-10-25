package org.example.springboot;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.service.RecommendService;
import org.example.springboot.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationConsistencyTest {
    OrderMapper orders; FavoriteMapper favorites; ProductMapper products; RecommendationFeedbackMapper feedback;
    RecommendService service;
    @BeforeAll static void metadata() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "recommendation-test");
        for (Class<?> type : List.of(Order.class, Favorite.class, Product.class, RecommendationFeedback.class)) TableInfoHelper.initTableInfo(assistant, type);
    }
    @BeforeEach void setup() {
        orders=mock(OrderMapper.class); favorites=mock(FavoriteMapper.class); products=mock(ProductMapper.class); feedback=mock(RecommendationFeedbackMapper.class);
        service=new RecommendService();
        ReflectionTestUtils.setField(service,"orderMapper",orders); ReflectionTestUtils.setField(service,"favoriteMapper",favorites);
        ReflectionTestUtils.setField(service,"productMapper",products); ReflectionTestUtils.setField(service,"feedbackMapper",feedback);
        ReflectionTestUtils.setField(service,"objectMapper",new ObjectMapper());
    }
    Product product(long id, int sales) { Product p=new Product();p.setId(id);p.setStatus(1);p.setSalesCount(sales);return p; }
    Order order(long user,long product) { Order o=new Order();o.setUserId(user);o.setProductId(product);o.setStatus(3);return o; }
    RecommendationFeedback vote(long id,String type) { RecommendationFeedback f=new RecommendationFeedback();f.setUserId(1L);f.setProductId(id);f.setFeedbackType(type);return f; }
    RecommendationResponse run(String type) { return service.generateRecommendationsWithExplanation(1L,type,null).getData(); }
    void aligned(RecommendationResponse data) { assertEquals(data.getProducts().stream().map(Product::getId).toList(),data.getExplanations().stream().map(e->e.getProductId()).toList()); }
    void collaborative(Product... candidates) {
        when(orders.selectList(any())).thenReturn(List.of(order(1,1)),List.of(order(2,1)),List.of(order(2,1),order(2,2),order(2,3)));
        when(products.selectList(any())).thenReturn(Arrays.asList(candidates));
    }
    @Test void legacyKeepsArrayAndDelegatesToSharedEngine() {
        RecommendService spy=spy(service);RecommendationResponse response=new RecommendationResponse();response.setProducts(List.of(product(2,0)));
        doReturn(org.example.springboot.common.Result.success(response)).when(spy).generateRecommendationsWithExplanation(1L,null,null);
        Object data=spy.generateRecommendations(1L).getData();assertTrue(data instanceof List<?>);assertEquals(response.getProducts(),data);verifyNoInteractions(products,orders);
    }
    @Test void collaborativeResultsFollowScoresNotDatabaseOrderAndUseOneBatchRead() {
        collaborative(product(3,0),product(2,100));var data=run(null);assertEquals(List.of(2L,3L),data.getProducts().stream().map(Product::getId).toList());aligned(data);
        verify(products,times(1)).selectList(any());verify(products,never()).selectById(anyLong());
    }
    @Test void equalScoresUseProductIdAsStableTieBreaker() {
        collaborative(product(3,0),product(2,0));var data=run(null);assertEquals(List.of(2L,3L),data.getProducts().stream().map(Product::getId).toList());aligned(data);
    }
    @Test void unpublishedCollaborativeCandidateIsExcluded() {
        Product hidden=product(3,200);hidden.setStatus(0);collaborative(hidden,product(2,0));assertEquals(List.of(2L),run(null).getProducts().stream().map(Product::getId).toList());
    }
    @Test void alreadyOwnedCollaborativeCandidateIsExcluded() {
        collaborative(product(2,0),product(3,100));when(feedback.selectList(any())).thenReturn(List.of(vote(3,"ALREADY_OWN")));assertEquals(List.of(2L),run(null).getProducts().stream().map(Product::getId).toList());
    }
    @ParameterizedTest @ValueSource(strings={"COLLABORATIVE","TRENDING","CONTENT","SERENDIPITY"})
    void everyQueryFiltersPublishedProductsAndBlocksOwnedBeforeLimit(String type) {
        when(feedback.selectList(any())).thenReturn(List.of(vote(8,"ALREADY_OWN")));
        when(products.selectList(any())).thenAnswer(call->{LambdaQueryWrapper<Product> q=call.getArgument(0);String sql=q.getSqlSegment();assertTrue(sql.contains("status ="));assertTrue(sql.contains("id NOT IN"));assertTrue(q.getParamNameValuePairs().containsValue(8L));return List.of();});
        var data=run(type);assertTrue(data.getProducts().isEmpty());aligned(data);
    }
    @ParameterizedTest @ValueSource(strings={"COLLABORATIVE","TRENDING","CONTENT","SERENDIPITY"})
    void likesCanBoostAndDislikesLowerRankWithoutMisaligningReasons(String type) {
        when(products.selectList(any())).thenReturn(List.of(product(2,10),product(3,10)));
        when(feedback.selectList(any())).thenReturn(List.of(vote(2,"DISLIKE"),vote(3,"LIKE")));
        var data=run(type);assertEquals(List.of(3L,2L),data.getProducts().stream().map(Product::getId).toList());aligned(data);
        assertTrue(data.getExplanations().get(0).getScore()>data.getExplanations().get(1).getScore());
    }
    @ParameterizedTest @ValueSource(strings={"null","{\"content\":null}","{\"content\":-1}","not json"})
    void invalidWeightsUseDefaultsInsteadOfFailing(String json) {
        when(products.selectList(any())).thenReturn(List.of(product(2,10)));
        assertEquals("0",service.generateRecommendationsWithExplanation(1L,"CONTENT",json).getCode());
    }
    @Test void deletedCandidatesFallBackToPublishedPopularList() {
        collaborative();var data=run(null);assertTrue(data.getStats().getFallbackUsed());assertTrue(data.getProducts().isEmpty());verify(products,times(2)).selectList(any());
    }

    @Test void similarityUsesFullNeighborHistoryNotOnlyOverlap() {
        var history = new ArrayList<Order>();
        for (long id=1; id<=100; id++) history.add(order(2,id));
        when(orders.selectList(any())).thenReturn(List.of(order(1,1)),List.of(order(2,1)),history);
        when(products.selectList(any())).thenReturn(List.of(product(2,10)));
        var data=run(null);
        assertEquals(0,data.getStats().getSimilarUserCount());
        assertTrue(data.getStats().getFallbackUsed());
        verify(orders,times(3)).selectList(any());
    }
    @Test void latestFeedbackReplacesOlderPreference() {
        var old=vote(2,"ALREADY_OWN");old.setId(1L);old.setFeedbackTime(new java.sql.Timestamp(1));
        var recent=vote(2,"LIKE");recent.setId(2L);recent.setFeedbackTime(new java.sql.Timestamp(2));
        when(feedback.selectList(any())).thenReturn(List.of(recent,old));
        when(products.selectList(any())).thenReturn(List.of(product(2,10)));
        var data=run("TRENDING");assertEquals(List.of(2L),data.getProducts().stream().map(Product::getId).toList());
        assertTrue(data.getExplanations().get(0).getScore()>1.0);
    }
    @Test void completedPurchaseIsExcludedBeforePopularLimit() {
        when(orders.selectList(any())).thenReturn(List.of(order(1,99)),List.of());
        when(products.selectList(any())).thenAnswer(call -> {
            LambdaQueryWrapper<Product> q=call.getArgument(0);
            assertTrue(q.getSqlSegment().contains("id NOT IN"));
            assertTrue(q.getParamNameValuePairs().containsValue(99L));return List.of(product(2,10));
        });
        assertEquals(1,run(null).getProducts().size());
    }
}
