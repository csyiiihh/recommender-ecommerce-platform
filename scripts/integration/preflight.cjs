const {spawnSync}=require('child_process');
const problems=[];
if(!process.env.INTEGRATION_MYSQL_PASSWORD)problems.push('缺少独立测试数据库密码 INTEGRATION_MYSQL_PASSWORD');
if((process.env.INTEGRATION_JWT_SECRET||'').length<32)problems.push('INTEGRATION_JWT_SECRET 至少需要 32 字符');
if(Object.keys(process.env).some(k=>(k.startsWith('ALIPAY_')||k.startsWith('SPRING_')||['JWT_SECRET','DB_PASSWORD','FILE_STORAGE_ROOT','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'].includes(k))&&process.env[k]))problems.push('请在无业务密钥与上传目录覆盖变量的新终端中执行');
const check=spawnSync('docker',['compose','version'],{encoding:'utf8'});
if(check.error||check.status!==0)problems.push('Docker Compose 尚不可用');
if(problems.length){console.log('联调准备未满足：\n- '+problems.join('\n- '));process.exit(2)}
console.log('环境前置检查通过。尚未启动服务、导入 SQL 或执行真实交易。');
