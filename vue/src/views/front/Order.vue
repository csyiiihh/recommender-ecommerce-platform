<template>
  <div class="order-page landscape-bg">
    <front-header></front-header>
    <div class="main-content cloud-decoration">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="header-left">
          <div class="title-wrapper">
            <div class="title-icon">
              <i class="fas fa-file-invoice"></i>
              <div class="icon-pulse"></div>
            </div>
            <div class="title-content">
        <h2>我的订单</h2>
              <div class="header-subtitle">查看和管理您的所有订单信息</div>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="order-stats">
            <div class="stats-item">
              <i class="fas fa-receipt"></i>
              <span class="stats-label">总订单</span>
              <span class="stats-value">{{ orderSummary?.total ?? '—' }}</span>
            </div>
            <div class="stats-item unpaid-stats">
              <i class="fas fa-credit-card"></i>
              <span class="stats-label">未支付</span>
              <span class="stats-value">{{ orderSummary?.unpaid ?? '—' }}</span>
              <div class="stats-tooltip">需要完成支付的订单</div>
            </div>
            <div class="stats-item">
              <i class="fas fa-clock"></i>
              <span class="stats-label">待处理</span>
              <span class="stats-value">{{ orderSummary?.pending ?? '—' }}</span>
              <div class="stats-tooltip">待支付和已支付订单</div>
            </div>
          </div>
          <div v-if="summaryError" class="summary-error" role="status">
            统计暂不可用 <el-button link type="primary" :loading="summaryLoading" @click="getOrderStatistics">重试统计</el-button>
          </div>
        </div>
      </div>

      <!-- 订单列表 -->
      <div class="order-content" v-loading="loading">
        <div v-if="listError && !loading" class="order-load-error" role="alert">
          <h3>订单加载失败</h3>
          <p>暂时无法获取订单，请稍后重试。</p>
          <el-button type="primary" @click="getOrders">重新加载订单</el-button>
        </div>
        <!-- 空订单提示 -->
        <div v-else-if="!loading && orders.length === 0" class="empty-orders">
          <div class="empty-animation">
            <div class="empty-icon">
              <i class="fas fa-file-invoice"></i>
              <div class="empty-particles">
                <div class="particle"></div>
                <div class="particle"></div>
                <div class="particle"></div>
                <div class="particle"></div>
            </div>
            </div>
          </div>
          <div class="empty-content">
            <h3>您还没有任何订单</h3>
            <p>探索我们的精选图书，开始您的购物之旅</p>
            <div class="empty-actions">
              <el-button type="primary" size="large" @click="$router.push('/products')">
                <i class="fas fa-shopping-cart"></i>
                立即购物
          </el-button>
              <el-button size="large" @click="$router.push('/')">
                <i class="fas fa-home"></i>
                返回首页
              </el-button>
            </div>
          </div>
        </div>

        <!-- 订单列表 -->
        <div v-else class="order-list">
          <transition-group name="order-item" tag="div">
            <div v-for="(order, index) in orders" :key="order.id" class="order-item" :style="{ 'animation-delay': (index * 0.1) + 's' }">
            <div class="order-header">
                <div class="order-info">
                  <div class="order-id-wrapper">
                    <i class="fas fa-hashtag"></i>
                    <span class="order-id">{{ order.id }}</span>
                  </div>
                  <div class="order-time-wrapper">
                    <i class="fas fa-calendar-alt"></i>
              <span class="order-time">{{ formatTime(order.createdAt) }}</span>
                  </div>
                </div>
              <div class="status-wrapper">
                  <div class="order-status" :class="getStatusClass(order.status)">
                    <div class="status-icon">
                      <i :class="getStatusIcon(order.status)"></i>
                    </div>
                    <span class="status-text">{{ getStatusText(order.status) }}</span>
                  </div>
              </div>
            </div>

              <div v-if="order.status === 5" class="refund-progress-note" role="status">
                <strong>退款处理中</strong>
                <span>{{ order.refundReason || '退款申请已提交，等待处理' }}</span>
                <small v-if="order.refundStatus === 5 && (order.refundReason?.startsWith('拼团失败自动退款') || order.refundReason === '支付后无法履约自动退款')">系统将自动处理，失败后会继续重试，无需重复提交。退款到账时间以支付渠道为准。</small>
              </div>
              <div v-if="order.status === 7" class="refund-progress-note" role="status">
                <strong>退款申请未通过</strong>
                <span>订单将继续按原状态处理，请关注后续履约进度。</span>
              </div>
              <!-- 订单图书和收货信息 -->
              <div class="order-body">
                <div class="product-section">
                  <div class="product-image-wrapper">
                <el-image :src="productImage(order.product)" fit="cover"
                  class="product-image">
                  <template #error>
                    <div class="image-slot">
                      <i class="fas fa-image"></i>
                    </div>
                  </template>
                </el-image>
                    <div class="image-overlay">
                      <i class="fas fa-eye"></i>
                    </div>
                  </div>
                <div class="product-details">
                  <h3 class="product-name">
                    {{ order.product?.name || '商品信息暂不可用' }}
                    <!-- 拼团标识 -->
                    <el-tag v-if="order.orderType === 1" type="warning" size="small" style="margin-left: 8px;">
                      <i class="fas fa-users"></i> 拼团
                    </el-tag>
                  </h3>
                    <div class="product-price-info">
                      <div class="price-container">
                        <span class="current-price">
                      ¥{{ Number(order.price || 0).toFixed(2) }}
                    </span>
                      </div>
                      <div class="quantity-info">
                        <span class="quantity-label">数量:</span>
                        <span class="quantity-value">{{ order.quantity }}</span>
                        <!-- 拼团限购提示 -->
                        <el-tag v-if="order.orderType === 1" type="warning" size="small" style="margin-left: 8px;">
                          限购1件
                        </el-tag>
                      </div>
                    </div>
                    <p v-if="order.status === 0" class="order-purchase-note">
                      如需调整数量，请取消后重新下单。
                    </p>
                </div>
              </div>

                <!-- 收货信息 -->
                <div class="delivery-section">
                  <div class="section-header">
                    <i class="fas fa-shipping-fast"></i>
                    <span>收货信息</span>
                  </div>
                  <div class="delivery-content">
                    <div class="info-row">
                  <div class="info-item">
                        <i class="fas fa-user"></i>
                        <span class="info-label">收货人:</span>
                        <span class="info-value">{{ order.recvName || '暂无收货人' }}</span>
                  </div>
                  <div class="info-item">
                        <i class="fas fa-phone"></i>
                        <span class="info-label">联系电话:</span>
                        <span class="info-value">{{ order.recvPhone || '暂无联系电话' }}</span>
                      </div>
                    </div>
                    <div class="info-row">
                      <div class="info-item address-item">
                        <i class="fas fa-map-marker-alt"></i>
                        <span class="info-label">收货地址:</span>
                        <span class="info-value">{{ order.recvAddress || '暂无收货地址' }}</span>
                      </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="order-footer">
                <div class="footer-left">
              <div class="order-total">
                    <span class="total-label">订单总计:</span>
                    <div class="total-price">
                      <span class="currency">¥</span>
                      <span class="amount">{{ order.totalPrice.toFixed(2) }}</span>
              </div>
                  </div>
                </div>
                <div class="footer-right">
              <div class="order-actions">
                    <!-- 主要操作按钮 -->
                    <div class="primary-actions">
                      <el-button v-if="order.status === 0 && order.product?.id" type="primary" :loading="payingOrderId === order.id" :disabled="payingOrderId !== null" @click="handlePayment(order)" class="action-btn pay-btn">
                        <i class="fas fa-credit-card"></i>
                  立即支付
                </el-button>
                      <el-button v-if="fulfillmentStatus(order) === 2" type="success" @click="handleConfirm(order)" class="action-btn confirm-btn">
                        <i class="fas fa-check-circle"></i>
                  确认收货
                </el-button>
                      <el-button v-if="order.status === 3 && order.product?.id" @click="handleReview(order)" class="action-btn review-btn">
                        <i class="fas fa-star"></i>
                        图书评价
                </el-button>
                    </div>
                    
                    <!-- 次要操作按钮 -->
                    <div class="secondary-actions">
                      <el-button v-if="[1, 2].includes(order.status)" link @click="showRefundDialog(order)" class="text-btn refund-btn">
                        <i class="fas fa-undo"></i>
                  申请退款
                </el-button>
                      <el-button v-if="order.status === 0" link @click="handleCancel(order)" class="text-btn cancel-btn">
                        <i class="fas fa-times"></i>
                  取消订单
                </el-button>
                      <el-button v-if="[0, 1].includes(order.status)" link @click="showAddressDialog(order)" class="text-btn address-btn">
                        <i class="fas fa-edit"></i>
                  修改收货信息
                </el-button>
                      <el-button v-if="canViewLogistics(order)" link @click="handleViewLogistics(order)" class="text-btn logistics-btn">
                        <i class="fas fa-truck"></i>
                  查看物流
                </el-button>

              </div>
            </div>
          </div>
              </div>
            </div>
          </transition-group>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0 && !listError">
          <el-pagination background v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next, jumper"
            @current-change="getOrders">
          </el-pagination>
        </div>
      </div>
    </div>
    <front-footer></front-footer>

    <!-- 修改收货信息对话框 -->
    <el-dialog title="修改收货信息" v-model="addressDialogVisible" width="500px" custom-class="address-dialog">
      <div v-if="!showAddressForm">
        <!-- 已有地址列表 -->
        <div class="address-list-section">
          <div class="section-header">
            <div class="section-title">
              <i class="fas fa-map-marker-alt"></i>
              选择收货地址
            </div>
            <el-button type="primary" plain size="small" @click="showAddressForm = true">
              <el-icon><Plus /></el-icon>
              新增地址
            </el-button>
          </div>
          <div class="address-list">
            <el-radio-group v-model="selectedAddressId" class="address-radio-group">
              <el-radio v-for="addr in addresses" :key="addr.id" :value="addr.id" class="address-radio-item">
                <div class="address-content">
                  <div class="address-info">
                    <div class="contact-info">
                      <span class="receiver">{{ addr.receiver }}</span>
                      <span class="phone">{{ addr.phone }}</span>
                      <el-tag size="small" type="success" v-if="addr.isDefault">默认地址</el-tag>
                    </div>
                    <div class="address-detail">{{ addr.address }}</div>
                  </div>
                  <div class="address-actions">
                    <el-button link size="small" @click.stop="editAddress(addr)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button link size="small" @click.stop="deleteAddress(addr)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </div>
      </div>

      <!-- 新增地址表单 -->
      <div v-else>
        <div class="form-header">
          <div class="section-title">
            <el-icon><Plus /></el-icon>
            新增地址
          </div>
          <el-button link @click="showAddressForm = false">
            <el-icon><ArrowLeft /></el-icon>
            返回地址列表
          </el-button>
        </div>
        <el-form :model="addressForm" :rules="addressRules" ref="addressForm" label-width="80px" class="address-form">
          <el-form-item label="收货人" prop="receiver">
            <el-input v-model="addressForm.receiver" placeholder="请输入收货人">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="addressForm.phone" placeholder="请输入联系电话">
              <template #prefix>
                <el-icon><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="收货地址" prop="address">
            <el-input type="textarea" v-model="addressForm.address" :rows="3" placeholder="请输入详细收货地址">
              <template #prefix>
                <el-icon><Location /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="saveToAddressBook">保存到地址簿</el-checkbox>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateOrderAddress">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改退款申请对话框 -->
    <el-dialog title="申请退款" v-model="refundDialogVisible" width="400px">
      <el-form :model="refundForm" :rules="refundRules" ref="refundForm" label-width="80px">
        <el-form-item label="退款原因" prop="reason">
          <el-select v-model="refundForm.reason" placeholder="请选择退款原因" style="width: 100%">
            <el-option label="图书质量问题" value="图书质量问题"></el-option>
            <el-option label="图书与描述不符" value="图书与描述不符"></el-option>
            <el-option label="发货太慢" value="发货太慢"></el-option>
            <el-option label="其他原因" value="其他原因"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="refundDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitRefund">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改评价对话框 -->
    <el-dialog title="图书评价" v-model="reviewDialogVisible" width="500px">
      <div v-if="currentOrder" class="review-product-info">
        <el-image :src="productImage(currentOrder.product)" fit="cover"
          class="review-product-image">
        </el-image>
        <div class="review-product-detail">
          <div class="review-product-name">{{ currentOrder.product.name }}</div>
          <div class="review-product-price">
            <span class="price">¥{{ currentOrder.price.toFixed(2) }}</span>
            <span class="quantity">x {{ currentOrder.quantity }}</span>
          </div>
        </div>
      </div>

      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewForm" label-width="80px" class="review-form" v-if="currentOrder">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="reviewForm.rating" :colors="['#9aaa92', '#F7BA2A', '#FF9900']" show-text :texts="['很差', '较差', '一般', '较好', '很好']">
          </el-rate>
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input type="textarea" v-model="reviewForm.content" :rows="4" placeholder="请分享您对图书的阅读体验..."></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview">提交评价</el-button>
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
        <div class="logistics-info-item">
          <span class="label">预计到达：</span>
          <span>{{ formatTime(logisticsInfo.expectedArrivalTime) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { h } from 'vue'
import { readFrontUser } from '@/utils/favoriteState'
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { formatTime } from '@/utils/time'
import { Plus, Edit, Delete, ArrowLeft, User, Phone, Location } from '@element-plus/icons-vue'

export default {
  name: 'OrderView',
  components: {
    FrontHeader,
    FrontFooter,
    Plus,
    Edit,
    Delete,
    ArrowLeft,
    User,
    Phone,
    Location
  },
  data() {
    return {
      userInfo: (readFrontUser() || {}),
      loading: false,
      payingOrderId: null,
      orders: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      orderSummary: null,
      summaryLoading: false,
      summaryError: false,
      listError: false,
      listSequence: 0,
      summarySequence: 0,
      paymentDialogVisible: false,
      paymentMethod: 'balance',
      currentOrder: null,
      addressDialogVisible: false,
      selectedAddressId: null,
      addresses: [],
      saveToAddressBook: false,
      addressForm: {
        id: '',
        receiver: '',
        phone: '',
        address: ''
      },
      currentEditOrder: null,
      addressRules: {
        receiver: [
          { required: true, message: '请输入收货人', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入收货地址', trigger: 'blur' }
        ]
      },
      showAddressForm: false,
      refundDialogVisible: false,
      refundForm: {
        reason: ''
      },
      refundRules: {
        reason: [
          { required: true, message: '请选择退款原因', trigger: 'change' }
        ]
      },
      currentRefundOrder: null,
      reviewDialogVisible: false,
      reviewForm: {
        rating: 5,
        content: ''
      },
      reviewRules: {
        rating: [
          { required: true, message: '请选择评分', trigger: 'change' }
        ],
        content: [
          { required: true, message: '请填写评价内容', trigger: 'blur' },
          { min: 5, max: 500, message: '评价内容长度在 5 到 500 个字符', trigger: 'blur' }
        ]
      },
      logisticsDialogVisible: false,
      logisticsInfo: null,
      logisticsStatusMap: {
        0: '待发货',
        1: '已发货',
        2: '已签收',
        3: '已取消'
      }
    }
  },
  created() {
    this.getOrders()
  },
  beforeUnmount() {
    this.listSequence++
    this.summarySequence++
  },
  mounted() {
    this.userInfo = (readFrontUser() || {})
  },
  methods: {
    isLogin() {
      const userStr = localStorage.getItem('frontUser')
      if (!userStr) {
        this.$message({
          type: 'warning',
          message: '请先登录'
        })
        this.$router.push('/login')
        return false
      }
      return true
    },
    formatTime,
    fulfillmentStatus(order) {
      return order.status === 7 ? order.lastStatus : order.status
    },
    canViewLogistics(order) {
      return [2, 3].includes(order.status) || ([5, 6, 7].includes(order.status) && order.lastStatus === 2)
    },
    productImage(product) {
      const url = product?.imageUrl
      if (typeof url !== 'string' || !url) return ''
      return url.startsWith('http') ? url : `/api${url}`
    },
    async getOrders() {
      if (!this.isLogin()) return
      const sequence = ++this.listSequence
      this.loading = true
      this.listError = false
      this.getOrderStatistics()
      try {
        const res = await Request.get('/order/page', { params: {
          userId: this.userInfo.id, currentPage: this.currentPage, size: this.pageSize
        } })
        if (sequence !== this.listSequence) return
        if (res.code !== '0' || !Array.isArray(res.data?.records)) throw new Error('订单数据暂不可用')
        this.orders = res.data.records
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
    async getOrderStatistics() {
      const sequence = ++this.summarySequence
      this.summaryLoading = true
      this.summaryError = false
      this.orderSummary = null
      try {
        const res = await Request.get('/order/my-summary')
        if (sequence !== this.summarySequence) return
        const data = res.data
        if (res.code !== '0' || !data || !['total', 'unpaid', 'pending'].every(key => Number.isSafeInteger(data[key]) && data[key] >= 0)
            || data.unpaid > data.pending || data.pending > data.total) throw new Error('订单统计暂不可用')
        this.orderSummary = data
      } catch (error) {
        if (sequence !== this.summarySequence) return
        logger.debug('订单统计暂不可用:', error.message)
        this.summaryError = true
      } finally {
        if (sequence === this.summarySequence) this.summaryLoading = false
      }
    },

      getStatusText(status) {
        const statusMap = {
          0: '待支付',
          1: '已支付',
          2: '已发货',
          3: '已完成',
          4: '已取消',
          5: '退款中',
          6: '已退款',
          7: '退款未通过'
        }
        return statusMap[status] || '未知状态'
      },

      getStatusClass(status) {
        const classMap = {
          0: 'pending',
          1: 'paid',
          2: 'shipped',
          3: 'completed',
          4: 'cancelled',
          5: 'refunding',
          6: 'refunded',
          7: 'refund-failed'
        }
        return classMap[status] || ''
      },

      async handleConfirm(order) {
        try {
          await this.$confirm('确认已收到图书吗？', '提示', {
            type: 'warning'
          })
          const res = await Request.put(`/order/${order.id}/status?status=3`);
          if (res.code === '0') {
            this.$message.success('确认收货成功')
            this.getOrders() // 这会自动更新待处理订单数量
          }
        } catch (error) {
          if (error !== 'cancel') {
            logger.error('确认收货失败:', error)
            this.$message.error('确认收货失败')
          }
        }
      },

      handleReview(order) {
        if (!order.product?.id) { this.$message.warning('商品信息暂不可用'); return }
        // 确保order包含完整的商品信息
        this.currentOrder = {
          ...order,
          product: {
            ...order.product
          }
        }
        this.reviewForm = {
          rating: 5,
          content: ''
        }
        this.reviewDialogVisible = true
      },

    async handleCancel(order) {
        this.isLogin()
        try {
          await this.$confirm('确定要取消订单吗？', '提示', {
            type: 'warning'
          })
          const res = await Request.put(`/order/${order.id}/status?status=4`);
          if (res.code === '0') {
            this.$message.success('订单已取消')
            this.getOrders() // 这会自动更新待处理订单数量
          }
        } catch (error) {
          if (error !== 'cancel') {
            logger.error('取消订单失败:', error)
            this.$message.error('取消订单失败')
          }
        }
      },

    async getAddresses() {
        this.isLogin()
        try {
          const userId = this.userInfo.id
          const res = await Request.get(`/address/user/${userId}`)
          if (res.code === '0') {
            this.addresses = res.data
          }
        } catch (error) {
          logger.error('获取地址列表失败:', error)
          this.$message.error('获取地址列表失败')
        }
      },

      // 添加编辑地址方法
      editAddress(address) {
        this.showAddressForm = true
        this.addressForm = {
          id: address.id,
          receiver: address.receiver,
          phone: address.phone,
          address: address.address
        }
      },

      // 添加删除地址方法
      async deleteAddress(address) {
        try {
          await this.$confirm('确定要删除该地址吗？', '提示', {
            type: 'warning'
          })
          const res = await Request.delete(`/address/${address.id}`)
          if (res.code === '0') {
            this.$message.success('删除地址成功')
            this.getAddresses() // 刷新地址列表
          }
        } catch (error) {
          if (error !== 'cancel') {
            logger.error('删除地址失败:', error)
            this.$message.error('删除地址失败')
          }
        }
      },

      formatAddress(addr) {
        return `${addr.receiver ? addr.receiver + ' ' : ''}${addr.phone} ${addr.address}`
      },

      showAddressDialog(order) {
        this.currentEditOrder = order
        this.addressForm = {
          id: '',
          receiver: order.recvName || '',
          phone: order.recvPhone || '',
          address: order.recvAddress || ''
        }
        this.selectedAddressId = null
        this.saveToAddressBook = false
        this.showAddressForm = false
        this.addressDialogVisible = true
        this.getAddresses()
      },

    async updateOrderAddress() {
      try {
        if (this.showAddressForm) {
          // 如果是新增地址，验证表单
          await this.$refs.addressForm.validate()
        } else if (!this.selectedAddressId) {
          // 如果是选择已有地址，验证是否已选择
          this.$message.warning('请选择收货地址')
          return
        }

        let receiver, phone, address, addressId
        if (this.showAddressForm) {
          // 使用表单数据
          receiver = this.addressForm.receiver
          phone = this.addressForm.phone
          address = this.addressForm.address
          addressId = this.addressForm.id // 获取地址ID，用于判断是新增还是编辑

          // 如果有地址ID，说明是编辑已有地址
          if (addressId) {
            // 更新地址簿中的地址
            const updateRes = await Request.put(`/address/${addressId}`, {
              id: addressId,
              userId: this.userInfo.id,
              receiver,
              phone,
              address
            })
            if (updateRes.code !== '0') {
              this.$message.error('更新地址失败')
              return
            }
          } else if (this.saveToAddressBook) {
            // 如果是新地址且需要保存到地址簿
            const userId = this.userInfo.id
            await Request.post('/address', {
              userId,
              receiver,
              phone,
              address
            })
          }
        } else {
          // 使用选中的地址
          const selectedAddress = this.addresses.find(addr => addr.id === this.selectedAddressId)
          receiver = selectedAddress.receiver
          phone = selectedAddress.phone
          address = selectedAddress.address
        }

        // 更新订单收货信息
        const orderRes = await Request.put(`/order/${this.currentEditOrder.id}/address`, null, {
          params: {
            name: receiver,
            address,
            phone
          }
        })

        if (orderRes.code === '0') {
          this.$message.success('收货信息更新成功')
          this.addressDialogVisible = false
          this.getOrders() // 刷新订单列表
        }
      } catch (error) {
        if (error !== 'cancel') {
          logger.error('更新收货信息失败:', error)
          this.$message.error('更新收货信息失败')
        }
      }
    },

      // 显示退款对话框
      showRefundDialog(order) {
        this.currentRefundOrder = order
        this.refundForm = {
          reason: ''
        }
        this.refundDialogVisible = true
      },

      // 提交退款申请
      submitRefund() {
        this.$refs.refundForm.validate(async (valid) => {
          if (valid) {
            try {
              const res = await Request.post(`/order/${this.currentRefundOrder.id}/refund?reason=${encodeURIComponent(this.refundForm.reason)}`);

              if (res.code === '0') {
                this.$message.success('退款申请已提交，请等待商家处理')
                this.refundDialogVisible = false
                this.getOrders()
              } else {
                this.$message.error(res.msg || '退款申请失败')
              }
            } catch (error) {
              logger.error('退款申请失败:', error)
              this.$message.error('退款申请失败')
            }
          }
        })
      },

    // 提交评价
    async submitReview() {
        try {
          await this.$refs.reviewForm.validate()
          this.isLogin()
          const reviewData = {
            userId: this.userInfo.id,
            productId: this.currentOrder.product.id,
            rating: this.reviewForm.rating,
            content: this.reviewForm.content
          }

          const res = await Request.post('/review', reviewData)
          if (res.code === '0') {
            this.$message.success('评价已提交，审核通过后展示')
            this.reviewDialogVisible = false
            this.getOrders()
          } else {
            this.$message.error(res.msg || '评价提交失败')
          }
        } catch (error) {
          if (error !== 'cancel') {
            logger.error('评价失败:', error)
            this.$message.error('评价失败')
          }
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
    },
    // 查看物流信息
    async handleViewLogistics(order) {
      try {
        const res = await Request.get(`/logistics/order/${order.id}`)
        if (res.code === '0') {
          this.logisticsInfo = res.data
          this.logisticsDialogVisible = true
        } else {
          this.$message.error(res.msg || '暂无物流信息')
        }
      } catch (error) {
        logger.error('获取物流信息失败:', error)
        this.$message.error('获取物流信息失败')
      }
    },
    // 添加支付处理方法 - 支付宝支付
    async handlePayment(order) {
      if (!this.isLogin() || this.payingOrderId !== null) return
      this.payingOrderId = order.id
      let loading = null
      try {
        // Render product names as text, including names containing HTML characters.
        const message = h('div', { class: 'payment-confirm' }, [
          h('div', { class: 'payment-info' }, [
            h('p', { class: 'product-name' }, `${order.product?.name || '订单商品'} × ${order.quantity}`),
            h('p', { class: 'order-info' }, `订单号: ${order.id}`),
            h('div', { class: 'payment-amount' }, [
              h('span', '支付金额:'), h('span', { class: 'amount' }, `¥${order.totalPrice.toFixed(2)}`)
            ]),
            h('p', { class: 'payment-method' }, '支付方式: 支付宝')
          ])
        ])
        await this.$confirm(message, '确认支付', {
          confirmButtonText: '去支付', cancelButtonText: '取消', type: 'warning',
          center: true, customClass: 'payment-confirm-dialog'
        })
        loading = this.$loading({ lock: true, text: '准备支付中...', background: 'rgba(255, 255, 255, 0.7)' })
        const prepareRes = await Request.post(`/order/${order.id}/prepare-pay`)
        if (prepareRes.code !== '0') { this.$message.error(prepareRes.msg || '准备支付失败'); return }
        loading.setText('正在跳转支付页面...')
        const response = await Request({
          method: 'post', baseURL: '', url: '/alipay/create', authScope: 'frontUser',
          params: { orderNo: prepareRes.data.outTradeNo }, responseType: 'text',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        })
        const div = document.createElement('div')
        div.innerHTML = response.data
        const form = div.querySelector('form')
        if (!form) throw new Error('未能打开支付页面，请在订单列表中重试')
        document.body.appendChild(div)
        try { form.submit() } finally { div.remove() }
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          logger.error('支付失败:', error)
          if (error.response?.status !== 409) this.$message.error('支付未完成，请刷新订单后重试')
        }
      } finally {
        if (loading) loading.close()
        this.payingOrderId = null
      }
    },

    // 新增状态图标方法
    getStatusIcon(status) {
      const iconMap = {
        0: 'fas fa-clock',
        1: 'fas fa-check-circle',
        2: 'fas fa-shipping-fast',
        3: 'fas fa-trophy',
        4: 'fas fa-times-circle',
        5: 'fas fa-undo',
        6: 'fas fa-check-double',
        7: 'fas fa-exclamation-triangle'
      }
      return iconMap[status] || 'fas fa-question-circle'
    }
  },
  computed: {
    orderStatusText() {
      const statusMap = {
        0: '待支付',
        1: '已支付',
        2: '已发货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款',
        7: '退款未通过'
      }
      return (status) => statusMap[status] || '未知状态'
    },

    orderStatusType() {
      const typeMap = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'success',
        4: 'info',
        5: 'warning',
        6: 'info',
        7: 'danger'
      }
      return (status) => typeMap[status] || 'info'
    },
  },
}
</script>

<style scoped>
.order-load-error { padding: 48px 20px; text-align: center; color: #52675e; }
.order-load-error p { margin: 12px 0 20px; }
.summary-error { margin-top: 8px; color: #8a5c35; font-size: 13px; }

/* 整体布局样式 */
.order-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #ffffff 0%, var(--store-surface) 50%, var(--store-surface-active) 100%);
  position: relative;
  overflow-x: hidden;
}

.order-page::before {
  content: '';
  position: fixed;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 25% 25%, rgba(var(--store-brand-rgb), 0.02) 0%, transparent 50%),
              radial-gradient(circle at 75% 75%, rgba(var(--store-brand-rgb), 0.02) 0%, transparent 50%);
  z-index: -1;
  animation: backgroundFloat 25s ease-in-out infinite;
}

@keyframes backgroundFloat {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(-10px, -5px) rotate(0.5deg); }
  66% { transform: translate(5px, -10px) rotate(-0.5deg); }
}

.main-content {
  flex: 1;
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* 页面标题样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 32px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
  animation: slideInFromTop 0.8s ease-out;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--primary-light) 50%, var(--primary-color) 100%);
  background-size: 200% 100%;
  animation: gradientMove 3s ease-in-out infinite;
}

@keyframes gradientMove {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes slideInFromTop {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-left {
  flex: 1;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-icon {
  width: 60px;
  height: 60px;
  border-radius: 20px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s ease;
  animation: iconPulse 3s ease-in-out infinite;
}

.title-icon:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 30px rgba(var(--store-brand-rgb), 0.3);
}

@keyframes iconPulse {
  0%, 100% { box-shadow: 0 8px 20px rgba(var(--store-brand-rgb), 0.2); }
  50% { box-shadow: 0 8px 20px rgba(var(--store-brand-rgb), 0.4), 0 0 30px rgba(var(--store-brand-rgb), 0.1); }
}

.title-icon i {
  color: white;
  font-size: 28px;
}

.icon-pulse {
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  border-radius: 23px;
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.3), rgba(var(--store-brand-rgb), 0.3));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.title-icon:hover .icon-pulse {
  opacity: 1;
  animation: glowPulse 1.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.1); opacity: 0.6; }
}

