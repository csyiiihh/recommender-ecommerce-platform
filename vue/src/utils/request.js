import axios from 'axios'
import router from "../router";
import { ElMessage } from 'element-plus'
import logger from './logger'

const request = axios.create({
    baseURL: '/api',
    timeout: 25000
})

// 添加错误状态标记和计时器存储
// 使用动态对象，支持所有HTTP状态码
let errorFlags = {
    401: false, // 未授权
    403: false, // 禁止访问
    500: false, // 服务器错误
    408: false, // 超时
    'network': false // 网络错误
};

// 存储错误标记的定时器，用于清理
let errorTimers = {};

// 防止消息重复显示的标记
let messageShown = false;

// 修改重置错误标记的函数
const resetErrorFlags = (specificError = null) => {
    if (specificError) {
        // 重置特定错误
        errorFlags[specificError] = false;
        // 清除对应的定时器
        if (errorTimers[specificError]) {
            clearTimeout(errorTimers[specificError]);
            delete errorTimers[specificError];
        }
    } else {
        // 重置所有错误
        Object.keys(errorFlags).forEach(key => {
            errorFlags[key] = false;
        });
        // 清除所有定时器
        Object.keys(errorTimers).forEach(key => {
            clearTimeout(errorTimers[key]);
            delete errorTimers[key];
        });
    }
    // 重置消息显示标记
    messageShown = false;
};

// 设置错误标记的函数
const setErrorFlag = (status) => {
    // 如果状态码不存在，先添加到错误标记中
    if (!errorFlags.hasOwnProperty(status)) {
        errorFlags[status] = false;
    }
    
    // 如果已经有定时器，先清除
    if (errorTimers[status]) {
        clearTimeout(errorTimers[status]);
    }
    
    errorFlags[status] = true;
    
    // 10秒后自动重置该错误状态（延长防抖时间）
    errorTimers[status] = setTimeout(() => {
        resetErrorFlags(status);
        delete errorTimers[status];
    }, 10000);
};

window.onerror = function(message, url, lineNumber) {
    logger.error("发生未捕获的错误:", message);
};
  
request.interceptors.request.use(
    config => {
        // 记录请求开始时间，用于后续超时判断
        config.metadata = {
            startTime: Date.now()
        };
        
        const user = localStorage.getItem('backUser');
        if (user) {
            try {
                config.headers['token'] = JSON.parse(user).token;
            } catch (e) {
                logger.error('Token parsing error:', e);
            }
        }
        return config;
    },
    error => {
        logger.error('request error:', error);
        return Promise.reject(error);
    }
);

