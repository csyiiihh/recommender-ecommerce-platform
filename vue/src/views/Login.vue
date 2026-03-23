<template>
  <div class="login-container">
    <!-- 中国风装饰元素 -->
    <div class="chinese-decoration">
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
      <div class="mountain-pattern"></div>
    </div>

    <div class="login-header">
      <div class="logo-section">
        <div class="logo-icon">
          <i class="fas fa-book-open"></i> <!-- 更换为书籍图标 -->
        </div>
        <h2 class="login-title">欢迎登录</h2>
      </div>
    </div>

    <el-form ref="loginForm" :model="loginForm" :rules="rules" class="login-form">
      <el-form-item prop="username">
        <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password">
        <el-input
            type="password"
            v-model="loginForm.password"
            placeholder="请输入密码"
            show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <div class="validate-container">
          <el-input
              v-model="loginForm.validCode"
              placeholder="验证码">
            <template #prefix>
              <el-icon><KeyIcon /></el-icon>
            </template>
          </el-input>
          <ValidCode @input="createValidCode" class="validate-code" />
        </div>
      </el-form-item>

      <div class="login-options">
        <div class="checkbox-wrapper">
          <el-checkbox v-model="rememberMe" class="remember-checkbox">
            <span>记住密码</span>
          </el-checkbox>
        </div>
        <el-link type="primary" @click="$router.push('/forget')" class="forget-link">
          <i class="fas fa-question-circle"></i>
          忘记密码？
        </el-link>
      </div>

      <div class="button-group">
        <el-button class="submit-button" @click="onLogin">
          登录
        </el-button>
      </div>

      <div class="form-footer">
        <span class="footer-text">还没有账户？</span>
        <el-link class="register-link" @click="toRegister">
          立即注册
        </el-link>
      </div>
    </el-form>

    <div class="features-section">
      <div class="feature-item">
        <i class="fas fa-certificate"></i> <!-- 更换为正版保证图标 -->
        <span>正版保证</span>
      </div>
      <div class="feature-item">
        <i class="fas fa-shipping-fast"></i> <!-- 保留快速配送图标 -->
        <span>快速送达</span>
      </div>
      <div class="feature-item">
        <i class="fas fa-book-reader"></i> <!-- 更换为阅读指导图标 -->
        <span>阅读指导</span>
      </div>
    </div>
  </div>
</template>

<script>
import ValidCode from "../components/Validate";
import request from "@/utils/request";
import logger from '@/utils/logger'
import { setRoutes } from "@/router";
import { User, Lock, Key as KeyIcon } from '@element-plus/icons-vue';

