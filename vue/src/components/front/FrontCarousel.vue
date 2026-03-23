<template>
  <div class="carousel-container">
    <el-carousel 
      :interval="5000" 
      arrow="hover" 
      :height="carouselHeight + 'px'"
      :autoplay="true"
    >
      <el-carousel-item v-for="item in carouselItems" :key="item.id">
        <div class="carousel-item">
          <div class="carousel-image-wrapper">
            <el-image 
              :src="buildImageUrl(item.imageUrl)" 
              fit="cover"
              class="carousel-image"
            >
              <template #error>
                <div class="image-slot">
                  <i class="fas fa-image"></i>
                  <span class="error-text">图片加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
          <div class="carousel-content">
            <div class="content-wrapper">
              <div class="tag-wrapper">
                <span class="tag">{{ item.tag }}</span>
              </div>
              <h2 class="title">{{ item.title }}</h2>
              <p class="description">{{ item.description }}</p>
              <div class="price-section" v-if="item.product && item.product.isDiscount">
                <div class="current-price">
                  <span class="currency">¥</span>
                  <span class="price">{{ item.product.discountPrice }}</span>
                </div>
                <span class="original-price">
                  ¥{{ item.product.price }}
                </span>
              </div>
              <div v-if="item.product" class="price-section">
                <div class="current-price">
                  <span class="currency">¥</span>
                  <span class="price">{{ item.product.price }}</span>
                </div>
              </div>
              <el-button v-if="item.product" 
                type="primary" 
                round 
                class="action-button"
                @click="handleView(item)"
              >
                <i class="fas fa-shopping-cart"></i>
                立即购买
                <i class="fas fa-arrow-right"></i>
              </el-button>
            </div>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'

export default {
  name: 'FrontCarousel',
  data() {
    return {
      carouselItems: [],
      carouselHeight: 600 // 设置默认高度，避免初始为0
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.calculateHeight()
      window.addEventListener('resize', this.calculateHeight)
    })
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.calculateHeight)
  },
  created() {
    this.getCarouselItems()
  },
  methods: {
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
    calculateHeight() {
      // 固定高度400px，与公告一致
      this.carouselHeight = 400
    },
    async getCarouselItems() {
      try {
        const res = await Request.get('/carousel/active')
        if (res.code === '0') {
          this.carouselItems = res.data
        }
      } catch (error) {
        logger.error('获取轮播图数据失败:', error)
      }
    },
    handleView(item) {
      if (item.product) {
        this.$router.push(`/product/${item.product.id}`)
      }
    }
  }
}
</script>

<style scoped>
.carousel-container {
  margin: 0;
  max-width: 1400px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  width: 100%;
  margin: 0 auto;
  padding: 0;
  position: relative;
  height: 400px; /* 固定高度，与公告一致 */
}

:deep(.el-carousel__container) {
  height: 400px !important;
}

.carousel-item {
  height: 100%;
  display: flex;
  position: relative;
  overflow: hidden;
}

.carousel-image-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 100%;
  transition: transform 0.6s ease;
}

.el-carousel__item:hover .carousel-image {
  transform: scale(1.05);
}

.carousel-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, 
    rgba(0,0,0,0.75) 0%, 
    rgba(0,0,0,0.5) 40%, 
    rgba(0,0,0,0.2) 70%,
    rgba(0,0,0,0) 100%
  );
}

.content-wrapper {
  padding: 0 80px;
  max-width: 600px;
  color: white;
}

.tag-wrapper {
  margin-bottom: 20px;
  position: relative;
  display: inline-block;
}

.tag {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(58, 91, 160, 0.9);
  color: white;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.3);
}

.title {
  font-size: 42px;
  font-weight: 600;
  margin: 0 0 20px;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.description {
  font-size: 18px;
  line-height: 1.6;
  margin: 0 0 30px;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.price-section {
  margin-bottom: 30px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.current-price {
  display: flex;
  align-items: baseline;
  background: rgba(58, 91, 160, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.currency {
  font-size: 20px;
  font-weight: 500;
  vertical-align: baseline;
  color: var(--primary-color);
}

.price {
  font-size: 36px;
  font-weight: 600;
  color: var(--primary-color);
  margin-left: 4px;
  text-shadow: 0 2px 4px rgba(58, 91, 160, 0.2);
}

.original-price {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: line-through;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.action-button {
  width: 200px;
    height: 42px;
    font-size: 16px;
    font-weight: 500;
    letter-spacing: 1px;
    margin-top: 20px;
    border-radius: 8px;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(58, 91, 160, 0.4);
}

.action-button i {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.action-button:hover i.fa-arrow-right {
  transform: translateX(4px);
}

/* 修改轮播图指示器样式 */
:deep(.el-carousel__indicators) {
  bottom: 30px;
}

:deep(.el-carousel__indicator) {
  padding: 12px 4px;
}

:deep(.el-carousel__button) {
  width: 30px;
  height: 3px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  transition: all 0.3s ease;
}

:deep(.el-carousel__indicator.is-active .el-carousel__button) {
  background-color: #fff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
}

/* 修改箭头样式 */
:deep(.el-carousel__arrow) {
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  width: 44px;
  height: 44px;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

:deep(.el-carousel__arrow:hover) {
  background-color: rgba(255, 255, 255, 0.25);
  transform: scale(1.1);
}

:deep(.el-carousel__arrow i) {
  color: rgba(255, 255, 255, 0.9);
  font-size: 20px;
}

/* 移除 indicator-position="outside" 相关样式 */
.el-carousel {
  margin-bottom: 0;
}

:deep(.el-carousel__container) {
  margin-bottom: 0;
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