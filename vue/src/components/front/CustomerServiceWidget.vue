<template>
  <div class="customer-service-widget">
    <!-- 客服入口按钮 -->
    <el-badge 
      :value="unreadCount" 
      :hidden="unreadCount === 0" 
      :max="99"
      class="service-badge"
    >
      <el-button
        type="warning"
        circle
        class="service-btn"
        @click="toggleChat"
        :title="showChat ? '关闭客服' : '联系客服'"
      >
        <i :class="showChat ? 'fas fa-times' : 'fas fa-headset'"></i>
      </el-button>
    </el-badge>

    <!-- 客服聊天窗口 -->
    <transition name="slide-fade">
      <div v-if="showChat" class="chat-window" @click.stop>
        <!-- 聊天窗口头部 -->
        <div class="chat-header">
          <div class="header-info">
            <i class="fas fa-headset"></i>
            <div class="header-text">
              <div class="title">在线客服</div>
              <div class="subtitle">{{ sessionStatus }}</div>
            </div>
          </div>
          <el-button
            link
            @click="closeChat"
            class="close-btn"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- 消息列表区域 -->
        <div class="message-list" ref="messageList">
          <div v-if="loading" class="loading-container">
            <i class="fas fa-spinner fa-spin"></i>
            <span>加载中...</span>
          </div>

          <div v-else-if="messages.length === 0" class="empty-message">
            <i class="fas fa-comments"></i>
            <p>欢迎！有什么可以帮助您的吗？</p>
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message-item', msg.senderType === 'USER' ? 'user-message' : 'service-message']"
          >
            <div class="message-avatar">
              <img v-if="msg.senderType === 'USER' && currentUser && currentUser.businessLicense" 
                   :src="'api' + currentUser.businessLicense" 
                   class="avatar-img" />
              <i v-else :class="msg.senderType === 'USER' ? 'fas fa-user' : 'fas fa-headset'"></i>
            </div>
            <div class="message-content">
              <div class="message-info">
                <span class="sender-name">{{ msg.senderType === 'USER' ? '我' : '客服' }}</span>
                <span class="send-time">{{ formatTime(msg.createdAt) }}</span>
              </div>
              <div class="message-text">{{ msg.content }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="请输入您的问题..."
            @keyup.enter.ctrl="sendMessage"
            class="message-input"
          ></el-input>
          <div class="input-actions">
            <span class="input-tip">Ctrl + Enter 发送</span>
            <el-button
              type="primary"
              size="small"
              @click="sendMessage"
              :disabled="!inputMessage.trim()"
              :loading="sending"
              class="send-btn-chinese"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import request from '@/utils/request';
import logger from '@/utils/logger'
import eventBus from '@/utils/eventBus';
import { Close } from '@element-plus/icons-vue'

export default {
  name: 'CustomerServiceWidget',
  components: {
    Close
  },
  data() {
    return {
      showChat: false,
      loading: false,
      sending: false,
      sessionId: null,
      messages: [],
      inputMessage: '',
      unreadCount: 0,
      pollingTimer: null,
      currentUser: null,
    };
  },
  computed: {
    sessionStatus() {
      if (!this.sessionId) {
        return '等待连接...';
      }
      return '在线';
    }
  },
  mounted() {
    this.loadCurrentUser();
    // 监听全局事件 - Vue 3 使用事件总线
    eventBus.on('toggle-customer-service', this.toggleChat);
    // 监听用户信息更新
    window.addEventListener('userInfoUpdated', this.loadCurrentUser);
  },
  beforeUnmount() {
    this.stopPolling();
    // Vue 3 使用 beforeUnmount 替代 beforeDestroy
    eventBus.off('toggle-customer-service', this.toggleChat);
    window.removeEventListener('userInfoUpdated', this.loadCurrentUser);
  },
  methods: {
    loadCurrentUser() {
      const frontUser = localStorage.getItem('frontUser');
      if (frontUser) {
        try {
          this.currentUser = JSON.parse(frontUser);
        } catch (e) {
          logger.error('解析用户信息失败:', e);
        }
      }
    },

    async toggleChat() {
      if (!this.currentUser) {
        this.$message.warning('请先登录');
        this.$router.push('/login');
        return;
      }

      this.showChat = !this.showChat;
      if (this.showChat) {
        await this.initSession();
        this.startPolling();
      } else {
        this.stopPolling();
      }
    },

    closeChat() {
      this.showChat = false;
      this.stopPolling();
    },

    async initSession() {
      if (!this.currentUser) return;

      this.loading = true;
      try {
        const res = await request.post('/customerService/session/create', null, {
          params: {
            userId: this.currentUser.id
          }
        });

        if (res.code === '0') {
          this.sessionId = res.data.id;
          await this.loadMessages();
        } else {
          this.$message.error(res.msg || '创建会话失败');
        }
      } catch (error) {
        logger.error('创建会话失败:', error);
        this.$message.error('创建会话失败');
      } finally {
        this.loading = false;
      }
    },

    async loadMessages() {
      if (!this.sessionId || !this.currentUser) return;

      try {
        const res = await request.get('/customerService/message/list', {
          params: {
            sessionId: this.sessionId,
            currentUserId: this.currentUser.id,
            userType: 'USER'
          }
        });

        if (res.code === '0') {
          this.messages = res.data || [];
          this.$nextTick(() => {
            this.scrollToBottom();
          });
          // 重置未读数
          this.unreadCount = 0;
        }
      } catch (error) {
        logger.error('加载消息失败:', error);
      }
    },

    async sendMessage() {
      if (!this.inputMessage.trim() || this.sending || !this.sessionId || !this.currentUser) {
        return;
      }

      this.sending = true;
      try {
        const res = await request.post('/customerService/message/send', {
          sessionId: this.sessionId,
          senderId: this.currentUser.id,
          senderType: 'USER',
          content: this.inputMessage.trim(),
          messageType: 'TEXT'
        });

        if (res.code === '0') {
          this.messages.push(res.data);
          this.inputMessage = '';
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        } else {
          this.$message.error(res.msg || '发送失败');
        }
      } catch (error) {
        logger.error('发送消息失败:', error);
        this.$message.error('发送消息失败');
      } finally {
        this.sending = false;
      }
    },

    startPolling() {
      this.stopPolling();
      this.pollingTimer = setInterval(() => {
        this.loadMessages();
      }, 10000); // 每10秒轮询一次
    },

    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },

    async loadUnreadCount() {
      if (!this.currentUser) return;

      try {
        const res = await request.get('/customerService/message/unread', {
          params: {
            userId: this.currentUser.id,
            userType: 'USER'
          }
        });

        if (res.code === '0') {
          this.unreadCount = res.data || 0;
        }
      } catch (error) {
        logger.error('获取未读数失败:', error);
      }
    },

    scrollToBottom() {
      const container = this.$refs.messageList;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },

    formatTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;

      if (diff < 60000) return '刚刚';
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;

      return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
    }
  }
};
</script>