.title-content h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--store-ink);
  margin: 0 0 8px 0;
}

.header-subtitle {
  font-size: 16px;
  color: var(--store-muted);
  font-weight: 400;
  margin: 0;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.order-stats {
  display: flex;
  gap: 24px;
}

.stats-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: rgba(var(--store-brand-rgb), 0.1);
  border-radius: 16px;
  transition: all 0.3s ease;
  position: relative;
}

.stats-item:hover {
  transform: translateY(-2px);
  background: rgba(var(--store-brand-rgb), 0.15);
}

/* 未支付订单统计项样式 - 使用一致的蓝色主题，稍微加强视觉效果 */
.stats-item.unpaid-stats {
  background: rgba(var(--store-brand-rgb), 0.12);
  border: 1px solid rgba(var(--store-brand-rgb), 0.25);
}

.stats-item.unpaid-stats:hover {
  background: rgba(var(--store-brand-rgb), 0.18);
  border-color: rgba(var(--store-brand-rgb), 0.35);
  box-shadow: 0 6px 16px rgba(var(--store-brand-rgb), 0.25);
}

.stats-item.unpaid-stats i {
  color: var(--primary-color);
  font-size: 22px; /* 稍微放大图标 */
}

.stats-item.unpaid-stats .stats-value {
  color: var(--primary-color);
  font-weight: 800; /* 加粗数值 */
}

