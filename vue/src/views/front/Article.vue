<template>
  <div class="article-page landscape-bg">
    <!-- 动态背景 -->
    <div class="bg-container cloud-decoration">
      <div class="bg-gradient"></div>
      <div class="bg-elements">
        <div class="floating-element circle" v-for="n in 8" :key="'circle-' + n" :style="getRandomPosition()"></div>
        <div class="floating-element leaf" v-for="n in 6" :key="'leaf-' + n" :style="getRandomPosition()"></div>
      </div>
    </div>

    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="header-left">
          <div class="title-wrapper">
            <div class="title-icon-wrapper">
              <i class="fas fa-newspaper"></i>
            </div>
            <div class="title-content">
              <h2>图书资讯</h2>
              <p class="title-subtitle">探索图书前沿，了解行业动态</p>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="stats-wrapper">
            <div class="stat-item">
              <i class="fas fa-file-alt"></i>
              <span class="stat-label">文章总数</span>
              <span class="stat-value">{{ total }}</span>
            </div>
            <div class="stat-item" v-if="viewStats.totalViews > 0">
              <i class="fas fa-eye"></i>
              <span class="stat-label">总阅读</span>
              <span class="stat-value">{{ formatNumber(viewStats.totalViews) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索和筛选区域 -->
      <div class="filter-section">
        <div class="search-wrapper">
          <div class="search-box">
            <i class="fas fa-search search-icon"></i>
            <input 
              v-model="searchKeyword" 
              @input="handleSearch"
              @focus="showSuggestions = true"
              @blur="hideSuggestions"
              placeholder="搜索图书资讯..."
              class="search-input"
            />
            <div class="search-suggestions" v-if="showSuggestions && searchSuggestions.length > 0">
              <div 
                v-for="suggestion in searchSuggestions" 
                :key="suggestion"
                @click="selectSuggestion(suggestion)"
                class="suggestion-item"
              >
                <i class="fas fa-search"></i>
                {{ suggestion }}
              </div>
            </div>
          </div>
        </div>
        
        <div class="filter-controls">
          <div class="sort-wrapper">
            <el-dropdown @command="handleSortChange" trigger="click">
              <el-button class="sort-btn">
                <i :class="getSortIcon()"></i>
                {{ getSortText() }}
                <i class="fas fa-chevron-down"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="newest">最新发布</el-dropdown-item>
                  <el-dropdown-item command="popular">热门阅读</el-dropdown-item>
                  <el-dropdown-item command="oldest">最早发布</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <div class="view-toggle">
            <el-button-group>
              <el-button 
                :type="viewMode === 'grid' ? 'primary' : 'default'" 
                @click="viewMode = 'grid'"
                class="view-btn"
              >
                <i class="fas fa-th-large"></i>
              </el-button>
              <el-button 
                :type="viewMode === 'list' ? 'primary' : 'default'" 
                @click="viewMode = 'list'"
                class="view-btn"
              >
                <i class="fas fa-list"></i>
              </el-button>
            </el-button-group>
          </div>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="article-content" v-loading="loading">
        <!-- 空状态提示 -->
        <div v-if="!loading && articles.length === 0" class="empty-articles">
          <div class="empty-icon">
            <i class="fas fa-newspaper"></i>
            <div class="floating-dots">
              <span v-for="n in 3" :key="n" class="dot"></span>
            </div>
          </div>
          <div class="empty-content">
            <h3>{{ searchKeyword ? '未找到相关资讯' : '暂无相关资讯' }}</h3>
            <p>{{ searchKeyword ? '试试其他关键词或浏览全部文章' : '敬请期待更多精彩内容' }}</p>
            <el-button v-if="searchKeyword" @click="clearSearch" type="primary" class="action-btn">
              <i class="fas fa-undo"></i>
              清除搜索
            </el-button>
          </div>
        </div>

        <!-- 文章列表 -->
        <transition-group 
          v-else 
          name="article-item" 
          tag="div" 
          :class="['article-list', viewMode + '-view']"
        >
          <div 
            v-for="(article, index) in articles" 
            :key="article.id" 
            class="article-item" 
            :style="{ 'animation-delay': (index * 0.1) + 's' }"
            @click="viewArticle(article)"
          >
            <div class="article-image-wrapper">
              <el-image 
                :src="article.coverImage?.startsWith('http') ? article.coverImage : `api${article.coverImage}`" 
                fit="cover"
                :lazy="true"
                class="article-image">
                <template #placeholder>
                  <div class="image-loading">
                    <div class="loading-spinner"></div>
                  </div>
                </template>
                <template #error>
                  <div class="image-error">
                    <i class="fas fa-image"></i>
                  </div>
                </template>
              </el-image>
              <div class="article-overlay">
                <i class="fas fa-eye"></i>
                <span>阅读全文</span>
              </div>
              <div class="article-badges">
                <span v-if="isNew(article)" class="badge new-badge">
                  <i class="fas fa-star"></i>
                  NEW
                </span>
                <span v-if="isHot(article)" class="badge hot-badge">
                  <i class="fas fa-fire"></i>
                  热门
                </span>
              </div>
            </div>
            
            <div class="article-details">
              <div class="article-header">
                <h3 class="article-title">{{ article.title }}</h3>
                <div class="article-meta-top">
                  <span class="publish-time">
                    <i class="fas fa-calendar-alt"></i>
                    {{ formatTime(article.createdAt) }}
                  </span>
                </div>
              </div>
              
              <p class="article-summary">{{ article.summary }}</p>
              
              <div class="article-footer">
                <div class="article-meta">
                  <span class="view-count">
                    <i class="fas fa-eye"></i>
                    {{ formatNumber(article.viewCount) }}
                  </span>
                  <span class="read-time">
                    <i class="fas fa-clock"></i>
                    {{ estimateReadTime(article.content) }}分钟阅读
                  </span>
                </div>
                <div class="article-actions">
                  <el-button link class="read-btn">
                    <span>阅读详情</span>
                    <i class="fas fa-arrow-right"></i>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </transition-group>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="getArticles"
            @size-change="handleSizeChange">
          </el-pagination>
        </div>
      </div>
    </div>
    <front-footer></front-footer>

    <!-- 文章详情对话框 -->
    <el-dialog
      :title="currentArticle?.title"
      v-model="dialogVisible"
      width="70%"
      class="article-dialog">
      <div class="article-content-wrapper">
        <div class="article-info">
          <span class="publish-time">发布时间：{{ formatTime(currentArticle?.createdAt) }}</span>
          <span class="view-count">
            <i class="fas fa-eye"></i>
            {{ currentArticle?.viewCount }} 阅读
          </span>
        </div>
        <div class="article-full-content" v-html="currentArticle?.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { formatTime } from '@/utils/time'

export default {
  name: 'Article',
  components: {
    FrontHeader,
    FrontFooter
  },
  data() {
    return {
      loading: false,
      articles: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      currentArticle: null,
      searchKeyword: '',
      sortBy: 'newest',
      viewMode: 'grid', // 'grid' 或 'list'
      showSuggestions: false,
      searchSuggestions: [],
      popularKeywords: ['文学小说', '科普读物', '历史传记', '儿童绘本', '阅读习惯', '出版动态', '电子书', '有声书', '经典名著', '畅销新书'],
      viewStats: {
        totalViews: 0
      }
    }
  },
  created() {
    this.getArticles()
  },
  methods: {
    formatTime,
    
    // 获取随机位置用于背景元素
    getRandomPosition() {
      return {
        left: Math.random() * 100 + '%',
        top: Math.random() * 100 + '%',
        animationDelay: Math.random() * 20 + 's',
        animationDuration: (15 + Math.random() * 10) + 's'
      }
    },

    async getArticles() {
      this.loading = true
      try {
        const res = await Request.get('/article/page', {
          params: {
            currentPage: this.currentPage,
            size: this.pageSize,
            status: 1, // 只获取上架的文章
            title: this.searchKeyword || '' // 搜索关键词
          }
        })
        if (res.code === '0') {
          let articles = res.data.records.map(article => ({
            id: article.id,
            title: article.title,
            content: article.content,
            summary: article.summary || this.generateSummary(article.content),
            coverImage: article.coverImage,
            viewCount: article.viewCount || 0,
            createdAt: article.createdAt,
            updatedAt: article.updatedAt
          }))
          
          // 根据排序方式排序
          articles = this.sortArticles(articles)
          
          this.articles = articles
          this.total = res.data.total
          
          // 计算总阅读数
          this.viewStats.totalViews = articles.reduce((sum, article) => sum + article.viewCount, 0)
        }
      } catch (error) {
        logger.error('获取文章失败:', error)
        this.$message.error('获取文章失败')
      } finally {
        this.loading = false
      }
    },
    
    // 排序文章
    sortArticles(articles) {
      switch (this.sortBy) {
        case 'newest':
          return articles.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        case 'oldest':
          return articles.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
        case 'popular':
          return articles.sort((a, b) => b.viewCount - a.viewCount)
        default:
          return articles
      }
    },

    generateSummary(content) {
      // 移除HTML标签
      const plainText = content.replace(/<[^>]+>/g, '')
      // 取前120个字符作为摘要
      return plainText.substring(0, 120) + (plainText.length > 120 ? '...' : '')
    },

    async viewArticle(article) {
      this.$router.push(`/article/${article.id}`)
    },

    // 搜索处理
    async handleSearch() {
      if (this.searchKeyword.length > 0) {
        this.updateSearchSuggestions()
      } else {
        this.searchSuggestions = []
      }
      
      // 防抖处理
      clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.currentPage = 1
        this.getArticles()
      }, 500)
    },

    // 更新搜索建议
    updateSearchSuggestions() {
      const keyword = this.searchKeyword.toLowerCase()
      this.searchSuggestions = this.popularKeywords
        .filter(item => item.includes(keyword))
        .slice(0, 5)
    },

    // 选择搜索建议
    selectSuggestion(suggestion) {
      this.searchKeyword = suggestion
      this.showSuggestions = false
      this.currentPage = 1
      this.getArticles()
    },

    // 隐藏搜索建议（延迟执行以允许点击）
    hideSuggestions() {
      setTimeout(() => {
        this.showSuggestions = false
      }, 200)
    },

    // 清除搜索
    clearSearch() {
      this.searchKeyword = ''
      this.currentPage = 1
      this.getArticles()
    },

    // 排序改变处理
    handleSortChange(command) {
      this.sortBy = command
      this.getArticles()
    },

    // 获取排序图标
    getSortIcon() {
      switch (this.sortBy) {
        case 'newest':
          return 'fas fa-clock'
        case 'popular':
          return 'fas fa-fire'
        case 'oldest':
          return 'fas fa-history'
        default:
          return 'fas fa-sort'
      }
    },

    // 获取排序文本
    getSortText() {
      switch (this.sortBy) {
        case 'newest':
          return '最新发布'
        case 'popular':
          return '热门阅读'
        case 'oldest':
          return '最早发布'
        default:
          return '排序'
      }
    },

    // 分页大小改变
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.getArticles()
    },

    // 判断是否为新文章（7天内）
    isNew(article) {
      const now = new Date()
      const articleDate = new Date(article.createdAt)
      const diffTime = Math.abs(now - articleDate)
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      return diffDays <= 7
    },

    // 判断是否为热门文章（阅读数超过平均值的1.5倍）
    isHot(article) {
      if (this.articles.length === 0) return false
      const avgViews = this.articles.reduce((sum, a) => sum + a.viewCount, 0) / this.articles.length
      return article.viewCount > avgViews * 1.5 && article.viewCount > 100
    },

    // 估计阅读时间
    estimateReadTime(content) {
      const plainText = content.replace(/<[^>]+>/g, '')
      const wordsPerMinute = 200 // 中文阅读速度大约每分钟200字
      const readTime = Math.ceil(plainText.length / wordsPerMinute)
      return Math.max(1, readTime) // 至少1分钟
    },

    // 格式化数字
    formatNumber(num) {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
      } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k'
      }
      return num.toString()
    }
  },
  
  beforeUnmount() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
    }
  }
}
</script>

