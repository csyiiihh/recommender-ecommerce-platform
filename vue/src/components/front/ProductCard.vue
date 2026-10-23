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
          fit="contain"
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
        <p v-if="product.author" class="book-author">{{ product.author }}</p>

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
            <div class="favorite-btn" role="button" tabindex="0" :aria-label="favoriteState === undefined ? '确认收藏状态' : (favoriteState ? '取消收藏' : '收藏图书')" :aria-pressed="favoriteState === true" :aria-busy="favoriteBusy" @keydown.enter.stop="handleFavorite"
              :class="{ 'is-favorite': isFavoritePage || favoriteState }"
              @click.stop="handleFavorite">
              <i :class="[isFavoritePage ? 'fas fa-trash-alt' : (favoriteBusy ? 'fas fa-spinner fa-spin' : (favoriteState === undefined ? 'fas fa-circle-question' : (favoriteState ? 'fas fa-heart' : 'far fa-heart')))]"></i>
            </div>
            <div class="add-to-cart" role="button" tabindex="0" :aria-label="'将' + product.name + '加入购物车'" @keydown.enter.stop="handleAddToCart" @click.stop="handleAddToCart">
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
import { favoriteStatus, loadFavoriteStates, toggleFavorite, readFrontUser } from '@/utils/favoriteState'
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
      userInfo: readFrontUser(),
      loading: true,
      imageLoadComplete: false,
      favoriteBusy: false
    }
  },
  computed: { favoriteState() { return this.isFavoritePage ? true : favoriteStatus(this.userInfo.id, this.product.id) } },
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
      if (this.isFavoritePage) { this.$emit('toggle-favorite', this.product); return }
      if (!this.isLogin() || this.favoriteBusy) return
      if (this.favoriteState === undefined) { await this.getFavoriteStatus(); return }
      this.favoriteBusy = true
      try {
        const status = await toggleFavorite(this.userInfo.id, this.product.id)
        if (status == null) return
        this.$emit('toggle-favorite', { product: this.product, status })
        this.$message.success(status === 1 ? '已收藏' : '已取消收藏')
      } catch (error) { this.$message.error(error.message || '收藏操作失败，请重试') }
      finally { this.favoriteBusy = false }
    },

    // 验证收藏状态的方法
    async verifyFavoriteStatus() {
      return this.getFavoriteStatus()
    },

    // 获取收藏状态的方法（延迟加载，避免大量并发请求）
    async getFavoriteStatus() {
      if (!this.userInfo?.id) return
      this.favoriteBusy = true
      try { await loadFavoriteStates(this.userInfo.id) }
      catch (error) { logger.debug('收藏状态暂不可用，点击时会重试', error.message) }
      finally { this.favoriteBusy = false }
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

    if (this.userInfo?.id) this.getFavoriteStatus()

  },

  beforeUnmount() {

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

.product-card { border:1px solid #e7e9e1;border-radius:7px;box-shadow:none;background:#fff;overflow:hidden;transition:border-color .2s; }
.product-card:hover { transform:none;box-shadow:0 5px 18px #34482f0d;border-color:#b7c5b0; }
.image-container { height:auto!important;aspect-ratio:4/4.4;background:#f0f1e9;padding:18px 24px;box-sizing:border-box; }
.product-image { width:100%;height:100%;mix-blend-mode:multiply; }
.product-content { padding:14px; }
.product-name { color:#2e4637;font-size:14px;line-height:1.5;margin:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;min-height:21px; }
.book-author { color:#7d877c;font-size:11px;line-height:1.6;margin:5px 0 8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis; }
.badge { box-shadow:none!important;border-radius:3px!important;padding:3px 5px!important;font-size:9px!important; }.badge.new{background:#546d52!important;}.badge.discount{background:#ac7648!important;}
.price .amount,.current-price,.amount { color:#a25d3d; }
.stock-info { display:none; }
.add-to-cart { color:#315749;background:#edf2e8;border:1px solid #dce5d4;box-shadow:none;border-radius:4px;min-height:32px;font-size:11px; }
.add-to-cart:hover { background:#315749;color:white;transform:none;box-shadow:none; }
.favorite-btn { background:transparent;box-shadow:none;color:#7d8c78; }
.add-to-cart:focus-visible,.favorite-btn:focus-visible { outline:2px solid #315749;outline-offset:2px; }
@media(max-width:600px){.image-container{padding:14px 16px;aspect-ratio:4/4.5;}.product-content{padding:10px;}.product-name{font-size:12px;}.book-author{font-size:10px;margin:4px 0 7px;}.add-to-cart{font-size:10px;min-height:30px;}.add-to-cart i{display:none;}.product-badges{top:7px;left:7px;gap:4px;}.badge{font-size:8px!important;}}

</style>
