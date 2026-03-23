<template>
  <div class="product-card" @click="handleClick">
    <!-- 骨架屏加载动画 -->
    <div v-if="loading" class="product-skeleton">
      <div class="skeleton-image"></div>
      <div class="skeleton-content">
        <div class="skeleton-line skeleton-title"></div>
        <div class="skeleton-line skeleton-price"></div>
        <div class="skeleton-line skeleton-info"></div>
        <div class="skeleton-line skeleton-actions"></div>
      </div>
    </div>
    
    <!-- 实际内容 -->
    <template v-else>
      <div class="image-container">
        <el-image 
          :src="buildImageUrl(product.imageUrl)" 
          class="product-image"
          fit="cover"
          lazy
          @load="imageLoaded"
          @error="imageError"
        >
          <template #error>
            <div class="image-slot">
              <i class="fas fa-image"></i>
              <span class="error-text">图片加载失败</span>
            </div>
          </template>
        </el-image>
        <div class="product-badges">
          <div class="badge new" v-if="product.isNew">新品</div>
          <div class="badge discount" v-if="product.isDiscount==1">特惠</div>
          <!-- 算法标识徽章 -->
          <div class="badge algo-badge" v-if="showAlgorithmInfo && algorithmType">
            <span class="algo-icon">{{ getAlgorithmIcon(algorithmType) }}</span>
            <span class="algo-text">{{ getAlgorithmName(algorithmType) }}</span>
          </div>
        </div>
      </div>
      
      <div class="product-content">
        <h3 class="product-name">{{ product.name }}</h3>
        
        <!-- 推荐解释（可折叠） -->
        <div class="recommendation-reason" v-if="showAlgorithmInfo && product.recommendationExplanation">
          <el-collapse>
            <el-collapse-item>
              <template #title>
                <div class="reason-header">
                  <i class="fas fa-info-circle"></i>
                  <span>推荐理由</span>
                </div>
              </template>
              <div class="reason-content">
                <p class="reason-text">{{ product.recommendationExplanation.reasonDescription || '基于您的阅读偏好推荐' }}</p>
                <div class="reason-meta" v-if="product.recommendationExplanation.score">
                  <span class="reason-score">推荐分数: {{ product.recommendationExplanation.score.toFixed(1) }}</span>
                  <span class="reason-confidence">置信度: {{ Math.round((product.recommendationExplanation.confidence || 0) * 100) }}%</span>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
        
        <div class="product-info">
          <div class="price-section">
            <div class="current-price">
              <span class="symbol">¥</span>
              <span class="amount">{{ product.isDiscount==1 ? product.discountPrice : product.price }}</span>
              <span class="original-price" v-if="product.isDiscount==1">¥{{ product.price }}</span>
            </div>
          </div>
          <div class="stock-info">
            <span class="stock" :class="{ 'low': product.stock < 10 }">
              库存: {{ product.stock }}
            </span>
            <span class="sales">已售{{ product.salesCount }}</span>
          </div>
          <div class="origin-info" v-if="product.placeOfOrigin">
            <i class="fas fa-map-marker-alt"></i>
            <span>{{ product.placeOfOrigin }}</span>
          </div>
        </div>
        
        <div class="product-footer">
          <div class="action-group">
            <div class="favorite-btn" 
              :class="{ 'is-favorite': isFavoritePage || product.isFavorite }"
              @click.stop="handleFavorite">
              <i :class="[isFavoritePage ? 'fas fa-trash-alt' : (product.isFavorite ? 'fas fa-heart' : 'far fa-heart')]"></i>
            </div>
            <div class="add-to-cart" @click.stop="handleAddToCart">
              <i class="fas fa-shopping-cart"></i>
              <span>加入购物车</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import Request from '@/utils/request'
import eventBus from '@/utils/eventBus'
import logger from '@/utils/logger'