<style scoped>
.customer-service-widget {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 9999;
}

.service-badge {
  display: block;
}

.service-btn {
  width: 60px;
  height: 60px;
  font-size: 24px;
  background: var(--gradient-primary);
  border: none;
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.4);
  transition: all 0.3s ease;
}

.service-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(58, 91, 160, 0.5);
  background: var(--gradient-primary) !important;
}

.service-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(58, 91, 160, 0.4);
}

/* 确保按钮颜色不变 */
.service-btn,
.service-btn:hover,
.service-btn:focus,
.service-btn:active {
  color: white !important;
}

/* 聊天窗口 - 中国风边框 */
.chat-window {
  position: fixed;
  bottom: 110px;
  right: 30px;
  width: 380px;
  height: 550px;
  background: white;
  border-radius: 16px;
  box-shadow: 
    0 8px 32px rgba(58, 91, 160, 0.15),
    0 0 0 1px rgba(58, 91, 160, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 青花瓷边框效果 */
  position: relative;
}

/* 四角装饰 */
.chat-window::before,
.chat-window::after {
  content: '';
  position: absolute;
  width: 40px;
  height: 40px;
  background-image: url("data:image/svg+xml,%3Csvg width='40' height='40' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0,8 Q0,0 8,0 L32,0 Q36,0 38,2' stroke='%233A5BA0' stroke-width='1' fill='none' opacity='0.3'/%3E%3Cpath d='M5,12 Q7,7 12,5' stroke='%23C9A063' stroke-width='0.8' fill='none' opacity='0.25'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  pointer-events: none;
  z-index: 10;
}

.chat-window::before {
  top: 0;
  left: 0;
}

.chat-window::after {
  bottom: 0;
  right: 0;
  transform: rotate(180deg);
}

/* 动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter,
.slide-fade-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* 聊天头部 - 中国风 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--gradient-primary);
  color: white;
  position: relative;
  overflow: hidden;
}

/* 祥云装饰 */
.chat-header::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 0;
  right: 0;
  height: 30px;
  background-image: url("data:image/svg+xml,%3Csvg width='120' height='30' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M10,15 Q20,8 30,12 Q40,16 50,12 Q60,8 70,15 Q80,20 90,15 Q100,10 110,15' stroke='white' stroke-width='1.5' fill='none' opacity='0.2'/%3E%3C/svg%3E");
  background-repeat: repeat-x;
  background-size: 120px 30px;
  pointer-events: none;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info > i {
  font-size: 24px;
}

.header-text .title {
  font-size: 16px;
  font-weight: 600;
}

.header-text .subtitle {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

.close-btn {
  color: white !important;
  font-size: 20px;
}

.close-btn:hover {
  opacity: 0.8;
}

/* 消息列表 */
/* 消息列表 - 宣纸纹理背景 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-paper);
  /* 宣纸纹理 */
  background-image: 
    repeating-linear-gradient(90deg, transparent, transparent 40px, rgba(58, 91, 160, 0.01) 40px, rgba(58, 91, 160, 0.01) 41px),
    repeating-linear-gradient(0deg, transparent, transparent 40px, rgba(58, 91, 160, 0.01) 40px, rgba(58, 91, 160, 0.01) 41px);
  position: relative;
}

/* 淡淡的水墨渲染效果 */
.message-list::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 10% 20%, rgba(95, 124, 110, 0.02) 0%, transparent 50%),
    radial-gradient(circle at 90% 80%, rgba(58, 91, 160, 0.02) 0%, transparent 50%);
  pointer-events: none;
}

