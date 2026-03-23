import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, ElNotification } from "element-plus";
import '../src/assets/init.css'
import '../src/assets/fonts/fonts.css'
import '../src/assets/iconfont.css'
import '../src/assets/styles/design-system.css'
import '../src/assets/styles/chinese-theme.css'
import '../src/assets/styles/chinese-patterns.css'

const app = createApp(App);

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(ElementPlus);
app.use(router);
app.use(store);

// 全局属性
app.config.globalProperties.HOST = "/api";
// 为了兼容 Vue 2 的写法，添加全局方法
app.config.globalProperties.$message = ElMessage;
app.config.globalProperties.$confirm = ElMessageBox.confirm;
app.config.globalProperties.$notify = ElNotification;
app.config.globalProperties.$alert = ElMessageBox.alert;
app.config.globalProperties.$prompt = ElMessageBox.prompt;

app.mount("#app");
