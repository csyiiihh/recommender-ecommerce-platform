const { chromium } = require('playwright');
const assert = require('assert/strict');
const fs = require('fs');
const path = require('path');
const origin = 'http://127.0.0.1:4175';
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j5xkAAAAASUVORK5CYII=','base64');
const user = {id:42, role:'USER', username:'fixture', token:'fixture'};
async function staticFiles(route) {
  const root = path.resolve(__dirname, '../dist');
  let file = path.resolve(root, '.' + new URL(route.request().url()).pathname);
  if (file !== root && !file.startsWith(root + path.sep)) return route.abort();
  if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) file = path.join(root, 'index.html');
  return route.fulfill({contentType:({'.html':'text/html','.js':'application/javascript','.css':'text/css','.ttf':'font/ttf','.woff2':'font/woff2'})[path.extname(file)] || 'application/octet-stream', body:fs.readFileSync(file)});
}
const ok = (route,data) => route.fulfill({contentType:'application/json',body:JSON.stringify({code:'0',data})});
(async () => {
  fs.mkdirSync('test-results',{recursive:true});
  const browser = await chromium.launch({executablePath:process.env.CHROME_BINARY || undefined,headless:true});
  const result = {fixtureOnly:true,scenarios:[],runtimeErrors:[]};
  async function context() {
    const ctx = await browser.newContext({viewport:{width:1440,height:1000}});
    await ctx.addInitScript(u => {localStorage.setItem('frontUser',JSON.stringify(u));},user);
    await ctx.route(origin + '/**',staticFiles);
    await ctx.route('**/api/**',route => {
      const p = new URL(route.request().url()).pathname;
      if (p.includes('/user/')) return ok(route,user);
      if (p.startsWith('/api/img/')) return route.fulfill({contentType:'image/png',body:png});
      if (p.includes('/page')) return ok(route,{records:[],total:0});
      return ok(route,[]);
    });
    const page = await ctx.newPage();
    page.on('pageerror',error=>result.runtimeErrors.push(error.message));
    return {ctx,page};
  }
  try {
    {
      const {ctx,page} = await context();
      const content = '<p><strong>正常公告排版</strong></p><img src="/broken-fixture.png" onerror="window.__noticeInjected=1"><a href="javascript:window.__noticeInjected=2">链接</a>';
      await ctx.route('**/api/notice/page?**',route=>ok(route,{records:[{id:1,title:'安全公告验证',content,time:'2035-01-01 10:00:00'}],total:1}));
      await page.goto(origin+'/',{waitUntil:'networkidle'});
      await page.locator('.service-notices > summary').click();
      await page.locator('.notice-title:visible').getByText('安全公告验证',{exact:true}).first().click();
      const detail = page.locator('.notice-detail-content');
      await detail.getByText('正常公告排版',{exact:true}).waitFor();
      const html = await detail.innerHTML();
      assert(!html.includes('onerror')); assert(!html.includes('javascript:')); assert(html.includes('<strong>'));
      assert.equal(await page.evaluate(()=>window.__noticeInjected),undefined);
      result.scenarios.push('announcement strips executable HTML and retains normal formatting');
      await ctx.close();
    }
    {
      const {ctx,page} = await context(); const queries = [];
      await ctx.route('**/api/article/page?**',route=>{
        const q = new URL(route.request().url()).searchParams; queries.push(Object.fromEntries(q));
        return ok(route,{records:[{id:1,title:'资讯排序验证',content:'<p>正文</p>',viewCount:5,createdAt:'2035-01-01 10:00:00'}],total:30});
      });
      await page.goto(origin+'/articles',{waitUntil:'networkidle'});
      await Promise.all([
        page.waitForResponse(r=>r.url().includes('/article/page?') && new URL(r.url()).searchParams.get('currentPage')==='2'),
        page.locator('.el-pagination .number').getByText('2',{exact:true}).click()
      ]);
      await page.getByText('最新发布',{exact:true}).first().click();
      await Promise.all([
        page.waitForResponse(r=>r.url().includes('/article/page?') && new URL(r.url()).searchParams.get('sort')==='popular'),
        page.getByRole('menuitem',{name:'热门阅读'}).click()
      ]);
      assert.equal(queries.at(-1).currentPage,'1'); assert.equal(queries.at(-1).sort,'popular');
      await page.getByText('本页阅读',{exact:true}).waitFor();
      result.scenarios.push('article sort goes to backend and resets pagination; reads labeled as current page');
      await ctx.close();
    }
    {
      const {ctx,page} = await context(); const posts = [];
      const activity = {id:2,activityName:'请求重试验证',status:1,groupPrice:20,originalPrice:30,stock:100,requiredMembers:3,validityHours:24,startTime:'2020-01-01 10:00:00',endTime:'2035-01-01 10:00:00',product:{id:9,name:'测试图书',imageUrl:'/img/cover.png'}};
      await ctx.route('**/api/group-buy/activity/2',route=>ok(route,activity));
      await ctx.route('**/api/address/user/42',route=>ok(route,[{id:8,userId:42,receiver:'测试收件人',phone:'123',address:'隔离测试地址'}]));
      await ctx.route('**/api/group-buy/start',route=>{
        posts.push({key:route.request().headers()['idempotency-key'],body:route.request().postDataJSON()});
        if (posts.length===1) return route.abort('failed');
        return ok(route,{orderId:88,paymentAllowed:false,replayed:true});
      });
      await page.goto(origin+'/group-buy/2',{waitUntil:'networkidle'});
      async function submit() {
        await page.getByRole('button',{name:'我要开团'}).click();
        const dialog = page.getByRole('dialog');
        await dialog.locator('.address-card').first().waitFor();
        const sent = page.waitForRequest(r=>r.url().endsWith('/api/group-buy/start'));
        await dialog.getByRole('button',{name:'确定',exact:true}).click(); await sent;
        await page.waitForFunction(()=>!Array.from(document.querySelectorAll('.el-loading-mask')).some(el=>el.offsetParent!==null));
      }
      await submit(); await submit();
      await page.getByText('订单已处理，请在我的订单中查看',{exact:true}).waitFor();
      assert.equal(posts.length,2); assert.match(posts[0].key,/^[a-f0-9]{32}$/); assert.equal(posts[0].key,posts[1].key); assert.deepEqual(posts[0].body,posts[1].body);
      result.scenarios.push('group checkout reuses request key after transport failure and respects processed replay');
      await ctx.close();
    }
    assert.deepEqual(result.runtimeErrors,[]);
    fs.writeFileSync('test-results/public-safety.json',JSON.stringify(result,null,2));
    console.log('PUBLIC SAFETY PASSED',JSON.stringify(result));
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exit(1)});
