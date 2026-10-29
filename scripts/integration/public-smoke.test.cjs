const { test } = require('node:test');
const assert = require('node:assert/strict');
const { runPublicSmoke } = require('./public-smoke.cjs');
const base = 'http://127.0.0.1:13334';
const result = rows => ({ ok:true, json:async () => ({ code:'0', data:{ records:rows } }) });
const log = () => {};

test('rejects missing, business and deceptive hosts before any request', async () => {
  for (const value of [null, 'http://localhost:13334', 'http://127.0.0.1:1234', 'https://business.invalid', base + '@business.invalid', base + '/']) {
    let called = false;
    assert.equal(await runPublicSmoke({base:value,fetchImpl:async()=>{called=true;throw Error('must not fetch')},log}),2);
    assert.equal(called,false);
  }
});
test('requires both public endpoints and sends only GET without redirects', async () => {
  const calls = [];
  const code = await runPublicSmoke({base,log,fetchImpl:async(url,options)=>{
    calls.push(url); assert.equal(options.method,'GET'); assert.equal(options.redirect,'error');
    assert(options.signal instanceof AbortSignal); assert.equal(options.body,undefined);
    return result([{id:1,status:1}]);
  }});
  assert.equal(code,0);
  assert.deepEqual(calls,[base+'/api/product/page?size=5',base+'/api/article/page?status=0&size=5']);
});
for (const emptyAt of [0,1]) test('missing sample in endpoint '+emptyAt+' stays unverified',async()=>{
  let call=0;
  assert.equal(await runPublicSmoke({base,log,fetchImpl:async()=>result(call++===emptyAt?[]:[{status:1}])}),2);
});
test('all-empty database cannot pass',async()=>{
  assert.equal(await runPublicSmoke({base,log,fetchImpl:async()=>result([])}),2);
});
test('unpublished and null records fail',async()=>{
  for(const row of [{status:0},{status:2},null]) assert.equal(await runPublicSmoke({base,log,fetchImpl:async()=>result([row])}),1);
});
test('HTTP failures and malformed envelopes fail',async()=>{
  for(const response of [{ok:false},{ok:true,json:async()=>({code:'500'})},{ok:true,json:async()=>({code:'0',data:{records:{}}})}]) {
    assert.equal(await runPublicSmoke({base,log,fetchImpl:async()=>response}),1);
  }
});
test('redirect or network rejection fails without a second request',async()=>{
  let calls=0;
  assert.equal(await runPublicSmoke({base,log,fetchImpl:async()=>{calls++;throw Error('redirect blocked')}}),1);
  assert.equal(calls,1);
});

test('CLI refuses an unset integration address before accessing a service', () => {
  const {spawnSync} = require('node:child_process');
  const path = require('node:path');
  const child = spawnSync(process.execPath,[path.join(__dirname,'public-smoke.cjs')],{
    env:{...process.env,INTEGRATION_API_BASE:''},encoding:'utf8'
  });
  assert.equal(child.status,2); assert(child.stdout.includes('13334'));
});
