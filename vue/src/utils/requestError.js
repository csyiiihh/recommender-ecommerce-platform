import { ElMessage } from 'element-plus'

// The request interceptor owns authentication errors; forms own other failures.
export function showRequestError(error, fallback = '保存失败，请稍后重试') {
  if (error.response?.status === 401) return
  ElMessage.error(error.response?.data?.msg || fallback)
}
