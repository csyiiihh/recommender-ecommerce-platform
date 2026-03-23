<template>
  <div class="customer-service-page">
    <div class="page-header">
      <h2>客服会话</h2>
      <el-tag type="warning">{{ activeSessions.length }} 个活跃会话</el-tag>
    </div>

    <div class="service-container">
      <!-- 左侧：会话列表 -->
      <div class="session-list-panel">
        <div class="panel-header">
          <span class="header-title">会话列表</span>
          <el-button size="small" @click="loadSessions" circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>

        <el-tabs v-model="activeTab" @tab-click="handleTabChange">
          <el-tab-pane label="待接入" name="pending">
            <el-badge :value="pendingSessions.length" :max="99" class="tab-badge">
              待接入
            </el-badge>
          </el-tab-pane>
          <el-tab-pane label="进行中" name="active">
            <el-badge :value="activeSessions.length" :max="99" class="tab-badge">
              进行中
            </el-badge>
          </el-tab-pane>
        </el-tabs>

        <div class="session-list">
          <div v-if="loading" class="loading-container">
            <i class="fas fa-spinner fa-spin"></i>
            <span>加载中...</span>
          </div>

          <div v-else-if="currentSessions.length === 0" class="empty-sessions">
            <i class="fas fa-comments"></i>
            <p>暂无会话</p>
          </div>

          <div
            v-for="session in currentSessions"
            :key="session.id"
            :class="['session-item', { 'active': currentSession && currentSession.id === session.id }]"
            @click="selectSession(session)"
          >
            <div class="session-avatar">
              <img v-if="session.user && session.user.businessLicense" 
                   :src="'/api' + session.user.businessLicense" 
                   class="avatar-img" />
              <i v-else class="fas fa-user"></i>
              <div v-if="session.unreadService > 0" class="unread-badge">{{ session.unreadService }}</div>
            </div>
            <div class="session-info">
              <div class="session-header">
                <span class="user-name">{{ session.user ? session.user.name || session.user.username : '未知用户' }}</span>
                <span class="session-time">{{ formatTime(session.lastMessageTime) }}</span>
              </div>
              <div class="session-preview">{{ session.lastMessage || '暂无消息' }}</div>
            </div>
            <div v-if="session.status === 0" class="pending-indicator">
              <el-tag size="small" type="warning">待接入</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：聊天区域 -->
      <div class="chat-panel">
        <div v-if="!currentSession" class="no-session-selected">
          <i class="fas fa-comment-dots"></i>
          <p>请选择一个会话开始聊天</p>
        </div>

        <template v-else>
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="user-info">
              <div class="user-avatar-large">
                <img v-if="currentSession.user && currentSession.user.businessLicense" 
                     :src="'/api' + currentSession.user.businessLicense" 
                     class="avatar-img" />
                <i v-else class="fas fa-user"></i>
              </div>
              <div class="user-details">
                <div class="user-name">{{ currentSession.user ? currentSession.user.name || currentSession.user.username : '未知用户' }}</div>
                <div class="user-status">
                  <el-tag size="small" :type="sessionStatusType">{{ sessionStatusText }}</el-tag>
                </div>
              </div>
            </div>
            <div class="chat-actions">
              <el-button
                v-if="currentSession.status === 0"
                type="primary"
                size="small"
                @click="acceptSession"
                :loading="accepting"
              >
                接入会话
              </el-button>
              <el-button
                v-if="currentSession.status === 1"
                size="small"
                @click="endSessionDialog"
              >
                结束会话
              </el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageList">
            <div v-if="loadingMessages" class="loading-container">
              <i class="fas fa-spinner fa-spin"></i>
              <span>加载消息中...</span>
            </div>

            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', msg.senderType === 'SERVICE' ? 'service-message' : 'user-message']"
            >
              <div class="message-avatar">
                <img v-if="msg.senderType === 'USER' && currentSession.user && currentSession.user.businessLicense" 
                     :src="'/api' + currentSession.user.businessLicense" 
                     class="avatar-img" />
                <i v-else :class="msg.senderType === 'SERVICE' ? 'fas fa-headset' : 'fas fa-user'"></i>
              </div>
              <div class="message-content">
                <div class="message-info">
                  <span class="sender-name">{{ msg.senderType === 'SERVICE' ? '客服' : '用户' }}</span>
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
              :rows="4"
              resize="none"
              placeholder="输入回复内容..."
              @keyup.enter.ctrl="sendMessage"
              :disabled="currentSession.status !== 1"
            ></el-input>
            <div class="input-actions">
              <span class="input-tip">Ctrl + Enter 发送</span>
              <el-button
                type="primary"
                size="small"
                @click="sendMessage"
                :disabled="!inputMessage.trim() || currentSession.status !== 1"
                :loading="sending"
              >
                发送消息
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';
import logger from '@/utils/logger'
import { Refresh } from '@element-plus/icons-vue'

