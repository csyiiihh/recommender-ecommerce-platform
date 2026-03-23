<template>
  <div class="category-container" :class="{'is-sidebar': isSidebar}">
    <div class="category-header" :class="{'sidebar-header': isSidebar}">
      <h2 class="section-title">
        📚 智能分类导航
        <span class="algo-tag" title="基于您的浏览偏好排序">智能排序</span>
      </h2>
    </div>
    <!-- 有数据时显示分类项 -->
    <div class="category-wrapper" :class="{'sidebar-layout': isSidebar}" v-if="!loading && categories.length > 0">
      <div 
        v-for="(category, index) in visibleCategories" 
        :key="category.id || index" 
        class="category-item"
      :class="{'high-match': index < 2, 'medium-match': index >= 2 && index < 4, 'sidebar-item': isSidebar}"
        :style="{animationDelay: index * 0.1 + 's'}"
        @click="handleCategoryClick(category)"
      >
        <div class="icon-wrapper">
          <i :class="['icon', category.icon]"></i>
          <div class="icon-bg"></div>
        </div>
        <span class="category-name">{{ category.name }}</span>
        <div class="category-desc" v-if="getCategoryMatch(category.id) >= 80">根据您常读类型优化</div>
        <div class="category-desc" v-else-if="getCategoryTrend(category.id) > 0">最近热门增长</div>
        <span class="match-badge" v-if="getCategoryMatch(category.id) >= 80">{{ Math.round(getCategoryMatch(category.id)) }}%匹配</span>
        <span class="trend-badge" v-else-if="getCategoryTrend(category.id) > 0">↑ {{ Math.round(getCategoryTrend(category.id)) }}%</span>
        <div class="hover-indicator"></div>
      </div>
      <div 
        v-if="showMore" 
        class="category-item more-item"
        :class="{'sidebar-item': isSidebar}"
        :style="{animationDelay: visibleCategories.length * 0.1 + 's'}"
        @click="handleMoreClick"
      >
        <div class="icon-wrapper">
          <i class="fas fa-ellipsis-h"></i>
          <div class="icon-bg"></div>
        </div>
        <span class="category-name">更多分类</span>
        <div class="hover-indicator"></div>
      </div>
      <!-- 算法探索分类 -->
      <div 
        class="category-item algorithm-card"
        :class="{'sidebar-item': isSidebar}"
        :style="{animationDelay: (visibleCategories.length + (showMore ? 1 : 0)) * 0.1 + 's'}"
        @click="handleAlgorithmExplore"
      >
        <div class="icon-wrapper">
          <i class="fas fa-robot"></i>
          <div class="icon-bg"></div>
        </div>
        <span class="category-name">算法探索</span>
        <div class="category-desc">发现惊喜书籍</div>
        <button class="btn-surprise">随机推荐</button>
        <div class="hover-indicator"></div>
      </div>
    </div>
    <!-- 加载状态 -->
    <div class="category-loading" v-if="loading && categories.length === 0">
      <div class="loading-item" v-for="n in 5" :key="n"></div>
    </div>
    <!-- 空状态 -->
    <div class="category-empty" v-if="!loading && categories.length === 0">
      <i class="fas fa-folder-open"></i>
      <p>暂无分类数据</p>
    </div>
  </div>
</template>

<script>
import Request from '@/utils/request.js'
import logger from '@/utils/logger'

