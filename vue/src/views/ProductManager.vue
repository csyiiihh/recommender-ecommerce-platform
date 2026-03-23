<template>
  <div class="product-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>商品管理</h2>
      <el-tag type="info">共 {{ total }} 件商品</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.name" placeholder="请输入商品名称" clearable style="width: 200px;"></el-input>
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="searchForm.categoryId" placeholder="请选择分类" clearable style="width: 200px;">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 200px;">
            <el-option label="上架" :value="1"></el-option>
            <el-option label="下架" :value="0"></el-option>
          </el-select>
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
        <el-button v-if="userInfo.role==='MERCHANT'" plain size="default" type="primary" class="add-button" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增商品
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
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-box"></i></div>
                <div class="stat-title">在售商品</div>
              </div>
              <div class="stat-value">{{ getOnSaleCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-box-open"></i></div>
                <div class="stat-title">已下架</div>
              </div>
              <div class="stat-value">{{ getOffSaleCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-tag"></i></div>
                <div class="stat-title">促销商品</div>
              </div>
              <div class="stat-value">{{ getDiscountCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-trophy"></i></div>
                <div class="stat-title">销量最高</div>
              </div>
              <div class="stat-value">{{ topSalesProduct?.name || '-' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 商品列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="products" border style="width: 100%" :default-sort="{prop: 'id', order: 'ascending'}">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="商品图片" width="120">
          <template #default="{ row }">
            <el-image v-if="row && row.imageUrl" :src="'/api'+row.imageUrl" :preview-src-list="['/api'+row.imageUrl]" fit="cover" style="width: 80px; height: 80px"></el-image>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称"></el-table-column>
        <el-table-column prop="price" label="价格" width="120">
          <template #default="{ row }">
            <span v-if="row && row.price !== undefined">¥{{ row.price.toFixed(2) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100"></el-table-column>
        <el-table-column prop="salesCount" label="销量" width="100"></el-table-column>
        <el-table-column label="分类" width="150">
          <template #default="{ row }">
            <div v-if="row.category" class="category-cell">
              <i v-if="row.category.icon" :class="row.category.icon" class="category-icon"></i>
              <span>{{ row.category.name }}</span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-if="row" v-model="row.status" :active-value="1" :inactive-value="0" @change="() => handleStatusChange(row)" active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
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
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.currentPage" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 商品表单对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="1200px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px" class="product-form">
        <!-- 左侧基本信息 -->
        <div class="form-left">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称"></el-input>
          </el-form-item>
          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <i v-if="item.icon" :class="item.icon" style="font-size: 18px; color: #409eff;"></i>
                  <span>{{ item.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="form.author" placeholder="请输入作者姓名"></el-input>
          </el-form-item>
          <el-form-item label="出版社" prop="publisher">
            <el-input v-model="form.publisher" placeholder="请输入出版社名称"></el-input>
          </el-form-item>
          <el-form-item label="ISBN" prop="isbn">
            <el-input v-model="form.isbn" placeholder="请输入ISBN号"></el-input>
          </el-form-item>
          <el-form-item label="出版日期" prop="publishDate">
            <el-date-picker v-model="form.publishDate" type="date" placeholder="请选择出版日期" style="width: 100%;" value-format="YYYY-MM-DD"></el-date-picker>
          </el-form-item>
          <el-form-item label="商品价格" prop="price">
            <el-input-number v-model="form.price" :precision="2" :step="0.1" :min="0" style="width: 100%;"></el-input-number>
          </el-form-item>
          <el-form-item label="商品库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%;"></el-input-number>
          </el-form-item>
          <el-form-item label="商品图片" prop="imageUrl">
            <el-upload class="avatar-uploader" action="#" :show-file-list="false" :auto-upload="false" :on-change="handleUpload">
              <img v-if="form.imageUrl" :src="'api/'+form.imageUrl" class="avatar">
              <i v-else class="fas fa-plus avatar-uploader-icon"></i>
            </el-upload>
          </el-form-item>
          <el-form-item label="开启折扣" class="discount-switch">
            <el-switch v-model="form.isDiscount" :active-value="1" :inactive-value="0"></el-switch>
          </el-form-item>
          <el-form-item label="折扣价格" v-if="form.isDiscount === 1" prop="discountPrice">
            <el-input-number v-model="form.discountPrice" :precision="2" :step="0.1" :min="0" :max="form.price" style="width: 100%;"></el-input-number>
            <span class="discount-tip">原价: ¥{{ form.price }}</span>
          </el-form-item>
        </div>

        <!-- 右侧商品描述 -->
        <div class="form-right">
          <el-form-item label="商品描述" prop="description" class="description-item">
            <div class="editor-wrapper">
              <Toolbar class="editor-toolbar" :editor="editor" :defaultConfig="toolbarConfig" :mode="mode" />
              <Editor class="editor-content" v-model="form.description" :defaultConfig="editorConfig" :mode="mode" @onCreated="handleCreated"
                @onChange="handleChange" />
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片裁剪对话框 - 暂时禁用，vue-cropper 不支持 Vue 3 -->
    <!-- TODO: 需要替换为支持 Vue 3 的图片裁剪库 -->
    <!--
    <el-dialog title="图片裁剪" v-model="cropperVisible" width="600px" append-to-body :close-on-click-modal="false" :close-on-press-escape="false">
      <div class="cropper-content">
        <vue-cropper ref="cropper" :img="cropImage" :info="true" :autoCrop="true" :autoCropWidth="300" :autoCropHeight="300" :fixedBox="true" :fixed="true"
          :fixedNumber="[1, 1]" :centerBox="true" outputType="png" :full="true" :canMove="true" :canMoveBox="true" :original="false" :enlarge="1"></vue-cropper>
      </div>
      <template #footer>
        <el-button @click="cropperVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleCropUpload">确 定</el-button>
      </template>
    </el-dialog>
    -->
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import Pagination from '@/components/Pagination/index.vue'
import { Plus, Refresh, Edit, Delete, Search } from '@element-plus/icons-vue'
// TODO: vue-cropper 不支持 Vue 3，需要替换为支持 Vue 3 的图片裁剪库
// import { VueCropper } from 'vue-cropper'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@/assets/iconfont.css'

export default {
  name: 'ProductManager',
  inject: ['userInfo'],
  components: {
    Pagination,
    // VueCropper, // TODO: 需要替换为支持 Vue 3 的图片裁剪库
    Editor,
    Toolbar,
    Plus,
    Refresh,
    Edit,
    Delete,
    Search
  },
  data() {
    // 折扣价验证规则
    const validateDiscountPrice = (rule, value, callback) => {
      if (this.form.isDiscount === 1) {
        if (!value) {
          callback(new Error('请输入折扣价格'))
        } else if (value >= this.form.price) {
          callback(new Error('折扣价必须低于原价'))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }

    return {
      // 搜索表单
      searchForm: {
        name: '',
        categoryId: '',
        status: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 商品列表
      products: [],
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
        categoryId: '',
        price: 0,
        stock: 0,
        imageUrl: '',
        description: '',
        isDiscount: 0,
        discountPrice: 0,
        author: '',
        publisher: '',
        isbn: '',
        publishDate: ''
      },
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入商品名称', trigger: 'blur' }
        ],
        categoryId: [
          { required: true, message: '请选择商品分类', trigger: 'change' }
        ],
        price: [
          { required: true, message: '请输入商品价格', trigger: 'blur' }
        ],
        stock: [
          { required: true, message: '请输入商品库存', trigger: 'blur' }
        ],
        discountPrice: [
          { validator: validateDiscountPrice, trigger: 'blur' }
        ]
      },
      // 图片裁剪相关
      cropperVisible: false,
      cropImage: '',
      uploadFile: null,
      topSalesProduct: null,
      editor: null,
      mode: 'default', // 或 'simple'
      toolbarConfig: {
        excludeKeys: [
          'group-video'  // 只移除视频功能
        ]
      },
      editorConfig: {
        placeholder: '请输入商品描述...',
        MENU_CONF: {
          uploadImage: {
            // 小于 5M 的图片转为 base64
            base64LimitSize: 5 * 1024 * 1024,
            // 自定义上传
            customUpload: async (file, insertFn) => {
              try {
                // 检查文件类型
                const isImage = file.type.startsWith('image/')
                if (!isImage) {
                  this.$message.error('上传文件只能是图片格式!')
                  return
                }

                // 检查文件大小
                const fileSize = file.size / 1024 / 1024 // 转换为 MB
                if (fileSize > 10) {
                  this.$message.error('图片大小不能超过 5MB!')
                  return
                }

                // 如果图片小于 10MB，编辑器会自动转为 base64
                // 只有大于 10MB 的图片才会走到这里
                // 创建 FormData
                const formData = new FormData()
                formData.append('file', file)

                // 发送请求
                const res = await Request.post('/file/upload/img', formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data',
                    'token': this.userInfo.token
                  }
                })

                if (res.code === '0') {
                  // 上传成功，插入图片
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
    this.getCategories()
    this.getList()
  },
  methods: {
    // 获取分类列表
    async getCategories() {
      try {
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data
        }
      } catch (error) {
        logger.error('获取分类列表失败:', error)
      }
    },
    // 获取商品列表
    async getList() {
      if(this.userInfo.role === 'MERCHANT'){
        this.queryParams.merchantId = this.userInfo.id
      }
      try {
        const params = {
          ...this.queryParams,
          name: this.searchForm.name,
          categoryId: this.searchForm.categoryId,
          status: this.searchForm.status
        }
        const res = await Request.get('/product/page', { params })
        if (res.code === '0') {
          this.products = res.data.records || []
          this.total = res.data.total
          // Update topSalesProduct safely
          this.topSalesProduct = this.products.length > 0 
            ? this.products.reduce((max, current) => 
                (current.salesCount > (max?.salesCount || 0)) ? current : max, null)
            : null
        }
      } catch (error) {
        logger.error('获取商品列表失败:', error)
        this.products = [] // Reset to empty array on error
        this.total = 0
        this.topSalesProduct = null
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
        name: '',
        categoryId: '',
        status: ''
      }
      this.handleSearch()
    },
    // 新增商品
    handleAdd() {
      this.dialogTitle = '新增商品'
      this.form = {
        name: '',
        categoryId: '',
        price: 0,
        stock: 0,
        imageUrl: '',
        description: '',
        isDiscount: 0,
        discountPrice: 0,
        author: '',
        publisher: '',
        isbn: '',
        publishDate: ''
      }
      this.dialogVisible = true
    },
    // 编辑商品
    handleEdit(row) {
      this.dialogTitle = '编辑商品'
      this.form = { 
        ...row,
        // 格式化日期字段，确保格式为 YYYY-MM-DD
        publishDate: this.formatDateForPicker(row.publishDate)
      }
      this.dialogVisible = true
    },
    // 格式化日期为日期选择器需要的格式 (YYYY-MM-DD)
    formatDateForPicker(date) {
      if (!date) return ''
      // 如果已经是字符串格式，直接返回
      if (typeof date === 'string') {
        // 如果是完整的日期时间字符串，只取日期部分
        if (date.includes(' ')) {
          return date.split(' ')[0]
        }
        return date
      }
      // 如果是 Date 对象或时间戳，格式化为 YYYY-MM-DD
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    // 删除商品
    handleDelete(row) {
      this.$confirm('确认删除该商品?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/product/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }else{
            this.$message({
              message: res.msg,
              type: 'error'
            })
          }
        } catch (error) {
          logger.error('删除商品失败:', error)
        }
      }).catch(() => { })
    },
    // 更改商品状态
    async handleStatusChange(row) {
      try {
        const res = await Request.put(`/product/${row.id}/status?status=${row.status}`)
        if (res.code === '0') {
          this.$message.success('状态更新成功')
        } else {
          row.status = row.status === 1 ? 0 : 1
          this.$message.error('状态更新失败')
        }
      } catch (error) {
        row.status = row.status === 1 ? 0 : 1
        logger.error('更新商品状态失败:', error)
        this.$message.error('更新失败')
      }
    },
    // 修改 handleUpload 方法 - 直接上传，不使用裁剪功能
    async handleUpload(file) {
      // 检查文件类型和大小
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
          this.$message.success('图片上传成功')
        } else {
          this.$message.error(res.msg || '图片上传失败')
        }
      } catch (error) {
        logger.error('图片上传失败:', error)
        this.$message.error('图片上传失败')
      }

      return false // 阻止自动上传
    },
    // 修改 handleCropUpload 方法
    async handleCropUpload() {
      try {
        // 获取裁剪后的 blob 数据
        const blob = await new Promise((resolve) => {
          this.$refs.cropper.getCropBlob((data) => resolve(data))
        })
        // 创建文件对象
        const fileName = `cropped-${Date.now()}.png`
        const file = new File([blob], fileName, { type: 'image/png' })

        // 创建 FormData
        const formData = new FormData()
        formData.append('file', file)

        // 发送上传请求
        const res = await Request.post('/file/upload/img', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            'token': this.userInfo.token
          }
        })

        if (res.code === '0') {
          this.form.imageUrl = res.data
          this.$message.success('图片上传成功')
          this.cropperVisible = false
        } else {
          throw new Error(res.msg || '上传失败')
        }
      } catch (error) {
        logger.error('图片上传失败:', error)
        this.$message.error(error.message || '图片上传失败')
      }
    },
    // 提交表单
    submitForm() {
      // if(this.form.)
      if(this.userInfo.role==='MERCHANT'){
        this.form.merchantId=this.userInfo.id
     
      }
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/product/${this.form.id}` : '/product'
            logger.debug('提交数据:', JSON.stringify(this.form, null, 2))
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '添加'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            logger.error('提交表单失败:', error)
            logger.error('错误状态:', error.response?.status)
            logger.error('错误数据:', error.response?.data)
            const errorMsg = error.response?.data?.msg || error.response?.data?.message || error.message || '操作失败'
            this.$message.error('提交失败: ' + errorMsg)
          }
        }
      })
    },
    handleCreated(editor) {
      this.editor = Object.seal(editor)
    },
    handleChange(editor) {
      this.form.description = editor.getHtml()
    }
  },
  computed: {
    getOnSaleCount() {
      return this.products ? this.products.filter(p => p.status === 1).length : 0
    },
    getOffSaleCount() {
      return this.products ? this.products.filter(p => p.status === 0).length : 0
    },
    getDiscountCount() {
      return this.products ? this.products.filter(p => p.isDiscount === 1).length : 0
    }
  },
  beforeUnmount() {
    if (this.editor) {
      this.editor.destroy()
    }
  },
  mounted() {
    // 移除 addQuillTitle 相关代码
  }
}
</script>

<style scoped>
.product-manager {
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
  margin-right: var(--spacing-lg);
}

.statistics-cards {
  margin-bottom: var(--spacing-xl);
}

.stat-card {
  border-radius: var(--border-radius-large);
  transition: var(--transition-base);
  border: none;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--box-shadow-light);
}

.stat-content {
  padding: var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-left {
  display: flex;
  flex-direction: column;
  text-align: center;
  width: 100px;
  gap: var(--spacing-xs);
  align-items: flex-start;
}

.stat-icon {
  font-size: var(--font-size-extra-large);
  color: var(--color-primary);
  opacity: 0.8;
  width: 100%;
  text-align: center;
}

.stat-title {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
  width: 100%;
  text-align: center;
}

.stat-value {
  font-size: var(--font-size-large);
  font-weight: var(--font-weight-primary);
  color: var(--color-text-primary);
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
}

.delete-btn:hover {
  color: #ff4949;
}

.discount-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

:deep(.el-switch.is-checked .el-switch__core) {
  border-color: #67c23a;
  background-color: #67c23a;
}

/* 修改表单布局样式 */
.product-form {
  display: flex;
  gap: 24px;
}

.form-left {
  width: 380px;
  flex-shrink: 0;
}

.form-right {
  flex: 1;
  min-width: 0; /* 防止flex子项溢出 */
}

.description-item {
  height: 100%;
  margin-bottom: 0;
}

.editor-wrapper {
  height: 600px; /* 设置固定高度 */
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.editor-toolbar {
  border-bottom: 1px solid #dcdfe6;
  background-color: #fff;
  flex-shrink: 0; /* 防止工具栏被压缩 */
}

.editor-content {
  flex: 1;
  overflow-y: auto; /* 添加垂直滚动条 */
}

/* 编辑器相关样式 */
:deep(.w-e-text-container) {
  height: calc(100% - 40px) !important; /* 减去工具栏的高度 */
  overflow-y: auto !important; /* 强制显示滚动条 */
}

:deep(.w-e-scroll) {
  height: 100% !important;
  min-height: unset !important; /* 移除最小高度限制 */
}

/* 调整左侧表单项样式 */
.form-left :deep(.el-form-item) {
  margin-bottom: 22px;
}

.form-left :deep(.el-input-number) {
  width: 100%;
}

.form-left :deep(.el-select) {
  width: 100%;
}

/* 上传图片区域样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

/* 编辑器相关样式 */
:deep(.w-e-text-container) {
  height: 100% !important;
}

:deep(.w-e-toolbar) {
  padding: 8px !important;
}

:deep(.w-e-bar-item) {
  padding: 4px 6px !important;
}

:deep(.w-e-bar-item button) {
  padding: 4px 10px !important;
}

:deep(.w-e-text-container) {
  background-color: #fff !important;
}

:deep(.w-e-panel-content-image) {
  max-height: 400px !important;
}

:deep(.w-e-panel-content-image .w-e-image-container) {
  min-height: 100px !important;
}

:deep(.w-e-progress-bar) {
  background-color: #409eff !important;
}

/* 对话框样式优化 */
:deep(.el-dialog__body) {
  padding: 24px;
}

.discount-switch {
  margin-bottom: 16px;
}

/* 优化图片上传相关样式 */
:deep(.w-e-text-container img) {
  max-width: 100%;
  height: auto;
  margin: 8px 0;
  border-radius: 4px;
}

:deep(.w-e-panel-content-image) {
  max-height: 400px !important;
  padding: 16px !important;
}

:deep(.w-e-progress-bar) {
  background-color: #409eff !important;
}

/* 优化图片上传提示 */
:deep(.w-e-panel-content-image .w-e-image-container) {
  min-height: 100px !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.w-e-text-placeholder) {
  color: #909399 !important;
}

/* 分类单元格样式 */
.category-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-icon {
  font-size: 18px;
  color: #409eff;
  display: inline-block;
  line-height: 1;
}

/* 图片裁剪相关样式 */
.cropper-content {
  height: 400px;
  width: 100%;
}

:deep(.cropper-view-box) {
  border-radius: 4px;
  border: 1px solid #409eff;
}

:deep(.cropper-container) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.vue-cropper) {
  width: 100% !important;
  height: 100% !important;
}

/* 调整裁剪对话框的样式 */
:deep(.el-dialog__body) {
  padding: 10px 20px;
}

.cropper-content {
  background-color: #f5f7fa;
  border-radius: 4px;
}

/* 优化裁剪工具的显示 */
:deep(.cropper-point) {
  background-color: #409eff;
  width: 8px;
  height: 8px;
}

:deep(.cropper-line) {
  background-color: #409eff;
}

:deep(.cropper-face) {
  background-color: inherit;
}
</style> 