<template>
  <div class="auth-container">
    <div class="modern-background">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
      <div class="floating-elements">
        <!-- 将漂浮元素改为与图书相关的 -->
        <!-- 已隐藏装饰元素 -->
      </div>
    </div>

    <div class="auth-content">
      <!-- 中国风装饰元素 - 保留，适合图书文化主题 -->
      <div class="chinese-decorative-elements">
        <div class="auspicious-cloud cloud-1"></div>
        <div class="auspicious-cloud cloud-2"></div>
        <div class="ink-bamboo bamboo-1"></div>
        <div class="ink-bamboo bamboo-2"></div>
        <div class="plum-blossom blossom-1"></div>
        <div class="plum-blossom blossom-2"></div>
        <div class="plum-blossom blossom-3"></div>
        <div class="wave-pattern wave-1"></div>
        <div class="wave-pattern wave-2"></div>
      </div>

      <div class="auth-layout">
        <!-- 左侧介绍区域 -->
        <div class="intro-section">
          <div class="intro-content">
            <div class="brand-area">
              <div class="brand-header">
                <div class="brand-logo">
                  <div class="logo-circle">
                    <i class="fas fa-book-open"></i>
                  </div>
                </div>
                <h1 class="brand-title">在线销售平台</h1>
              </div>
            </div>

            <div class="features-list">
              <div class="feature-item">
                <div class="feature-icon">
                  <i class="fas fa-star"></i>
                </div>
                <div class="feature-content">
                  <h3>内容精选</h3>
                  <p>从海量出版物中严选经典与新锐，每本书都代表品质与价值</p>
                </div>
              </div>

              <div class="feature-item">
                <div class="feature-icon">
                  <i class="fas fa-shipping-fast"></i>
                </div>
                <div class="feature-content">
                  <h3>快速送达</h3>
                  <p>智能仓储覆盖全国核心城市，新书速递与日常订单高效送达</p>
                </div>
              </div>

              <div class="feature-item">
                <div class="feature-icon">
                  <i class="fas fa-certificate"></i>
                </div>
                <div class="feature-content">
                  <h3>正版保证</h3>
                  <p>与权威出版社直接合作，确保每一页文字都获得正版授权</p>
                </div>
              </div>

              <div class="feature-item">
                <div class="feature-icon">
                  <i class="fas fa-user-graduate"></i>
                </div>
                <div class="feature-content">
                  <h3>阅读管家</h3>
                  <p>根据您的阅读喜好提供个性化书单与导读，陪伴全程阅读旅程</p>
                </div>
              </div>
            </div>

            <div class="stats-section">
              <div class="stat-item">
                <div class="stat-number">10000+</div>
                <div class="stat-label">阅读爱好者</div>
              </div>
              <div class="stat-item">
                <div class="stat-number">500+</div>
                <div class="stat-label">精选书单</div>
              </div>
              <div class="stat-item">
                <div class="stat-number">99%</div>
                <div class="stat-label">满意度</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧表单区域 -->
        <div class="form-section">
          <div class="form-container">
            <div class="form-card">
              <router-view />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
    name: 'LRLayout'
}
</script>

<style scoped>
.auth-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.modern-background {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 0;
}

/* 中国风背景渐变 - 宣纸与水墨 */
.bg-gradient {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, 
        #F7F9FA 0%,
        #EEF3F6 20%,
        #E8EFF8 40%,
        #DDE7F5 60%,
        #EEF3F6 80%,
        #F7F9FA 100%);
    animation: gradientShift 30s ease-in-out infinite;
}

