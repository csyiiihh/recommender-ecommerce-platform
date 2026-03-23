<template>
  <div class="group-buy-list landscape-bg">
    <!-- 导航栏 -->
    <front-header></front-header>
    
    <!-- 主要内容 -->
    <div class="main-content cloud-decoration">
      <!-- 页面标题 -->
      <div class="page-header porcelain-texture">
        <div class="title-container corner-decoration">
          <div class="title-wrapper">
            <div class="title-icon-wrapper">
              <i class="fas fa-users"></i>
              <div class="icon-glow"></div>
            </div>
            <div class="title-content">
              <h2 class="page-title brush-text">
                <span>限时拼团</span>
                <div class="title-accent"></div>
              </h2>
              <div class="page-subtitle">
                <span class="subtitle-text">好友一起拼，价格更优惠！</span>
                <div class="products-count" v-if="total > 0">
                  <i class="fas fa-fire"></i>
                  {{ total }} 个活动进行中
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 活动列表 -->
      <div class="activity-container" v-loading="loading">
      <el-empty v-if="!loading && activities.length === 0" description="暂无拼团活动"></el-empty>
      
      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="activity in activities" :key="activity.id">
          <el-card 
            class="activity-card" 
            :body-style="{ padding: '0px' }"
            shadow="hover"
            @click="goToDetail(activity.id)">
            
            <!-- 商品图片 -->
            <div class="card-image">
              <img :src="getImageUrl(activity.product)" alt="">
              <div class="tag-container">
                <el-tag type="danger" size="small" effect="dark">
                  {{ activity.requiredMembers }}人成团
                </el-tag>
              </div>
            </div>
            
            <!-- 商品信息 -->
            <div class="card-content">
              <h3 class="title" :title="activity.activityName">{{ activity.activityName }}</h3>
              
              <!-- 价格 -->
              <div class="price-section">
                <div class="group-price">
                  <span class="label">拼团价</span>
                  <span class="price">¥{{ activity.groupPrice }}</span>
                </div>
                <div class="original-price">
                  原价 ¥{{ activity.originalPrice }}
                </div>
              </div>
              
              <!-- 底部信息 -->
              <div class="card-footer">
                <div class="sales-info">
                  <i class="fas fa-shopping-cart"></i>
                  已拼{{ activity.salesCount }}件
                </div>
                <el-button type="danger" size="small" round>
                  立即拼团
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          @current-change="handlePageChange"
          :current-page="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total">
        </el-pagination>
      </div>
    </div>
    
    </div><!-- main-content -->
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import FrontHeader from '@/components/front/FrontHeader.vue'

export default {
  name: 'GroupBuyList',
  components: {
    FrontHeader
  },
  data() {
    return {
      loading: false,
      activities: [],
      currentPage: 1,
      pageSize: 12,
      total: 0
    }
  },
  mounted() {
    this.loadActivities()
  },
  methods: {
    async loadActivities() {
      this.loading = true
      try {
        // 使用专门的用户前台接口，只返回进行中的活动
        const res = await Request.get('/group-buy/activities/active', {
          params: {
            currentPage: this.currentPage,
            size: this.pageSize
          }
        })
        
        if (res.code === '200' || res.code === '0') {
          this.activities = res.data.records || []
          this.total = res.data.total || 0
        } else {
          this.$message.error(res.msg || '加载拼团活动失败')
        }
      } catch (error) {
        logger.error('加载拼团活动失败:', error)
        this.$message.error('加载失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    handlePageChange(page) {
      this.currentPage = page
      this.loadActivities()
      // 滚动到顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    
    goToDetail(activityId) {
      this.$router.push(`/group-buy/${activityId}`)
    },
    
    getImageUrl(product) {
      if (!product || !product.imageUrl) {
        return require('@/assets/logo.png')
      }
      // 如果是完整URL则直接返回，否则拼接/api前缀
      if (product.imageUrl.startsWith('http')) {
        return product.imageUrl
      }
      return `/api${product.imageUrl}`
    }
  }
}
</script>

<style scoped>
.group-buy-list {
  min-height: 100vh;
  background: linear-gradient(to bottom, #ecf5ff 0%, #ffffff 100%);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
}

/* 页面标题样式 - 和Products.vue保持一致 */
.page-header {
  margin-bottom: 30px;
}

.title-container {
  margin-bottom: 25px;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-icon-wrapper {
  position: relative;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(58, 91, 160, 0.3);
}

.title-icon-wrapper i {
  font-size: 28px;
  color: white;
  z-index: 1;
}

.icon-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  border-radius: 16px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}

.title-content {
  flex: 1;
}

.page-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  position: relative;
  display: inline-block;
}

.page-title span {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-accent {
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 60px;
  height: 4px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.page-subtitle {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.subtitle-text {
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

.products-count {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--gradient-primary);
  border-radius: 20px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.3);
}

.products-count i {
  font-size: 14px;
}

.activity-container {
  min-height: 500px;
}

.activity-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  border-radius: 8px;
  overflow: hidden;
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.card-image {
  width: 100%;
  height: 200px;
  position: relative;
  overflow: hidden;
  background: #f5f5f5;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.activity-card:hover .card-image img {
  transform: scale(1.1);
}

.tag-container {
  position: absolute;
  top: 10px;
  left: 10px;
}

.card-content {
  padding: 15px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 12px 0;
  height: 44px;
  line-height: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-section {
  margin-bottom: 12px;
}

.group-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 5px;
}

.group-price .label {
  font-size: 12px;
  color: var(--accent-color);
  font-weight: 500;
}

.group-price .price {
  font-size: 24px;
  color: var(--accent-color);
  font-weight: bold;
}

.original-price {
  font-size: 13px;
  color: #999;
  text-decoration: line-through;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.sales-info {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .group-buy-list {
    padding: 15px;
  }
  
  .card-image {
    height: 180px;
  }
  
  .title {
    font-size: 15px;
  }
  
  .group-price .price {
    font-size: 20px;
  }
}
</style>

