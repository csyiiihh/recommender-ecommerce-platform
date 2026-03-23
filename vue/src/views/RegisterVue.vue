<template>
  <div class="register-container">
    <!-- 中国风装饰元素 - 调整为与图书文化相关的元素 -->
    <div class="chinese-decoration">
      <!-- 已隐藏装饰元素 -->
      <div class="ink-wash-effect"></div>
    </div>

    <div class="register-header">
      <div class="logo-section">
        <div class="logo-icon">
          <i class="fas fa-book-open"></i> <!-- 更换为书籍图标 -->
        </div>
        <h2 class="register-title">加入阅读社区</h2> <!-- 修改标题为阅读相关 -->
      </div>
    </div>

    <el-form ref="registerForm" :model="registerForm" :rules="rules" class="register-form">
      <div class="form-content">
        <div class="form-row">
          <el-form-item prop="username">
            <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
            </el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item prop="password">
            <el-input
                type="password"
                v-model="registerForm.password"
                placeholder="请设置密码"
                show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item prop="name">
            <el-input
                v-model="registerForm.name"
                placeholder="请输入真实姓名"
            >
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item prop="email">
            <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱地址"
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item prop="code">
            <div class="validate-container">
              <el-input
                  v-model="registerForm.code"
                  placeholder="请输入验证码"
              >
                <template #prefix>
                  <el-icon><ChatLineRound /></el-icon>
                </template>
              </el-input>
              <el-button
                  :disabled="disabled"
                  class="validate-btn"
                  :type="disabled ? 'info' : 'primary'"
                  @click="sendVerificationCode">
                <span v-if="!disabled">发送验证码</span>
                <span v-else>{{ buttonContent }}</span>
              </el-button>
            </div>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item prop="role">
            <el-select
                v-model="registerForm.role"
                placeholder="请选择用户角色"
                class="role-select">
              <el-option value="USER">
                <div class="role-option">
                  <i class="fas fa-user-circle role-icon"></i>
                  <span>阅读爱好者</span> <!-- 修改为阅读相关 -->
                </div>
              </el-option>
              <el-option value="MERCHANT">
                <div class="role-option">
                  <i class="fas fa-store role-icon"></i>
                  <span>书商伙伴</span> <!-- 修改为图书相关 -->
                </div>
              </el-option>
              <el-option value="ADMIN">
                <div class="role-option">
                  <i class="fas fa-user-shield role-icon"></i>
                  <span>平台管理员</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </div>

        <div class="button-group">
          <el-button class="submit-button" @click="onRegister">
            开启阅读之旅
          </el-button> <!-- 修改按钮文字 -->
        </div>

        <div class="form-footer">
          <el-link class="back-link" @click="$router.push('/login')">
            <i class="fas fa-arrow-left"></i> 返回登录
          </el-link>
        </div>
      </div>
    </el-form>

    <div class="register-footer">
      <p class="terms-text">
        注册即表示您同意我们的
        <a href="#" class="terms-link">用户协议</a>
        和
        <a href="#" class="terms-link">隐私政策</a>
      </p>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";
import logger from '@/utils/logger'
import { User, Lock, UserFilled, Message, ChatLineRound } from '@element-plus/icons-vue'

