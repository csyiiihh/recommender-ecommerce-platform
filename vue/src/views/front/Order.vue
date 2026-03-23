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
              <span class="stats-value">{{ total }}</span>
            </div>
            <div class="stats-item unpaid-stats">
              <i class="fas fa-credit-card"></i>
              <span class="stats-label">未支付</span>
              <span class="stats-value">{{ unpaidTotal }}</span>
              <div class="stats-tooltip">需要完成支付的订单</div>
            </div>
            <div class="stats-item" v-if="pendingOrdersCount > 0">
              <i class="fas fa-clock"></i>
              <span class="stats-label">待处理</span>
              <span class="stats-value">{{ pendingOrdersCount }}</span>
              <div class="stats-tooltip">待支付和已支付订单</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单列表 -->
      <div class="order-content" v-loading="loading">
        <!-- 空订单提示 -->
        <div v-if="!loading && orders.length === 0" class="empty-orders">
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

              <!-- 订单图书和收货信息 -->
              <div class="order-body">
                <div class="product-section">
                  <div class="product-image-wrapper">
                <el-image :src="order.product.imageUrl?.startsWith('http') ? order.product.imageUrl : `/api${order.product.imageUrl}`" fit="cover"
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
                    {{ order.product.name }}
                    <!-- 拼团标识 -->
                    <el-tag v-if="order.orderType === 1" type="warning" size="small" style="margin-left: 8px;">
                      <i class="fas fa-users"></i> 拼团
                    </el-tag>
                  </h3>
                    <div class="product-price-info">
                      <div class="price-container">
                        <span class="current-price" :class="{ 'discount': order.product.isDiscount }">
                      ¥{{ (order.product.isDiscount ? order.product.discountPrice : order.price).toFixed(2) }}
                    </span>
                    <span class="original-price" v-if="order.product.isDiscount">
                      ¥{{ order.product.price.toFixed(2) }}
                    </span>
                        <div class="discount-badge" v-if="order.product.isDiscount">
                          <i class="fas fa-tag"></i>
                          优惠
                  </div>
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
                    <div class="product-meta">
                      <div class="meta-item">
                        <i class="fas fa-cube"></i>
                        <span>库存: {{ order.product.stock || 0 }}</span>
                      </div>
                    <!-- 数量调整：拼团订单不允许修改数量 -->
                    <div class="quantity-adjust" v-if="order.status === 0 && order.orderType !== 1">
                      <el-input-number 
                        v-model="order.quantity" 
                        :min="1" 
                        :max="order.product.stock" 
                        size="small"
                        @change="handleQuantityChange(order)"
                          class="quantity-input"
                      ></el-input-number>
                    </div>
                    <!-- 拼团订单提示 -->
                    <div class="quantity-tip" v-if="order.status === 0 && order.orderType === 1" style="color: #E6A23C; font-size: 12px; margin-top: 8px;">
                      <i class="fas fa-info-circle"></i> 拼团订单限购1件，不可修改数量
                    </div>
                  </div>
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
                      <el-button v-if="order.status === 0" type="primary" @click="handlePayment(order)" class="action-btn pay-btn">
                        <i class="fas fa-credit-card"></i>
                  立即支付
                </el-button>
                      <el-button v-if="order.status === 2" type="success" @click="handleConfirm(order)" class="action-btn confirm-btn">
                        <i class="fas fa-check-circle"></i>
                  确认收货
                </el-button>
                      <el-button v-if="order.status === 3" @click="handleReview(order)" class="action-btn review-btn">
                        <i class="fas fa-star"></i>
                        图书评价
                </el-button>
                    </div>
                    
                    <!-- 次要操作按钮 -->
                    <div class="secondary-actions">
                      <el-button v-if="[1, 2, 3].includes(order.status)" link @click="showRefundDialog(order)" class="text-btn refund-btn">
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
                      <el-button v-if="order.status > 1" link @click="handleViewLogistics(order)" class="text-btn logistics-btn">
                        <i class="fas fa-truck"></i>
                  查看物流
                </el-button>
                      <el-button v-if="[3, 4, 6].includes(order.status)" link @click="handleDelete(order)" class="text-btn delete-btn">
                        <i class="fas fa-trash-alt"></i>
                  删除订单
                </el-button>
              </div>
            </div>
          </div>
              </div>
            </div>
          </transition-group>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
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
        <el-image :src="currentOrder.product.imageUrl?.startsWith('http') ? currentOrder.product.imageUrl : `/api${currentOrder.product.imageUrl}`" fit="cover"
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
          <el-rate v-model="reviewForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" show-text :texts="['很差', '较差', '一般', '较好', '很好']">
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

    <!-- 添加删除订单对话框 -->
    <el-dialog
      title="删除订单"
      v-model="deleteDialogVisible"
      width="30%">
      <span>确定要删除该订单吗？删除后将无法恢复。</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">确定</el-button>
        </span>
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
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import axios from 'axios'
import { formatTime } from '@/utils/time'
import { Plus, Edit, Delete, ArrowLeft, User, Phone, Location } from '@element-plus/icons-vue'

