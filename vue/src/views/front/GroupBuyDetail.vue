<template>
  <div class="group-buy-detail landscape-bg">
    <!-- 导航栏 -->
    <front-header></front-header>
    
    <!-- 主要内容 -->
    <div class="main-content porcelain-texture">
      <!-- 面包屑导航 -->
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/group-buy' }">限时拼团</el-breadcrumb-item>
        <el-breadcrumb-item>{{ activity.activityName || '拼团详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-loading="loading" class="detail-container">
      <!-- 活动信息卡片 -->
      <el-card class="activity-info-card" shadow="never">
        <el-row :gutter="30">
          <!-- 商品图片 -->
          <el-col :xs="24" :sm="24" :md="10">
            <div class="product-image">
              <img :src="getImageUrl(activity.product)" alt="">
            </div>
          </el-col>

          <!-- 活动详情 -->
          <el-col :xs="24" :sm="24" :md="14">
            <div class="activity-info">
              <h2 class="activity-title">{{ activity.activityName }}</h2>
              
              <!-- 价格 -->
              <div class="price-section">
                <div class="group-price-row">
                  <span class="label">拼团价</span>
                  <span class="price">¥{{ activity.groupPrice }}</span>
                  <el-tag size="small" type="danger" effect="dark">
                    省¥{{ (activity.originalPrice - activity.groupPrice).toFixed(2) }}
                  </el-tag>
                </div>
                <div class="original-price">
                  原价：¥{{ activity.originalPrice }}
                </div>
              </div>

              <!-- 拼团规则 -->
              <div class="rule-section">
                <el-row :gutter="15">
                  <el-col :span="12">
                    <div class="rule-item">
                      <i class="fas fa-users"></i>
                      <span>{{ activity.requiredMembers }}人成团</span>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="rule-item">
                      <i class="fas fa-clock"></i>
                      <span>{{ activity.validityHours }}小时内成团</span>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="rule-item">
                      <i class="fas fa-box"></i>
                      <span>剩余{{ activity.stock }}件</span>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="rule-item">
                      <i class="fas fa-shopping-cart"></i>
                      <span>已拼{{ activity.salesCount }}件</span>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="rule-item">
                      <i class="fas fa-exclamation-triangle"></i>
                      <span>每人限购1件</span>
                    </div>
                  </el-col>
                </el-row>
              </div>

              <!-- 商品描述 -->
              <div class="description" v-if="activity.activityDesc">
                <p>{{ activity.activityDesc }}</p>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <el-button 
                  type="danger" 
                  size="large" 
                  @click="handleStartTeam"
                  :disabled="!activity.stock || activity.stock <= 0">
                  <i class="fas fa-gift"></i>
                  我要开团
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 正在拼团列表 -->
      <el-card class="team-list-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span><i class="fas fa-users"></i> 正在拼团（{{ activeTeams.length }}）</span>
            <el-button link @click="loadActiveTeams" :loading="teamsLoading">
              <i class="fas fa-sync-alt"></i> 刷新
            </el-button>
          </div>
        </template>

        <el-empty v-if="!teamsLoading && activeTeams.length === 0" description="暂无正在进行的拼团"></el-empty>

        <div v-else class="team-list">
          <div 
            v-for="team in activeTeams" 
            :key="team.id" 
            class="team-item"
            :class="{ 'almost-full': team.currentPeople >= team.requiredPeople - 1 }">
            
            <div class="team-header">
              <div class="leader-info">
                <el-avatar 
                  :size="40" 
                  :src="getUserAvatar(team.leader)"
                  icon="fas fa-user">
                </el-avatar>
                <div class="leader-name">
                  <span>{{ team.leader ? team.leader.username : '未知用户' }}</span>
                  <el-tag size="small" type="warning">团长</el-tag>
                </div>
              </div>
              
              <!-- 根据团队状态显示不同内容 -->
              <!-- status=0: 拼团中，显示倒计时 -->
              <!-- status=1: 拼团成功，显示成功标签 -->
              <div v-if="team.status === 1" class="team-success">
                <el-tag type="success" size="default">
                  <i class="fas fa-check-circle"></i> 拼团成功
                </el-tag>
              </div>
              <count-down 
                v-else
                :end-time="team.expireTime" 
                format="short"
                @end="onTeamExpire(team.id)">
              </count-down>
            </div>

            <div class="team-body">
              <div class="progress-section">
                <el-progress 
                  :percentage="(team.currentPeople / team.requiredPeople * 100)"
                  :color="getProgressColor(team)"
                  :stroke-width="8">
                </el-progress>
                <div class="progress-text">
                  <span class="current">{{ team.currentPeople }}</span>
                  /
                  <span class="required">{{ team.requiredPeople }}</span>人
                  <span class="tip" v-if="team.status === 1">
                    <el-tag type="success" size="small">已成团</el-tag>
                  </span>
                  <span class="tip" v-else-if="team.currentPeople < team.requiredPeople">
                    还差<strong>{{ team.requiredPeople - team.currentPeople }}</strong>人成团
                  </span>
                </div>
              </div>

              <!-- 成员头像列表 -->
              <div class="members-avatars" v-if="team.members && team.members.length > 0">
                <el-avatar 
                  v-for="member in team.members" 
                  :key="member.id"
                  :size="32"
                  :src="getUserAvatar(member.user)"
                  icon="fas fa-user">
                </el-avatar>
                <span class="members-count" v-if="team.members.length > 5">
                  +{{ team.members.length - 5 }}
                </span>
              </div>

              <el-button 
                type="danger" 
                size="small" 
                plain
                @click="handleJoinTeam(team.id)"
                :disabled="team.currentPeople >= team.requiredPeople">
                立即参团
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 地址选择对话框 -->
    <el-dialog 
      title="选择收货地址和支付方式" 
      v-model="addressDialogVisible"
      width="600px"
      :close-on-click-modal="false"
      custom-class="address-dialog">
      
      <div v-loading="addressLoading" class="address-dialog-body">
        <el-empty v-if="addresses.length === 0" description="暂无收货地址">
          <el-button type="primary" size="small" @click="goToAddressManage">
            去添加地址
          </el-button>
        </el-empty>

        <div v-else>
          <!-- 地址选择 -->
          <div class="section-title">
            <i class="fas fa-map-marker-alt"></i>
            收货地址
          </div>
          <div class="address-list">
            <div 
              v-for="address in addresses" 
              :key="address.id" 
              class="address-card"
              :class="{ 'selected': selectedAddressId === address.id }"
              @click="selectedAddressId = address.id">
              <div class="radio-icon">
                <i class="fas fa-check-circle" v-if="selectedAddressId === address.id"></i>
                <i class="far fa-circle" v-else></i>
              </div>
              <div class="address-info">
                <div class="address-header">
                  <span class="receiver">{{ address.receiver }}</span>
                  <span class="phone">{{ address.phone }}</span>
                </div>
                <div class="address-detail">{{ address.address }}</div>
              </div>
            </div>
          </div>
          
          <!-- 支付方式选择 -->
          <div class="section-title" style="margin-top: 20px;">
            <i class="fas fa-wallet"></i>
            支付方式
          </div>
          <div class="payment-methods">
            <div 
              class="payment-card"
              :class="{ 'selected': paymentMethod === 'balance' }"
              @click="paymentMethod = 'balance'">
              <div class="radio-icon">
                <i class="fas fa-check-circle" v-if="paymentMethod === 'balance'"></i>
                <i class="far fa-circle" v-else></i>
              </div>
              <div class="payment-info">
                <i class="fas fa-wallet payment-icon balance-icon"></i>
                <div class="payment-name">余额支付</div>
                <div class="payment-desc">快速便捷，一键完成</div>
              </div>
            </div>
            <div 
              class="payment-card"
              :class="{ 'selected': paymentMethod === 'alipay' }"
              @click="paymentMethod = 'alipay'">
              <div class="radio-icon">
                <i class="fas fa-check-circle" v-if="paymentMethod === 'alipay'"></i>
                <i class="far fa-circle" v-else></i>
              </div>
              <div class="payment-info">
                <i class="fab fa-alipay payment-icon alipay-icon"></i>
                <div class="payment-name">支付宝支付</div>
                <div class="payment-desc">安全可靠，支持多种方式</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="confirmAddress"
            :disabled="!selectedAddressId">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    </div><!-- main-content -->
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import CountDown from '@/components/CountDown.vue'
import FrontHeader from '@/components/front/FrontHeader.vue'
import axios from 'axios'

export default {
  name: 'GroupBuyDetail',
  components: {
    CountDown,
    FrontHeader
  },
  data() {
    return {
      activityId: null,
      loading: false,
      teamsLoading: false,
      addressLoading: false,
      activity: {
        product: {}
      },
      activeTeams: [],
      
      // 地址相关
      addressDialogVisible: false,
      addresses: [],
      selectedAddressId: null,
      
      // 支付方式
      paymentMethod: 'balance', // 'balance' 或 'alipay'，默认余额支付
      
      // 当前操作
      currentAction: '', // 'start' 或 'join'
      currentTeamId: null
    }
  },
  mounted() {
    this.activityId = this.$route.params.id
    if (this.activityId) {
      this.loadActivityDetail()
      this.loadActiveTeams()
    }
  },
  methods: {
    async loadActivityDetail() {
      this.loading = true
      try {
        const res = await Request.get(`/group-buy/activity/${this.activityId}`)
        
        logger.debug('活动详情API响应:', res)
        
        if (res.code === '200' || res.code === '0') {
          this.activity = res.data
          logger.debug('活动数据已加载:', this.activity)
          logger.debug('库存值:', this.activity.stock)
        } else {
          logger.error('API返回错误:', res)
          this.$message.error(res.msg || '加载活动详情失败')
        }
      } catch (error) {
        logger.error('加载活动详情失败:', error)
        this.$message.error('加载失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    async loadActiveTeams() {
      this.teamsLoading = true
      try {
        const res = await Request.get('/group-buy/teams', {
          params: { activityId: this.activityId }
        })
        
        if (res.code === '200' || res.code === '0') {
          this.activeTeams = res.data || []
        }
      } catch (error) {
        logger.error('加载拼团列表失败:', error)
      } finally {
        this.teamsLoading = false
      }
    },
    
    async loadAddresses() {
      this.addressLoading = true
      try {
        const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
        if (!user.id) {
          this.$message.warning('请先登录')
          this.$router.push('/login')
          return
        }
        
        const res = await Request.get(`/address/user/${user.id}`)
        
        if (res.code === '200' || res.code === '0') {
          this.addresses = res.data || []
          // 默认选中第一个地址
          if (this.addresses.length > 0) {
            this.selectedAddressId = this.addresses[0].id
          }
        }
      } catch (error) {
        logger.error('加载地址失败:', error)
        this.$message.error('加载地址失败')
      } finally {
        this.addressLoading = false
      }
    },
    
    handleStartTeam() {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      if (!user.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      
      this.currentAction = 'start'
      this.currentTeamId = null
      this.addressDialogVisible = true
      this.loadAddresses()
    },
    
    handleJoinTeam(teamId) {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      if (!user.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      
      this.currentAction = 'join'
      this.currentTeamId = teamId
      this.addressDialogVisible = true
      this.loadAddresses()
    },
    
    async confirmAddress() {
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }
      
      this.addressDialogVisible = false
      
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      
      if (this.currentAction === 'start') {
        if (this.paymentMethod === 'balance') {
          await this.createTeamAndPayByBalance(user.id)
        } else {
          await this.createTeamAndPay(user.id)
        }
      } else if (this.currentAction === 'join') {
        if (this.paymentMethod === 'balance') {
          await this.joinTeamAndPayByBalance(user.id)
        } else {
          await this.joinTeamAndPay(user.id)
        }
      }
    },
    
    // 开团 - 余额支付
    async createTeamAndPayByBalance(userId) {
      const loading = this.$loading({
        lock: true,
        text: '正在创建拼团订单...',
        spinner: 'fas fa-spinner fa-spin'
      })
      
      try {
        // 1. 创建拼团订单
        const res = await Request.post('/group-buy/start', {
          activityId: this.activityId,
          userId: userId,
          addressId: this.selectedAddressId,
          quantity: 1
        })
        
        if (res.code === '200' || res.code === '0') {
          const orderId = res.data.orderId
          
          // 2. 调用余额支付
          const payRes = await Request.post(`/order/${orderId}/pay-balance`)
          loading.close()
          
          if (payRes.code === '0') {
            this.$message.success('开团成功！支付完成')
            
            // 刷新拼团列表
            await this.loadActiveTeams()
            
            // 跳转到我的拼团页面
            setTimeout(() => {
              this.$router.push('/my-group-buy')
            }, 1500)
          } else {
            this.$message.error(payRes.msg || '支付失败')
          }
        } else {
          loading.close()
          this.$message.error(res.msg || '创建订单失败')
        }
      } catch (error) {
        loading.close()
        logger.error('开团失败:', error)
        const msg = error.response?.data?.msg || error.message || '开团失败，请稍后重试'
        
        // 特殊处理重复开团的提示
        if (msg.includes('已经在该活动中开团') || msg.includes('请等待拼团结束') || msg.includes('不能重复开团')) {
          this.$message({
            type: 'warning',
            message: msg,
            duration: 3000,
            showClose: true
          })
        } else {
          this.$message.error(msg)
        }
      }
    },
    
    // 开团 - 支付宝支付
    async createTeamAndPay(userId) {
      const loading = this.$loading({
        lock: true,
        text: '正在创建拼团订单...',
        spinner: 'fas fa-spinner fa-spin'
      })
      
      try {
        // 1. 创建拼团订单
        const res = await Request.post('/group-buy/start', {
          activityId: this.activityId,
          userId: userId,
          addressId: this.selectedAddressId,
          quantity: 1
        })
        
        if (res.code === '200' || res.code === '0') {
          const orderId = res.data.orderId
          
          // 2. 调用支付宝支付
          await this.callAlipay(orderId)
        } else {
          this.$message.error(res.msg || '创建订单失败')
        }
      } catch (error) {
        logger.error('开团失败:', error)
        const msg = error.response?.data?.msg || error.message || '开团失败，请稍后重试'
        
        // 特殊处理重复开团的提示
        if (msg.includes('已经在该活动中开团') || msg.includes('请等待拼团结束') || msg.includes('不能重复开团')) {
          this.$message({
            type: 'warning',
            message: msg,
            duration: 3000,
            showClose: true
          })
        } else {
          this.$message.error(msg)
        }
      } finally {
        loading.close()
      }
    },
    
    // 参团 - 余额支付
    async joinTeamAndPayByBalance(userId) {
      const loading = this.$loading({
        lock: true,
        text: '正在加入拼团...',
        spinner: 'fas fa-spinner fa-spin'
      })
      
      try {
        // 1. 参与拼团
        const res = await Request.post('/group-buy/join', {
          teamId: this.currentTeamId,
          userId: userId,
          addressId: this.selectedAddressId,
          quantity: 1
        })
        
        if (res.code === '200' || res.code === '0') {
          const orderId = res.data.orderId
          
          // 2. 调用余额支付
          const payRes = await Request.post(`/order/${orderId}/pay-balance`)
          loading.close()
          
          if (payRes.code === '0') {
            this.$message.success('参团成功！支付完成')
            
            // 刷新拼团列表
            await this.loadActiveTeams()
            
            // 跳转到我的拼团页面
            setTimeout(() => {
              this.$router.push('/my-group-buy')
            }, 1500)
          } else {
            this.$message.error(payRes.msg || '支付失败')
          }
        } else {
          loading.close()
          this.$message.error(res.msg || '参团失败')
        }
      } catch (error) {
        loading.close()
        logger.error('参团失败:', error)
        this.$message.error(error.response?.data?.msg || '参团失败，请稍后重试')
      }
    },
    
    // 参团 - 支付宝支付
    async joinTeamAndPay(userId) {
      const loading = this.$loading({
        lock: true,
        text: '正在加入拼团...',
        spinner: 'fas fa-spinner fa-spin'
      })
      
      try {
        // 1. 参与拼团
        const res = await Request.post('/group-buy/join', {
          teamId: this.currentTeamId,
          userId: userId,
          addressId: this.selectedAddressId,
          quantity: 1
        })
        
        if (res.code === '200' || res.code === '0') {
          const orderId = res.data.orderId
          
          // 2. 调用支付宝支付
          await this.callAlipay(orderId)
        } else {
          this.$message.error(res.msg || '参团失败')
        }
      } catch (error) {
        logger.error('参团失败:', error)
        this.$message.error(error.response?.data?.msg || '参团失败，请稍后重试')
      } finally {
        loading.close()
      }
    },
    
    async callAlipay(orderId) {
      try {
        const orderName = `拼团-${this.activity.title}`
        const payPrice = this.activity.groupPrice.toFixed(2)
        
        // 调用支付宝接口
        const response = await axios({
          method: 'post',
          url: 'http://localhost:1234/alipay/create',
          params: {
            orderNo: orderId,
            orderName: orderName,
            payPrice: payPrice
          },
          responseType: 'text'
        })
        
        // 在新窗口打开支付宝支付页面
        const newWindow = window.open('', '_blank')
        newWindow.document.write(response.data)
        newWindow.document.close()
        
        // 提示用户
        this.$message.success('订单创建成功，请在新窗口完成支付')
        
        // 刷新拼团列表
        setTimeout(() => {
          this.loadActiveTeams()
        }, 2000)
        
      } catch (error) {
        logger.error('调用支付失败:', error)
        this.$message.error('支付失败，请稍后重试')
      }
    },
    
    onTeamExpire(teamId) {
      logger.debug('团已过期:', teamId)
      // 刷新列表
      this.loadActiveTeams()
    },
    
    goToAddressManage() {
      this.$router.push('/address')
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
    
    getProgressColor(team) {
      const percent = team.currentPeople / team.requiredPeople * 100
      if (percent >= 100) return '#67c23a'
      if (percent >= 80) return '#e6a23c'
      return '#409eff'
    }
  }
}
</script>

<style scoped>
.group-buy-detail {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f0fdf4 0%, #ffffff 100%);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.breadcrumb {
  margin-bottom: 25px;
  padding: 12px 0;
  font-size: 14px;
}

.detail-container {
  margin-top: 20px;
}

/* 活动信息卡片 */
.activity-info-card {
  margin-bottom: 20px;
}

.product-image {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-info {
  padding: 10px 0;
}

.activity-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
  line-height: 1.4;
}

.price-section {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.group-price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.group-price-row .label {
  font-size: 14px;
  color: var(--accent-color);
  font-weight: 500;
}

.group-price-row .price {
  font-size: 36px;
  color: var(--accent-color);
  font-weight: bold;
}

.original-price {
  font-size: 15px;
  color: #999;
  text-decoration: line-through;
}

.rule-section {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 15px;
  color: #666;
}

.rule-item i {
  font-size: 18px;
  color: #409eff;
}

.description {
  padding: 15px 0;
  color: #666;
  line-height: 1.8;
}

.action-buttons {
  margin-top: 30px;
}

.action-buttons .el-button {
  width: 300px;
  font-size: 16px;
  font-weight: 500;
}

/* 拼团列表卡片 */
.team-list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}

.card-header i {
  margin-right: 5px;
}

.team-list {
  display: grid;
  gap: 15px;
}

.team-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.team-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.team-item.almost-full {
  border-color: var(--accent-color);
  background: rgba(194, 58, 59, 0.05);
}

.team-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.leader-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.leader-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
}

.team-success {
  display: flex;
  align-items: center;
}

.team-success .el-tag {
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 500;
}

.team-success i.fa-check-circle,
.team-success i.fa-check {
  margin-right: 4px;
  font-size: 16px;
}

.team-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-section {
  flex: 1;
}

.progress-text {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.progress-text .current {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.progress-text .required {
  font-size: 16px;
  font-weight: 500;
}

.progress-text .tip {
  margin-left: 10px;
  color: var(--accent-color);
}

.progress-text strong {
  font-size: 16px;
}

.members-avatars {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.members-count {
  font-size: 13px;
  color: #999;
  margin-left: 5px;
}

/* 地址选择对话框 */
.address-dialog-body {
  max-height: 500px;
  overflow-y: auto;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-card {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.address-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.address-card.selected {
  border-color: #409eff;
  background: #f0f9ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.radio-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  margin-top: 2px;
}

.radio-icon i {
  font-size: 20px;
  color: #409eff;
}

.address-card:not(.selected) .radio-icon i {
  color: #dcdfe6;
}

.address-info {
  flex: 1;
  min-width: 0;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.receiver {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.phone {
  font-size: 14px;
  color: #606266;
}

.address-detail {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 支付方式选择 */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title i {
  font-size: 18px;
  color: #409eff;
}

.payment-methods {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.payment-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.payment-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.payment-card.selected {
  border-color: #409eff;
  background: #f0f9ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.payment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.payment-icon {
  font-size: 32px;
  margin-bottom: 4px;
}

.payment-icon.balance-icon {
  color: #67c23a;
}

.payment-icon.alipay-icon {
  color: #1677ff;
}

.payment-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.payment-desc {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .group-buy-detail {
    padding: 15px;
  }
  
  .product-image {
    height: 280px;
  }
  
  .activity-title {
    font-size: 20px;
  }
  
  .group-price-row .price {
    font-size: 28px;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
}
</style>

