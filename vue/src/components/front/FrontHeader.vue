<template>
  <div class="front-header porcelain-texture">
    <!-- 中国风背景装饰 -->
    <div class="header-bg-decoration landscape-bg">
      <!-- 祥云图案 -->
      <div class="cloud-pattern-layer"></div>
      <!-- 水波纹效果 -->
      <div class="water-ripple-layer"></div>
      <!-- 动态粒子 -->
      <div class="bg-particles">
        <div v-for="n in 12" :key="n" class="particle" :style="getParticleStyle()"></div>
      </div>
    </div>

    <div class="header-container">
      <!-- Logo区域 -->
      <div class="logo-area" @click="$router.push('/')">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <i class="fas fa-book-open"></i>
            <div class="logo-accent"></div>
          </div>
          <div class="logo-text">
            <span class="brand-name">在线销售平台</span>
            <span class="brand-subtitle">Book & Knowledge</span>
          </div>
        </div>
      </div>

      <!-- 导航菜单 -->
      <div class="nav-section">
        <nav class="nav-menu">
          <div 
            v-for="item in navItems" 
            :key="item.path"
            :class="['nav-item', { 'active': isActiveRoute(item.path) }]"
            @click.stop="navigateTo(item.path)"
          >
            <div class="nav-content" @click.stop="navigateTo(item.path)">
              <i :class="item.icon"></i>
              <span class="nav-text">{{ item.name }}</span>
              <div class="nav-indicator"></div>
            </div>
          </div>
        </nav>
      </div>

      <!-- 移动端导航按钮 -->
      <div class="mobile-nav-btn" @click="toggleMobileNav">
        <i class="fas fa-bars"></i>
      </div>

      <!-- 移动端导航菜单 -->
      <div v-if="showMobileNav" class="mobile-nav-overlay" @click="closeMobileNav">
        <div class="mobile-nav-menu" @click.stop>
          <div class="mobile-nav-header">
            <span>导航菜单</span>
            <i class="fas fa-times" @click="closeMobileNav"></i>
          </div>
          <div class="mobile-nav-items">
            <div 
              v-for="item in navItems" 
              :key="item.path"
              :class="['mobile-nav-item', { 'active': isActiveRoute(item.path) }]"
              @click="navigateToMobile(item.path)"
            >
              <i :class="item.icon"></i>
              <span>{{ item.name }}</span>
              <i class="fas fa-chevron-right"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧功能区 -->
      <div class="right-section">
        <!-- 搜索框 -->
        <div class="search-container">
          <div class="search-box">
            <input 
              v-model="searchKey" 
              @keyup.enter="handleSearch"
              @focus="searchFocused = true"
              @blur="hideSuggestions"
              placeholder="搜索商品、资讯..."
              class="search-input"
            />
            <i class="fas fa-search search-icon"></i>
            <div class="search-effects">
              <div class="search-glow"></div>
            </div>
          </div>
          
          <!-- 搜索建议 -->
          <div v-if="searchFocused && searchSuggestions.length > 0" class="search-suggestions">
            <div class="suggestions-header">
              <i class="fas fa-clock"></i>
              <span>热门搜索</span>
            </div>
            <div 
              v-for="suggestion in searchSuggestions" 
              :key="suggestion"
              @click="selectSearchSuggestion(suggestion)"
              class="suggestion-item"
            >
              <i class="fas fa-search"></i>
              <span>{{ suggestion }}</span>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-actions">
          <!-- 算法控制按钮（仅登录用户显示） -->
          <div class="algorithm-quick" @click="goToRecommendCenter" v-if="isLoggedIn" title="智能推荐">
            <i class="fas fa-robot"></i>
          </div>
          
          <!-- 个性化设置按钮（仅登录用户显示） -->
          <div class="personalize-quick" @click="showPersonalizeDialog = true" v-if="isLoggedIn" title="个性化设置">
            <i class="fas fa-bullseye"></i>
          </div>
          
          <!-- 客服快捷入口 -->
          <div class="service-quick" @click="toggleCustomerService" title="在线客服">
            <i class="fas fa-headset"></i>
          </div>

          <!-- 购物车快捷入口 -->
          <div class="cart-quick" @click="$router.push('/cart')" v-if="cartCount > 0">
            <div class="cart-icon">
              <i class="fas fa-shopping-cart"></i>
              <span class="cart-badge">{{ cartCount }}</span>
            </div>
          </div>

          <!-- 收藏快捷入口 -->
          <div class="favorite-quick" @click="goToFavorite" v-if="isLoggedIn">
            <i class="fas fa-heart"></i>
          </div>
        </div>
        
        <!-- 个性化设置对话框 -->
        <el-dialog 
          v-model="showPersonalizeDialog" 
          title="🎯 个性化设置" 
          width="400px"
        >
          <div class="personalize-settings">
            <p class="settings-desc">调整推荐偏好，让算法更懂你</p>
            <el-button type="primary" @click="goToRecommendCenter" style="width: 100%;">
              <i class="fas fa-robot"></i>
              前往推荐中心
            </el-button>
          </div>
        </el-dialog>

        <!-- 用户操作区 -->
        <div class="user-actions">
          <!-- 未登录状态 -->
          <template v-if="!isLoggedIn">
            <el-button class="login-btn" @click="goToLogin">
              <i class="fas fa-user"></i>
              <span>登录</span>
              <div class="btn-glow"></div>
            </el-button>
          </template>
          
          <!-- 已登录状态 -->
          <template v-else>
            <el-dropdown trigger="click" class="user-dropdown">
              <div class="user-profile">
                <div class="avatar-wrapper">
                  <div class="user-avatar">
                    <img v-if="userInfo.businessLicense" :src="'/api' + userInfo.businessLicense" class="avatar-img" />
                    <i v-else class="fas fa-user"></i>
                  </div>
                  <div class="online-indicator"></div>
                </div>
                <div class="user-details">
                  <span class="username">{{ userInfo.username }}</span>
                  <span class="user-role">用户</span>
                </div>
                <i class="fas fa-chevron-down dropdown-arrow"></i>
              </div>
              
              <template #dropdown>
                <el-dropdown-menu class="user-menu">
                  <el-dropdown-item @click="goToUserCenter" class="menu-item">
                    <div class="item-content">
                      <i class="fas fa-user-circle"></i>
                      <span>个人中心</span>
                      <i class="fas fa-arrow-right item-arrow"></i>
                    </div>
                  </el-dropdown-item>
                  
                  <el-dropdown-item @click="goToFavorite" class="menu-item">
                    <div class="item-content">
                      <i class="fas fa-heart"></i>
                      <span>我的收藏</span>
                      <i class="fas fa-arrow-right item-arrow"></i>
                    </div>
                  </el-dropdown-item>
                  
                  <el-dropdown-item @click="$router.push('/order')" class="menu-item">
                    <div class="item-content">
                      <i class="fas fa-file-invoice"></i>
                      <span>我的订单</span>
                      <i class="fas fa-arrow-right item-arrow"></i>
                    </div>
                  </el-dropdown-item>
                  
                  <el-dropdown-item @click="$router.push('/my-group-buy')" class="menu-item">
                    <div class="item-content">
                      <i class="fas fa-users"></i>
                      <span>我的拼团</span>
                      <i class="fas fa-arrow-right item-arrow"></i>
                    </div>
                  </el-dropdown-item>
                  
                  <el-dropdown-item divided @click="logout" class="menu-item logout-item">
                    <div class="item-content">
                      <i class="fas fa-sign-out-alt"></i>
                      <span>退出登录</span>
                      <i class="fas fa-arrow-right item-arrow"></i>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import router from '@/router';