.stats-item:hover .stats-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateY(-5px);
}

.stats-item i {
  color: var(--primary-color);
  font-size: 20px;
}

.stats-label {
  font-size: 12px;
  color: var(--store-muted);
  font-weight: 500;
}

.stats-value {
  font-size: 18px;
  color: var(--primary-color);
  font-weight: 700;
}

.stats-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(0);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  z-index: 10;
}

.stats-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: rgba(0, 0, 0, 0.8);
}

/* 空订单状态样式 */
.empty-orders {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-animation {
  margin-bottom: 32px;
}

.empty-icon {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.1), rgba(var(--store-brand-rgb), 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  animation: float 3s ease-in-out infinite;
}

.empty-icon i {
  font-size: 48px;
  color: rgba(var(--store-brand-rgb), 0.6);
}

.empty-particles {
  position: absolute;
  top: -15px;
  right: -15px;
  width: 50px;
  height: 50px;
}

.particle {
  position: absolute;
  width: 8px;
  height: 8px;
  background: rgba(var(--store-brand-rgb), 0.4);
  border-radius: 50%;
  animation: particleFloat 2s ease-in-out infinite;
}

.particle:nth-child(1) { top: 0; left: 0; animation-delay: 0s; }
.particle:nth-child(2) { top: 0; right: 0; animation-delay: 0.5s; }
.particle:nth-child(3) { bottom: 0; left: 0; animation-delay: 1s; }
.particle:nth-child(4) { bottom: 0; right: 0; animation-delay: 1.5s; }

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

@keyframes particleFloat {
  0%, 100% { transform: translateY(0px) scale(1); opacity: 0.4; }
  50% { transform: translateY(-20px) scale(1.3); opacity: 0.8; }
}

.empty-content h3 {
  font-size: 24px;
  font-weight: 600;
  color: var(--store-text);
  margin: 0 0 12px 0;
}

.empty-content p {
  font-size: 16px;
  margin: 0 0 32px 0;
  color: #6b7280;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.empty-actions .el-button {
  border-radius: 16px;
  padding: 14px 28px;
  font-weight: 500;
  font-size: 15px;
  transition: all 0.3s ease;
}

.empty-actions .el-button--primary {
  background: var(--gradient-primary);
  border: none;
}

.empty-actions .el-button i {
  margin-right: 8px;
}

.empty-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(var(--store-brand-rgb), 0.3);
}

