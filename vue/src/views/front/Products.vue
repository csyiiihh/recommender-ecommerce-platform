<template>
  <div class="products-page landscape-bg">
    <front-header></front-header>
    <div class="main-content cloud-decoration">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="title-container">
          <div class="title-wrapper">
            <div class="title-icon-wrapper">
              <i class="fas fa-store"></i>
              <div class="icon-glow"></div>
            </div>
            <div class="title-content">
              <h2 class="page-title">
                <span>全部商品</span>
                <div class="title-accent"></div>
              </h2>
              <div class="page-subtitle">
                <span class="subtitle-text">精选优质图书，品质保障</span>
                <div class="products-count" v-if="total > 0">
                  <i class="fas fa-book"></i>
                  共 {{ total }} 本图书
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="search-section">
          <div class="search-box">
            <div class="search-input-wrapper">
              <el-input 
                v-model="searchKeyword" 
                placeholder="请输入图书名称、ISBN等关键词" 
                clearable
                @clear="handleSearch"
                @keyup.enter="handleSearch"
                class="modern-search"
                size="large"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button 
                type="primary" 
                @click="handleSearch"
                class="search-btn"
                size="large"
              >
                <el-icon><Search /></el-icon>
              </el-button>
            </div>
            <div class="search-suggestions" v-if="showSuggestions && searchSuggestions.length">
              <div 
                v-for="suggestion in searchSuggestions" 
                :key="suggestion"
                class="suggestion-item"
                @click="selectSuggestion(suggestion)"
              >
                <el-icon><Search /></el-icon>
                {{ suggestion }}
              </div>
            </div>
          </div>
          <!-- 图书字段搜索区域 -->
          <div class="book-search-fields">
            <el-input 
              v-model="searchAuthor" 
              placeholder="搜索作者" 
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              class="field-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
            <el-input 
              v-model="searchPublisher" 
              placeholder="搜索出版社" 
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              class="field-input"
            >
              <template #prefix>
                <el-icon><OfficeBuilding /></el-icon>
              </template>
            </el-input>
            <el-input 
              v-model="searchIsbn" 
              placeholder="搜索ISBN" 
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              class="field-input"
            >
              <template #prefix>
                <el-icon><Document /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <!-- 顶部过滤器 -->
      <div class="filter-section">
        <div class="filter-bar">
          <div class="filter-group">
            <div class="filter-item">
              <el-dropdown trigger="click" @command="handleCategoryChange" class="modern-dropdown">
                <span class="filter-label" :class="{'active': selectedCategory}">
                  <div class="filter-icon">
                    <i class="fas fa-th-large"></i>
                  </div>
                  <div class="filter-text">
                    <span class="filter-title">分类</span>
                    <span class="filter-value" v-if="selectedCategory">{{ getCategoryName(selectedCategory) }}</span>
                  </div>
                  <i class="fas fa-chevron-down filter-arrow"></i>
                </span>
                <template #dropdown>
                  <el-dropdown-menu class="enhanced-dropdown">
                    <el-dropdown-item command="" class="dropdown-item">
                      <i class="fas fa-th"></i>
                      全部图书
                    </el-dropdown-item>
                    <el-dropdown-item 
                      v-for="category in categories" 
                      :key="category.id"
                      :command="category.id"
                      class="dropdown-item"
                    >
                      <i class="fas fa-tag"></i>
                      {{ category.name }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="filter-item">
              <el-dropdown trigger="click" @command="handlePriceRangeChange" class="modern-dropdown">
                <span class="filter-label" :class="{'active': priceRange}">
                  <div class="filter-icon">
                    <i class="fas fa-dollar-sign"></i>
                  </div>
                  <div class="filter-text">
                    <span class="filter-title">价格</span>
                    <span class="filter-value" v-if="priceRange">{{ getPriceRangeLabel(priceRange) }}</span>
                  </div>
                  <i class="fas fa-chevron-down filter-arrow"></i>
                </span>
                <template #dropdown>
                  <el-dropdown-menu class="enhanced-dropdown">
                    <el-dropdown-item command="" class="dropdown-item">
                      <i class="fas fa-infinity"></i>
                      全部价格
                    </el-dropdown-item>
                    <el-dropdown-item 
                      v-for="(range, index) in priceRanges" 
                      :key="index"
                      :command="range.value"
                      class="dropdown-item"
                    >
                      <i class="fas fa-coins"></i>
                      {{ range.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="filter-item">
              <el-dropdown trigger="click" @command="handleSortChange" class="modern-dropdown">
                <span class="filter-label" :class="{'active': sortBy !== 'default'}">
                  <div class="filter-icon">
                    <i class="fas fa-sort-amount-down"></i>
                  </div>
                  <div class="filter-text">
                    <span class="filter-title">排序</span>
                    <span class="filter-value" v-if="sortBy !== 'default'">{{ getSortLabel(sortBy) }}</span>
                  </div>
                  <i class="fas fa-chevron-down filter-arrow"></i>
                </span>
                <template #dropdown>
                  <el-dropdown-menu class="enhanced-dropdown">
                    <el-dropdown-item 
                      v-for="option in sortOptions" 
                      :key="option.value"
                      :command="option.value"
                      class="dropdown-item"
                    >
                      <i :class="getSortIcon(option.value)"></i>
                      {{ option.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div class="filter-actions">
            <div class="view-options">
              <el-button-group class="view-toggle">
                <el-button 
                  :type="viewMode === 'grid' ? 'primary' : ''"
                  @click="viewMode = 'grid'"
                  size="small"
                >
                  <i class="fas fa-th"></i>
                </el-button>
                <el-button 
                  :type="viewMode === 'list' ? 'primary' : ''"
                  @click="viewMode = 'list'"
                  size="small"
                >
                  <i class="fas fa-list"></i>
                </el-button>
              </el-button-group>
            </div>
            <el-button 
              link 
              class="reset-btn" 
              @click="resetFilters" 
              v-if="hasFilters"
            >
              <i class="fas fa-undo"></i> 重置筛选
            </el-button>
          </div>
        </div>
        
        <!-- 快速筛选标签 -->
        <div class="quick-filters" v-if="categories.length">
          <div class="quick-filters-title">热门分类：</div>
          <div class="quick-filter-tags">
            <el-tag 
              v-for="category in popularCategories" 
              :key="category.id"
              :type="selectedCategory === category.id ? 'success' : 'info'"
              :effect="selectedCategory === category.id ? 'dark' : 'light'"
              @click="handleCategoryChange(category.id)"
              class="quick-tag"
            >
              {{ category.name }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 已选择的过滤条件 -->
      <div class="selected-filters" v-if="hasFilters">
        <div class="selected-filters-title">已选条件：</div>
        <div class="selected-filters-content">
          <el-tag 
            v-if="selectedCategory" 
            closable
            @close="handleCategoryChange('')"
            type="success"
            effect="light"
          >
            分类: {{ getCategoryName(selectedCategory) }}
          </el-tag>
          <el-tag 
            v-if="priceRange" 
            closable
            @close="handlePriceRangeChange('')"
            type="success"
            effect="light"
          >
            价格: {{ getPriceRangeLabel(priceRange) }}
          </el-tag>
          <el-tag 
            v-if="sortBy !== 'default'" 
            closable
            @close="handleSortChange('default')"
            type="success"
            effect="light"
          >
            {{ getSortLabel(sortBy) }}
          </el-tag>
          <el-tag 
            v-if="searchKeyword" 
            closable
            @close="clearSearch"
            type="success"
            effect="light"
          >
            关键词: {{ searchKeyword }}
          </el-tag>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="products-section" v-loading="loading" element-loading-text="正在加载图书...">
        <div class="products-header" v-if="products.length > 0">
          <div class="results-info">
            <span class="results-text">
              <i class="fas fa-filter"></i>
              找到 <strong>{{ total }}</strong> 本图书
            </span>
            <div class="loading-indicator" v-if="loading">
              <i class="fas fa-spinner fa-spin"></i>
              加载中...
            </div>
          </div>
        </div>
        
        <transition-group 
          name="fade-list" 
          tag="div" 
          :class="{
            'grid-container': viewMode === 'grid',
            'list-container': viewMode === 'list'
          }"
        >
          <div 
            v-for="(product, index) in products" 
            :key="product.id"
            :class="{
              'product-item': viewMode === 'grid',
              'product-list-item': viewMode === 'list'
            }"
            :style="{ 'animation-delay': (index * 0.05) + 's' }"
          >
            <product-card 
              :product="product" 
              :view-mode="viewMode"
              @add-to-cart="handleAddToCart"
              @toggle-favorite="handleToggleFavorite"
            />
          </div>
        </transition-group>
        
        <div v-if="!loading && products.length === 0" class="empty-state">
          <div class="empty-icon">
            <i class="fas fa-search"></i>
          </div>
          <div class="empty-content">
            <h3>暂无相关图书</h3>
            <p>试试调整筛选条件或搜索其他关键词</p>
            <div class="empty-actions">
              <el-button type="primary" @click="resetFilters">
                <i class="fas fa-undo"></i>
                清除筛选条件
              </el-button>
              <el-button @click="$router.push('/')">
                <i class="fas fa-home"></i>
                返回首页
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          background
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="sizes, prev, pager, next, jumper, total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        >
        </el-pagination>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import ProductCard from '@/components/front/ProductCard.vue'
import Request from '@/utils/request'
import { debounce } from 'lodash'
import eventBus from '@/utils/eventBus'
import logger from '@/utils/logger'
import { User, OfficeBuilding, Document, Search } from '@element-plus/icons-vue'

export default {
  name: 'Products',
  components: {
    FrontHeader,
    FrontFooter,
    ProductCard,
    User,
    OfficeBuilding,
    Document,
    Search
  },
  data() {
    return {
      loading: false,
      products: [],
      categories: [],
      selectedCategory: '',
      priceRange: '',
      priceRanges: [
        { label: '0-50元', value: '0-50' },
        { label: '50-100元', value: '50-100' },
        { label: '100-200元', value: '100-200' },
        { label: '200元以上', value: '200-' }
      ],
      sortOptions: [
        { label: '默认排序', value: 'default' },
        { label: '销量优先', value: 'sales,desc' },
        { label: '价格从低到高', value: 'price,asc' },
        { label: '价格从高到低', value: 'price,desc' },
        { label: '新品优先', value: 'createdAt,desc' }
      ],
      sortBy: 'default',
      searchKeyword: '',
      searchAuthor: '',
      searchPublisher: '',
      searchIsbn: '',
      currentPage: 1,
      pageSize: 12,
      total: 0,
      debouncedSearch: null,
      viewMode: 'grid', // 'grid' | 'list'
      showSuggestions: false,
      searchSuggestions: ['文学小说', '科技教育', '历史传记', '艺术设计', '经管励志'],
      popularCategories: []
    }
  },
  computed: {
    hasFilters() {
      return this.selectedCategory || this.priceRange || this.sortBy !== 'default' || this.searchKeyword || this.searchAuthor || this.searchPublisher || this.searchIsbn
    }
  },
  methods: {
    // 获取商品分类
    async getCategories() {
      try {
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data
        }
      } catch (error) {
        logger.error('获取分类失败:', error)
      }
    },
    // 获取商品列表
    async getProducts() {
      this.loading = true
      try {
        const params = {
          status: 1,
          currentPage: this.currentPage,
          size: this.pageSize
        }

        // 添加分类筛选
        if (this.selectedCategory) {
          params.categoryId = this.selectedCategory
        }

        // 添加价格区间筛选
        if (this.priceRange) {
          const [min, max] = this.priceRange.split('-')
          if (min) params.minPrice = min
          if (max) params.maxPrice = max
        }

        // 添加排序
        if (this.sortBy !== 'default') {
          const [field, order] = this.sortBy.split(',')
          params.sortField = field
          params.sortOrder = order
        }

        // 添加搜索关键词
        if (this.searchKeyword) {
          params.name = this.searchKeyword
        }
        // 添加图书相关搜索
        if (this.searchAuthor) {
          params.author = this.searchAuthor
        }
        if (this.searchPublisher) {
          params.publisher = this.searchPublisher
        }
        if (this.searchIsbn) {
          params.isbn = this.searchIsbn
        }

        const res = await Request.get('/product/page', { params })
        if (res.code === '0') {
          if (res.data && res.data.records) {
            this.products = res.data.records.map(product => ({
              ...product,
              isFavorite: false,
              imageUrl: product.imageUrl?.startsWith('http') ? product.imageUrl : `${product.imageUrl}`
            }))
            this.total = res.data.total
          } else {
            this.products = []
            this.total = 0
          }
        } else {
          this.products = []
          this.total = 0
        }
      } catch (error) {
        logger.error('获取图书列表失败:', error)
        this.$message.error('获取图书列表失败')
        this.products = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleCategoryChange(categoryId) {
      this.selectedCategory = categoryId
      this.currentPage = 1
      this.getProducts()
    },
    handlePriceRangeChange(range) {
      this.priceRange = range
      this.currentPage = 1
      this.getProducts()
    },
    handleSortChange(value) {
      this.sortBy = value
      this.currentPage = 1
      this.getProducts()
    },
    handleSearch() {
      this.debouncedSearch()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.getProducts()
    },
    handleRouteChange() {
      const query = {}
      if (this.selectedCategory) {
        query.category = this.selectedCategory
      }
      if (this.searchKeyword) {
        query.keyword = this.searchKeyword
      }
      // 更新URL，但不触发路由变化
      this.$router.replace({ query }).catch(() => {})
    },
    getCategoryName(id) {
      const category = this.categories.find(c => c.id === id)
      return category ? category.name : '全部'
    },
    getPriceRangeLabel(value) {
      const range = this.priceRanges.find(r => r.value === value)
      return range ? range.label : '全部'
    },
    getSortLabel(value) {
      const option = this.sortOptions.find(o => o.value === value)
      return option ? option.label : '默认排序'
    },
    clearSearch() {
      this.searchKeyword = ''
      this.handleSearch()
    },
    resetFilters() {
      this.selectedCategory = ''
      this.priceRange = ''
      this.sortBy = 'default'
      this.searchKeyword = ''
      this.searchAuthor = ''
      this.searchPublisher = ''
      this.searchIsbn = ''
      this.currentPage = 1
      this.getProducts()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.getProducts()
    },
    
    // 新增方法
    selectSuggestion(suggestion) {
      this.searchKeyword = suggestion
      this.showSuggestions = false
      this.handleSearch()
    },
    
    getSortIcon(value) {
      const icons = {
        'default': 'fas fa-sort',
        'sales,desc': 'fas fa-fire',
        'price,asc': 'fas fa-sort-amount-up',
        'price,desc': 'fas fa-sort-amount-down',
        'createdAt,desc': 'fas fa-star'
      }
      return icons[value] || 'fas fa-sort'
    },
    
    handleAddToCart(product) {
      this.$message({
        type: 'success',
        message: '已添加到购物车'
      })
    },
    
    handleToggleFavorite({ product, status }) {
      // 更新本地数据
      const targetProduct = this.products.find(p => p.id === product.id)
      if (targetProduct) {
        // status现在传递的是boolean值，不再是1/0
        const isFavorite = typeof status === 'boolean' ? status : status === 1
        targetProduct.isFavorite = isFavorite
        
        logger.debug('Products页面更新收藏状态:', {
          productId: product.id,
          productName: product.name,
          newStatus: isFavorite
        })
      }
    },
    
    // 设置热门分类
    setPopularCategories() {
      // 取前5个分类作为热门分类
      this.popularCategories = this.categories.slice(0, 5)
    },

    // 处理全局收藏状态变化
    handleGlobalFavoriteChange(data) {
      logger.debug('Products页收到全局收藏状态变化:', data)
      
      // 更新产品列表中对应商品的收藏状态
      this.products.forEach((product, index) => {
        if (product.id === data.productId) {
          this.products[index].isFavorite = data.status
          logger.debug(`更新商品 ${product.name} 收藏状态为:`, data.status)
        }
      })
    }
  },
  watch: {
    searchKeyword() {
      this.handleSearch()
      this.handleRouteChange()
    },
    selectedCategory() {
      this.handleRouteChange()
    },
    sortBy() {
      this.currentPage = 1
      this.getProducts()
    }
  },
  created() {
    this.debouncedSearch = debounce(() => {
      this.currentPage = 1
      this.getProducts()
    }, 300)

    this.getCategories()
    this.getProducts()
    
    const { category, keyword } = this.$route.query
    if (category) this.selectedCategory = category
    if (keyword) {
      this.searchKeyword = keyword
      this.handleSearch()
    }
    
    // 设置热门分类
    this.$nextTick(() => {
      this.setPopularCategories()
    })
    
    // 监听全局收藏状态变化事件，实现页面间收藏状态同步
    eventBus.on('favoriteStatusChanged', this.handleGlobalFavoriteChange)
  },
  
  beforeUnmount() {
    // Vue 3 使用 beforeUnmount 替代 beforeDestroy
    // 移除事件监听器
    eventBus.off('favoriteStatusChanged', this.handleGlobalFavoriteChange)
    
    if (this.debouncedSearch) {
      this.debouncedSearch.cancel()
    }
  }
}
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #ffffff 0%, #f5f7fa 50%, #ecf5ff 100%);
  position: relative;
  overflow-x: hidden;
}

.products-page::before {
  content: '';
  position: fixed;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 25% 25%, rgba(58, 91, 160, 0.02) 0%, transparent 50%),
              radial-gradient(circle at 75% 75%, rgba(91, 124, 189, 0.02) 0%, transparent 50%);
  z-index: -1;
  animation: backgroundFloat 25s ease-in-out infinite;
}

@keyframes backgroundFloat {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(-10px, -5px) rotate(0.5deg); }
  66% { transform: translate(5px, -10px) rotate(-0.5deg); }
}

.main-content {
  flex: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* 页面标题样式 */
.page-header {
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 24px;
  border-radius: 18px;
  box-shadow: 0 6px 24px rgba(58, 91, 160, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  animation: slideInFromTop 0.8s ease-out;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--primary-light) 50%, var(--primary-color) 100%);
  background-size: 200% 100%;
  animation: gradientMove 3s ease-in-out infinite;
}

@keyframes gradientMove {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes slideInFromTop {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.title-container {
  flex: 1;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.title-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s ease;
  animation: iconPulse 3s ease-in-out infinite;
  flex-shrink: 0;
}

.title-icon-wrapper:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 30px rgba(58, 91, 160, 0.3);
}

@keyframes iconPulse {
  0%, 100% { box-shadow: 0 8px 20px rgba(58, 91, 160, 0.2); }
  50% { box-shadow: 0 8px 20px rgba(58, 91, 160, 0.4), 0 0 30px rgba(58, 91, 160, 0.1); }
}

.title-icon-wrapper i {
  color: white;
  font-size: 26px;
}

.icon-glow {
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  border-radius: 23px;
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.3), rgba(91, 124, 189, 0.3));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.title-icon-wrapper:hover .icon-glow {
  opacity: 1;
  animation: glowPulse 1.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.1); opacity: 0.6; }
}

