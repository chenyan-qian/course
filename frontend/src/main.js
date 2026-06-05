import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// axios 全局配置
axios.defaults.baseURL = 'http://localhost:8080'

// 请求拦截器：自动带 token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

// 响应拦截器：401 时跳回登录页
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

// 挂载到全局，方便组件里直接用 this.$axios
app.config.globalProperties.$axios = axios

app.use(ElementPlus)
app.use(router)
app.mount('#app')
