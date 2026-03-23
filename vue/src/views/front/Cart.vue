<template>
  <div class="cart-page landscape-bg">
    <front-header></front-header>
    <div class="main-content porcelain-texture">
      <!-- 购物车标题 -->
      <div class="page-header">
        <div class="header-left">
          <div class="title-wrapper">
            <div class="title-icon">
              <i class="fas fa-shopping-cart"></i>
              <div class="icon-pulse"></div>
            </div>
            <div class="title-content">
              <h2>我的购物车</h2>
              <div class="header-subtitle">精选优质图书，尽情享受购书乐趣</div>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="cart-stats">
            <div class="stats-item">
              <i class="fas fa-box"></i>
              <span class="stats-label">图书数量</span>
              <span class="stats-value">{{ totalItems }}</span>
            </div>
            <div class="stats-item" v-if="selectedCount > 0">
              <i class="fas fa-check-circle"></i>
              <span class="stats-label">已选中</span>
              <span class="stats-value">{{ selectedCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 购物车主体 -->
      <div class="cart-content" v-loading="loading">
        <!-- 空购物车提示 -->
        <div v-if="!loading && cartItems.length === 0" class="empty-cart">
          <div class="empty-animation">
            <div class="empty-icon">
              <i class="fas fa-shopping-cart"></i>
              <div class="empty-items">
                <div class="empty-item"></div>
                <div class="empty-item"></div>
                <div class="empty-item"></div>
              </div>
            </div>
          </div>
          <div class="empty-content">
            <h3>您的购物车还是空的</h3>
            <p>去发现一些好东西吧！</p>
            <div class="empty-actions">
              <el-button type="primary" size="large" @click="$router.push('/products')">
                <i class="fas fa-store"></i>
                立即购物
              </el-button>
              <el-button size="large" @click="$router.push('/')">
                <i class="fas fa-home"></i>
                返回首页
              </el-button>
            </div>
          </div>
        </div>

        <!-- 购物车列表 -->
        <template v-else>
          <!-- 表头 -->
          <div class="cart-header">
            <div class="header-select">
              <el-checkbox v-model="allSelected" @change="handleSelectAll" class="select-all-checkbox">
                <span class="select-text">全选</span>
              </el-checkbox>
            </div>
            <div class="header-item product-header">
              <i class="fas fa-tags"></i>
              图书信息
            </div>
            <div class="header-item price-header">
              <i class="fas fa-dollar-sign"></i>
              单价
            </div>
            <div class="header-item quantity-header">
              <i class="fas fa-sort-numeric-up"></i>
              数量
            </div>
            <div class="header-item subtotal-header">
              <i class="fas fa-calculator"></i>
              小计
            </div>
            <div class="header-item action-header">
              <i class="fas fa-cog"></i>
              操作
            </div>
          </div>

          <!-- 购物车图书列表 -->
          <div class="cart-list">
            <transition-group name="cart-item" tag="div">
              <div v-for="(item, index) in cartItems" :key="item.id" class="cart-item" :style="{ 'animation-delay': (index * 0.1) + 's' }">
                <div class="item-checkbox">
                  <el-checkbox v-model="item.selected" @change="handleItemSelect" class="item-select-checkbox">
                  </el-checkbox>
                </div>

                <div class="product-info">
                  <div class="product-image-wrapper">
                    <el-image :src="'/api'+item.product.imageUrl" fit="cover" class="product-image" @click="$router.push(`/product/${item.product.id}`)">
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
                  <div class="product-detail">
                    <div class="product-name" @click="$router.push(`/product/${item.product.id}`)">
                      {{ item.product.name }}
                    </div>
                    <div class="product-meta">
                      <div class="product-stock">
                        <i class="fas fa-cube"></i>
                        库存: {{ item.product.stock }}
                      </div>
                      <div class="product-category" v-if="item.product.categoryName">
                        <i class="fas fa-tag"></i>
                        {{ item.product.categoryName }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="product-price">
                  <div class="price-container">
                    <span class="current-price">
                      ¥{{ item.product.isDiscount ? item.product.discountPrice : item.product.price }}
                    </span>
                    <span class="original-price" v-if="item.product.isDiscount">
                      ¥{{ item.product.price }}
                    </span>
                    <div class="discount-badge" v-if="item.product.isDiscount">
                      <i class="fas fa-tag"></i>
                      优惠
                    </div>
                  </div>
                </div>

                <div class="product-quantity">
                  <div class="quantity-control">
                    <el-button size="small" @click="decreaseQuantity(item)" :disabled="item.quantity <= 1" class="quantity-btn">
                      <el-icon><Minus /></el-icon>
                    </el-button>
                    <el-input-number v-model="item.quantity" :min="1" :max="item.product.stock" size="small" :controls="false"
                      @change="(value) => handleQuantityChange(item, value)" class="quantity-input"></el-input-number>
                    <el-button size="small" @click="increaseQuantity(item)" :disabled="item.quantity >= item.product.stock" class="quantity-btn">
                      <el-icon><Plus /></el-icon>
                    </el-button>
                  </div>
                </div>

                <div class="product-subtotal">
                  <div class="subtotal-amount">
                    <span class="subtotal-symbol">¥</span>
                    <span class="subtotal-number">{{ calculateSubtotal(item).toFixed(2) }}</span>
                  </div>
                </div>

                <div class="product-actions">
                  <div class="action-buttons">
                    <el-button link :class="['favorite-btn', { 'is-favorite': item.product.isFavorite }]" @click="handleToggleFavorite(item)">
                      <i :class="item.product.isFavorite ? 'fas fa-heart' : 'far fa-heart'"></i>
                      {{ item.product.isFavorite ? '已收藏' : '收藏' }}
                    </el-button>
                    <el-button link class="delete-btn" @click="handleDelete(item)">
                      <i class="fas fa-trash-alt"></i>
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </transition-group>
          </div>

          <!-- 收货地址选择区域 -->
          <div class="address-section" v-if="cartItems.some(item => item.selected)">
            <div class="address-header">
              <div class="section-title">
                <i class="fas fa-map-marker-alt"></i>
                收货地址
              </div>
              <div class="required-badge">必选</div>
            </div>
            <div class="address-content">
              <div class="address-selector">
                <el-select v-model="selectedAddressId" placeholder="请选择收货地址" class="address-select" size="large">
                  <el-option v-for="addr in addresses" :key="addr.id" :label="formatAddress(addr)" :value="addr.id">
                    <div class="address-option">
                      <div class="receiver-info">
                        <span class="receiver-name">{{ addr.receiver }}</span>
                        <span class="receiver-phone">{{ addr.phone }}</span>
                      </div>
                      <div class="address-detail">{{ addr.address }}</div>
                    </div>
                  </el-option>
                </el-select>
                <el-button type="primary" plain @click="$router.push('/user-center')" class="add-address-btn">
                  <i class="fas fa-plus"></i>
                  添加新地址
                </el-button>
              </div>
            </div>
          </div>

          <!-- 购物车底部 -->
          <div class="cart-footer">
            <div class="footer-left">
              <div class="footer-select">
                <el-checkbox v-model="allSelected" @change="handleSelectAll" class="footer-select-all">
                  <span class="select-all-text">全选</span>
                </el-checkbox>
              </div>
              <div class="footer-actions">
                <el-button link class="batch-delete-btn" @click="handleBatchDelete" :disabled="selectedCount === 0">
                  <i class="fas fa-trash-alt"></i>
                  删除选中图书
                </el-button>
              </div>
            </div>
            <div class="footer-right">
              <div class="checkout-summary">
                <div class="summary-row">
                  <span class="summary-label">已选择</span>
                  <span class="summary-value">{{ selectedCount }} 本图书</span>
                </div>
                <div class="summary-row total-row">
                  <span class="summary-label">合计：</span>
                  <div class="total-price">
                    <span class="currency">¥</span>
                    <span class="amount">{{ totalPrice.toFixed(2) }}</span>
                  </div>
                </div>
              </div>
              <el-button type="success" size="large" :disabled="selectedCount === 0 || !selectedAddressId" @click="handleBalanceCheckout" class="checkout-btn balance-btn">
                <i class="fas fa-wallet"></i>
                <span>余额支付</span>
              </el-button>
              <el-button type="primary" size="large" :disabled="selectedCount === 0 || !selectedAddressId" @click="handleCheckout" class="checkout-btn">
                <i class="fab fa-alipay"></i>
                <span>支付宝支付</span>
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import axios from 'axios'
import eventBus from '@/utils/eventBus'
import logger from '@/utils/logger'
import { Minus, Plus } from '@element-plus/icons-vue'

export default {
  name: 'Cart',
  components: {
    FrontHeader,
    FrontFooter,
    Minus,
    Plus
  },
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: false,
      cartItems: [],
      allSelected: false,
      selectedAddressId: null,
      addresses: []
    }
  },
  computed: {
    totalItems() {
      return this.cartItems.length
    },
    selectedCount() {
      return this.cartItems.filter(item => item.selected).length
    },
    totalPrice() {
      return this.cartItems
        .filter(item => item.selected)
        .reduce((total, item) => {
          const price = item.product.isDiscount === 1 ? item.product.discountPrice : item.product.price
          return total + price * item.quantity
        }, 0)
    }
  },
 
  methods: {
    // 获取购物车数据
    async getCartItems() {
      this.loading = true
      try {
        const userId = this.userInfo.id
        const res = await Request.get(`/cart/user/${userId}`)
        if (res.code === '0') {
          // 使用Vue.set确保响应式数据
          const cartData = res.data ? res.data.map(item => ({
            ...item,
            selected: false
          })) : []
          
          // 清空原数组并重新赋值以确保响应式
          this.cartItems.splice(0, this.cartItems.length, ...cartData)
          
          // 如果购物车为空，确保显示空状态
          if (!res.data || res.data.length === 0) {
            this.cartItems.splice(0, this.cartItems.length)
          }
          
          // 强制更新一次以确保UI同步
          this.$forceUpdate()
          
          // 异步获取商品收藏状态（不阻塞购物车加载，失败不影响主流程）
          // 使用 catch 确保即使失败也不会影响购物车显示
          this.getFavoriteStatus().catch(() => {
            // 静默处理，已在 getFavoriteStatus 内部处理
          })
        }
      } catch (error) {
        logger.error('获取购物车失败:', error)
        this.$message.error('获取购物车失败')
      } finally {
        this.loading = false
      }
    },
    
    // 获取商品收藏状态
    async getFavoriteStatus() {
      if (!this.userInfo?.id || this.cartItems.length === 0) return
      
      try {
        logger.debug('购物车获取收藏状态开始:', {
          '用户ID': this.userInfo.id,
          '购物车商品数量': this.cartItems.length,
          '购物车商品ID列表': this.cartItems.map(item => item.product.id)
        })
        
        // 标记为静默请求，超时时不显示错误消息
        const res = await Request.get(`/favorite/user/${this.userInfo.id}`, {
          silent: true,
          timeout: 10000 // 10秒超时
        })
        
        logger.debug('购物车收藏状态API响应:', res)
        
        if (res.code === '0') {
          const favorites = res.data || []
          
          logger.debug('解析收藏列表:', {
            '收藏总数': favorites.length,
            '收藏详情': favorites
          })
          
          // 使用Vue响应式更新购物车商品的收藏状态
          this.cartItems.forEach((item, index) => {
            const favoriteItem = favorites.find(f => f.productId === item.product.id)
            const isFavorite = favoriteItem && favoriteItem.status === 1
            
            logger.debug(`商品收藏状态更新:`, {
              '商品ID': item.product.id,
              '商品名称': item.product.name,
              '找到的收藏记录': favoriteItem,
              '最终收藏状态': isFavorite
            })
            
            this.cartItems[index].product.isFavorite = isFavorite
          })
        }
      } catch (error) {
        // 静默处理错误，不显示错误消息，只记录debug日志
        // 如果获取失败，商品默认不显示收藏状态（isFavorite保持undefined或false）
        if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
          logger.debug('购物车收藏状态请求超时（静默处理）:', error.message)
        } else {
          logger.debug('获取收藏状态失败（静默处理）:', error.message)
        }
        // 不设置 isFavorite，保持默认状态
      }
    },
    
    // 更新商品数量
    async handleQuantityChange(item, value) {
      const originalQuantity = item.quantity
      
      // 立即更新本地状态以获得即时反馈
      item.quantity = value
      
      try {
        await Request.put(`/cart/${item.id}?quantity=${value}`)
        this.$message.success('更新成功')
        
        // 强制更新购物车统计
        this.$forceUpdate()
      } catch (error) {
        logger.error('更新数量失败:', error)
        this.$message.error('更新数量失败')
        // 恢复原数量
        item.quantity = originalQuantity
      }
    },
    // 删除
    async handleDelete(item) {
      try {
        await this.$confirm('确定要删除该图书吗？', '提示', {
          type: 'warning'
        })
         Request.delete(`/cart/${item.id}`).then(res=>{
          if(res.code === '0'){
            this.$message.success('删除成功')
            // 更新本地数据以立即反映删除操作
            const index = this.cartItems.findIndex(i => i.id === item.id)
            if (index !== -1) {
              this.cartItems.splice(index, 1)
            }
            // 如果已经没有商品，确保更新全选状态
            if (this.cartItems.length === 0) {
              this.allSelected = false
            }
          }
         })
       
      } catch (error) {
        if (error !== 'cancel') {
          logger.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    // 批量删除
    async handleBatchDelete(isCheckout = false) {
      try {
        const selectedItems = this.cartItems.filter(item => item.selected)
        if (selectedItems.length === 0 && !isCheckout) {
          this.$message.warning('请选择要删除的图书')
          return
        }

        if (!isCheckout) {
          await this.$confirm('确定要删除选中的图书吗？', '提示', {
            type: 'warning'
          })
        }

        const ids = selectedItems.map(item => item.id).join(',')
        const res = await Request.delete(`/cart/batch?ids=${ids}`)
        if (res.code === '0') {
          if (!isCheckout) {
            this.$message.success('删除成功')
          }
          // 直接从本地列表中移除被删除的商品
          for (const item of selectedItems) {
            const index = this.cartItems.findIndex(i => i.id === item.id)
            if (index !== -1) {
              this.cartItems.splice(index, 1)
            }
          }
          // 如果没有商品了，重置全选状态
          if (this.cartItems.length === 0) {
            this.allSelected = false
          }
        }
      } catch (error) {
        if (error !== 'cancel') {
          logger.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    // 全选/取消全选
    handleSelectAll(val) {
      this.cartItems.forEach((item, index) => {
        this.cartItems[index].selected = val
      })
    },
    // 单个商品选择
    handleItemSelect() {
      this.allSelected = this.cartItems.every(item => item.selected)
      // 强制更新以确保UI同步
      this.$forceUpdate()
    },
    // 结算 - 余额支付（一键支付）
    async handleBalanceCheckout() {
      if (this.addresses.length === 0) {
        this.$message.warning('您还没有添加收货地址')
        this.$router.push('/user-center')
        return
      }
      
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }

      try {
        const selectedItems = this.cartItems.filter(item => item.selected)
        const selectedAddress = this.addresses.find(addr => addr.id === this.selectedAddressId)
        
        // 创建订单数据
        const orderData = {
          cartIds: selectedItems.map(item => item.id),
          userId: this.userInfo.id,
          recvName: selectedAddress.receiver,
          recvAddress: selectedAddress.address,
          recvPhone: selectedAddress.phone
        }
        
        // 创建订单
        const loading = this.$loading({
          lock: true,
          text: '订单处理中...',
          background: 'rgba(255,255,255,0.8)'
        })
        
        const orderRes = await Request.post('/order/cart/create', orderData)
        
        if (orderRes.code === '0') {
          const { outTradeNo, orderCount } = orderRes.data
          
          // 直接调用余额支付接口
          const payRes = await Request.post('/order/cart/pay-balance', { outTradeNo })
          loading.close()
          
          if (payRes.code === '0') {
            this.$message.success(`支付成功！已完成${orderCount}个订单`)
            
            // 刷新购物车
            await this.getCartItems()
            
            // 跳转到订单页面
            setTimeout(() => {
              this.$router.push('/order')
            }, 1500)
          } else {
            this.$message.error(payRes.msg || '支付失败')
          }
        } else {
          loading.close()
          this.$message.error(orderRes.msg || '创建订单失败')
        }
      } catch (error) {
        logger.error('支付失败:', error)
        this.$message.error('支付失败：' + (error.message || '未知错误'))
      }
    },
    
    // 结算 - 支付宝支付
    async handleCheckout() {
      if (this.addresses.length === 0) {
        this.$message.warning('您还没有添加收货地址')
        this.$router.push('/user-center')
        return
      }
      
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }

      try {
        const selectedItems = this.cartItems.filter(item => item.selected)
        const selectedAddress = this.addresses.find(addr => addr.id === this.selectedAddressId)
        
        // 创建订单数据
        const orderData = {
          cartIds: selectedItems.map(item => item.id),
          userId: this.userInfo.id,
          recvName: selectedAddress.receiver,
          recvAddress: selectedAddress.address,
          recvPhone: selectedAddress.phone
        }
        
        // 创建订单
        const loading = this.$loading({
          lock: true,
          text: '订单处理中...',
          background: 'rgba(255,255,255,0.8)'
        })
        
        const orderRes = await Request.post('/order/cart/create', orderData)
        loading.close()
        
        if (orderRes.code === '0') {
          const { outTradeNo, totalAmount, orderCount } = orderRes.data
          
          this.$message.success(`成功创建${orderCount}个订单，正在跳转支付...`)
          
          // 准备支付宝支付参数
          const currentDate = new Date().toLocaleDateString('zh-CN')
          const orderName = `${currentDate}-在线销售平台-${this.userInfo.username || this.userInfo.name}-购物车订单`
          
          // 调用支付宝支付接口
          const payLoading = this.$loading({
            lock: true,
            text: '正在跳转支付页面...',
            background: 'rgba(255,255,255,0.8)'
          })
          
          setTimeout(async () => {
            try {
              // 使用axios直接调用支付接口，设置响应类型为text以接收HTML
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
              
              payLoading.close()
              
              // 将支付页面HTML写入新窗口
              const div = document.createElement('div')
              div.innerHTML = response.data
              document.body.appendChild(div)
              document.querySelector('form').submit()
              document.body.removeChild(div)
            } catch (error) {
              payLoading.close()
              logger.error('支付失败:', error)
              this.$message.error('支付失败：' + (error.response?.data || error.message))
            }
          }, 1000)
        } else {
          this.$message.error(orderRes.msg || '创建订单失败')
        }
      } catch (error) {
        logger.error('下单失败:', error)
        this.$message.error('下单失败：' + (error.message || '未知错误'))
      }
    },
    async getAddresses() {
      try {
          const userId = this.userInfo.id
        const res = await Request.get(`/address/user/${userId}`)
        if (res.code === '0') {
          this.addresses = res.data
          // 如果只有一个地址，自动选中
          if (this.addresses.length === 1) {
            this.selectedAddressId = this.addresses[0].id
          }
        }
      } catch (error) {
        logger.error('获取地址列表失败:', error)
        this.$message.error('获取地址列表失败')
      }
    },
    formatAddress(addr) {
      return `${addr.phone} ${addr.address}`
    },
    // 添加小计计算方法
    calculateSubtotal(item) {
      const price = item.product.isDiscount === 1 ? item.product.discountPrice : item.product.price
      return price * item.quantity
    },
    
    // 新增方法：数量增减
    increaseQuantity(item) {
      if (item.quantity < item.product.stock) {
        const newQuantity = item.quantity + 1
        this.handleQuantityChange(item, newQuantity)
      }
    },
    
    decreaseQuantity(item) {
      if (item.quantity > 1) {
        const newQuantity = item.quantity - 1
        this.handleQuantityChange(item, newQuantity)
      }
    },
    
    // 收藏商品切换
    async handleToggleFavorite(item) {
      if (!this.userInfo.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }

      try {
        // 记录操作前的状态
        const beforeStatus = item.product.isFavorite
        
        logger.debug('购物车收藏操作开始:', {
          productId: item.product.id,
          productName: item.product.name,
          beforeStatus: beforeStatus,
          beforeStatusType: typeof beforeStatus,
          expectOperation: beforeStatus ? '取消收藏' : '收藏',
          商品当前收藏状态: beforeStatus ? '已收藏' : '未收藏',
          商品对象: item.product
        })
        
        // 🔄 购物车恢复原始源码的正确收藏逻辑
        // 原始源码逻辑: status: this.product.isFavorite ? 1 : 0
        // 与ProductCard保持一致的正确逻辑
        const targetStatus = beforeStatus ? 1 : 0
        
        logger.debug('购物车恢复原始源码收藏逻辑:', {
          '原始源码逻辑': 'status: isFavorite ? 1 : 0',
          '当前商品状态': beforeStatus ? '已收藏' : '未收藏',
          '用户操作意图': beforeStatus ? '取消收藏' : '添加收藏',
          '发送给服务器的状态': targetStatus,
          '说明': '已恢复原始源码的正确逻辑',
          '商品信息': item.product.name
        })
        
        const data = {
          userId: this.userInfo.id,
          productId: item.product.id,
          status: targetStatus
        }
        
        logger.debug('购物车发送收藏请求详情:', {
          '请求数据': data,
          '用户ID': data.userId,
          '商品ID': data.productId,
          '商品名称': item.product.name,
          '发送的状态值': data.status,
          '状态值类型': typeof data.status,
          '逻辑状态': '✅ 已恢复原始源码的正确逻辑'
        })
        
        const res = await Request.post('/favorite', data)
        
        logger.debug('收藏接口响应:', {
          code: res.code,
          data: res.data,
          status: res.data?.status,
          msg: res.msg,
          fullResponse: res
        })
        
        if (res.code === '0') {
          // 检查服务器是否真正处理了收藏操作
          const serverConfirmedStatus = res.data?.status
          
          logger.debug('购物车收藏 - 服务器响应详情:', {
            发送的状态: targetStatus,
            服务器返回状态: serverConfirmedStatus,
            服务器返回状态类型: typeof serverConfirmedStatus,
            操作意图: targetStatus === 1 ? '收藏' : '取消收藏',
            商品信息: item.product.name,
            完整响应: res,
            响应数据: res.data
          })
          
          if (serverConfirmedStatus !== undefined && serverConfirmedStatus !== null) {
            // 服务器确认状态处理 - 兼容不同的返回格式
            const isNowFavorite = serverConfirmedStatus === 1 || serverConfirmedStatus === '1' || serverConfirmedStatus === true
            
            logger.debug('购物车收藏状态处理:', {
              原始服务器状态: serverConfirmedStatus,
              处理后收藏状态: isNowFavorite,
              操作类型: isNowFavorite ? '收藏成功' : '取消收藏成功',
              商品: item.product.name
            })
            
            // 更新UI状态 - 基于服务器确认状态
            item.product.isFavorite = isNowFavorite
            
            // 显示正确的操作反馈
            this.$message.success(isNowFavorite ? '收藏成功' : '已取消收藏')
            
            logger.debug('购物车收藏操作完成:', {
              productId: item.product.id,
              productName: item.product.name,
              finalStatus: isNowFavorite,
              serverConfirmed: true,
              operationType: isNowFavorite ? '执行收藏' : '执行取消收藏'
            })
            
            // 触发全局收藏状态更新事件，同步其他页面
            eventBus.emit('favoriteStatusChanged', {
              productId: item.product.id,
              status: isNowFavorite
            })
          } else {
            logger.error('购物车收藏 - 服务器响应缺少status字段:', res)
            this.$message({
              type: 'error',
              message: '收藏状态异常，请刷新页面重试'
            })
          }
        } else {
          logger.error('收藏操作失败:', res.msg)
          this.$message.error(res.msg || '操作失败')
        }
      } catch (error) {
        logger.error('收藏操作异常:', error)
        this.$message.error('操作失败')
      }
    },

    // 验证购物车商品的收藏状态
    async verifyCartItemFavoriteStatus(item) {
      try {
        logger.debug(`验证购物车商品 ${item.product.id} 的收藏状态...`)
        
        const userId = this.userInfo.id
        const res = await Request.get(`/favorite/check?userId=${userId}&productId=${item.product.id}`)
        
        logger.debug('购物车商品收藏状态验证结果:', {
          productId: item.product.id,
          productName: item.product.name,
          response: res,
          currentFrontendStatus: item.product.isFavorite
        })
        
        if (res.code === '0') {
          const serverStatus = res.data?.isFavorite || false
          
          if (serverStatus !== item.product.isFavorite) {
            logger.warn('发现购物车商品前端与服务器状态不一致:', {
              productId: item.product.id,
              frontendStatus: item.product.isFavorite,
              serverStatus: serverStatus
            })
            
            // 同步服务器状态
            item.product.isFavorite = serverStatus
            
            // 触发全局状态更新
            eventBus.emit('favoriteStatusChanged', {
              productId: item.product.id,
              status: serverStatus
            })
            
            this.$message({
              type: 'info',
              message: `${item.product.name} 收藏状态已同步`
            })
          } else {
            logger.debug('购物车商品前端与服务器状态一致')
          }
        }
      } catch (error) {
        logger.error('验证购物车商品收藏状态失败:', error)
      }
    },

    // 处理全局收藏状态变化
    handleGlobalFavoriteChange(data) {
      logger.debug('收到全局收藏状态变化:', data)
      
      // 更新购物车中对应商品的收藏状态
      this.cartItems.forEach((item, index) => {
        if (item.product.id === data.productId) {
          this.cartItems[index].product.isFavorite = data.status
          logger.debug(`更新购物车商品 ${item.product.name} 收藏状态为:`, data.status)
        }
      })
    }
  },
  created() {
    const userInfo = localStorage.getItem('frontUser')
    if(!userInfo){
      this.$message.warning('请先登录')
      this.$router.push('/login')
    }
    else{
      this.userInfo = JSON.parse(userInfo)
      this.getCartItems()
      this.getAddresses()
    }
    
    // 监听全局收藏状态变化事件，实现页面间收藏状态同步
    eventBus.on('favoriteStatusChanged', this.handleGlobalFavoriteChange)
  },
  
  beforeUnmount() {
    // Vue 3 使用 beforeUnmount 替代 beforeDestroy
    // 移除事件监听器
    eventBus.off('favoriteStatusChanged', this.handleGlobalFavoriteChange)
  },
  mounted() {
 
  }
}
</script>

<style scoped>
/* 整体布局样式 */
.cart-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #ffffff 0%, #f5f7fa 50%, #ecf5ff 100%);
  position: relative;
  overflow-x: hidden;
}

.cart-page::before {
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

.cart-stats {
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
}

.stats-item:hover {
  transform: translateY(-2px);
  background: rgba(58, 91, 160, 0.15);
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

/* 购物车内容样式 */
.cart-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  min-height: 400px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  animation: slideInFromBottom 0.8s ease-out 0.2s both;
}

@keyframes slideInFromBottom {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空购物车样式 */
.empty-cart {
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

.empty-items {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 40px;
  height: 40px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-item {
  width: 8px;
  height: 8px;
  background: rgba(58, 91, 160, 0.3);
  border-radius: 50%;
  animation: floatItems 2s ease-in-out infinite;
}

.empty-item:nth-child(2) { animation-delay: 0.3s; }
.empty-item:nth-child(3) { animation-delay: 0.6s; }

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

@keyframes floatItems {
  0%, 100% { transform: translateY(0px) scale(1); opacity: 0.3; }
  50% { transform: translateY(-15px) scale(1.2); opacity: 0.8; }
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

/* 表头样式 */
.cart-header {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  align-items: center;
  padding: 20px 32px;
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.8));
  border-radius: 24px 24px 0 0;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
}

.header-select,
.header-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-item i {
  color: var(--primary-color);
  font-size: 16px;
}

.select-text {
  font-weight: 600;
  color: #64748b;
}

/* 商品项样式 */
.cart-item {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  transform: translateY(20px);
}

.cart-item:hover {
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.04), rgba(255, 255, 255, 0.5));
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.12);
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 商品信息样式 */
.product-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-image-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
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
  border-radius: 12px;
}

.product-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.image-overlay i {
  color: white;
  font-size: 20px;
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: 16px;
  color: #1a202c;
  margin-bottom: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: color 0.3s ease;
  line-height: 1.4;
}

.product-name:hover {
  color: var(--primary-color);
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-stock,
.product-category {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  padding: 4px 8px;
  background: rgba(58, 91, 160, 0.08);
  border-radius: 8px;
  width: fit-content;
}

.product-stock i,
.product-category i {
  font-size: 12px;
  color: var(--primary-color);
}

/* 价格样式 */
.product-price {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.price-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.current-price {
  color: #ff6b6b;
  font-weight: 700;
  font-size: 18px;
}

.original-price {
  font-size: 14px;
  color: #9ca3af;
  text-decoration: line-through;
}

.discount-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 600;
}

.discount-badge i {
  font-size: 10px;
}

/* 数量控制样式 */
.product-quantity {
  display: flex;
  justify-content: center;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid rgba(58, 91, 160, 0.2);
  border-radius: 16px;
  padding: 4px;
  transition: all 0.3s ease;
}

.quantity-control:hover {
  border-color: rgba(58, 91, 160, 0.4);
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.12);
}

.quantity-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gradient-primary);
  border: none;
  color: white;
  transition: all 0.3s ease;
}

.quantity-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.3);
}