request.interceptors.response.use(
    response => {
        // 记录响应时间（仅记录非常慢的请求，避免过多日志）
        if (response.config.metadata?.startTime) {
            const duration = Date.now() - response.config.metadata.startTime;
            // 只记录超过10秒或接近超时时间的请求
            const timeout = response.config.timeout || 25000;
            const timeoutThreshold = timeout * 0.8; // 超时时间的80%作为阈值
            
            if (duration > timeoutThreshold) {
                // 接近超时，记录警告
                logger.warn('请求耗时接近超时:', {
                    url: response.config.url,
                    method: response.config.method,
                    duration: `${(duration / 1000).toFixed(2)}秒`,
                    timeout: `${timeout}ms`,
                    threshold: `${(timeoutThreshold / 1000).toFixed(2)}秒`
                });
            } else if (duration > 10000) {
                // 超过10秒但不是超时，记录debug级别（开发环境可见）
                logger.debug('请求耗时较长（已成功响应）:', {
                    url: response.config.url,
                    method: response.config.method,
                    duration: `${(duration / 1000).toFixed(2)}秒`
                });
            }
        }
        
        if (response.config.responseType === 'blob') {
            return response;
        }

        let res = response.data;
        if (typeof res === 'string') {
            try {
                res = JSON.parse(res);
            } catch (e) {
                logger.error('Response parsing error:', e);
            }
        }
        return res;
    },
    error => {
        // 记录完整的错误信息用于调试
        logger.debug('请求错误详情:', {
            hasResponse: !!error.response,
            status: error.response?.status,
            statusText: error.response?.statusText,
            code: error.code,
            message: error.message,
            url: error.config?.url,
            method: error.config?.method
        });
        
        // 如果请求被取消，不显示错误提示
        if (axios.isCancel && axios.isCancel(error)) {
            logger.debug('请求被取消:', error.message);
            return Promise.reject(error);
        }
        // 兼容性处理：检查错误是否为取消错误
        if (error.message === 'canceled' || 
            error.message === 'Request aborted' ||
            error.__CANCEL__ === true ||
            (error.code && error.code === 'ERR_CANCELED')) {
            logger.debug('请求被取消:', error.message || 'Unknown');
            return Promise.reject(error);
        }
        
        // 防止重复处理错误
        if (messageShown) {
            return Promise.reject(error);
        }
        
        let status = null;
        
        if (error.response) {
            // 有响应，处理HTTP错误状态码
            status = error.response.status;
            
            // 尝试获取后端返回的错误信息
            let errorMsg = null;
            if (error.response.data) {
                const data = error.response.data;
                if (typeof data === 'object') {
                    errorMsg = data.msg || data.message || data.error || data.errorMessage;
                } else if (typeof data === 'string') {
                    try {
                        const parsed = JSON.parse(data);
                        errorMsg = parsed.msg || parsed.message || parsed.error;
                    } catch (e) {
                        // 不是JSON，忽略
                    }
                }
            }
            
            // 检查是否未被标记过（新状态码会自动添加到errorFlags）
            if (!errorFlags.hasOwnProperty(status)) {
                errorFlags[status] = false;
            }
            
            if (!errorFlags[status]) {
                setErrorFlag(status);
                messageShown = true;
                
                // 清除用户信息（仅在401时）
                if (status === 401) {
                    try {
                        localStorage.removeItem('backUser');
                        localStorage.removeItem('userMenuList');
                        localStorage.removeItem('frontUser');
                        router.push('/login');
                    } catch (e) {
                        logger.error('Error during logout:', e);
                    }
                }
                handleErrorResponse(status, errorMsg);
            }
        } else if (!error.response && (error.code === 'ECONNABORTED' || 
                                       error.message?.includes('timeout') ||
                                       error.message?.includes('Timeout'))) {
            // 请求超时 - 只有在没有响应的情况下才判断为超时
            // 计算实际耗时
            const duration = error.config?.metadata?.startTime ? 
                Date.now() - error.config.metadata.startTime : null;
            const timeout = error.config?.timeout || 25000;
            
            // 检查是否是静默请求（不显示错误消息的请求）
            const isSilentRequest = error.config?.silent === true || error.config?.silentError === true;
            
            // 记录详细信息（静默请求使用debug级别）
            if (isSilentRequest) {
                logger.debug('请求超时（静默处理）:', {
                    url: error.config?.url,
                    method: error.config?.method,
                    timeout: `${timeout}ms`,
                    actualDuration: duration ? `${(duration / 1000).toFixed(2)}秒` : '未知',
                    code: error.code
                });
            } else {
                logger.warn('请求超时:', {
                    url: error.config?.url,
                    method: error.config?.method,
                    timeout: `${timeout}ms`,
                    actualDuration: duration ? `${(duration / 1000).toFixed(2)}秒` : '未知',
                    code: error.code,
                    message: error.message
                });
            }
            
            // 只有当实际耗时接近或超过超时时间时才显示错误（且不是静默请求）
            // 这样可以避免误判（例如请求被取消但仍然触发了超时检测）
            // 放宽条件到 80%，因为有些请求可能刚好在超时前完成但被标记为超时
            const shouldShowTimeout = !duration || duration >= timeout * 0.8;
            
            if (!isSilentRequest && !errorFlags[408] && shouldShowTimeout) {
                // 记录详细的超时信息，包括请求URL
                const requestUrl = error.config?.url || '未知请求';
                logger.error(`请求超时: ${requestUrl}`, {
                    url: requestUrl,
                    method: error.config?.method?.toUpperCase() || 'GET',
                    timeout: `${timeout}ms`,
                    actualDuration: duration ? `${(duration / 1000).toFixed(2)}秒` : '未知',
                    code: error.code
                });
                
                setErrorFlag(408);
                messageShown = true;
                handleErrorResponse(408);
            } else if (isSilentRequest) {
                // 静默请求，只记录debug日志，不显示错误消息
                logger.debug('静默请求超时，不显示错误消息:', {
                    url: error.config?.url,
                    duration: duration ? `${(duration / 1000).toFixed(2)}秒` : '未知'
                });
            } else if (duration && duration < timeout * 0.8) {
                // 实际耗时未达到超时阈值，可能是其他原因导致的，记录但不显示
                logger.debug('请求未真正超时，可能是其他错误:', {
                    url: error.config?.url,
                    duration: `${(duration / 1000).toFixed(2)}秒`,
                    timeout: `${timeout}ms`
                });
            }
        } else if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
            // 网络错误（真正的网络连接问题）
            if (!errorFlags['network']) {
                setErrorFlag('network');
                messageShown = true;
                handleErrorResponse('network');
            }
        } else {
            // 没有 response 的错误（可能是网络问题、CORS、后端未启动等）
            if (!messageShown && !errorFlags['network']) {
                // 记录详细错误信息
                logger.error('请求错误（无响应）:', {
                    message: error.message,
                    code: error.code,
                    name: error.name,
                    url: error.config?.url,
                    method: error.config?.method,
                    stack: error.stack
                });
                
                // 判断是否需要显示错误提示
                let shouldShowError = false;
                let errorType = 'network';
                
                // 检查错误代码和消息
                if (error.code === 'ECONNREFUSED' || 
                    error.code === 'ERR_CONNECTION_REFUSED' ||
                    error.message?.includes('Failed to fetch') ||
                    error.message?.includes('NetworkError') ||
                    error.message?.includes('Network request failed')) {
                    shouldShowError = true;
                    errorType = 'network';
                } else if (error.code === 'ERR_INTERNET_DISCONNECTED') {
                    shouldShowError = true;
                    errorType = 'network';
                } else if (error.code && error.code.startsWith('ERR_')) {
                    // 其他 ERR_ 开头的错误，可能是网络相关
                    shouldShowError = true;
                    errorType = 'network';
                } else if (!error.code && !error.message) {
                    // 完全未知的错误，可能是响应处理过程中的问题
                    shouldShowError = true;
                    errorType = 'unknown';
                }
                
                if (shouldShowError) {
                    setErrorFlag(errorType);
                    messageShown = true;
                    handleErrorResponse(errorType);
                }
            }
        }
        
        return Promise.reject(error);
    }
);

