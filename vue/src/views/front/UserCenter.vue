<template>
  <div class="user-center">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>个人中心</h2>
      </div>

      <div class="content-wrapper">
        <!-- 左侧导航 -->
        <div class="side-menu">
          <el-menu :default-active="activeMenu" class="menu-list" @select="handleMenuSelect">
            <el-menu-item index="info">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="address">
              <el-icon><Location /></el-icon>
              <span>收货地址</span>
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-menu>
        </div>

        <!-- 右侧内容区 -->
        <div class="content-area">
          <!-- 个人信息 -->
          <div v-if="activeMenu === 'info'" class="info-section">
            <div class="section-header">
              <h3>个人信息</h3>
              <p class="section-desc">您可以在这里修改您的个人信息</p>
            </div>
            <el-form :model="userInfo" :rules="rules" ref="userForm" label-width="100px" class="user-form">
              <!-- 头像上传 -->
              <el-form-item label="头像">
                <div class="avatar-upload-section">
                  <div class="avatar-preview">
                    <img v-if="userInfo.businessLicense" :src="'/api' + userInfo.businessLicense" class="avatar-img" />
                    <div v-else class="avatar-placeholder">
                      <el-icon><User /></el-icon>
                    </div>
                  </div>
                  <div class="avatar-upload-btn">
                    <el-upload
                      class="avatar-uploader"
                      action="/api/file/upload/img"
                      :show-file-list="false"
                      :on-success="handleAvatarSuccess"
                      :before-upload="beforeAvatarUpload"
                      :headers="uploadHeaders"
                    >
                      <el-button size="small" type="primary">
                        <el-icon><Upload /></el-icon>
                        上传头像
                      </el-button>
                      <template #tip>
                        <div class="el-upload__tip">支持jpg/png格式，大小不超过2MB</div>
                      </template>
                    </el-upload>
                  </div>
                </div>
              </el-form-item>
              
              <el-form-item label="用户名" prop="username">
                <el-input v-model="userInfo.username" disabled></el-input>
              </el-form-item>
              <el-form-item label="真实姓名" prop="name">
                <el-input v-model="userInfo.name"></el-input>
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userInfo.email"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateUserInfo">保存修改</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 收货地址 -->
          <div v-if="activeMenu === 'address'" class="address-section">
            <div class="section-header">
              <div class="header-left">
                <h3>收货地址</h3>
                <p class="section-desc">管理您的收货地址信息</p>
              </div>
              <el-button type="primary" @click="showAddressDialog('add')">
                <el-icon><Plus /></el-icon>
                新增地址
              </el-button>
            </div>

            <div class="address-list">
              <template v-if="addresses.length === 0">
                <el-empty description="暂无收货地址">
                  <el-button type="primary" @click="showAddressDialog('add')">添加地址</el-button>
                </el-empty>
              </template>
              
              <template v-else>
                <el-card v-for="address in addresses" 
                  :key="address.id" 
                  class="address-item" 
                  shadow="hover"
                >
                  <div class="address-info">
                    <h4 class="receiver">
                      <span class="name">{{ address.receiver }}</span>
                      <span class="phone">{{ address.phone }}</span>
                    </h4>
                    <p class="detail">{{ address.address }}</p>
                  </div>
                  <div class="address-actions">
                    <el-button link @click="showAddressDialog('edit', address)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button link class="delete-btn" @click="deleteAddress(address.id)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </el-card>
              </template>
            </div>
          </div>

          <!-- 修改密码 -->
          <div v-if="activeMenu === 'password'" class="password-section">
            <div class="section-header">
              <h3>修改密码</h3>
              <p class="section-desc">您可以在这里修改您的账号密码</p>
            </div>
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px" class="user-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password></el-input>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password></el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updatePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>

    <!-- 地址编辑对话框 -->
    <el-dialog :title="dialogType === 'add' ? '新增地址' : '编辑地址'" v-model="addressDialogVisible" width="500px" :close-on-click-modal="false">
      <el-form :model="addressForm" :rules="addressRules" ref="addressForm" label-width="100px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addressForm.receiver" placeholder="请输入收货人姓名"></el-input>
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号码"></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input 
            type="textarea" 
            v-model="addressForm.address"
            placeholder="请输入详细地址，如：xx省xx市xx区xx街道xx号"
            :rows="3"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div>
          <el-button @click="addressDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAddress">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import logger from '@/utils/logger'