export default {
    name: 'Register',
    data() {

        return {
            countdown: 0,
            disabled: false,
            timer: null,
            emailCode: '',
            buttonContent: '发送验证码',
            registerForm: {
                username: '',
                password: '',
                name: '',
                email: '',
                code: '',
                role: 'USER',
                status: 1
            },
            rules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' },
                    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
                ],
                name: [
                    { required: true, message: '请输入真实姓名', trigger: 'blur' }
                ],
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' }
                ],
                role: [
                    { required: true, message: '请选择用户角色', trigger: 'change' }
                ]
            }
        };
    },
    methods: {
        goToHome() {
            this.$router.push('/');
        },
        sendVerificationCode() {
            if (this.disabled) return;
            
            if (!this.registerForm.email) {
                this.$message.error('请先输入邮箱地址');
                return;
            }

            request.get(`/email/sendEmail/${this.registerForm.email}`).then(res => {
                if (res.code === '0') {
                    this.$message({
                        type: 'success',
                        message: "验证码已发送到您的邮箱,请查收"
                    });
                    logger.debug(res.data);
                    this.emailCode = res.data;
                    this.startCountdown();
                } else {
                    this.$message({
                        type: 'error',
                        message: res.msg
                    });
                }
            });
        },
        
        startCountdown() {
            this.countdown = 60;
            this.disabled = true;
            this.buttonContent = `${this.countdown}秒后重试`;

            this.timer = setInterval(() => {
                if (this.countdown > 0) {
                    this.countdown--;
                    this.buttonContent = `${this.countdown}秒后重试`;
                } else {
                    clearInterval(this.timer);
                    this.disabled = false;
                    this.buttonContent = '发送验证码';
                }
            }, 1000);
        },

        onRegister() {
            this.$refs.registerForm.validate((valid) => {
                if (valid) {
                    // 将验证码转换为字符串后再比较
                    if (String(this.registerForm.code) !== String(this.emailCode)) {
                        logger.debug('Input code:', this.registerForm.code, 'Expected code:', this.emailCode);
                        this.$message({
                            type: 'error',
                            message: '验证码不正确'
                        });
                        return;
                    }


                    request.post("/user/add", this.registerForm).then(res => {
                        if (res.code === '0') {
                            this.$message({
                                type: 'success',
                                message: "注册成功，请登录"
                            });
                            this.$router.push('/login');
                        } else {
                            this.$message({
                                type: 'error',
                                message: res.msg
                            });
                        }
                    });
                }
            });
        }
    },
    beforeUnmount() {
        clearInterval(this.timer);
    }
};
</script>

<style scoped>
.register-container {
    width: 100%;
    max-width: 480px;
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

/* 中国风背景装饰 - 山水意境 */
.register-container::before {
    display: none;
}

.register-container::after {
    content: '';
    position: absolute;
    bottom: -100px;
    left: -40px;
    width: 320px;
    height: 320px;
    background: radial-gradient(circle, rgba(58, 91, 160, 0.03) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
    animation: floatSlow 28s ease-in-out infinite reverse;
}

@keyframes floatSlow {
    0%, 100% { transform: translate(0, 0) scale(1); }
    50% { transform: translate(-15px, 15px) scale(1.05); }
}

/* 中国风装饰元素 */
.chinese-decoration {
    position: absolute;
    inset: 0;
    pointer-events: none;
    overflow: hidden;
    z-index: 0;
}

/* 图书相关装饰元素样式示例 - 已隐藏 */
.book-stack-1, .book-stack-2 {
  display: none;
}

.quill-pen-1 {
  display: none;
}

.scroll-pattern {
  display: none;
}

/* 竹叶装饰 */
.bamboo-leaf {
    position: absolute;
    width: 60px;
    height: 8px;
    background: linear-gradient(90deg, transparent 0%, rgba(58, 91, 160, 0.08) 50%, transparent 100%);
    border-radius: 50%;
    transform-origin: left center;
    animation: sway 8s ease-in-out infinite;
}

.bamboo-leaf::before {
    content: '';
    position: absolute;
    width: 30px;
    height: 6px;
    background: linear-gradient(90deg, rgba(58, 91, 160, 0.06) 0%, transparent 100%);
    border-radius: 50%;
    top: -8px;
    left: 20px;
    transform: rotate(-30deg);
}

.bamboo-leaf::after {
    content: '';
    position: absolute;
    width: 30px;
    height: 6px;
    background: linear-gradient(90deg, rgba(58, 91, 160, 0.06) 0%, transparent 100%);
    border-radius: 50%;
    top: 8px;
    left: 20px;
    transform: rotate(30deg);
}

.bamboo-leaf-1 {
    top: 8%;
    right: 5%;
    transform: rotate(-25deg);
    animation-delay: 0s;
}

.bamboo-leaf-2 {
    top: 45%;
    left: 3%;
    transform: rotate(45deg);
    animation-delay: -3s;
    width: 50px;
}

.bamboo-leaf-3 {
    bottom: 20%;
    right: 8%;
    transform: rotate(-60deg);
    animation-delay: -6s;
    width: 55px;
}

@keyframes sway {
    0%, 100% { 
        transform: rotate(0deg);
        opacity: 0.4;
    }
    50% { 
        transform: rotate(8deg);
        opacity: 0.7;
    }
}

/* 水墨渲染效果 */
.ink-wash-effect {
    position: absolute;
    top: 15%;
    left: -10%;
    width: 200px;
    height: 200px;
    background: radial-gradient(circle, rgba(58, 91, 160, 0.04) 0%, transparent 60%);
    border-radius: 50%;
    filter: blur(30px);
    animation: inkFlow 20s ease-in-out infinite;
}

@keyframes inkFlow {
    0%, 100% { 
        transform: translate(0, 0) scale(1);
        opacity: 0.3;
    }
    50% { 
        transform: translate(40px, 30px) scale(1.2);
        opacity: 0.5;
    }
}

/* 青花瓷纹理装饰 */
.porcelain-pattern {
    position: absolute;
    bottom: -5%;
    right: -8%;
    width: 250px;
    height: 250px;
    background: 
        radial-gradient(circle at center, transparent 40%, rgba(58, 91, 160, 0.03) 41%, rgba(58, 91, 160, 0.03) 43%, transparent 44%),
        radial-gradient(circle at center, transparent 55%, rgba(58, 91, 160, 0.02) 56%, rgba(58, 91, 160, 0.02) 58%, transparent 59%),
        radial-gradient(circle at center, transparent 70%, rgba(58, 91, 160, 0.015) 71%, rgba(58, 91, 160, 0.015) 72%, transparent 73%);
    border-radius: 50%;
    opacity: 0.6;
    animation: rotate 40s linear infinite;
}

.porcelain-pattern::before {
    content: '';
    position: absolute;
    inset: 20%;
    background: radial-gradient(circle, rgba(58, 91, 160, 0.02) 0%, transparent 70%);
    border-radius: 50%;
}

@keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.register-header {
    text-align: center;
    margin-bottom: 28px;
    position: relative;
    z-index: 10;
}

.logo-section {
    position: relative;
}

/* 中国风Logo - 种子/竹子主题 */
.logo-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 68px;
    height: 68px;
    background: var(--gradient-primary);
    border-radius: 50%;
    margin-bottom: 16px;
    box-shadow: 
        0 8px 32px rgba(58, 91, 160, 0.25),
        inset 0 2px 8px rgba(255, 255, 255, 0.2);
    animation: floatRotate 5s ease-in-out infinite;
    position: relative;
    z-index: 2;
    border: 3px solid rgba(255, 255, 255, 0.8);
}

/* 竹子/植物纹理 */
.logo-icon::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background-image: 
        radial-gradient(circle at 25% 25%, rgba(255, 255, 255, 0.3) 0%, transparent 35%),
        radial-gradient(circle at 75% 75%, rgba(255, 255, 255, 0.2) 0%, transparent 35%);
    pointer-events: none;
}

