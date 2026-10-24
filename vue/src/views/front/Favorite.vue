<template>
  <div class="favorite-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>我的收藏</h2>
        <div class="header-actions">
          <div class="favorite-count">共 {{ total }} 件商品</div>
          <el-button 
            type="primary" 
            size="small" 
            @click="refreshAndVerifyFavorites"
            :loading="loading"
          >
            刷新
          </el-button>
        </div>
      </div>

      <!-- 收藏列表 -->
      <div class="favorite-content" v-loading="loading">
        <!-- 空收藏提示 -->
        <el-empty 
          v-if="!loading && favorites.length === 0"
          description="暂无收藏商品"
        >
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>

        <!-- 收藏商品列表 -->
        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="item in favorites" :key="item.id">
            <product-card
              :product="item.product"
              :is-favorite-page="true"
      
              @toggle-favorite="handleCancelFavorite"
            />
          </el-col>
        </el-row>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="favorites.length > 0">
          <el-pagination
            background
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, jumper"
            @current-change="handlePageChange"
          >
          </el-pagination>
        </div>
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
import eventBus from '@/utils/eventBus'
import logger from '@/utils/logger'

export default {
  name: 'FavoriteView',
  components: {
    FrontHeader,
    FrontFooter,
    ProductCard
  },
  data() {
    return {
      cancelingFavorite: false,
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: false,
      favorites: [],
      currentPage: 1,
      pageSize: 12,
      total: 0
    }
  },
  computed: {
    totalItems() {
      return this.total
    }
  },
  methods: {
    // 获取收藏列表
    async getFavorites() {
      this.loading = true
      try {
        const userId = this.userInfo.id
        
        logger.debug('获取收藏列表请求:', {
          userId: userId,
          currentPage: this.currentPage,
          pageSize: this.pageSize
        })
        
        const res = await Request.get(`/favorite/page?userId=${userId}&currentPage=${this.currentPage}&size=${this.pageSize}`)
        
        logger.debug('收藏列表响应:', {
          code: res.code,
          data: res.data,
          total: res.data?.total,
          records: res.data?.records?.length
        })
        
        if (res.code === '0') {
          if(res.data.total === 0){
            this.favorites = []
            this.total = 0
            logger.debug('用户暂无收藏商品')
          } else {
            this.favorites = res.data.records.map(item => ({
              ...item,
              product: {
                ...item.product,
                isFavorite: item.status === 1  // 设置收藏状态
              }
            }))
            this.total = res.data.total
            
            logger.debug('收藏列表加载完成 - 数据结构检查:', {
              显示商品数: this.favorites.length,
              总数: this.total,
              '原始API响应records': res.data.records,
              '处理后的favorites': this.favorites,
              '第一个商品详情': this.favorites[0] ? {
                'favorites[0].id': this.favorites[0].id,
                'favorites[0].productId': this.favorites[0].productId,
                'favorites[0].product': this.favorites[0].product,
                'favorites[0].product?.id': this.favorites[0].product?.id,
                'favorites[0].status': this.favorites[0].status,
                '完整对象': this.favorites[0]
              } : null
            })
          }
        } else {
          logger.error('获取收藏列表失败:', res.msg)
          this.$message({
            type: 'error',
            message: res.msg || '获取收藏列表失败'
          })
        }
      } catch (error) {
        logger.error('获取收藏列表异常:', error)
        this.$message({
          type: 'error',
          message: '获取收藏列表失败'
        })
      } finally {
        this.loading = false
      }
    },
    // 取消收藏
    async handleCancelFavorite(item) {
      if (this.cancelingFavorite) return
      const productId = item.productId ?? item.product?.id ?? item.id
      if (productId == null) { this.$message.error('图书信息缺失，请刷新重试'); return }
      this.cancelingFavorite = true
      try {
        await this.$confirm('确定要取消收藏该图书吗？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
        const result = await Request.put(`/favorite/${this.userInfo.id}/${productId}/status`, null, { params: { status: 0 } })
        if (result.code !== '0') { this.$message.error(result.msg || '取消收藏失败'); return }
        // One global event updates the list, its count, and all book cards.
        eventBus.emit('favoriteStatusChanged', { userId: this.userInfo.id, productId, status: false })
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') this.$message.error('取消收藏失败，请重试')
      } finally { this.cancelingFavorite = false }
    },
    isLogin() {
      const userStr = localStorage.getItem('frontUser')
      if (!userStr) {
        this.$message({
          type: 'warning',
          message: '请先登录'
        })
        this.$router.push('/login')
        return
      }
    },
    

    // 跳转到商品详情
    goToDetail(product) {
      this.$router.push(`/product/${product.id}`)
    },
    // 页码改变
    handlePageChange() {
      this.getFavorites()
    },

    // 处理全局收藏状态变化
    handleGlobalFavoriteChange(data) {
      if (data.userId != null && String(data.userId) !== String(this.userInfo.id)) return
      logger.debug('Favorite页收到全局收藏状态变化:', data)
      
      // 如果商品被取消收藏，从收藏列表中移除
      if (!data.status) {
        const beforeCount = this.favorites.length
        this.favorites = this.favorites.filter(item => String(item.product.id) !== String(data.productId))
        const afterCount = this.favorites.length
        
        if (beforeCount !== afterCount) {
          logger.debug(`从收藏列表移除商品 ID: ${data.productId}，数量从 ${beforeCount} 变为 ${afterCount}`)
          
          // 更新总数
          this.total = Math.max(0, this.total - (beforeCount - afterCount))
          
          // 显示提示信息
          this.$message.success('商品已从收藏中移除')
          
          // 如果当前页没有商品了，切换到上一页
          if (this.favorites.length === 0 && this.currentPage > 1) {
            this.currentPage--
            this.getFavorites()
          }
        }
      } else {
        // 如果商品被收藏，刷新列表以显示新收藏的商品
        logger.debug(`商品 ${data.productId} 被收藏，刷新收藏列表`)
        
        // 延迟刷新，确保服务器数据已保存
        setTimeout(() => {
          this.getFavorites()
        }, 500)
      }
    },

    // 刷新收藏列表并验证
    async refreshAndVerifyFavorites() {
      logger.debug('手动刷新收藏列表...')
      await this.getFavorites()
      
      this.$message({
        type: 'info',
        message: '收藏列表已刷新'
      })
    }
  },
  created() {
    // 监听全局收藏状态变化事件，实现页面间收藏状态同步
    eventBus.on('favoriteStatusChanged', this.handleGlobalFavoriteChange)
  },
  
  beforeUnmount() {
    // Vue 3 使用 beforeUnmount 替代 beforeDestroy
    // 移除事件监听器
    eventBus.off('favoriteStatusChanged', this.handleGlobalFavoriteChange)
  },
  
  mounted() {
    const userInfo = localStorage.getItem('frontUser')
    if(!userInfo){
      this.$message.warning('请先登录')
      this.$router.push('/login')
    }
    else{
      this.userInfo = JSON.parse(userInfo)
      this.getFavorites()
    }
  }
}
</script>

<style scoped>
.favorite-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, var(--store-surface) 0%, var(--store-surface-active) 100%);
}

