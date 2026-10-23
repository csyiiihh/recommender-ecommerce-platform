import Request from './request'

// Management selectors use the authorized endpoint and bounded pages, including offline books for stock adjustments.
export async function getProductOptions(filters = {}) {
  const products = []
  let currentPage = 1
  let pages = 1
  do {
    const result = await Request.get('/product/manage/page', { params: { ...filters, currentPage, size: 100 } })
    if (result.code !== '0') return result
    products.push(...(result.data?.records || []))
    pages = result.data?.pages || Math.ceil((result.data?.total || 0) / 100) || 1
    currentPage += 1
  } while (currentPage <= pages)
  return { code: '0', data: products }
}