/* 订单列表样式 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.order-item {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: slideInUp 0.6s ease forwards;
  opacity: 0;
  transform: translateY(20px);
}

.order-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 50px rgba(var(--store-brand-rgb), 0.15);
}

@keyframes slideInUp {
  to {
  opacity: 1;
    transform: translateY(0);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px 20px;
  border-bottom: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.order-info {
  display: flex;
  align-items: center;
  gap: 32px;
}

.order-id-wrapper,
.order-time-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-id-wrapper i,
.order-time-wrapper i {
  color: var(--primary-color);
  font-size: 16px;
}

.order-id,
.order-time {
  color: var(--store-muted);
  font-size: 14px;
  font-weight: 500;
}

.status-wrapper {
  display: flex;
  align-items: center;
}

.order-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 20px;
  font-weight: 600;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.order-status::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: currentColor;
  opacity: 0.1;
}

.status-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: currentColor;
  color: white;
}

.status-icon i {
  font-size: 12px;
}

.status-text {
  position: relative;
  z-index: 1;
  font-size: 14px;
}

.order-status.pending {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

.order-status.paid {
  color: var(--store-brand);
  background: rgba(var(--store-brand-rgb), 0.1);
}

.order-status.shipped {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.order-status.completed {
  color: #059669;
  background: rgba(5, 150, 105, 0.1);
}

.order-status.cancelled {
  color: #6b7280;
  background: rgba(107, 114, 128, 0.1);
}

.order-status.refunding {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

.order-status.refunded {
  color: #6b7280;
  background: rgba(107, 114, 128, 0.1);
}

.order-status.refund-failed {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* 订单主体样式 */
.order-body {
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.product-section {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.product-image-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  flex-shrink: 0;
}

.product-image {
  width: 100px;
  height: 100px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.product-image:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(var(--store-brand-rgb), 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 16px;
}

.product-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(var(--store-brand-rgb), 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 16px;
}

.image-overlay i {
  color: white;
  font-size: 24px;
}

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-name {
  font-size: 18px;
  color: var(--store-ink);
  margin: 0;
  font-weight: 600;
  line-height: 1.4;
}

.product-price-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-price {
  color: #ff6b6b;
  font-weight: 700;
  font-size: 20px;
}

.current-price.discount {
  color: #ff6b6b;
}

.quantity-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-label {
  color: var(--store-muted);
  font-size: 14px;
  font-weight: 500;
}

.quantity-value {
  color: var(--store-ink);
  font-size: 16px;
  font-weight: 600;
  background: rgba(var(--store-brand-rgb), 0.1);
  padding: 4px 12px;
  border-radius: 8px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(var(--store-brand-rgb), 0.08);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--store-muted);
}

.meta-item i {
  color: var(--primary-color);
  font-size: 14px;
}

.order-purchase-note {
  margin: 10px 0 0;
  color: var(--store-muted);
  font-size: 13px;
  line-height: 1.6;
}

/* 收货信息样式 */
.delivery-section {
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.04), rgba(255, 255, 255, 0.8));
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--store-ink);
}

