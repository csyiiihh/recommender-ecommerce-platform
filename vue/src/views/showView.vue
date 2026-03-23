<template>
  <div class="dashboard-wrapper">
    <!-- 数据概览卡片 -->
    <div class="stat-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="fas fa-shopping-bag stat-icon"></i>
          <div class="stat-title">本月订单</div>
        </div>
        <div class="stat-value">
          <count-to :startVal="0" :endVal="orderStats.currentMonthOrders" :duration="2000"></count-to>
        </div>
        <div class="stat-footer">
          较上月
          <span :class="orderTrend >= 0 ? 'up' : 'down'">
            {{ orderStats.growthRate }}
            <i :class="orderTrend >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          </span>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="fas fa-money-bill-wave stat-icon"></i>
          <div class="stat-title">本月销售额</div>
        </div>
        <div class="stat-value">
          ¥<count-to :startVal="0" :endVal="salesStats.currentMonthSales" :duration="2000" :decimals="2" :key="'sales-' + salesStats.currentMonthSales">
          </count-to>
        </div>
        <div class="stat-footer">
          较上月
          <span :class="saleTrend >= 0 ? 'up' : 'down'">
            {{ salesStats.growthRate }}
            <i :class="saleTrend >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          </span>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="fas fa-users stat-icon"></i>
          <div class="stat-title">今年用户数</div>
        </div>
        <div class="stat-value">
          <count-to :startVal="0" :endVal="userStats.currentYearUsers" :duration="2000" :key="'user-' + userStats.currentYearUsers">
          </count-to>
        </div>
        <div class="stat-footer">
          较去年
          <span :class="userTrend >= 0 ? 'up' : 'down'">
            {{ userStats.growthRate }}
            <i :class="userTrend >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          </span>
        </div>
      </el-card>
    </div>

    <div class="content-wrapper">
      <!-- 热销商品TOP5图表 -->
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-header">
            <span>热销商品 TOP5</span>
            <el-button link @click="fetchTopProducts">刷新</el-button>
          </div>
        </template>
        <div class="chart-content">
          <div ref="topProductsChart" class="chart"></div>
        </div>
      </el-card>

      <!-- 品类销售占比图表 -->
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-header">
            <span>品类销售占比</span>
            <el-button link @click="fetchCategoryStats">刷新</el-button>
          </div>
        </template>
        <div class="chart-content">
          <div ref="categoryChart" class="chart"></div>
        </div>
      </el-card>
    </div>

    <!-- 通知公告 -->
    <div class="content-wrapper">
      <el-card class="notice-card" shadow="hover">
        <template #header>
          <div class="notice-header">
            <span>通知公告</span>
            <el-button link @click="fetchData">刷新</el-button>
          </div>
        </template>
        <div class="notice-content">
          <el-timeline>
            <el-timeline-item v-for="(notice, index) in announcements" :key="index" :timestamp="notice.time" :type="getNoticeType(notice.type)">
              <el-card class="notice-item" shadow="never">
                <h4>{{ notice.title }}</h4>
                <p class="notice-text">{{ notice.content }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { h } from 'vue'
import Request from '../utils/request.js'
import logger from '@/utils/logger'
import * as echarts from 'echarts'

export default {
  name: 'ShowView',
  components: {
    CountTo: {
      props: {
        startVal: { type: Number, default: 0 },
        endVal: { type: Number, default: 0 },
        duration: { type: Number, default: 2000 },
        decimals: { type: Number, default: 0 }
      },
      data() {
        return {
          currentVal: this.startVal,
          animationFrameId: null,
          isDestroyed: false
        }
      },
      watch: {
        endVal: {
          handler(newVal, oldVal) {
            if (!this.isDestroyed) {
              const numVal = Number(newVal) || 0
              const numOldVal = Number(oldVal) || 0
              // 如果值发生变化，重新动画
              if (numVal !== this.currentVal && numVal !== numOldVal) {
                this.animate()
              } else if (numVal === this.currentVal && numVal !== numOldVal) {
                // 值已经等于当前值，直接设置
                this.currentVal = numVal
              }
            }
          },
          immediate: true
        }
      },
      mounted() {
        const numEndVal = Number(this.endVal) || 0
        this.currentVal = this.startVal
        // 即使 endVal 和 startVal 相同，也要显示值
        if (numEndVal === this.startVal) {
          this.currentVal = numEndVal
        } else if (numEndVal !== this.startVal) {
          this.animate()
        }
      },
      beforeUnmount() {
        this.isDestroyed = true
        if (this.animationFrameId !== null) {
          cancelAnimationFrame(this.animationFrameId)
          this.animationFrameId = null
        }
      },
      methods: {
        animate() {
          if (this.isDestroyed) {
            return
          }
          
          // 取消之前的动画
          if (this.animationFrameId !== null) {
            cancelAnimationFrame(this.animationFrameId)
          }
          
          const startTime = Date.now()
          const startVal = this.currentVal
          const endVal = Number(this.endVal) || 0
          const duration = this.duration
          
          // 如果 endVal 为 0 且 startVal 也为 0，直接设置值，不执行动画
          if (endVal === 0 && startVal === 0) {
            this.currentVal = 0
            this.animationFrameId = null
            return
          }
          
          const animate = () => {
            if (this.isDestroyed) {
              return
            }
            
            const now = Date.now()
            const progress = Math.min((now - startTime) / duration, 1)
            
            // 使用缓动函数
            const easeOutQuart = 1 - Math.pow(1 - progress, 4)
            this.currentVal = this.decimals > 0
              ? parseFloat((startVal + (endVal - startVal) * easeOutQuart).toFixed(this.decimals))
              : Math.floor(startVal + (endVal - startVal) * easeOutQuart)
            
            if (progress < 1 && !this.isDestroyed) {
              this.animationFrameId = requestAnimationFrame(animate)
            } else {
              this.currentVal = endVal
              this.animationFrameId = null
            }
          }
          
          this.animationFrameId = requestAnimationFrame(animate)
        }
      },
      render() {
        // 使用导入的 h 函数
        const displayValue = this.decimals > 0 
          ? this.currentVal.toFixed(this.decimals)
          : String(this.currentVal)
        return h('span', displayValue)
      },
      computed: {
        displayValue() {
          return this.decimals > 0 
            ? this.currentVal.toFixed(this.decimals)
            : this.currentVal
        }
      }
    }
  },
  data() {
    return {
      noticeLimit: 10,
      announcements: [],
      // 统计数据
      orderStats: {
        currentMonthOrders: 0,
        lastMonthOrders: 0,
        growthRate: '0.00%'
      },
      salesStats: {
        currentMonthSales: 0,
        lastMonthSales: 0,
        growthRate: '0.00%'
      },
      userStats: {
        currentYearUsers: 0,
        lastYearUsers: 0,
        growthRate: '0.00%'
      },
      // 热销商品数据
      topProductsChart: null,
      topProducts: [],
      // 添加品类统计数据
      categoryChart: null,
      categoryStats: []
    }
  },
  computed: {
    // 计算订单增长率数值（去掉百分号）
    orderTrend() {
      return parseFloat(this.orderStats.growthRate)
    },
    // 计算销售额增长率数值
    saleTrend() {
      return parseFloat(this.salesStats.growthRate)
    },
    // 计算用户增长率数值
    userTrend() {
      return parseFloat(this.userStats.growthRate)
    }
  },
  created() {
    this.fetchData()
    this.fetchStatistics()
    this.fetchTopProducts()
    this.fetchCategoryStats()
  },
  mounted() {
    this.$nextTick(() => {
      this.initTopProductsChart()
      this.initCategoryChart()
    })
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeUnmount() {
    // 清理 ECharts 实例
    if (this.topProductsChart) {
      try {
        this.topProductsChart.dispose()
      } catch (e) {
        logger.warn('清理 topProductsChart 时出错:', e)
      }
      this.topProductsChart = null
    }
    if (this.categoryChart) {
      try {
        this.categoryChart.dispose()
      } catch (e) {
        logger.warn('清理 categoryChart 时出错:', e)
      }
      this.categoryChart = null
    }
    window.removeEventListener('resize', this.resizeCharts)
  },
  methods: {
    // 获取通知数据
    fetchData() {
      Request.get("/notice/limit", {
        params: {
          count: this.noticeLimit
        }
      }).then(response => {
        if (response.code === '0') {
          this.announcements = response.data
        }
      })
    },
    // 获取统计数据
    async fetchStatistics() {
      try {
        // 获取订单统计
        const orderResponse = await Request.get("/statistics/orders/monthly")
        if (orderResponse.code === '0' && orderResponse.data) {
          this.orderStats = {
            currentMonthOrders: Number(orderResponse.data.currentMonthOrders) || 0,
            lastMonthOrders: Number(orderResponse.data.lastMonthOrders) || 0,
            growthRate: orderResponse.data.growthRate || '0.00%'
          }
        } else {
          logger.warn('订单统计数据格式异常:', orderResponse)
          this.orderStats = {
            currentMonthOrders: 0,
            lastMonthOrders: 0,
            growthRate: '0.00%'
          }
        }
      } catch (error) {
        logger.error('获取订单统计失败:', error)
        this.orderStats = {
          currentMonthOrders: 0,
          lastMonthOrders: 0,
          growthRate: '0.00%'
        }
      }

      try {
        // 获取销售额统计
        const salesResponse = await Request.get("/statistics/sales/monthly")
        if (salesResponse.code === '0' && salesResponse.data) {
          this.salesStats = {
            currentMonthSales: Number(salesResponse.data.currentMonthSales) || 0,
            lastMonthSales: Number(salesResponse.data.lastMonthSales) || 0,
            growthRate: salesResponse.data.growthRate || '0.00%'
          }
        } else {
          logger.warn('销售额统计数据格式异常:', salesResponse)
          this.salesStats = {
            currentMonthSales: 0,
            lastMonthSales: 0,
            growthRate: '0.00%'
          }
        }
      } catch (error) {
        logger.error('获取销售额统计失败:', error)
        this.salesStats = {
          currentMonthSales: 0,
          lastMonthSales: 0,
          growthRate: '0.00%'
        }
      }

      try {
        // 获取用户统计
        const userResponse = await Request.get("/statistics/users/yearly")
        if (userResponse.code === '0' && userResponse.data) {
          this.userStats = {
            currentYearUsers: Number(userResponse.data.currentYearUsers) || 0,
            lastYearUsers: Number(userResponse.data.lastYearUsers) || 0,
            growthRate: userResponse.data.growthRate || '0.00%'
          }
        } else {
          logger.warn('用户统计数据格式异常:', userResponse)
          this.userStats = {
            currentYearUsers: 0,
            lastYearUsers: 0,
            growthRate: '0.00%'
          }
        }
      } catch (error) {
        logger.error('获取用户统计失败:', error)
        this.userStats = {
          currentYearUsers: 0,
          lastYearUsers: 0,
          growthRate: '0.00%'
        }
      }
    },
    getNoticeType(type) {
      const types = {
        1: 'primary',   // 普通通知
        2: 'success',   // 活动通知
        3: 'warning',   // 重要通知
        4: 'danger'     // 紧急通知
      }
      return types[type] || 'primary'
    },
    // 初始化图表
    initTopProductsChart() {
      if (!this.$refs.topProductsChart) {
        return
      }
      
      // 如果已经存在实例，先销毁
      if (this.topProductsChart) {
        try {
          this.topProductsChart.dispose()
        } catch (e) {
          logger.warn('销毁旧图表实例时出错:', e)
        }
      }
      
      try {
        this.topProductsChart = echarts.init(this.$refs.topProductsChart)
        if (this.topProducts.length > 0) {
          this.updateTopProductsChart()
        }
      } catch (e) {
        logger.error('初始化热销商品图表失败:', e)
      }
    },

    // 更新图表数据
    updateTopProductsChart() {
      if (!this.topProductsChart) {
        return
      }
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c}件'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: {
            formatter: (value) => value + '件'
          }
        },
        yAxis: {
          type: 'category',
          data: this.topProducts.map(item => item.name).reverse(),
          axisLabel: {
            formatter: (value) => {
              if (value.length > 10) {
                return value.substring(0, 10) + '...'
              }
              return value
            }
          }
        },
        series: [{
          name: '销售数量',
          type: 'bar',
          data: this.topProducts.map(item => item.salesCount).reverse(),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#7B9BC8' },
              { offset: 0.5, color: '#3A5BA0' },
              { offset: 1, color: '#3A5BA0' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#4A6FB8' },
                { offset: 0.7, color: '#4A6FB8' },
                { offset: 1, color: '#7B9BC8' }
              ])
            }
          }
        }]
      }
      this.topProductsChart.setOption(option)
    },

    // 获取热销商品数据
    async fetchTopProducts() {
      try {
        const response = await Request.get('/statistics/products/top5')
        if (response.code === '0' && response.data.topProducts) {
          this.topProducts = response.data.topProducts
          this.$nextTick(() => {
            this.updateTopProductsChart()
          })
        }
      } catch (error) {
        logger.error('获取热销商品数据失败:', error)
      }
    },

    // 初始化品类图表
    initCategoryChart() {
      if (!this.$refs.categoryChart) {
        return
      }
      
      // 如果已经存在实例，先销毁
      if (this.categoryChart) {
        try {
          this.categoryChart.dispose()
        } catch (e) {
          logger.warn('销毁旧图表实例时出错:', e)
        }
      }
      
      try {
        this.categoryChart = echarts.init(this.$refs.categoryChart)
        if (this.categoryStats.length > 0) {
          this.updateCategoryChart()
        }
      } catch (e) {
        logger.error('初始化品类图表失败:', e)
      }
    },

    // 更新品类图表数据
    updateCategoryChart() {
      if (!this.categoryChart) {
        return
      }
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [
          {
            name: '品类销售',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              formatter: '{b}: {d}%'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: true
            },
            data: this.categoryStats.map(item => ({
              name: item.name,
              value: item.salesCount
            }))
          }
        ],
        color: [
          '#3A5BA0', '#5F7C6E', '#C9A063', '#C23A3B',
          '#909399', '#6B8E92', '#D4A5A5', '#8B7FA8'
        ]
      }
      this.categoryChart.setOption(option)
    },

    // 获取品类统计数据
    async fetchCategoryStats() {
      try {
        const response = await Request.get('/statistics/category/sales')
        if (response.code === '0' && response.data.categoryStats) {
          this.categoryStats = response.data.categoryStats
          this.$nextTick(() => {
            this.updateCategoryChart()
          })
        }
      } catch (error) {
        logger.error('获取品类统计数据失败:', error)
      }
    },

    // 统一处理图表缩放
    resizeCharts() {
      if (this.topProductsChart) {
        this.topProductsChart.resize()
      }
      if (this.categoryChart) {
        this.categoryChart.resize()
      }
    }
  }
}
</script>