@keyframes gradientShift {
    0%, 100% {
        background: linear-gradient(135deg, 
            #F7F9FA 0%,
            #EEF3F6 20%,
            #E8EFF8 40%,
            #DDE7F5 60%,
            #EEF3F6 80%,
            #F7F9FA 100%);
    }
    33% {
        background: linear-gradient(135deg, 
            #EDF2FA 0%,
            #E0EAF2 20%,
            #D4E0F0 40%,
            #C8D6E8 60%,
            #E0EAF2 80%,
            #EDF2FA 100%);
    }
    66% {
        background: linear-gradient(135deg, 
            #F5F8FA 0%,
            #E8EFF8 20%,
            #DDE7F5 40%,
            #D4E0F0 60%,
            #E8EFF8 80%,
            #F5F8FA 100%);
    }
}

/* 中国风纹样 - 青花瓷回纹 */
.bg-pattern {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    opacity: 0.06;
    background-image: 
        radial-gradient(circle at 25% 25%, #3A5BA0 2px, transparent 2px),
        radial-gradient(circle at 75% 75%, #5B7BBF 1.5px, transparent 1.5px),
        radial-gradient(circle at 50% 100%, #5F7C6E 1.5px, transparent 1.5px),
        radial-gradient(ellipse at 20% 60%, #3A5BA0 1px, transparent 1px),
        radial-gradient(ellipse at 80% 40%, #8DA0D9 1px, transparent 1px);
    background-size: 100px 100px, 80px 80px, 90px 90px, 60px 60px, 70px 70px;
    background-position: 0 0, 30px 30px, 20px 20px, 45px 45px, 55px 15px;
    animation: patternMove 40s linear infinite;
}

@keyframes patternMove {
    0% {
        transform: translate(0, 0);
    }
    100% {
        transform: translate(60px, 60px);
    }
}

.floating-elements {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
}

/* 中国风浮动圆圈 - 水墨渲染 */
.circle {
    position: absolute;
    border-radius: 50%;
    background: linear-gradient(135deg, rgba(58, 91, 160, 0.08), rgba(58, 91, 160, 0.03));
    animation: float 7s ease-in-out infinite;
    filter: blur(20px);
}

.circle-1 {
    width: 140px;
    height: 140px;
    top: 12%;
    left: 6%;
    animation-delay: 0s;
}

.circle-2 {
    width: 100px;
    height: 100px;
    top: 68%;
    right: 12%;
    background: linear-gradient(135deg, rgba(95, 124, 110, 0.08), rgba(95, 124, 110, 0.03));
    animation-delay: 2.5s;
}

.circle-3 {
    width: 80px;
    height: 80px;
    top: 35%;
    right: 22%;
    background: linear-gradient(135deg, rgba(141, 160, 217, 0.08), rgba(141, 160, 217, 0.03));
    animation-delay: 5s;
}

/* 中国风梅花花瓣装饰 */
.leaf {
    position: absolute;
    width: 16px;
    height: 16px;
    background: radial-gradient(circle, rgba(194, 58, 59, 0.15) 0%, rgba(194, 58, 59, 0.05) 70%);
    border-radius: 50% 0 50% 0;
    transform: rotate(0deg);
    animation: leafFloat 10s ease-in-out infinite;
    box-shadow: 0 2px 8px rgba(194, 58, 59, 0.1);
}

.leaf::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 4px;
    height: 4px;
    background: rgba(201, 160, 99, 0.3);
    border-radius: 50%;
}

/* 图书相关漂浮元素样式示例 - 已隐藏 */
.book-1, .book-2, .book-3 {
  display: none;
}

.quill-1, .quill-2 {
  display: none;
}

.parchment-1 {
  display: none;
}

.leaf-1 {
    top: 22%;
    left: 78%;
    animation-delay: 1.5s;
}

.leaf-2 {
    top: 58%;
    left: 18%;
    animation-delay: 4s;
}

.leaf-3 {
    top: 78%;
    left: 68%;
    animation-delay: 7s;
}

/* 中国风装饰元素 */
.chinese-decorative-elements {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 0;
}

/* 祥云装饰 */
.auspicious-cloud {
    position: absolute;
    width: 60px;
    height: 20px;
    background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    border-radius: 50%;
    filter: blur(6px);
    animation: cloudDrift 20s ease-in-out infinite;
}

.auspicious-cloud::before,
.auspicious-cloud::after {
    content: '';
    position: absolute;
    background: radial-gradient(ellipse, rgba(255, 255, 255, 0.12) 0%, transparent 70%);
    border-radius: 50%;
    filter: blur(5px);
}

.auspicious-cloud::before {
    width: 40px;
    height: 16px;
    top: -6px;
    left: 10px;
}

.auspicious-cloud::after {
    width: 45px;
    height: 18px;
    top: -4px;
    right: 8px;
}

.cloud-1 {
    top: 18%;
    left: 10%;
    animation-delay: 0s;
}

.cloud-2 {
    top: 70%;
    right: 15%;
    animation-delay: 5s;
}

@keyframes cloudDrift {
    0%, 100% { 
        transform: translateX(0) translateY(0);
        opacity: 0.4;
    }
    50% { 
        transform: translateX(15px) translateY(-8px);
        opacity: 0.7;
}
}

/* 水墨竹子 */
.ink-bamboo {
    position: absolute;
    width: 3px;
    height: 50px;
    background: linear-gradient(180deg, 
        rgba(58, 91, 160, 0.15) 0%, 
        rgba(58, 91, 160, 0.08) 50%,
        transparent 100%);
    border-radius: 2px;
    animation: bambooSway 15s ease-in-out infinite;
}

.ink-bamboo::before {
    content: '';
    position: absolute;
    width: 20px;
    height: 3px;
    background: linear-gradient(90deg, rgba(95, 124, 110, 0.12) 0%, transparent 100%);
    border-radius: 2px;
    top: 15px;
    left: 3px;
}

.ink-bamboo::after {
    content: '';
    position: absolute;
    width: 18px;
    height: 3px;
    background: linear-gradient(90deg, rgba(95, 124, 110, 0.1) 0%, transparent 100%);
    border-radius: 2px;
    top: 30px;
    left: 3px;
}

.bamboo-1 {
    top: 25%;
    left: 15%;
    animation-delay: 2s;
}

.bamboo-2 {
    top: 60%;
    right: 20%;
    animation-delay: 7s;
}

@keyframes bambooSway {
    0%, 100% { transform: rotate(0deg); }
    25% { transform: rotate(3deg); }
    75% { transform: rotate(-3deg); }
}

/* 梅花装饰 */
.plum-blossom {
    position: absolute;
    width: 12px;
    height: 12px;
    animation: blossomFloat 12s ease-in-out infinite;
}

.plum-blossom::before,
.plum-blossom::after {
    content: '';
    position: absolute;
    width: 6px;
    height: 6px;
    background: radial-gradient(circle, rgba(194, 58, 59, 0.2) 0%, rgba(194, 58, 59, 0.08) 70%);
    border-radius: 50% 0 50% 0;
}

.plum-blossom::before {
    top: 0;
    left: 0;
    transform: rotate(0deg);
}

.plum-blossom::after {
    top: 3px;
    left: 3px;
    transform: rotate(45deg);
}

.blossom-1 {
    top: 35%;
    left: 25%;
    animation-delay: 1s;
}

.blossom-2 {
    top: 55%;
    right: 28%;
    animation-delay: 5s;
}

.blossom-3 {
    top: 80%;
    left: 30%;
    animation-delay: 9s;
}

@keyframes blossomFloat {
    0%, 100% { 
        transform: translateY(0) rotate(0deg);
        opacity: 0.5;
    }
    50% { 
        transform: translateY(-12px) rotate(180deg);
        opacity: 0.8;
    }
}

/* 水纹装饰 */
.wave-pattern {
    position: absolute;
    width: 40px;
    height: 12px;
    border-radius: 50%;
    border: 2px solid rgba(58, 91, 160, 0.1);
    animation: waveExpand 8s ease-in-out infinite;
}

.wave-pattern::before {
    content: '';
    position: absolute;
    inset: -6px;
    border-radius: 50%;
    border: 1px solid rgba(58, 91, 160, 0.06);
}

.wave-1 {
    top: 45%;
    left: 20%;
    animation-delay: 0s;
}

.wave-2 {
    bottom: 30%;
    right: 25%;
    animation-delay: 4s;
}

@keyframes waveExpand {
    0%, 100% { 
        transform: scale(1);
        opacity: 0.3;
    }
    50% { 
        transform: scale(1.3);
        opacity: 0.6;
    }
}

@keyframes float {
    0%, 100% {
        transform: translateY(0px) scale(1);
        opacity: 0.7;
    }
    50% {
        transform: translateY(-20px) scale(1.1);
        opacity: 0.9;
    }
}

@keyframes leafFloat {
    0%, 100% {
        transform: rotate(45deg) translateY(0px);
        opacity: 0.6;
    }
    25% {
        transform: rotate(50deg) translateY(-10px);
        opacity: 0.8;
    }
    75% {
        transform: rotate(40deg) translateY(-5px);
        opacity: 0.7;
    }
}

/* 新的左右分栏布局 */
.auth-content {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 32px;
    position: relative;
    z-index: 1;
    min-height: 100vh;
}

.auth-layout {
    display: flex;
    width: 100%;
    max-width: 1400px;
    min-height: auto;
    border-radius: 20px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(226, 232, 240, 0.6);
    box-shadow: 
        0 12px 40px rgba(0, 0, 0, 0.08),
        0 4px 16px rgba(0, 0, 0, 0.04),
        inset 0 1px 0 rgba(255, 255, 255, 0.6);
    transition: all 0.3s ease;
}

.auth-layout:hover {
    transform: translateY(-2px);
    box-shadow: 
        0 16px 48px rgba(0, 0, 0, 0.1),
        0 8px 24px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* 左侧介绍区域 - 中国风青花蓝渐变 */
.intro-section {
    flex: 0 0 55%;
    background: linear-gradient(135deg, #3A5BA0 0%, #5B7BBF 50%, #8DA0D9 100%);
    color: white;
    padding: 40px 32px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: relative;
    overflow: hidden;
}

/* 青花瓷纹理背景 */
.intro-section::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: 
        radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.08) 0%, transparent 40%),
        radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.06) 0%, transparent 35%),
        radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.04) 0%, transparent 50%);
    opacity: 1;
}

/* 中国风回纹装饰 */
.intro-section::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url("data:image/svg+xml,%3Csvg width='80' height='80' viewBox='0 0 80 80' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M40 36a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm0-12a4 4 0 1 0 0 8 4 4 0 0 0 0-8z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
    opacity: 0.4;
    animation: patternRotate 60s linear infinite;
}

@keyframes patternRotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.intro-content {
    position: relative;
    z-index: 1;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.brand-area {
    margin-bottom: 40px;
    margin-top: -60px;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.brand-header {
    display: flex;
    align-items: center;
    gap: 14px;
    width: 100%;
}

.brand-logo {
    display: flex;
    justify-content: flex-start;
    flex-shrink: 0;
}

/* 中国风Logo圆圈 */
.logo-circle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 50px;
    height: 50px;
    background: rgba(255, 255, 255, 0.25);
    border-radius: 50%;
    backdrop-filter: blur(15px);
    border: 2px solid rgba(255, 255, 255, 0.4);
    animation: logoFloat 5s ease-in-out infinite;
    box-shadow: 
        0 8px 32px rgba(0, 0, 0, 0.15),
        inset 0 2px 8px rgba(255, 255, 255, 0.2);
    position: relative;
}

/* Logo外围装饰 */
.logo-circle::before {
    content: '';
    position: absolute;
    inset: -8px;
    border-radius: 50%;
    border: 2px dashed rgba(255, 255, 255, 0.25);
    animation: logoRotate 30s linear infinite;
}

@keyframes logoRotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.logo-circle i {
    font-size: 24px;
    color: white;
    filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.25));
}

@keyframes logoFloat {
    0%, 100% { 
        transform: translateY(0px) scale(1); 
    }
    50% { 
        transform: translateY(-10px) scale(1.05); 
    }
}

/* 中国风品牌标题 */
.brand-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0;
    letter-spacing: 2px;
    line-height: 1.3;
    text-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
    font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
    color: white;
}