export default {
  name: 'Order',
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
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: false,
      orders: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      pendingTotal: 0, // 新增：待处理订单总数
      unpaidTotal: 0, // 新增：未支付订单总数
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
      deleteDialogVisible: false,
      currentDeleteOrder: null,
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
  mounted() {
    this.userInfo = JSON.parse(localStorage.getItem('frontUser') || '{}')
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
    async getOrders() {
      this.isLogin()
      this.loading = true
      try {
        const userId = this.userInfo.id
        
        // 获取订单列表
        const res = await Request.get('/order/page', {
          params: {
            userId,
            currentPage: this.currentPage,
            size: this.pageSize
          }
        })
        
        if (res.code === '0') {
          if (res.data.total === 0) {
            this.orders = []
            this.total = 0
            this.pendingTotal = 0
            this.unpaidTotal = 0
          } else {
            this.orders = res.data.records
            this.total = res.data.total
            
            // 获取订单状态统计数据
            await this.getOrderStatistics(userId)
          }
        }
      } catch (error) {
        logger.error('获取订单列表失败:', error)
        this.$message.error('获取订单列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 新增：获取订单状态统计数据
    async getOrderStatistics(userId) {
      // 使用当前页数据作为主要统计来源（避免查询所有订单导致超时）
      // 如果当前页没有数据，才尝试获取第一页数据
      try {
        logger.debug('开始获取订单统计数据，用户ID:', userId)
        
        // 如果当前页已有数据，直接使用当前页数据统计
        if (this.orders && this.orders.length > 0) {
          this.unpaidTotal = this.orders.filter(order => order.status === 0).length
          this.pendingTotal = this.orders.filter(order => [0, 1].includes(order.status)).length
          logger.debug('使用当前页数据统计 - 未支付:', this.unpaidTotal, '待处理:', this.pendingTotal)
          return
        }
        
        // 如果当前页没有数据，尝试获取第一页数据（限制size为合理值，避免超时）
        const res = await Request.get('/order/page', {
          params: {
            userId,
            currentPage: 1,
            size: 100 // 限制为100条，避免超时
          },
          timeout: 10000, // 10秒超时，如果超时则使用降级方案
          silent: true // 标记为静默请求，超时时不显示错误消息
        })
        
        logger.debug('订单API响应:', res)
        
        if (res.code === '0' && res.data && res.data.records) {
          const orders = res.data.records
          
          // 统计当前页数据
          this.unpaidTotal = orders.filter(order => order.status === 0).length
          const paidCount = orders.filter(order => order.status === 1).length
          this.pendingTotal = this.unpaidTotal + paidCount
          
          // 如果返回的数据少于总数，说明还有更多订单，但为了性能不再查询
          const total = res.data.total || 0
          if (total > orders.length) {
            logger.debug(`订单总数(${total})超过当前页数据(${orders.length})，仅统计当前页`)
          }
          
          logger.debug('统计结果 - 当前页订单:', orders.length, '未支付:', this.unpaidTotal, '已支付:', paidCount, '待处理:', this.pendingTotal)
        } else {
          // 使用默认值
          this.unpaidTotal = 0
          this.pendingTotal = 0
          logger.debug('获取订单数据失败，使用默认值')
        }
        
      } catch (error) {
        // 静默处理所有错误，使用降级方案
        // 不记录错误日志，避免在控制台产生噪音
        if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
          logger.debug('订单统计请求超时，使用降级方案（静默处理）')
        } else {
          logger.debug('获取订单统计数据失败，使用降级方案（静默处理）:', error.message)
        }
        
        // 使用当前页数据作为备选方案
        if (this.orders && this.orders.length > 0) {
          this.unpaidTotal = this.orders.filter(order => order.status === 0).length
          this.pendingTotal = this.orders.filter(order => [0, 1].includes(order.status)).length
        } else {
          // 如果没有当前页数据，使用默认值
          this.unpaidTotal = 0
          this.pendingTotal = 0
        }
        logger.debug('使用降级方案 - 未支付:', this.unpaidTotal, '待处理:', this.pendingTotal)
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
          7: '退款失败'
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
            this.$message.success('评价成功')
            this.reviewDialogVisible = false
            // 更新订单状态为已评价
            await Request.put(`/order/${this.currentOrder.id}/status?status=3`)
            this.getOrders()
          }
        } catch (error) {
          if (error !== 'cancel') {
            logger.error('评价失败:', error)
            this.$message.error('评价失败')
          }
        }
      },
    async handleQuantityChange(order) {
      try {
        // 创建更新订单对象
        const updateOrder = {
          id: order.id,
          quantity: order.quantity,
          totalPrice: order.quantity * (order.product.isDiscount ? order.product.discountPrice : order.price)
        };

        const res = await Request.put(`/order/${order.id}`, updateOrder);
        
        if (res.code === '0') {
          // 更新本地订单总价
          order.totalPrice = updateOrder.totalPrice;
          this.$message.success('数量修改成功');
        } else {
          this.$message.error(res.msg || '修改失败');
          // 修改失败时重新获取订单列表
          this.getOrders();
        }
      } catch (error) {
        logger.error('修改数量失败:', error);
        this.$message.error('修改数量失败');
        // 修改失败时重新获取订单列表
        this.getOrders();
      }
    },
    handleDelete(order) {
      this.currentDeleteOrder = order;
      this.deleteDialogVisible = true;
    },
    async confirmDelete() {
      try {
        const res = await Request.delete(`/order/${this.currentDeleteOrder.id}`);
        if (res.code === '0') {
          this.$message.success('订单删除成功');
          this.deleteDialogVisible = false;
          this.getOrders(); // 刷新订单列表
        } else {
          this.$message.error(res.msg || '删除失败');
        }
      } catch (error) {
        logger.error('删除订单失败:', error);
        this.$message.error('删除订单失败');
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
      if(!this.isLogin()) return;
      
      try {
        // 获取商品信息，展示更详细的支付信息
        const productName = order.product.name;
        const quantity = order.quantity;
        
        // 添加确认支付对话框
        await this.$confirm(
          `<div class="payment-confirm">
            <div class="payment-info">
              <p class="product-name">${productName} × ${quantity}</p>
              <p class="order-info">订单号: ${order.id}</p>
              <div class="payment-amount">
                <span>支付金额:</span>
                <span class="amount">¥${order.totalPrice.toFixed(2)}</span>
              </div>
              <p class="payment-method">支付方式: 支付宝</p>
            </div>
          </div>`, 
          '确认支付', 
          {
            confirmButtonText: '去支付',
            cancelButtonText: '取消',
            type: 'warning',
            center: true,
            dangerouslyUseHTMLString: true,
            customClass: 'payment-confirm-dialog'
          }
        );
        
        // 显示加载中
        const loading = this.$loading({
          lock: true,
          text: '准备支付中...',
          spinner: 'fas fa-spinner fa-spin',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        // 先调用准备支付接口，获取商户订单号
        const prepareRes = await Request.post(`/order/${order.id}/prepare-pay`)
        
        if (prepareRes.code !== '0') {
          loading.close()
          this.$message.error(prepareRes.msg || '准备支付失败')
          return
        }
        
        const { outTradeNo, totalAmount } = prepareRes.data
        
        // 准备支付宝支付参数
        const currentDate = new Date().toLocaleDateString('zh-CN')
        const userInfo = JSON.parse(localStorage.getItem('frontUser') || '{}')
        const orderName = `${currentDate}-图书商城-${userInfo.username || userInfo.name}-订单${order.id}`
        
        loading.text = '正在跳转支付页面...'
        
        // 调用支付宝支付接口
        const response = await axios({
          method: 'post',
          url: 'http://localhost:1234/alipay/create',
          params: {
            orderNo: outTradeNo,
            orderName: orderName,
            payPrice: totalAmount.toFixed(2)
          },
          responseType: 'text',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        })
        
        loading.close()
        
        // 将支付页面HTML写入新窗口
        const div = document.createElement('div')
        div.innerHTML = response.data
        document.body.appendChild(div)
        document.querySelector('form').submit()
        document.body.removeChild(div)
        
      } catch (error) {
        if (error === 'cancel') {
          this.$message({
            type: 'info',
            message: '已取消支付'
          });
        } else {
          logger.error('支付失败:', error);
          this.$message.error('支付失败：' + (error.response?.data || error.message || '请稍后重试'));
        }
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
    // 待处理订单数量（使用从后端获取的准确数据）
    pendingOrdersCount() {
      return this.pendingTotal
    },
    
    orderStatusText() {
      const statusMap = {
        0: '待支付',
        1: '已支付',
        2: '已发货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款',
        7: '退款失败'
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
/* 整体布局样式 */
.order-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #ffffff 0%, #f5f7fa 50%, #ecf5ff 100%);
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
  background: radial-gradient(circle at 25% 25%, rgba(58, 91, 160, 0.02) 0%, transparent 50%),
              radial-gradient(circle at 75% 75%, rgba(91, 124, 189, 0.02) 0%, transparent 50%);
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
  box-shadow: 0 12px 30px rgba(58, 91, 160, 0.3);
}

@keyframes iconPulse {
  0%, 100% { box-shadow: 0 8px 20px rgba(58, 91, 160, 0.2); }
  50% { box-shadow: 0 8px 20px rgba(58, 91, 160, 0.4), 0 0 30px rgba(58, 91, 160, 0.1); }
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
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.3), rgba(91, 124, 189, 0.3));
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
  color: #1a202c;
  margin: 0 0 8px 0;
}

.header-subtitle {
  font-size: 16px;
  color: #64748b;
  font-weight: 400;
  margin: 0;
}

.header-right {
  display: flex;
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
  background: rgba(58, 91, 160, 0.1);
  border-radius: 16px;
  transition: all 0.3s ease;
  position: relative;
}

.stats-item:hover {
  transform: translateY(-2px);
  background: rgba(58, 91, 160, 0.15);
}

/* 未支付订单统计项样式 - 使用一致的蓝色主题，稍微加强视觉效果 */
.stats-item.unpaid-stats {
  background: rgba(58, 91, 160, 0.12);
  border: 1px solid rgba(58, 91, 160, 0.25);
}

.stats-item.unpaid-stats:hover {
  background: rgba(58, 91, 160, 0.18);
  border-color: rgba(58, 91, 160, 0.35);
  box-shadow: 0 6px 16px rgba(58, 91, 160, 0.25);
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
  color: #64748b;
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
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.1), rgba(58, 91, 160, 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  animation: float 3s ease-in-out infinite;
}

.empty-icon i {
  font-size: 48px;
  color: rgba(58, 91, 160, 0.6);
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
  background: rgba(58, 91, 160, 0.4);
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
  color: #374151;
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
  box-shadow: 0 8px 20px rgba(58, 91, 160, 0.3);
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
  box-shadow: 0 20px 50px rgba(58, 91, 160, 0.15);
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
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
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
  color: #64748b;
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
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
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
  background: rgba(58, 91, 160, 0.8);
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
  background: rgba(58, 91, 160, 0.8);
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
  color: #1a202c;
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

.original-price {
  font-size: 16px;
  color: #9ca3af;
  text-decoration: line-through;
}

.discount-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  color: white;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 600;
}

.discount-badge i {
  font-size: 10px;
}

.quantity-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-label {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.quantity-value {
  color: #1a202c;
  font-size: 16px;
  font-weight: 600;
  background: rgba(58, 91, 160, 0.1);
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
  background: rgba(58, 91, 160, 0.08);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
}

.meta-item i {
  color: var(--primary-color);
  font-size: 14px;
}

.quantity-adjust {
  display: flex;
  align-items: center;
}

.quantity-input {
  width: 120px;
}

.quantity-input :deep(.el-input__inner) {
  border: 1.5px solid rgba(58, 91, 160, 0.2);
  border-radius: 8px;
  text-align: center;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.08);
}

.quantity-input :deep(.el-input__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(58, 91, 160, 0.12), 0 4px 16px rgba(58, 91, 160, 0.15);
}

/* 收货信息样式 */
.delivery-section {
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.04), rgba(255, 255, 255, 0.8));
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(58, 91, 160, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
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
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
}

.info-value {
  color: #1a202c;
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
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.04), rgba(255, 255, 255, 0.8));
  border-radius: 0 0 24px 24px;
  border-top: 1px solid rgba(58, 91, 160, 0.1);
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
  color: #64748b;
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
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
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
  color: #64748b;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: transparent;
  border: 1px solid transparent;
}

.text-btn:hover {
  background: rgba(58, 91, 160, 0.08);
  border-color: rgba(58, 91, 160, 0.2);
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
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.2);
}

.logistics-btn:hover {
  color: #06b6d4;
  background: rgba(6, 182, 212, 0.1);
  border-color: rgba(6, 182, 212, 0.2);
}

.delete-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
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
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.1);
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
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.9));
  margin: 0;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: #1a202c;
}

