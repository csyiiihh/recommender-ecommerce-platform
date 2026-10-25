// Read-only smoke check; only the dedicated integration listener is permitted.
const base=process.env.INTEGRATION_API_BASE;
if(base!=='http://127.0.0.1:13334'){console.error('仅允许 INTEGRATION_API_BASE=http://127.0.0.1:13334');process.exit(2)}
(async()=>{let samples=0;for(const url of ['/api/product/page?size=5','/api/article/page?status=0&size=5']){
const response=await fetch(base+url,{signal:AbortSignal.timeout(10000)});if(!response.ok)throw Error('接口状态异常 '+response.status);
const result=await response.json();if(result.code!=='0'||!Array.isArray(result.data?.records))throw Error('接口结构异常');
for(const row of result.data.records)if(row.status!==1)throw Error('公开接口包含未上架记录');samples+=result.data.records.length;
}if(!samples){console.log('接口可访问，但缺少样本；尚不能判定业务联调通过。');process.exitCode=2}else console.log('公开读取检查通过，交易、并发及支付尚未验证。')})().catch(()=>{console.error('隔离环境检查失败，请核对服务与样本；未执行写入。');process.exitCode=1});