.main-content {
  flex: 1;
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* 页面标题样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  border: 1px solid var(--store-border-light);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: var(--gradient-primary);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: var(--store-ink);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.favorite-count {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  background: rgba(var(--store-brand-rgb), 0.1);
  padding: 6px 16px;
  border-radius: 20px;
}

/* 收藏内容样式 */
.favorite-content {
  min-height: 400px;
  position: relative;
}

/* 商品网格样式 */
.el-row {
  margin: 0 -10px;
}

.el-col {
  padding: 10px;
  transition: all 0.3s ease;
}

.el-col:hover {
  transform: translateY(-2px);
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: var(--primary-color);
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: var(--primary-color);
}

/* 空状态样式 */
:deep(.el-empty) {
  padding: 60px 0;
  background: white;
  border-radius: 12px;
  border: 1px solid var(--store-border-light);
}

:deep(.el-empty .el-button) {
  margin-top: 20px;
  padding: 10px 24px;
  font-size: 15px;
  border-radius: 6px;
  background: var(--gradient-primary);
  border: none;
  transition: all 0.3s ease;
}

:deep(.el-empty .el-button:hover) {
  transform: translateY(-1px);
}

/* 加载状态样式 */
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

/* 响应式布局 */
@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
  
  .page-header {
    padding: 16px 20px;
  }
  
  .page-header h2 {
    font-size: 20px;
  }
  
  .favorite-count {
    font-size: 13px;
  }
}
</style> 