export default {
  name: 'FrontCategory',
  props: {
    layout: {
      type: String,
      default: 'grid'
    }
  },
  data() {
    return {
      categories: [],
      categoryMatches: {}, // 存储分类匹配度数据
      maxVisible: 5,
      loading: true,
      iconMap: {
        '文学小说': 'fas fa-book',
        '科技教育': 'fas fa-laptop-code',
        '历史传记': 'fas fa-history',
        '艺术设计': 'fas fa-palette',
        '经管励志': 'fas fa-trophy',
        // '水产': 'fas fa-fish',
        // '肉禽蛋': 'fas fa-drumstick-bite',
        // '干货坚果': 'fas fa-seedling',
        // '其他': 'fas fa-ellipsis-h'
      }
    }
  },
  computed: {
    isSidebar() {
      return this.layout === 'sidebar'
    },
    visibleLimit() {
      return this.isSidebar ? 6 : this.maxVisible
    },
    visibleCategories() {
      return this.categories.slice(0, this.visibleLimit)
    },
    showMore() {
      return this.categories.length > this.visibleLimit
    }
  },
  created() {
    this.fetchCategories()
    this.fetchCategoryMatches()
  },
  methods: {
    async fetchCategories() {
      try {
        this.loading = true;
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data.map(item => ({
            ...item,
            icon: this.getIconByName(item.name)
          }))
        }
      } catch (error) {
        logger.error('获取分类数据失败:', error)
        this.categories = []
      } finally {
        this.loading = false;
      }
    },
    // 获取用户分类偏好匹配度
    async fetchCategoryMatches() {
      const userStr = localStorage.getItem('frontUser');
      if (!userStr) {
        // 未登录用户使用默认值
        return;
      }
      try {
        const user = JSON.parse(userStr);
        const res = await Request.get(`/recommend/category-matches/${user.id}`);
        if (res.code === '0' && res.data) {
          this.categoryMatches = res.data;
        }
      } catch (error) {
        logger.error('获取分类匹配度失败:', error);
        // 失败时使用默认值
      }
    },
    // 获取分类匹配度
    getCategoryMatch(categoryId) {
      if (this.categoryMatches[categoryId]) {
        return this.categoryMatches[categoryId].match || 0;
      }
      // 默认值：根据索引计算（向后兼容）
      const index = this.categories.findIndex(c => c.id === categoryId);
      return index < 2 ? 92 - index * 2 : 0;
    },
    // 获取分类趋势
    getCategoryTrend(categoryId) {
      if (this.categoryMatches[categoryId]) {
        return this.categoryMatches[categoryId].trend || 0;
      }
      // 默认值：根据索引计算（向后兼容）
      const index = this.categories.findIndex(c => c.id === categoryId);
      return index >= 2 && index < 4 ? 15 + index * 2 : 0;
    },
    getIconByName(name) {
      return this.iconMap[name] || 'fas fa-ellipsis-h'
    },
    handleCategoryClick(category) {
      this.$router.push({
        name: 'category',
        params: { id: category.id },
        query: { name: category.name }
      })
    },
    handleMoreClick() {
      this.$router.push('/products')
    },
    handleAlgorithmExplore() {
      // 跳转到推荐中心或触发随机推荐
      const userStr = localStorage.getItem('frontUser');
      if (userStr) {
        this.$router.push('/recommend-center');
      } else {
        this.$message.warning('请先登录以使用算法探索功能');
        this.$router.push('/login');
      }
    }
  }
}
</script>

<style scoped>
.category-container {
  background: linear-gradient(to right, #ffffff, #f8faf5);
  padding: 24px 0;
  margin: 0 0 40px 0;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.category-container.is-sidebar {
  height: 100%;
  margin: 0;
  padding: 12px 10px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.05);
  background: #fff;
  border-radius: 14px;
}

.category-header {
  max-width: 1200px;
  margin: 0 auto 20px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-header.sidebar-header {
  margin: 0 0 8px 0;
  padding: 0 8px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.algo-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  color: #667eea;
  border-radius: 10px;
  font-weight: 500;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.category-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(circle at 0% 0%, rgba(58, 91, 160, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 100% 0%, rgba(58, 91, 160, 0.05) 0%, transparent 50%);
  opacity: 0.9;
  z-index: 0;
}

.category-container.is-sidebar::before {
  display: none;
}

.category-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 20px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  position: relative;
  z-index: 1;
  min-height: auto;
}

.category-wrapper.sidebar-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  justify-content: start;
  max-height: 520px;
  overflow-y: auto;
  padding: 6px 8px 8px;
  gap: 10px;
  max-width: none;
  margin: 0;
}

/* 加载状态 */
.category-loading {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 20px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.loading-item {
  flex: 1;
  height: 140px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 16px;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 空状态 */
.category-empty {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 20px;
  text-align: center;
  color: #909399;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.category-empty i {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.category-empty p {
  margin: 0;
  font-size: 14px;
}

.category-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
  overflow: hidden;
  animation: fadeInUp 0.5s ease forwards;
  opacity: 1;
  transform: translateY(0);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.04);
  min-height: auto;
}

.category-item.sidebar-item {
  width: 100%;
  padding: 12px 10px;
  min-height: 70px;
  align-items: center;
}

@keyframes fadeInUp {
  from {
    opacity: 0.3;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.category-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(58, 91, 160, 0.1) 0%, transparent 70%);
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.5s cubic-bezier(0.25, 1, 0.5, 1);
}

.category-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-6px) scale(1.03);
  box-shadow: 
    0 12px 28px rgba(58, 91, 160, 0.15),
    0 4px 8px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(58, 91, 160, 0.1);
  border-color: rgba(58, 91, 160, 0.2);
}

