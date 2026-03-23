<template>
    <div class="forget-container">
        <!-- 中国风装饰元素 -->
        <div class="chinese-decoration">
            <div class="cloud cloud-1"></div>
            <div class="cloud cloud-2"></div>
            <div class="ink-bamboo"></div>
        </div>

        <div class="forget-header">
            <div class="logo-icon">
                <i class="fas fa-key"></i>
            </div>
        <h2 class="forget-title">重设安全密钥</h2>
        </div>

        <el-form ref="resetForm" :model="resetForm" :rules="rules" class="forget-form">
            <div class="form-content">
                <el-form-item prop="email">
                    <el-input 
                        v-model="resetForm.email" 
                        placeholder="请输入邮箱"
                    >
                      <template #prefix>
                        <el-icon><Message /></el-icon>
                      </template>
                    </el-input>
                </el-form-item>

                <el-form-item prop="code">
                    <div class="validate-container">
                        <el-input 
                            v-model="resetForm.code" 
                            placeholder="请输入验证码"
                        >
                          <template #prefix>
                            <el-icon><ChatLineRound /></el-icon>
                          </template>
                        </el-input>
                        <el-button 
                            :disabled="disabled || sendingCode"
                            class="validate-btn"
                            :loading="sendingCode"
                            @click="sendVerificationCode">
                            <span v-if="!disabled && !sendingCode">发送验证码</span>
                            <span v-else-if="sendingCode">发送中...</span>
                            <span v-else>{{ buttonContent }}</span>
                        </el-button>
                    </div>
                </el-form-item>

                <el-form-item prop="newPassword">
                    <el-input 
                        type="password" 
                        v-model="resetForm.newPassword" 
                        placeholder="请设置新密码"
                    >
                      <template #prefix>
                        <el-icon><Lock /></el-icon>
                      </template>
                    </el-input>
                </el-form-item>

                <el-form-item>
                    <el-button 
                        class="submit-button"
                        :loading="loading"
                        :disabled="loading"
                        @click="onResetPassword">
                        <span v-if="!loading">重置密码</span>
                        <span v-else>重置中...</span>
                    </el-button>
                </el-form-item>

                <div class="form-footer">
                    <el-link class="back-link" @click="$router.push('/login')">
                        <i class="fas fa-arrow-left"></i> 返回登录
                    </el-link>
                </div>
            </div>
        </el-form>
    </div>
</template>

<script>
import request from "@/utils/request";
import logger from '@/utils/logger'
import { Message, ChatLineRound, Lock } from '@element-plus/icons-vue'

export default {
    name: 'ResetPassword',
    data() {
        return {
            countdown: 0,
            disabled: false,
            timer: null,
            emailCode: '',
            buttonContent: '发送验证码',
            loading: false, // 添加重置密码loading状态
            sendingCode: false, // 添加发送验证码loading状态
            resetForm: {
                email: '',
                code: '',
                newPassword: '',
            },
            rules: {
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' }
                ],
                newPassword: [
                    { required: true, message: '请输入新密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
                ]
            }
        };
    },
    methods: {
        sendVerificationCode() {
            if (this.disabled || this.sendingCode) return;

            if (!this.resetForm.email) {
                this.$message.error('请先输入邮箱地址');
                return;
            }

            // 邮箱格式验证
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(this.resetForm.email)) {
                this.$message.error('请输入正确的邮箱格式');
                return;
            }

            this.sendingCode = true;
            request.get(`/email/findByEmail/${this.resetForm.email}`)
            .then(res => {
                if (res.code == '0') {
                    this.$message({
                        type: 'success',
                        message: "验证码已发送到您的邮箱,请查收"
                    })
                    this.emailCode = res.data;
                } else {
                    this.$message({
                        type: 'error',
                        message: res.msg
                    });
                    this.sendingCode = false;
                    return;
                }
            }).catch(error => {
                this.$message.error(error.response?.data?.msg || '验证码发送失败，请稍后重试');
                this.sendingCode = false;
            });

            // 只有在成功发送后才开始倒计时
            if (this.sendingCode) {
                    this.disabled = true; // 禁用按钮
                this.buttonContent = `${this.countdown}秒后可重发`; // 更新按钮文本为倒计时

                this.timer = setInterval(() => {
                    if (this.countdown > 0) {
                        this.countdown--; // 每秒减少1秒
                        this.buttonContent = `${this.countdown}秒后可重发`; // 更新按钮文本
                    } else {
                        clearInterval(this.timer); // 清除定时器
                        this.countdown = 0; // 重置倒计时
                        this.disabled = false; // 启用按钮
                        this.buttonContent = '发送验证码'; // 重置按钮文本
                        this.sendingCode = false;
                    }
                }, 1000);
            }
    },
    onResetPassword() {
        this.$refs.resetForm.validate((valid) => {
            if (valid) {
                // 防止重复提交
                if (this.loading) return;
                
                if (String(this.resetForm.code) !== String(this.emailCode)) {
                    this.$message({
                        type: 'error',
                        message: '验证码不正确'
                    });
                    return;
                }
                
                this.loading = true;
                // 发送重置密码请求到后端
                request.get("/user/forget", {
                    params: {
                        email: this.resetForm.email,
                        newPassword: this.resetForm.newPassword,

                    },
                }).then(res => {
                    if (res.code == '0') {
                        this.$message({
                            type: 'success',
                            message: "密码重置成功"
                        });
                        this.$router.push("/login");
                    } else {
                        this.$message({
                            type: 'error',
                            message: res.msg
                        });
                    }
                }).catch(error => {
                    this.$message.error(error.response?.data?.msg || '重置密码失败，请稍后重试');
                }).finally(() => {
                    this.loading = false;
                });
            } else {
                logger.error('重置密码失败: 表单校验失败');
                return false;
            }
        });
    }
},
beforeUnmount() {
    clearInterval(this.timer);
},
  };
