<template>
  <div class="customer-service-history">
    <div class="page-header">
      <h2>会话历史记录</h2>
      <el-tag type="info">共 {{ total }} 条记录</el-tag>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="会话状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="全部" value=""></el-option>
            <el-option label="待接入" :value="0"></el-option>
            <el-option label="进行中" :value="1"></el-option>
            <el-option label="已结束" :value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon pending-icon">
                <i class="fas fa-clock"></i>
              </div>
              <div class="stat-info">
                <div class="stat-title">待接入</div>
                <div class="stat-value">{{ getCountByStatus(0) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon active-icon">
                <i class="fas fa-comments"></i>
              </div>
              <div class="stat-info">
                <div class="stat-title">进行中</div>
                <div class="stat-value">{{ getCountByStatus(1) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon closed-icon">
                <i class="fas fa-check-circle"></i>
              </div>
              <div class="stat-info">
                <div class="stat-title">已结束</div>
                <div class="stat-value">{{ getCountByStatus(2) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total-icon">
                <i class="fas fa-chart-bar"></i>
              </div>
              <div class="stat-info">
                <div class="stat-title">总计</div>
                <div class="stat-value">{{ total }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 会话列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table
        :data="sessions"
        border
        style="width: 100%"
        v-loading="loading"
        :default-sort="{ prop: 'lastMessageTime', order: 'descending' }"
      >
        <el-table-column prop="id" label="会话ID" width="100" sortable></el-table-column>
        
        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <div class="user-info-cell">
              <div class="user-avatar-small">
                <img v-if="row.user && row.user.businessLicense" 
                     :src="'/api' + row.user.businessLicense" 
                     class="avatar-img" />
                <i v-else class="fas fa-user"></i>
              </div>
              <div class="user-details">
                <div>{{ row.user ? row.user.name || row.user.username : '未知用户' }}</div>
                <div class="user-id">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="客服人员" width="120">
          <template #default="{ row }">
            <span v-if="row.service">{{ row.service.name || row.service.username }}</span>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>

        <el-table-column label="最后消息" min-width="250">
          <template #default="{ row }">
            <div class="last-message">
              {{ row.lastMessage || '暂无消息' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="会话状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="未读消息" width="100" align="center">
          <template #default="{ row }">
            <el-badge :value="row.unreadService" :max="99" v-if="row.unreadService > 0">
              <span>{{ row.unreadService }}</span>
            </el-badge>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="lastMessageTime" label="最后消息时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.lastMessageTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              size="small"
              @click="viewMessages(row)"
            >
              <el-icon><View /></el-icon>
              查看消息
            </el-button>
            <el-button
              link
              size="small"
              @click="deleteSession(row)"
              style="color: #F56C6C;"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        >
        </el-pagination>
      </div>
    </el-card>

    <!-- 消息详情对话框 -->
    <el-dialog
      title="消息记录"
      v-model="messageDialogVisible"
      width="60%"
      top="5vh"
    >
      <div class="message-dialog-header" v-if="currentSession">
        <div class="session-info-detail">
          <div class="info-item">
            <span class="label">会话ID:</span>
            <span class="value">{{ currentSession.id }}</span>
          </div>
          <div class="info-item">
            <span class="label">用户:</span>
            <span class="value">{{ currentSession.user ? currentSession.user.name || currentSession.user.username : '未知' }}</span>
          </div>
          <div class="info-item">
            <span class="label">客服:</span>
            <span class="value">{{ currentSession.service ? currentSession.service.name || currentSession.service.username : '未分配' }}</span>
          </div>
          <div class="info-item">
            <span class="label">状态:</span>
            <el-tag :type="getStatusType(currentSession.status)" size="small">
              {{ getStatusText(currentSession.status) }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="message-list-dialog" v-loading="loadingMessages">
        <div v-if="dialogMessages.length === 0" class="empty-messages">
          <i class="fas fa-comments"></i>
          <p>暂无消息记录</p>
        </div>

            <div
              v-for="msg in dialogMessages"
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
              <span class="send-time">{{ formatDateTime(msg.createdAt) }}</span>
            </div>
            <div class="message-text">{{ msg.content }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="messageDialogVisible = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request';
import logger from '@/utils/logger'
import { View, Delete } from '@element-plus/icons-vue'

export default {
  name: 'CustomerServiceHistory',
  components: {
    View,
    Delete
  },
  data() {
    return {
      loading: false,
      loadingMessages: false,
      sessions: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      searchForm: {
        status: '',
        userId: ''
      },
      messageDialogVisible: false,
      currentSession: null,
      dialogMessages: [],
      currentUser: null,
    };
  },
  mounted() {
    this.loadCurrentUser();
    this.loadHistory();
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

    async loadHistory() {
      this.loading = true;
      try {
        const params = {
          currentPage: this.currentPage,
          pageSize: this.pageSize
        };

        if (this.searchForm.status !== null && this.searchForm.status !== '' && this.searchForm.status !== undefined) {
          params.status = this.searchForm.status;
        }
        if (this.searchForm.userId) {
          params.userId = this.searchForm.userId;
        }

        const res = await request.get('/customerService/session/history', { params });

        if (res.code === '0') {
          this.sessions = res.data.records || [];
          this.total = res.data.total || 0;
        } else {
          this.$message.error(res.msg || '加载历史记录失败');
        }
      } catch (error) {
        logger.error('加载历史记录失败:', error);
        this.$message.error('加载历史记录失败');
      } finally {
        this.loading = false;
      }
    },

    handleSearch() {
      this.currentPage = 1;
      this.loadHistory();
    },

    resetSearch() {
      this.searchForm = {
        status: '',
        userId: ''
      };
      this.currentPage = 1;
      this.loadHistory();
    },

    handleSizeChange(val) {
      this.pageSize = val;
      this.loadHistory();
    },

    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadHistory();
    },

    async viewMessages(session) {
      this.currentSession = session;
      this.messageDialogVisible = true;
      await this.loadSessionMessages();
    },

    async loadSessionMessages() {
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
          this.dialogMessages = res.data || [];
        } else {
          this.$message.error(res.msg || '加载消息失败');
        }
      } catch (error) {
        logger.error('加载消息失败:', error);
        this.$message.error('加载消息失败');
      } finally {
        this.loadingMessages = false;
      }
    },

    getCountByStatus(status) {
      return this.sessions.filter(s => s.status === status).length;
    },

    getStatusType(status) {
      const typeMap = { 0: 'warning', 1: 'success', 2: 'info' };
      return typeMap[status] || 'info';
    },

    getStatusText(status) {
      const textMap = { 0: '待接入', 1: '进行中', 2: '已结束' };
      return textMap[status] || '未知';
    },

    deleteSession(session) {
      this.$confirm(`确认删除会话ID ${session.id} 及其所有消息记录吗？此操作不可恢复！`, '删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        try {
          const res = await request.delete(`/customerService/session/${session.id}`);
          
          if (res.code === '0') {
            this.$message.success('删除成功');
            // 重新加载列表
            this.loadHistory();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        } catch (error) {
          logger.error('删除会话失败:', error);
          this.$message.error('删除会话失败');
        }
      }).catch(() => {
        // 用户取消删除
      });
    },

    formatDateTime(timestamp) {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    }
  }
};
</script>

<style scoped>
.customer-service-history {
  padding: 20px;
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

.search-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 0;
}

/* 统计卡片 */
.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.pending-icon {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.active-icon {
  background: linear-gradient(135deg, #409eff, #a0cfff);
}

.closed-icon {
  background: linear-gradient(135deg, #67c23a, #b3e19d);
}

.total-icon {
  background: linear-gradient(135deg, #FF8C42, #FFB366);
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

/* 表格 */
.table-card {
  margin-bottom: 20px;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #FF8C42;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  overflow: hidden;
}

.user-avatar-small .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.user-id {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.last-message {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-muted {
  color: #c0c4cc;
}

/* 分页 */
.pagination-container {
  margin-top: 20px;
  text-align: right;
}

/* 对话框 */
.message-dialog-header {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.session-info-detail {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.info-item .value {
  font-size: 14px;
  color: #2c3e50;
}

.message-list-dialog {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.empty-messages i {
  font-size: 48px;
  margin-bottom: 12px;
  color: #dcdfe6;
}

/* 消息项 */
.message-item {
  display: flex;
  margin-bottom: 20px;
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
</style>