export default {
  name: 'CustomerService',
  data() {
    return {
      loading: false,
      loadingMessages: false,
      sending: false,
      accepting: false,
      sessions: [],
      currentSession: null,
      messages: [],
      inputMessage: '',
      activeTab: 'pending',
      pollingTimer: null,
      currentUser: null,
    };
  },
  computed: {
    pendingSessions() {
      return this.sessions.filter(s => s.status === 0);
    },
    activeSessions() {
      return this.sessions.filter(s => s.status === 1);
    },
    currentSessions() {
      return this.activeTab === 'pending' ? this.pendingSessions : this.activeSessions;
    },
    sessionStatusType() {
      if (!this.currentSession) return 'info';
      return this.currentSession.status === 0 ? 'warning' : this.currentSession.status === 1 ? 'success' : 'info';
    },
    sessionStatusText() {
      if (!this.currentSession) return '';
      const statusMap = { 0: '待接入', 1: '进行中', 2: '已结束' };
      return statusMap[this.currentSession.status] || '未知';
    }
  },
  mounted() {
    this.loadCurrentUser();
    this.loadSessions();
    this.startPolling();
  },
  beforeUnmount() {
    this.stopPolling();
  },
  methods: {
    loadCurrentUser() {
      const backUser = localStorage.getItem('backUser');
      if (backUser) {
        try {
          this.currentUser = JSON.parse(backUser);
        } catch (e) {
          logger.error('解析用户信息失败:', e);
        }
      }
    },

    async loadSessions() {
      if (!this.currentUser) return;

      this.loading = true;
      try {
        const res = await request.get('/customerService/session/service', {
          params: {
            serviceId: this.currentUser.id
          }
        });

        if (res.code === '0') {
          this.sessions = res.data || [];
          
          // 解析用户信息（从后端返回的数据中提取）
          this.sessions.forEach(session => {
            if (!session.user) {
              session.user = {
                id: session.userId,
                username: '用户' + session.userId,
                name: '用户' + session.userId
              };
            }
          });
        } else {
          this.$message.error(res.msg || '加载会话列表失败');
        }
      } catch (error) {
        logger.error('加载会话列表失败:', error);
      } finally {
        this.loading = false;
      }
    },

    async selectSession(session) {
      this.currentSession = session;
      await this.loadMessages();
    },

    async loadMessages() {
      if (!this.currentSession || !this.currentUser) return;

      this.loadingMessages = true;
      try {
        const res = await request.get('/customerService/message/list', {
          params: {
            sessionId: this.currentSession.id,
            currentUserId: this.currentUser.id,
            userType: 'SERVICE'
          }
        });

        if (res.code === '0') {
          this.messages = res.data || [];
          this.$nextTick(() => {
            this.scrollToBottom();
          });
          
          // 更新会话的未读数
          this.currentSession.unreadService = 0;
        }
      } catch (error) {
        logger.error('加载消息失败:', error);
      } finally {
        this.loadingMessages = false;
      }
    },

    async acceptSession() {
      if (!this.currentSession || !this.currentUser) return;

      this.accepting = true;
      try {
        const res = await request.post('/customerService/session/accept', null, {
          params: {
            sessionId: this.currentSession.id,
            serviceId: this.currentUser.id
          }
        });

        if (res.code === '0') {
          this.$message.success('会话接入成功');
          this.currentSession.status = 1;
          this.currentSession.serviceId = this.currentUser.id;
          await this.loadSessions();
        } else {
          this.$message.error(res.msg || '接入失败');
        }
      } catch (error) {
        logger.error('接入会话失败:', error);
        this.$message.error('接入会话失败');
      } finally {
        this.accepting = false;
      }
    },

    endSessionDialog() {
      this.$confirm('确认结束当前会话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.endSession();
      }).catch(() => {});
    },

    async endSession() {
      if (!this.currentSession) return;

      try {
        const res = await request.post('/customerService/session/end', null, {
          params: {
            sessionId: this.currentSession.id
          }
        });

        if (res.code === '0') {
          this.$message.success('会话已结束');
          this.currentSession.status = 2;
          this.currentSession = null;
          await this.loadSessions();
        } else {
          this.$message.error(res.msg || '结束会话失败');
        }
      } catch (error) {
        logger.error('结束会话失败:', error);
        this.$message.error('结束会话失败');
      }
    },

    async sendMessage() {
      if (!this.inputMessage.trim() || this.sending || !this.currentSession || !this.currentUser) {
        return;
      }

      if (this.currentSession.status !== 1) {
        this.$message.warning('请先接入会话');
        return;
      }

      this.sending = true;
      try {
        const res = await request.post('/customerService/message/send', {
          sessionId: this.currentSession.id,
          senderId: this.currentUser.id,
          senderType: 'SERVICE',
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

    handleTabChange() {
      this.currentSession = null;
      this.messages = [];
    },

    startPolling() {
      this.stopPolling();
      this.pollingTimer = setInterval(() => {
        this.loadSessions();
        if (this.currentSession) {
          this.loadMessages();
        }
      }, 10000); // 每10秒轮询
    },

    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
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
.customer-service-page {
  padding: 20px;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #2c3e50;
}

.service-container {
  flex: 1;
  display: flex;
  gap: 20px;
  overflow: hidden;
}

/* 会话列表面板 */
.session-list-panel {
  width: 320px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.loading-container,
.empty-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
}

.loading-container i,
.empty-sessions i {
  font-size: 48px;
  margin-bottom: 12px;
  color: #dcdfe6;
}

/* 会话项 */
.session-item {
  display: flex;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
}

.session-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #FF8C42;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.session-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.session-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.session-time {
  font-size: 12px;
  color: #909399;
}

.session-preview {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-indicator {
  position: absolute;
  top: 16px;
  right: 20px;
}

/* 聊天面板 */
.chat-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.no-session-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.no-session-selected i {
  font-size: 64px;
  margin-bottom: 16px;
  color: #dcdfe6;
}

.no-session-selected p {
  font-size: 16px;
}

/* 聊天头部 */
.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-large {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #FF8C42;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  overflow: hidden;
}

.user-avatar-large .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details .user-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
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
  flex-direction: row;
}

.service-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #FF8C42;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
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
  max-width: 60%;
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

.service-message .message-info {
  flex-direction: row-reverse;
}

.sender-name {
  font-weight: 500;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  background: white;
  word-wrap: break-word;
  line-height: 1.6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.service-message .message-text {
  background: #409eff;
  color: white;
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #e4e7ed;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-tip {
  font-size: 12px;
  color: #909399;
}

/* 标签 */
.tab-badge {
  width: 100%;
}

/* 响应式 */
@media (max-width: 1200px) {
  .session-list-panel {
    width: 280px;
  }
}

@media (max-width: 768px) {
  .service-container {
    flex-direction: column;
  }

  .session-list-panel {
    width: 100%;
    height: 300px;
  }
}
</style>