.title-content {
  flex: 1;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  position: relative;
}

.page-title span {
  position: relative;
  display: inline-block;
}

.title-accent {
  position: absolute;
  bottom: -4px;
  left: 0;
  height: 3px;
  width: 0;
  background: var(--gradient-primary);
  border-radius: 2px;
  animation: accentGrow 2s ease-in-out 0.5s forwards;
}

@keyframes accentGrow {
  from { width: 0; }
  to { width: 100%; }
}

.page-subtitle {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 0;
}

.subtitle-text {
  font-size: 16px;
  color: #64748b;
  font-weight: 400;
}

.products-count {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(58, 91, 160, 0.1);
  color: var(--primary-color);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.search-section {
  width: 100%;
  max-width: 100%;
  position: relative;
  margin-top: 20px;
}

.search-box {
  position: relative;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  width: 100%;
}

.modern-search {
  flex: 1;
}

.modern-search :deep(.el-input__inner) {
  height: 48px;
  border: 1.5px solid #e2e8f0;
  border-radius: 24px;
  padding-left: 42px;
  padding-right: 16px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(58, 91, 160, 0.1), 0 1px 3px rgba(0, 0, 0, 0.08);
}

.modern-search :deep(.el-input__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(58, 91, 160, 0.15), 0 4px 20px rgba(58, 91, 160, 0.2);
  background: rgba(255, 255, 255, 1);
}

.modern-search :deep(.el-input__prefix) {
  left: 14px;
}

.modern-search :deep(.el-input__prefix .el-icon) {
  color: var(--primary-color);
  font-size: 18px;
}

.search-btn {
  height: 48px;
  width: 48px;
  min-width: 48px;
  border-radius: 24px;
  background: var(--gradient-primary);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.25);
  flex-shrink: 0;
}