export default {
  name: 'Login',
  components: {
    ValidCode,
    User,
    Lock,
    KeyIcon
  },
  data() {
    return {
      validCode: '', //通过valicode获取的验证码
      loginForm: {
        username: '',
        password: '',
        validCode: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ]
      },
      rememberMe: false
    };
  },
  mounted() {
    this.loadRememberedCredentials();
  },
  methods: {
    goToHome() {
      this.$router.push('/');
    },
    toRegister() {
      this.$router.push("/register");
    },
    createValidCode(data) {
      this.validCode = data
    },
    loadRememberedCredentials() {
      const remember = localStorage.getItem('rememberMe') === 'true';
      if (remember) {
        this.rememberMe = true;
        this.loginForm.username = localStorage.getItem('username') || '';
        this.loginForm.password = localStorage.getItem('password') || '';
      }
    },
    onLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          // 验证码比较时转换为小写，实现不区分大小写
          if (this.loginForm.validCode.toLowerCase() !== this.validCode.toLowerCase()) {
            this.$message.error("验证码错误");
            return;
          }
          request.post("/user/login", this.loginForm)
            .then(res => {
              if (res.code === "0") {
                this.$message({
                  message: "登录成功",
                  type: "success",
                  duration: 1000,
                  showClose: true
                })
                if (res.data.token) {
                  localStorage.setItem("token", res.data.token);
                }
                if (res.data) {
                  if (res.data.role === 'USER') {
                    localStorage.setItem("frontUser", JSON.stringify(res.data));
                  } else {
                    localStorage.setItem("backUser", JSON.stringify(res.data));
                  }
                }

                // 根据用户角色决定跳转路径
                if (res.data.role !== 'USER') {
                  if (res.data.menuList) {
                    localStorage.setItem("userMenuList", JSON.stringify(res.data.menuList));
                  }
                  setRoutes();
                  this.$router.push('/showView');
                } else {
                  this.$router.push('/');
                }

                if (this.rememberMe) {
                  localStorage.setItem('rememberMe', 'true');
                  localStorage.setItem('username', this.loginForm.username);
                  localStorage.setItem('password', this.loginForm.password);
                } else {
                  localStorage.removeItem('rememberMe');
                  localStorage.removeItem('username');
                  localStorage.removeItem('password');
                }
              } else {
                this.$message.error(res.msg);
              }
            })
            .catch(error => {
              logger.error("登录失败:", error);
            });
        } else {
          return false;
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 32px 24px;
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 中国风背景装饰 */
.login-container::before {
  display: none;
}

.login-container::after {
  content: '';
  position: absolute;
  bottom: -80px;
  right: -30px;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(52, 152, 219, 0.03) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: floatCloud 25s ease-in-out infinite reverse;
}

@keyframes floatCloud {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

/* 中国风装饰元素 */
.chinese-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

/* 云朵装饰 */
.cloud {
  position: absolute;
  width: 120px;
  height: 40px;
  background: radial-gradient(circle at 30% 50%, rgba(52, 152, 219, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(8px);
  animation: driftCloud 25s ease-in-out infinite;
}

.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: radial-gradient(circle, rgba(52, 152, 219, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(6px);
}

.cloud::before {
  width: 80px;
  height: 32px;
  top: -10px;
  left: 15px;
}

.cloud::after {
  width: 90px;
  height: 36px;
  top: -5px;
  right: 15px;
}

.cloud-1 {
  top: 10%;
  left: 5%;
  animation-delay: 0s;
}

.cloud-2 {
  top: 60%;
  right: 8%;
  animation-delay: -8s;
  width: 100px;
  height: 35px;
}

.cloud-3 {
  bottom: 15%;
  left: 10%;
  animation-delay: -15s;
  width: 90px;
  height: 30px;
}

@keyframes driftCloud {
  0%, 100% { 
    transform: translate(0, 0);
    opacity: 0.3;
  }
  50% { 
    transform: translate(30px, -15px);
    opacity: 0.6;
  }
}

/* 山水图案装饰 */
.mountain-pattern {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 180px;
  background: linear-gradient(to top, rgba(52, 152, 219, 0.03) 0%, transparent 100%);
  opacity: 0.5;
}

.mountain-pattern::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100px;
  background: 
    radial-gradient(ellipse at 20% 100%, rgba(52, 152, 219, 0.04) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 100%, rgba(52, 152, 219, 0.08) 0%, transparent 45%),
    radial-gradient(ellipse at 80% 100%, rgba(52, 152, 219, 0.04) 0%, transparent 38%);
  filter: blur(2px);
}

.mountain-pattern::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: 
    radial-gradient(ellipse at 30% 100%, rgba(52, 152, 219, 0.04) 0%, transparent 35%),
    radial-gradient(ellipse at 70% 100%, rgba(52, 152, 219, 0.04) 0%, transparent 40%);
  filter: blur(3px);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
  z-index: 10;
}

.logo-section {
  position: relative;
}

/* 中国风Logo图标 - 青花瓷样式 */
.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: 50%;
  margin-bottom: 16px;
  box-shadow: 
    0 8px 32px rgba(52, 152, 219, 0.2),
    inset 0 2px 8px rgba(255, 255, 255, 0.2);
  animation: float 4s ease-in-out infinite;
  position: relative;
  z-index: 2;
  border: 3px solid rgba(255, 255, 255, 0.8);
}

/* 青花瓷纹理 */
.logo-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background-image: 
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.3) 0%, transparent 40%),
    radial-gradient(circle at 70% 70%, rgba(255, 255, 255, 0.2) 0%, transparent 40%);
  pointer-events: none;
}

/* Logo外围装饰圆环 */
.logo-icon::after {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  border: 2px dashed rgba(52, 152, 219, 0.2);
  animation: rotate 30s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logo-icon i {
  color: white;
  font-size: 32px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  position: relative;
  z-index: 1;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) scale(1); }
  50% { transform: translateY(-10px) scale(1.05); }
}

