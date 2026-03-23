<template>
  <div class="breadcrumb-container">
    <el-breadcrumb separator="/">
      <!-- <el-breadcrumb-item :to="{ path: '/showView' }">首页</el-breadcrumb-item> -->
      <el-breadcrumb-item v-for="item in BreadcrumbItemList" :key="item.path">{{ item.meta.title }}</el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script>
import logger from '@/utils/logger'

export default {
  name: 'BreadCrumbs',
  data() {
    return {
      hasClick: false
    }
  },
  computed: {
    BreadcrumbItemList() {
      return this.$route.matched
    },
  },
  created() {
    logger.debug("------------------------------")
    logger.debug(this.$route)
    logger.debug("------------------------------")
    this.isHome(this.$route.path)
  },
  methods: {
    isHome(path) {
      if (path === '/showView') {
        this.hasClick = false
      } else {
        this.hasClick = true
      }
    },
    goHome() {
      this.$router.push('/showView')
    },
  }
}
</script>

<style lang="less" scoped>
.breadcrumb-container {
  display: flex;
  align-items: center;
  height: 100%;
  padding-left: 16px;
}

:deep(.el-breadcrumb__inner) {
  font-size: 18px;
  font-weight: 600;
  color: #606266;
}

:deep(.el-breadcrumb__separator) {
  margin: 0 8px;
  color: #909399;
  font-size: 18px;
}

.home-link {
  font-size: 18px;
  font-weight: 600;
}
</style>