.section-header i {
  color: var(--primary-color);
  font-size: 18px;
}

.delivery-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.info-item.address-item {
  flex-basis: 100%;
}

.info-item i {
  color: var(--primary-color);
  font-size: 16px;
  flex-shrink: 0;
}

.info-label {
  color: var(--store-muted);
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
}

.info-value {
  color: var(--store-ink);
  font-size: 14px;
  font-weight: 500;
  min-width: 0;
  word-break: break-all;
}

/* 订单底部样式 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 32px;
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.04), rgba(255, 255, 255, 0.8));
  border-radius: 0 0 24px 24px;
  border-top: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.footer-left {
  display: flex;
  align-items: center;
}

.order-total {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-label {
  color: var(--store-muted);
  font-size: 16px;
  font-weight: 500;
}

.total-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.currency {
  font-size: 18px;
  color: #ff6b6b;
  font-weight: 600;
}

.amount {
  font-size: 24px;
  color: #ff6b6b;
  font-weight: 700;
}

.footer-right {
  display: flex;
  align-items: center;
}

.order-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
}

.primary-actions {
  display: flex;
  gap: 12px;
}

.secondary-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 16px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
  border: none;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: currentColor;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.action-btn:hover::before {
  opacity: 0.1;
}

.action-btn i {
  position: relative;
  z-index: 1;
}

.action-btn span {
  position: relative;
  z-index: 1;
}

.pay-btn {
  background: linear-gradient(135deg, var(--store-brand), var(--store-brand-hover));
  color: white;
}

.confirm-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.review-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
}

.text-btn {
  color: var(--store-muted);
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: transparent;
  border: 1px solid transparent;
}

.text-btn:hover {
  background: rgba(var(--store-brand-rgb), 0.08);
  border-color: rgba(var(--store-brand-rgb), 0.2);
  transform: translateY(-1px);
}

.refund-btn:hover {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.2);
}

.cancel-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
}

.address-btn:hover {
  color: var(--store-olive);
  background: rgba(var(--store-olive-rgb), 0.1);
  border-color: rgba(var(--store-olive-rgb), 0.2);
}

.logistics-btn:hover {
  color: var(--store-brand-light);
  background: rgba(var(--store-brand-light-rgb), 0.1);
  border-color: rgba(var(--store-brand-light-rgb), 0.2);
}



/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin: 40px 0;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 8px 25px rgba(var(--store-brand-rgb), 0.1);
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: var(--primary-color);
  border-radius: 8px;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: var(--primary-color);
  transform: translateY(-1px);
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
  transition: all 0.3s ease;
}

/* 加载状态样式 */
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 24px;
}

