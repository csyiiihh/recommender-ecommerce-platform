<template>
  <div class="user-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>用户管理</h2>
      <el-tag type="info">共 {{ total }} 个用户</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable></el-input>
        </el-form-item>
        <el-form-item label="用户角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable style="width: 220px;">
            <el-option
              v-for="option in roleOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 220px;">
            <el-option label="启用" :value="1"></el-option>
            <el-option label="禁用" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="default" type="primary" @click="handleSearch" plain>查询</el-button>
          <el-button size="default" @click="resetSearch" plain>重置</el-button>
        </el-form-item>
      </el-form>
      <div class="operation-buttons">
        <el-button  type="primary" plain size="default" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
        <el-button plain size="default" @click="getList">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </el-card>

    <!-- 数据统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card stat-card-normal">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-user"></i></div>
                <div class="stat-title">普通用户</div>
              </div>
              <div class="stat-value">{{ userStats.normalUsers }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card stat-card-merchant">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-store"></i></div>
                <div class="stat-title">商户用户</div>
              </div>
              <div class="stat-value">{{ userStats.merchants }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card stat-card-disabled">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-exclamation-triangle"></i></div>
                <div class="stat-title">禁用账号</div>
              </div>
              <div class="stat-value">{{ userStats.disabled }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 用户列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="users" v-loading="listLoading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
        <el-table-column prop="name" label="真实姓名" min-width="120"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger">超级管理员</el-tag>
            <el-tag v-else-if="row.role === 'ADMIN'" type="warning">管理员</el-tag>
            <el-tag v-else-if="row.role === 'MERCHANT'" type="success">商户</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button link size="small" class="delete-btn" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
            <el-button 
              v-if="row.role === 'MERCHANT'" 
              link 
              size="small" 
              @click="handleViewLicense(row)">
              <el-icon><Picture /></el-icon> 查看执照
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-card>

    <!-- 用户表单对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="dialogMode === 'edit'"></el-input>
        </el-form-item>
        <el-form-item label="真实姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogMode === 'add'">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="用户角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="option in roleOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用">
          </el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 营业执照预览对话框 -->
    <el-dialog title="营业执照预览" v-model="licenseDialogVisible" width="500px">
      <div  style="min-height: 200px;">
        <div class="license-preview" v-if="currentLicense">
          <img 
            :src="'/api' + currentLicense" 
            alt="营业执照" 
            class="license-image"
           
            @error="handleImageError"
          >
        </div>
        <div v-else class="no-license">
          <el-icon><Picture /></el-icon>
          <p>暂未上传营业执照</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import Pagination from '@/components/Pagination/index.vue'
import { Plus, Refresh, Edit, Delete, Picture } from '@element-plus/icons-vue'

export default {
  name: 'UserManager',
  components: { 
    Pagination,
    Plus,
    Refresh,
    Edit,
    Delete,
    Picture
  },
  inject: ['userInfo'],
  data() {
    // 邮箱验证规则
    const validateEmail = (rule, value, callback) => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!value) {
        callback(new Error('请输入邮箱地址'))
      } else if (!emailRegex.test(value)) {
        callback(new Error('请输入正确的邮箱地址'))
      } else {
        callback()
      }
    }

    return {
      // 搜索表单
      searchForm: {
        username: '',
        role: '',
        status: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 用户列表
      users: [],
      // 总数
      total: 0,
      // 加载状态
      listLoading: false,
      // 对话框显示
      dialogVisible: false,
      // 对话框标题
      dialogTitle: '',
      // 对话框模式
      dialogMode: 'add',
      // 表单数据
      form: {
        username: '',
        name: '',
        email: '',
        password: '',
        role: '',
        status: 1
      },
      // 用户统计
      userStats: {
        normalUsers: 0,
        merchants: 0,
        admins: 0,
        disabled: 0
      },
      // 表单校验规则
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        email: [
          { required: true, validator: validateEmail, trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择用户角色', trigger: 'change' }
        ]
      },
      // 添加当前用户角色相关数据
      currentRole: '',
      roleOptions: [], // 动态角色选项
      licenseDialogVisible: false,
      currentLicense: '',
      imageLoading: false,
    }
  },
  created() {
    this.currentRole = this.userInfo.role
    this.initRoleOptions()
    this.getList()
  },
  methods: {
    // 初始化角色选项
    initRoleOptions() {
      switch (this.currentRole) {
        case 'SUPER_ADMIN':
          this.roleOptions = [
            { label: '管理员', value: 'ADMIN' },
            { label: '商户', value: 'MERCHANT' },
            { label: '普通用户', value: 'USER' }
          ]
          break
        case 'ADMIN':
          this.roleOptions = [
            { label: '商户', value: 'MERCHANT' },
            { label: '普通用户', value: 'USER' }
          ]
          break
        default:
          this.roleOptions = []
      }
    },

    // 获取用户列表
    async getList() {
      this.listLoading = true
      try {
        const params = {
          ...this.queryParams,
          ...this.searchForm
        }
        const res = await Request.get('/user/page', { params })
        if (res.code === '0') {
          // 修改用户列表过滤逻辑
          this.users = res.data.records.filter(user => {
            // 超级管理员可以看到除了超级管理员之外的所有用户
            if (this.currentRole === 'SUPER_ADMIN') {
              return user.role !== 'SUPER_ADMIN'
            } 
            // 管理员可以看到普通用户和商户
            else if (this.currentRole === 'ADMIN') {
              return ['USER', 'MERCHANT'].includes(user.role)
            }
            return false
          })
          // 使用后端返回的总数，而不是过滤后的长度
          this.total = res.data.total
          this.calculateUserStats()
        }
      } catch (error) {
        logger.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
      } finally {
        this.listLoading = false
      }
    },

    // 计算用户统计数据
    calculateUserStats() {
      this.userStats = {
        normalUsers: this.users.filter(user => user.role === 'USER').length,
        merchants: this.users.filter(user => user.role === 'MERCHANT').length,
        admins: this.users.filter(user => user.role === 'ADMIN').length,
        disabled: this.users.filter(user => user.status === 0).length
      }
    },

    // 搜索
    handleSearch() {
      this.queryParams.currentPage = 1
      this.getList()
    },

    // 重置搜索
    resetSearch() {
      this.searchForm = {
        username: '',
        role: '',
        status: ''
      }
      this.handleSearch()
    },

    // 新增用户
    handleAdd() {
      this.dialogMode = 'add'
      this.dialogTitle = '新增用户'
      this.form = {
        username: '',
        name: '',
        email: '',
        password: '',
        role: '',
        status: 1
      }
      this.dialogVisible = true
    },

    // 编辑用户
    handleEdit(row) {
      // 检查权限
      if (this.currentRole === 'ADMIN' && (row.role === 'SUPER_ADMIN' || row.role === 'ADMIN')) {
        this.$message.error('没有权限编辑该用户')
        return
      }
      
      this.dialogMode = 'edit'
      this.dialogTitle = '编辑用户'
      this.form = { ...row }
      delete this.form.password
      this.dialogVisible = true
    },

    // 删除用户
    handleDelete(row) {
      // 检查权限
      if (this.currentRole === 'ADMIN' && (row.role === 'SUPER_ADMIN' || row.role === 'ADMIN')) {
        this.$message.error('没有权限删除该用户')
        return
      }

      this.$confirm('确认删除该用户?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/user/delete/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }else{
            this.$message.error(res.msg || '删除失败')
          }
        } catch (error) {
          logger.error('删除用户失败:', error)
        }
      }).catch(() => {})
    },

    // 更改用户状态
    async handleStatusChange(row) {
      // 检查权限
      if (this.currentRole === 'ADMIN' && (row.role === 'SUPER_ADMIN' || row.role === 'ADMIN')) {
        this.$message.error('没有权限修改该用户状态')
        row.status = row.status === 1 ? 0 : 1 // 恢复状态
        return
      }

      try {
        const res = await Request.put(`/user/${row.id}`, {
          status: row.status
        })
        if (res.code === '0') {
          this.$message.success('状态更新成功')
          this.calculateUserStats()
        } else {
          row.status = row.status === 1 ? 0 : 1
          this.$message.error('状态更新失败')
        }
      } catch (error) {
        row.status = row.status === 1 ? 0 : 1
        logger.error('更新用户状态失败:', error)
        this.$message.error('更新失败')
      }
    },

    // 提交表单
    submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.dialogMode === 'add' ? 'post' : 'put'
            const url = this.dialogMode === 'add' ? '/user/add' : `/user/${this.form.id}`
            
            // 添加加载状态
            this.$loading({
              lock: true,
              text: '提交中...',
            })
            
            const res = await Request[method](url, this.form)
            
            if (res.code === '0') {
              this.$message.success(`${this.dialogMode === 'add' ? '添加' : '更新'}成功`)
              this.dialogVisible = false
              this.getList()
            } else {
              // 处理后端返回的错误
              this.$message.error(res.msg || '操作失败')
            }
          } catch (error) {
            logger.error('提交表单失败:', error)
            this.$message.error(error.response?.data?.msg || '操作失败，请稍后重试')
          } finally {
            // 关闭加载状态
            this.$loading().close()
          }
        } else {
          // 表单验证失败
          this.$message.warning('请填写完整所有必填项')
          return false
        }
      })
    },

    handleViewLicense(row) {
      this.imageLoading = true;
      this.currentLicense = row.businessLicense;
      this.licenseDialogVisible = true;
    },
    
    handleImageError() {
      this.imageLoading = false;
      this.$message.error('图片加载失败');
    },
    formatDate(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleString()
    }
  }
}
</script>

