// Upload responses use /img paths; existing records can contain names or full URLs.
export function buildImageUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return ''
  const url = value.trim()
  if (/^https?:\/\//i.test(url)) return url
  const local = url.replace(/\/{2,}/g, '/')
  if (local.startsWith('/api/')) return local
  if (local.startsWith('/img/')) return `/api${local}`
  if (local.startsWith('img/')) return `/api/${local}`
  return `/api/img/${local.replace(/^\/+/, '')}`
}
