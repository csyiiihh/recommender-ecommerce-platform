<template>
  <div id="app">
    <router-view></router-view>
    <customer-service-widget v-if="showCustomerService"></customer-service-widget>
  </div>
</template>

<script>
import CustomerServiceWidget from '@/components/front/CustomerServiceWidget.vue'

export default {
  name: 'App',
  components: {
    CustomerServiceWidget
  },
  computed: {
    showCustomerService() {
      // 只在前端用户页面显示客服组件
      // 1. 排除登录、注册、忘记密码页面
      const excludePaths = ['/login', '/register', '/forget']
      if (excludePaths.includes(this.$route.path)) {
        return false
      }
      
      // 2. 排除所有管理端页面（管理端使用的是 home layout）
      // 检查路由是否匹配了 home layout
      const isBackendPage = this.$route.matched.some(route => route.name === 'home')
      if (isBackendPage) {
        return false
      }
      
      // 3. 其他前端用户页面显示客服组件
      return true
    }
  }
}
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  height: 100%;
}

/* 全局滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>
