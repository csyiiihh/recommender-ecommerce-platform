<template>
  <div class="article-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>图书资讯管理</h2>
      <el-tag type="info">共 {{ total }} 篇文章</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="资讯标题">
          <el-input v-model="searchForm.title" placeholder="请输入资讯标题" clearable></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="上架" :value="1"></el-option>
            <el-option label="下架" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="default" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="default" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="operation-buttons">
        <el-button plain size="default" type="primary" class="add-button" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增资讯
        </el-button>
        <el-button plain size="default" @click="getList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 数据统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-file-alt"></i></div>
                <div class="stat-title">已发布</div>
              </div>
              <div class="stat-value">{{ getPublishedCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-file-archive"></i></div>
                <div class="stat-title">已下架</div>
              </div>
              <div class="stat-value">{{ getUnpublishedCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-eye"></i></div>
                <div class="stat-title">总浏览量</div>
              </div>
              <div class="stat-value">{{ getTotalViews }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 文章列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="articles" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="封面图" width="120">
          <template #default="{ row }">
            <el-image 
              v-if="row.coverImage"
              :src="buildImageUrl(row.coverImage)" 
              :preview-src-list="[buildImageUrl(row.coverImage)]" 
              fit="cover" 
              style="width: 80px; height: 80px">
            </el-image>
            <i v-else class="fas fa-image" style="font-size: 30px; color: #909399;"></i>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip></el-table-column>
        <el-table-column prop="summary" label="摘要" show-overflow-tooltip></el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="() => handleStatusChange(row)"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
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
      <pagination 
        v-show="total > 0" 
        :total="total" 
        v-model:page="queryParams.currentPage" 
        v-model:limit="queryParams.size" 
        @pagination="getList" />
    </el-card>

    <!-- 文章表单对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="1200px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px" class="article-form">
        <div class="form-container">
          <!-- 左侧基本信息 -->
          <div class="form-left">
            <el-form-item label="资讯标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入资讯标题"></el-input>
            </el-form-item>
            <el-form-item label="资讯摘要" prop="summary">
              <el-input 
                type="textarea" 
                v-model="form.summary" 
                :rows="4" 
                placeholder="请输入资讯摘要"
                maxlength="50"
                show-word-limit
              ></el-input>
            </el-form-item>
            <el-form-item label="封面图片" prop="coverImage">
              <el-upload
                class="avatar-uploader"
                action="#"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleUpload">
                <img v-if="form.coverImage" :src="buildImageUrl(form.coverImage)" class="avatar">
                <i v-else class="fas fa-plus avatar-uploader-icon"></i>
              </el-upload>
              <div class="upload-tip">建议尺寸：300×200px，支持 jpg、png 格式，大小不超过 10MB</div>
            </el-form-item>
            <el-form-item label="发布状态" prop="status">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架">
              </el-switch>
            </el-form-item>
          </div>

          <!-- 右侧文章内容 -->
          <div class="form-right">
            <el-form-item label="文章内容" prop="content" class="content-item">
              <div class="editor-wrapper">
                <Toolbar class="editor-toolbar" :editor="editor" :defaultConfig="toolbarConfig" :mode="mode" />
                <Editor class="editor-content" v-model="form.content" :defaultConfig="editorConfig" :mode="mode" @onCreated="handleCreated" @onChange="handleChange" />
              </div>
            </el-form-item>
          </div>
        </div>
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
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { formatTime } from '@/utils/time'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'

export default {
  name: 'ArticleManager',
  components: {
    Pagination,
    Editor,
    Toolbar,
    Plus,
    Refresh,
    Edit,
    Delete
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        title: '',
        status: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 文章列表
      articles: [],
      // 总数
      total: 0,
      // 对话框显示
      dialogVisible: false,
      // 对话框标题
      dialogTitle: '',
      // 表单数据
      form: {
        id: undefined,
        title: '',
        content: '',
        summary: '',
        coverImage: '',
        status: 1
      },
      // 表单校验规则
      rules: {
        title: [
          { required: true, message: '请输入资讯标题', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入资讯内容', trigger: 'blur' }
        ],
        summary: [
          { max: 50, message: '摘要不能超过50个字符', trigger: 'blur' }
        ]
      },
      // 富文本编辑器相关
      editor: null,
      mode: 'default',
      toolbarConfig: {
        excludeKeys: [
          'group-video'
        ]
      },
      editorConfig: {
        placeholder: '请输入文章内容...',
        MENU_CONF: {
          uploadImage: {
            base64LimitSize: 5 * 1024 * 1024,
            customUpload: async (file, insertFn) => {
              try {
                const isImage = file.type.startsWith('image/')
                if (!isImage) {
                  this.$message.error('上传文件只能是图片格式!')
                  return
                }

                const fileSize = file.size / 1024 / 1024
                if (fileSize > 10) {
                  this.$message.error('图片大小不能超过 10MB!')
                  return
                }

                const formData = new FormData()
                formData.append('file', file)

                const res = await Request.post('/file/upload/img', formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data'
                  }
                })

                if (res.code === '0') {
                  insertFn(`/api${res.data}`)
                  this.$message.success('图片上传成功')
                } else {
                  this.$message.error(res.msg || '图片上传失败')
                }
              } catch (error) {
                logger.error('上传图片失败:', error)
                this.$message.error('图片上传失败')
              }
            }
          }
        }
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatTime,
    // 构建正确的图片URL
    buildImageUrl(imageUrl) {
      if (!imageUrl) {
        return '';
      }
      
      // 如果已经是完整的HTTP/HTTPS URL，直接返回
      if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
        return imageUrl;
      }
      
      // 如果以 / 开头，说明是相对路径（如 /img/xxx.png）
      if (imageUrl.startsWith('/')) {
        return `/api${imageUrl}`;
      }
      
      // 如果只是文件名（如 1767524244906.png），添加 /img/ 前缀
      return `/api/img/${imageUrl}`;
    },
    // 获取文章列表
    async getList() {
      try {
        const params = {
          ...this.queryParams,
          ...this.searchForm
        }
        const res = await Request.get('/article/page', { params })
        if (res.code === '0') {
          this.articles = res.data.records || []
          this.total = res.data.total
        }
      } catch (error) {
        logger.error('获取文章列表失败:', error)
        this.$message.error('获取文章列表失败')
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
        title: '',
        status: ''
      }
      this.handleSearch()
    },
    // 新增文章
    handleAdd() {
      this.dialogTitle = '新增资讯'
      this.form = {
        title: '',
        content: '',
        summary: '',
        coverImage: '',
        status: 1
      }
      this.dialogVisible = true
    },
    // 编辑文章
    handleEdit(row) {
      this.dialogTitle = '编辑资讯'
      this.form = { ...row }
      this.dialogVisible = true
    },
    // 删除文章
    handleDelete(row) {
      this.$confirm('确认删除该资讯?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/article/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }
        } catch (error) {
          logger.error('删除文章失败:', error)
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    // 更改文章状态
    async handleStatusChange(row) {
      try {
        const res = await Request.put(`/article/${row.id}/status?status=${row.status}`)
        if (res.code === '0') {
          this.$message.success('状态更新成功')
        } else {
          row.status = row.status === 1 ? 0 : 1
          this.$message.error('状态更新失败')
        }
      } catch (error) {
        row.status = row.status === 1 ? 0 : 1
        logger.error('更新文章状态失败:', error)
        this.$message.error('更新失败')
      }
    },
    // 处理图片上传
    async handleUpload(file) {
      const isImage = file.raw.type.startsWith('image/')
      const isLt10M = file.raw.size / 1024 / 1024 < 10

      if (!isImage) {
        this.$message.error('上传文件只能是图片格式!')
        return false
      }
      if (!isLt10M) {
        this.$message.error('上传图片大小不能超过 10MB!')
        return false
      }

      try {
        const formData = new FormData()
        formData.append('file', file.raw)

        const res = await Request.post('/file/upload/img', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        if (res.code === '0') {
          this.form.coverImage = res.data
          this.$message.success('图片上传成功')
        } else {
          this.$message.error(res.msg || '上传失败')
        }
      } catch (error) {
        logger.error('图片上传失败:', error)
        this.$message.error('图片上传失败，请重试')
      }
      
      return false
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/article/${this.form.id}` : '/article'
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '添加'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            logger.error('提交表单失败:', error)
            this.$message.error('提交失败')
          }
        }
      })
    },
    // 富文本编辑器相关方法
    handleCreated(editor) {
      this.editor = Object.seal(editor)
    },
    handleChange(editor) {
      this.form.content = editor.getHtml()
    }
  },
  computed: {
    getPublishedCount() {
      return this.articles.filter(a => a.status === 1).length
    },
    getUnpublishedCount() {
      return this.articles.filter(a => a.status === 0).length
    },
    getTotalViews() {
      return this.articles.reduce((sum, article) => sum + (article.viewCount || 0), 0)
    }
  },
  beforeUnmount() {
    if (this.editor) {
      this.editor.destroy()
    }
  }
}
</script>

<style scoped>
.article-manager {
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

.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
  border: none;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-content {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-left {
  display: flex;
  flex-direction: column;
  text-align: center;
  width: 100px;
  gap: 4px;
  align-items: flex-start;
}

.stat-icon {
  font-size: 24px;
  color: #409eff;
  opacity: 0.8;
  width: 100%;
  text-align: center;
}

.stat-title {
  font-size: 13px;
  color: #909399;
  width: 100%;
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2f3d;
}

.operation-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.operation-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 500;
  color: #1f2f3d;
  padding: 12px 0;
}

:deep(.el-table td) {
  padding: 12px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafcff;
}

/* 对话框样式 */
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

/* 表单样式 */
.article-form {
  height: 700px;
  overflow: hidden;
}

.form-container {
  display: flex;
  gap: 24px;
  height: 100%;
}

.form-left {
  width: 400px;
  flex-shrink: 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  height: 100%;
  overflow-y: auto;
}

.form-right {
  flex: 1;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

.content-item {
  height: 100%;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.editor-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  min-height: 0;
}

.editor-toolbar {
  border-bottom: 1px solid #dcdfe6;
  background-color: #fff;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  flex-shrink: 0;
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  height: 100%;
  min-height: 0;
}

:deep(.w-e-text-container) {
  height: 100% !important;
  background: white;
  overflow: hidden;
}

:deep(.w-e-scroll) {
  height: 100% !important;
  min-height: unset !important;
  padding: 0 10px;
  box-sizing: border-box;
}

:deep(.w-e-text) {
  min-height: 500px !important;
  padding: 10px 0;
}

/* 图片上传样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 300px;
  height: 200px;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 300px;
  height: 200px;
  line-height: 200px;
  text-align: center;
}

.avatar {
  width: 300px;
  height: 200px;
  display: block;
  object-fit: cover;
}

.delete-btn {
  color: #f56c6c;
  margin-left: 12px;
}

.delete-btn:hover {
  color: #ff4949;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style> 