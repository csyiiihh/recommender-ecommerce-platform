package org.example.springboot;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.mapping.BoundSql;
import org.example.springboot.controller.ArticleController;
import org.example.springboot.entity.Article;
import org.example.springboot.entity.User;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.service.AccessService;
import org.example.springboot.service.ArticleService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleVisibilityAndViewsTest {
    ArticleMapper mapper;
    ArticleService service;
    @BeforeEach void setup() {
        mapper = mock(ArticleMapper.class);
        service = new ArticleService();
        ReflectionTestUtils.setField(service, "articleMapper", mapper);
        SecurityContextHolder.clearContext();
    }
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }
    Article article(Integer count) {
        Article a = new Article(); a.setId(7L); a.setViewCount(count);
        a.setTitle("Current title"); a.setContent("Current content");
        when(mapper.selectPublishedForUpdate(7L)).thenReturn(a);
        return a;
    }
    ArticleController controller(ArticleService target) {
        ArticleController c = new ArticleController();
        ReflectionTestUtils.setField(c, "articleService", target);
        ReflectionTestUtils.setField(c, "access", new AccessService());
        return c;
    }
    void login(String role) {
        if ("ANONYMOUS".equals(role)) return;
        User user = new User(); user.setId(42L); user.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {0, 1, 2})
    void publicListAlwaysRequestsPublishedContent(Integer requestedStatus) {
        ArticleService target = mock(ArticleService.class);
        controller(target).getArticlesByPage("book", requestedStatus, 1, 10);
        verify(target).getArticlesByPage("book", 1, 1, 10);
    }
    @ParameterizedTest @ValueSource(strings = {"ANONYMOUS", "USER", "MERCHANT"})
    void managementListRejectsNonAdministrators(String role) {
        login(role);
        ArticleService target = mock(ArticleService.class);
        assertThrows(AccessDeniedException.class, () -> controller(target).getManagedArticlesByPage(null, 0, 1, 10));
        verifyNoInteractions(target);
    }
    @Test void administratorCanListUnpublishedContent() {
        login("ADMIN"); ArticleService target = mock(ArticleService.class);
        controller(target).getManagedArticlesByPage(null, 0, 1, 10);
        verify(target).getArticlesByPage(null, 0, 1, 10);
    }
    @Test void administratorCanListAllStatuses() {
        login("ADMIN"); ArticleService target = mock(ArticleService.class);
        controller(target).getManagedArticlesByPage(null, null, 1, 10);
        verify(target).getArticlesByPage(null, null, 1, 10);
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {0, -5, 9})
    void readingUpdatesOnlyCounterAndHandlesOldNullValues(Integer count) {
        Article a = article(count); when(mapper.incrementPublishedViews(7L)).thenReturn(1);
        assertEquals("0", service.getArticleById(7L).getCode());
        assertEquals((count == null ? 0 : Math.max(0, count)) + 1, a.getViewCount());
        assertEquals("Current title", a.getTitle());
        verify(mapper, never()).updateById(any(Article.class));
        var order = inOrder(mapper); order.verify(mapper).selectPublishedForUpdate(7L); order.verify(mapper).incrementPublishedViews(7L);
    }
    @Test void absentArticleDoesNotIncrementAnything() {
        assertNotEquals("0", service.getArticleById(7L).getCode());
        verify(mapper, never()).incrementPublishedViews(anyLong());
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {0, 2})
    void unpublishedArticleNeverReturnsContent(Integer status) {
        Article a = article(4); a.setStatus(status);
        assertNotEquals("0", service.getArticleById(7L).getCode());
        verify(mapper, never()).incrementPublishedViews(anyLong());
    }
    @Test void failedCounterWriteCannotReportSuccessfulRead() {
        Article a = article(4);
        assertThrows(BusinessException.class, () -> service.getArticleById(7L));
        assertEquals(4, a.getViewCount());
        verify(mapper, never()).updateById(any(Article.class));
    }
    @Test void counterSaturatesWithoutOverflow() {
        Article a = article(Integer.MAX_VALUE);
        assertEquals("0", service.getArticleById(7L).getCode());
        assertEquals(Integer.MAX_VALUE, a.getViewCount());
        verify(mapper, never()).incrementPublishedViews(anyLong());
    }
    @Test void editorCannotReplaceLiveCounter() {
        Article old = new Article(); old.setViewCount(80);
        when(mapper.selectById(7L)).thenReturn(old);
        when(mapper.updateById(any(Article.class))).thenReturn(1);
        Article edit = new Article(); edit.setViewCount(999);
        assertEquals("0", service.updateArticle(7L, edit).getCode());
        assertNull(edit.getViewCount()); assertEquals(80, old.getViewCount());
    }
    @Test void statusChangeWritesNeitherContentNorCounter() {
        Article old = new Article(); old.setTitle("Old title"); old.setViewCount(5);
        when(mapper.selectById(7L)).thenReturn(old);
        when(mapper.updateById(any(Article.class))).thenAnswer(call -> {
            Article patch = call.getArgument(0);
            assertEquals(7L, patch.getId()); assertEquals(0, patch.getStatus());
            assertNull(patch.getTitle()); assertNull(patch.getContent());
            assertNull(patch.getCoverImage()); assertNull(patch.getViewCount());
            return 1;
        });
        assertEquals("0", service.updateArticleStatus(7L, 0).getCode());
    }
    @Test void createdArticleCannotSupplyItsOwnCounter() {
        when(mapper.insert(any(Article.class))).thenReturn(1);
        Article a = new Article(); a.setViewCount(999);
        assertEquals("0", service.createArticle(a).getCode()); assertEquals(0, a.getViewCount());
    }
    @Test void mapperLocksOnlyPublishedContentAndCounterSqlCannotRewriteContent() {
        MybatisConfiguration config = new MybatisConfiguration(); config.addMapper(ArticleMapper.class);
        String base = ArticleMapper.class.getName() + ".";
        String select = config.getMappedStatement(base + "selectPublishedForUpdate").getBoundSql(Map.of("id", 7L)).getSql().toLowerCase();
        assertTrue(select.contains("status = 1")); assertTrue(select.contains("for update"));
        assertTrue(select.contains("id = ?"));
        BoundSql statement = config.getMappedStatement(base + "incrementPublishedViews").getBoundSql(Map.of("id", 7L));
        String update = statement.getSql().toLowerCase();
        String writes = update.substring(update.indexOf("set"), update.indexOf("where"));
        assertTrue(writes.contains("view_count"));
        for (String field : List.of("title", "content", "cover_image", "status")) assertFalse(writes.contains(field));
        assertTrue(update.contains("status = 1")); assertTrue(update.contains("coalesce(view_count, 0)"));
        assertEquals(1, statement.getParameterMappings().size());
    }
}
