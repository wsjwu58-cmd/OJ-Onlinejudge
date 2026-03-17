import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'

// Monaco Editor (worker + language services + theme)
import './utils/monaco'

// 代码高亮指令
import Highlight from './utils/highLight'

// 创建Pinia实例
const pinia = createPinia()

const app = createApp(App)

// 使用插件
app.use(router)
app.use(pinia)
app.use(ElementPlus)
app.use(Highlight)

app.mount('#app')