<style lang="less" scoped>
.user-manager {
  padding: var(--spacing-xxl);
  background-color: var(--color-bg-page);
  min-height: calc(100vh - var(--layout-header-height));
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-xxl);

  h2 {
    font-size: var(--font-size-extra-large);
    font-weight: var(--font-weight-primary);
    color: var(--color-text-primary);
    margin: 0;
    margin-right: var(--spacing-lg);
  }
}

.operation-area {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
  margin-bottom: var(--spacing-xxl);
}

.statistics-cards {
  margin-bottom: 24px;
}

/* 优化统计卡片间距 */
.statistics-cards .el-row {
  margin: 0 -10px;
}

.statistics-cards .el-col {
  padding: 0 10px;
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;
  overflow: hidden;
  position: relative;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #409eff, #67c23a);
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover::before {
    opacity: 1;
  }

  &.stat-card-normal {
    &::before {
      background: linear-gradient(90deg, #409eff, #66b1ff);
    }
    .stat-icon {
      color: #409eff;
    }
  }

  &.stat-card-merchant {
    &::before {
      background: linear-gradient(90deg, #67c23a, #85ce61);
    }
    .stat-icon {
      color: #67c23a;
    }
  }

  &.stat-card-disabled {
    &::before {
      background: linear-gradient(90deg, #f56c6c, #ff7875);
    }
    .stat-icon {
      color: #f56c6c;
    }
  }
}

.stat-content {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 100px;
}

.stat-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 8px;
  transition: transform 0.3s;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1);
}

.stat-title {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #1f2f3d;
  line-height: 1;
  text-align: right;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.operation-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;

  th {
    background-color: #fafafa;
    font-weight: 500;
    color: #1f2f3d;
    padding: 12px 0;
  }

  td {
    padding: 12px 0;
  }
}

.delete-btn {
  color: #f56c6c;
  margin-left: 12px;

  &:hover {
    color: #ff4949;
  }
}

.license-preview {
  text-align: center;
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
   
  .license-image {
    width: 100%;
    max-width: 420px; /* A4 比例 */
    height: 594px; /* 保持 1:1.414 的比例 */
    object-fit: contain;
    background-color: #f5f7fa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
  }
}

.no-license {
  text-align: center;
  padding: 60px 0;
  background-color: #f5f7fa;
  border-radius: 4px;
  color: #909399;
   
  i {
    font-size: 48px;
    margin-bottom: 16px;
  }
   
  p {
    margin: 0;
    font-size: 14px;
  }
}
</style>