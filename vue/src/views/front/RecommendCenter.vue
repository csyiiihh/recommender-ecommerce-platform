<template>
  <div class="recommend-center-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题和统计信息 -->
      <div class="page-header">
        <div class="header-left">
          <h2>
            <el-icon><MagicStick /></el-icon>
            智能推荐中心
          </h2>
          <p class="subtitle">基于协同过滤算法的个性化商品推荐</p>
        </div>
        <div class="header-right">
          <el-button 
            type="primary" 
                    :icon="iconRefresh" 
            @click="refreshRecommendations"
            :loading="loading"
          >
            刷新推荐
          </el-button>
        </div>
      </div>

      <!-- 推荐统计卡片 + 反馈历史 -->
      <div class="stats-and-history" v-if="recommendationData && recommendationData.stats">
        <el-row :gutter="20">
          <el-col :span="16">
            <div class="stats-cards">
              <el-row :gutter="20">
                <el-col :span="6">
                  <el-card class="stat-card">
                    <div class="stat-content">
                      <div class="stat-value">{{ recommendationData.stats.similarUserCount || 0 }}</div>
                      <div class="stat-label">相似用户数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stat-card">
                    <div class="stat-content">
                      <div class="stat-value">{{ recommendationData.stats.userBehaviorCount || 0 }}</div>
                      <div class="stat-label">历史行为数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stat-card">
                    <div class="stat-content">
                      <div class="stat-value">{{ recommendationData.stats.executionTime || 0 }}ms</div>
                      <div class="stat-label">算法执行时间</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stat-card">
                    <div class="stat-content">
                      <div class="stat-value">
                        <el-tag 
                          :type="recommendationData.stats.fallbackUsed ? 'warning' : 'success'"
                          class="algorithm-type-tag"
                        >
                          {{ recommendationData.algorithmType === 'POPULAR' ? '热门推荐' : '协同过滤' }}
                        </el-tag>
                      </div>
                      <div class="stat-label">推荐算法类型</div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </el-col>
          <el-col :span="8">
            <el-card class="feedback-history-card" v-loading="feedbackLoading">
              <div class="feedback-history-header">
                <span class="title">📝 我的反馈记录</span>
                <el-button link type="primary" size="small" @click="loadFeedbackHistory">刷新</el-button>
              </div>
              <div v-if="!feedbackHistory.length" class="feedback-empty">暂无反馈记录</div>
              <div v-else class="feedback-history-list">
                <div v-for="item in feedbackHistory" :key="item.id" class="feedback-item">
                  <div class="feedback-main">
                    <span class="feedback-product">商品ID: {{ item.productId }}</span>
                    <span class="feedback-type">{{ formatFeedbackType(item.feedbackType) }}</span>
                  </div>
                  <div class="feedback-meta">
                    <span class="feedback-time">{{ formatTime(item.feedbackTime) }}</span>
                    <el-button link type="primary" size="small" @click="undoFeedback(item.id)">撤销</el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 推荐商品列表 -->
      <div class="recommendations-content" v-loading="loading">
        <!-- 有推荐数据时展示列表 -->
        <div
          v-if="!loading && recommendationData && recommendationData.products && recommendationData.products.length > 0"
          class="recommendations-list"
        >
          <div 
            v-for="(product, index) in recommendationData.products" 
            :key="product.id"
            class="recommendation-item"
          >
            <el-card class="product-card">
              <div class="product-content">
                <!-- 商品图片 -->
                <div class="product-image">
                  <img 
                    :src="getProductImage(product)" 
                    :alt="product.name"
                    @click="goToProductDetail(product.id)"
                  />
                  <div class="recommend-badge">
                    <el-tag type="success" size="small">推荐</el-tag>
                  </div>
                </div>

                <!-- 商品信息 -->
                <div class="product-info">
                  <h3 class="product-name" @click="goToProductDetail(product.id)">
                    {{ product.name }}
                  </h3>
                  <div class="product-price">
                    <span class="price">¥{{ product.price }}</span>
                    <span v-if="product.isDiscount" class="discount-price">
                      ¥{{ product.discountPrice }}
                    </span>
                  </div>
                  
                  <!-- 推荐解释 -->
                  <div class="recommendation-explanation" v-if="recommendationData.explanations && recommendationData.explanations[index]">
                    <el-collapse>
                      <el-collapse-item>
                        <template #title>
                          <div class="explanation-header">
                            <el-icon><InfoFilled /></el-icon>
                            <span>为什么推荐这个商品？</span>
                            <el-tag 
                              :type="getReasonTypeTag(recommendationData.explanations[index].reasonType)"
                              size="small"
                              style="margin-left: 10px;"
                            >
                              {{ getReasonTypeText(recommendationData.explanations[index].reasonType) }}
                            </el-tag>
                          </div>
                        </template>
                        <div class="explanation-content">
                          <!-- 降级为热门推荐原因说明 -->
                          <div
                            class="explanation-item"
                            v-if="recommendationData && recommendationData.algorithmType === 'POPULAR' && recommendationData.stats && recommendationData.stats.fallbackReason"
                          >
                            <strong>当前仍基于热门推荐的原因：</strong>
                            <p>{{ recommendationData.stats.fallbackReason }}</p>
                          </div>
                          <!-- 算法得分雷达图 -->
                          <div class="explanation-item" v-if="hasAlgorithmScores(recommendationData.explanations[index])">
                            <strong>算法得分分析：</strong>
                            <div class="radar-wrapper" :ref="el => setRadarRef(product.id, el)"></div>
                          </div>

                          <div class="explanation-item">
                            <strong>推荐理由：</strong>
                            <p>{{ recommendationData.explanations[index].reasonDescription }}</p>
                          </div>
                          
                          <div class="explanation-item" v-if="recommendationData.explanations[index].similarUserCount > 0">
                            <strong>相似用户：</strong>
                            <p>找到 {{ recommendationData.explanations[index].similarUserCount }} 位与您兴趣相似的用户</p>
                            <div class="similar-users" v-if="recommendationData.explanations[index].similarUserHints && recommendationData.explanations[index].similarUserHints.length > 0">
                              <el-tag 
                                v-for="(hint, idx) in recommendationData.explanations[index].similarUserHints" 
                                :key="idx"
                                size="small"
                                style="margin-right: 8px;"
                              >
                                {{ hint }}
                              </el-tag>
                            </div>
                          </div>
                          
                          <div class="explanation-item">
                            <strong>推荐分数：</strong>
                            <el-progress 
                              :percentage="Math.min(100, (recommendationData.explanations[index].score / 10) * 100)"
                              :format="() => recommendationData.explanations[index].score.toFixed(2)"
                            />
                          </div>
                          
                          <div class="explanation-item">
                            <strong>算法置信度：</strong>
                            <el-progress 
                              :percentage="Math.round(recommendationData.explanations[index].confidence * 100)"
                              :color="getConfidenceColor(recommendationData.explanations[index].confidence)"
                            />
                          </div>
                          
                          <div class="explanation-item" v-if="recommendationData.explanations[index].sourceProductIds && recommendationData.explanations[index].sourceProductIds.length > 0">
                            <strong>基于您的偏好：</strong>
                            <p>此推荐基于您对相关商品的购买/收藏行为</p>
                          </div>

                          <!-- 支撑因素时间线 -->
                          <div class="explanation-item" v-if="recommendationData.explanations[index].supportingFactors && recommendationData.explanations[index].supportingFactors.length">
                            <strong>支撑因素：</strong>
                            <div class="timeline">
                              <div class="timeline-item" v-for="(factor, fi) in recommendationData.explanations[index].supportingFactors" :key="fi">
                                <div class="timeline-dot"></div>
                                <div class="timeline-content">
                                  <span class="factor-type">{{ factor.type }}</span>
                                  <span class="factor-desc">{{ factor.description }}</span>
                                  <span class="factor-score" v-if="factor.score !== undefined">+{{ factor.score }}分</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>

                  <!-- 操作按钮 -->
                  <div class="product-actions">
                    <el-button 
                      type="primary" 
                      size="small"
                      @click="goToProductDetail(product.id)"
                    >
                      查看详情
                    </el-button>
                    <el-button 
                      type="success" 
                      size="small"
                    :icon="iconLike"
                      @click="handleFeedback(product.id, 'LIKE')"
                    >
                      喜欢
                    </el-button>
                    <el-dropdown trigger="click" @command="cmd => handleFeedback(product.id, cmd)">
                      <el-button size="small" type="info">
                        更多反馈
                        <el-icon class="el-icon--right"><i class="fas fa-angle-down"></i></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="ALREADY_OWN">已拥有</el-dropdown-item>
                          <el-dropdown-item command="NOT_INTERESTED">不感兴趣</el-dropdown-item>
                          <el-dropdown-item command="TOO_EXPENSIVE">太贵</el-dropdown-item>
                          <el-dropdown-item command="WILL_BUY_LATER">稍后购买</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <el-button 
                      type="warning" 
                      size="small"
                    :icon="iconDislike"
                      @click="handleFeedback(product.id, 'DISLIKE')"
                    >
                      不喜欢
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <!-- 无数据时展示空状态 -->
        <el-empty 
          v-else-if="!loading"
          description="暂无推荐商品"
        >
          <el-button type="primary" @click="refreshRecommendations">获取推荐</el-button>
        </el-empty>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, InfoFilled, Like, Dislike } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'RecommendCenter',
  components: {
    FrontHeader,
    FrontFooter,
    MagicStick,
    Refresh,
    InfoFilled,
    Like,
    Dislike
  },
  data() {
    const safeMarkRaw = (comp) => comp ? markRaw(comp) : comp
    return {
      loading: false,
      recommendationData: null,
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      // 图标用于按钮绑定（避免被响应式包装触发警告）
      iconRefresh: safeMarkRaw(Refresh),
      iconLike: safeMarkRaw(Like),
      iconDislike: safeMarkRaw(Dislike),
      radarRefs: {},
      radarCharts: {},
      feedbackOptions: [
        { value: 'LIKE', label: '👍 喜欢' },
        { value: 'DISLIKE', label: '👎 不喜欢' },
        { value: 'ALREADY_OWN', label: '📚 已拥有' },
        { value: 'NOT_INTERESTED', label: '🙅 不感兴趣' },
        { value: 'TOO_EXPENSIVE', label: '💰 太贵' },
        { value: 'WILL_BUY_LATER', label: '🕒 稍后购买' }
      ],
      feedbackHistory: [],
      feedbackLoading: false
    }
  },
  mounted() {
    this.checkLoginAndLoad()
  },
  watch: {
    recommendationData: {
      deep: true,
      handler() {
        this.$nextTick(() => {
          this.renderAllRadarCharts()
        })
      }
    }
  },
  methods: {
    setRadarRef(productId, el) {
      if (el) {
        this.radarRefs[productId] = el
      }
    },

    renderAllRadarCharts() {
      if (!this.recommendationData || !this.recommendationData.products) return
      this.recommendationData.products.forEach((product, index) => {
        const explanation = this.recommendationData.explanations
          ? this.recommendationData.explanations[index]
          : null
        this.renderRadarChart(product.id, explanation)
      })
    },

    renderRadarChart(productId, explanation) {
      if (!explanation || !explanation.algorithmScores) return
      const el = this.radarRefs[productId]
      if (!el) return
      if (this.radarCharts[productId]) {
        this.radarCharts[productId].dispose()
      }
      // 折叠面板未展开时容器宽高为 0，延后渲染以避免 ECharts 警告
      if (!el.clientWidth || !el.clientHeight) {
        setTimeout(() => this.renderRadarChart(productId, explanation), 120)
        return
      }
      const chart = echarts.init(el)
      const indicators = [
        { key: 'collaborativeFiltering', name: '协同过滤' },
        { key: 'contentBased', name: '内容匹配' },
        { key: 'trending', name: '热门趋势' },
        { key: 'portrait', name: '用户画像' }
      ]
      const normalize = (v) => {
        if (v == null) return 0
        const num = Number(v)
        if (Number.isNaN(num)) return 0
        // 将值映射到 0-100，简单压缩
        return Math.max(0, Math.min(100, Math.round(num * 10)))
      }
      const values = indicators.map(ind => normalize(explanation.algorithmScores[ind.key]))
      const option = {
        tooltip: {},
        radar: {
          indicator: indicators.map(i => ({ name: i.name, max: 100 })),
          radius: 70
        },
        series: [{
          type: 'radar',
          data: [{
            value: values,
            name: '算法得分',
            areaStyle: { color: 'rgba(64,158,255,0.2)' },
            lineStyle: { color: '#409eff' }
          }]
        }]
      }
      chart.setOption(option)
      this.radarCharts[productId] = chart
    },

    hasAlgorithmScores(explanation) {
      return explanation && explanation.algorithmScores
    },

    formatFeedbackType(type) {
      const map = {
        LIKE: '👍 喜欢',
        DISLIKE: '👎 不喜欢',
        ALREADY_OWN: '📚 已拥有',
        NOT_INTERESTED: '🙅 不感兴趣',
        TOO_EXPENSIVE: '💰 太贵',
        WILL_BUY_LATER: '🕒 稍后购买'
      }
      return map[type] || type
    },

    formatTime(time) {
      if (!time) return ''
      try {
        const d = new Date(time)
        const pad = (n) => (n < 10 ? '0' + n : n)
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
      } catch {
        return time
      }
    },

    checkLoginAndLoad() {
      if (!this.userInfo || !this.userInfo.id) {
        ElMessage.warning('请先登录以获取个性化推荐')
        this.$router.push('/login')
        return
      }
      this.loadRecommendations()
      this.loadFeedbackHistory()
    },
    
    async loadRecommendations() {
      if (!this.userInfo || !this.userInfo.id) {
        return
      }
      
      this.loading = true
      try {
        const res = await Request.get(`/recommend/user/${this.userInfo.id}/explanation`)
        if (res.code === '0') {
          this.recommendationData = res.data
          this.$nextTick(() => this.renderAllRadarCharts())
        } else {
          ElMessage.error(res.msg || '获取推荐失败')
        }
      } catch (error) {
        logger.error('获取推荐失败:', error)
        ElMessage.error('获取推荐失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    async refreshRecommendations() {
      await this.loadRecommendations()
      ElMessage.success('推荐已刷新')
    },
    
    async handleFeedback(productId, feedbackType) {
      if (!this.userInfo || !this.userInfo.id) {
        ElMessage.warning('请先登录')
        return
      }
      
      try {
        const res = await Request.post('/recommend/feedback', null, {
          params: {
            userId: this.userInfo.id,
            productId: productId,
            feedbackType: feedbackType
          }
        })
        
        if (res.code === '0') {
          const fbTextMap = {
            LIKE: '感谢您的喜欢！我们会根据您的喜好优化推荐',
            DISLIKE: '已记录不喜欢，下次会减少推荐',
            ALREADY_OWN: '已记录，后续不再推荐已拥有商品',
            NOT_INTERESTED: '已记录不感兴趣，我们会减少类似推荐',
            TOO_EXPENSIVE: '已记录太贵，我们会减少高价推荐',
            WILL_BUY_LATER: '已记录稍后购买，稍后可再尝试推荐'
          }
          ElMessage.success(fbTextMap[feedbackType] || '反馈已记录')
          // 更新推荐与反馈历史
          setTimeout(() => {
            this.refreshRecommendations()
            this.loadFeedbackHistory()
          }, 1000)
        } else {
          ElMessage.error(res.msg || '反馈失败')
        }
      } catch (error) {
        logger.error('反馈失败:', error)
        ElMessage.error('反馈失败，请稍后重试')
      }
    },

    async loadFeedbackHistory() {
      if (!this.userInfo || !this.userInfo.id) return
      this.feedbackLoading = true
      try {
        const res = await Request.get('/recommend/feedback/history', {
          params: { userId: this.userInfo.id }
        })
        if (res.code === '0') {
          this.feedbackHistory = res.data || []
        }
      } catch (e) {
        logger.error('获取反馈历史失败', e)
      } finally {
        this.feedbackLoading = false
      }
    },

    async undoFeedback(id) {
      if (!this.userInfo || !this.userInfo.id) return
      try {
        const res = await Request.delete(`/recommend/feedback/${id}`, {
          params: { userId: this.userInfo.id }
        })
        if (res.code === '0') {
          ElMessage.success('已撤销该反馈')
          this.loadFeedbackHistory()
          this.refreshRecommendations()
        } else {
          ElMessage.error(res.msg || '撤销失败')
        }
      } catch (e) {
        logger.error('撤销反馈失败', e)
        ElMessage.error('撤销失败，请稍后重试')
      }
    },
    
    goToProductDetail(productId) {
      this.$router.push(`/product/${productId}`)
    },
    
    getReasonTypeTag(type) {
      const map = {
        'COLLABORATIVE': 'success',
        'POPULAR': 'warning',
        'SIMILAR_USER': 'info'
      }
      return map[type] || 'info'
    },
    
    getReasonTypeText(type) {
      const map = {
        'COLLABORATIVE': '协同过滤',
        'POPULAR': '热门推荐',
        'SIMILAR_USER': '相似用户'
      }
      return map[type] || type
    },
    
    getConfidenceColor(confidence) {
      if (confidence >= 0.8) return '#67c23a'
      if (confidence >= 0.6) return '#e6a23c'
      return '#f56c6c'
    },

    getProductImage(product) {
      if (!product || !product.imageUrl) {
        return require('@/assets/logo.png')
      }
      const url = product.imageUrl
      if (/^https?:\/\//.test(url)) {
        return url
      }
      if (url.startsWith('/api/')) {
        return url
      }
      if (url.startsWith('/img/')) {
        return `/api${url}`
      }
      // 默认补全为文件服务路径
      return `/api/img/${url.replace(/^\//, '')}`
    }
  }
}
</script>

<style scoped lang="less">
.recommend-center-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  
  .main-content {
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px;
  }
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 32px 28px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
    color: white;
    
    .header-left {
      h2 {
        margin: 0 0 10px 0;
        font-size: 32px;
        font-weight: 700;
        color: white;
        display: flex;
        align-items: center;
        gap: 12px;
        
        .el-icon {
          font-size: 36px;
        }
      }
      
      .subtitle {
        margin: 0;
        color: rgba(255, 255, 255, 0.9);
        font-size: 15px;
        font-weight: 300;
      }
    }
    
    .header-right {
      .el-button {
        background: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;
        font-weight: 500;
        padding: 12px 24px;
        border-radius: 8px;
        transition: all 0.3s;
        
        &:hover {
          background: rgba(255, 255, 255, 0.3);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }
      }
    }
  }
  
  .stats-cards {
    margin-bottom: 20px;
    
    .stat-card {
      text-align: center;
      
      .stat-content {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          margin-bottom: 8px;
          
          .algorithm-type-tag {
            background: none !important;
            -webkit-background-clip: unset !important;
            -webkit-text-fill-color: unset !important;
            background-clip: unset !important;
            
            :deep(.el-tag__content) {
              color: #ffffff !important;
              font-weight: 700 !important;
              font-size: 16px !important;
            }
          }
        }
        
        .stat-label {
          font-size: 14px;
          color: #666;
        }
      }
    }
  }
  
  .recommendations-content {
    .recommendations-list {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    
    .recommendation-item {
      .product-card {
        border-radius: 16px;
        overflow: hidden;
        transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
        border: 1px solid rgba(0, 0, 0, 0.06);
        background: white;
        
        &:hover {
          box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
          transform: translateY(-6px);
        }
        
        :deep(.el-card__body) {
          padding: 0;
        }
      }
      
      .product-content {
        display: flex;
        gap: 20px;
        
        .product-image {
          position: relative;
          width: 200px;
          height: 200px;
          flex-shrink: 0;
          overflow: hidden;
          background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
          
          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 8px;
            cursor: pointer;
            transition: transform 0.3s;
            
            &:hover {
              transform: scale(1.05);
            }
          }
          
          .recommend-badge {
            position: absolute;
            top: 10px;
            right: 10px;
          }
        }
        
        .product-info {
          flex: 1;
          
          .product-name {
            font-size: 20px;
            font-weight: 700;
            color: #2c3e50;
            margin: 0 0 12px 0;
            cursor: pointer;
            transition: color 0.3s;
            
            &:hover {
              color: #667eea;
            }
          }
          
          .product-price {
            margin-bottom: 16px;
            
            .price {
              font-size: 24px;
              color: #f56c6c;
              font-weight: 700;
            }
            
            .discount-price {
              font-size: 16px;
              color: #999;
              text-decoration: line-through;
              margin-left: 8px;
            }
          }
          
          .recommendation-explanation {
            margin: 16px 0;
            
            :deep(.el-collapse) {
              border: none;
              
              .el-collapse-item__header {
                background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
                border-radius: 8px;
                padding: 12px 16px;
                font-weight: 600;
                color: #495057;
                border: none;
                
                &:hover {
                  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
                }
              }
              
              .el-collapse-item__content {
                padding: 0;
              }
            }
            
            .explanation-header {
              display: flex;
              align-items: center;
              gap: 8px;
              font-weight: 600;
            }
            
            .explanation-content {
              padding: 16px;
              background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
              border-radius: 4px;
              
              .explanation-item {
                margin-bottom: 16px;
                
                &:last-child {
                  margin-bottom: 0;
                }
                
                strong {
                  display: block;
                  margin-bottom: 10px;
                  color: #2c3e50;
                  font-size: 14px;
                  font-weight: 600;
                }
                
                p {
                  margin: 0;
                  color: #555;
                  line-height: 1.7;
                  font-size: 14px;
                }
                
                .similar-users {
                  margin-top: 10px;
                  
                  :deep(.el-tag) {
                    margin-right: 8px;
                    margin-bottom: 6px;
                    border-radius: 6px;
                    padding: 4px 10px;
                  }
                }

                .radar-wrapper {
                  width: 260px;
                  height: 220px;
                  background: #fff;
                  border: 1px solid #ebeef5;
                  border-radius: 8px;
                  padding: 8px;
                }

                .timeline {
                  margin-top: 12px;
                  border-left: 3px solid #667eea;
                  padding-left: 16px;
                  display: flex;
                  flex-direction: column;
                  gap: 14px;
                }

                .timeline-item {
                  position: relative;
                  padding-left: 16px;
                }

                .timeline-dot {
                  position: absolute;
                  left: -19px;
                  top: 6px;
                  width: 12px;
                  height: 12px;
                  background: #667eea;
                  border-radius: 50%;
                  border: 3px solid #fff;
                  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
                }

                .timeline-content {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 10px;
                  align-items: center;
                  color: #555;
                  font-size: 13px;
                }

                .factor-type {
                  padding: 4px 10px;
                  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                  color: white;
                  border-radius: 12px;
                  font-weight: 600;
                  font-size: 12px;
                }

                .factor-desc {
                  flex: 1;
                  min-width: 120px;
                }

                .factor-score {
                  color: #67c23a;
                  font-weight: 700;
                  font-size: 14px;
                }
              }
            }
          }
          
          .product-actions {
            display: flex;
            gap: 12px;
            margin-top: 16px;
            
            .el-button {
              border-radius: 8px;
              font-weight: 500;
              padding: 10px 20px;
              transition: all 0.3s;
              
              &:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
              }
            }
          }
        }
      }
    }
  }
}