import logger from '@/utils/logger'
import eventBus from '@/utils/eventBus';

export default {
  name: 'FrontHeader',
  data() {
    return {
      searchKey: '',
      isLoggedIn: false,
      userInfo: {},
      searchFocused: false,
      cartCount: 0,
      showMobileNav: false,
      showPersonalizeDialog: false,
      searchSuggestions: [
        '文学小说', '科技教育', '历史传记', '艺术设计', '经管励志'
      ],
      navItems: [
        { path: '/', name: '首页', icon: 'fas fa-home' },
        { path: '/products', name: '全部商品', icon: 'fas fa-shopping-bag' },
        // { path: '/group-buy', name: '限时拼团', icon: 'fas fa-users' },
        { path: '/cart', name: '购物车', icon: 'fas fa-shopping-cart' },
        { path: '/order', name: '我的订单', icon: 'fas fa-file-invoice' },
        { path: '/articles', name: '图书资讯', icon: 'fas fa-newspaper' }
      ]
    }
  },
  created() {
    this.checkLoginStatus();
    this.updateCartCount();
  },
  mounted() {
    // 监听购物车变化
    window.addEventListener('cartUpdated', this.updateCartCount);
    // 监听用户信息更新
    window.addEventListener('userInfoUpdated', this.checkLoginStatus);
  },
  beforeUnmount() {
    window.removeEventListener('cartUpdated', this.updateCartCount);
    window.removeEventListener('userInfoUpdated', this.checkLoginStatus);
  },
  methods: {
    // 生成粒子样式
    getParticleStyle() {
      return {
        left: Math.random() * 100 + '%',
        animationDelay: Math.random() * 20 + 's',
        animationDuration: (10 + Math.random() * 20) + 's',
        width: (2 + Math.random() * 4) + 'px',
        height: (2 + Math.random() * 4) + 'px'
      }
    },

    // 检查登录状态
    checkLoginStatus() {
      const userInfo = localStorage.getItem('frontUser');
      if (userInfo) {
        this.isLoggedIn = true;
        this.userInfo = JSON.parse(userInfo);
      }
    },

    // 更新购物车数量
    async updateCartCount() {
      if (!this.isLoggedIn || !this.userInfo.id) {
        this.cartCount = 0;
        return;
      }
      
      try {
        // 这里需要根据实际API调用
        const cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
        this.cartCount = cartItems.length;
      } catch (error) {
        logger.error('获取购物车数量失败:', error);
        this.cartCount = 0;
      }
    },

    // 切换客服窗口
    toggleCustomerService() {
      eventBus.emit('toggle-customer-service');
    },

    // 导航到指定路径
    navigateTo(path) {
      try {
        if (this.$route.path !== path) {
          this.$router.push(path);
        }
      } catch (error) {
        logger.error('路由跳转失败:', error);
        this.$message.error('页面跳转失败');
      }
    },

    // 检查路由是否活跃
    isActiveRoute(path) {
      if (path === '/') {
        return this.$route.path === '/';
      }
      return this.$route.path.startsWith(path);
    },

    // 选择搜索建议
    selectSearchSuggestion(suggestion) {
      this.searchKey = suggestion;
      this.searchFocused = false;
      this.handleSearch();
    },

    // 隐藏搜索建议（延迟执行以允许点击）
    hideSuggestions() {
      setTimeout(() => {
        this.searchFocused = false;
      }, 200);
    },

    // 切换移动端导航显示状态
    toggleMobileNav() {
      this.showMobileNav = !this.showMobileNav;
    },

    // 关闭移动端导航
    closeMobileNav() {
      this.showMobileNav = false;
    },

    // 移动端导航跳转
    navigateToMobile(path) {
      this.closeMobileNav();
      this.navigateTo(path);
    },
    goToLogin() {
      this.$router.push('/login')
    },
    goToUserCenter() {
      this.$router.push('/user-center')
    },
    goToFavorite() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      this.$router.push('/favorite')
    },
    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('frontUser');
      this.isLoggedIn = false;
      this.userInfo = {};
      this.$message.success('已退出登录');
      this.$router.push('/');
      window.location.reload()
    },
    handleSearch() {
      if (!this.searchKey.trim()) {
        this.$message({
          type: 'warning',
          message: '请输入搜索关键词'
        })
        return
      }

      this.$router.push({
        path: '/search',
        query: {
          keyword: this.searchKey.trim()
        }
      })

      this.searchKey = ''
    },
    goToAdmin() {
      // 检查是否已登录
      const userMenuList = localStorage.getItem('userMenuList');
      const backUser = localStorage.getItem('backUser');
      if (!userMenuList || !backUser) {
        this.$message.warning('请先登录');
        this.$router.push('/login');
        return;
      }
      
      this.$router.push('/showView');
    },
    
    goToRecommendCenter() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录以查看个性化推荐');
        this.$router.push('/login');
        return;
      }
      this.$router.push('/recommend-center');
    }
  },
  computed: {
    activePath() {
      return this.$router.currentRoute.fullPath;
    }
  }
}
</script>