/* Logo外围装饰圆环 */
.logo-icon::after {
    content: '';
    position: absolute;
    inset: -8px;
    border-radius: 50%;
    border: 2px dashed rgba(58, 91, 160, 0.25);
    animation: rotateReverse 35s linear infinite;
}

@keyframes rotateReverse {
    from { transform: rotate(360deg); }
    to { transform: rotate(0deg); }
}

@keyframes floatRotate {
    0%, 100% { 
        transform: translateY(0px) rotate(0deg); 
    }
    50% { 
        transform: translateY(-12px) rotate(5deg); 
    }
}

.logo-icon i {
    color: white;
    font-size: 32px;
    filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.25));
    position: relative;
    z-index: 1;
}

/* 中国风标题 */
.register-title {
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

/* 标题水墨笔触效果 */
.register-title::after {
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

.register-title:hover {
    color: var(--primary-color);
    transform: translateY(-2px);
    text-shadow: 0 4px 12px rgba(58, 91, 160, 0.25);
}

.register-title:hover::after {
    width: 100%;
}

.register-form {
    margin-top: 0;
    position: relative;
    z-index: 1;
}

.form-content {
    width: 100%;
    margin: 0 auto;
}

.form-row {
    margin-bottom: 20px;
    animation: slideInRight 0.6s ease-out backwards;
}

.form-row:nth-child(1) { animation-delay: 0.1s; }
.form-row:nth-child(2) { animation-delay: 0.15s; }
.form-row:nth-child(3) { animation-delay: 0.2s; }
.form-row:nth-child(4) { animation-delay: 0.25s; }
.form-row:nth-child(5) { animation-delay: 0.3s; }
.form-row:nth-child(6) { animation-delay: 0.35s; }

@keyframes slideInRight {
    from {
        opacity: 0;
        transform: translateX(30px);
    }
    to {
        opacity: 1;
        transform: translateX(0);
    }
}

/* 中国风输入框 - 增强版 */
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
    position: relative;
    box-shadow: 0 1px 3px rgba(58, 91, 160, 0.06);
}

:deep(.el-input__inner::placeholder) {
    color: var(--ink-light);
    transition: all 0.3s ease;
}

:deep(.el-input__inner:focus) {
    border-color: var(--primary-color);
    box-shadow: 
        0 0 0 3px rgba(58, 91, 160, 0.1),
        0 2px 8px rgba(58, 91, 160, 0.12);
    background: white;
    border-width: 1.5px;
}

:deep(.el-input__inner:focus::placeholder) {
    opacity: 0.5;
    transform: translateX(4px);
}

:deep(.el-input__inner:hover:not(:focus)) {
    border-color: var(--primary-light);
    box-shadow: 0 1px 4px rgba(58, 91, 160, 0.08);
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

/* 中国风下拉选择器 */
:deep(.el-select .el-input__inner) {
    height: 46px;
    line-height: 46px;
    border-radius: 12px;
    border: 2px solid var(--border-color-light);
    padding-left: 16px;
    padding-right: 35px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
}

:deep(.el-select .el-input__inner:focus) {
    border-color: var(--primary-color);
    box-shadow: 
        0 0 0 3px rgba(58, 91, 160, 0.12),
        0 4px 12px rgba(58, 91, 160, 0.18);
    background: white;
    transform: translateY(-1px);
}

:deep(.el-select .el-input__inner:hover) {
    border-color: var(--primary-light);
}

:deep(.el-select .el-icon-arrow-up:before) {
    color: var(--primary-color);
}

.validate-container {
    display: flex;
    gap: 12px;
}

/* 中国风验证码按钮 - 增强版 */
.validate-btn {
    flex-shrink: 0;
    width: 130px;
    height: 48px;
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 1px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    background: var(--gradient-primary) !important;
    border: none !important;
    color: white !important;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    box-shadow: 
        0 4px 12px rgba(58, 91, 160, 0.4),
        0 2px 4px rgba(0, 0, 0, 0.1);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

/* 金色光泽效果 */
.validate-btn::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.25) 0%, transparent 50%);
    transform: translateX(-100%);
    transition: transform 0.6s ease;
}

