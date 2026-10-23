import { reactive } from 'vue'
import Request from './request'
import eventBus from './eventBus'

const cache = reactive({ userId: null, loaded: false, loadedAt: 0, ids: new Set(), pending: new Set() })
let request = null
let sequence = 0
const changes = new Map()
export function readFrontUser() {
  try { return JSON.parse(localStorage.getItem('frontUser') || '{}') } catch { return {} }
}
export function favoriteStatus(userId, productId) {
  if (!userId) return false
  if (String(userId) !== cache.userId || !cache.loaded) return undefined
  return cache.ids.has(String(productId))
}
function commit(userId, productId, active) {
  if (String(userId) !== cache.userId) return
  const key = String(productId)
  if (active) cache.ids.add(key)
  else cache.ids.delete(key)
  changes.set(key, { version: ++sequence, active })
}
export async function loadFavoriteStates(userId) {
  if (!userId) return
  const user = String(userId)
  if (cache.userId !== user) {
    cache.userId = user; cache.loaded = false; cache.loadedAt = 0; cache.ids = new Set(); changes.clear()
  }
  if (cache.loaded && Date.now() - cache.loadedAt < 30000) return
  if (request?.userId === user) return request.promise
  const version = sequence
  const token = Symbol('favorites')
  const promise = (async () => {
    try {
      const result = await Request.get(`/favorite/user/${userId}/statuses`, { silent: true, timeout: 10000 })
      if (result.code !== '0' || !Array.isArray(result.data)) throw new Error(result.msg || '收藏状态读取失败')
      if (cache.userId !== user || request?.token !== token) return
      const ids = new Set(result.data.map(String))
      // Do not let an older list response undo a newer successful toggle.
      for (const [key, change] of changes) if (change.version > version) {
        if (change.active) ids.add(key)
        else ids.delete(key)
      }
      cache.ids = ids; cache.loaded = true; cache.loadedAt = Date.now()
    } finally {
      if (request?.token === token) request = null
    }
  })()
  request = { userId: user, token, promise }
  return promise
}
export async function toggleFavorite(userId, productId) {
  const key = `${userId}:${productId}`
  if (cache.pending.has(key)) return null
  cache.pending.add(key)
  try {
    await loadFavoriteStates(userId)
    const current = favoriteStatus(userId, productId)
    if (current === undefined) throw new Error('收藏状态尚未确认，请稍后重试')
    // The existing POST API receives the previous state and returns the new state.
    const result = await Request.post('/favorite', { userId, productId, status: current ? 1 : 0 })
    if (result.code !== '0' || ![0, 1].includes(Number(result.data?.status))) throw new Error(result.msg || '收藏操作失败')
    const status = Number(result.data.status)
    commit(userId, productId, status === 1)
    eventBus.emit('favoriteStatusChanged', { userId, productId, status: status === 1 })
    return status
  } finally { cache.pending.delete(key) }
}
eventBus.on('favoriteStatusChanged', data => {
  if (data?.productId == null) return
  const userId = data.userId ?? readFrontUser().id
  commit(userId, data.productId, data.status === true || Number(data.status) === 1)
})
