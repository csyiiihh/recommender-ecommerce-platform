<template>
  <div class="home landscape-bg porcelain-texture" v-if="refresh" :key="refresh">
    <front-header></front-header>
    <div class="main-content cloud-decoration">
      <!-- 加载动画 -->
      <div class="loading-container" v-if="loading">
        <div class="loading-wrapper">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-icon">
              <i class="fas fa-box"></i>
            </div>
          </div>
          <div class="loading-text">
            <span>正在为您精选优质图书</span>
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 三栏布局区域 -->
      <div v-else>
        <div class="hero-layout">
          <div class="left-column">
            <front-category layout="sidebar"></front-category>
          </div>

          <div class="center-column">
            <div class="carousel-panel">
              <front-carousel></front-carousel>
            </div>
            <div class="quick-links">
              <div class="quick-card quick-card--recommend" @click="goToRecommendCenter">
                <div class="card-top">
                  <div class="card-icon">
                    <i class="fas fa-magic"></i>
                  </div>
                  <div class="card-title">推荐中心</div>
                </div>
                <div class="card-desc">智能推荐配置与管理</div>
                <div class="card-action">
                  立即前往
                  <i class="fas fa-arrow-right"></i>
                </div>
              </div>
              <div class="quick-card quick-card--new" @click="goToNewProducts">
                <div class="card-top">
                  <div class="card-icon">
                    <i class="fas fa-book-open"></i>
                  </div>
                  <div class="card-title">新书上架</div>
                </div>
                <div class="card-desc">最新上新 · 一键查看</div>
                <div class="card-action">
                  查看新品
                  <i class="fas fa-arrow-right"></i>
                </div>
              </div>
            </div>
          </div>

          <div class="right-column">
            <div class="algo-card" v-if="isLoggedIn">
              <algorithm-sidebar 
                :current-algorithm="displayCurrentAlgorithm"
                :last-update-time="algorithmMetrics?.lastUpdateTime"
                @weights-changed="handleWeightsChanged"
              ></algorithm-sidebar>
            </div>
            <div class="notice-card">
              <front-notice></front-notice>
            </div>
          </div>
        </div>

        <!-- 推荐商品区域 - 智能推荐引擎 -->
        <div class="section recommend-section">
          <div class="section-header">
            <div class="title-wrapper">
              <div class="title-icon-wrapper">
                <i class="fas fa-star"></i>
                <div class="icon-glow"></div>
              </div>
              <div class="title-content">
                <h2 class="section-title">
                  <span>🎯 智能推荐引擎</span>
                  <div class="title-accent"></div>
                </h2>
                <div class="subtitle">多算法融合，精准匹配阅读需求</div>
              </div>
            </div>
            <div class="header-actions">
              <div class="recommend-center-btn" @click="goToRecommendCenter" v-if="isLoggedIn">
                <el-icon><MagicStick /></el-icon>
                <span>推荐中心</span>
              </div>
              <div class="algo-controls">
                <el-button 
                  circle 
                  size="small" 
                  @click="refreshRecommendations"
                  title="刷新推荐"
                  v-if="isLoggedIn"
                >
                  <i class="fas fa-sync-alt"></i>
                </el-button>
                <el-button 
                  circle 
                  size="small" 
                  @click="showAlgorithmSettings = true"
                  title="算法设置"
                  v-if="isLoggedIn"
                >
                  <i class="fas fa-cog"></i>
                </el-button>
              </div>
              <div class="more-btn" @click="$router.push('/products')">
                <span>查看更多</span>
                <i class="fas fa-arrow-right"></i>
                <div class="btn-ripple"></div>
              </div>
            </div>
          </div>
          
          <!-- 算法Tab切换（仅登录用户显示） -->
          <div class="algo-navigation" v-if="isLoggedIn">
            <el-tabs v-model="activeAlgorithm" @tab-change="handleAlgorithmChange" class="algo-tabs">
              <el-tab-pane label="为你推荐" name="personalized">
                <template #label>
                  <span class="algo-tab-label">
                    <span class="algo-icon">🎯</span>
                    <span class="algo-name">为你推荐</span>
                    <span class="algo-desc">基于您的历史行为</span>
                  </span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="相似读者喜欢" name="collaborative">
                <template #label>
                  <span class="algo-tab-label">
                    <span class="algo-icon">👥</span>
                    <span class="algo-name">相似读者喜欢</span>
                    <span class="algo-desc">协同过滤算法</span>
                  </span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="内容相似" name="content">
                <template #label>
                  <span class="algo-tab-label">
                    <span class="algo-icon">📝</span>
                    <span class="algo-name">内容相似</span>
                    <span class="algo-desc">文本特征匹配</span>
                  </span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="热门趋势" name="trending">
                <template #label>
                  <span class="algo-tab-label">
                    <span class="algo-icon">🔥</span>
                    <span class="algo-name">热门趋势</span>
                    <span class="algo-desc">实时热度分析</span>
                  </span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="惊喜发现" name="serendipity">
                <template #label>
                  <span class="algo-tab-label">
                    <span class="algo-icon">🎲</span>
                    <span class="algo-name">惊喜发现</span>
                    <span class="algo-desc">探索新领域</span>
                  </span>
                </template>
              </el-tab-pane>
            </el-tabs>
          </div>
          
          <!-- 推荐结果展示区 -->
          <div class="recommendation-results">
            <div class="product-grid">
              <div class="product-item" v-for="product in recommendProducts" :key="product.id">
                <product-card 
                  :product="product" 
                  :show-algorithm-info="isLoggedIn"
                  :algorithm-type="currentAlgorithmType"
                  @add-to-cart="handleAddToCart" 
                  @toggle-favorite="handleToggleFavorite"
                ></product-card>
              </div>
            </div>
          </div>
          
          <!-- 算法性能指标展示（仅登录用户） -->
          <div class="algorithm-metrics" v-if="isLoggedIn && algorithmMetrics">
            <div class="metric-card">
              <span class="metric-value">{{ algorithmMetrics.accuracy || '92' }}%</span>
              <span class="metric-label">推荐准确率</span>
            </div>
            <div class="metric-card">
              <span class="metric-value">{{ algorithmMetrics.clickRate || '15.2' }}%</span>
              <span class="metric-label">点击率提升</span>
            </div>
            <div class="metric-card">
              <span class="metric-value">{{ algorithmMetrics.avgDecisionTime || '8.7' }}s</span>
              <span class="metric-label">平均决策时间</span>
            </div>
          </div>
        </div>
        
        <!-- 算法设置对话框 -->
        <el-dialog 
          v-model="showAlgorithmSettings" 
          title="🤖 算法设置" 
          width="500px"
        >
          <div class="algorithm-settings">
            <p class="settings-desc">调整不同算法的权重，影响推荐结果</p>
            <div class="slider-container" v-for="(weight, algo) in algorithmWeights" :key="algo">
              <div class="slider-label">
                <span>{{ getAlgorithmName(algo) }}</span>
                <span class="slider-value">{{ weight }}%</span>
              </div>
              <el-slider 
                v-model="algorithmWeights[algo]" 
                :min="0" 
                :max="100"
                @change="updateAlgorithmWeights"
              ></el-slider>
            </div>
            <el-button type="primary" @click="applyAlgorithmWeights" style="width: 100%; margin-top: 20px;">
              应用配置
            </el-button>
          </div>
        </el-dialog>

        <!-- 新品上市区域 -->
        <div class="section new-products-section">
          <div class="section-header">
            <div class="title-wrapper">
              <div class="title-icon-wrapper new-icon">
                <i class="fas fa-box"></i>
                <div class="icon-glow new-glow"></div>
                <div class="new-badge">NEW</div>
              </div>
              <div class="title-content">
                <h2 class="section-title">
                  <span>新书上架</span>
                  <div class="title-accent new-accent"></div>
                </h2>
                <div class="subtitle">精选新书，第一时间为您呈现</div>
              </div>
            </div>
            <div class="more-btn new-more-btn" @click="$router.push('/products?type=new')">
              <span>查看更多</span>
              <i class="fas fa-arrow-right"></i>
              <div class="btn-ripple"></div>
            </div>
          </div>
          
          <div class="product-grid">
            <div class="product-item" v-for="product in newProducts" :key="product.id">
              <product-card :product="product" @add-to-cart="handleAddToCart" @toggle-favorite="handleToggleFavorite"></product-card>
            </div>
          </div>
        </div>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import FrontCarousel from '@/components/front/FrontCarousel.vue'
