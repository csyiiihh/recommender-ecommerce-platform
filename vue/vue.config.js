const { defineConfig } = require("@vue/cli-service");
module.exports = defineConfig({
  productionSourceMap: false,
  transpileDependencies: true,
  lintOnSave: true, //关闭语法检查
  // Load optional admin dependencies only when their routes are opened.
  chainWebpack: config => { config.plugins.delete('prefetch'); },
  configureWebpack: {
    optimization: {
      splitChunks: {
        cacheGroups: {
          charts: { test: /[\\/]node_modules[\\/](echarts|zrender)[\\/]/, name: 'charts', chunks: 'all', priority: 30 },
          spreadsheet: { test: /[\\/]node_modules[\\/](xlsx|file-saver)[\\/]/, name: 'spreadsheet', chunks: 'all', priority: 30 },
          editor: { test: /[\\/]node_modules[\\/](@wangeditor|slate|slate-history|slate-hyperscript|snabbdom)[\\/]/, name: 'editor', chunks: 'all', priority: 30 }
        }
      }
    }
  },
  devServer: {  
    client: {
      overlay: false
    },              //设置本地默认端口  选填
    port: 8080,
    proxy: {                 //设置代理，必须填
      '/alipay': { target: 'http://localhost:1234', changeOrigin: true },
      '/api': {              //设置拦截器  拦截器格式   斜杠+拦截器名字，名字可以自己定
        target: 'http://localhost:1234',     //代理的目标地址
        changeOrigin: true,              //是否设置同源，输入是的
      }
    }
 
  },
});