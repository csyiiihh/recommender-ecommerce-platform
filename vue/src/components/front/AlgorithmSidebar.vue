<template>
  <div class="algorithm-sidebar">
    <!-- 算法工作台卡片 -->
    <div class="sidebar-card">
      <h3 class="card-title">
        <i class="fas fa-robot"></i>
        🤖 算法工作台
      </h3>
      
      <div class="algo-status">
        <div class="status-item">
          <span class="label">当前算法：</span>
          <span class="value">{{ currentAlgorithm || '混合推荐 (CF+Content)' }}</span>
        </div>
        <div class="status-item">
          <span class="label">数据更新：</span>
          <span class="value">{{ displayTime }}</span>
        </div>
      </div>
      
      <div class="algo-weights" v-if="showWeights">
        <h4 class="weights-title">算法权重配置</h4>
        <div class="slider-container" v-for="(weight, algo) in algorithmWeights" :key="algo">
          <div class="slider-label">
            <label>{{ getAlgorithmName(algo) }}</label>
            <span class="slider-value">{{ weight }}%</span>
          </div>
          <el-slider 
            v-model="algorithmWeights[algo]" 
            :min="0" 
            :max="100"
            @change="handleWeightChange"
            size="small"
          ></el-slider>
        </div>
        <el-button 
          type="primary" 
          size="small" 
          @click="applyWeights"
          style="width: 100%; margin-top: 12px;"
        >
          应用配置
        </el-button>
      </div>
      
      <el-button 
        link
        size="small" 
        @click="showWeights = !showWeights"
        style="width: 100%; margin-top: 8px; color: #667eea;"
      >
        {{ showWeights ? '收起配置' : '展开配置' }}
      </el-button>
    </div>
  </div>
</template>

<script>
import logger from '@/utils/logger'

export default {
  name: 'AlgorithmSidebar',
  props: {
    currentAlgorithm: {
      type: String,
      default: '混合推荐 (CF+Content)'
    },
    lastUpdateTime: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      showWeights: false,
      algorithmWeights: {
        collaborative: 60,
        content: 30,
        trending: 10
      },
      currentTime: Date.now(),
      updateTimer: null
    }
  },
  computed: {
    displayTime() {
      if (!this.lastUpdateTime) {
        return '5分钟前'
      }
      
      // 解析时间戳（可能是数字或字符串格式）
      let timestamp
      if (typeof this.lastUpdateTime === 'number') {
        timestamp = this.lastUpdateTime
      } else if (typeof this.lastUpdateTime === 'string') {
        // 尝试解析为数字
        const num = Number(this.lastUpdateTime)
        if (!isNaN(num)) {
          timestamp = num
        } else {
          // 尝试解析为日期字符串
          timestamp = Date.parse(this.lastUpdateTime)
        }
      } else {
        return '5分钟前'
      }
      
      if (!timestamp || isNaN(timestamp)) {
        return '5分钟前'
      }
      
      // 使用当前时间（定时器更新）来计算相对时间
      const now = this.currentTime
      const diffMs = now - timestamp
      const diffMinutes = Math.floor(diffMs / 60000)
      
      if (diffMinutes < 1) return '刚刚'
      if (diffMinutes < 60) return diffMinutes + ' 分钟前'
      
      const diffHours = Math.floor(diffMinutes / 60)
      if (diffHours < 24) return diffHours + ' 小时前'
      
      const diffDays = Math.floor(diffHours / 24)
      if (diffDays < 7) return diffDays + ' 天前'
      
      // 超过7天显示具体日期时间
      const date = new Date(timestamp)
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}`
    }
  },
  mounted() {
    // 从本地存储加载权重配置
    const savedWeights = localStorage.getItem('algorithmWeights');
    if (savedWeights) {
      try {
        this.algorithmWeights = JSON.parse(savedWeights);
      } catch (e) {
        logger.error('加载算法权重失败:', e);
      }
    }
    
    // 启动定时器，每60秒（1分钟）更新一次时间显示，因为时间显示的最小单位是分钟
    this.updateTimer = setInterval(() => {
      this.currentTime = Date.now()
    }, 60000)
  },
  beforeUnmount() {
    // 清除定时器
    if (this.updateTimer) {
      clearInterval(this.updateTimer)
      this.updateTimer = null
    }
  },
  methods: {
    getAlgorithmName(algo) {
      const names = {
        collaborative: '协同过滤',
        content: '内容匹配',
        trending: '热门趋势'
      };
      return names[algo] || algo;
    },
    handleWeightChange() {
      // 实时保存到本地存储
      localStorage.setItem('algorithmWeights', JSON.stringify(this.algorithmWeights));
    },
    applyWeights() {
      this.handleWeightChange();
      this.$message.success('算法权重已更新');
      // 触发事件通知父组件
      this.$emit('weights-changed', this.algorithmWeights);
    }
  }
}
</script>

<style scoped>
.algorithm-sidebar {
  width: 100%;
}

.sidebar-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title i {
  color: #667eea;
}

.algo-status {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-item .label {
  color: #666;
}

.status-item .value {
  color: #2c3e50;
  font-weight: 500;
}

.algo-weights {
  margin-top: 16px;
}

.weights-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 12px 0;
}

.slider-container {
  margin-bottom: 16px;
}

.slider-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.slider-label label {
  color: #333;
  font-weight: 500;
}

.slider-value {
  color: #667eea;
  font-weight: 600;
  font-size: 14px;
}

:deep(.el-slider__runway) {
  background-color: #e4e7ed;
}

:deep(.el-slider__bar) {
  background-color: #667eea;
}

:deep(.el-slider__button) {
  border-color: #667eea;
}

@media (max-width: 768px) {
  .sidebar-card {
    padding: 16px;
  }
  
  .card-title {
    font-size: 14px;
  }
  
  .status-item {
    font-size: 12px;
  }
}
</style>




