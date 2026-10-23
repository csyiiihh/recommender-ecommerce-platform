<template>
  <div class="home" v-if="refresh" :key="refresh">
    <front-header />
    <main class="main-content">
      <section class="hero-layout" aria-label="精选图书">
        <div class="home-intro">
          <span class="eyebrow">BOOKS &amp; EVERYDAY LIFE</span>
          <h1>让好书，<br class="desktop-break" />走进日常。</h1>
          <p>从一本书开始，发现值得停留的世界。</p>
          <router-link to="/products" class="browse-link">探索全部图书 <span aria-hidden="true">↗</span></router-link>
          <div class="intro-note"><span></span> 阅读的下一站，由你选择</div>
        </div>
        <front-carousel />
      </section>
      <section class="category-strip" aria-label="图书分类"><front-category /></section>
      <section class="section recommend-section" aria-labelledby="recommend-title" :aria-busy="loading">
        <div class="section-header">
          <div><span class="eyebrow">DISCOVER YOUR NEXT READ</span><h2 id="recommend-title">{{ isLoggedIn ? '为你推荐' : '发现好书' }}</h2><p>在熟悉的喜好之外，遇见新的可能。</p></div>
          <router-link to="/products" class="section-more">全部图书 <span aria-hidden="true">→</span></router-link>
        </div>
        <div class="reading-tabs" role="group" aria-label="图书推荐方式">
          <button type="button" :class="{ selected: activeAlgorithm === 'personalized' }" :aria-pressed="activeAlgorithm === 'personalized'" @click="handleAlgorithmChange('personalized')">{{ isLoggedIn ? '为你精选' : '推荐图书' }}</button>
          <button type="button" :class="{ selected: activeAlgorithm === 'trending' }" :aria-pressed="activeAlgorithm === 'trending'" @click="handleAlgorithmChange('trending')">人气好书</button>
          <button v-if="isLoggedIn" type="button" class="preferences-link" @click="goToRecommendCenter">阅读偏好 <span aria-hidden="true">↗</span></button>
        </div>
        <div v-if="loading" class="loading-books" role="status">正在为你寻找好书…</div>
        <div v-else-if="recommendProducts.length" class="product-grid">
          <product-card v-for="product in recommendProducts" :key="product.id" :product="product" @toggle-favorite="handleToggleFavorite" />
        </div>
        <el-empty v-else description="暂时没有推荐图书，可以逛逛全部图书" />
      </section>
      <section class="section new-products-section" aria-labelledby="new-title">
        <div class="section-header">
          <div><span class="eyebrow">JUST ARRIVED</span><h2 id="new-title">书架上的新朋友</h2><p>新书上架，给阅读多一点新鲜感。</p></div>
          <router-link to="/products?type=new" class="section-more">更多新书 <span aria-hidden="true">→</span></router-link>
        </div>
        <div v-if="newProducts.length" class="product-grid">
          <product-card v-for="product in newProducts.slice(0, 4)" :key="product.id" :product="product" @toggle-favorite="handleToggleFavorite" />
        </div>
        <el-empty v-else description="新书正在整理中" />
      </section>
      <details class="service-notices"><summary>公告与服务 <span>查看商城最新消息</span></summary><front-notice /></details>
    </main>
    <front-footer />
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import FrontCarousel from '@/components/front/FrontCarousel.vue'
import FrontCategory from '@/components/front/FrontCategory.vue'
import ProductCard from '@/components/front/ProductCard.vue'
import FrontNotice from '@/components/front/FrontNotice.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'

export default {
  name: 'HomeView',
  components: { FrontHeader, FrontFooter, FrontCarousel, FrontCategory, ProductCard, FrontNotice },
  data() {
    return { recommendProducts: [], newProducts: [], isLoggedIn: false, userId: null, refresh: true, loading: true, activeAlgorithm: 'personalized', requestVersion: 0 }
  },
  methods: {
    async handleAlgorithmChange(algorithm) {
      this.activeAlgorithm = algorithm
      await this.loadRecommendations()
    },
    async loadRecommendations() {
      const version = ++this.requestVersion
      this.loading = true
      try {
        let products = []
        if (this.isLoggedIn && this.activeAlgorithm === 'personalized') {
          try {
            const result = await Request.get(`/recommend/user/${this.userId}/explanation`, { params: { algorithmType: 'PERSONALIZED' }, silent: true, timeout: 10000 })
            if (result.code === '0') products = result.data?.products || result.data?.records || result.data || []
          } catch (error) { logger.debug('个性化推荐暂不可用，使用图书列表', error.message) }
        }
        if (!Array.isArray(products) || !products.length) {
          const result = await Request.get('/product/page', { params: { size: 8, sortField: this.activeAlgorithm === 'trending' ? 'sales' : 'createdAt', sortOrder: 'desc' }, silent: true })
          products = result.code === '0' ? result.data?.records || [] : []
        }
        // A slow response from a previous tab must not overwrite the current selection.
        if (version === this.requestVersion) this.recommendProducts = products.slice(0, 8).map(p => ({ ...p }))
      } catch (error) {
        if (version === this.requestVersion) this.recommendProducts = []
        logger.error('推荐图书加载失败', error)
      } finally {
        if (version === this.requestVersion) this.loading = false
      }
    },
    async getNewProducts() {
      try {
        const result = await Request.get('/product/page', { params: { size: 4, sortField: 'updatedAt', sortOrder: 'desc' }, silent: true })
        if (result.code === '0') this.newProducts = (result.data?.records || []).slice(0, 4).map(p => ({ ...p }))
      } catch (error) { logger.error('新书加载失败', error) }
    },
    handleToggleFavorite({ product, status }) {
      for (const item of [...this.recommendProducts, ...this.newProducts]) if (item.id === product.id) item.isFavorite = status === 1
    },
    goToRecommendCenter() { this.$router.push('/recommend-center') }
  },
  created() {
    try {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      this.isLoggedIn = Boolean(user.id && user.token && user.role === 'USER')
      this.userId = this.isLoggedIn ? user.id : null
    } catch { this.isLoggedIn = false }
    this.loadRecommendations()
    this.getNewProducts()
  },
  beforeUnmount() { ++this.requestVersion }
}
</script>