export default {
  name: 'ProductCard',
  props: {
    product: {
      type: Object,
      required: true
    },
    isFavoritePage: {
      type: Boolean,
      default: false
    },
    showAlgorithmInfo: {
      type: Boolean,
      default: false
    },
    algorithmType: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: true,
      imageLoadComplete: false,
      favoriteStatusTimer: null // 收藏状态请求的定时器，用于延迟加载和清理
    }
  },
  methods: {
    // 图片加载完成
    imageLoaded() {
      // 移除延迟，图片加载完立即显示
      this.imageLoadComplete = true;
      this.loading = false;
    },
    
    // 图片加载失败
    imageError() {
      this.imageLoadComplete = false;
      this.loading = false;
    },
    
    isLogin() {
      // 检查是否登录
      const userStr = localStorage.getItem('frontUser')
        if (!userStr) {
          this.$message({
            type: 'warning',
            message: '请先登录'
          })
          this.$router.push('/login')
          return false
        }
        return true
    },
    async handleAddToCart() {
      try {
   
         if(!this.isLogin()){
          return
         }

        const data = {
          userId: this.userInfo.id,
          productId: this.product.id,
          quantity: 1
        }

        const res = await Request.post('/cart', data)
        if (res.code === '0') {
          this.$message({
            type: 'success',
            message: '已添加到购物车'
          })
          this.$emit('add-to-cart', this.product)
        } else {
          this.$message({
            type: 'error',
            message: res.msg || '添加失败'
          })
        }
      } catch (error) {
        logger.error('添加到购物车失败:', error)
        this.$message({
          type: 'error',
          message: '添加到购物车失败'
        })
      }
    },

    async handleFavorite() {
      try {
        // 如果是在收藏页面，只触发事件
        if (this.isFavoritePage) {
          this.$emit('toggle-favorite', this.product)
          return
        }

        if(!this.isLogin()){
          return
        }

        // 记录操作前的状态
        const beforeStatus = this.product.isFavorite
        
        logger.debug('ProductCard收藏操作开始:', {
          productId: this.product.id,
          productName: this.product.name,
          beforeStatus: beforeStatus,
          beforeStatusType: typeof beforeStatus,
          expectOperation: beforeStatus ? '取消收藏' : '收藏',
          商品当前收藏状态: beforeStatus ? '已收藏' : '未收藏'
        })
        
        // 🔄 恢复原始源码的正确收藏逻辑
        // 原始源码逻辑: status: this.product.isFavorite ? 1 : 0
        // 这个逻辑是正确的，我之前理解错了
        const targetStatus = beforeStatus ? 1 : 0
        
        logger.debug('恢复原始源码收藏逻辑:', {
          '原始源码逻辑': 'status: isFavorite ? 1 : 0',
          '当前商品状态': beforeStatus ? '已收藏' : '未收藏',
          '用户操作意图': beforeStatus ? '取消收藏' : '添加收藏',
          '发送给服务器的状态': targetStatus,
          '说明': '已恢复原始源码的正确逻辑'
        })
        
        const data = {
          userId: this.userInfo.id,
          productId: this.product.id,
          status: targetStatus
        }
        
        logger.debug('ProductCard发送收藏请求详情:', {
          '请求数据': data,
          '用户ID': data.userId,
          '商品ID': data.productId, 
          '发送的状态值': data.status,
          '状态值类型': typeof data.status,
          '逻辑状态': '✅ 已恢复原始源码的正确逻辑'
        })
        
        const res = await Request.post('/favorite', data)
        
        logger.debug('ProductCard收藏接口响应:', {
          code: res.code,
          data: res.data,
          status: res.data?.status,
          msg: res.msg,
          fullResponse: res
        })
        
        if (res.code === '0') {
          // 检查服务器是否真正处理了收藏操作
          const serverConfirmedStatus = res.data?.status
          
          logger.debug('服务器收藏响应详情:', {
            发送的状态: targetStatus,
            服务器返回状态: serverConfirmedStatus,
            服务器返回状态类型: typeof serverConfirmedStatus,
            操作意图: targetStatus === 1 ? '收藏' : '取消收藏',
            完整响应: res,
            响应数据: res.data
          })
          
          if (serverConfirmedStatus !== undefined && serverConfirmedStatus !== null) {
            // 服务器确认状态处理 - 兼容不同的返回格式
            const isNowFavorite = serverConfirmedStatus === 1 || serverConfirmedStatus === '1' || serverConfirmedStatus === true
            
            logger.debug('收藏状态处理:', {
              原始服务器状态: serverConfirmedStatus,
              处理后收藏状态: isNowFavorite,
              操作类型: isNowFavorite ? '收藏成功' : '取消收藏成功'
            })
            
            // 更新UI状态 - 基于服务器确认状态
            this.product.isFavorite = isNowFavorite
            
            // 显示正确的操作反馈
            this.$message({
              type: 'success',
              message: isNowFavorite ? '收藏成功' : '已取消收藏'
            })
            
            logger.debug('ProductCard收藏操作完成:', {
              productId: this.product.id,
              productName: this.product.name,
              finalStatus: isNowFavorite,
              serverConfirmed: true,
              operationType: isNowFavorite ? '执行收藏' : '执行取消收藏'
            })
            
            // 触发事件并传递最新状态
            this.$emit('toggle-favorite', {
              product: this.product,
              status: isNowFavorite ? 1 : 0
            })
            
            // 触发全局收藏状态更新事件，同步其他页面
            eventBus.emit('favoriteStatusChanged', {
              productId: this.product.id,
              status: isNowFavorite
            })
          } else {
            logger.error('服务器响应缺少status字段:', res)
            this.$message({
              type: 'error',
              message: '收藏状态异常，请刷新页面重试'
            })
          }
        } else {
          this.$message({
            type: 'error',
            message: res.msg || '操作失败'
          })
        }
      } catch (error) {
        logger.error('收藏操作失败:', error)
        this.$message({
          type: 'error',
          message: '操作失败'
        })
      }
    },

    // 验证收藏状态的方法
    async verifyFavoriteStatus() {
      try {
        logger.debug(`验证商品 ${this.product.id} 的收藏状态...`)
        
        const userId = this.userInfo.id
        const res = await Request.get(`/favorite/check?userId=${userId}&productId=${this.product.id}`)
        
        logger.debug('收藏状态验证结果:', {
          productId: this.product.id,
          response: res,
          currentFrontendStatus: this.product.isFavorite
        })
        
        if (res.code === '0') {
          const serverStatus = res.data?.isFavorite || false
          
          if (serverStatus !== this.product.isFavorite) {
            logger.warn('发现前端与服务器状态不一致:', {
              productId: this.product.id,
              frontendStatus: this.product.isFavorite,
              serverStatus: serverStatus
            })
            
            // 同步服务器状态
            this.product.isFavorite = serverStatus
            
            // 触发全局状态更新
            eventBus.emit('favoriteStatusChanged', {
              productId: this.product.id,
              status: serverStatus
            })
            
            this.$message({
              type: 'info',
              message: '收藏状态已同步'
            })
          } else {
            logger.debug('前端与服务器状态一致')
          }
        }
      } catch (error) {
        logger.error('验证收藏状态失败:', error)
      }
    },

    // 获取收藏状态的方法（延迟加载，避免大量并发请求）
    async getFavoriteStatus() {
      if (this.userInfo?.id) {
        try {
          // 直接使用获取所有收藏列表的方式（check接口返回500，暂时不使用）
          // 注意：这个请求可能会很慢，因为需要获取用户的所有收藏记录
          const res = await Request.get(`/favorite/user/${this.userInfo.id}`, {
            timeout: 15000 // 15秒超时
          })
          
          if (res.code === '0') {
            const favorites = res.data || []
            const favoriteItem = favorites.find(item => item.productId === this.product.id)
            const isFavorite = favoriteItem && favoriteItem.status === 1
            
            this.product.isFavorite = isFavorite
          }
        } catch (error) {
          // 静默处理所有错误，不影响用户体验
          // 超时或服务器错误时保持当前状态，不更新
          if (error.code !== 'ECONNABORTED' && error.response?.status !== 500) {
            // 只记录非超时和非500的错误，但不显示给用户
            logger.debug('获取收藏状态失败（静默处理）:', {
              productId: this.product.id,
              error: error.message,
              code: error.code
            })
          }
        }
      }
    },

    handleClick() {
      this.$router.push(`/product/${this.product.id}`)
    },
    
    // 获取算法图标
    getAlgorithmIcon(type) {
      const icons = {
        'COLLABORATIVE': '👥',
        'CONTENT': '📝',
        'TRENDING': '🔥',
        'PERSONALIZED': '🎯',
        'SERENDIPITY': '🎲'
      };
      return icons[type] || '🤖';
    },
    
    // 获取算法名称
    getAlgorithmName(type) {
      const names = {
        'COLLABORATIVE': '协同过滤',
        'CONTENT': '内容匹配',
        'TRENDING': '热门趋势',
        'PERSONALIZED': '个性化',
        'SERENDIPITY': '惊喜发现'
      };
      return names[type] || '智能推荐';
    },
    
    // 设置一个延迟结束加载状态的方法
    startLoading() {
      this.loading = true;
      // 减少最大等待时间从2000ms到1000ms
      setTimeout(() => {
        this.loading = false;
      }, 1000);
    },

    // 构建正确的图片URL
    buildImageUrl(imageUrl) {
      if (!imageUrl) {
        return '';
      }
      
      // 如果已经是完整的HTTP/HTTPS URL，直接返回
      if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
        return imageUrl;
      }
      
      // 如果以 / 开头，说明是相对路径（如 /img/xxx.png）
      if (imageUrl.startsWith('/')) {
        return `/api${imageUrl}`;
      }
      
      // 如果只是文件名（如 1767148875583.png），添加 /img/ 前缀
      return `/api/img/${imageUrl}`;
    },
    
    // 在created钩子之前添加图片预加载方法
    preloadImage(src) {
      return new Promise((resolve, reject) => {
        if (!src) {
          reject(new Error('Image source is empty'));
          return;
        }
        
        const img = new Image();
        img.onload = () => resolve(src);
        img.onerror = (err) => {
          logger.warn('图片加载失败:', { src, error: err });
          reject(err);
        };
        img.src = src;
      });
    }
  },
  created() {
    this.startLoading();
  },
  mounted() {
    // 使用缓存的方式处理图片URL
    if (this.product.imageUrl) {
      const imgSrc = this.buildImageUrl(this.product.imageUrl);
      
      // 预加载图片
      this.preloadImage(imgSrc)
        .then(() => {
          this.imageLoadComplete = true;
          this.loading = false; // 预加载成功后直接关闭加载状态
        })
        .catch((error) => {
          logger.warn('图片预加载失败，将使用默认占位图:', { 
            imageUrl: this.product.imageUrl, 
            constructedUrl: imgSrc,
            error 
          });
          // 即使预加载失败，也通过startLoading中的超时机制最终关闭loading
          this.loading = false;
        });
    } else {
      // 没有图片URL时，直接关闭loading
      this.loading = false;
    }
    
    // 延迟加载收藏状态（仅在未从父组件获取到收藏状态时）
    // 如果父组件已经传递了 isFavorite 属性，则不需要再请求
    // 这样可以避免重复请求，减少后端压力
    if (this.product.isFavorite === undefined || this.product.isFavorite === null) {
      // 只有在收藏状态未定义时才请求
      // 使用随机延迟（1000-3000ms）分散请求，减少并发压力
      const randomDelay = Math.floor(Math.random() * 2000) + 1000; // 1000-3000ms随机延迟
      this.favoriteStatusTimer = setTimeout(() => {
        // 检查组件是否仍然挂载（简单检查，避免已销毁的组件发起请求）
        if (this.$el && this.$el.parentNode && (this.product.isFavorite === undefined || this.product.isFavorite === null)) {
          // 再次检查，避免在延迟期间父组件已经设置了收藏状态
          this.getFavoriteStatus();
        }
      }, randomDelay);
    } else {
      // 父组件已经提供了收藏状态，不需要请求
      logger.debug('收藏状态已从父组件获取，跳过独立请求:', {
        productId: this.product.id,
        isFavorite: this.product.isFavorite
      });
    }
  },
  
  beforeUnmount() {
    // 组件销毁前清理定时器，避免内存泄漏和无效请求
    if (this.favoriteStatusTimer) {
      clearTimeout(this.favoriteStatusTimer);
      this.favoriteStatusTimer = null;
    }
  }
}
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 2px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  cursor: pointer;
  min-height: 350px;
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.product-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.02) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 0;
  pointer-events: none;
}

