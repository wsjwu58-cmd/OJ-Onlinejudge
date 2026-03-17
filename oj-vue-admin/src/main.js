import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import Highlight from './utils/highLight'
// import './assets/main.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.use(Highlight)
app.mount('#app')
