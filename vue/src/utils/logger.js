/**
 * 前端日志工具
 * 替代 console.log，支持环境控制和生产环境自动移除
 */

const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
  NONE: 4
}

// 根据环境变量设置日志级别
const getLogLevel = () => {
  const env = process.env.NODE_ENV
  const logLevel = process.env.VUE_APP_LOG_LEVEL || 'INFO'
  
  // 生产环境默认只显示 WARN 和 ERROR
  if (env === 'production') {
    return LOG_LEVELS[logLevel] || LOG_LEVELS.WARN
  }
  
  // 开发环境默认显示所有日志
  return LOG_LEVELS[logLevel] || LOG_LEVELS.DEBUG
}

const currentLogLevel = getLogLevel()

/**
 * 格式化日志消息
 */
const formatMessage = (level, message, ...args) => {
  const timestamp = new Date().toISOString()
  const prefix = `[${timestamp}] [${level}]`
  
  if (args.length > 0) {
    return [prefix, message, ...args]
  }
  return [prefix, message]
}

/**
 * 日志工具类
 */
const logger = {
  /**
   * Debug 级别日志（开发环境）
   */
  debug(message, ...args) {
    if (currentLogLevel <= LOG_LEVELS.DEBUG) {
      console.debug(...formatMessage('DEBUG', message, ...args))
    }
  },

  /**
   * Info 级别日志
   */
  info(message, ...args) {
    if (currentLogLevel <= LOG_LEVELS.INFO) {
      console.info(...formatMessage('INFO', message, ...args))
    }
  },

  /**
   * Warn 级别日志
   */
  warn(message, ...args) {
    if (currentLogLevel <= LOG_LEVELS.WARN) {
      console.warn(...formatMessage('WARN', message, ...args))
    }
  },

  /**
   * Error 级别日志
   */
  error(message, ...args) {
    if (currentLogLevel <= LOG_LEVELS.ERROR) {
      console.error(...formatMessage('ERROR', message, ...args))
    }
  },

  /**
   * 分组日志（开发环境）
   */
  group(label) {
    if (currentLogLevel <= LOG_LEVELS.DEBUG) {
      console.group(label)
    }
  },

  groupEnd() {
    if (currentLogLevel <= LOG_LEVELS.DEBUG) {
      console.groupEnd()
    }
  },

  /**
   * 表格日志（开发环境）
   */
  table(data) {
    if (currentLogLevel <= LOG_LEVELS.DEBUG) {
      console.table(data)
    }
  }
}

export default logger



































