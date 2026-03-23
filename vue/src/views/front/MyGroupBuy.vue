<template>
  <div class="my-group-buy">
    <!-- 页面头部 -->
    <el-page-header @back="$router.back()" content="我的拼团"></el-page-header>

    <div class="content-container">
      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="拼团中" name="0">
          <template #label>
            <span>
              <i class="fas fa-spinner fa-spin"></i> 拼团中
              <el-badge :value="statusCounts[0]" v-if="statusCounts[0] > 0" class="badge"></el-badge>
            </span>
          </template>
        </el-tab-pane>
        
        <el-tab-pane label="拼团成功" name="1">
          <template #label>
            <span>
              <i class="fas fa-check-circle"></i> 拼团成功
              <el-badge :value="statusCounts[1]" v-if="statusCounts[1] > 0" class="badge"></el-badge>
            </span>
          </template>
        </el-tab-pane>
        
        <el-tab-pane label="拼团失败" name="2">
          <template #label>
            <span>
              <i class="fas fa-times-circle"></i> 拼团失败
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 列表内容 -->
      <div v-loading="loading" class="list-container">
        <el-empty v-if="!loading && teams.length === 0" :description="emptyText"></el-empty>

        <div v-else class="team-cards">
          <el-card 
            v-for="team in teams" 
            :key="team.id"
            class="team-card"
            shadow="hover">
            
            <!-- 卡片头部 -->
            <template #header>
              <div class="card-header">
                <div class="left-info">
                <el-tag 
                  :type="getStatusTagType(team.status)" 
                  size="small" 
                  effect="dark">
                  {{ getStatusText(team.status) }}
                </el-tag>
                <span class="team-id">团号：{{ team.id }}</span>
              </div>
              
              <count-down 
                v-if="team.status === 0"
                :end-time="team.expireTime"
                format="short">
              </count-down>
              
              <span v-else class="time-text">
                {{ formatTime(team.status === 1 ? team.successTime : team.updatedAt) }}
              </span>
              </div>
            </template>

            <!-- 卡片内容 -->
            <el-row :gutter="20">
              <!-- 商品图片 -->
              <el-col :xs="8" :sm="6">
                <div class="product-image">
                  <img 
                    :src="getImageUrl(team.activity?.product)" 
                    alt=""
                    @click="goToDetail(team.activity?.id)">
                </div>
              </el-col>

              <!-- 商品信息 -->
              <el-col :xs="16" :sm="18">
                <div class="product-info">
                  <h4 
                    class="product-title" 
                    @click="goToDetail(team.activity?.id)">
                    {{ team.activity?.activityName }}
                  </h4>
                  
                  <div class="price-info">
                    <span class="price">¥{{ team.activity?.groupPrice }}</span>
                    <span class="original-price">¥{{ team.activity?.originalPrice }}</span>
                  </div>

                  <!-- 拼团进度 -->
                  <div class="team-progress">
                    <el-progress 
                      :percentage="(team.currentPeople / team.requiredPeople * 100)"
                      :color="getProgressColor(team)"
                      :stroke-width="6"
                      :show-text="false">
                    </el-progress>
                    <span class="progress-text">
                      {{ team.currentPeople }}/{{ team.requiredPeople }}人
                      <span v-if="team.status === 0 && team.currentPeople < team.requiredPeople">
                        （还差{{ team.requiredPeople - team.currentPeople }}人）
                      </span>
                    </span>
                  </div>

                  <!-- 团长标识 -->
                  <div class="role-tag" v-if="isLeader(team)">
                    <el-tag type="warning" size="small">我是团长</el-tag>
                  </div>
                </div>
              </el-col>
            </el-row>

            <!-- 卡片底部操作 -->
            <div class="card-footer">
              <div class="members-preview" v-if="team.members && team.members.length > 0">
                <el-avatar 
                  v-for="member in team.members.slice(0, 5)" 
                  :key="member.id"
                  :size="28"
                  :src="getUserAvatar(member.user)"
                  icon="fas fa-user">
                </el-avatar>
                <span class="members-more" v-if="team.members.length > 5">
                  +{{ team.members.length - 5 }}
                </span>
              </div>

              <div class="actions">
                <el-button 
                  v-if="team.status === 0"
                  type="primary" 
                  size="small"
                  plain
                  @click="shareTeam(team)">
                  <i class="fas fa-share-alt"></i> 邀请好友
                </el-button>
                
                <el-button 
                  size="small"
                  @click="viewTeamDetail(team.id)">
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 分享对话框 -->
    <el-dialog 
      title="邀请好友参团" 
      v-model="shareDialogVisible"
      width="500px">
      <div class="share-content">
        <div class="share-info">
          <p>分享此链接给好友，一起拼团更优惠！</p>
          <el-input 
            v-model="shareLink" 
            readonly
            ref="shareInput">
            <template #append>
              <el-button 
                @click="copyLink">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 可选：二维码 -->
        <div class="qrcode-container" v-if="false">
          <p>扫描二维码参团</p>
          <!-- 这里可以集成qrcode.js生成二维码 -->
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import CountDown from '@/components/CountDown.vue'