.features-list {
    display: flex;
    flex-direction: column;
    gap: 18px;
    margin-bottom: 32px;
    width: 100%;
}

/* 中国风特色卡片 - 横向布局 */
.feature-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 14px;
    background: rgba(255, 255, 255, 0.12);
    border-radius: 10px;
    backdrop-filter: blur(15px);
    border: 2px solid rgba(255, 255, 255, 0.25);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

/* 卡片水墨渲染效果 */
.feature-item::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 50% 0%, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.4s ease;
}

.feature-item:hover::before {
    opacity: 1;
}

.feature-item:hover {
    transform: translateY(-4px);
    background: rgba(255, 255, 255, 0.18);
    border-color: rgba(255, 255, 255, 0.35);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

/* 中国风图标容器 */
.feature-icon {
    width: 44px;
    height: 44px;
    background: rgba(255, 255, 255, 0.25);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border: 2px solid rgba(255, 255, 255, 0.3);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
}

.feature-icon::before {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 1px dashed rgba(255, 255, 255, 0.2);
    animation: iconRotate 20s linear infinite;
}

@keyframes iconRotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.feature-item:hover .feature-icon {
    background: rgba(255, 255, 255, 0.35);
    transform: scale(1.1) rotateY(10deg);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.feature-icon i {
    font-size: 20px;
    color: white;
    filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.2));
    transition: transform 0.3s ease;
}