import { User, Location, Lock, Upload, Plus, Edit, Delete } from '@element-plus/icons-vue'

export default {
  name: 'UserCenter',
  components: {
    FrontHeader,
    FrontFooter,
    User,
    Location,
    Lock,
    Upload,
    Plus,
    Edit,
    Delete
  },
  data() {
    return {
      activeMenu: 'info',
      currentUser: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      userInfo: {
        username: '',
        name: '',
        email: '',
        businessLicense: ''
      },
      uploadHeaders: {
        token: ''
      },
      rules: {
        name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ]
      },
      addresses: [],
      addressDialogVisible: false,
      dialogType: 'add',
      addressForm: {
        id: '',
        receiver: '',
        phone: '',
        address: '',
      },
      addressRules: {
        receiver: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入手机号码', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      },
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { 
            validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入的密码不一致'));
              } else {
                callback();
              }
            }, 
            trigger: 'blur' 
          }
        ]
      }
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
      this.uploadHeaders.token = this.userInfo.token || ''
      this.getUserInfo()
      this.getAddresses()
    }
  },

  methods: {
    // 头像上传成功回调
    handleAvatarSuccess(res, file) {
      if (res.code === '0') {
        this.userInfo.businessLicense = res.data
        this.$message.success('头像上传成功')
        // 立即保存到服务器
        this.updateUserInfo()
      } else {
        this.$message.error(res.msg || '头像上传失败')
      }
    },

    // 头像上传前验证
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('头像图片只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt2M) {
        this.$message.error('头像图片大小不能超过 2MB!')
        return false
      }
      return true
    },

    handleMenuSelect(index) {
      this.activeMenu = index;
    },
    async getUserInfo() {
      try {
        const userId = this.currentUser.id
        const res = await Request.get(`/user/${userId}`)
        if (res.code === '0') {
          this.userInfo = res.data
        }
      } catch (error) {
        logger.error('获取用户信息失败:', error)
        this.$message.error('获取用户信息失败')
      }
    },

    async updateUserInfo() {
      try {
        this.$refs.userForm.validate(async (valid) => {
          if (valid) {
            const res = await Request.put(`/user/${this.userInfo.id}`, this.userInfo)
            if (res.code === '0') {
              this.$message.success('个人信息更新成功')
              // 更新本地存储的用户信息
              const updatedUser = { ...this.currentUser, ...this.userInfo }
              localStorage.setItem('frontUser', JSON.stringify(updatedUser))
              // 触发全局事件，通知其他组件更新
              window.dispatchEvent(new Event('userInfoUpdated'))
            }
          }
        })
      } catch (error) {
        logger.error('更新用户信息失败:', error)
        this.$message.error('更新用户信息失败')
      }
    },

    async getAddresses() {
      try {
        const userId = this.currentUser.id
        const res = await Request.get(`/address/user/${userId}`)
        if (res.code === '0') {
          this.addresses = res.data
        }
      } catch (error) {
        logger.error('获取地址列表失败:', error)
        this.$message.error('获取地址列表失败')
      }
    },

    showAddressDialog(type, address) {
      this.dialogType = type
      this.addressDialogVisible = true
      if (type === 'edit' && address) {
        this.addressForm = {
          id: address.id,
          receiver: address.receiver,
          phone: address.phone,
          address: address.address
        }
      } else {
        this.addressForm = {
          receiver: '',
          phone: '',
          address: ''
        }
      }
    },

    async saveAddress() {
      try {
        await this.$refs.addressForm.validate()
        const userId = this.currentUser.id
        const addressData = {
          ...this.addressForm,
          userId,
        }

        if (this.dialogType === 'add') {
          const res = await Request.post('/address', addressData)
          if (res.code === '0') {
            this.$message.success('添加地址成功')
            this.getAddresses()
          }
        } else {
          const res = await Request.put(`/address/${addressData.id}`, addressData)
          if (res.code === '0') {
            this.$message.success('更新地址成功')
            this.getAddresses()
          }
        }
        this.addressDialogVisible = false
      } catch (error) {
        logger.error('保存地址失败:', error)
        this.$message.error('保存地址失败')
      }
    },

    async deleteAddress(id) {
      try {
        await this.$confirm('确定要删除该地址吗？', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/address/${id}`)
        if (res.code === '0') {
          this.$message.success('删除地址成功')
          this.getAddresses()
        }
      } catch (error) {
        if (error !== 'cancel') {
          logger.error('删除地址失败:', error)
          this.$message.error('删除地址失败')
        }
      }
    },

    async updatePassword() {
      try {
        this.$refs.passwordForm.validate(async (valid) => {
          if (valid) {
            const userId = this.currentUser.id
            const params = {
        
              newPassword: this.passwordForm.newPassword,
              oldPassword: this.passwordForm.oldPassword
            }
            const res = await Request.put(`/user/password/${userId}`, params)
            if (res.code === '0') {
              this.$message.success('密码修改成功')
              this.passwordForm = {
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
              }
              this.$refs.passwordForm.resetFields()
            } else {
              this.$message.error(res.msg || '密码修改失败')
            }
          }
        })
      } catch (error) {
        logger.error('修改密码失败:', error)
        this.$message.error('修改密码失败')
      }
    }
  }
}
</script>

<style scoped>
.user-center {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8faf5 0%, #f5f7fa 100%);
}

.main-content {
  flex: 1;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* 页面标题样式 */
.page-header {
  margin-bottom: 24px;
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #67C23A, #85ce61);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: #2c3e50;
  margin: 0;
}

.content-wrapper {
  display: flex;
  gap: 24px;
}

/* 左侧菜单样式 */
.side-menu {
  width: 240px;
  background: white;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.menu-list {
  border-right: none;
}

.menu-list .el-menu-item {
  height: 56px;
  line-height: 56px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.menu-list .el-menu-item:hover {
  background: linear-gradient(to right, rgba(103, 194, 58, 0.05), transparent);
}

.menu-list .el-menu-item.is-active {
  background: linear-gradient(to right, rgba(103, 194, 58, 0.1), transparent);
  border-right: 3px solid #67C23A;
}

.menu-list .el-menu-item i {
  font-size: 18px;
  margin-right: 12px;
  color: #909399;
}

.menu-list .el-menu-item.is-active i {
  color: #67C23A;
}

/* 右侧内容区样式 */
.content-area {
  flex: 1;
  background: white;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  padding: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.header-left h3 {
  font-size: 24px;
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.section-desc {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

/* 表单样式 */
.user-form {
  max-width: 500px;
}

/* 头像上传样式 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e4e7ed;
  background: #f5f7fa;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #c0c4cc;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}

.avatar-upload-btn {
  flex: 1;
}

.avatar-uploader {
  display: block;
}

:deep(.avatar-uploader .el-upload) {
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.el-upload__tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

/* 地址列表样式 */
.address-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.address-item {
  transition: all 0.3s ease;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.address-item:hover {
  transform: translateY(-2px);
  background: linear-gradient(to right, rgba(103, 194, 58, 0.02), transparent);
}

.address-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(to bottom, #67C23A, #85ce61);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.address-item:hover::before {
  opacity: 1;
}

.address-info {
  flex: 1;
}

.receiver {
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.phone {
  color: #606266;
  font-weight: normal;
}

.detail {
  color: #606266;
  margin: 0;
  line-height: 1.5;
}

.address-actions {
  margin-top: 16px;
  display: flex;
  gap: 16px;
}

.delete-btn {
  color: #f56c6c;
}

/* 按钮样式 */
.el-button--primary {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.el-button--primary:hover {
  transform: translateY(-1px);
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
  margin: 0;
  background: linear-gradient(135deg, #f8faf5, #fff);
}

:deep(.el-dialog__body) {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 自定义滚动条样式 */
:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 3px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f0f2f5;
  margin-top: 0;
}

/* 表单项样式 */
:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border-radius: 6px;
  transition: all 0.3s ease;
  border-color: #dcdfe6;
}

:deep(.el-input__inner:hover),
:deep(.el-textarea__inner:hover) {
  border-color: #67c23a;
}

:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus) {
  border-color: #67c23a;
}

/* 空状态样式 */
:deep(.el-empty) {
  padding: 40px 0;
}

:deep(.el-empty .el-button--primary) {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
  transition: all 0.3s ease;
}

:deep(.el-empty .el-button--primary:hover) {
  transform: translateY(-1px);
}

/* 密码表单样式 */
.password-section {
  width: 100%;
}

:deep(.el-empty .el-button--primary) {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
  transition: all 0.3s ease;
}

:deep(.el-empty .el-button--primary:hover) {
  transform: translateY(-1px);
}
</style> 