<style scoped>
/* 基础样式 */
.front-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 80px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  overflow: hidden;
  position: relative;
}

/* 中国风背景装饰 */
.header-bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(58, 91, 160, 0.02) 0%,
    rgba(95, 124, 110, 0.02) 50%,
    rgba(58, 91, 160, 0.03) 100%
  );
  z-index: -1;
  overflow: hidden;
}

/* 祥云图案层 */
.cloud-pattern-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg width='300' height='80' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M50,40 Q70,25 90,35 Q110,45 130,35 Q150,25 170,40 Q190,50 210,40 Q230,30 250,40' stroke='%233A5BA0' stroke-width='1.5' fill='none' opacity='0.08'/%3E%3Cpath d='M30,55 Q50,45 70,52 Q90,58 110,52 Q130,45 150,55' stroke='%235F7C6E' stroke-width='1' fill='none' opacity='0.06'/%3E%3C/svg%3E");
  background-repeat: repeat-x;
  background-position: 0 center;
  animation: cloudDrift 60s linear infinite;
  opacity: 0.6;
}

@keyframes cloudDrift {
  0% {
    background-position: 0 center;
  }
  100% {
    background-position: 300px center;
  }
}

/* 水波纹层 */
.water-ripple-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background-image: url("data:image/svg+xml,%3Csvg width='200' height='40' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0,20 Q50,10 100,20 T200,20 L200,40 L0,40 Z' fill='%233A5BA0' opacity='0.03'/%3E%3Cpath d='M0,25 Q50,20 100,25 T200,25' stroke='%233A5BA0' stroke-width='1' fill='none' opacity='0.06'/%3E%3C/svg%3E");
  background-repeat: repeat-x;
  background-size: 200px 40px;
  animation: waterFlow 25s linear infinite;
  opacity: 0.5;
}

