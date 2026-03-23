<template>
  <div class="category-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>分类管理</h2>
      <el-tag type="info">共 {{ total }} 个分类</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="分类名称">
          <el-input v-model="searchForm.name" placeholder="请输入分类名称" clearable style="width: 200px;"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="default" plain type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button size="default" plain @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      <div class="operation-buttons">
        <el-button plain size="default" type="primary" class="add-button" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
        <el-button plain size="default" @click="getList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>


    <!-- 分类列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="categories" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="图标" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.icon" class="icon-display">
              <i :class="getIconClass(row.icon)" class="category-icon" :title="row.icon"></i>
            </div>
            <span v-else class="no-icon">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称"></el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link size="small" class="delete-btn" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.currentPage" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 分类表单对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" filterable placeholder="请选择图标" style="width: 100%;">
            <el-option v-for="dict in iconDict" :key="dict.itemKey" :label="dict.itemKey" :value="dict.itemValue">
              <div class="icon-option">
                <i :class="getIconClass(dict.itemValue)" class="icon-option-icon"></i>
                <span>{{ dict.itemKey }}</span>
              </div>
            </el-option>
          </el-select>
          <div v-if="form.icon" class="icon-preview">
            <i :class="getIconClass(form.icon)" class="icon-preview-icon"></i>
            <div class="icon-preview-text">图标预览</div>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" :rows="4" placeholder="请输入分类描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import Pagination from '@/components/Pagination/index.vue'
import { Plus, Refresh, Edit, Delete, Search } from '@element-plus/icons-vue'
import '@/assets/iconfont.css'