.quantity-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  transform: none;
  box-shadow: none;
}

.quantity-input {
  width: 80px;
}

.quantity-input :deep(.el-input__inner),
.quantity-input :deep(.el-input__wrapper) {
  text-align: center;
  border: none;
  background: transparent;
  font-weight: 600;
  font-size: 16px;
  color: #1a202c;
}

.quantity-input :deep(.el-input__inner) {
  padding: 0;
  text-align: center;
}

.quantity-input :deep(input) {
  text-align: center;
  font-weight: 600;
  font-size: 16px;
  color: #1a202c;
}

/* 小计样式 */
.product-subtotal {
  display: flex;
  justify-content: center;
}

.subtotal-amount {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.subtotal-symbol {
  color: #ff6b6b;
  font-weight: 600;
  font-size: 14px;
}

.subtotal-number {
  color: #ff6b6b;
  font-weight: 700;
  font-size: 20px;
}

/* 操作按钮样式 */
.product-actions {
  display: flex;
  justify-content: center;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.favorite-btn,
.delete-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;
}

.favorite-btn {
  color: #64748b;
  background: rgba(100, 116, 139, 0.1);
}

.favorite-btn:hover {
  background: rgba(100, 116, 139, 0.2);
  transform: translateY(-1px);
}

.favorite-btn.is-favorite {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.favorite-btn.is-favorite:hover {
  background: rgba(255, 107, 107, 0.2);
}

.delete-btn {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: translateY(-1px);
}

/* 收货地址区域样式 */
.address-section {
  padding: 24px 32px;
  border-bottom: 1px solid rgba(58, 91, 160, 0.1);
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.04), rgba(255, 255, 255, 0.8));
  animation: slideInFromLeft 0.8s ease-out 0.4s both;
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.address-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  color: #1a202c;
  font-weight: 600;
}

