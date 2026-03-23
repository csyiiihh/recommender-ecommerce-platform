<template>
  <div class="carousel-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>轮播图管理</h2>
      <el-tag type="info">共 {{ total }} 个轮播图</el-tag>
    </div>

    <div class="content-box">
      <!-- 操作栏 -->
      <el-card class="operation-area" shadow="hover">
        <div class="control-btns">
          <div class="right-btns">
            <el-button type="primary" plain size="default" @click="handleAdd">
              <el-icon><Plus /></el-icon> 新增轮播图
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 表格区域 -->
      <el-card class="table-card" shadow="hover">
        <el-table :data="carouselList" 
                 border 
                 v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column label="图片" width="220" align="center">
            <template #default="{ row }">
              <img :src="buildImageUrl(row.imageUrl)" class="preview-image">
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="120" />
          <el-table-column prop="tag" label="标签" width="120" />
          <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status ? 'success' : 'info'">
                {{ row.status ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="handleEdit(row)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <pagination v-show="total > 0"
                   :total="total"
                   v-model:page="queryParams.currentPage"
                   v-model:limit="queryParams.size"
                   @pagination="getList" />
      </el-card>

      <!-- 新增/编辑弹窗 -->
      <el-dialog :title="dialogTitle" 
                v-model="dialogVisible" 
                width="700px"
                :close-on-click-modal="false">
        <el-form :model="form" :rules="rules" ref="form" label-width="100px">
          <el-form-item label="图片" prop="imageUrl">
            <el-upload class="carousel-uploader"
                      action="#"
                      :show-file-list="false"
                      :auto-upload="false"
                      :on-change="handleUpload"
                      :disabled="uploadLoading">
              <img v-if="form.imageUrl" :src="buildImageUrl(form.imageUrl)" class="carousel-image">
              <div v-else class="carousel-uploader-placeholder">
                <el-icon v-if="!uploadLoading"><Plus /></el-icon>
                <el-icon v-else class="is-loading"><Loading /></el-icon>
                <div class="placeholder-text">{{ uploadLoading ? '上传中...' : '点击上传图片' }}</div>
              </div>
            </el-upload>
            <div class="upload-tip">建议尺寸：900×300px，支持 jpg、png 格式，大小不超过 10MB</div>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="标签" prop="tag">
            <el-input v-model="form.tag" placeholder="请输入标签" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" 
                      v-model="form.description" 
                      :autosize="{ minRows: 3, maxRows: 5 }"
                      placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="关联商品" prop="productId">
            <el-select v-model="form.productId" 
                      filterable 
                      clearable
                      placeholder="请选择商品"
                      style="width: 100%;">
              <el-option label="不关联商品" :value="null"></el-option>
              <el-option v-for="item in productList"
                        :key="item.id"
                        :label="item.name"
                        :value="item.id">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="form.sortOrder" 
                           :min="0"
                           controls-position="right"
                           style="width: 120px;" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="form.status"
                      :active-value="1"
                      :inactive-value="0">
            </el-switch>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false" plain>取 消</el-button>
            <el-button type="primary" @click="submitForm" :loading="submitLoading" plain>确 定</el-button>
          </div>
        </template>
      </el-dialog>

    </div>
  </div>
</template>

<script>
import Request from '../utils/request.js'
import logger from '@/utils/logger'
import Pagination from '../components/Pagination/index.vue'
import { Plus, Edit, Delete, Loading } from '@element-plus/icons-vue'

export default {
  name: 'CarouselManager',
  components: { 
    Pagination,
    Plus,
    Edit,
    Delete,
    Loading
  },
  inject: ['userInfo'],
  data() {
    return {
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      total: 0,
      carouselList: [],
      productList: [],
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: undefined,
        imageUrl: '',
        title: '',
        tag: '',
        description: '',
        productId: null,
        sortOrder: 0,
        status: 1
      },
      rules: {
        imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
      },
      loading: false,
      submitLoading: false,
      uploadLoading: false
    }
  },
  created() {
    this.getList()
    this.getProducts()
  },
  methods: {
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
    // 获取轮播图列表
    async getList() {
      try {
        this.loading = true
        const res = await Request.get('/carousel/page', {
          params: this.queryParams
        })
        if (res.code === '0') {
          this.carouselList = res.data.records
          this.total = res.data.total
        }
      } catch (error) {
        logger.error('获取轮播图列表失败:', error)
        this.$message.error('获取轮播图列表失败')
      } finally {
        this.loading = false
      }
    },
    // 获取商品列表
    async getProducts() {
      try {
        const res = await Request.get('/product/all')
        if (res.code === '0') {
          this.productList = res.data
        }
      } catch (error) {
        logger.error('获取商品列表失败:', error)
      }
    },
    // 处理新增
    handleAdd() {
      this.dialogTitle = '新增轮播图'
      this.form = {
        imageUrl: '',
        title: '',
        tag: '',
        description: '',
        productId: null,
        sortOrder: 0,
        status: 1
      }
      this.dialogVisible = true
    },
    // 处理编辑
    handleEdit(row) {
      this.dialogTitle = '编辑轮播图'
      this.form = { ...row }
      this.dialogVisible = true
    },
    // 处理删除
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该轮播图?', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/carousel/${row.id}`)
        if (res.code === '0') {
          this.$message.success('删除成功')
          this.getList()
        }
      } catch (error) {
        logger.error('删除轮播图失败:', error)
      }
    },
    // 处理图片上传
    async handleUpload(file) {
      // 检查文件类型和大小
      const isImage = file.raw.type.startsWith('image/')
      const isLt10M = file.raw.size / 1024 / 1024 < 10

      if (!isImage) {
        this.$message.error('上传文件只能是图片格式!')
        return
      }
      if (!isLt10M) {
        this.$message.error('上传图片大小不能超过 10MB!')
        return
      }

      try {
        this.uploadLoading = true
        
        // 创建 FormData
        const formData = new FormData()
        formData.append('file', file.raw)
        
        // 发送上传请求
        const res = await Request.post('/file/upload/img', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (res.code === '0') {
          this.form.imageUrl = res.data
          this.$message.success('上传成功')
        } else {
          this.$message.error(res.msg || '上传失败')
        }
      } catch (error) {
        logger.error('上传失败:', error)
        this.$message.error('上传失败，请重试')
      } finally {
        this.uploadLoading = false
      }
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            this.submitLoading = true
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/carousel/${this.form.id}` : '/carousel'
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '新增'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            logger.error('保存轮播图失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.carousel-manager {
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

.table-card {
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
}

.control-btns {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;

  .right-btns {
    display: flex;
    gap: 10px;
  }
}

.preview-image {
  height: 60px;
  width: 180px;
  border-radius: 4px;
  object-fit: contain;
  background-color: #f5f7fa;
}

.carousel-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 450px;
  height: 150px;
  display: flex;
  justify-content: center;
  align-items: center;

  &:hover {
    border-color: #409EFF;
  }

  .carousel-image {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    background-color: #f5f7fa;
  }

  .carousel-uploader-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #8c939d;
    background-color: #f5f7fa;

    i.fa-plus {
      font-size: 28px;
      margin-bottom: 8px;
      color: #c0c4cc;
    }

    .placeholder-text {
      font-size: 14px;
      color: #909399;
    }
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;

  .el-dialog__header {
    padding: 24px;
    border-bottom: 1px solid #ebeef5;
    margin: 0;
  }

  .el-dialog__body {
    padding: 32px 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid #ebeef5;
  }
}
</style> 