.search-btn .el-icon {
  font-size: 18px;
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(58, 91, 160, 0.35);
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 52px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid #e2e8f0;
  z-index: 1000;
  margin-top: 8px;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #64748b;
  font-size: 14px;
}

.suggestion-item:hover {
  background: rgba(58, 91, 160, 0.08);
  color: var(--primary-color);
}

.suggestion-item .el-icon {
  font-size: 14px;
  opacity: 0.7;
}

.book-search-fields {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.field-input {
  flex: 1;
  min-width: 160px;
  max-width: 220px;
}

/* 过滤器样式 */
.filter-section {
  margin-bottom: 24px;
  animation: slideInFromLeft 0.8s ease-out 0.2s both;
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.filter-bar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.2);
  overflow: hidden;
}

.filter-group {
  display: flex;
  position: relative;
}

.filter-item {
  position: relative;
}

.filter-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 30px;
  background: linear-gradient(to bottom, transparent, #e2e8f0, transparent);
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 20px 24px;
  transition: all 0.3s ease;
  color: #64748b;
  font-size: 15px;
  position: relative;
  font-weight: 500;
  min-width: 120px;
}

.filter-label:hover {
  background: rgba(58, 91, 160, 0.08);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.filter-label.active {
  background: rgba(58, 91, 160, 0.12);
  color: var(--primary-color);
}

.filter-label.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.filter-icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: rgba(58, 91, 160, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.filter-label:hover .filter-icon,
.filter-label.active .filter-icon {
  background: rgba(58, 91, 160, 0.15);
  transform: scale(1.1);
}

.filter-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 60px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.filter-value {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
  line-height: 1;
}

.filter-arrow {
  font-size: 12px;
  transition: transform 0.3s ease;
  opacity: 0.6;
}

.filter-label:hover .filter-arrow {
  transform: translateY(-1px);
  opacity: 1;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-right: 24px;
}

.view-options {
  display: flex;
  align-items: center;
}

.view-toggle {
  border-radius: 12px;
  overflow: hidden;
  background: rgba(58, 91, 160, 0.1);
}

.view-toggle :deep(.el-button) {
  border: none;
  background: transparent;
  color: var(--primary-color);
  padding: 8px 12px;
  border-radius: 0;
  transition: all 0.3s ease;
}

.view-toggle :deep(.el-button--primary) {
  background: var(--primary-color);
  color: white;
}

.reset-btn {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.reset-btn:hover {
  background: rgba(58, 91, 160, 0.1);
  transform: translateY(-1px);
}

.reset-btn i {
  margin-right: 6px;
}

/* 快速筛选标签 */
.quick-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(236, 245, 255, 0.8);
  border-radius: 16px;
  border: 1px dashed rgba(58, 91, 160, 0.2);
}

.quick-filters-title {
  font-size: 14px;
  color: var(--primary-color);
  font-weight: 600;
  white-space: nowrap;
}

.quick-filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 20px;
}

.quick-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.2);
}