/* 中国风标题 */
.login-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--ink-darkest);
  margin: 0;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 4px;
  position: relative;
  display: inline-block;
}

/* 标题下划线装饰 */
.login-title::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 2px;
  transition: width 0.4s ease;
}

.login-title:hover {
  color: var(--primary-color);
  transform: translateY(-2px);
  text-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);
}

.login-title:hover::after {
  width: 100%;
}

.login-form {
  margin-top: 0;
  position: relative;
  z-index: 1;
}

/* 中国风输入框样式 */
:deep(.el-input__inner) {
  height: 46px;
  line-height: 46px;
  border-radius: 12px;
  border: 1.5px solid var(--border-color-light);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding-left: 42px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  font-size: 15px;
  color: var(--ink-darkest);
  box-shadow: 0 1px 3px rgba(52, 152, 219, 0.06);
}

:deep(.el-input__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 
    0 0 0 3px rgba(52, 152, 219, 0.1),
    0 2px 8px rgba(52, 152, 219, 0.12);
  background: white;
}

:deep(.el-input__inner:hover:not(:focus)) {
  border-color: var(--primary-light);
  box-shadow: 0 1px 4px rgba(52, 152, 219, 0.08);
}

:deep(.el-input__prefix) {
  left: 14px;
  top: 0;
  height: 100%;
  display: flex;
  align-items: center;
}

:deep(.el-input__prefix i) {
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  color: var(--primary-color);
  transition: all 0.3s ease;
}

:deep(.el-input:focus-within .el-input__prefix i) {
  color: var(--primary-color);
  transform: scale(1.1);
}

.validate-container {
  display: flex;
  gap: 12px;
}

/* 中国风验证码样式 */
.validate-code {
  flex-shrink: 0;
  width: 120px;
  height: 46px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid var(--border-color-light);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  position: relative;
}