<style lang="less" scoped>
.dashboard-wrapper {
  padding: 20px;
}

.stat-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  min-width: 240px;

  .stat-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .stat-icon {
    font-size: 24px;
    margin-right: 12px;
    color: #3A5BA0;
  }

  .stat-title {
    font-size: 16px;
    color: #606266;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 16px;
  }

  .stat-footer {
    font-size: 14px;
    color: #909399;

    .up {
      color: #5F7C6E;
    }

    .down {
      color: #C23A3B;
    }
  }
}

.content-wrapper {
  display: flex;
  gap: 20px;
  margin-top: 20px;

  .chart-card {
    flex: 1;
    min-width: 400px;
  }

  .notice-card {
    flex: 1;
    min-width: 400px;
  }
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.chart-content {
  height: 400px;
  .chart {
    width: 100%;
    height: 100%;
  }
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.notice-content {
  max-height: 600px;
  overflow-y: auto;
}

.notice-item {
  margin-bottom: 8px;

  h4 {
    margin: 0 0 8px;
    color: #303133;
  }

  .notice-text {
    color: #606266;
    margin: 0;
    line-height: 1.6;
  }
}

:deep(.el-timeline-item__node--primary) {
  background-color: #3A5BA0;
}

:deep(.el-timeline-item__node--success) {
  background-color: #5F7C6E;
}

:deep(.el-timeline-item__node--warning) {
  background-color: #C9A063;
}

:deep(.el-timeline-item__node--danger) {
  background-color: #C23A3B;
}

/* 确保图表容器有最小高度 */
.chart {
  min-height: 300px;
}

/* 适配小屏幕 */
@media screen and (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;

    .chart-card,
    .notice-card {
      width: 100%;
    }
  }
}
</style>