:deep(.el-loading-spinner) {
  color: var(--primary-color);
}

/* 过渡动画 */
.order-item-enter-active, 
.order-item-leave-active {
  transition: all 0.4s ease;
}

.order-item-enter, 
.order-item-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.order-item-move {
  transition: transform 0.4s ease;
}

/* 对话框样式优化 */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

:deep(.el-dialog__header) {
  padding: 24px 32px;
  border-bottom: 1px solid rgba(var(--store-brand-rgb), 0.1);
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.06), rgba(255, 255, 255, 0.9));
  margin: 0;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: var(--store-ink);
}

:deep(.el-dialog__body) {
  padding: 32px;
  max-height: 60vh;
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  padding: 20px 32px;
  border-top: 1px solid rgba(var(--store-brand-rgb), 0.1);
  background: rgba(var(--store-surface-active-rgb), 0.5);
  margin-top: 0;
}

:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 8px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: rgba(var(--store-brand-rgb), 0.3);
  border-radius: 4px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: transparent;
}

/* 地址相关样式 */
.address-list-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: var(--store-ink);
}

.section-title i {
  color: var(--primary-color);
  font-size: 20px;
}

.address-list {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

.address-radio-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.address-radio-item {
  width: 100%;
  margin: 0;
  padding: 20px;
  border: 1.5px solid rgba(var(--store-brand-rgb), 0.15);
  border-radius: 16px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(var(--store-brand-rgb), 0.08);
}

.address-radio-item:hover {
  border-color: rgba(var(--store-brand-rgb), 0.3);
  box-shadow: 0 8px 25px rgba(var(--store-brand-rgb), 0.12);
  transform: translateY(-2px);
}

.address-radio-item.is-checked {
  border-color: var(--primary-color);
  background: rgba(var(--store-brand-rgb), 0.06);
  box-shadow: 0 8px 25px rgba(var(--store-brand-rgb), 0.18);
}

.address-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-left: 12px;
}