.product-card:hover {
  transform: translateY(-8px) scale(1.01);
  box-shadow: 
    0 16px 40px rgba(52, 152, 219, 0.15),
    0 8px 16px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(52, 152, 219, 0.1);
  border-color: rgba(52, 152, 219, 0.2);
}

.product-card:hover::before {
  opacity: 1;
}

/* 骨架屏样式 */
.product-skeleton {
  width: 100%;
  height: 100%;
  padding-bottom: 16px;
}

.skeleton-image {
  width: 100%;
  padding-bottom: 100%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-content {
  padding: 16px;
}

.skeleton-line {
  height: 16px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.skeleton-title {
  width: 80%;
  height: 18px;
}

.skeleton-price {
  width: 40%;
}

.skeleton-info {
  width: 65%;
}

.skeleton-actions {
  width: 100%;
  height: 32px;
  margin-top: 16px;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.image-container {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
  overflow: hidden;
  background: #f8f9fa;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.25, 1, 0.5, 1);
  object-fit: cover;
}

.product-card:hover .product-image {
  transform: scale(1.08);
}

.product-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
  z-index: 1;
}

.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.badge.new {
  background: var(--gradient-primary);
}

.badge.discount {
  background: linear-gradient(135deg, var(--accent-color), var(--accent-light));
}

.badge.algo-badge {
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 11px;
  animation: badgePulse 2s ease-in-out infinite;
}

.algo-icon {
  font-size: 12px;
}

.algo-text {
  font-weight: 600;
}

@keyframes badgePulse {
  0%, 100% {
    box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
  }
  50% {
    box-shadow: 0 2px 12px rgba(102, 126, 234, 0.5);
  }
}

/* 推荐理由样式 */
.recommendation-reason {
  margin: 8px 0;
  font-size: 12px;
}

:deep(.recommendation-reason .el-collapse) {
  border: none;
}

:deep(.recommendation-reason .el-collapse-item__header) {
  padding: 8px 0;
  height: auto;
  line-height: 1.5;
  border: none;
  background: transparent;
}

:deep(.recommendation-reason .el-collapse-item__wrap) {
  border: none;
}

.reason-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 500;
}

.reason-header i {
  font-size: 14px;
}

.reason-content {
  padding: 8px 0;
  font-size: 12px;
}

.reason-text {
  margin: 0 0 8px 0;
  color: #666;
  line-height: 1.5;
}

.reason-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #909399;
}

