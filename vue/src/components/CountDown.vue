<template>
  <span class="count-down" :class="{ 'is-danger': isDanger }">
    <i class="fas fa-clock"></i>
    {{ timeText }}
  </span>
</template>

<script>
export default {
  name: 'CountDown',
  props: {
    // 结束时间（时间戳或ISO字符串）
    endTime: {
      type: [Number, String],
      required: true
    },
    // 格式：'full' 显示完整时间，'short' 显示简短时间
    format: {
      type: String,
      default: 'full'
    }
  },
  data() {
    return {
      timeText: '',
      timer: null,
      isDanger: false
    }
  },
  mounted() {
    this.updateTime()
    this.timer = setInterval(this.updateTime, 1000)
  },
  beforeUnmount() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    updateTime() {
      const end = typeof this.endTime === 'string' 
        ? new Date(this.endTime.replace(/-/g, '/')).getTime()
        : this.endTime
      
      const now = Date.now()
      const diff = end - now
      
      if (diff <= 0) {
        this.timeText = '已结束'
        this.isDanger = true
        if (this.timer) {
          clearInterval(this.timer)
        }
        this.$emit('end')
        return
      }
      
      // 判断是否危险状态（少于1小时）
      this.isDanger = diff < 3600000
      
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((diff % (1000 * 60)) / 1000)
      
      if (this.format === 'short') {
        if (days > 0) {
          this.timeText = `${days}天${hours}时`
        } else if (hours > 0) {
          this.timeText = `${hours}时${minutes}分`
        } else {
          this.timeText = `${minutes}:${seconds.toString().padStart(2, '0')}`
        }
      } else {
        // full格式
        if (days > 0) {
          this.timeText = `${days}天 ${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
        } else {
          this.timeText = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
        }
      }
    }
  },
  watch: {
    endTime() {
      this.updateTime()
    }
  }
}
</script>

<style scoped>
.count-down {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'Courier New', monospace;
  font-weight: 500;
  color: #52c41a;
}

.count-down.is-danger {
  color: #ff4d4f;
}

.count-down i {
  font-size: 14px;
}
</style>