.address-info {
  flex: 1;
}

.contact-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.receiver {
  font-weight: 600;
  color: var(--store-ink);
  font-size: 16px;
}

.phone {
  color: var(--store-muted);
  font-size: 14px;
  background: rgba(var(--store-brand-rgb), 0.1);
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.address-detail {
  color: var(--store-muted);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.address-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: all 0.3s ease;
}

.address-radio-item:hover .address-actions {
  opacity: 1;
}

.address-actions .el-button {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 8px;
  font-weight: 500;
}

/* 表单样式 */
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.address-form {
  padding: 20px 0;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--store-ink);
}

:deep(.el-input__prefix) {
  color: var(--primary-color);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border: 1.5px solid rgba(var(--store-brand-rgb), 0.15);
  border-radius: 12px;
  transition: all 0.3s ease;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(var(--store-brand-rgb), 0.08);
}

:deep(.el-input__inner:hover),
:deep(.el-textarea__inner:hover) {
  border-color: rgba(var(--store-brand-rgb), 0.3);
  box-shadow: 0 3px 10px rgba(var(--store-brand-rgb), 0.12);
}

:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--store-brand-rgb), 0.12), 0 4px 16px rgba(var(--store-brand-rgb), 0.15);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

:deep(.el-checkbox__inner:hover) {
  border-color: var(--primary-color);
}