/* 已选择的过滤条件 */
.selected-filters {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: rgba(58, 91, 160, 0.06);
  border-radius: 16px;
  border: 1px dashed rgba(58, 91, 160, 0.25);
}

.selected-filters-title {
  font-size: 14px;
  color: var(--primary-color);
  font-weight: 500;
  margin-right: 16px;
}

.selected-filters-content {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.selected-filters :deep(.el-tag) {
  padding: 0 12px;
  height: 32px;
  line-height: 30px;
  border-radius: 16px;
  font-size: 13px;
}

.selected-filters :deep(.el-tag .el-tag__close) {
  background-color: transparent;
  color: var(--primary-color);
  font-weight: bold;
  right: 0;
}

.selected-filters :deep(.el-tag .el-tag__close:hover) {
  background-color: var(--primary-color);
  color: white;
}

/* 下拉菜单样式优化 */
:deep(.enhanced-dropdown) {
  padding: 12px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(20px);
  background: rgba(255, 255, 255, 0.95);
}

:deep(.dropdown-item) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  font-size: 14px;
  border-radius: 12px;
  margin: 4px 0;
  color: #64748b;
  transition: all 0.3s ease;
  cursor: pointer;
}

:deep(.dropdown-item:hover) {
  background: rgba(58, 91, 160, 0.12);
  color: var(--primary-color);
  transform: translateX(4px);
}