.loading-container,
.empty-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.loading-container i {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-message i {
  font-size: 48px;
  margin-bottom: 12px;
  color: #dcdfe6;
}

.empty-message p {
  margin: 0;
  font-size: 14px;
}

/* 消息项 */
.message-item {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
  overflow: hidden;
}

.message-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.service-message .message-avatar {
  background: #409eff;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.user-message .message-info {
  flex-direction: row-reverse;
}

.sender-name {
  font-weight: 500;
}

/* 中国风气泡样式 */
.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.6;
  position: relative;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.12);
}

/* 客服消息 - 白色青花瓷纹理 */
.service-message .message-text {
  background: linear-gradient(135deg, #FFFFFF 0%, #F5F8FA 100%);
  color: #1a1a1a;
  font-weight: 500;
  border: 1px solid rgba(58, 91, 160, 0.15);
  /* 青花瓷纹理 */
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(58, 91, 160, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(58, 91, 160, 0.04) 0%, transparent 50%);
}

/* 客服气泡尾巴 - 左侧 */
.service-message .message-text::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 12px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 8px 8px 8px 0;
  border-color: transparent rgba(58, 91, 160, 0.15) transparent transparent;
}

.service-message .message-text::after {
  content: '';
  position: absolute;
  left: -7px;
  top: 12px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 8px 8px 8px 0;
  border-color: transparent #FFFFFF transparent transparent;
}

/* 用户消息 - 浅青花蓝背景 + 深黑色文字 */
.user-message .message-text {
  background: linear-gradient(135deg, #EDF2FA 0%, #DDE7F5 100%);
  color: #1a1a1a;
  font-weight: 500;
  border: 1px solid rgba(58, 91, 160, 0.25);
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.15);
  /* 青花瓷纹理 */
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(58, 91, 160, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(58, 91, 160, 0.04) 0%, transparent 50%);
}

/* 用户气泡尾巴 - 右侧（浅色） */
.user-message .message-text::before {
  content: '';
  position: absolute;
  right: -8px;
  top: 12px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 8px 0 8px 8px;
  border-color: transparent transparent transparent #EDF2FA;
  filter: drop-shadow(2px 2px 3px rgba(58, 91, 160, 0.1));
}

/* 气泡上的装饰点 */
.message-text::after {
  content: '';
  position: absolute;
  bottom: 4px;
  right: 8px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.2;
}

/* 输入区域 */
/* 输入区域 - 中国风 */
.input-area {
  padding: 12px 16px;
  background: var(--bg-paper);
  border-top: 2px solid rgba(58, 91, 160, 0.1);
  position: relative;
}

/* 顶部祥云装饰 */
.input-area::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 0;
  right: 0;
  height: 20px;
  background-image: url("data:image/svg+xml,%3Csvg width='100' height='20' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0,10 Q25,5 50,10 T100,10' stroke='%233A5BA0' stroke-width='1' fill='none' opacity='0.15'/%3E%3C/svg%3E");
  background-repeat: repeat-x;
  background-size: 100px 20px;
  pointer-events: none;
}

.message-input {
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-tip {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 发送按钮 - 中国风 */
.send-btn-chinese {
  background: var(--gradient-primary) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(58, 91, 160, 0.25) !important;
  transition: all 0.3s ease !important;
}

.send-btn-chinese:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(58, 91, 160, 0.35) !important;
}

.send-btn-chinese:active {
  transform: translateY(0) !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .customer-service-widget {
    bottom: 20px;
    right: 20px;
  }

  .service-btn {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }

  .chat-window {
    right: 20px;
    bottom: 90px;
    width: calc(100vw - 40px);
    max-width: 380px;
  }
}
</style>