<style scoped>
.home { min-height:100vh; background:#f8f7f3; color:#253e35; }
.main-content { width:100%; max-width:1280px; padding:36px 40px 48px; margin:0 auto; box-sizing:border-box; }
.hero-layout { display:grid; grid-template-columns: .8fr 1.5fr; gap:40px; align-items:center; }
.home-intro { padding:20px 0; }
.eyebrow { color:#738577; letter-spacing:.16em; font-size:11px; font-weight:600; }
h1 { font-family:'Noto Serif SC','Source Han Serif SC','SimSun',serif; font-size:52px; font-weight:600; line-height:1.4; letter-spacing:.06em; margin:16px 0; }
.home-intro p { font-size:14px; color:#69776e; line-height:1.8; margin-bottom:24px; }
.browse-link { display:inline-flex; align-items:center; gap:28px; padding:13px 20px; border-radius:6px; color:#fff; background:#315749; text-decoration:none; font-size:14px; }
.browse-link:hover { background:#254437; }
.intro-note { display:flex; align-items:center; gap:8px; margin-top:24px; font-size:11px; color:#7b877e; }
.intro-note span { width:6px; height:6px; border-radius:50%; background:#a8bca4; }
.category-strip { margin:26px 0 40px; padding:12px 0; border-block:1px solid #e4e7dd; }
.section { padding:0; margin-bottom:48px; background:transparent; }
.section-header { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:20px; }
.section-header h2 { font-family:'Noto Serif SC','SimSun',serif; font-size:27px; font-weight:600; margin:8px 0; letter-spacing:.03em; }
.section-header p { margin:0; font-size:13px; color:#7c847e; }
.section-more { color:#45604e; text-decoration:none; font-size:13px; white-space:nowrap; padding:10px 0 10px 10px; }
.section-more span { margin-left:12px; }
.reading-tabs { display:flex; gap:8px; align-items:center; margin-bottom:22px; }
.reading-tabs button { border:1px solid transparent; border-radius:5px; background:transparent; color:#64756a; cursor:pointer; padding:9px 16px; font-size:13px; min-height:38px; }
.reading-tabs button.selected { border-color:#d3ded1; background:#e9eee5; color:#315749; font-weight:600; }
.reading-tabs .preferences-link { margin-left:auto; padding-right:0; }
.product-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:22px; }
.loading-books { padding:64px 20px; text-align:center; color:#738577; }
.service-notices { border-top:1px solid #e1e6dd; padding-top:20px; }
.service-notices summary { cursor:pointer; font-size:14px; padding:10px 0; }
.service-notices summary span { font-size:12px; color:#7b877e; margin-left:16px; }
.service-notices :deep(.notice-container) { margin-top:12px; max-height:300px; }
a:focus-visible, button:focus-visible, summary:focus-visible { outline:2px solid #315749; outline-offset:4px; }
@media(max-width:1000px) { .main-content{padding:24px;} .hero-layout{gap:24px;grid-template-columns:.85fr 1.3fr;} h1{font-size:38px;} .product-grid{gap:16px;} }
@media(max-width:600px) {
 .main-content{padding:18px 16px 32px;} .hero-layout{display:flex;flex-direction:column;gap:14px;align-items:stretch;} .home-intro{padding:0;} .home-intro .eyebrow{font-size:9px;} h1{font-size:27px;margin:7px 0;line-height:1.4;} .desktop-break{display:none;} .home-intro p{font-size:12px;margin:0;} .browse-link,.intro-note{display:none;}
 .category-strip{margin:14px 0 24px;padding:8px 0;} .section{margin-bottom:32px;} .section-header{gap:8px;margin-bottom:16px;} .section-header .eyebrow{font-size:9px;letter-spacing:.1em;} .section-header h2{font-size:23px;margin:5px 0;} .section-header p{font-size:11px;} .section-more{font-size:12px;} .section-more span{margin-left:3px;} .product-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;} .reading-tabs{margin-bottom:16px;} .reading-tabs button{padding:8px 12px;font-size:12px;} .service-notices summary span{font-size:11px;}
}

@media(max-width:600px){.home-intro>.eyebrow{display:none;}.home-intro h1{font-size:25px;margin:0 0 5px;}.category-strip{margin-bottom:20px;}.section-header .eyebrow,.section-header p{display:none;}.section-header{margin-bottom:14px;}.section-header h2{margin:0;}.reading-tabs{margin-bottom:14px;}}
</style>