.validate-btn::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s ease;
}

.validate-btn:hover::before {
    transform: translateX(100%);
}

.validate-btn:hover::after {
    opacity: 1;
}

.validate-btn:hover:not(:disabled) {
    transform: translateY(-3px) scale(1.02);
    box-shadow: 
        0 8px 24px rgba(58, 91, 160, 0.55),
        0 0 0 4px rgba(58, 91, 160, 0.2),
        0 4px 8px rgba(0, 0, 0, 0.15) !important;
    background: linear-gradient(135deg, #5B7CBD 0%, #3A5BA0 100%) !important;
    border-color: transparent !important;
    color: white !important;
}

.validate-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none !important;
    background: linear-gradient(135deg, #D4D4D4 0%, #A8A8A8 100%) !important;
    color: #666666 !important;
    text-shadow: none !important;
    border-color: transparent !important;
}

.role-select {
    width: 100%;
}

/* Element UI 组件样式覆盖 - 验证码输入框保持统一样式 */
.validate-container :deep(.el-input__inner) {
    height: 46px;
    line-height: 46px;
    border-radius: 12px;
    border: 2px solid var(--border-color-light);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    padding-left: 42px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
}

.validate-container :deep(.el-input__inner:focus) {
    border-color: var(--primary-color);
    box-shadow: 
        0 0 0 3px rgba(58, 91, 160, 0.12),
        0 4px 12px rgba(58, 91, 160, 0.18);
    background: white;
    transform: translateY(-1px);
}

/* 中国风角色选项 */
.role-option {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  transition: all 0.3s ease;
}

.role-icon {
  width: 20px;
  text-align: center;
  font-size: 18px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.role-option .fas.fa-user-circle {
  color: var(--primary-color);
}

.role-option .fas.fa-store {
  color: var(--gold-color);
}

.role-option .fas.fa-user-shield {
  color: var(--accent-color);
}

.role-option span {
  flex: 1;
  font-weight: 500;
  color: var(--ink-darkest);
  letter-spacing: 0.5px;
}

:deep(.el-select-dropdown__item:hover .role-option .role-icon) {
  transform: scale(1.15) rotateY(10deg);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.2));
}

