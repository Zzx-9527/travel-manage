import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'font-awesome/css/font-awesome.min.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import './assets/css/main.css'

const app = createApp(App)

app.use(createPinia())
// Element 图标不全局注册（300+ 组件全量注册增加启动开销）；
// 需要时在组件内显式 import，如 ScenicManagement 的 Check/Close
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