:deep(.dropdown-item i) {
  width: 16px;
  font-size: 14px;
  opacity: 0.7;
}

:deep(.dropdown-item:hover i) {
  opacity: 1;
  color: var(--primary-color);
}

/* 商品网格 */
.products-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-height: 500px;
  position: relative;
  animation: slideInFromBottom 0.8s ease-out 0.4s both;
}

@keyframes slideInFromBottom {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.products-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
}

.results-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.results-text {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #64748b;
  font-weight: 500;
}

.results-text i {
  color: var(--primary-color);
  font-size: 16px;
}

.results-text strong {
  color: var(--primary-color);
  font-weight: 700;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary-color);
  font-size: 14px;
}

.loading-indicator i {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-item {
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  transform: translateY(20px);
}

.product-list-item {
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  transform: translateY(20px);
}

.product-item:hover,
.product-list-item:hover {
  transform: translateY(-2px);
}

@keyframes fadeInUp {
  0% {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-5px) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 动画效果 */
.fade-list-enter-active, .fade-list-leave-active {
  transition: all 0.3s ease;
}

.fade-list-enter, .fade-list-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  animation: fadeIn 0.8s ease-out;
}

.empty-icon {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.1), rgba(58, 91, 160, 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

.empty-icon i {
  font-size: 48px;
  color: rgba(58, 91, 160, 0.6);
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-content h3 {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 12px 0;
}

.empty-content p {
  font-size: 16px;
  margin: 0 0 32px 0;
  color: #6b7280;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.empty-actions .el-button {
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 500;
}

.empty-actions .el-button--primary {
  background: var(--gradient-primary);
  border: none;
}

.empty-actions .el-button i {
  margin-right: 6px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding: 20px 0;
  animation: slideInFromBottom 0.8s ease-out 0.6s both;
}

:deep(.el-pagination) {
  padding: 20px 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 40px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

:deep(.el-pagination.is-background .el-pager li) {
  margin: 0 6px;
  border-radius: 12px;
  min-width: 40px;
  height: 40px;
  line-height: 38px;
  transition: all 0.3s ease;
  font-weight: 500;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: var(--gradient-primary);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.3);
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: var(--primary-color);
  background: rgba(58, 91, 160, 0.1);
  transform: translateY(-1px);
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 12px;
  margin: 0 6px;
  width: 40px;
  height: 40px;
  transition: all 0.3s ease;
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: rgba(58, 91, 160, 0.1);
  color: var(--primary-color);
  transform: translateY(-1px);
}

:deep(.el-select .el-input) {
  margin: 0 12px;
}

:deep(.el-select .el-input .el-input__inner) {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  transition: all 0.3s ease;
}

:deep(.el-select .el-input .el-input__inner:focus) {
  border-color: var(--primary-color);
}

:deep(.el-pagination__total) {
  margin-right: 20px;
  font-weight: 500;
  color: #64748b;
}

:deep(.el-pagination__jump) {
  margin-left: 20px;
  font-weight: 500;
  color: #64748b;
}

:deep(.el-pagination__jump .el-input .el-input__inner) {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  transition: all 0.3s ease;
}

:deep(.el-pagination__jump .el-input .el-input__inner:focus) {
  border-color: var(--primary-color);
}

/* 响应式布局优化 */
@media (max-width: 1200px) {
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
  }
  
  .products-section {
    padding: 28px;
  }
  
  .search-section {
    width: 350px;
  }
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 24px;
    padding: 28px;
  }
  
  .search-section {
    width: 100%;
  }
  
  .title-wrapper {
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .products-page::before {
    animation: none; /* 禁用复杂动画以提高性能 */
  }
  
  .main-content {
    padding: 16px;
  }
  
  .page-header {
    padding: 24px 20px;
    margin-bottom: 20px;
    border-radius: 20px;
  }
  
  .title-icon-wrapper {
    width: 50px;
    height: 50px;
  }
  
  .title-icon-wrapper i {
    font-size: 24px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .subtitle-text {
    font-size: 14px;
  }
  
  .products-count {
    font-size: 12px;
    padding: 3px 8px;
  }

  .filter-bar {
    flex-direction: column;
    padding: 0;
    border-radius: 16px;
  }
  
  .filter-group {
    width: 100%;
    overflow-x: auto;
    padding: 0;
  }
  
  .filter-label {
    padding: 16px 20px;
    min-width: 100px;
    font-size: 14px;
  }
  
  .filter-text {
    min-width: 50px;
  }
  
  .filter-title {
    font-size: 13px;
  }
  
  .filter-value {
    font-size: 11px;
  }
  
  .filter-actions {
    width: 100%;
    padding: 16px 20px;
    border-top: 1px solid rgba(58, 91, 160, 0.1);
    justify-content: space-between;
  }
  
  .quick-filters {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }
  
  .quick-filters-title {
    margin-bottom: 0;
  }

  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
  }
  
  .list-container {
    gap: 12px;
  }
  
  .products-section {
    padding: 20px 16px;
    border-radius: 16px;
  }
  
  .products-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .results-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .pagination-wrapper {
    margin-top: 32px;
  }
  
  :deep(.el-pagination) {
    padding: 16px 20px;
    border-radius: 24px;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }
  
  .search-input-wrapper {
    flex-direction: column;
    gap: 12px;
  }
  
  .search-btn {
    width: 100%;
    border-radius: 16px;
  }
  
  .search-suggestions {
    right: 0;
  }
}

@media (max-width: 480px) {
  .page-header {
    padding: 20px 16px;
  }
  
  .title-wrapper {
    gap: 12px;
  }
  
  .title-icon-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .title-icon-wrapper i {
    font-size: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 8px;
  }
  
  .products-section {
    padding: 16px 12px;
  }
  
  .empty-state {
    padding: 60px 20px;
  }
  
  .empty-icon {
    width: 80px;
    height: 80px;
  }
  
  .empty-icon i {
    font-size: 32px;
  }
  
  .empty-content h3 {
    font-size: 18px;
  }
  
  .empty-content p {
    font-size: 14px;
  }
  
  .empty-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .empty-actions .el-button {
    width: 100%;
  }
}

/* 无障碍性优化 */
@media (prefers-reduced-motion: reduce) {
  .products-page::before,
  .title-icon-wrapper,
  .product-item,
  .product-list-item,
  .page-header,
  .filter-section,
  .products-section,
  .pagination-wrapper,
  .empty-icon {
    animation: none !important;
    transition: opacity 0.2s ease !important;
  }
  
  .product-item,
  .product-list-item {
    opacity: 1;
    transform: none;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .products-page {
    background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1e2d1e 100%);
  }
  
  .page-header,
  .filter-bar,
  .products-section {
    background: rgba(45, 45, 45, 0.95);
    color: #e0e0e0;
  }
  
  .page-title {
    color: #e0e0e0;
  }
  
  .subtitle-text {
    color: #b0b0b0;
  }
}

/* 性能优化 */
.product-item,
.product-list-item,
.page-header,
.filter-bar,
.products-section {
  will-change: transform;
}

/* 滚动条优化 */
.filter-group::-webkit-scrollbar {
  height: 4px;
}

.filter-group::-webkit-scrollbar-track {
  background: rgba(58, 91, 160, 0.1);
  border-radius: 2px;
}

.filter-group::-webkit-scrollbar-thumb {
  background: rgba(58, 91, 160, 0.3);
  border-radius: 2px;
}

.filter-group::-webkit-scrollbar-thumb:hover {
  background: rgba(58, 91, 160, 0.5);
}
</style> 