:deep(.el-select-dropdown__item.selected .role-option .role-icon) {
  animation: iconBounce 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-select-dropdown__item:hover) {
    background: rgba(58, 91, 160, 0.08);
    color: var(--primary-color);
    transform: translateX(4px);
}

:deep(.el-select-dropdown__item.selected) {
    background: var(--gradient-primary);
    color: white;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(58, 91, 160, 0.25);
}

@keyframes iconBounce {
  0% { transform: scale(1); }
  30% { transform: scale(1.25) rotateZ(10deg); }
  60% { transform: scale(0.95) rotateZ(-5deg); }
  100% { transform: scale(1) rotateZ(0deg); }
}

.validate-container :deep(.el-input__prefix) {
    left: 14px;
    top: 0;
    height: 100%;
    display: flex;
    align-items: center;
}

.validate-container :deep(.el-input__prefix i) {
    font-size: 18px;
    line-height: 1;
    display: flex;
    align-items: center;
    color: var(--primary-color);
}

:deep(.el-form-item) {
    margin-bottom: 20px;
}

.button-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 36px;
    animation: fadeInUp 0.6s ease-out 0.4s backwards;
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

/* 中国风提交按钮 - 增强版 */
.submit-button {
    width: 100%;
    height: 52px;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 4px;
    border-radius: 12px;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    background: linear-gradient(135deg, #C23A3B 0%, #A52E2F 100%) !important;
    border: none !important;
    color: white !important;
    text-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
    box-shadow: 
        0 6px 20px rgba(194, 58, 59, 0.35),
        0 2px 4px rgba(0, 0, 0, 0.1);
    position: relative;
    overflow: hidden;
}

/* 按钮光泽效果 */
.submit-button::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.25) 0%, transparent 50%);
    transform: translateX(-100%);
    transition: transform 0.6s ease;
}

.submit-button::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s ease;
}

.submit-button:hover::before {
    transform: translateX(100%);
}

.submit-button:hover::after {
    opacity: 1;
}

.submit-button:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: 
        0 12px 32px rgba(58, 91, 160, 0.5),
        0 0 0 4px rgba(58, 91, 160, 0.2),
        0 4px 8px rgba(0, 0, 0, 0.15) !important;
    background: linear-gradient(135deg, #5B7CBD 0%, #3A5BA0 100%) !important;
    border-color: transparent !important;
    color: white !important;
}

.submit-button:active {
    transform: translateY(-2px) scale(0.98);
    background: linear-gradient(135deg, #2E4880 0%, #1E3460 100%) !important;
    color: white !important;
    border-color: transparent !important;
    box-shadow: 
        0 6px 16px rgba(58, 91, 160, 0.4),
        0 0 0 2px rgba(58, 91, 160, 0.25) !important;
}

/* 返回登录链接 */
.form-footer {
    display: flex;
    justify-content: center;
    margin-top: 28px;
    font-size: 15px;
    animation: fadeIn 0.6s ease-out 0.5s backwards;
}

@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

.form-footer .el-link {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    letter-spacing: 1px;
    transition: all 0.3s ease;
}

:deep(.form-footer .el-link.el-link--warning) {
    color: var(--primary-color);
}

:deep(.form-footer .el-link.el-link--warning:hover) {
    color: var(--primary-light);
    transform: translateX(-4px);
    text-shadow: 0 2px 8px rgba(58, 91, 160, 0.25);
}

:deep(.form-footer .el-link i) {
    transition: transform 0.3s ease;
}

:deep(.form-footer .el-link:hover i) {
    transform: translateX(-3px);
}

/* 中国风注册页脚 */
.register-footer {
    margin-top: 40px;
    text-align: center;
    padding-top: 28px;
    border-top: 2px solid var(--border-color-lighter);
    position: relative;
    animation: fadeIn 0.6s ease-out 0.6s backwards;
}

/* 顶部装饰 */
.register-footer::before {
    content: '';
    position: absolute;
    top: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 100px;
    height: 4px;
    background: var(--gradient-accent);
    border-radius: 2px;
}

.terms-text {
    font-size: 14px;
    color: var(--ink-medium);
    margin: 0;
    line-height: 1.6;
    letter-spacing: 0.5px;
}

/* 协议链接 */
.terms-link {
    color: var(--primary-color);
    text-decoration: none;
    font-weight: 600;
    transition: all 0.3s ease;
    position: relative;
}

.terms-link::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 2px;
    background: var(--primary-color);
    transition: width 0.3s ease;
}