.feature-item:hover .feature-icon i {
    transform: scale(1.1);
}

.feature-content {
    flex: 1;
    min-width: 0;
}

.feature-content h3 {
    font-size: 15px;
    font-weight: 600;
    margin: 0 0 6px 0;
    color: white;
    letter-spacing: 0.5px;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    line-height: 1.4;
}

.feature-content p {
    font-size: 12px;
    opacity: 0.92;
    margin: 0;
    line-height: 1.6;
    letter-spacing: 0.3px;
    color: white;
}

/* 中国风统计数据区 */
.stats-section {
    display: flex;
    justify-content: space-around;
    text-align: center;
    padding: 18px 14px;
    margin-top: 60px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    backdrop-filter: blur(10px);
    border: 2px solid rgba(255, 255, 255, 0.15);
    width: 100%;
    max-width: 100%;
}

.stat-item {
    padding: 0 20px;
    position: relative;
    transition: transform 0.3s ease;
}

.stat-item:hover {
    transform: translateY(-4px);
}

.stat-item:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 2px;
    height: 36px;
    background: linear-gradient(180deg, 
        transparent 0%, 
        rgba(255, 255, 255, 0.3) 50%, 
        transparent 100%);
}

.stat-number {
    font-size: 24px;
    font-weight: 700;
    color: white;
    margin-bottom: 6px;
    text-shadow: 0 3px 12px rgba(0, 0, 0, 0.2);
    font-family: 'Georgia', serif;
    letter-spacing: 1px;
}