.reason-score,
.reason-confidence {
  padding: 2px 8px;
  background: rgba(58, 91, 160, 0.08);
  border-radius: 10px;
}

.product-content {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.product-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.8;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  height: 25px;
  margin-bottom: 0;
}

.product-info {
  margin-top: 2px;
  flex: 1;
}

.price-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.current-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.current-price .symbol {
  font-size: 13px;
  color: var(--accent-color);
  font-weight: 600;
}

.current-price .amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-color);
}

.original-price {
  color: #909399;
  font-size: 13px;
  text-decoration: line-through;
  margin-left: 6px;
}

.stock-info {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  background: rgba(245, 247, 250, 0.8);
  padding: 4px 10px;
  border-radius: 20px;
}

.stock {
  color: var(--primary-color);
  font-weight: 500;
}

.stock.low {
  color: #E6A23C;
}

.origin-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
  background: rgba(245, 247, 250, 0.8);
  padding: 4px 10px;
  border-radius: 20px;
}

.origin-info i {
  font-size: 14px;
  color: var(--primary-color);
}

.product-footer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f2f5;
}

.action-group {
  display: flex;
  gap: 10px;
}

.favorite-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #909399;
  transition: all 0.3s ease;
  background: #f5f7fa;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.favorite-btn:hover {
  background: #f0f2f5;
  transform: translateY(-2px);
}

.favorite-btn.is-favorite {
  color: #fff;
  background: linear-gradient(135deg, var(--accent-color), var(--accent-light));
  box-shadow: 0 4px 10px rgba(230, 126, 34, 0.2);
}

.add-to-cart {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--gradient-primary);
  color: white;
  padding: 10px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 4px 12px rgba(52, 152, 219, 0.25),
    0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.add-to-cart::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 50%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.add-to-cart:hover::before {
  transform: translateX(100%);
}

.add-to-cart:hover {
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-light));
  transform: translateY(-3px) scale(1.02);
  box-shadow: 
    0 8px 20px rgba(52, 152, 219, 0.35),
    0 4px 8px rgba(0, 0, 0, 0.12);
}

.add-to-cart:active {
  transform: translateY(-1px) scale(0.98);
}

.add-to-cart i {
  font-size: 16px;
}

.image-slot {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 24px;
  background: #f5f7fa;
  gap: 8px;
}

.image-slot .error-text {
  font-size: 12px;
  color: #c0c4cc;
}
</style> 