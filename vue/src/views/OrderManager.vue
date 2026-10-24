<template>
  <div class="order-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>订单管理</h2>
      <el-tag type="info">共 {{ loading || listError ? '—' : total }} 个订单</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.id" placeholder="请输入订单号" clearable></el-input>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option v-for="(label, value) in orderStatusMap" :key="value" :label="label" :value="Number(value)">
            </el-option>
          </el-select>
        </el-form-item>
        <!-- 管理员可以查看所有用户订单 -->
        <el-form-item label="用户账号" v-if="isAdmin">
          <el-input v-model="searchForm.userId" placeholder="请输入用户账号" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="default" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="default" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单统计卡片 -->
    <h3 class="page-statistics-title">本页订单概况</h3>
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-clock"></i></div>
                <div class="stat-title">待付款</div>
              </div>
              <div class="stat-value">{{ loading || listError ? '—' : getOrderCountByStatus(0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-box"></i></div>
                <div class="stat-title">待发货</div>
              </div>
              <div class="stat-value">{{ loading || listError ? '—' : getOrderCountByStatus(1) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-truck"></i></div>
                <div class="stat-title">已发货</div>
              </div>
              <div class="stat-value">{{ loading || listError ? '—' : getOrderCountByStatus(2) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="fas fa-check-circle"></i></div>
                <div class="stat-title">已完成</div>
              </div>
              <div class="stat-value">{{ loading || listError ? '—' : getOrderCountByStatus(3) }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 订单列表 -->
    <el-card class="table-card" shadow="hover">
      <div v-if="listError" role="alert" class="list-error">
        <p>订单加载失败，请重试。</p>
        <el-button type="primary" @click="getList">重新加载订单</el-button>
      </div>
      <el-table v-else v-loading="loading" :data="orders" border style="width: 100%" :default-sort="{prop: 'createdAt', order: 'descending'}">
        <el-table-column prop="id" label="订单号" width="120" sortable></el-table-column>
        <el-table-column label="商品信息" min-width="300">
          <template #default="{ row }">
            <div class="product-info">
              <el-image
                :src="productImage(row.product)"
                :preview-src-list="productImage(row.product) ? [productImage(row.product)] : []"
                fit="cover"
                style="width: 50px; height: 50px">
              </el-image>
              <div class="product-detail">
                <div class="product-name">{{ row.product?.name || '商品信息暂不可用' }}</div>
                <div class="product-price">
                  ¥{{ row.price.toFixed(2) }} × {{ row.quantity }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="订单金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalPrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="收货信息" width="200">
          <template #default="{ row }">
            <div>{{ row.recvAddress }}</div>
            <div>{{ row.recvPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.status)">
              {{ orderStatusMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" :sortable="true">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="isMerchant || isAdmin">
              <el-button v-if="canShip(row)" link size="small" :loading="shipPreparingId === row.id" :disabled="actionsBusy" @click="handleShip(row)">发货</el-button>
              <el-button v-if="canViewLogistics(row)" link size="small" :disabled="actionsBusy" @click="handleViewLogistics(row)">查看物流</el-button>
              <el-button v-if="row.status === 5" link size="small" :disabled="actionsBusy" @click="handleRefund(row)">处理退款</el-button>
            </template>
            <el-button link size="small" :disabled="actionsBusy" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0 && !listError" :total="total" v-model:page="queryParams.currentPage" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 添加退款处理对话框 -->
    <el-dialog title="退款处理" v-model="refundDialogVisible" width="500px" :close-on-click-modal="!refundSubmitting" :close-on-press-escape="!refundSubmitting" :show-close="!refundSubmitting">
      <div class="refund-info">
        <p>订单号：{{ currentOrder?.id }}</p>
        <p>退款金额：¥{{ currentOrder?.totalPrice }}</p>
        <p>退款原因：{{ currentOrder?.refundReason }}</p>
      </div>
      <div class="refund-operation">
        <el-alert v-if="isAutomaticRefund(currentOrder)" type="warning" :closable="false" title="此订单属于系统补偿退款，不能拒绝退款。" />
        <el-form ref="refundForm" :model="refundForm" :rules="refundRules" :disabled="refundSubmitting" label-width="80px">
          <el-form-item label="处理结果">
            <el-radio-group v-model="refundForm.status">
              <el-radio :value="6">同意退款</el-radio>
              <el-radio :value="7" :disabled="isAutomaticRefund(currentOrder)">拒绝退款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理备注" prop="remark">
            <el-input type="textarea" v-model="refundForm.remark" :rows="3" maxlength="255" show-word-limit></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="refundDialogVisible = false" :disabled="refundSubmitting">取 消</el-button>
          <el-button type="primary" @click="submitRefund" :loading="refundSubmitting" :disabled="refundSubmitting">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog title="订单详情" v-model="detailDialogVisible" width="600px">
      <div class="order-detail">
        <!-- 订单基本信息 -->
        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-item">
            <span class="label">订单编号：</span>
            <span>{{ currentOrder?.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">下单时间：</span>
            <span>{{ formatTime(currentOrder?.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">订单状态：</span>
            <el-tag :type="getOrderStatusType(currentOrder?.status)">
              {{ orderStatusMap[currentOrder?.status] }}
            </el-tag>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="detail-section">
          <h4>商品信息</h4>
          <div class="product-detail-info">
            <el-image
              :src="productImage(currentOrder?.product)"
              :preview-src-list="productImage(currentOrder?.product) ? [productImage(currentOrder?.product)] : []"
              fit="cover"
              style="width: 80px; height: 80px; border-radius: 4px;">
            </el-image>
            <div class="product-info-right">
              <div class="product-name">{{ currentOrder?.product?.name }}</div>
              <div class="product-price">
                <span>单价：¥{{ currentOrder?.price }}</span>
                <span class="quantity">数量：{{ currentOrder?.quantity }}</span>
              </div>
              <div class="total-price">
                总价：¥{{ currentOrder?.totalPrice }}
              </div>
            </div>
          </div>
        </div>

        <!-- 收货信息 -->
        <div class="detail-section">
          <h4>收货信息</h4>
          <div class="detail-item">
            <span class="label">收货人：</span>
            <span>{{ currentOrder?.recvName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话：</span>
            <span>{{ currentOrder?.recvPhone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">收货地址：</span>
            <span>{{ currentOrder?.recvAddress }}</span>
          </div>
        </div>

        <!-- 添加商户信息部分 -->
        <div class="detail-section">
          <h4>商户信息</h4>
          <div class="detail-item">
            <span class="label">商户姓名：</span>
            <span>{{ currentOrder?.merchant?.name }}</span>
          </div>
        </div>

        <!-- 退款信息(如果有) -->
        <div class="detail-section" v-if="currentOrder?.refundStatus > 0">
          <h4>退款信息</h4>
          <div class="detail-item">
            <span class="label">退款状态：</span>
            <el-tag :type="getRefundStatusType(currentOrder?.refundStatus)">
              {{ refundStatusMap[currentOrder?.refundStatus] }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">退款原因：</span>
            <span>{{ currentOrder?.refundReason }}</span>
          </div>
          <div class="detail-item" v-if="currentOrder?.refundTime">
            <span class="label">退款时间：</span>
            <span>{{ formatTime(currentOrder?.refundTime) }}</span>
          </div>
          <div class="detail-item" v-if="currentOrder?.remark">
            <span class="label">处理备注：</span>
            <span>{{ currentOrder?.remark }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 修改发货对话框 -->
    <el-dialog title="发货处理" v-model="shipDialogVisible" width="500px" :close-on-click-modal="!shipSubmitting" :close-on-press-escape="!shipSubmitting" :show-close="!shipSubmitting">
      <el-form :model="shipForm" :rules="shipRules" ref="shipForm" :disabled="shipSubmitting" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="shipForm.receiverName" disabled></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="shipForm.receiverPhone" disabled></el-input>
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input type="textarea" v-model="shipForm.receiverAddress" disabled :rows="2"></el-input>
        </el-form-item>
        <el-form-item label="预计到达" prop="expectedArrivalTime">
          <!-- 改用文本输入，避免日期控件被环境/插件强制写入占位符 -->
          <el-input
            v-model="shipForm.expectedArrivalTime"
            placeholder="请输入预计到达时间，格式：yyyy-MM-dd HH:mm:ss"
            clearable>
          </el-input>
        </el-form-item>
        <el-form-item label="快递公司" prop="companyName">
          <el-select v-model="shipForm.companyName" placeholder="请选择快递公司">
            <el-option label="顺丰快递" value="顺丰快递"></el-option>
            <el-option label="中通快递" value="中通快递"></el-option>
            <el-option label="圆通快递" value="圆通快递"></el-option>
            <el-option label="韵达快递" value="韵达快递"></el-option>
            <el-option label="申通快递" value="申通快递"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="trackingNumber">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入快递单号"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="shipDialogVisible = false" :disabled="shipSubmitting">取 消</el-button>
          <el-button type="primary" @click="submitShip" :loading="shipSubmitting" :disabled="shipSubmitting">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加物流信息对话框 -->
    <el-dialog title="物流信息" v-model="logisticsDialogVisible" width="600px">
      <div class="logistics-info" v-if="logisticsInfo">
        <div class="logistics-header">
          <div class="company-info">
            <span class="label">快递公司：</span>
            <span>{{ logisticsInfo.companyName }}</span>
          </div>
          <div class="tracking-info">
            <span class="label">快递单号：</span>
            <span>{{ logisticsInfo.trackingNumber }}</span>
          </div>
          <div class="status-info">
            <span class="label">物流状态：</span>
            <el-tag :type="getLogisticsStatusType(logisticsInfo.status)">
              {{ logisticsStatusMap[logisticsInfo.status] }}
            </el-tag>
          </div>
          <div class="time-info">
            <div>
              <span class="label">创建时间：</span>
              <span>{{ formatTime(logisticsInfo.createdAt) }}</span>
            </div>
            <div>
              <span class="label">更新时间：</span>
              <span>{{ formatTime(logisticsInfo.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div class="logistics-address">
          <h4>收货信息</h4>
          <div class="address-content">
            <p><span class="label">收货人：</span>{{ logisticsInfo.receiverName }}</p>
            <p><span class="label">联系电话：</span>{{ logisticsInfo.receiverPhone }}</p>
            <p><span class="label">收货地址：</span>{{ logisticsInfo.receiverAddress }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import logger from '@/utils/logger'
import Pagination from '@/components/Pagination/index.vue'
import { formatTime } from '@/utils/time'  // 导入时间格式化函数

export default {
  name: 'OrderManager',
  components: {
    Pagination
  },
  inject: ['userInfo'],
  data() {
    return {
      // 搜索表单
      searchForm: {
        id: undefined,
        status: undefined,
        userId: undefined
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 订单列表
      orders: [],
      loading: false,
      listError: false,
      listSequence: 0,
      shipPreparingId: null,
      shipSubmitting: false,
      refundSubmitting: false,
      refundRules: {
        remark: [{ required: true, whitespace: true, max: 255, message: '请填写处理备注（最多255字）', trigger: 'blur' }]
      },
      // 总数
      total: 0,
      // 订单状态映射
      orderStatusMap: {
        0: '待支付',
        1: '已支付',
        2: '已发货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款',
        7: '退款未通过'
      },
      // 添加退款相关数据
      refundDialogVisible: false,
      currentOrder: null,
      refundForm: {
        status: 6,
        remark: ''
      },
      detailDialogVisible: false,
      refundStatusMap: {
        0: '无退款',
        1: '申请退款',
        2: '退款中',
        3: '已退款',
        4: '退款未通过',
        5: '系统补偿待退款'
      },
      // 修改发货对话框
      shipDialogVisible: false,
      logisticsDialogVisible: false,
      shipForm: {
        orderId: null,
        receiverName: '',
        receiverPhone: '',
        receiverAddress: '',
        companyName: '',
        trackingNumber: '',
        expectedArrivalTime: '',
      },
      shipRules: {
        companyName: [
          { required: true, message: '请选择快递公司', trigger: 'change' }
        ],
        trackingNumber: [
          { required: true, whitespace: true, message: '请输入快递单号', trigger: 'blur' }
        ]
      },
      logisticsInfo: null,
      logisticsStatusMap: {
        0: '待发货',
        1: '已发货',
        2: '已签收',
        3: '已取消'
      }
    }
  },
  computed: {
    actionsBusy() {
      return this.shipPreparingId !== null || this.shipSubmitting || this.refundSubmitting
    },
    // 判断是否为管理员
    isAdmin() {
      return this.userInfo?.role === 'ADMIN'
    },
    // 判断是否为商户
    isMerchant() {
      return this.userInfo?.role === 'MERCHANT'
    }
  },
  created() {
    this.getList()
  },
  beforeUnmount() { this.listSequence++ },
  methods: {
    productImage(product) {
      const url = product?.imageUrl
      return typeof url === 'string' && url ? (url.startsWith('http') ? url : `/api${url}`) : ''
    },
    fulfillmentStatus(order) {
      return order?.status === 7 ? order.lastStatus : order?.status
    },
    canShip(order) { return this.fulfillmentStatus(order) === 1 },
    canViewLogistics(order) {
      return [2, 3].includes(order.status) || ([5, 6, 7].includes(order.status) && order.lastStatus === 2)
    },
    isAutomaticRefund(order) {
      return order?.refundStatus === 5 && (order.refundReason?.startsWith('拼团失败自动退款') || order.refundReason === '支付后无法履约自动退款')
    },
    // 获取订单列表
    async getList() {
      const sequence = ++this.listSequence
      this.loading = true
      this.listError = false
      try {
        const params = { ...this.queryParams, ...this.searchForm }
        if (!this.isAdmin) params.merchantId = this.userInfo.id
        const res = await Request.get('/order/page', { params })
        if (sequence !== this.listSequence) return
        if (res.code !== '0' || !res.data || !Number.isSafeInteger(res.data.total) || res.data.total < 0
            || (!Array.isArray(res.data.records) && !(res.data.total === 0 && res.data.records == null))) throw new Error('订单数据暂不可用')
        this.orders = res.data.records || []
        this.total = res.data.total
      } catch (error) {
        if (sequence !== this.listSequence) return
        logger.error('获取订单列表失败:', error)
        this.orders = []
        this.total = 0
        this.listError = true
      } finally {
        if (sequence === this.listSequence) this.loading = false
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
        id: '',
        status: '',
        userId: ''
      }
      this.handleSearch()
    },
    // 获取订单状态类型
    getOrderStatusType(status) {
      const typeMap = {
        0: 'warning',   // 待支付
        1: 'primary',   // 已支付
        2: 'success',   // 已发货
        3: 'info',      // 已完成
        4: 'danger',    // 已取消
        5: 'warning',   // 退款中
        6: 'info',      // 已退款
        7: 'danger'     // 退款失败
      }
      return typeMap[status] || 'info'
    },
    // 获取指定状态的订单数量
    getOrderCountByStatus(status) {
      return this.orders.filter(order => this.fulfillmentStatus(order) === status).length
    },
    // Group orders must have completed their team before shipment.
    async handleShip(order) {
      if (this.actionsBusy || !(this.isAdmin || this.isMerchant) || !this.canShip(order)) return
      this.shipPreparingId = order.id
      try {
        if (order.orderType === 1) {
          if (!order.groupBuyTeamId) { this.$message.warning('拼团信息不完整，暂不能发货'); return }
          const res = await Request.get(`/group-buy/team/${order.groupBuyTeamId}`)
          if (res.code !== '0' || res.data?.status !== 1) { this.$message.warning('拼团尚未成功，暂不能发货'); return }
        }
        this.currentOrder = order
        this.shipForm = { orderId: order.id, receiverName: order.recvName, receiverPhone: order.recvPhone,
          receiverAddress: order.recvAddress, companyName: '', trackingNumber: '', expectedArrivalTime: '' }
        this.shipDialogVisible = true
        this.$nextTick(() => this.$refs.shipForm?.clearValidate())
      } catch (error) {
        logger.error('核对发货条件失败:', error)
        this.$message.error('暂时无法核对发货条件，请重试')
      } finally { this.shipPreparingId = null }
    },
    // 查看详情
    handleDetail(order) {
      if (this.actionsBusy) return
      this.currentOrder = order
      this.detailDialogVisible = true
    },
    // 处理退款
    handleRefund(order) {
      if (this.actionsBusy || !(this.isAdmin || this.isMerchant) || order.status !== 5) return
      this.currentOrder = order
      this.refundForm = {
        status: 6,
        remark: ''
      }
      this.refundDialogVisible = true
      this.$nextTick(() => this.$refs.refundForm?.clearValidate())
    },
    // 提交退款处理
    async submitRefund() {
      if (this.actionsBusy || !(this.isAdmin || this.isMerchant) || this.currentOrder?.status !== 5) return
      if (this.isAutomaticRefund(this.currentOrder) && this.refundForm.status === 7) { this.$message.warning('系统补偿退款不能拒绝'); return }
      this.refundSubmitting = true
      try {
        try { await this.$refs.refundForm.validate() } catch { return }
        const orderId = this.currentOrder.id
        const decision = this.refundForm.status
        const res = await Request.put(`/order/${orderId}/handle-refund`, null, {
          params: { status: decision, remark: this.refundForm.remark.trim() }
        })
        if (res.code !== '0') { this.$message.error(res.msg || '退款处理失败'); return }
        this.$message.success(decision === 6 ? '退款已通过' : '退款已拒绝')
        this.refundDialogVisible = false
        await this.getList()
      } catch (error) {
        logger.error('退款处理失败:', error)
        this.$message.error(error.response?.data?.msg || '退款结果未确认，请刷新后核对退款状态')
      } finally { this.refundSubmitting = false }
    },
    // 获取退款状态对应的类型
    getRefundStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'warning',
        3: 'success',
        4: 'danger'
      }
      return typeMap[status] || 'info'
    },
    // 添加格式化时间方法
    formatTime,
    // Creating logistics and advancing the order are one backend transaction.
    async submitShip() {
      if (this.actionsBusy || !(this.isAdmin || this.isMerchant) || !this.canShip(this.currentOrder)) return
      this.shipSubmitting = true
      let loading = null
      try {
        try { await this.$refs.shipForm.validate() } catch { return }
        const payload = { ...this.shipForm, companyName: this.shipForm.companyName.trim(),
          trackingNumber: this.shipForm.trackingNumber.trim(), expectedArrivalTime: this.shipForm.expectedArrivalTime.trim() || null }
        loading = this.$loading({ lock: true, text: '正在处理发货请求...' })
        const res = await Request.post('/logistics', payload)
        if (res.code !== '0') { this.$message.error(res.msg || '发货处理失败'); return }
        this.$message.success('发货成功')
        this.shipDialogVisible = false
        await this.getList()
      } catch (error) {
        logger.error('发货处理过程中出错:', error)
        this.$message.error(error.response?.data?.msg || '发货结果未确认，请刷新订单或查看物流后再处理')
      } finally {
        if (loading) loading.close()
        this.shipSubmitting = false
      }
    },
    // 查看物流信息
    async handleViewLogistics(order) {
      try {
        // 显示加载中
        const loading = this.$loading({
          lock: true,
          text: '正在获取物流信息...',
          spinner: 'fas fa-spinner fa-spin',
          background: 'rgba(0, 0, 0, 0.7)'
        });

        try {
          const res = await Request.get(`/logistics/order/${order.id}`)

          if (res.code === '0' && res.data) {
            this.logisticsInfo = res.data
            // 添加物流时间显示
            this.logisticsInfo.createdTime = formatTime(res.data.createdAt)
            this.logisticsInfo.updatedTime = formatTime(res.data.updatedAt)
            this.logisticsDialogVisible = true
          } else {
            this.$message({
              type: 'warning',
              message: res.msg || '暂无物流信息',
              duration: 3000
            })
          }
        } finally {
          // 关闭加载提示
          loading.close();
        }
      } catch (error) {
        logger.error('获取物流信息失败:', error)
        this.$message.error('获取物流信息失败，请稍后重试')
      }
    },
    // 获取物流状态对应的类型
    getLogisticsStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'primary',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    }
  }
}
</script>

<style scoped>
.page-statistics-title { margin: 20px 0 12px; font-size: 15px; color: #606266; }
.list-error { padding: 24px; text-align: center; }

.order-manager {
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

.operation-area {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
  margin-bottom: var(--spacing-xxl);
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
  gap: var(--spacing-sm);
}

.stat-icon {
  font-size: var(--font-size-extra-large);
  color: var(--color-primary);
}

.stat-title {
  font-size: var(--font-size-base);
  color: var(--color-text-regular);
}

.stat-value {
  font-size: var(--font-size-extra-large);
  font-weight: var(--font-weight-primary);
  color: var(--color-text-primary);
}

.table-card {
  border-radius: var(--border-radius-large);
  box-shadow: var(--box-shadow-base);
}

.product-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.product-name {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.product-price {
  font-size: var(--font-size-small);
  color: var(--color-text-secondary);
}

.refund-info {
  padding: var(--spacing-xl);
  background: var(--color-bg-base);
  border-radius: var(--border-radius-large);
  margin-bottom: var(--spacing-xl);
}

.refund-info p {
  margin: var(--spacing-sm) 0;
  color: var(--color-text-regular);
}

.refund-operation {
  padding: 0 var(--spacing-xl);
}

.order-detail {
  padding: var(--spacing-xl);
}

.detail-section {
  margin-bottom: var(--spacing-xxl);
  padding-bottom: var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-lighter);
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.detail-section h4 {
  font-size: var(--font-size-medium);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-lg) 0;
  font-weight: var(--font-weight-primary);
}

.detail-item {
  display: flex;
  margin-bottom: var(--spacing-md);
  font-size: var(--font-size-base);
  line-height: var(--line-height-base);
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .label {
  width: 100px;
  color: var(--color-text-secondary);
}

.product-detail-info {
  display: flex;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: var(--color-bg-base);
  border-radius: var(--border-radius-large);
}

.product-info-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.product-name {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-primary);
}

.product-price {
  color: var(--color-text-regular);
  font-size: var(--font-size-small);
  display: flex;
  gap: var(--spacing-lg);
}

.total-price {
  color: var(--color-danger);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-primary);
}

/* 添加物流信息相关样式 */
.logistics-info {
  padding: var(--spacing-xl);
}

.logistics-header {
  background: var(--color-bg-base);
  padding: var(--spacing-xl);
  border-radius: var(--border-radius-large);
  margin-bottom: var(--spacing-xl);
}

.logistics-header > div {
  margin-bottom: var(--spacing-md);
}

.logistics-header > div:last-child {
  margin-bottom: 0;
}

.logistics-header .label {
  color: var(--color-text-secondary);
  margin-right: var(--spacing-sm);
}

.time-info {
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px dashed var(--color-border-light);
}

.time-info > div {
  margin-bottom: var(--spacing-sm);
}

.logistics-address {
  background: var(--color-bg-white);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
}

.logistics-address h4 {
  font-size: var(--font-size-medium);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-lg) 0;
}

.address-content p {
  margin: var(--spacing-sm) 0;
  color: var(--color-text-regular);
}
</style>