/* 物流信息样式 */
.logistics-info {
  padding: 0;
}

.logistics-header {
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.06), rgba(255, 255, 255, 0.8));
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.logistics-header > div {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.logistics-header > div:last-child {
  margin-bottom: 0;
}

.logistics-header .label {
  color: var(--store-muted);
  font-weight: 500;
  min-width: 80px;
}

.logistics-address {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(var(--store-brand-rgb), 0.1);
}

.logistics-address h4 {
  font-size: 18px;
  color: var(--store-ink);
  margin: 0 0 16px 0;
  font-weight: 600;
}

.logistics-info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
}

.logistics-info-item .label {
  color: var(--store-muted);
  font-weight: 500;
  min-width: 100px;
}

/* 支付确认对话框样式 */
:deep(.payment-confirm-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.payment-confirm-dialog .el-message-box__header) {
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.06), rgba(255, 255, 255, 0.9));
  padding: 20px 24px;
}

:deep(.payment-confirm-dialog .el-message-box__title) {
  color: var(--store-ink);
  font-size: 20px;
  font-weight: 600;
}

:deep(.payment-confirm-dialog .el-message-box__content) {
  padding: 24px;
  padding-bottom: 16px;
}

:deep(.payment-confirm-dialog .el-message-box__btns) {
  padding: 16px 24px;
  border-top: 1px solid rgba(var(--store-brand-rgb), 0.1);
  background: rgba(var(--store-surface-active-rgb), 0.5);
}

:deep(.payment-confirm-dialog .el-button--primary) {
  background: var(--gradient-primary);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(var(--store-brand-rgb), 0.25);
}

:deep(.payment-confirm-dialog .el-button--primary:hover) {
  background: linear-gradient(135deg, var(--store-brand), var(--store-brand));
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(var(--store-brand-rgb), 0.35);
}

.payment-confirm {
  padding: 0;
}

.payment-info {
  background: linear-gradient(135deg, rgba(var(--store-brand-rgb), 0.06), rgba(255, 255, 255, 0.8));
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(var(--store-brand-rgb), 0.2);
}

.payment-info .product-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--store-ink);
  margin: 0 0 12px 0;
}

.payment-info .order-info {
  font-size: 14px;
  color: var(--store-muted);
  margin: 8px 0;
  font-weight: 500;
}

.payment-amount {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(var(--store-brand-rgb), 0.2);
}

.payment-amount .amount {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    max-width: 100%;
    padding: 24px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 20px;
  }
  .header-right {
    display: flex;
    width: 100%;
  }
  .order-stats {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
    gap: 20px;
    padding: 24px;
    text-align: center;
  }
  
  .header-left,
  .header-right {
    width: 100%;
    justify-content: center;
  }
  
  .order-body {
    padding: 20px 24px;
  }
  
  .product-section {
    flex-direction: column;
    gap: 16px;
  }
  
  .product-image-wrapper {
    align-self: center;
  }
  
  .order-footer {
    flex-direction: column;
    gap: 16px;
    padding: 20px 24px;
    text-align: center;
  }
  
  .footer-left,
  .footer-right {
    width: 100%;
    justify-content: center;
  }
  
  .order-actions {
    align-items: center;
  }
  
  .secondary-actions {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
  
  .page-header {
    padding: 20px;
    border-radius: 16px;
  }
  
  .title-wrapper {
    gap: 16px;
  }
  
  .title-icon {
    width: 50px;
    height: 50px;
  }
  
  .title-icon i {
    font-size: 24px;
  }
  
  .title-content h2 {
    font-size: 24px;
  }
  
  .header-subtitle {
    font-size: 14px;
  }
  
  .order-stats {
    gap: 12px;
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .stats-item {
    padding: 12px 16px;
    min-width: 80px;
  }
  
  .order-item {
    border-radius: 16px;
  }
  
  .order-header {
    padding: 20px 24px 16px;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .order-info {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  
  .order-body {
    padding: 16px 20px;
  }
  
  .product-image {
    width: 80px;
    height: 80px;
  }
  
  .delivery-content {
    gap: 8px;
  }
  
  .info-row {
    flex-direction: column;
    gap: 8px;
  }
  
  .order-footer {
    padding: 16px 20px;
  }
  
  .primary-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .action-btn {
    width: 100%;
    justify-content: center;
  }
  
  .secondary-actions {
    width: 100%;
    justify-content: space-around;
  }
  
  .empty-orders {
    padding: 60px 16px;
  }
  
  .empty-icon {
    width: 80px;
    height: 80px;
  }
  
  .empty-icon i {
    font-size: 32px;
  }
  
  .empty-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .empty-actions .el-button {
    width: 100%;
  }
}

/* 无障碍性优化 */
@media (prefers-reduced-motion: reduce) {
  .order-page::before,
  .title-icon,
  .order-item,
  .page-header,
  .empty-icon,
  .particle {
    animation: none !important;
    transition: opacity 0.2s ease !important;
  }
  
  .order-item {
    opacity: 1;
    transform: none;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .order-page {
    background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1e2d1e 100%);
  }
  
  .page-header,
  .order-item,
  .pagination-wrapper {
    background: rgba(45, 45, 45, 0.95);
    color: #e0e0e0;
  }
  
  .title-content h2 {
    color: #e0e0e0;
  }
  
  .header-subtitle {
    color: #b0b0b0;
  }
}

/* 性能优化 */
.order-item,
.page-header,
.empty-icon,
.title-icon {
  will-change: transform;
}

.refund-progress-note{margin:12px 20px;padding:12px 16px;background:#f5f2e8;border:1px solid #e6debe;border-radius:6px;display:flex;flex-direction:column;gap:5px;color:#695e3e;font-size:12px;}.refund-progress-note strong{font-size:13px;}.refund-progress-note small{line-height:1.6;color:#7a735f;}
</style> 