.section-title i {
  color: var(--primary-color);
  font-size: 20px;
}

.required-badge {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  color: white;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 600;
}

.address-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.address-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 600px;
}

.address-select {
  flex: 1;
  min-width: 300px;
}

.address-select :deep(.el-input__inner) {
  border: 1.5px solid rgba(58, 91, 160, 0.2);
  border-radius: 16px;
  padding: 12px 20px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.08);
}

.address-select :deep(.el-input__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(58, 91, 160, 0.12), 0 4px 16px rgba(58, 91, 160, 0.15);
}

/* 优化地址下拉选项的显示 */
.address-select :deep(.el-select-dropdown__item) {
  height: auto !important;
  min-height: 90px !important;
  max-height: none !important;
  padding: 0 !important;
  line-height: 1.5 !important;
  white-space: normal !important;
  overflow: visible !important;
}

.address-select :deep(.el-select-dropdown__item:hover) {
  background-color: rgba(58, 91, 160, 0.08) !important;
}

.address-select :deep(.el-select-dropdown) {
  max-height: 350px;
  overflow-y: auto;
}

.address-select :deep(.el-select-dropdown__item .el-select-dropdown__item-label) {
  height: auto !important;
  min-height: 90px !important;
  line-height: 1.5 !important;
  white-space: normal !important;
  overflow: visible !important;
  display: block !important;
}