@keyframes waterFlow {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 200px 0;
  }
}

.bg-particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  background: rgba(58, 91, 160, 0.1);
  border-radius: 50%;
  animation: particleFloat infinite linear;
}

/* 主容器 */
.header-container {
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 32px;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 1;
  flex-wrap: nowrap;
}

/* Logo区域 */
.logo-area {
  cursor: pointer;
  transition: all 0.4s ease;
  flex-shrink: 0;
}

.logo-area:hover {
  transform: translateY(-2px);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(58, 91, 160, 0.08);
  border: 1px solid rgba(58, 91, 160, 0.1);
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

.logo-wrapper::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.4) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.logo-area:hover .logo-wrapper {
  background: rgba(58, 91, 160, 0.15);
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.2);
}

.logo-area:hover .logo-wrapper::before {
  opacity: 1;
}

.logo-icon {
  position: relative;
  width: 45px;
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: 12px;
  color: white;
  font-size: 20px;
  animation: logoFloat 6s ease-in-out infinite;
}

.logo-accent {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: var(--gradient-gold);
  border-radius: 50%;
  animation: accentPulse 2s ease-in-out infinite;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  background: linear-gradient(135deg, var(--ink-darkest), var(--primary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 500;
  opacity: 0.8;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 导航区域 */
.nav-section {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.5);
  padding: 6px 8px;
  border-radius: 50px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}

.nav-item {
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  pointer-events: auto;
  z-index: 10;
  flex-shrink: 0;
}

.nav-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 25px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  pointer-events: auto;
  cursor: pointer;
  white-space: nowrap;
}

.nav-item:hover .nav-content {
  background: rgba(58, 91, 160, 0.1);
  transform: translateY(-2px);
}

.nav-item.active .nav-content {
  background: var(--gradient-primary);
  color: white;
  box-shadow: 0 4px 15px rgba(58, 91, 160, 0.3);
}

.nav-content i {
  font-size: 16px;
  transition: all 0.3s ease;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-item:hover .nav-content i,
.nav-item:hover .nav-text {
  color: var(--primary-color);
}

.nav-item.active .nav-content i,
.nav-item.active .nav-text {
  color: white;
}

.nav-indicator {
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.nav-item.active .nav-indicator {
  width: 60%;
}

/* 右侧功能区 */
.right-section {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* 搜索区域 */
.search-container {
  position: relative;
  width: 240px;
  flex-shrink: 0;
}

.search-box {
  position: relative;
  width: 100%;
}

.search-input {
  width: 100%;
  height: 42px;
  padding: 0 20px 0 50px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 25px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  font-size: 14px;
  transition: all 0.3s ease;
  outline: none;
}

.search-input:focus {
  border-color: var(--primary-color);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 4px rgba(58, 91, 160, 0.1);
}

.search-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
  font-size: 16px;
  transition: color 0.3s ease;
  z-index: 2;
}

.search-input:focus + .search-icon {
  color: var(--primary-color);
}

.search-effects {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  border-radius: 25px;
  overflow: hidden;
}

.search-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(58, 91, 160, 0.3), transparent);
  transition: left 0.6s ease;
}

.search-input:focus ~ .search-effects .search-glow {
  left: 100%;
}

/* 搜索建议 */
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-top: 8px;
  overflow: hidden;
  z-index: 1000;
}

