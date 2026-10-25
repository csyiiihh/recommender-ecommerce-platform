async function localFiles(r){const path=require('path'),fs=require('fs');const root=path.resolve(__dirname,'../dist');let f=path.resolve(root,'.'+new URL(r.request().url()).pathname);if(f!==root&&!f.startsWith(root+path.sep))return r.abort();if(!fs.existsSync(f)||fs.statSync(f).isDirectory())f=path.join(root,'index.html');return r.fulfill({contentType:({'.js':'application/javascript','.css':'text/css','.html':'text/html','.png':'image/png','.woff2':'font/woff2','.ttf':'font/ttf'})[path.extname(f)]||'application/octet-stream',body:fs.readFileSync(f)})}
const {chromium}=require('playwright');
const fs=require('fs'),assert=require('assert/strict');
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j5xkAAAAASUVORK5CYII=','base64');
(async()=>{require('fs').mkdirSync(require('path').resolve(__dirname,'../test-results'),{recursive:true});
 const browser=await chromium.launch({executablePath:process.env.CHROME_BINARY || undefined,headless:true});
 const ctx=await browser.newContext({viewport:{width:1500,height:1000}}),result={fixtureOnly:true,scenarios:[],runtimeErrors:[]};
 const user={id:42,role:'MERCHANT',username:'fixture',name:'测试商家',token:'fixture'};
 await ctx.addInitScript(u=>{localStorage.setItem('backUser',JSON.stringify(u));localStorage.setItem('userMenuList',JSON.stringify([{id:1,name:'商品管理',path:'/product-manager',pagePath:'ProductManager',icon:'fa fa-book',children:[]}]))},user);
 const rows=[{id:1,name:'测试图书',description:'<p>可编辑的示例内容</p>',imageUrl:'/img/shared.png',categoryId:1,category:{id:1,name:'文学'},merchantId:42,price:39,stock:20,salesCount:5,status:1,isDiscount:0}];
 let failSave=true,saves=0,uploads=0;
 await ctx.route('http://127.0.0.1:4175/**',localFiles);await ctx.route('**/api/**',async r=>{
  const q=r.request(),p=new URL(q.url()).pathname;
  const send=(data,code='0',status=200)=>r.fulfill({status,contentType:'application/json',body:JSON.stringify({code,data,msg:code==='0'?'fixture':'模拟保存失败'})});
  if(p.startsWith('/api/img/'))return r.fulfill({contentType:'image/png',body:png});
  if(p.includes('/user/'))return send(user);
  if(p==='/api/category/all')return send([{id:1,name:'文学'}]);
  if(p==='/api/product/manage/page'){assert.equal(new URL(q.url()).searchParams.get('merchantId'),'42');return send({records:rows,total:1});}
  if(p==='/api/file/upload/img'){assert.equal(q.method(),'POST');uploads++;return send('/img/new.png');}
  if(p==='/api/product/1'&&q.method()==='PUT'){saves++;await new Promise(resolve=>setTimeout(resolve,250));const patch=q.postDataJSON();assert.equal(patch.imageUrl,'/img/new.png');assert.equal(patch.merchantId,42);assert.equal(patch.name,'清理后商品编辑');if(failSave)return send(null,'500',500);Object.assign(rows[0],patch);return send(patch);}
  throw new Error('Unexpected API: '+q.method()+' '+p);
 });
 const page=await ctx.newPage();page.on('pageerror',e=>result.runtimeErrors.push(e.message));
 try{
  await page.goto('http://127.0.0.1:4175/product-manager',{waitUntil:'networkidle'});await page.locator('.el-table__row').first().waitFor();assert.equal(await page.locator('.el-table__row').count(),1);result.scenarios.push('merchant management list remains scoped after dependency cleanup');
  await page.locator('.el-table__row').getByRole('button',{name:'编辑'}).click();const d=page.getByRole('dialog',{name:'编辑商品'});await d.waitFor();assert(await d.locator('.w-e-text-container').isVisible());assert.equal(await d.locator('img.avatar').getAttribute('src'),'/api/img/shared.png');assert.equal(await page.locator('.cropper-content,.vue-cropper').count(),0);result.scenarios.push('wangEditor and product edit dialog remain usable without disabled cropper');
  await d.getByPlaceholder('请输入商品名称').fill('清理后商品编辑');await d.locator('input[type=file]').setInputFiles({name:'new.png',mimeType:'image/png',buffer:png});await page.getByText('图片上传成功',{exact:true}).waitFor();assert.equal(uploads,1);result.scenarios.push('direct image upload remains functional without old upload components');
  await d.getByRole('button',{name:'确 定',exact:true}).evaluate(el=>{el.click();el.click()});await page.locator('.el-message--error').first().waitFor();assert(await d.isVisible());assert.equal(await page.locator('.el-message--error').count(),1);assert.equal(await d.getByPlaceholder('请输入商品名称').inputValue(),'清理后商品编辑');assert.equal(saves,1);failSave=false;await d.getByRole('button',{name:'确 定',exact:true}).evaluate(el=>{el.click();el.click()});await d.waitFor({state:'hidden'});await page.locator('.el-table__row').getByText('清理后商品编辑',{exact:true}).waitFor();assert.equal(saves,2);result.scenarios.push('failed save retains input and retry refreshes the updated product');
  assert.deepEqual(result.runtimeErrors,[]);await page.screenshot({path:'test-results/冗余清理-商品编辑验证.png',fullPage:true});fs.writeFileSync('test-results/冗余清理商品浏览器验证.json',JSON.stringify(result,null,2));console.log('PRODUCT CLEANUP BROWSER PASSED',JSON.stringify(result));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