.category-item:hover::before {
  opacity: 1;
  transform: scale(1.5);
}

.icon-wrapper {
  width: 64px;
  height: 64px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  z-index: 2;
}

.category-wrapper.sidebar-layout .icon-wrapper {
  width: 52px;
  height: 52px;
  margin-bottom: 8px;
}

.icon {
  font-size: 28px;
  color: var(--primary-color);
  position: relative;
  z-index: 2;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

.category-item:hover .icon {
  animation: none;
  transform: scale(1.15);
  color: var(--primary-light);
}

.icon-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(58, 91, 160, 0.12),
    rgba(58, 91, 160, 0.18)
  );
  border-radius: 50%;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}

.category-item:hover .icon-bg {
  transform: scale(1.15);
  background: linear-gradient(135deg, 
    rgba(58, 91, 160, 0.18),
    rgba(58, 91, 160, 0.25)
  );
  box-shadow: 0 6px 15px rgba(58, 91, 160, 0.15);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(58, 91, 160, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(58, 91, 160, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(58, 91, 160, 0);
  }
}

.category-name {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  margin-top: 4px;
  z-index: 2;
}

.category-item.sidebar-item .category-name {
  font-size: 14px;
}

.category-desc {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
  z-index: 2;
  position: relative;
}

.category-item.sidebar-item .category-desc {
  font-size: 11px;
}

.match-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #67c23a, #85ce61);
  color: white;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  z-index: 2;
  position: relative;
}

.trend-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #e6a23c, #f0a020);
  color: white;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  z-index: 2;
  position: relative;
}

.category-item.high-match {
  border: 1px solid rgba(103, 194, 58, 0.2);
}

.category-item.medium-match {
  border: 1px solid rgba(230, 162, 60, 0.2);
}

.algorithm-card {
  border: 1px solid rgba(102, 126, 234, 0.2);
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
}

.algorithm-card:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  border-color: rgba(102, 126, 234, 0.4);
}

.btn-surprise {
  margin-top: 8px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 2;
  position: relative;
}

.btn-surprise:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.category-item:hover .category-name {
  color: var(--primary-color);
  transform: scale(1.05);
}

.hover-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 3px;
  background: var(--gradient-primary);
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  transform: translateX(-50%);
  border-radius: 3px;
  z-index: 2;
}

.category-item:hover .hover-indicator {
  width: 30px;
}

.more-item .icon-bg {
  background: linear-gradient(135deg, 
    rgba(144, 147, 153, 0.12),
    rgba(144, 147, 153, 0.18)
  );
}

.more-item .icon {
  color: #909399;
}

.more-item:hover .icon-bg {
  background: linear-gradient(135deg, 
    rgba(144, 147, 153, 0.18),
    rgba(144, 147, 153, 0.25)
  );
  box-shadow: 0 6px 15px rgba(144, 147, 153, 0.15);
}

.more-item:hover::before {
  background: radial-gradient(circle at center, rgba(144, 147, 153, 0.1) 0%, transparent 70%);
}

.more-item:hover .icon {
  color: #606266;
}

.more-item:hover .category-name {
  color: #606266;
}

.more-item:hover .hover-indicator {
  background: linear-gradient(to right, #909399, #c0c4cc);
}

@media (max-width: 768px) {
  .category-wrapper {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .category-item {
    flex: 0 0 calc(25% - 9px);
    padding: 12px 8px;
  }
  
  .icon-wrapper {
    width: 50px;
    height: 50px;
    margin-bottom: 8px;
  }
  
  .icon {
    font-size: 24px;
  }
  
  .category-name {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .category-container {
    padding: 16px 0;
  }
  
  .category-item {
    flex: 0 0 calc(33.33% - 8px);
    padding: 10px 6px;
  }
  
  .icon-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .icon {
    font-size: 20px;
  }
  
  .category-name {
    font-size: 13px;
  }
}
</style> 