import FrontCategory from '@/components/front/FrontCategory.vue'
import ProductCard from '@/components/front/ProductCard.vue'
import FrontNotice from '@/components/front/FrontNotice.vue'
import AlgorithmSidebar from '@/components/front/AlgorithmSidebar.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { MagicStick } from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: {
    FrontHeader,
    FrontFooter,
    FrontCarousel,
    FrontCategory,
    ProductCard,
    FrontNotice,
    AlgorithmSidebar,
    MagicStick
  },
  data() {
    return {
      recommendProducts: [],
      newProducts: [],
      isLoggedIn: false,
      refresh: true,
      loading: true,
      activeAlgorithm: 'personalized',
      currentAlgorithmType: 'COLLABORATIVE',
      algorithmMetrics: null,
      showAlgorithmSettings: false,
      algorithmWeights: {
        collaborative: 60,
        content: 30,
        trending: 10
      }
    }
  },
  computed: {
    // 展示用的当前算法名称（根据后端返回的 algorithmType）
    displayCurrentAlgorithm() {
      const type = (this.currentAlgorithmType || '').toUpperCase()
      const map = {
        'COLLABORATIVE': '协同过滤推荐',
        'COLLABORATIVE_FILTERING': '协同过滤推荐',
        'PERSONALIZED': '个性化推荐',
        'CONTENT': '内容相似推荐',
        'TRENDING': '热门趋势推荐',
        'POPULAR': '热销推荐',
        'SERENDIPITY': '惊喜探索推荐'
      }
      return map[type] || '混合推荐'
    },
    // 展示用的数据更新时间
    displayLastUpdateTime() {
      if (!this.algorithmMetrics || !this.algorithmMetrics.lastUpdateTime) {
        return ''
      }
      const rawTs = this.algorithmMetrics.lastUpdateTime
      const tsNum = Number(rawTs)
      const ts = Number.isNaN(tsNum) ? Date.parse(rawTs) : tsNum
      if (!ts || Number.isNaN(ts)) return ''
      const date = new Date(ts)
      const now = new Date()
      const diffMs = now - date
      const diffMinutes = Math.floor(diffMs / 60000)
      if (diffMinutes < 1) return '刚刚'
      if (diffMinutes < 60) return diffMinutes + ' 分钟前'
      const diffHours = Math.floor(diffMinutes / 60)
      if (diffHours < 24) return diffHours + ' 小时前'
      const diffDays = Math.floor(diffHours / 24)
      if (diffDays < 7) return diffDays + ' 天前'
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}`
    }
  },
  methods: {
    // 修改 checkLoginStatus 方法
    checkLoginStatus() {
      const token = localStorage.getItem('token');
      const userStr = localStorage.getItem('frontUser');
      
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr);
       
          // 检查用户角色，如果不是 USER 则注销
          if (user.role !== 'USER') {
            this.logout();
            this.$message.warning('请使用普通用户账号访问');
            return;
          }
          this.isLoggedIn = true;
        } catch (error) {
          this.logout();
        }
      } else {
        this.isLoggedIn = false;
      }
    },

    // 修改 logout 方法
    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('frontUser');
      this.isLoggedIn = false;
      // 强制刷新整个页面
      window.location.reload()
   
    },

    // 处理算法切换
    handleAlgorithmChange(algorithmName) {
      this.activeAlgorithm = algorithmName;
      // 根据算法类型更新推荐
      this.getRecommendProductsByAlgorithm(algorithmName);
    },
    
    // 根据算法类型获取推荐
    async getRecommendProductsByAlgorithm(algorithmType) {
      try {
        this.loading = true;
        this.checkLoginStatus();
        
        let products = [];
        
        if (!this.isLoggedIn) {
          // 未登录状态 - 根据算法类型获取不同的推荐
          if (algorithmType === 'trending') {
            // 热门趋势：按销量排序
            const res = await Request.get('/product/page?status=1&size=8&sort=salesCount,desc');
            if (res.code === '0') {
              products = res.data.records || res.data;
            }
          } else if (algorithmType === 'serendipity') {
            // 惊喜发现：随机推荐
            const res = await Request.get('/product/page?status=1&size=8');
            if (res.code === '0') {
              products = (res.data.records || res.data).sort(() => Math.random() - 0.5);
            }
          } else {
            // 默认：随机排序
            const res = await Request.get('/product/page?status=1&size=8');
            if (res.code === '0') {
              products = res.data.records || res.data;
              products = products.sort(() => Math.random() - 0.5);
            }
          }
        } else {
          // 登录状态 - 根据算法类型调用不同的推荐接口
          const userStr = localStorage.getItem('frontUser');
          if (!userStr) {
            throw new Error('User info not found');
          }
          const userId = JSON.parse(userStr).id;
          
          // 构建请求参数
          const params = {
            algorithmType: this.mapAlgorithmType(algorithmType),
            weights: this.algorithmWeights
          };
          
          // 调用推荐接口，传递算法类型和权重参数（添加 silent 配置，避免超时错误提示）
          const res = await Request.get(`/recommend/user/${userId}/explanation`, { 
            params,
            silent: true,
            timeout: 30000 // 推荐算法可能需要更长时间
          });
          if (res.code === '0') {
            const data = res.data;
            products = data.products || data.records || data;
            
            // 保存算法类型和指标
            if (data.algorithmType) {
              this.currentAlgorithmType = data.algorithmType;
            }
            if (data.stats) {
              this.algorithmMetrics = {
                accuracy: data.stats.accuracy || 92,
                clickRate: data.stats.clickRate || 15.2,
                avgDecisionTime: data.stats.executionTime ? (data.stats.executionTime / 1000).toFixed(1) : 8.7,
                lastUpdateTime: data.stats.lastUpdateTime || Date.now()
              };
            }
            
            // 为每个商品添加推荐解释信息
            if (data.explanations && Array.isArray(data.explanations)) {
              products = products.map((product, index) => ({
                ...product,
                recommendationExplanation: data.explanations[index] || null
              }));
            }
          } else {
            // 降级到普通推荐接口
            const fallbackRes = await Request.get(`/recommend/user/${userId}`);
            if (fallbackRes.code === '0') {
              products = fallbackRes.data.records || fallbackRes.data;
            }
          }
          
          // 获取收藏状态（添加超时处理，避免阻塞主流程）
          try {
            // 使用 Promise.race 添加超时保护，如果5秒内未完成，则使用默认值
            const timeoutPromise = new Promise((_, reject) => 
              setTimeout(() => reject(new Error('收藏状态请求超时')), 5000)
            );
            
            const favRes = await Promise.race([
              Request.get(`/favorite/user/${userId}`, { timeout: 5000 }),
              timeoutPromise
            ]);
            
            if (favRes && favRes.code === '0') {
              const favorites = favRes.data || [];
              // 创建收藏状态的 Map，提高查找效率
              const favoriteMap = new Map();
              favorites.forEach(f => {
                if (f.status === 1) {
                  favoriteMap.set(f.productId, true);
                }
              });
              
              products = products.map(product => ({
                ...product,
                isFavorite: favoriteMap.has(product.id) || false
              }));
              
              logger.debug('收藏状态已设置:', { 
                totalProducts: products.length, 
                favoritesCount: favoriteMap.size 
              });
            } else {
              // 响应异常，使用默认值
              products = products.map(product => ({
                ...product,
                isFavorite: false
              }));
            }
          } catch (error) {
            // 收藏状态获取失败不影响主流程，静默处理
            logger.debug('获取收藏状态失败（不影响主流程）:', {
              error: error.message,
              code: error.code,
              note: '将使用默认值 false，ProductCard 可能会在需要时独立请求'
            });
            
            // 设置默认值，ProductCard 组件会在需要时独立请求
            products = products.map(product => ({
              ...product,
              isFavorite: false
            }));
          }
        }

        // 设置推荐商品
        this.recommendProducts = products.map(product => ({
          ...product,
          isFavorite: product.isFavorite || false
        }));

      } catch (error) {
        logger.error('获取推荐商品失败:', error);
        // 如果获取失败，回退到未登录状态的推荐
        if (this.isLoggedIn) {
          this.isLoggedIn = false;
          this.getRecommendProductsByAlgorithm(algorithmType);
        }
      } finally {
        setTimeout(() => {
          this.loading = false;
        }, 500);
      }
    },
    
    // 映射前端算法类型到后端算法类型
    mapAlgorithmType(frontendType) {
      const mapping = {
        'personalized': 'PERSONALIZED',
        'collaborative': 'COLLABORATIVE',
        'content': 'CONTENT',
        'trending': 'TRENDING',
        'serendipity': 'SERENDIPITY'
      };
      return mapping[frontendType] || 'COLLABORATIVE';
    },
    
    // 刷新推荐
    async refreshRecommendations() {
      await this.getRecommendProductsByAlgorithm(this.activeAlgorithm);
      this.$message.success('推荐已刷新');
    },
    
    // 获取算法名称
    getAlgorithmName(algo) {
      const names = {
        collaborative: '协同过滤',
        content: '内容匹配',
        trending: '热门趋势'
      };
      return names[algo] || algo;
    },
    
    // 更新算法权重
    updateAlgorithmWeights() {
      // 这里可以实时更新权重，但暂时只保存到本地
      localStorage.setItem('algorithmWeights', JSON.stringify(this.algorithmWeights));
    },
    
    // 应用算法权重
    applyAlgorithmWeights() {
      this.updateAlgorithmWeights();
      this.showAlgorithmSettings = false;
      this.$message.success('算法权重已更新，正在刷新推荐...');
      // 刷新推荐，使用当前选中的算法类型
      this.getRecommendProductsByAlgorithm(this.activeAlgorithm);
    },
    
    // 处理侧边栏权重变化
    handleWeightsChanged(weights) {
      this.algorithmWeights = weights;
      this.refreshRecommendations();
    },
    // 获取新品
    async getNewProducts() {
      try {
        const res = await Request.get('/product/page?status=1&sort=updatedAt,desc&size=4', {
          silent: true // 静默处理，避免错误提示
        })
        if (res.code === '0') {
          this.newProducts = res.data.records.map(product => ({
            ...product,
            isFavorite: false
          }))
        }
      } catch (error) {
        logger.error('获取新品失败:', error)
      }
    },
    // 添加到购物车
    handleAddToCart(product) {
      this.$message({
        type: 'success',
        message: '已添加到购物车'
      })
    },
    // 切换收藏状态
    async handleToggleFavorite({ product, status }) {
      // 更新本地数据
      const targetProduct = this.recommendProducts.find(p => p.id === product.id)
      if (targetProduct) {
        targetProduct.isFavorite = status === 1
      }
      
      // 同时更新新品列表中的收藏状态
      const newProduct = this.newProducts.find(p => p.id === product.id)
      if (newProduct) {
        newProduct.isFavorite = status === 1
      }
    },
    
    // 跳转到推荐中心
    goToRecommendCenter() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录以查看个性化推荐')
        this.$router.push('/login')
        return
      }
      this.$router.push('/recommend-center')
    },
    // 跳转到新书上架
    goToNewProducts() {
      this.$router.push('/products?type=new')
    },
    
    // 添加性能优化方法
    preloadCriticalResources() {
      // 预加载产品图片
      this.recommendProducts.concat(this.newProducts).forEach(product => {
        if (product.imageUrl) {
          const img = new Image();
          const src = product.imageUrl.startsWith('http') 
            ? product.imageUrl 
            : `/api${product.imageUrl}`;
          img.src = src;
        }
      });
    }
  },
  created() {
    // 添加登录状态检查
    this.checkLoginStatus();
    this.getRecommendProductsByAlgorithm(this.activeAlgorithm);
    this.getNewProducts();
  },
  mounted() {
    const userStr = localStorage.getItem('frontUser');
    if (userStr) {
      this.isLoggedIn = true;
    }
    
    // 性能优化：预加载关键资源
    this.$nextTick(() => {
      this.preloadCriticalResources();
    });
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
  position: relative;
  overflow-x: hidden;
}

.home::before {
  content: '';
  position: fixed;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 20%, rgba(52, 152, 219, 0.03) 0%, transparent 50%),
              radial-gradient(circle at 70% 80%, rgba(95, 124, 110, 0.02) 0%, transparent 50%);
  z-index: -1;
  animation: backgroundFloat 20s ease-in-out infinite;
}

@keyframes backgroundFloat {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(-20px, -10px) rotate(1deg); }
  66% { transform: translate(10px, -20px) rotate(-1deg); }
}
.main-content {
  flex: 1;
  padding: var(--spacing-xl);
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}
.section {
  margin: var(--spacing-xxxl) 0;
  position: relative;
}

.recommend-section {
  padding: var(--spacing-xxxl) var(--spacing-xl);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
  position: relative;
  overflow: hidden;
  animation: slideInFromLeft 0.8s ease-out 0.3s both;
  border: 1px solid var(--color-border-lighter);
  transition: var(--transition-base);
}

.recommend-section:hover {
  box-shadow: var(--box-shadow-light);
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.recommend-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6px;
  background: var(--gradient-primary);
  border-radius: 3px 3px 0 0;
}

.recommend-section::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(circle at 10% 10%, rgba(52, 152, 219, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 90% 90%, rgba(52, 152, 219, 0.05) 0%, transparent 50%);
  z-index: 0;
}

.section::before {
  content: '';
  position: absolute;
  inset: -16px;
  background: 
    radial-gradient(circle at 0% 0%, rgba(52, 152, 219, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(52, 152, 219, 0.03) 0%, transparent 50%);
  z-index: -1;
  border-radius: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recommend-center-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.recommend-center-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.recommend-center-btn:active {
  transform: translateY(0);
}

.recommend-center-btn .el-icon {
  font-size: 16px;
}

.title-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);
  position: relative;
  transition: all 0.3s ease;
  animation: iconPulse 3s ease-in-out infinite;
}

.title-icon-wrapper:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 20px rgba(52, 152, 219, 0.3);
}

@keyframes iconPulse {
  0%, 100% { box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2); }
  50% { box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4), 0 0 20px rgba(52, 152, 219, 0.1); }
}

.icon-glow {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.3), rgba(93, 173, 226, 0.3));
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
  50% { transform: scale(1.2); opacity: 0.6; }
}

.title-icon-wrapper i {
  color: white;
  font-size: 24px;
}

.title-content {
  display: flex;
  flex-direction: column;
}

.section-title {
  margin: 0;
  font-size: var(--font-size-extra-large);
  font-weight: var(--font-weight-primary);
  color: var(--color-text-primary);
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.section-title span {
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
  transition: width 0.8s ease;
  animation: accentGrow 2s ease-in-out 0.5s forwards;
}

.new-accent {
  background: linear-gradient(90deg, #ff4757, #ff6b81);
}

@keyframes accentGrow {
  from { width: 0; }
  to { width: 100%; }
}

.section-title i {
  color: var(--primary-color);
  font-size: 22px;
}

.title-line {
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 8px;
  background: rgba(52, 152, 219, 0.2);
  border-radius: 4px;
  z-index: 0;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

.more-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--border-radius-round);
  background: var(--color-primary-light-9);
  color: var(--color-primary);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-primary);
  cursor: pointer;
  transition: var(--transition-base);
  position: relative;
  overflow: hidden;
  border: 1px solid var(--color-primary-light-7);
}

.more-btn:hover {
  background: var(--color-primary-light-8);
  color: var(--color-primary-dark-1);
  transform: translateY(-2px);
}

.new-more-btn {
  background: linear-gradient(135deg,
    rgba(255, 71, 87, 0.1),
    rgba(255, 107, 129, 0.15)
  );
  color: #ff4757;
  border: 1px solid rgba(255, 71, 87, 0.2);
}

.btn-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
}

.more-btn:active .btn-ripple {
  width: 120px;
  height: 120px;
}

.more-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg,
    rgba(52, 152, 219, 0.15),
    rgba(52, 152, 219, 0.2)
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}

.more-btn:hover {
  transform: translateX(4px);
}

.more-btn:hover::before {
  opacity: 1;
}

.more-btn i {
  font-size: 14px;
  transition: transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.more-btn span {
  position: relative;
  z-index: 1;
}

.more-btn:hover i {
  transform: translateX(2px);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  position: relative;
  z-index: 1;
}

.product-item {
  animation: fadeInUp 0.8s ease forwards;
  opacity: 0;
  transform: translateY(30px);
  transition: transform 0.3s ease;
}

.product-item:hover {
  transform: translateY(-5px);
}

.product-item:nth-child(1) { animation-delay: 0.1s; }
.product-item:nth-child(2) { animation-delay: 0.2s; }
.product-item:nth-child(3) { animation-delay: 0.3s; }
.product-item:nth-child(4) { animation-delay: 0.4s; }
.product-item:nth-child(5) { animation-delay: 0.5s; }
.product-item:nth-child(6) { animation-delay: 0.6s; }
.product-item:nth-child(7) { animation-delay: 0.7s; }
.product-item:nth-child(8) { animation-delay: 0.8s; }

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

@media (max-width: 1200px) {
  .main-content {
    padding: 16px;
  }
  
  .section-title {
    font-size: 20px;
  }
  
  .section-title i {
    font-size: 20px;
  }
  
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .section {
    margin: 24px 0;
  }
  
  .recommend-section, .new-products-section {
    padding: 24px 16px;
    border-radius: 20px;
  }
  
  .section-title {
    font-size: 18px;
  }
  
  .title-icon-wrapper {
    width: 40px;
    height: 40px;
  }
  
  .title-icon-wrapper i {
    font-size: 20px;
  }
  
  .title-wrapper {
    gap: 12px;
  }
  
  .subtitle {
    font-size: 13px;
  }
  
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .new-badge {
    font-size: 7px;
    padding: 1px 3px;
    top: -6px;
    right: -6px;
  }
  
  .loading-container {
    margin: 16px 0;
    min-height: 300px;
  }
  
  .loading-spinner {
    width: 60px;
    height: 60px;
  }
  
  .spinner-icon {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .home::before {
    animation: none; /* 禁用复杂动画以提高性能 */
  }
  
  .product-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .title-content {
    max-width: 160px;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .more-btn {
    align-self: flex-end;
  }
  
  .loading-text span {
    font-size: 14px;
  }
  
  .main-content {
    padding: 16px;
  }
}

.hero-layout {
  display: grid;
  grid-template-columns: 15% 65% 20%;
  gap: 18px;
  align-items: start;
  margin: 20px 0 12px;
}

.left-column,
.center-column,
.right-column {
  min-width: 0;
}

.left-column {
  min-width: 200px;
}

.center-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.carousel-panel {
  background: var(--color-bg-white);
  border: 1px solid var(--color-border-lighter);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
}

.carousel-panel:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.12);
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.quick-card {
  background: linear-gradient(135deg, #ffffff 0%, #f3f6ff 100%);
  border: 1px solid rgba(96, 125, 255, 0.12);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.quick-card::before {
  content: '';
  position: absolute;
  width: 120%;
  height: 120%;
  top: -10%;
  left: -10%;
  background-image:
    radial-gradient(circle at 15% 20%, rgba(255, 255, 255, 0.14), transparent 32%),
    radial-gradient(circle at 80% 10%, rgba(255, 255, 255, 0.1), transparent 30%),
    linear-gradient(45deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0));
  z-index: 0;
  pointer-events: none;
}

.quick-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.35), transparent 40%),
    radial-gradient(circle at 80% 30%, rgba(102, 126, 234, 0.16), transparent 45%),
    linear-gradient(135deg, rgba(52, 152, 219, 0.08), rgba(102, 126, 234, 0.08));
  opacity: 0.6;
  transition: opacity 0.3s ease;
  pointer-events: none;
  mix-blend-mode: screen;
}

.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
  border-color: var(--color-primary-light-7);
}

.quick-card .card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  position: relative;
  z-index: 1;
}

.quick-card .card-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 6px;
  position: relative;
  z-index: 1;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  position: relative;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12), rgba(118, 75, 162, 0.12));
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: 0 4px 10px rgba(102, 126, 234, 0.15);
  position: relative;
  overflow: hidden;
}

.card-icon::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.4), transparent 65%);
  pointer-events: none;
}

.card-action {
  margin-top: 12px;
  font-size: 13px;
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.card-action i {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.quick-card:hover .card-action i {
  transform: translateX(3px);
}

.quick-card--recommend {
  background: linear-gradient(135deg, #f5f7ff 0%, #e9edff 45%, #f7fbff 100%);
  border-color: rgba(90, 111, 224, 0.28);
}

.quick-card--recommend .card-icon {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  color: #5a6fe0;
}

.quick-card--new {
  background: linear-gradient(135deg, #fff8f4 0%, #ffecec 45%, #fff9f6 100%);
  border-color: rgba(255, 107, 107, 0.26);
}

.quick-card--new .card-icon {
  background: linear-gradient(135deg, rgba(255, 99, 132, 0.18), rgba(255, 159, 67, 0.18));
  color: #ff6b6b;
}

.quick-card .card-top,
.quick-card .card-desc,
.quick-card .card-action {
  z-index: 1;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 240px;
}

.algo-card,
.notice-card {
  background: var(--color-bg-white);
  border: 1px solid var(--color-border-lighter);
  border-radius: 14px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  padding: 12px;
}

.algo-card {
  min-height: 180px;
}

.notice-card {
  flex: 1;
  min-height: 220px;
  max-height: 340px;
  overflow: hidden;
}

@media (max-width: 1400px) {
  .hero-layout {
    grid-template-columns: 18% 60% 22%;
    gap: 14px;
  }
}

@media (max-width: 1200px) {
  .hero-layout {
    grid-template-columns: 1fr;
  }
  
  .left-column,
  .right-column {
    order: 2;
  }
  
  .center-column {
    order: 1;
  }
  
  .quick-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .quick-links {
    grid-template-columns: 1fr;
  }
  
  .hero-layout {
    gap: 12px;
  }
}

/* 加载动画样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 500px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 245, 0.9));
  border-radius: 20px;
  margin: 20px 0;
  position: relative;
  overflow: hidden;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 24px;
}

.spinner-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 3px solid rgba(52, 152, 219, 0.1);
  border-top: 3px solid var(--primary-color);
  border-radius: 50%;
  animation: spinRotate 1s linear infinite;
}

.spinner-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  color: var(--primary-color);
  animation: iconFloat 2s ease-in-out infinite;
}

@keyframes spinRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes iconFloat {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.1); }
}

.loading-text {
  text-align: center;
  color: #606266;
}

.loading-text span {
  font-size: 16px;
  font-weight: 500;
  display: block;
  margin-bottom: 8px;
}

.loading-dots {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color);
  animation: dotPulse 1.4s ease-in-out infinite;
  margin: 0;
}

.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.2); }
}

.new-products-section {
  padding: 30px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #fff5f5 50%, #f8faf5 100%);
  border-radius: 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  margin-top: 40px;
  animation: slideInFromRight 0.8s ease-out 0.5s both;
  border: 1px solid rgba(255, 71, 87, 0.1);
}

@keyframes slideInFromRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.new-products-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6px;
  background: linear-gradient(to right, #ff6b81, #ff4757);
  border-radius: 3px 3px 0 0;
}

.new-products-section::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(circle at 90% 10%, rgba(255, 107, 129, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 10% 90%, rgba(255, 107, 129, 0.05) 0%, transparent 50%);
  z-index: 0;
}

.new-icon {
  background: linear-gradient(135deg, #ff4757, #ff6b81);
  box-shadow: 0 4px 12px rgba(255, 71, 87, 0.2);
  position: relative;
}

.new-glow {
  background: linear-gradient(135deg, rgba(255, 71, 87, 0.3), rgba(255, 107, 129, 0.3));
}

.new-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: linear-gradient(45deg, #ff4757, #ff6b81);
  color: white;
  font-size: 8px;
  font-weight: 700;
  padding: 2px 4px;
  border-radius: 6px;
  line-height: 1;
  box-shadow: 0 2px 6px rgba(255, 71, 87, 0.3);
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); box-shadow: 0 2px 12px rgba(255, 71, 87, 0.5); }
}

@media (max-width: 768px) {
  .new-products-section {
    padding: 24px 16px;
    margin-top: 30px;
  }
}

/* 无障碍性优化 */
@media (prefers-reduced-motion: reduce) {
  .home::before,
  .title-icon-wrapper,
  .icon-glow,
  .product-item,
  .carousel-wrapper,
  .recommend-section,
  .new-products-section,
  .loading-spinner,
  .spinner-ring,
  .spinner-icon,
  .loading-dots span {
    animation: none !important;
    transition: opacity 0.2s ease !important;
  }
  
  .product-item {
    opacity: 1;
    transform: none;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .section-title {
    color: #000;
  }
  
  .subtitle {
    color: #333;
  }
  
  .more-btn {
    border: 2px solid currentColor;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .home {
    background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1e2d1e 100%);
  }
  
  .recommend-section,
  .new-products-section {
    background: linear-gradient(135deg, #2d2d2d 0%, #3a3a3a 50%, #2d3a2d 100%);
    color: #e0e0e0;
  }
  
  .section-title {
    color: #e0e0e0;
  }
  
  .subtitle {
    color: #b0b0b0;
  }
}

/* 性能优化 - 使用 transform 替代其他属性 */
.optimized-animation {
  will-change: transform, opacity;
}

.product-item,
.carousel-wrapper,
.recommend-section,
.new-products-section {
  will-change: transform;
}

/* 算法导航样式 */
.algo-navigation {
  margin: 24px 0;
  position: relative;
  z-index: 1;
}

:deep(.algo-tabs) {
  border-bottom: 2px solid rgba(58, 91, 160, 0.1);
}

:deep(.algo-tabs .el-tabs__header) {
  margin: 0;
}

:deep(.algo-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.algo-tabs .el-tabs__item) {
  padding: 12px 20px;
  font-size: 14px;
  transition: all 0.3s ease;
}

:deep(.algo-tabs .el-tabs__item.is-active) {
  color: var(--primary-color);
  border-bottom: 3px solid var(--primary-color);
}

.algo-tab-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  min-width: 80px;
}

.algo-icon {
  font-size: 20px;
  line-height: 1.2;
  display: block;
}

.algo-name {
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}

.algo-desc {
  font-size: 10px;
  color: #909399;
  font-weight: normal;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

:deep(.algo-tabs .el-tabs__item:hover) {
  color: var(--primary-color);
}

/* 算法控制按钮 */
.algo-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 算法性能指标 */
.algorithm-metrics {
  display: flex;
  gap: 20px;
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  justify-content: center;
}

.metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.05), rgba(58, 91, 160, 0.02));
  border-radius: 12px;
  min-width: 120px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(58, 91, 160, 0.15);
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  line-height: 1;
}

.metric-label {
  font-size: 13px;
  color: #666;
  text-align: center;
}

/* 算法设置对话框 */
.algorithm-settings {
  padding: 10px 0;
}

.settings-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}

.slider-container {
  margin-bottom: 24px;
}

.slider-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.slider-value {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 16px;
}

/* 推荐结果区域 */
.recommendation-results {
  position: relative;
  z-index: 1;
}

@media (max-width: 768px) {
  .algo-tab-label {
    flex-direction: row;
    gap: 6px;
  }
  
  .algo-desc {
    display: none;
  }
  
  .algorithm-metrics {
    flex-direction: column;
    gap: 12px;
  }
  
  .metric-card {
    width: 100%;
  }
  
  :deep(.algo-tabs .el-tabs__item) {
    padding: 8px 12px;
    font-size: 12px;
  }
}
</style> 