function handleErrorResponse(status, errorMsg = null) {
    // 统一状态码类型：如果是数字字符串，转换为数字；如果是特殊字符串（如 'network', 'unknown'），保持原样
    let normalizedStatus = status;
    if (typeof status === 'string' && !isNaN(status) && status.trim() !== '') {
        // 数字字符串，转换为数字
        normalizedStatus = parseInt(status, 10);
    }
    
    // 如果后端返回了错误信息，优先使用后端的错误信息
    if (errorMsg) {
        try {
            ElMessage.error({
                message: errorMsg,
                duration: 3000,
                showClose: true,
                onClose: () => {
                    messageShown = false;
                }
            });
            return;
        } catch (e) {
            logger.error('Error showing message:', e);
        }
    }
    
    let message = '';
    switch (normalizedStatus) {
        case 400:
            message = '请求参数错误，请检查后重试！';
            break;
        case 401:
            message = '登录失效，请重新登录！';
            break;
        case 403:
            message = '您没有权限访问该资源！';
            break;
        case 404:
            message = '请求的资源不存在！';
            break;
        case 405:
            message = '请求方法不被允许！';
            break;
        case 408:
            // 超时错误可以提供更多信息
            message = '请求超时，请检查您的网络连接或稍后重试！';
            break;
        case 409:
            message = '数据冲突，请检查后重试！';
            break;
        case 422:
            message = '请求参数验证失败，请检查后重试！';
            break;
        case 429:
            message = '请求过于频繁，请稍后再试！';
            break;
        case 500:
            message = '服务器内部错误，请稍后再试！';
            break;
        case 502:
            message = '网关错误，服务器暂时无法访问！';
            break;
        case 503:
            message = '服务不可用，服务器暂时过载或维护中！';
            break;
        case 504:
            message = '网关超时，请稍后再试！';
            break;
        case 'network':
            message = '网络连接错误，请检查网络设置！';
            break;
        case 'unknown':
            message = '请求发生错误，请稍后再试！';
            break;
        default:
            // 对于未知状态码，记录详细信息但显示通用提示
            if (typeof normalizedStatus === 'number') {
                logger.warn(`未处理的HTTP状态码: ${normalizedStatus}`);
                message = `请求失败（${normalizedStatus}），请稍后再试！`;
            } else {
                logger.warn(`未知错误类型: ${normalizedStatus}`, typeof normalizedStatus);
                message = '请求发生错误，请稍后再试！';
            }
            break;
    }
    
    if (message) {
        try {
            ElMessage.error({
                message: message,
                duration: 3000,
                showClose: true,
                onClose: () => {
                    // 消息关闭后重置消息显示标记
                    messageShown = false;
                }
            });
        } catch (e) {
            logger.error('Error showing message:', e);
            // 如果显示消息出错，确保重置标记
            messageShown = false;
        }
    }
}

// 重置方法
request.resetAuth = () => {
    resetErrorFlags();
    messageShown = false;
};

export default request