</script>

<style scoped>
.forget-container {
    width: 100%;
    max-width: 380px;
    padding: 50px 0 0 0;
    font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
    position: relative;
    overflow: hidden;
    min-height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    animation: fadeInInk 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 中国风装饰元素 */
.chinese-decoration {
    position: absolute;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    pointer-events: none;
    z-index: 0;
}

.cloud {
    position: absolute;
    width: 80px;
    height: 40px;
    background: radial-gradient(ellipse at center, rgba(58, 91, 160, 0.06) 0%, transparent 70%);
    border-radius: 50%;
    animation: floatCloud 15s ease-in-out infinite;
}

.cloud-1 {
    top: 10%;
    left: 5%;
    animation-delay: 0s;
}

.cloud-2 {
    top: 60%;
    right: 8%;
    animation-delay: 3s;
}

.ink-bamboo {
    position: absolute;
    bottom: 15%;
    right: 5%;
    width: 60px;
    height: 120px;
    background: linear-gradient(to top, rgba(95, 124, 110, 0.08), transparent);
    border-radius: 4px;
    animation: sway 8s ease-in-out infinite;
}

@keyframes floatCloud {
    0%, 100% { transform: translate(0, 0); }
    50% { transform: translate(-10px, -15px); }
}

@keyframes sway {
    0%, 100% { transform: rotate(0deg); }
    50% { transform: rotate(3deg); }
}

.forget-header {
    text-align: center;
    margin-bottom: 32px;
    position: relative;
    z-index: 10;
}

.logo-icon {
    width: 70px;
    height: 70px;
    margin: 0 auto 20px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 24px rgba(58, 91, 160, 0.25);
    border: 3px dashed rgba(201, 160, 99, 0.4);
    animation: float 3s ease-in-out infinite, rotate360 20s linear infinite;
    position: relative;
    z-index: 2;
}

.logo-icon i {
    font-size: 32px;
    color: white;
}

@keyframes float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-10px); }
}