export default {
  name: 'CategoryManager',
  components: {
    Pagination,
    Plus,
    Refresh,
    Edit,
    Delete,
    Search
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        name: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 分类列表
      categories: [],
      // 总数
      total: 0,
      // 对话框显示
      dialogVisible: false,
      // 对话框标题
      dialogTitle: '',
      // 表单数据
      form: {
        id: undefined,
        name: '',
        icon: '',
        description: ''
      },
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' }
        ],
        icon: [
          { required: true, message: '请选择图标', trigger: 'change' }
        ]
      },
      // 表单标签宽度
      formLabelWidth: '100px',
      iconDict: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取分类列表
    async getList() {
      try {
        const params = {
          ...this.queryParams,
          name: this.searchForm.name
        }
        const res = await Request.get('/category/page', { params })
        if (res.code === '0') {
          this.categories = res.data.records.map(item => {
            // 调试：打印图标值
            logger.debug('分类图标数据:', {
              id: item.id,
              name: item.name,
              icon: item.icon,
              iconType: typeof item.icon
            })
            return {
              ...item,
              createdAt: new Date(item.createdAt).toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
              }).replace(/\//g, '-')
            }
          })
          this.total = res.data.total
        }
      } catch (error) {
        logger.error('获取分类列表失败:', error)
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
        name: ''
      }
      this.handleSearch()
    },
    // 新增分类
    handleAdd() {
      this.dialogTitle = '新增分类'
      this.form = {
        name: '',
        icon: '',
        description: ''
      }
      this.loadIconList()
      this.dialogVisible = true
    },
    // 编辑分类
    handleEdit(row) {
      this.dialogTitle = '编辑分类'
      this.form = { ...row }
      this.form.createdAt = undefined
      this.form.updatedAt = undefined
      this.loadIconList()
      this.dialogVisible = true

    },
    // 删除分类
    handleDelete(row) {
      this.$confirm('确认删除该分类?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/category/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }
        } catch (error) {
          logger.error('删除分类失败:', error)
        }
      }).catch(() => {})
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/category/${this.form.id}` : '/category'
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '添加'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            logger.error('提交表单失败:', error)
          }
        }
      })
    },
    // 添加加载图标列表方法
    async loadIconList() {
      try {
        const res = await Request.get("/dictitem/findByType", {
          params: {
            code: "icon"
          }
        })
        if (res.code === '0') {
          this.iconDict = res.data
        } else {
          this.$message({
            type: 'error',
            message: '获取图标列表失败'
          })
        }
      } catch (error) {
        logger.error('获取图标列表失败:', error)
        this.$message.error('获取图标列表失败')
      }
    },
    // 获取图标类名，确保正确显示
    getIconClass(iconValue) {
      if (!iconValue) {
        return ''
      }
      
      // 转换为字符串
      if (typeof iconValue !== 'string') {
        iconValue = String(iconValue)
      }
      
      // 去除首尾空格
      iconValue = iconValue.trim()
      
      // 如果已经包含 iconfont 或 Font Awesome 类，直接返回
      if (iconValue.includes('iconfont') || 
          iconValue.includes('fa-') || 
          iconValue.startsWith('fas ') || 
          iconValue.startsWith('far ') || 
          iconValue.startsWith('fal ') || 
          iconValue.startsWith('fab ')) {
        return iconValue
      }
      
      // 如果以 icon- 开头，添加 iconfont 类
      if (iconValue.startsWith('icon-')) {
        return `iconfont ${iconValue}`
      }
      
      // 如果以 fa- 开头但没有 fas/far/fal/fab 前缀，添加 fas 前缀
      if (iconValue.startsWith('fa-')) {
        return `fas ${iconValue}`
      }
      
      // 如果包含空格，可能是多个类名，检查是否已有字体类
      if (iconValue.includes(' ')) {
        const parts = iconValue.split(' ')
        const hasFontClass = parts.some(part => 
          part === 'iconfont' || 
          part === 'fas' || 
          part === 'far' || 
          part === 'fal' || 
          part === 'fab'
        )
        if (!hasFontClass) {
          return `iconfont ${iconValue}`
        }
        return iconValue
      }
      
      // 其他情况，尝试添加 iconfont
      return `iconfont ${iconValue}`
    }
  }
}
</script>

<style scoped>
.category-manager {
  padding: var(--spacing-xxl);
  background-color: var(--color-bg-page);
  min-height: calc(100vh - var(--layout-header-height));
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-xxl);
}

.page-header h2 {
  font-size: var(--font-size-extra-large);
  font-weight: var(--font-weight-primary);
  color: var(--color-text-primary);
  margin: 0;
  margin-right: 15px;
}

.operation-area {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  transition: box-shadow 0.3s ease;
}

.operation-area:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s ease;
}

.table-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.operation-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-start;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 0;
}

.search-form :deep(.el-form-item__label) {
  width: auto !important;
  min-width: 80px;
  text-align: right;
  padding-right: 12px;
}

.search-form :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.search-form :deep(.el-button) {
  margin-left: 0;
  margin-right: 8px;
}

.search-form :deep(.el-button:last-child) {
  margin-right: 0;
}

/* 美化表格 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

:deep(.el-table th) {
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f2f5 100%);
  font-weight: 600;
  color: #1f2f3d;
  padding: 16px 0;
  border-bottom: 2px solid #e4e7ed;
}

:deep(.el-table td) {
  padding: 14px 0;
  transition: background-color 0.2s ease;
}

:deep(.el-table tbody tr:hover td) {
  background-color: #f5f7fa !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafcff;
}

:deep(.el-table--border) {
  border: 1px solid #ebeef5;
}

:deep(.el-table--border th, .el-table--border td) {
  border-right: 1px solid #ebeef5;
}

/* 美化对话框 */
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

:deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid #ebeef5;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-dialog__body) {
  padding: 32px 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
}

/* 美化表单 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

.delete-btn {
  color: #f56c6c;
  margin-left: 12px;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  color: #ff4949;
  transform: scale(1.1);
}

/* 图标显示样式 */
.icon-display {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  min-height: 40px;
  width: 100%;
}

.category-icon {
  font-size: 24px;
  color: #409eff;
  display: inline-block !important;
  line-height: 1;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  visibility: visible !important;
  opacity: 1 !important;
  /* 默认使用 iconfont */
  font-family: 'iconfont', sans-serif !important;
  font-weight: normal;
}

/* iconfont 图标样式 - 确保正确应用 */
i.category-icon.iconfont,
i[class*="iconfont"].category-icon {
  font-family: 'iconfont' !important;
  font-weight: normal;
}

/* Font Awesome 图标样式 */
i.category-icon[class*="fa-"],
i.category-icon[class*="fas"],
i.category-icon[class*="far"],
i.category-icon[class*="fal"],
i.category-icon[class*="fab"] {
  font-family: 'Font Awesome 6 Free', 'Font Awesome 5 Free', sans-serif !important;
  font-weight: 900;
}

.no-icon {
  color: #909399;
  font-size: 12px;
}

/* 图标选择器样式 */
.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-option-icon {
  font-size: 18px;
  color: #409eff;
  display: inline-block !important;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-family: 'iconfont', sans-serif !important;
  font-weight: normal;
}

i.icon-option-icon.iconfont,
i[class*="iconfont"].icon-option-icon {
  font-family: 'iconfont' !important;
  font-weight: normal;
}

i.icon-option-icon[class*="fa-"],
i.icon-option-icon[class*="fas"],
i.icon-option-icon[class*="far"],
i.icon-option-icon[class*="fal"],
i.icon-option-icon[class*="fab"] {
  font-family: 'Font Awesome 6 Free', 'Font Awesome 5 Free', sans-serif !important;
  font-weight: 900;
}

/* 图标预览样式 */
.icon-preview {
  margin-top: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf5ff 100%);
  border-radius: 8px;
  text-align: center;
  border: 1px solid #e4e7ed;
}

.icon-preview-icon {
  font-size: 36px;
  color: #409eff;
  display: inline-block !important;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  margin-bottom: 8px;
  font-family: 'iconfont', sans-serif !important;
  font-weight: normal;
}

i.icon-preview-icon.iconfont,
i[class*="iconfont"].icon-preview-icon {
  font-family: 'iconfont' !important;
  font-weight: normal;
}

i.icon-preview-icon[class*="fa-"],
i.icon-preview-icon[class*="fas"],
i.icon-preview-icon[class*="far"],
i.icon-preview-icon[class*="fal"],
i.icon-preview-icon[class*="fab"] {
  font-family: 'Font Awesome 6 Free', 'Font Awesome 5 Free', sans-serif !important;
  font-weight: 900;
}

.icon-preview-text {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}
</style> 