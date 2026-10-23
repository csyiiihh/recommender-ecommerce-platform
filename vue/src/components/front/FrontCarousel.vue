<template>
  <div class="carousel-container">
    <div v-if="!carouselItems.length" class="carousel-empty"><span>阅读，不止于此</span><p>每一次翻页，都是新的出发。</p><router-link to="/products">发现更多好书 →</router-link></div>
    <el-carousel v-else
      :interval="5000"
      arrow="hover"
      :height="carouselHeight + 'px'"
      :autoplay="false"
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
              <div class="price-section" v-if="item.product && Number(item.product.isDiscount) === 1">
                <div class="current-price">
                  <span class="currency">¥</span>
                  <span class="price">{{ item.product.discountPrice }}</span>
                </div>
                <span class="original-price">
                  ¥{{ item.product.price }}
                </span>
              </div>
              <div v-else-if="item.product" class="price-section">
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
      carouselHeight: 360 // 设置默认高度，避免初始为0
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
      this.carouselHeight = window.innerWidth <= 600 ? 220 : 360
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
.carousel-container { width:100%;min-width:0;border-radius:10px;overflow:hidden;box-shadow:none;background:#e9eee2; }
.carousel-item { position:relative;height:100%;display:flex;align-items:center;background:#e9eee2;overflow:hidden; }
.carousel-image-wrapper { position:absolute;left:56%;right:0;top:0;bottom:0;width:auto;height:auto;display:flex;align-items:center;padding:30px 24px 30px 0;box-sizing:border-box; }
.carousel-image { width:100%;height:100%;opacity:1; }
.carousel-image :deep(img) { object-fit:contain!important;filter:drop-shadow(6px 12px 8px #34482f26); }
.carousel-content { position:relative;inset:auto;width:58%;height:auto;padding:26px;box-sizing:border-box;background:transparent;display:block;color:#273e31; }
.content-wrapper { max-width:none;text-align:left;padding:0;width:100%;color:#273e31; }
.tag-wrapper { margin-bottom:12px; }
.tag { color:#596f55;background:#dbe4d0;border-radius:3px;font-size:10px;padding:4px 8px;letter-spacing:.1em; }
.title { font-family:'SimSun',serif;font-size:28px;line-height:1.5;margin:0 0 12px;text-shadow:none;font-weight:600;color:#273e31;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden; }
.description { font-size:12px;line-height:1.6;color:#65725d;margin:0 0 16px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden; }
.price-section { display:flex;gap:8px;align-items:baseline;justify-content:flex-start;margin:0 0 14px; }
.current-price { color:#9e5936;background:transparent;box-shadow:none;padding:0;font-weight:600; }
.currency { font-size:12px; }.price{font-size:24px;}.original-price{color:#839078;font-size:12px;}
.action-button { padding:10px 15px!important;height:36px;background:#315749!important;border:0!important;border-radius:5px!important;color:white;box-shadow:none!important;font-size:12px!important; }
.action-button i { display:none; }
.carousel-empty { padding:46px 28px;min-height:180px;box-sizing:border-box; }.carousel-empty span{font-family:'SimSun',serif;font-size:26px;}.carousel-empty p,.carousel-empty a{color:#5a705b;font-size:13px;}
@media(max-width:600px){.carousel-content{width:62%;padding:18px;} .carousel-image-wrapper{left:59%;padding:22px 12px 22px 0;}.title{font-size:20px;line-height:1.4;margin-bottom:8px;-webkit-line-clamp:2;}.description{font-size:10px;margin-bottom:8px;-webkit-line-clamp:1;}.tag-wrapper{margin-bottom:9px;}.tag{font-size:9px;}.price-section{margin-bottom:8px;}.price{font-size:20px;}.action-button{height:30px;padding:8px 12px!important;font-size:11px!important;}}


</style>
