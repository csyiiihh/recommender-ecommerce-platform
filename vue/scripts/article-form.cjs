async function localFiles(r){const path=require('path'),fs=require('fs');const root=path.resolve(__dirname,'../dist');let f=path.resolve(root,'.'+new URL(r.request().url()).pathname);if(f!==root&&!f.startsWith(root+path.sep))return r.abort();if(!fs.existsSync(f)||fs.statSync(f).isDirectory())f=path.join(root,'index.html');return r.fulfill({contentType:({'.js':'application/javascript','.css':'text/css','.html':'text/html','.png':'image/png','.woff2':'font/woff2','.ttf':'font/ttf'})[path.extname(f)]||'application/octet-stream',body:fs.readFileSync(f)})}
const {chromium}=require('playwright');
const fs=require('fs'),assert=require('assert/strict');
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j5xkAAAAASUVORK5CYII=','base64');
(async()=>{require('fs').mkdirSync(require('path').resolve(__dirname,'../test-results'),{recursive:true});
 const browser=await chromium.launch({executablePath:process.env.CHROME_BINARY || undefined,headless:true});
 const ctx=await browser.newContext({viewport:{width:1500,height:1000}}),result={fixtureOnly:true,scenarios:[],runtimeErrors:[]};
 const user={id:42,role:'ADMIN',username:'fixture',name:'测试管理员',token:'fixture'};
 await ctx.addInitScript(u=>{localStorage.setItem('backUser',JSON.stringify(u));localStorage.setItem('userMenuList',JSON.stringify([{id:1,name:'资讯管理',path:'/article-manager',pagePath:'ArticleManager',icon:'fa fa-book',children:[]}]))},user);
 const rows=[{id:1,title:'测试资讯一',summary:'测试摘要',content:'<p>仅用于隔离验证</p>',coverImage:'/img/shared.png',status:1,viewCount:3},{id:2,title:'测试资讯二',summary:'共用封面',content:'<p>示例</p>',coverImage:'/img/shared.png',status:1,viewCount:2}];
 let failSave=true,failDelete=true,saves=0,deletes=0;const calls=[];
 await ctx.route('http://127.0.0.1:4175/**',localFiles);await ctx.route('**/api/**',async r=>{
  const q=r.request(),p=new URL(q.url()).pathname,method=q.method();calls.push({p,method});
  const send=(data,code='0',status=200)=>r.fulfill({status,contentType:'application/json',body:JSON.stringify({code,data,msg:code==='0'?'fixture':'模拟数据库写入失败'})});
  if(p.startsWith('/api/img/'))return r.fulfill({contentType:'image/png',body:png});
  if(p.includes('/user/'))return send(user);
  if(p==='/api/article/manage/page')return send({records:rows,total:rows.length});
  if(p==='/api/file/upload/img'){assert.equal(method,'POST');return send('/img/new.png');}
  if(p==='/api/article/1'&&method==='PUT'){saves++;await new Promise(resolve=>setTimeout(resolve,250));const patch=q.postDataJSON();assert.equal(patch.coverImage,'/img/new.png');assert.equal(patch.title,'换封面测试');if(failSave)return send(null,'500',500);Object.assign(rows[0],patch);return send(patch);}
  if(p==='/api/article/1'&&method==='DELETE'){deletes++;if(failDelete)return send(null,'500',500);rows.splice(0,1);return send(null);}
  throw new Error('Unexpected API: '+method+' '+p);
 });
 const page=await ctx.newPage();page.on('pageerror',e=>result.runtimeErrors.push(e.message));
 const row=id=>page.locator('.el-table__row').filter({has:page.locator('td .cell').filter({hasText:new RegExp('^'+id+'$')})});
 try{
  await page.goto('http://127.0.0.1:4175/article-manager',{waitUntil:'networkidle'});await page.locator('.article-manager').waitFor();assert.equal(await page.locator('.el-table__row').count(),2);
  await row(1).getByRole('button',{name:'编辑'}).click();const dialog=page.getByRole('dialog',{name:'编辑资讯'});await dialog.getByPlaceholder('请输入资讯标题').fill('换封面测试');
  await dialog.locator('input[type=file]').setInputFiles({name:'new.png',mimeType:'image/png',buffer:png});await page.getByText('图片上传成功',{exact:true}).waitFor();
  await dialog.getByRole('button',{name:'确 定',exact:true}).evaluate(el=>{el.click();el.click()});await page.locator('.el-message--error').first().waitFor();assert(await dialog.isVisible());assert.equal(await page.locator('.el-message--error').count(),1);assert.equal(await dialog.getByPlaceholder('请输入资讯标题').inputValue(),'换封面测试');assert((await row(1).locator('.el-image img').getAttribute('src')).endsWith('/shared.png'));
  result.scenarios.push('failed save preserves editor input and existing list cover');
  failSave=false;await dialog.getByRole('button',{name:'确 定',exact:true}).evaluate(el=>{el.click();el.click()});await dialog.waitFor({state:'hidden'});await page.getByText('更新成功',{exact:true}).waitFor();assert.equal(saves,2);await page.waitForFunction(()=>document.querySelector('.el-table__row img')?.getAttribute('src')?.endsWith('/new.png'));
  result.scenarios.push('retry saves new cover and refreshes list');
  async function remove(){await row(1).getByRole('button',{name:'删除'}).click();await page.locator('.el-message-box__btns button.el-button--primary').click();}
  await remove();await page.getByText('删除失败',{exact:true}).waitFor();assert.equal(await page.locator('.el-table__row').count(),2);failDelete=false;await remove();await page.getByText('删除成功',{exact:true}).waitFor();await row(1).waitFor({state:'detached'});assert.equal(deletes,2);assert(await row(2).locator('.el-image img').evaluate(i=>i.complete&&i.naturalWidth>0));
  result.scenarios.push('failed deletion retains record; successful retry preserves other article cover');
  assert.equal(calls.filter(x=>x.p.includes('/file/remove')).length,0);assert.deepEqual(result.runtimeErrors,[]);await page.screenshot({path:'test-results/资讯图片隔离验证.png',fullPage:true});fs.writeFileSync('test-results/资讯图片浏览器验证.json',JSON.stringify(result,null,2));console.log('ARTICLE BROWSER PASSED',JSON.stringify(result));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
