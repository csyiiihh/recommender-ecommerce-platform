package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Article;
import org.example.springboot.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleService.class);

    @Autowired
    private ArticleMapper articleMapper;

    public Result<?> createArticle(Article article) {
        try {
            article.setViewCount(0);
            int result = articleMapper.insert(article);
            if (result > 0) {
                LOGGER.info("创建资讯成功，资讯ID：{}，标题：{}", article.getId(), article.getTitle());
                return ResultUtils.success(article);
            }
            LOGGER.warn("创建资讯失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建资讯失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建资讯异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建资讯失败：" + e.getMessage());
        }
    }

    public Result<?> updateArticle(Long id, Article article) {
        // 检查资讯是否存在
        Article oldArticle = articleMapper.selectById(id);
        if (oldArticle == null) {
            LOGGER.warn("更新资讯失败，资讯ID：{} 不存在", id);
            return ResultUtils.dataNotFound("资讯不存在");
        }

        try {
            // Images may be shared; database failures cannot restore deleted files.
            // Retain uploads on edits and deletions; clean up only after a reference audit.
            article.setId(id);
            article.setViewCount(null); // The editor must not overwrite live read counts.
            int result = articleMapper.updateById(article);
            if (result > 0) {
                LOGGER.info("更新资讯成功，资讯ID：{}，标题：{}", id, article.getTitle());
                return ResultUtils.success(article);
            }
            LOGGER.warn("更新资讯失败，资讯ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新资讯失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新资讯异常，资讯ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新资讯失败：" + e.getMessage());
        }
    }

    public Result<?> deleteArticle(Long id) {
        // 检查资讯是否存在
        Article article = articleMapper.selectById(id);
        if (article == null) {
            LOGGER.warn("删除资讯失败，资讯ID：{} 不存在", id);
            return ResultUtils.dataNotFound("资讯不存在");
        }
        
        try {
            // Keep shared cover files when removing article records.
            int result = articleMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除资讯成功，资讯ID：{}，标题：{}", id, article.getTitle());
                return ResultUtils.success();
            }
            LOGGER.warn("删除资讯失败，资讯ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除资讯失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除资讯异常，资讯ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除资讯失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<?> getArticleById(Long id) {
        // Lock only published content; an unpublish or edit waits for this read transaction.
        Article article = articleMapper.selectPublishedForUpdate(id);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) return ResultUtils.dataNotFound("资讯不存在");
        int views = article.getViewCount() == null ? 0 : Math.max(0, article.getViewCount());
        if (views < Integer.MAX_VALUE) {
            if (articleMapper.incrementPublishedViews(id) != 1) {
                throw new BusinessException("更新资讯浏览量失败");
            }
            article.setViewCount(views + 1);
        }
        return ResultUtils.success(article);
    }

    public Page<Article> getArticlesByPage(String title, Integer status, Integer currentPage, Integer size) {
        return getArticlesByPage(title, status, currentPage, size, "newest");
    }

    public Page<Article> getArticlesByPage(String title, Integer status, Integer currentPage, Integer size, String sort) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) q.like(Article::getTitle, title.strip());
        if (status != null) q.eq(Article::getStatus, status);
        switch (sort == null ? "newest" : sort) {
            case "newest" -> q.orderByDesc(Article::getCreatedAt);
            case "oldest" -> q.orderByAsc(Article::getCreatedAt);
            case "popular" -> q.orderByDesc(Article::getViewCount);
            default -> throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "无效资讯排序");
        }
        q.orderByDesc(Article::getId);
        return articleMapper.selectPage(new Page<>(currentPage, Math.min(size, 100)), q);
    }

    public Result<?> updateArticleStatus(Long id, Integer status) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            LOGGER.warn("更新资讯状态失败，资讯ID：{} 不存在", id);
            return ResultUtils.dataNotFound("资讯不存在");
        }
        
        try {
            if (status == null || (status != 0 && status != 1)) return ResultUtils.paramError("状态必须为0或1");
            Article patch = new Article();
            patch.setId(id);
            patch.setStatus(status);
            patch.setViewCount(null);
            int result = articleMapper.updateById(patch);
            if (result > 0) {
                LOGGER.info("更新资讯状态成功，资讯ID：{}，标题：{}，新状态：{}", id, article.getTitle(), status);
                return ResultUtils.success();
            }
            LOGGER.warn("更新资讯状态失败，资讯ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新资讯状态失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新资讯状态异常，资讯ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新资讯状态失败：" + e.getMessage());
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除资讯失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // Batch deletion also retains uploads referenced by other content.
            LambdaQueryWrapper<Article> wrapper = Wrappers.lambdaQuery();
            wrapper.in(Article::getId, ids);
            int result = articleMapper.delete(wrapper);
            
            if (result > 0) {
                LOGGER.info("批量删除资讯成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除资讯失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除资讯失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除资讯异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除资讯失败：" + e.getMessage());
        }
    }
} 