.stat-label {
    font-size: 12px;
    opacity: 0.95;
    color: white;
    font-weight: 500;
    letter-spacing: 0.5px;
}

/* 右侧表单区域 */
.form-section {
    flex: 0 0 45%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 32px;
    background: linear-gradient(135deg, 
        rgba(255, 255, 255, 0.98) 0%, 
        rgba(255, 255, 255, 0.97) 30%,
        rgba(255, 255, 255, 0.96) 60%,
        rgba(255, 255, 255, 0.95) 100%);
    overflow: hidden;
    box-sizing: border-box;
    position: relative;
}

.form-section::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 1px;
    background: linear-gradient(to bottom, 
        transparent 0%, 
        rgba(58, 91, 160, 0.08) 10%, 
        rgba(58, 91, 160, 0.15) 30%,
        rgba(58, 91, 160, 0.18) 50%,
        rgba(58, 91, 160, 0.15) 70%,
        rgba(58, 91, 160, 0.08) 90%, 
        transparent 100%);
    z-index: 1;
    opacity: 0.5;
}

.form-section::after {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 80px;
    background: linear-gradient(to right, 
        rgba(58, 91, 160, 0.04) 0%, 
        rgba(58, 91, 160, 0.02) 40%,
        rgba(58, 91, 160, 0.01) 70%,
        transparent 100%);
    z-index: 0;
    pointer-events: none;
}

