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

import java.util.List;

@Service
public class ArticleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleService.class);

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private FileService fileService;

    public Result<?> createArticle(Article article) {
        try {
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
            // 处理封面图片
            String oldImg = oldArticle.getCoverImage();
            String newImg = article.getCoverImage();
            if (oldImg != null && newImg != null && !oldImg.equals(newImg)) {
                fileService.fileRemove(oldImg);
            }

            article.setId(id);
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
            // 删除封面图片
            if (article.getCoverImage() != null) {
                fileService.fileRemove(article.getCoverImage());
            }

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

    public Result<?> getArticleById(Long id) {
        try {
            Article article = articleMapper.selectById(id);
            if (article == null) {
                LOGGER.warn("查询资讯失败，资讯ID：{} 不存在", id);
                return ResultUtils.dataNotFound("资讯不存在");
            }
            
            // 增加浏览量
            article.setViewCount(article.getViewCount() + 1);
            articleMapper.updateById(article);
            LOGGER.debug("查询资讯成功，资讯ID：{}，浏览量：{}", id, article.getViewCount());
            return ResultUtils.success(article);
        } catch (Exception e) {
            LOGGER.error("查询资讯异常，资讯ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询资讯失败：" + e.getMessage());
        }
    }

    public Page<Article> getArticlesByPage(String title, Integer status,
                                         Integer currentPage, Integer size) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        
        if (title != null && !title.trim().isEmpty()) {
            queryWrapper.like(Article::getTitle, title.trim());
        }
        if (status != null) {
            queryWrapper.eq(Article::getStatus, status);
        }
        
        queryWrapper.orderByDesc(Article::getCreatedAt);

        Page<Article> page = new Page<>(currentPage, size);
        return articleMapper.selectPage(page, queryWrapper);
    }

    public Result<?> updateArticleStatus(Long id, Integer status) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            LOGGER.warn("更新资讯状态失败，资讯ID：{} 不存在", id);
            return ResultUtils.dataNotFound("资讯不存在");
        }
        
        try {
            article.setStatus(status);
            int result = articleMapper.updateById(article);
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
            // 删除相关的封面图片
            for (Long id : ids) {
                Article article = articleMapper.selectById(id);
                if (article != null && article.getCoverImage() != null) {
                    fileService.fileRemove(article.getCoverImage());
                }
            }

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