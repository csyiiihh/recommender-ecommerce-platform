<template>
  <div class="team-detail">
    <!-- 页面头部 -->
    <el-page-header @back="$router.back()" content="拼团详情"></el-page-header>

    <div v-loading="loading" class="content-container">
      <el-empty v-if="!loading && !team" description="拼团不存在"></el-empty>

      <div v-else class="detail-content">
        <!-- 状态卡片 -->
        <el-card class="status-card" shadow="always">
          <div class="status-content">
            <!-- 拼团中 -->
            <div v-if="team.status === 0" class="status-ongoing">
              <div class="status-icon">
                <i class="fas fa-spinner fa-spin"></i>
              </div>
              <h2>拼团进行中</h2>
              <p class="status-desc">邀请好友参团，{{ team.requiredPeople }}人成团即可享受优惠！</p>
              
              <div class="countdown-box">
                <span class="label">剩余时间：</span>
                <count-down 
                  :end-time="team.expireTime"
                  format="long"
                  @finished="handleCountdownFinish">
                </count-down>
              </div>

              <div class="progress-box">
                <el-progress 
                  :percentage="progressPercent"
                  :color="progressColor"
                  :stroke-width="12"
                  :show-text="false">
                </el-progress>
                <div class="progress-text">
                  <span class="current">{{ team.currentPeople }}</span>
                  <span class="slash">/</span>
                  <span class="total">{{ team.requiredPeople }}人</span>
                  <span class="tip">（还差{{ team.requiredPeople - team.currentPeople }}人成团）</span>
                </div>
              </div>
            </div>

            <!-- 拼团成功 -->
            <div v-else-if="team.status === 1" class="status-success">
              <div class="status-icon success">
                <i class="fas fa-check-circle"></i>
              </div>
              <h2>拼团成功！</h2>
              <p class="status-desc">恭喜您，拼团已成功！商品将尽快为您发货。</p>
              
              <div class="success-info">
                <div class="info-item">
                  <span class="label">成团时间：</span>
                  <span class="value">{{ formatTime(team.successTime) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">参团人数：</span>
                  <span class="value">{{ team.currentPeople }}人</span>
                </div>
              </div>
            </div>

            <!-- 拼团失败 -->
            <div v-else-if="team.status === 2" class="status-failed">
              <div class="status-icon failed">
                <i class="fas fa-times-circle"></i>
              </div>
              <h2>拼团失败</h2>
              <p class="status-desc">{{ getFailReasonDesc(team.failReason) }}</p>
              
              <div class="failed-info">
                <div class="info-item">
                  <span class="label">失败原因：</span>
                  <span class="value">{{ getFailReasonText(team.failReason) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">失败时间：</span>
                  <span class="value">{{ formatTime(team.updatedAt) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">参团人数：</span>
                  <span class="value">{{ team.currentPeople }}/{{ team.requiredPeople }}人</span>
                </div>
              </div>

              <el-alert
                title="温馨提示"
                type="info"
                description="拼团失败的订单将自动退款，请留意您的支付账户。"
                :closable="false"
                show-icon
                style="margin-top: 20px;">
              </el-alert>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons" v-if="team.status === 0">
            <el-button 
              type="primary" 
              size="default"
              @click="shareTeam">
              <i class="fas fa-share-alt"></i>
              邀请好友参团
            </el-button>
            
            <el-button 
              size="default"
              @click="goToActivityDetail">
              <i class="fas fa-shopping-cart"></i>
              查看商品详情
            </el-button>
          </div>

          <div class="action-buttons" v-else-if="team.status === 1">
            <el-button 
              type="primary" 
              size="default"
              @click="viewOrder">
              <i class="fas fa-eye"></i>
              查看订单
            </el-button>
            
            <el-button 
              size="default"
              @click="goToActivityDetail">
              <i class="fas fa-redo"></i>
              再拼一单
            </el-button>
          </div>

          <div class="action-buttons" v-else-if="team.status === 2">
            <el-button 
              type="primary" 
              size="default"
              @click="goToActivityDetail">
              <i class="fas fa-redo"></i>
              重新开团
            </el-button>
          </div>
        </el-card>

        <!-- 商品信息卡片 -->
        <el-card class="product-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>商品信息</span>
              <el-tag :type="getActivityStatusType(team.activity?.status)" size="small">
                {{ getActivityStatusText(team.activity?.status) }}
              </el-tag>
            </div>
          </template>

          <el-row :gutter="20">
            <el-col :xs="24" :sm="8" :md="6">
              <div class="product-image">
                <img 
                  :src="getImageUrl(team.activity?.product)" 
                  alt=""
                  @click="goToActivityDetail">
              </div>
            </el-col>

            <el-col :xs="24" :sm="16" :md="18">
              <div class="product-info">
                <h3 
                  class="product-title" 
                  @click="goToActivityDetail">
                  {{ team.activity?.activityName }}
                </h3>
                
                <div class="product-desc" v-if="team.activity?.activityDesc">
                  {{ team.activity?.activityDesc }}
                </div>

                <div class="price-info">
                  <div class="price-item">
                    <span class="label">拼团价：</span>
                    <span class="group-price">¥{{ team.activity?.groupPrice }}</span>
                  </div>
                  <div class="price-item">
                    <span class="label">原价：</span>
                    <span class="original-price">¥{{ team.activity?.originalPrice }}</span>
                  </div>
                  <div class="price-item">
                    <span class="label">已售：</span>
                    <span class="sales">{{ team.activity?.salesCount || 0 }}件</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 团员信息卡片 -->
        <el-card class="members-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>团员信息</span>
              <span class="member-count">{{ team.currentPeople }}/{{ team.requiredPeople }}人</span>
            </div>
          </template>

          <div class="members-list">
            <div 
              v-for="(member, index) in team.members" 
              :key="member.id"
              class="member-item">
              <div class="member-avatar">
                <el-avatar 
                  :size="50"
                  :src="getUserAvatar(member.user)"
                  icon="fas fa-user">
                </el-avatar>
                <el-tag 
                  v-if="member.isLeader === 1" 
                  type="warning" 
                  size="small"
                  class="leader-tag">
                  团长
                </el-tag>
              </div>

              <div class="member-info">
                <div class="member-name">
                  {{ member.user?.username || '用户' + member.userId }}
                </div>
                <div class="member-time">
                  {{ formatTime(member.joinTime) }}
                </div>
              </div>

              <div class="member-status">
                <el-tag 
                  :type="member.status === 1 ? 'success' : 'info'" 
                  size="small">
                  {{ member.status === 1 ? '已支付' : '待支付' }}
                </el-tag>
              </div>
            </div>

            <!-- 空位占位符 -->
            <div 
              v-for="i in (team.requiredPeople - team.currentPeople)" 
              :key="'empty-' + i"
              class="member-item empty">
              <div class="member-avatar">
                <el-avatar 
                  :size="50"
                  icon="fas fa-user">
                </el-avatar>
              </div>
              <div class="member-info">
                <div class="member-name empty-text">等待参团</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 拼团规则 -->
        <el-card class="rules-card" shadow="never">
          <template #header>
            <div class="card-header">
              <i class="fas fa-exclamation-triangle"></i> 拼团规则
            </div>
          </template>

          <div class="rules-content">
            <ol class="rules-list">
              <li>参与拼团需在活动时间内完成支付；</li>
              <li>{{ team.requiredPeople }}人成团，每人限购1件；</li>
              <li>拼团时间为{{ team.activity?.validityHours || 24 }}小时，超时未成团将自动退款；</li>
              <li>成团后不支持退款，可联系客服处理售后；</li>
              <li>如有疑问，请联系在线客服。</li>
            </ol>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 分享对话框 -->
    <el-dialog 
      title="邀请好友参团" 
      v-model="shareDialogVisible"
      width="500px"
      center>
      <div class="share-content">
        <div class="share-info">
          <p class="share-desc">
            <i class="fas fa-info-circle"></i>
            分享此链接给好友，一起拼团更优惠！
          </p>
          
          <div class="share-link-box">
            <el-input 
              v-model="shareLink" 
              readonly
              ref="shareInput"
              class="share-input">
              <template #append>
                <el-button 
                  @click="copyLink"
                  type="primary">
                  <el-icon><DocumentCopy /></el-icon>
                  复制链接
                </el-button>
              </template>
            </el-input>
          </div>

          <div class="share-tips">
            <p><i class="fas fa-star"></i> 好友通过链接参团即可立即拼团成功</p>
            <p><i class="fas fa-star"></i> 拼团成功后商品将统一发货</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import CountDown from '@/components/CountDown.vue'
import { DocumentCopy } from '@element-plus/icons-vue'

export default {
  name: 'GroupBuyTeamDetail',
  components: {
    CountDown,
    DocumentCopy
  },
  data() {
    return {
      loading: false,
      team: null,
      shareDialogVisible: false,
      shareLink: ''
    }
  },
  computed: {
    progressPercent() {
      if (!this.team) return 0
      return (this.team.currentPeople / this.team.requiredPeople * 100)
    },
    progressColor() {
      const percent = this.progressPercent
      if (percent >= 80) return '#e6a23c'
      return '#409eff'
    }
  },
  mounted() {
    this.loadTeamDetail()
  },
  methods: {
    async loadTeamDetail() {
      const teamId = this.$route.params.id
      if (!teamId) {
        this.$message.error('团ID无效')
        this.$router.back()
        return
      }

      this.loading = true
      try {
        const res = await Request.get(`/group-buy/team/${teamId}`)
        
        if (res.code === '200' || res.code === '0') {
          this.team = res.data
          logger.debug('拼团详情:', this.team)
        } else {
          this.$message.error(res.msg || '加载失败')
          setTimeout(() => this.$router.back(), 1500)
        }
      } catch (error) {
        logger.error('加载拼团详情失败:', error)
        this.$message.error('加载失败，请稍后重试')
        setTimeout(() => this.$router.back(), 1500)
      } finally {
        this.loading = false
      }
    },

    handleCountdownFinish() {
      this.$message.warning('拼团时间已结束')
      setTimeout(() => this.loadTeamDetail(), 1000)
    },

    shareTeam() {
      const baseUrl = window.location.origin
      this.shareLink = `${baseUrl}/#/group-buy/${this.team.activityId}?teamId=${this.team.id}`
      this.shareDialogVisible = true
    },

    copyLink() {
      this.$refs.shareInput.select()
      try {
        document.execCommand('copy')
        this.$message.success('链接已复制到剪贴板')
        this.shareDialogVisible = false
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      }
    },

    viewOrder() {
      // 查找当前用户的订单
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      const member = this.team.members?.find(m => m.userId === user.id)
      
      if (member && member.orderId) {
        this.$router.push(`/order`)
      } else {
        this.$message.warning('未找到订单信息')
      }
    },

    goToActivityDetail() {
      if (this.team?.activityId) {
        this.$router.push(`/group-buy/${this.team.activityId}`)
      }
    },

    getActivityStatusText(status) {
      const texts = {
        0: '未发布',
        1: '进行中',
        2: '已结束'
      }
      return texts[status] || '未知'
    },

    getActivityStatusType(status) {
      const types = {
        0: 'info',
        1: 'success',
        2: 'warning'
      }
      return types[status] || 'info'
    },

    getImageUrl(product) {
      if (!product || !product.imageUrl) {
        return require('@/assets/logo.png')
      }
      if (product.imageUrl.startsWith('http')) {
        return product.imageUrl
      }
      return `/api${product.imageUrl}`
    },

    getUserAvatar(user) {
      if (!user || !user.avatar) {
        return ''
      }
      if (user.avatar.startsWith('http')) {
        return user.avatar
      }
      return `http://localhost:1234${user.avatar}`
    },

    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const Y = date.getFullYear()
      const M = (date.getMonth() + 1).toString().padStart(2, '0')
      const D = date.getDate().toString().padStart(2, '0')
      const h = date.getHours().toString().padStart(2, '0')
      const m = date.getMinutes().toString().padStart(2, '0')
      const s = date.getSeconds().toString().padStart(2, '0')
      return `${Y}-${M}-${D} ${h}:${m}:${s}`
    },
    
    getFailReasonText(reason) {
      const reasonMap = {
        'timeout': '超时未成团',
        'leader_refund': '团长退款',
        'member_refund': '成员退款',
        'cancelled': '主动取消',
        'cancelled_offline': '活动已下架',
        'cancelled_ended': '活动已结束'
      }
      return reasonMap[reason] || '未知原因'
    },
    
    getFailReasonDesc(reason) {
      const descMap = {
        'timeout': '很遗憾，本次拼团未能在规定时间内成团。',
        'leader_refund': '团长已申请退款，拼团自动失败。',
        'member_refund': '成员退款导致人数不足，拼团自动失败，已为您办理退款。',
        'cancelled': '拼团已被取消。',
        'cancelled_offline': '管理员已将活动下架，拼团自动失败，已为您办理退款。',
        'cancelled_ended': '管理员已结束该活动，拼团自动失败，已为您办理退款。'
      }
      return descMap[reason] || '很遗憾，本次拼团未能成功。'
    }
  }
}
</script>

<style scoped>
.team-detail {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
  background: #f5f7fa;
  min-height: 100vh;
}

.content-container {
  margin-top: 20px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 状态卡片 */
.status-card {
  border-radius: 12px;
  overflow: hidden;
}

.status-content {
  text-align: center;
  padding: 30px 20px;
}

.status-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.status-ongoing .status-icon {
  color: #409eff;
  animation: rotate 2s linear infinite;
}

.status-success .status-icon {
  color: #67c23a;
}

.status-failed .status-icon {
  color: #f56c6c;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-content h2 {
  font-size: 28px;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.status-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 25px 0;
}

.countdown-box {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 30px;
  color: white;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 25px;
}

.countdown-box .label {
  opacity: 0.9;
}

.progress-box {
  max-width: 600px;
  margin: 0 auto;
}

.progress-text {
  margin-top: 12px;
  font-size: 16px;
  color: #333;
}

.progress-text .current {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.progress-text .slash {
  margin: 0 4px;
  color: #999;
}

.progress-text .total {
  font-size: 18px;
  font-weight: 500;
}

.progress-text .tip {
  margin-left: 10px;
  font-size: 14px;
  color: #e6a23c;
}

.success-info,
.failed-info {
  max-width: 500px;
  margin: 20px auto 0;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #e4e7ed;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #909399;
  font-size: 14px;
}

.info-item .value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 30px;
  padding-top: 25px;
  border-top: 1px solid #ebeef5;
}

/* 商品信息卡片 */
.product-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.product-image {
  width: 100%;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-image:hover img {
  transform: scale(1.1);
}

.product-info {
  padding: 10px 0;
}

.product-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  cursor: pointer;
  line-height: 1.5;
}

.product-title:hover {
  color: #409eff;
}

.product-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 15px;
}

.price-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.price-item {
  display: flex;
  align-items: baseline;
}

.price-item .label {
  font-size: 13px;
  color: #999;
  margin-right: 5px;
}

.group-price {
  font-size: 24px;
  color: #ff4d4f;
  font-weight: bold;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.sales {
  font-size: 14px;
  color: #666;
}

/* 团员信息卡片 */
.members-card {
  border-radius: 12px;
}

.member-count {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.members-list {
  display: grid;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.member-item:not(.empty):hover {
  background: #e9ecef;
  transform: translateX(5px);
}

.member-item.empty {
  opacity: 0.5;
}

.member-avatar {
  position: relative;
  flex-shrink: 0;
}

.leader-tag {
  position: absolute;
  bottom: -5px;
  right: -5px;
  border: 2px solid white;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.member-name.empty-text {
  color: #999;
}

.member-time {
  font-size: 13px;
  color: #999;
}

.member-status {
  flex-shrink: 0;
}

/* 拼团规则 */
.rules-card {
  border-radius: 12px;
  background: #fff9e6;
  border: 1px solid #ffe58f;
}

.rules-card >>> .el-card__header {
  background: #fff9e6;
  border-bottom-color: #ffe58f;
}

.rules-content {
  padding: 10px 0;
}

.rules-list {
  margin: 0;
  padding-left: 20px;
  line-height: 2;
  color: #666;
}

.rules-list li {
  font-size: 14px;
}

/* 分享对话框 */
.share-content {
  padding: 10px;
}

.share-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-desc i {
  font-size: 18px;
  color: #409eff;
}

.share-link-box {
  margin-bottom: 20px;
}

.share-input {
  font-size: 14px;
}

.share-tips {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 15px;
}

.share-tips p {
  font-size: 13px;
  color: #666;
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-tips i {
  color: #ffa726;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .team-detail {
    padding: 15px;
  }

  .status-content {
    padding: 20px 15px;
  }

  .status-content h2 {
    font-size: 22px;
  }

  .status-icon {
    font-size: 60px;
  }

  .countdown-box {
    font-size: 14px;
    padding: 10px 18px;
  }

  .progress-text .current {
    font-size: 20px;
  }

  .progress-text .total {
    font-size: 16px;
  }

  .progress-text .tip {
    display: block;
    margin: 5px 0 0 0;
  }

  .action-buttons {
    flex-direction: column;
    gap: 10px;
  }

  .action-buttons .el-button {
    width: 100%;
  }

  .product-image {
    height: 150px;
  }

  .price-info {
    flex-direction: column;
    gap: 10px;
  }

  .member-item {
    padding: 12px;
  }

  .rules-list {
    font-size: 13px;
  }
}
</style>