.validate-code::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.05) 0%, rgba(141, 160, 217, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.validate-code:hover {
  border-color: var(--primary-color);
  box-shadow: 
    0 0 0 3px rgba(52, 152, 219, 0.1),
    0 4px 12px rgba(52, 152, 219, 0.15);
  transform: translateY(-2px);
}

.validate-code:hover::before {
  opacity: 1;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 28px 0 36px;
  padding: 0 4px;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
}

/* 中国风复选框 */
:deep(.remember-checkbox .el-checkbox__label) {
  color: var(--ink-dark);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

:deep(.remember-checkbox .el-checkbox__input .el-checkbox__inner) {
  border-radius: 4px;
  border: 2px solid var(--border-color-dark);
  transition: all 0.3s ease;
}

:deep(.remember-checkbox .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

:deep(.remember-checkbox .el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--primary-color);
}

/* 忘记密码链接 - 中国风 */
.forget-link {
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
  color: var(--primary-color);
  letter-spacing: 0.5px;
}

.forget-link:hover {
  transform: translateX(-2px);
  color: var(--primary-light);
  text-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}

.forget-link i {
  margin-right: 4px;
  font-size: 13px;
  transition: transform 0.3s ease;
}

.forget-link:hover i {
  transform: rotate(-15deg);
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 36px;
}

/* 中国风提交按钮 - 增强文字可读性 */
.submit-button {
  width: 100%;
  height: 48px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 3px;
  border-radius: 12px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: linear-gradient(135deg, #3498DB 0%, #2C3E50 100%) !important;
  border: none !important;
  color: white !important;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  box-shadow: 0 4px 16px rgba(52, 152, 219, 0.3);
  position: relative;
  overflow: hidden;
}

/* 按钮水墨效果 */
.submit-button::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, transparent 50%);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.submit-button:hover::before {
  transform: translateX(100%);
}

.submit-button:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 8px 24px rgba(52, 152, 219, 0.4),
    0 0 0 4px rgba(52, 152, 219, 0.15) !important;
  background: linear-gradient(135deg, #5DADE2 0%, #3498DB 100%) !important;
  border-color: transparent !important;
  color: white !important;
}

.submit-button:active {
  transform: translateY(-1px);
  background: linear-gradient(135deg, #2C3E50 0%, #1E3460 100%) !important;
  color: white !important;
  border-color: transparent !important;
}

/* 底部注册链接 */
.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 28px;
  font-size: 15px;
}

.footer-text {
  color: var(--ink-medium);
  font-weight: 400;
  letter-spacing: 0.5px;
}

.form-footer .register-link {
  color: var(--primary-color);
  font-weight: 600;
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

.form-footer .register-link:hover {
  color: var(--primary-light);
  text-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}


/* 中国风功能特色区 */
.features-section {
  display: flex;
  justify-content: space-around;
  margin-top: 44px;
  padding-top: 36px;
  border-top: 2px solid var(--border-color-lighter);
  position: relative;
}

/* 顶部装饰线条 */
.features-section::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 4px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--ink-dark);
  font-size: 13px;
  font-weight: 500;
  text-align: center;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

/* 功能项悬停效果 */
.feature-item::before {
  content: '';
  position: absolute;
  inset: -12px;
  border-radius: 12px;
  background: radial-gradient(circle, rgba(52, 152, 219, 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-item:hover::before {
  opacity: 1;
}

.feature-item:hover {
  transform: translateY(-4px);
  color: var(--primary-color);
}

.feature-item i {
  font-size: 26px;
  color: var(--primary-color);
  margin-bottom: 4px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
  /* 图标光晕效果 */
  filter: drop-shadow(0 2px 8px rgba(52, 152, 219, 0.2));
}

.feature-item:hover i {
  transform: scale(1.15) rotateY(15deg);
  filter: drop-shadow(0 4px 12px rgba(52, 152, 219, 0.35));
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .login-container::before,
  .login-container::after {
    display: none;
  }

  .chinese-decoration {
    opacity: 0.5;
  }

  .cloud {
    width: 100px;
    height: 35px;
  }

  .mountain-pattern {
    height: 120px;
  }
  
  .login-header {
    margin-bottom: 24px;
  }
  
  .logo-icon {
    width: 60px;
    height: 60px;
  }
  
  .logo-icon i {
    font-size: 28px;
  }
  
  .login-title {
    font-size: 28px;
    letter-spacing: 3px;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 24px 16px;
    max-width: 420px;
  }

  .chinese-decoration {
    opacity: 0.3;
  }

  .cloud {
    display: none;
  }

  .mountain-pattern {
    height: 80px;
    opacity: 0.4;
  }
  
  .login-header {
    margin-bottom: 20px;
  }
  
  .logo-icon {
    width: 54px;
    height: 54px;
    border-width: 2px;
  }
  
  .logo-icon::after {
    inset: -6px;
  }
  
  .logo-icon i {
    font-size: 26px;
  }
  
  .login-title {
    font-size: 26px;
    letter-spacing: 2px;
  }
  
  :deep(.el-input__inner) {
    height: 44px;
    line-height: 44px;
  }
  
  .validate-container {
    gap: 8px;
  }
  
  .validate-code {
    width: 110px;
    height: 44px;
  }
  
  .submit-button {
    height: 46px;
    font-size: 16px;
    letter-spacing: 2px;
  }
  
  .features-section {
    margin-top: 32px;
    padding-top: 24px;
  }
  
  .feature-item {
    font-size: 12px;
  }
  
  .feature-item i {
    font-size: 22px;
  }
}

/* 水墨渲染进入动画 */
.login-container {
  animation: fadeInInk 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeInInk {
  from {
    opacity: 0;
    transform: translateY(40px);
    filter: blur(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

/* 表单项渐入动画 */
:deep(.el-form-item) {
  animation: slideInLeft 0.6s ease-out backwards;
}

:deep(.el-form-item:nth-child(1)) {
  animation-delay: 0.1s;
}

:deep(.el-form-item:nth-child(2)) {
  animation-delay: 0.2s;
}

:deep(.el-form-item:nth-child(3)) {
  animation-delay: 0.3s;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 按钮加载状态 */
.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
  background: linear-gradient(135deg, #D4D4D4 0%, #A8A8A8 100%) !important;
  color: #666666 !important;
  text-shadow: none !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  border-color: transparent !important;
}

/* 焦点管理 - 中国风 */
.submit-button:focus {
  outline: none !important;
  box-shadow: 0 0 0 4px rgba(52, 152, 219, 0.2) !important;
}

/* Element UI 全局覆盖 */
:deep(.el-link) {
  transition: all 0.3s ease;
}

:deep(.el-checkbox__inner) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 功能区动画 */
.features-section {
  animation: fadeInUp 0.8s ease-out 0.4s backwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>