<style scoped>
/* 基础页面样式 */
.article-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #f5f7fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 动态背景 */
.bg-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    45deg,
    rgba(58, 91, 160, 0.02) 0%,
    rgba(91, 124, 189, 0.02) 25%,
    rgba(139, 163, 217, 0.02) 50%,
    rgba(58, 91, 160, 0.02) 75%,
    rgba(45, 72, 128, 0.02) 100%
  );
  animation: backgroundFloat 20s ease-in-out infinite;
}

.bg-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.floating-element {
  position: absolute;
  opacity: 0.03;
}

.floating-element.circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--gradient-primary);
  animation: float 15s ease-in-out infinite;
}

.floating-element.leaf {
  width: 15px;
  height: 25px;
  background: linear-gradient(45deg, var(--primary-color), var(--primary-light));
  border-radius: 0 100% 0 100%;
  animation: float 20s ease-in-out infinite;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 24px 32px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 5px;
  height: 100%;
  background: var(--gradient-primary);
  border-radius: 0 5px 5px 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  background: var(--gradient-primary);
  border-radius: 15px;
  color: white;
  font-size: 20px;
  animation: iconPulse 3s ease-in-out infinite;
}

.title-content h2 {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 4px 0;
  background: linear-gradient(135deg, var(--ink-darkest), var(--primary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-subtitle {
  color: #606266;
  font-size: 14px;
  margin: 0;
  opacity: 0.8;
}

.header-right {
  display: flex;
  align-items: center;
}

.stats-wrapper {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: rgba(58, 91, 160, 0.08);
  border-radius: 16px;
  border: 1px solid rgba(58, 91, 160, 0.1);
  min-width: 120px;
  text-align: center;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: rgba(58, 91, 160, 0.12);
  transform: translateY(-2px);
}

.stat-item i {
  color: var(--primary-color);
  font-size: 18px;
}

.stat-label {
  color: #909399;
  font-size: 12px;
  font-weight: 500;
}

.stat-value {
  color: var(--primary-color);
  font-size: 20px;
  font-weight: 700;
}

/* 搜索和筛选区域 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 20px 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.search-wrapper {
  flex: 1;
  max-width: 500px;
  position: relative;
}

.search-box {
  position: relative;
  width: 100%;
}

.search-input {
  width: 100%;
  padding: 14px 20px 14px 50px;
  border: 2px solid #e4e7ed;
  border-radius: 25px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
  outline: none;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(58, 91, 160, 0.1);
}

.search-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
  font-size: 16px;
  transition: color 0.3s ease;
}

.search-input:focus + .search-icon {
  color: var(--primary-color);
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border: 1px solid #e4e7ed;
  margin-top: 8px;
  z-index: 100;
  overflow: hidden;
}

.suggestion-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
  font-size: 14px;
}

.suggestion-item:hover {
  background: #f5f7fa;
}

.suggestion-item i {
  color: #909399;
  font-size: 12px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sort-btn {
  padding: 10px 20px;
  border-radius: 20px;
  border: 1px solid #e4e7ed;
  background: white;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.sort-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.view-toggle .el-button-group .el-button {
  padding: 10px 14px;
  border-radius: 8px;
  margin: 0 2px;
}

.view-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
}

/* 文章列表 */
.article-content {
  position: relative;
}

.article-list {
  display: grid;
  gap: 24px;
  margin-bottom: 40px;
}

.article-list.grid-view {
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
}

.article-list.list-view {
  grid-template-columns: 1fr;
}

.article-item {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  transition: all 0.4s ease;
  cursor: pointer;
  position: relative;
  animation: fadeIn 0.8s ease forwards;
  opacity: 0;
  transform: translateY(30px);
}

.article-item:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.list-view .article-item {
  display: flex;
  align-items: stretch;
}

.list-view .article-item .article-image-wrapper {
  width: 300px;
  flex-shrink: 0;
}

.list-view .article-item .article-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.article-image-wrapper {
  position: relative;
  overflow: hidden;
}

.grid-view .article-image-wrapper {
  height: 220px;
}

.list-view .article-image-wrapper {
  height: 100%;
  min-height: 200px;
}

.article-image {
  width: 100%;
  height: 100%;
  transition: transform 0.4s ease;
}

.article-item:hover .article-image {
  transform: scale(1.05);
}

.article-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s ease;
  font-size: 16px;
  gap: 8px;
}

.article-item:hover .article-overlay {
  opacity: 1;
}

.article-badges {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(10px);
}

.new-badge {
  background: rgba(255, 193, 7, 0.9);
  color: white;
  animation: glowPulse 2s ease-in-out infinite;
}

.hot-badge {
  background: rgba(244, 67, 54, 0.9);
  color: white;
  animation: glowPulse 2s ease-in-out infinite alternate;
}

.image-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #e4e7ed;
  border-top: 3px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}

.article-details {
  padding: 24px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 16px;
}

.article-title {
  flex: 1;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.4;
  margin: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  transition: color 0.3s ease;
}

.article-item:hover .article-title {
  color: var(--primary-color);
}

.article-meta-top {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.article-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 20px 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  flex: 1;
}

.list-view .article-summary {
  -webkit-line-clamp: 4;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 12px;
}

.view-count, .read-time, .publish-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-actions {
  display: flex;
  align-items: center;
}

.read-btn {
  color: var(--primary-color);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.read-btn:hover {
  color: var(--primary-dark);
  transform: translateX(4px);
}

/* 空状态 */
.empty-articles {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.empty-icon {
  position: relative;
  margin-bottom: 24px;
}

.empty-icon i {
  font-size: 64px;
  color: #c0c4cc;
  opacity: 0.6;
}

.floating-dots {
  position: absolute;
  top: -10px;
  right: -10px;
}

.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color);
  margin: 0 2px;
  animation: float 2s ease-in-out infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

.empty-content h3 {
  color: #606266;
  font-size: 20px;
  margin: 0 0 12px 0;
  font-weight: 500;
}

.empty-content p {
  color: #909399;
  font-size: 14px;
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.action-btn {
  padding: 12px 24px;
  border-radius: 25px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin: 40px 0;
  padding: 32px 0;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.95);
  --el-pagination-hover-color: var(--primary-color);
  backdrop-filter: blur(20px);
  padding: 16px 24px;
  border-radius: 50px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: var(--primary-color);
  border-radius: 50%;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: var(--primary-color);
}

:deep(.el-pagination .el-select .el-input) {
  border-radius: 20px;
}

/* 动画 */
@keyframes backgroundFloat {
  0%, 100% { 
    background-position: 0% 50%; 
  }
  50% { 
    background-position: 100% 50%; 
  }
}

@keyframes float {
  0%, 100% { 
    transform: translateY(0px) rotate(0deg); 
  }
  50% { 
    transform: translateY(-20px) rotate(180deg); 
  }
}

@keyframes iconPulse {
  0%, 100% { 
    transform: scale(1); 
    box-shadow: 0 0 0 0 rgba(58, 91, 160, 0.4); 
  }
  50% { 
    transform: scale(1.05); 
    box-shadow: 0 0 0 10px rgba(58, 91, 160, 0); 
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes glowPulse {
  0%, 100% { 
    box-shadow: 0 0 5px rgba(255, 255, 255, 0.5); 
  }
  50% { 
    box-shadow: 0 0 20px rgba(255, 255, 255, 0.8); 
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 文章项动画 */
.article-item-enter-active, .article-item-leave-active {
  transition: all 0.6s ease;
}

.article-item-enter, .article-item-leave-to {
  opacity: 0;
  transform: translateY(30px) scale(0.95);
}

.article-item-move {
  transition: transform 0.6s ease;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    max-width: 1000px;
    padding: 24px;
  }
  
  .article-list.grid-view {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  }
  
  .stats-wrapper {
    gap: 16px;
  }
  
  .stat-item {
    min-width: 100px;
    padding: 12px 16px;
  }
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .header-right {
    width: 100%;
  }
  
  .stats-wrapper {
    justify-content: space-around;
    width: 100%;
  }
  
  .filter-section {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .filter-controls {
    justify-content: space-between;
  }
  
  .list-view .article-item {
    flex-direction: column;
  }
  
  .list-view .article-item .article-image-wrapper {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
  
  .page-header {
    padding: 20px;
    margin-bottom: 20px;
  }
  
  .title-wrapper {
    gap: 12px;
  }
  
  .title-icon-wrapper {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
  
  .title-content h2 {
    font-size: 22px;
  }
  
  .stats-wrapper {
    flex-direction: column;
    gap: 12px;
  }
  
  .stat-item {
    padding: 12px 16px;
  }
  
  .article-list.grid-view {
    grid-template-columns: 1fr;
  }
  
  .article-details {
    padding: 20px;
  }
  
  .article-title {
    font-size: 18px;
  }
  
  .filter-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .search-input {
    padding: 12px 16px 12px 45px;
  }
}

/* 可访问性支持 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .article-page {
    background: #1a1a1a;
  }
  
  .page-header, .filter-section, .article-item {
    background: rgba(30, 30, 30, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .title-content h2, .article-title {
    color: #e0e0e0;
  }
  
  .title-subtitle, .article-summary {
    color: #b0b0b0;
  }
  
  .search-input {
    background: rgba(40, 40, 40, 0.8);
    border-color: rgba(255, 255, 255, 0.1);
    color: #e0e0e0;
  }
  
  .empty-articles {
    background: rgba(30, 30, 30, 0.95);
  }
}
</style> 