.stats-and-history {
  margin-bottom: 20px;
  
  :deep(.el-row) {
    margin: 0;
  }
  
  :deep(.el-col) {
    padding: 0 10px;
    
    &:first-child {
      padding-left: 0;
    }
    
    &:last-child {
      padding-right: 0;
    }
  }
}

.feedback-history-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.feedback-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
  
  .title {
    font-weight: 700;
    color: #2c3e50;
    font-size: 16px;
  }
  
  .el-button {
    font-size: 12px;
  }
}

.feedback-empty {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 40px 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.feedback-history-list {
  max-height: 260px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
    
    &:hover {
      background: #a8a8a8;
    }
  }
}

.feedback-item {
  padding: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
  
  &:hover {
    background: linear-gradient(135deg, #e9ecef 0%, #f8f9fa 100%);
    transform: translateX(4px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
}

.feedback-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 500;
}

.feedback-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #6c757d;
}

.feedback-type {
  color: #667eea;
  font-weight: 600;
  padding: 2px 8px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 4px;
}

.feedback-time {
  font-style: italic;
  color: #adb5bd;
}

.feedback-product {
  font-weight: 600;
}

@media (max-width: 768px) {
  .recommend-center-page {
    .main-content {
      padding: 16px;
    }
    
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding: 24px 20px;
      
      h2 {
        font-size: 24px;
      }
    }
    
    .recommend-center-page {
      .product-content {
        flex-direction: column;
        
        .product-image {
          width: 100%;
          height: auto;
        }
      }
    }
  }
}
</style>