@keyframes rotate360 {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.forget-title {
    font-size: 32px;
    font-weight: 700;
    background: var(--gradient-primary);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0;
    letter-spacing: 4px;
    position: relative;
}

.forget-form {
    margin-top: 0;
    position: relative;
    z-index: 1;
}

.form-content {
    width: 85%;
    margin: 0 auto;
}

.validate-container {
    display: flex;
    gap: 16px;
}

.validate-btn {
    flex-shrink: 0;
    width: 120px;
    height: 44px;
    font-size: 14px;
    font-weight: 700;
    border-radius: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 15px;
    background: linear-gradient(135deg, #B8935C 0%, #8B6A3A 100%) !important;
    border: none !important;
    color: white !important;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    box-shadow: 0 4px 12px rgba(184, 147, 92, 0.35);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;
    letter-spacing: 0.5px;
}

.validate-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(184, 147, 92, 0.45),
                0 0 30px rgba(201, 160, 99, 0.3) !important;
    background: linear-gradient(135deg, #C9A063 0%, #9D773E 100%) !important;
    border-color: transparent !important;
    color: white !important;
}

.validate-btn:disabled,
.validate-btn.is-loading {
    opacity: 0.7;
    cursor: not-allowed;
    background: linear-gradient(135deg, #B8935C 0%, #8B6A3A 100%) !important;
    box-shadow: 0 4px 12px rgba(184, 147, 92, 0.3) !important;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2) !important;
    color: white !important;
    border-color: transparent !important;
    transform: none !important;
}

.validate-btn.is-loading:hover {
    transform: none !important;
}

.submit-button {
    width: 100%;
    height: 50px;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 3px;
    margin-top: 20px;
    border-radius: 25px;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    background: linear-gradient(135deg, #C23A3B 0%, #A52E2F 100%) !important;
    border: none !important;
    color: white !important;
    text-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
    box-shadow: 0 6px 20px rgba(194, 58, 59, 0.35);
    cursor: pointer;
    position: relative;
    overflow: hidden;
}

.submit-button::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
}

.submit-button:hover::before {
    width: 300px;
    height: 300px;
}

.submit-button:hover {
    transform: translateY(-3px) scale(1.02);
    box-shadow: 0 12px 32px rgba(194, 58, 59, 0.45),
                0 0 40px rgba(224, 92, 93, 0.3) !important;
    background: linear-gradient(135deg, #D44345 0%, #B83334 100%) !important;
    border-color: transparent !important;
    color: white !important;
}

.submit-button:active {
    transform: translateY(-1px) scale(1.01);
    background: linear-gradient(135deg, #A52E2F 0%, #8B2426 100%) !important;
    color: white !important;
}

.submit-button:disabled,
.submit-button.is-loading {
    opacity: 0.85;
    cursor: not-allowed;
    transform: none !important;
    background: linear-gradient(135deg, #C23A3B 0%, #A52E2F 100%) !important;
    color: white !important;
    box-shadow: 0 6px 20px rgba(194, 58, 59, 0.35) !important;
}

.submit-button.is-loading:hover {
    transform: none !important;
    box-shadow: 0 6px 20px rgba(194, 58, 59, 0.35) !important;
}

.submit-button:focus {
    outline: none !important;
    box-shadow: 0 0 0 4px rgba(194, 58, 59, 0.2) !important;
}

.validate-btn:focus {
    outline: none !important;
    box-shadow: 0 0 0 4px rgba(201, 160, 99, 0.2) !important;
}

.validate-btn:active:not(:disabled) {
    transform: translateY(0);
    background: linear-gradient(135deg, #8B6A3A 0%, #6B4F2A 100%) !important;
    color: white !important;
}

.form-footer {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    font-size: 14px;
}

.back-link {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--primary-color);
    font-weight: 500;
    transition: all 0.3s ease;
    text-decoration: none;
}

.back-link:hover {
    color: var(--primary-light);
    transform: translateX(-3px);
}

.back-link i {
    transition: transform 0.3s ease;
}

.back-link:hover i {
    transform: translateX(-2px);
}

/* Element UI 组件样式覆盖 */
:deep(.el-input__inner) {
    height: 44px;
    line-height: 44px;
    border-radius: 22px;
    border: 2px solid var(--border-color);
    background: rgba(255, 255, 255, 0.9);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    padding-left: 42px;
}

:deep(.el-input__inner:focus) {
    border-color: var(--primary-color);
    background: white;
    box-shadow: 0 0 0 3px rgba(58, 91, 160, 0.1),
                0 4px 12px rgba(58, 91, 160, 0.15);
    transform: translateY(-1px);
}

:deep(.el-input__inner:hover) {
    border-color: var(--primary-light);
}

:deep(.el-form-item) {
    margin-bottom: 24px;
}

:deep(.el-input__prefix) {
    left: 15px;
    top: 0;
    height: 100%;
    display: flex;
    align-items: center;
    color: var(--primary-color);
}

:deep(.el-input__prefix i) {
    font-size: 18px;
    line-height: 1;
    display: flex;
    align-items: center;
}

:deep(.el-input:focus-within .el-input__prefix) {
    color: var(--primary-light);
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

/* 响应式调整 */
@media (max-width: 1024px) {
    .chinese-decoration {
        opacity: 0.5;
    }
    
    .cloud {
        display: none;
    }
}

@media (max-width: 768px) {
    .forget-title {
        font-size: 28px;
        letter-spacing: 3px;
    }
    
    .logo-icon {
        width: 60px;
        height: 60px;
    }
    
    .logo-icon i {
        font-size: 28px;
    }
}

@media (max-width: 480px) {
    .forget-container {
        padding: 30px 20px 0 20px;
    }
    
    .chinese-decoration {
        opacity: 0.3;
    }
    
    .ink-bamboo {
        display: none;
    }
    
    .forget-title {
        font-size: 24px;
        letter-spacing: 2px;
    }
    
    .logo-icon {
        width: 55px;
        height: 55px;
        margin-bottom: 15px;
    }
    
    .logo-icon i {
        font-size: 24px;
    }
    
    .forget-header {
        margin-bottom: 24px;
    }
    
    .form-content {
        width: 100%;
    }
    
    .validate-container {
        gap: 8px;
    }
    
    .validate-btn {
        width: 100px;
        font-size: 13px;
        height: 42px;
        padding: 0 10px;
    }
    
    .submit-button {
        height: 48px;
        font-size: 17px;
        letter-spacing: 2px;
    }
    
    :deep(.el-input__inner) {
        height: 42px;
        line-height: 42px;
    }
}
</style>