export default {
  name: 'MyGroupBuy',
  components: {
    CountDown,
    DocumentCopy
  },
  data() {
    return {
      loading: false,
      activeTab: '0',
      teams: [],
      statusCounts: {
        0: 0, // 拼团中
        1: 0, // 成功
        2: 0  // 失败
      },
      
      // 分享
      shareDialogVisible: false,
      shareLink: '',
      currentShareTeam: null
    }
  },
  computed: {
    emptyText() {
      const texts = {
        '0': '暂无进行中的拼团',
        '1': '暂无成功的拼团',
        '2': '暂无失败的拼团'
      }
      return texts[this.activeTab] || '暂无数据'
    }
  },
  mounted() {
    this.loadUserTeams()
    this.loadStatusCounts()
  },
  methods: {
    async loadUserTeams() {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      if (!user.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }

      this.loading = true
      try {
        const res = await Request.get('/group-buy/my-teams', {
          params: {
            userId: user.id,
            status: parseInt(this.activeTab)
          }
        })
        
        if (res.code === '200' || res.code === '0') {
          this.teams = res.data || []
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      } catch (error) {
        logger.error('加载我的拼团失败:', error)
        this.$message.error('加载失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    async loadStatusCounts() {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      if (!user.id) return

      try {
        // 分别获取三种状态的数量
        const promises = [0, 1, 2].map(status => 
          Request.get('/group-buy/my-teams', {
            params: { userId: user.id, status }
          })
        )
        
        const results = await Promise.all(promises)
        results.forEach((res, index) => {
          if (res.code === '200' || res.code === '0') {
            this.statusCounts[index] = (res.data || []).length
          }
        })
      } catch (error) {
        logger.error('加载状态统计失败:', error)
      }
    },
    
    handleTabClick() {
      this.loadUserTeams()
    },
    
    isLeader(team) {
      const user = JSON.parse(localStorage.getItem('frontUser') || '{}')
      return team.leaderUserId === user.id
    },
    
    getStatusText(status) {
      const texts = {
        0: '拼团中',
        1: '拼团成功',
        2: '拼团失败',
        3: '已取消'
      }
      return texts[status] || '未知'
    },
    
    getStatusTagType(status) {
      const types = {
        0: 'warning',
        1: 'success',
        2: 'danger',
        3: 'info'
      }
      return types[status] || 'info'
    },
    
    getProgressColor(team) {
      if (team.status === 1) return '#67c23a'
      if (team.status === 2) return '#f56c6c'
      const percent = team.currentPeople / team.requiredPeople * 100
      if (percent >= 80) return '#e6a23c'
      return '#409eff'
    },
    
    shareTeam(team) {
      this.currentShareTeam = team
      const baseUrl = window.location.origin
      this.shareLink = `${baseUrl}/#/group-buy/${team.activityId}?teamId=${team.id}`
      this.shareDialogVisible = true
    },
    
    copyLink() {
      this.$refs.shareInput.select()
      try {
        document.execCommand('copy')
        this.$message.success('链接已复制到剪贴板')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      }
    },
    
    viewTeamDetail(teamId) {
      // 可以创建一个团详情页面，或者跳转到活动详情页
      this.$router.push(`/group-buy-team/${teamId}`)
    },
    
    goToDetail(activityId) {
      if (activityId) {
        this.$router.push(`/group-buy/${activityId}`)
      }
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
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    }
  }
}
</script>

<style scoped>
.my-group-buy {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.content-container {
  margin-top: 20px;
}

.badge {
  margin-left: 5px;
}

.list-container {
  min-height: 400px;
  padding: 20px 0;
}

.team-cards {
  display: grid;
  gap: 20px;
}

.team-card {
  transition: all 0.3s;
}

.team-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.team-id {
  font-size: 13px;
  color: #999;
}

.time-text {
  font-size: 13px;
  color: #999;
}

.product-image {
  width: 100%;
  height: 120px;
  border-radius: 6px;
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
  padding: 5px 0;
}

.product-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 10px 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-title:hover {
  color: #409eff;
}

.price-info {
  margin-bottom: 12px;
}

.price {
  font-size: 20px;
  color: #ff4d4f;
  font-weight: bold;
  margin-right: 10px;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.team-progress {
  margin-bottom: 10px;
}

.progress-text {
  font-size: 13px;
  color: #666;
  margin-top: 5px;
  display: block;
}

.role-tag {
  margin-top: 8px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  margin-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.members-preview {
  display: flex;
  align-items: center;
  gap: 5px;
}

.members-more {
  font-size: 12px;
  color: #999;
  margin-left: 5px;
}

.actions {
  display: flex;
  gap: 10px;
}

/* 分享对话框 */
.share-content {
  padding: 10px;
}

.share-info {
  margin-bottom: 20px;
}

.share-info p {
  margin-bottom: 15px;
  color: #666;
}

.qrcode-container {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .my-group-buy {
    padding: 15px;
  }
  
  .product-image {
    height: 100px;
  }
  
  .product-title {
    font-size: 14px;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .actions {
    width: 100%;
  }
  
  .actions .el-button {
    flex: 1;
  }
}
</style>