:deep(.el-dialog__body) {
  padding: 32px;
  max-height: 60vh;
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  padding: 20px 32px;
  border-top: 1px solid rgba(58, 91, 160, 0.1);
  background: rgba(236, 245, 255, 0.5);
  margin-top: 0;
}

:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 8px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: rgba(58, 91, 160, 0.3);
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
  color: #1a202c;
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
  border: 1.5px solid rgba(58, 91, 160, 0.15);
  border-radius: 16px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.08);
}

.address-radio-item:hover {
  border-color: rgba(58, 91, 160, 0.3);
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.12);
  transform: translateY(-2px);
}

.address-radio-item.is-checked {
  border-color: var(--primary-color);
  background: rgba(58, 91, 160, 0.06);
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.18);
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
  color: #1a202c;
  font-size: 16px;
}

.phone {
  color: #64748b;
  font-size: 14px;
  background: rgba(58, 91, 160, 0.1);
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.address-detail {
  color: #64748b;
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
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
}

.address-form {
  padding: 20px 0;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #1a202c;
}

:deep(.el-input__prefix) {
  color: var(--primary-color);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border: 1.5px solid rgba(58, 91, 160, 0.15);
  border-radius: 12px;
  transition: all 0.3s ease;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.08);
}

:deep(.el-input__inner:hover),
:deep(.el-textarea__inner:hover) {
  border-color: rgba(58, 91, 160, 0.3);
  box-shadow: 0 3px 10px rgba(58, 91, 160, 0.12);
}

:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(58, 91, 160, 0.12), 0 4px 16px rgba(58, 91, 160, 0.15);
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
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.8));
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(58, 91, 160, 0.1);
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
  color: #64748b;
  font-weight: 500;
  min-width: 80px;
}

.logistics-address {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(58, 91, 160, 0.1);
}

.logistics-address h4 {
  font-size: 18px;
  color: #1a202c;
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
  color: #64748b;
  font-weight: 500;
  min-width: 100px;
}

/* 支付确认对话框样式 */
:deep(.payment-confirm-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.payment-confirm-dialog .el-message-box__header) {
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.9));
  padding: 20px 24px;
}

:deep(.payment-confirm-dialog .el-message-box__title) {
  color: #1a202c;
  font-size: 20px;
  font-weight: 600;
}

:deep(.payment-confirm-dialog .el-message-box__content) {
  padding: 24px;
  padding-bottom: 16px;
}

:deep(.payment-confirm-dialog .el-message-box__btns) {
  padding: 16px 24px;
  border-top: 1px solid rgba(58, 91, 160, 0.1);
  background: rgba(236, 245, 255, 0.5);
}

:deep(.payment-confirm-dialog .el-button--primary) {
  background: var(--gradient-primary);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.25);
}

:deep(.payment-confirm-dialog .el-button--primary:hover) {
  background: linear-gradient(135deg, #4A6FA8, #3A5BA0);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(58, 91, 160, 0.35);
}

.payment-confirm {
  padding: 0;
}

.payment-info {
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.8));
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(58, 91, 160, 0.2);
}

.payment-info .product-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 12px 0;
}

.payment-info .order-info {
  font-size: 14px;
  color: #64748b;
  margin: 8px 0;
  font-weight: 500;
}

.payment-amount {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(58, 91, 160, 0.2);
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
  
  .header-right {
    display: none;
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
</style> 