package org.example.springboot;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.example.springboot.entity.Article;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ArticleMapper;
import org.example.springboot.service.ArticleService;
import org.example.springboot.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleImageRetentionTest {
    enum WriteResult { SUCCESS, ZERO_ROWS, EXCEPTION }

    @Mock ArticleMapper articles;
    @Mock FileService files;
    @InjectMocks ArticleService service;

    private Article existing() {
        Article article = new Article();
        article.setId(2L);
        article.setTitle("Shared cover");
        article.setCoverImage("/img/shared.png");
        when(articles.selectById(2L)).thenReturn(article);
        return article;
    }

    private int write(WriteResult result) {
        if (result == WriteResult.EXCEPTION) throw new IllegalStateException("database unavailable");
        return result == WriteResult.SUCCESS ? 1 : 0;
    }

    @ParameterizedTest
    @EnumSource(WriteResult.class)
    void replacingCoverRetainsOldFileRegardlessOfDatabaseOutcome(WriteResult result) {
        Article old = existing();
        Article patch = new Article();
        patch.setId(999L);
        patch.setCoverImage("/img/new.png");
        when(articles.updateById(any(Article.class))).thenAnswer(call -> write(result));

        if (result == WriteResult.SUCCESS) {
            assertEquals("0", service.updateArticle(2L, patch).getCode());
        } else {
            assertThrows(BusinessException.class, () -> service.updateArticle(2L, patch));
        }
        assertEquals(2L, patch.getId());
        assertEquals("/img/shared.png", old.getCoverImage());
        verify(articles).updateById(patch);
        verifyNoInteractions(files);
    }

    @ParameterizedTest
    @EnumSource(WriteResult.class)
    void deletingArticleRetainsSharedFileRegardlessOfDatabaseOutcome(WriteResult result) {
        existing();
        when(articles.deleteById(2L)).thenAnswer(call -> write(result));

        if (result == WriteResult.SUCCESS) {
            assertEquals("0", service.deleteArticle(2L).getCode());
        } else {
            assertThrows(BusinessException.class, () -> service.deleteArticle(2L));
        }
        verify(articles).deleteById(2L);
        verifyNoInteractions(files);
    }

    @ParameterizedTest
    @EnumSource(WriteResult.class)
    void batchDeletionRetainsFilesRegardlessOfDatabaseOutcome(WriteResult result) {
        when(articles.delete(any(Wrapper.class))).thenAnswer(call -> write(result));

        if (result == WriteResult.SUCCESS) {
            assertEquals("0", service.deleteBatch(List.of(2L, 3L)).getCode());
        } else {
            assertThrows(BusinessException.class, () -> service.deleteBatch(List.of(2L, 3L)));
        }
        verify(articles).delete(any(Wrapper.class));
        verify(articles, never()).selectById(anyLong());
        verifyNoInteractions(files);
    }

    @Test
    void missingArticleCannotUpdateOrDeleteFiles() {
        assertNotEquals("0", service.updateArticle(2L, new Article()).getCode());
        verify(articles, never()).updateById(any(Article.class));
        verifyNoInteractions(files);
    }

    @Test
    void missingArticleCannotDeleteFiles() {
        assertNotEquals("0", service.deleteArticle(2L).getCode());
        verify(articles, never()).deleteById(any(java.io.Serializable.class));
        verifyNoInteractions(files);
    }

    @Test
    void emptyBatchNeverTouchesDatabaseOrFiles() {
        assertNotEquals("0", service.deleteBatch(List.of()).getCode());
        assertNotEquals("0", service.deleteBatch(null).getCode());
        verifyNoInteractions(articles, files);
    }

    @Test
    void unchangedCoverIsRetained() {
        existing();
        Article patch = new Article();
        patch.setCoverImage("/img/shared.png");
        when(articles.updateById(any(Article.class))).thenReturn(1);
        assertEquals("0", service.updateArticle(2L, patch).getCode());
        verifyNoInteractions(files);
    }
}