.suggestions-header {
  padding: 12px 20px;
  background: rgba(58, 91, 160, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 500;
}

.suggestion-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
  font-size: 14px;
}

.suggestion-item:hover {
  background: rgba(58, 91, 160, 0.08);
  color: var(--primary-color);
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-quick, .favorite-quick, .service-quick {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.service-quick {
  background: rgba(58, 91, 160, 0.1);
  color: var(--primary-color);
}

.service-quick:hover {
  background: rgba(58, 91, 160, 0.2);
  transform: translateY(-2px);
}

.cart-quick {
  background: rgba(194, 58, 59, 0.1);
  color: var(--accent-color);
}

.cart-quick:hover {
  background: rgba(194, 58, 59, 0.2);
  transform: translateY(-2px);
}

.favorite-quick {
  background: rgba(194, 58, 59, 0.1);
  color: var(--accent-color);
}

.favorite-quick:hover {
  background: rgba(194, 58, 59, 0.2);
  transform: translateY(-2px);
}

.algorithm-quick {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  color: #667eea;
}

.algorithm-quick:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  transform: translateY(-2px);
}

.personalize-quick {
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.1), rgba(52, 152, 219, 0.1));
  color: var(--primary-color);
}

.personalize-quick:hover {
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.2), rgba(52, 152, 219, 0.2));
  transform: translateY(-2px);
}

.personalize-settings {
  padding: 10px 0;
}

.personalize-settings .settings-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: center;
}

.cart-icon {
  position: relative;
}

.cart-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--accent-color);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 600;
}

/* 用户操作区 */
.user-actions {
  display: flex;
  align-items: center;
}

/* 登录按钮 */
.login-btn {
  position: relative;
  height: 42px;
  padding: 0 24px;
  border-radius: 25px;
  border: none;
  background: var(--gradient-primary);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.3);
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-light));
}

.login-btn:active {
  transform: translateY(0);
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.6s ease;
}

.login-btn:hover .btn-glow {
  left: 100%;
}

/* 用户资料 */
.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-radius: 25px;
  background: rgba(58, 91, 160, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(58, 91, 160, 0.1);
}

.user-profile:hover {
  background: rgba(58, 91, 160, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(58, 91, 160, 0.2);
}

.avatar-wrapper {
  position: relative;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  overflow: hidden;
}

.user-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.online-indicator {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background: #52c41a;
  border-radius: 50%;
  border: 2px solid white;
  animation: onlinePulse 2s ease-in-out infinite;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.user-role {
  font-size: 12px;
  color: var(--primary-color);
  opacity: 0.8;
}

.dropdown-arrow {
  font-size: 12px;
  color: #909399;
  transition: transform 0.3s ease;
}

.user-profile:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* 用户菜单 */
:deep(.user-menu) {
  min-width: 200px;
  padding: 12px;
  border-radius: 16px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

:deep(.menu-item) {
  border-radius: 12px;
  margin: 4px 0;
  transition: all 0.2s ease;
  border: none;
}

:deep(.menu-item:hover) {
  background: rgba(58, 91, 160, 0.08);
}

:deep(.logout-item:hover) {
  background: rgba(255, 107, 107, 0.08);
}

.item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  width: 100%;
}

.item-content i:first-child {
  font-size: 16px;
  color: #606266;
  width: 20px;
}

.item-content span {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
  color: #2c3e50;
}

.item-arrow {
  font-size: 12px;
  color: #c0c4cc;
  opacity: 0;
  transition: all 0.2s ease;
}

:deep(.menu-item:hover) .item-content i:first-child {
  color: var(--primary-color);
}

:deep(.logout-item:hover) .item-content i:first-child {
  color: #ff6b6b;
}

:deep(.menu-item:hover) .item-arrow {
  opacity: 1;
  transform: translateX(4px);
}

.menu-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 8px 0;
}

/* 移动端导航按钮 */
.mobile-nav-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(58, 91, 160, 0.08);
  color: var(--primary-color);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(58, 91, 160, 0.1);
}

.mobile-nav-btn:hover {
  background: rgba(58, 91, 160, 0.15);
  transform: translateY(-2px);
}

.mobile-nav-btn i {
  font-size: 18px;
}

/* 移动端导航覆盖层 */
.mobile-nav-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

