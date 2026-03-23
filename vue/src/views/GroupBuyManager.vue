<template>
  <div class="groupbuy-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>拼团活动管理</h2>
      <el-tag type="info">共 {{ total }} 个活动</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="活动名称">
          <el-input v-model="searchForm.activityName" placeholder="请输入活动名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="未开始" :value="0"></el-option>
            <el-option label="进行中" :value="1"></el-option>
            <el-option label="已结束" :value="2"></el-option>
            <el-option label="已下架" :value="3"></el-option>
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
          新增活动
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
                <div class="stat-icon active-icon"><i class="fas fa-users"></i></div>
                <div class="stat-title">进行中</div>
              </div>
              <div class="stat-value">{{ getActiveCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon pending-icon"><i class="fas fa-clock"></i></div>
                <div class="stat-title">未开始</div>
              </div>
              <div class="stat-value">{{ getPendingCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon end-icon"><i class="fas fa-check-circle"></i></div>
                <div class="stat-title">已结束</div>
              </div>
              <div class="stat-value">{{ getEndCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon offline-icon"><i class="fas fa-ban"></i></div>
                <div class="stat-title">已下架</div>
              </div>
              <div class="stat-value">{{ getOfflineCount }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 活动列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="activities" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="商品图片" width="120">
          <template #default="{ row }">
            <el-image v-if="row.product" :src="'/api'+row.product.imageUrl" :preview-src-list="['/api'+row.product.imageUrl]" fit="cover" style="width: 80px; height: 80px"></el-image>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="activityName" label="活动名称" min-width="200"></el-table-column>
        <el-table-column prop="product.name" label="关联商品" min-width="150"></el-table-column>
        <el-table-column label="拼团价" width="100">
          <template #default="{ row }">
            ¥{{ row.groupPrice ? row.groupPrice.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column label="成团人数" width="100">
          <template #default="{ row }">
            {{ row.requiredMembers }}人
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="100">
          <template #default="{ row }">
            {{ row.validityHours }}小时
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="160">
          <template #default="{ row }">
            <div>{{ formatDate(row.startTime) }}</div>
            <div>{{ formatDate(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click="handleViewTeams(row)" style="margin-right: 8px;">
              <el-icon><View /></el-icon>
              查看拼团
            </el-button>
            <el-button link size="small" @click="handleEdit(row)" style="margin-right: 8px;">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-dropdown @command="(cmd) => handleStatusChange(row, cmd)" trigger="click" style="margin-left: 0;">
              <el-button link size="small">
                更多<i class="fas fa-chevron-down"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                <!-- 未发布(0)：可以上架 -->
                <el-dropdown-item v-if="row.status === 0" command="publish">
                  <el-icon><Upload /></el-icon>
                  上架
                </el-dropdown-item>
                
                <!-- 进行中(1)：可以结束或下架 -->
                <el-dropdown-item v-if="row.status === 1" command="end">
                  <el-icon><VideoPause /></el-icon>
                  结束
                </el-dropdown-item>
                <el-dropdown-item v-if="row.status === 1" command="offline">
                  <el-icon><Download /></el-icon>
                  下架
                </el-dropdown-item>
                
                <!-- 已结束(2)：可以重新开始 -->
                <el-dropdown-item v-if="row.status === 2" command="start">
                  <el-icon><VideoPlay /></el-icon>
                  开始
                </el-dropdown-item>
                
                <!-- 已下架(3)：可以重新上架 -->
                <el-dropdown-item v-if="row.status === 3" command="publish">
                  <el-icon><Upload /></el-icon>
                  上架
                </el-dropdown-item>
                
                <!-- 所有状态都可以删除 -->
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="queryParams.currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="800px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="form" label-width="120px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动名称"></el-input>
        </el-form-item>
        <el-form-item label="关联商品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择商品" filterable style="width: 100%;" @change="handleProductChange">
            <el-option v-for="item in products" :key="item.id" :label="`${item.name} (¥${item.price})`" :value="item.id">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ item.name }}</span>
                <span style="color: #8492a6; font-size: 13px;">库存: {{ item.stock }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="拼团价格" prop="groupPrice">
          <el-input-number v-model="form.groupPrice" :precision="2" :step="1" :min="0.01" style="width: 100%;"></el-input-number>
          <div v-if="selectedProduct" style="margin-top: 5px; color: #909399; font-size: 12px;">
            原价: ¥{{ selectedProduct.price }}, 折扣: {{ discount }}折
          </div>
        </el-form-item>
        <el-form-item label="成团人数" prop="requiredMembers">
          <el-input-number v-model="form.requiredMembers" :min="2" :max="100" :step="1" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="拼团有效期" prop="validityHours">
          <el-input-number v-model="form.validityHours" :min="1" :max="72" :step="1" style="width: 100%;"></el-input-number>
          <span style="margin-left: 10px;">小时</span>
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-col :span="11">
            <el-form-item prop="startTime">
              <el-date-picker type="datetime" placeholder="选择开始时间" v-model="form.startTime" style="width: 100%;" value-format="YYYY-MM-DD HH:mm:ss"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col class="line" :span="2" style="text-align: center;">至</el-col>
          <el-col :span="11">
            <el-form-item prop="endTime">
              <el-date-picker type="datetime" placeholder="选择结束时间" v-model="form.endTime" style="width: 100%;" value-format="YYYY-MM-DD HH:mm:ss"></el-date-picker>
            </el-form-item>
          </el-col>
        </el-form-item>
        <el-form-item label="活动描述" prop="activityDesc">
          <el-input type="textarea" v-model="form.activityDesc" :rows="4" placeholder="请输入活动描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看拼团团队对话框 -->
    <el-dialog title="拼团团队列表" v-model="teamsDialogVisible" width="1000px">
      <el-table :data="teams" border v-loading="teamsLoading">
        <el-table-column prop="id" label="团队ID" width="80"></el-table-column>
        <el-table-column label="团长" width="120">
          <template #default="{ row }">
            {{ row.leader ? row.leader.username : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="当前人数/所需人数" width="150">
          <template #default="{ row }">
            <el-progress :percentage="(row.currentPeople / row.requiredPeople * 100)" :format="() => `${row.currentPeople}/${row.requiredPeople}`"></el-progress>
          </template>
        </el-table-column>
        <el-table-column label="拼团状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTeamStatusType(row.status)" size="small">
              {{ getTeamStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.expireTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link size="small" @click="handleViewMembers(row)">查看成员</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          @current-change="handleTeamsPageChange"
          :current-page="teamsPage"
          :page-size="10"
          layout="prev, pager, next"
          :total="teamsTotal">
        </el-pagination>
      </div>
    </el-dialog>

    <!-- 查看团队成员对话框 -->
    <el-dialog title="团队成员" v-model="membersDialogVisible" width="600px" append-to-body>
      <el-table :data="currentTeamMembers" border>
        <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isLeader === 1" type="warning" size="small">团长</el-tag>
            <el-tag v-else type="info" size="small">团员</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">已支付</el-tag>
            <el-tag v-else-if="row.status === 2" type="info" size="small">已退款</el-tag>
            <el-tag v-else type="warning" size="small">待支付</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" min-width="160">
          <template #default="{ row }">
            {{ formatDate(row.joinTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { Plus, Refresh, View, Edit, Upload, VideoPause, Download, VideoPlay, Delete } from '@element-plus/icons-vue'

export default {
  name: 'GroupBuyManager',
  components: {
    Plus,
    Refresh,
    View,
    Edit,
    Upload,
    VideoPause,
    Download,
    VideoPlay,
    Delete
  },
  data() {
    return {
      loading: false,
      teamsLoading: false,
      activities: [],
      products: [],
      teams: [],
      currentTeamMembers: [],
      total: 0,
      teamsTotal: 0,
      teamsPage: 1,
      currentActivityId: null,
      queryParams: {
        currentPage: 1,
        pageSize: 10
      },
      searchForm: {
        activityName: '',
        status: null
      },
      dialogVisible: false,
      teamsDialogVisible: false,
      membersDialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        activityName: '',
        productId: null,
        groupPrice: null,
        requiredMembers: 3,
        validityHours: 24,
        startTime: '',
        endTime: '',
        activityDesc: '',
        merchantId: null
      },
      selectedProduct: null,
      rules: {
        activityName: [
          { required: true, message: '请输入活动名称', trigger: 'blur' }
        ],
        productId: [
          { required: true, message: '请选择商品', trigger: 'change' }
        ],
        groupPrice: [
          { required: true, message: '请输入拼团价格', trigger: 'blur' }
        ],
        requiredMembers: [
          { required: true, message: '请输入成团人数', trigger: 'blur' }
        ],
        validityHours: [
          { required: true, message: '请输入拼团有效期', trigger: 'blur' }
        ],
        startTime: [
          { required: true, message: '请选择开始时间', trigger: 'change' }
        ],
        endTime: [
          { required: true, message: '请选择结束时间', trigger: 'change' }
        ]
      },
      userInfo: {}
    }
  },
  computed: {
    getActiveCount() {
      return this.activities.filter(item => item.status === 1).length
    },
    getPendingCount() {
      return this.activities.filter(item => item.status === 0).length
    },
    getEndCount() {
      return this.activities.filter(item => item.status === 2).length
    },
    getOfflineCount() {
      return this.activities.filter(item => item.status === 3).length
    },
    discount() {
      if (this.selectedProduct && this.form.groupPrice) {
        return (this.form.groupPrice / this.selectedProduct.price * 10).toFixed(1)
      }
      return '-'
    }
  },
  created() {
    logger.debug('=== GroupBuyManager 组件已创建 ===')
    this.userInfo = JSON.parse(localStorage.getItem('backUser') || '{}')
    logger.debug('当前用户信息:', this.userInfo)
    logger.debug('开始获取活动列表...')
    this.getList()
    logger.debug('开始获取商品列表...')
    this.getProducts()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const params = {
          currentPage: this.queryParams.currentPage,
          pageSize: this.queryParams.pageSize,
          ...this.searchForm
        }
        const res = await Request.get('/group-buy/activities', { params })
        if (res.code === '200' || res.code === '0') {
          this.activities = res.data.records || []
          this.total = res.data.total || 0
        }
      } catch (error) {
        this.$message.error('获取活动列表失败')
        logger.error(error)
      } finally {
        this.loading = false
      }
    },
    async getProducts() {
      logger.debug('>>> getProducts 方法开始执行')
      try {
        // 根据用户角色获取商品
        const params = {}
        // 如果是商家，只获取自己的商品
        if (this.userInfo.role === 'MERCHANT') {
          params.merchantId = this.userInfo.id
          logger.debug('商家用户，只获取商家ID为', this.userInfo.id, '的商品')
        } else {
          logger.debug('管理员用户，获取所有商品')
        }
        // 只获取上架的商品
        params.status = 1
        
        const requestParams = {
          ...params,
          currentPage: 1,
          size: 1000,
          status: 1
        }
        
        logger.debug('请求参数:', requestParams)
        logger.debug('请求URL: /product/page')
        logger.debug('发送请求前...')
        
        const res = await Request.get('/product/page', { 
          params: requestParams
        })
        
        logger.debug('收到响应:', res)
        logger.debug('响应码:', res.code)
        logger.debug('响应数据:', res.data)
        
        if (res.code === '200' || res.code === '0') {
          this.products = res.data?.records || []
          logger.debug('✓ 成功获取商品列表:', this.products.length, '个')
          if (this.products.length > 0) {
            logger.debug('第一个商品:', this.products[0])
          } else {
            logger.warn('⚠️ 商品列表为空！')
          }
        } else {
          logger.error('✗ API返回错误，code:', res.code)
          this.$message.error('获取商品失败: ' + (res.msg || '未知错误'))
        }
      } catch (error) {
        logger.error('✗✗✗ 获取商品列表异常 ✗✗✗')
        logger.error('错误对象:', error)
        logger.error('错误消息:', error.message)
        logger.error('错误堆栈:', error.stack)
        this.$message.error('获取商品列表失败: ' + error.message)
      }
      logger.debug('>>> getProducts 方法执行结束')
    },
    async getTeams(activityId, page = 1) {
      this.teamsLoading = true
      try {
        const res = await Request.get('/group-buy/teams', {
          params: {
            activityId: activityId
          }
        })
        if (res.code === '200' || res.code === '0') {
          const allTeams = res.data || []
          this.teamsTotal = allTeams.length
          // 前端分页
          const start = (page - 1) * 10
          const end = start + 10
          this.teams = allTeams.slice(start, end)
          logger.debug('获取到拼团团队:', allTeams.length, '个')
        }
      } catch (error) {
        this.$message.error('获取拼团列表失败')
        logger.error(error)
      } finally {
        this.teamsLoading = false
      }
    },
    async getTeamMembers(teamId) {
      try {
        const res = await Request.get('/group-buy/team/' + teamId)
        if (res.code === '200' || res.code === '0') {
          this.currentTeamMembers = res.data.members || []
        }
      } catch (error) {
        this.$message.error('获取团队成员失败')
        logger.error(error)
      }
    },
    handleSearch() {
      this.queryParams.currentPage = 1
      this.getList()
    },
    resetSearch() {
      this.searchForm = {
        activityName: '',
        status: null
      }
      this.queryParams.currentPage = 1
      this.getList()
    },
    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.getList()
    },
    handleCurrentChange(val) {
      this.queryParams.currentPage = val
      this.getList()
    },
    handleTeamsPageChange(val) {
      this.teamsPage = val
      this.getTeams(this.currentActivityId, val)
    },
    handleAdd() {
      logger.debug('点击新增活动按钮')
      this.dialogTitle = '新增拼团活动'
      this.resetForm()
      logger.debug('当前商品列表数量:', this.products.length)
      // 如果商品列表为空，重新获取
      if (this.products.length === 0) {
        logger.debug('商品列表为空，重新获取...')
        this.getProducts()
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑拼团活动'
      this.form = {
        id: row.id,
        activityName: row.activityName,
        productId: row.productId,
        groupPrice: row.groupPrice,
        requiredMembers: row.requiredMembers,
        validityHours: row.validityHours,
        startTime: this.formatDate(row.startTime),
        endTime: this.formatDate(row.endTime),
        activityDesc: row.activityDesc,
        merchantId: row.merchantId
      }
      this.selectedProduct = this.products.find(p => p.id === row.productId)
      this.dialogVisible = true
    },
    handleProductChange(productId) {
      this.selectedProduct = this.products.find(p => p.id === productId)
      if (this.selectedProduct && !this.form.groupPrice) {
        this.form.groupPrice = (this.selectedProduct.price * 0.8).toFixed(2)
      }
    },
    submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            // 准备提交数据
            const submitData = {
              ...this.form,
              merchantId: this.userInfo.id,
              // 确保数值类型正确
              groupPrice: parseFloat(this.form.groupPrice),
              requiredMembers: parseInt(this.form.requiredMembers),
              validityHours: parseInt(this.form.validityHours)
            }
            
            logger.debug('提交数据:', submitData)
            
            if (this.form.id) {
              await Request.put('/group-buy/activity', submitData)
              this.$message.success('更新成功')
            } else {
              await Request.post('/group-buy/activity', submitData)
              this.$message.success('新增成功')
            }
            this.dialogVisible = false
            this.getList()
          } catch (error) {
            logger.error('提交失败:', error)
            logger.error('错误状态:', error.response?.status)
            logger.error('错误数据:', error.response?.data)
            logger.error('错误数据(JSON):', JSON.stringify(error.response?.data, null, 2))
            const errorMsg = error.response?.data?.msg || error.response?.data?.message || error.message || '操作失败'
            this.$message.error('创建失败: ' + errorMsg)
          }
        }
      })
    },
    resetForm() {
      this.form = {
        id: null,
        activityName: '',
        productId: null,
        groupPrice: null,
        requiredMembers: 3,
        validityHours: 24,
        startTime: '',
        endTime: '',
        activityDesc: '',
        merchantId: null
      }
      this.selectedProduct = null
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    async handleStatusChange(row, command) {
      logger.debug('handleStatusChange - command:', command, 'row:', row)
      
      if (command === 'delete') {
        this.$confirm('确定要删除该活动吗？删除后不可恢复。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          try {
            await Request.delete('/group-buy/activity/' + row.id)
            this.$message.success('删除成功')
            this.getList()
          } catch (error) {
            logger.error('删除失败:', error)
            const msg = error.response?.data?.msg || error.message || '删除失败'
            this.$message.error(msg)
          }
        }).catch(() => {
          logger.debug('取消删除')
        })
        return
      }

      // 状态映射：0-未发布 1-进行中 2-已结束 3-已下架
      const statusMap = {
        'publish': 1,   // 上架/发布（0→1 或 3→1）
        'start': 1,     // 开始（2→1，从已结束重新开始）
        'end': 2,       // 结束（1→2）
        'offline': 3    // 下架（1→3，下架后变为已下架状态）
      }
      const status = statusMap[command]
      
      if (status === undefined) {
        this.$message.error('未知操作')
        return
      }
      
      // 根据当前状态和目标状态显示不同的提示
      const actionText = {
        'publish': '上架',
        'start': '开始',
        'end': '结束',
        'offline': '下架'
      }[command] || '操作'
      
      try {
        logger.debug('========== 活动状态操作 ==========')
        logger.debug('操作:', actionText)
        logger.debug('活动ID:', row.id)
        logger.debug('当前状态:', row.status)
        logger.debug('目标状态:', status)
        logger.debug('请求URL: PUT /group-buy/activity/status/' + row.id + '?status=' + status)
        
        const res = await Request.put('/group-buy/activity/status/' + row.id, null, {
          params: { status }
        })
        
        logger.debug('后端响应:', res)
        logger.debug('响应code:', res.code)
        logger.debug('响应msg:', res.msg)
        
        // 检查返回结果的 code 字段（后端成功返回 code='0'）
        if (res.code === '0' || res.code === 0) {
          logger.debug('操作成功！')
          this.$message({
            type: 'success',
            message: actionText + '成功'
          })
          this.getList()
        } else {
          // 后端返回了错误信息
          logger.debug('操作失败！code:', res.code)
          const msg = res.msg || (actionText + '失败')
          
          // 如果是活动已过期的错误，提供快速编辑的选项
          if (msg.includes('活动已过期') || msg.includes('无法开启')) {
            this.$confirm(msg + ' 是否立即编辑活动时间？', '提示', {
              confirmButtonText: '立即编辑',
              cancelButtonText: '取消',
              type: 'warning'
            }).then(() => {
              // 打开编辑对话框
              this.handleEdit(row)
            }).catch(() => {
              // 用户取消
            })
          } else {
            // 其他错误直接显示
            this.$message({
              type: 'error',
              message: msg
            })
          }
        }
      } catch (error) {
        logger.error('========== 请求异常 ==========')
        logger.error('异常信息:', error)
        const msg = error.response?.data?.msg || error.message || (actionText + '失败')
        this.$message({
          type: 'error',
          message: msg
        })
      }
    },
    handleViewTeams(row) {
      this.currentActivityId = row.id
      this.teamsPage = 1
      this.getTeams(row.id)
      this.teamsDialogVisible = true
    },
    handleViewMembers(row) {
      this.getTeamMembers(row.id)
      this.membersDialogVisible = true
    },
    getStatusText(status) {
      const map = {
        0: '未发布',
        1: '进行中',
        2: '已结束',
        3: '已下架'
      }
      return map[status] || '未知'
    },
    getStatusType(status) {
      const map = {
        0: 'info',      // 未发布 - 灰色
        1: 'success',   // 进行中 - 绿色
        2: 'warning',   // 已结束 - 橙色
        3: 'danger'     // 已下架 - 红色
      }
      return map[status] || ''
    },
    getTeamStatusText(status) {
      const map = {
        0: '进行中',
        1: '已成团',
        2: '已失败'
      }
      return map[status] || '未知'
    },
    getTeamStatusType(status) {
      const map = {
        0: 'warning',
        1: 'success',
        2: 'info'
      }
      return map[status] || ''
    },
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hour = String(d.getHours()).padStart(2, '0')
      const minute = String(d.getMinutes()).padStart(2, '0')
      const second = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    }
  }
}
</script>

<style scoped>
.groupbuy-manager {
  padding: var(--spacing-xxl);
  background-color: var(--color-bg-page);
  min-height: calc(100vh - var(--layout-header-height));
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-xl);
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 600;
}

.operation-area {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 10px;
}

.operation-buttons {
  display: flex;
  gap: 10px;
}

.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.active-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.pending-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.end-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.offline-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-title {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  text-align: right;
}

.delete-btn {
  color: #f56c6c !important;
}

.delete-btn:hover {
  color: #f78989 !important;
}
</style>