.address-option {
  padding: 20px 16px !important;
  min-height: 90px;
  line-height: 1.5;
  width: 100%;
  box-sizing: border-box;
  display: block;
}

.receiver-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  width: 100%;
}

.receiver-name {
  color: #1a202c;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: none;
}

.receiver-phone {
  color: #64748b;
  font-size: 14px;
  background: rgba(58, 91, 160, 0.1);
  padding: 6px 12px;
  border-radius: 8px;
  white-space: nowrap;
  flex-shrink: 0;
  font-weight: 500;
}

.address-detail {
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: normal;
  margin-top: 4px;
  width: 100%;
  display: block;
}

.add-address-btn {
  border: 1.5px solid rgba(58, 91, 160, 0.3);
  color: var(--primary-color);
  border-radius: 16px;
  padding: 12px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.add-address-btn:hover {
  background: rgba(58, 91, 160, 0.1);
  border-color: var(--primary-color);
  transform: translateY(-2px);
}

/* 底部样式 */
.cart-footer {
  padding: 24px 32px;
  background: linear-gradient(135deg, rgba(58, 91, 160, 0.06), rgba(255, 255, 255, 0.9));
  border-radius: 0 0 24px 24px;
  border-top: 1px solid rgba(58, 91, 160, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: slideInFromBottom 0.8s ease-out 0.6s both;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.footer-select {
  display: flex;
  align-items: center;
}

.footer-select-all .select-all-text {
  font-weight: 600;
  color: #64748b;
  margin-left: 8px;
}

.footer-actions {
  display: flex;
  align-items: center;
}

.batch-delete-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ef4444;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.batch-delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  transform: translateY(-1px);
}

.batch-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.checkout-summary {
  text-align: right;
}

.summary-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.summary-row:last-child {
  margin-bottom: 0;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.summary-value {
  font-size: 14px;
  color: #1a202c;
  font-weight: 600;
}

.total-row {
  margin-top: 4px;
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
  font-size: 28px;
  color: #ff6b6b;
  font-weight: 700;
}

.checkout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 32px;
  font-size: 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border: none;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  margin-left: 12px;
}

.checkout-btn.balance-btn {
  background: var(--gradient-primary);
  margin-left: 0;
}

.checkout-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #66b1ff, #409EFF);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.checkout-btn.balance-btn::before {
  background: linear-gradient(135deg, var(--primary-light), var(--primary-color));
}

.checkout-btn.balance-btn:hover:not(:disabled) {
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.3);
}

