// Only read the dedicated integration listener; no real backend is needed for contract tests.
const ALLOWED_BASE = 'http://127.0.0.1:13334';
const ROUTES = ['/api/product/page?size=5', '/api/article/page?status=0&size=5'];

async function runPublicSmoke({ base = process.env.INTEGRATION_API_BASE, fetchImpl = globalThis.fetch, log = console.log } = {}) {
  if (base !== ALLOWED_BASE) {
    log('仅允许 INTEGRATION_API_BASE=' + ALLOWED_BASE);
    return 2;
  }
  try {
    const missing = [];
    for (const route of ROUTES) {
      const response = await fetchImpl(base + route, {
        method: 'GET', redirect: 'error', signal: AbortSignal.timeout(10000)
      });
      if (!response.ok) throw new Error('Invalid HTTP response');
      const result = await response.json();
      if (result.code !== '0' || !Array.isArray(result.data?.records)) throw new Error('Invalid response shape');
      if (result.data.records.some(row => !row || row.status !== 1)) throw new Error('Unpublished public record');
      if (result.data.records.length === 0) missing.push(route);
    }
    if (missing.length) {
      log('接口可访问，但商品或资讯缺少样本；尚不能判定只读样本检查通过。');
      return 2;
    }
    log('公开读取样本检查通过，交易、并发及支付尚未验证。');
    return 0;
  } catch {
    log('隔离环境读取检查失败，请核对服务、响应与样本；未执行写入。');
    return 1;
  }
}

if (require.main === module) runPublicSmoke().then(code => { process.exitCode = code; });
module.exports = { runPublicSmoke };