/* 移动端导航菜单 */
.mobile-nav-menu {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  animation: slideInRight 0.3s ease;
  overflow-y: auto;
}

.mobile-nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(58, 91, 160, 0.08);
}

.mobile-nav-header span {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.mobile-nav-header i {
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.mobile-nav-header i:hover {
  background: rgba(0, 0, 0, 0.06);
  color: var(--primary-color);
}

.mobile-nav-items {
  padding: 20px 0;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 4px solid transparent;
}

.mobile-nav-item:hover {
  background: rgba(58, 91, 160, 0.08);
  border-left-color: var(--primary-color);
}

.mobile-nav-item.active {
  background: rgba(58, 91, 160, 0.12);
  border-left-color: var(--primary-color);
  color: var(--primary-color);
}

.mobile-nav-item i:first-child {
  font-size: 18px;
  color: #606266;
  width: 24px;
  margin-right: 16px;
}

.mobile-nav-item.active i:first-child {
  color: var(--primary-color);
}

.mobile-nav-item span {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
}

.mobile-nav-item i:last-child {
  font-size: 14px;
  color: #c0c4cc;
  opacity: 0;
  transition: all 0.2s ease;
}

.mobile-nav-item:hover i:last-child {
  opacity: 1;
  transform: translateX(4px);
  color: var(--primary-color);
}

/* 移动端导航动画 */
@keyframes slideInRight {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 动画 */
@keyframes headerGradient {
  0%, 100% { 
    background-position: 0% 50%; 
  }
  50% { 
    background-position: 100% 50%; 
  }
}

@keyframes particleFloat {
  0% { 
    transform: translateY(100vh) rotate(0deg); 
    opacity: 0; 
  }
  10% { 
    opacity: 1; 
  }
  90% { 
    opacity: 1; 
  }
  100% { 
    transform: translateY(-100vh) rotate(360deg); 
    opacity: 0; 
  }
}

@keyframes logoFloat {
  0%, 100% { 
    transform: translateY(0px) rotate(0deg); 
  }
  50% { 
    transform: translateY(-3px) rotate(2deg); 
  }
}

@keyframes accentPulse {
  0%, 100% { 
    transform: scale(1); 
    opacity: 1; 
  }
  50% { 
    transform: scale(1.2); 
    opacity: 0.8; 
  }
}

@keyframes onlinePulse {
  0%, 100% { 
    transform: scale(1); 
    opacity: 1; 
  }
  50% { 
    transform: scale(1.1); 
    opacity: 0.8; 
  }
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-container {
    max-width: 1200px;
    padding: 0 24px;
    gap: 32px;
  }
  
  .search-container {
    width: 240px;
  }
}

@media (max-width: 1200px) {
  .header-container {
    padding: 0 20px;
    gap: 24px;
  }
  
  .brand-subtitle {
    display: none;
  }
  
  .search-container {
    width: 200px;
  }
  
  .nav-content {
    padding: 10px 16px;
  }
  
  .nav-text {
    font-size: 13px;
  }
}

@media (max-width: 992px) {
  .front-header {
    height: 70px;
  }
  
  .nav-section {
    display: none;
  }

  .mobile-nav-btn {
    display: flex;
  }
  
  .logo-text {
    display: none;
  }
  
  .search-container {
    width: 180px;
  }
  
  .user-details {
    display: none;
  }
  
  .login-btn span {
    display: none;
  }
  
  .login-btn {
    width: 42px;
    height: 42px;
    padding: 0;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
    gap: 16px;
  }
  
  .search-container {
    width: 150px;
  }
  
  .quick-actions {
    display: none;
  }
  
  .search-input {
    font-size: 13px;
    height: 38px;
  }
}

/* 可访问性支持 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .front-header {
    background: rgba(30, 30, 30, 0.95);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
  
  .logo-wrapper {
    background: rgba(103, 194, 58, 0.15);
  }
  
  .brand-name {
    color: #e0e0e0;
  }
  
  .nav-menu {
    background: rgba(40, 40, 40, 0.5);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .search-input {
    background: rgba(40, 40, 40, 0.8);
    border-color: rgba(255, 255, 255, 0.1);
    color: #e0e0e0;
  }
  
  .search-suggestions {
    background: rgba(40, 40, 40, 0.95);
  }
  
  .user-profile {
    background: rgba(103, 194, 58, 0.15);
  }
  
  .username {
    color: #e0e0e0;
  }
}
</style> 