.form-container {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    margin-left: 0;
}

.form-card {
    width: 100%;
    box-sizing: border-box;
}

/* 响应式设计 */
@media (max-width: 1024px) {
    .auth-layout {
        flex-direction: column;
        max-width: 500px;
        min-height: auto;
    }
    
    .intro-section {
        flex: none;
        padding: 40px 30px;
    }
    
    .brand-title {
        font-size: 28px;
    }
    
    .features-list {
        gap: 16px;
        margin-bottom: 30px;
    }
    
    .form-section {
        flex: none;
        padding: 40px 30px;
    }
}

@media (max-width: 768px) {
    .auth-content {
        padding: 20px;
    }
    
    .auth-layout {
        border-radius: 20px;
        min-height: auto;
    }
    
    .intro-section {
        padding: 30px 20px;
    }
    
    .brand-area {
        margin-bottom: 30px;
    }
    
    .logo-circle {
        width: 60px;
        height: 60px;
    }
    
    .logo-circle i {
        font-size: 28px;
    }
    
    .brand-title {
        font-size: 24px;
    }
    
    .brand-subtitle {
        font-size: 16px;
    }
    
    .features-grid {
        margin-bottom: 30px;
    }
    
    .form-section {
        padding: 30px 20px;
    }
    
    .circle-1 {
        width: 80px;
        height: 80px;
    }
    
    .circle-2 {
        width: 60px;
        height: 60px;
    }
    
    .circle-3 {
        width: 40px;
        height: 40px;
    }
    
    .leaf {
        width: 14px;
        height: 14px;
    }
    
    /* 中国风元素移动端优化 */
    .auspicious-cloud {
        width: 50px;
        height: 18px;
    }
    
    .ink-bamboo {
        width: 2px;
        height: 40px;
    }
    
    .plum-blossom {
        width: 10px;
        height: 10px;
    }
    
    .wave-pattern {
        width: 35px;
        height: 10px;
    }
}

@media (max-width: 480px) {
    .auth-content {
        padding: 16px;
    }
    
    .intro-section {
        padding: 20px 16px;
    }
    
    .form-section {
        padding: 20px 16px;
    }
    
    .stats-section {
        flex-direction: column;
        gap: 20px;
    }
    
    .stat-item {
        padding: 0;
    }
}

/* 优化深色模式 */
@media (prefers-color-scheme: dark) {
    .bg-gradient {
        background: linear-gradient(135deg, 
            #0f172a 0%,
            #1e293b 25%,
            #334155 50%,
            #475569 75%,
            #64748b 100%);
    }
    
    .auth-layout {
        background: rgba(30, 41, 59, 0.95);
        border: 1px solid rgba(255, 255, 255, 0.1);
    }
    
    .form-section {
        background: rgba(30, 41, 59, 0.98);
    }
}

/* 减少动画以提高性能 */
@media (prefers-reduced-motion: reduce) {
    .bg-gradient,
    .bg-pattern,
    .circle,
    .leaf,
    .logo-circle,
    .wheat,
    .carrot,
    .apple,
    .corn,
    .tomato,
    .rice-grain {
        animation: none;
    }
    
    .auth-layout,
    .feature-card {
        transition: none;
    }
}
</style>