.terms-link:hover {
    color: var(--primary-light);
    text-shadow: 0 2px 6px rgba(58, 91, 160, 0.2);
}

.terms-link:hover::after {
    width: 100%;
}

/* 响应式调整 */
@media (max-width: 1024px) {
    .register-container::before,
    .register-container::after {
        display: none;
    }

    .chinese-decoration {
        opacity: 0.5;
    }

    .bamboo-leaf {
        width: 50px;
    }

    .porcelain-pattern {
        width: 200px;
        height: 200px;
    }

    .ink-wash-effect {
        width: 150px;
        height: 150px;
    }
    
    .register-header {
        margin-bottom: 20px;
    }
    
    .logo-icon {
        width: 60px;
        height: 60px;
    }
    
    .logo-icon i {
        font-size: 28px;
    }
    
    .register-title {
        font-size: 28px;
        letter-spacing: 3px;
    }
    
    .form-row {
        margin-bottom: 18px;
    }
}

@media (max-width: 480px) {
    .register-container {
        padding: 24px 16px;
        max-width: 520px;
    }

    .chinese-decoration {
        opacity: 0.25;
    }

    .bamboo-leaf {
        display: none;
    }

    .porcelain-pattern,
    .ink-wash-effect {
        opacity: 0.3;
    }
    
    .register-header {
        margin-bottom: 18px;
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
    
    .register-title {
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
    
    .validate-container :deep(.el-input__inner) {
        height: 44px;
        line-height: 44px;
    }
    
    .validate-btn {
        width: 114px;
        height: 44px;
        font-size: 13px;
        letter-spacing: 0.5px;
    }
    
    .submit-button {
        height: 48px;
        font-size: 17px;
        letter-spacing: 3px;
    }
    
    .form-row {
        margin-bottom: 16px;
    }
    
    .register-footer {
        margin-top: 28px;
        padding-top: 20px;
    }
    
    .register-footer::before {
        width: 80px;
    }
    
    .terms-text {
        font-size: 13px;
    }
}

/* 水墨渲染进入动画 */
.register-container {
    animation: fadeInSpread 0.9s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeInSpread {
    from {
        opacity: 0;
        transform: translateY(50px) scale(0.95);
        filter: blur(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
        filter: blur(0);
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
    box-shadow: 0 0 0 4px rgba(58, 91, 160, 0.2) !important;
}

.validate-btn:focus {
    outline: none !important;
    box-shadow: 0 0 0 4px rgba(58, 91, 160, 0.2) !important;
}

/* 确保按下状态保持蓝色 */
.validate-btn:active:not(:disabled) {
    transform: scale(0.95);
    background: linear-gradient(135deg, #2E4880 0%, #1E3460 100%) !important;
    color: white !important;
}

/* 中国风下拉框样式 */
:deep(.el-select-dropdown) {
    border: 2px solid var(--border-color-light);
    border-radius: 14px;
    box-shadow: 
        0 12px 40px rgba(0, 0, 0, 0.15),
        0 0 0 1px rgba(58, 91, 160, 0.05);
    backdrop-filter: blur(10px);
    background: rgba(255, 255, 255, 0.98);
}

:deep(.el-option) {
    padding: 14px 22px;
    font-size: 15px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    margin: 4px 8px;
    border-radius: 10px;
}

:deep(.el-option:hover) {
    background: rgba(58, 91, 160, 0.08);
    color: var(--primary-color);
    transform: translateX(4px);
}

:deep(.el-option.selected) {
    background: var(--gradient-primary);
    color: white;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(58, 91, 160, 0.25);
}

/* Element UI 全局覆盖 */
:deep(.el-link) {
    transition: all 0.3s ease;
}

:deep(.el-input__suffix) {
    right: 12px;
}

:deep(.el-input__suffix .el-icon) {
    color: var(--primary-color);
    transition: all 0.3s ease;
}

:deep(.el-input:hover .el-input__suffix .el-icon) {
    color: var(--primary-light);
}

/* 输入框占位符样式 */
:deep(.el-input__inner::placeholder) {
    color: var(--ink-light);
    letter-spacing: 0.5px;
}

/* 返回登录链接样式 */
.form-footer .back-link {
    color: var(--primary-color);
    font-weight: 500;
    transition: all 0.3s ease;
}

.form-footer .back-link:hover {
    color: var(--primary-light);
    text-shadow: 0 2px 8px rgba(58, 91, 160, 0.2);
}
</style>