.checkout-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.3);
}

.checkout-btn.balance-btn:hover:not(:disabled) {
  box-shadow: 0 8px 25px rgba(58, 91, 160, 0.3);
}

.checkout-btn:hover:not(:disabled)::before {
  opacity: 1;
}

.checkout-btn span,
.checkout-btn i {
  position: relative;
  z-index: 1;
}

.checkout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* Checkbox样式优化 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  transition: all 0.3s ease;
}

:deep(.el-checkbox__inner) {
  border-radius: 6px;
  width: 18px;
  height: 18px;
  transition: all 0.3s ease;
}

:deep(.el-checkbox__inner:hover) {
  border-color: var(--primary-color);
  transform: scale(1.1);
}

:deep(.el-checkbox__label) {
  color: #64748b;
  font-weight: 500;
}

/* 过渡动画 */
.cart-item-enter-active, 
.cart-item-leave-active {
  transition: all 0.3s ease;
}

.cart-item-enter, 
.cart-item-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.cart-item-move {
  transition: transform 0.3s ease;
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

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    max-width: 100%;
    padding: 24px;
  }
  
  .cart-header,
  .cart-item {
    grid-template-columns: 60px 2fr 0.8fr 0.8fr 0.8fr 0.8fr;
    padding: 20px 24px;
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
  
  .cart-header {
    display: none;
  }
  
  .cart-item {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 20px;
    border-radius: 16px;
    margin-bottom: 16px;
    background: rgba(255, 255, 255, 0.8);
  }
  
  .product-info {
    order: 1;
    width: 100%;
    justify-content: flex-start;
  }
  
  .item-checkbox {
    order: 0;
    align-self: flex-start;
  }
  
  .product-price {
    order: 2;
    align-items: flex-start;
  }
  
  .product-quantity {
    order: 3;
    justify-content: flex-start;
  }
  
  .product-subtotal {
    order: 4;
    justify-content: flex-start;
  }
  
  .product-actions {
    order: 5;
    justify-content: flex-start;
  }
  
  .action-buttons {
    flex-direction: row;
  }
  
  .cart-footer {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }
  
  .footer-left,
  .footer-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .address-selector {
    flex-direction: column;
    gap: 12px;
  }
  
  .address-select {
    width: 100%;
    min-width: auto;
  }
  
  .add-address-btn {
    width: 100%;
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
  
  .cart-stats {
    gap: 12px;
  }
  
  .stats-item {
    padding: 12px 16px;
  }
  
  .cart-content {
    border-radius: 16px;
    padding: 16px;
  }
  
  .cart-item {
    padding: 16px;
  }
  
  .product-image {
    width: 60px;
    height: 60px;
  }
  
  .empty-cart {
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
  
  .address-section {
    padding: 16px;
  }
  
  .cart-footer {
    padding: 16px;
  }
  
  .checkout-btn {
    width: 100%;
    justify-content: center;
  }
}

/* 无障碍性优化 */
@media (prefers-reduced-motion: reduce) {
  .cart-page::before,
  .title-icon,
  .cart-item,
  .page-header,
  .cart-content,
  .address-section,
  .cart-footer,
  .empty-icon {
    animation: none !important;
    transition: opacity 0.2s ease !important;
  }
  
  .cart-item {
    opacity: 1;
    transform: none;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .cart-page {
    background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1e2d1e 100%);
  }
  
  .page-header,
  .cart-content,
  .address-section {
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

/* 全局地址选择下拉框样式覆盖 */
.el-select-dropdown.is-multiple .el-select-dropdown__item,
.el-select-dropdown .el-select-dropdown__item {
  height: auto !important;
  min-height: 90px !important;
  max-height: none !important;
  padding: 0 !important;
  line-height: 1.5 !important;
  white-space: normal !important;
  overflow: visible !important;
}

.el-select-dropdown .el-select-dropdown__item.selected {
  background-color: rgba(58, 91, 160, 0.1) !important;
}

/* 确保地址选项容器完整显示 */
.cart-page .el-select-dropdown {
  z-index: 9999 !important;
}

.cart-page .el-select-dropdown .el-scrollbar__view {
  padding: 0 !important;
}

/* 性能优化 */
.cart-item,
.page-header,
.cart-content,
.address-section,
.cart-footer {
  will-change: transform;
}

.el-select-dropdown .el-select-dropdown__item.selected {
  background-color: rgba(58, 91, 160, 0.1) !important;
}

/* 确保地址选项容器完整显示 */
.cart-page .el-select-dropdown {
  z-index: 9999 !important;
}

.cart-page .el-select-dropdown .el-scrollbar__view {
  padding: 0 !important;
}
</style> 