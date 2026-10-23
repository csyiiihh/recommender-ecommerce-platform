// Request keys are not authentication credentials. Only fingerprints and keys are persisted.
const memory = new Map()
const storageName = (scope, buyerId) => `checkout-request:v1:${scope}:${buyerId}`
function read(name) {
  if (memory.has(name)) return memory.get(name)
  try {
    const saved = JSON.parse(sessionStorage.getItem(name) || '[]')
    const rows = Array.isArray(saved) ? saved.filter(row => typeof row.fingerprint === 'string' && /^[a-f0-9]{64}$/.test(row.fingerprint) && typeof row.key === 'string' && /^[A-Za-z0-9_-]{16,128}$/.test(row.key)).slice(0, 20) : []
    memory.set(name, rows)
    return rows
  } catch { memory.set(name, []); return memory.get(name) }
}
function write(name, rows) {
  memory.set(name, rows)
  try { sessionStorage.setItem(name, JSON.stringify(rows.filter(row => /^[a-f0-9]{64}$/.test(row.fingerprint)))) } catch { /* In-memory retries still work if storage is unavailable. */ }
}
export async function checkoutRequestKey(scope, buyerId, intent) {
  if (!['single', 'cart'].includes(scope) || !buyerId) throw new Error('请重新登录后提交订单')
  const serialized = JSON.stringify(intent)
  let fingerprint
  try {
    const digest = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(serialized))
    fingerprint = Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('')
  } catch { fingerprint = `memory:${serialized}` } // Never persist plaintext delivery information.
  const name = storageName(scope, buyerId)
  const rows = read(name)
  const existing = rows.find(row => row.fingerprint === fingerprint)
  if (existing) return existing.key
  const bytes = window.crypto.getRandomValues(new Uint8Array(16))
  const key = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  write(name, [{ fingerprint, key }, ...rows].slice(0, 20))
  return key
}
export function forgetCheckoutRequest(scope, buyerId, key) {
  const name = storageName(scope, buyerId)
  write(name, read(name).filter(row => row.key !== key))
}
