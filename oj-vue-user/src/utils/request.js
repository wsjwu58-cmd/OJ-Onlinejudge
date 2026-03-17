import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 600000,
  // 自定义 JSON 解析：把超过 JS 安全整数的数字转为字符串，防止精度丢失
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        // 将超过15位的纯数字值转为字符串（匹配 JSON 中 : 后面的长数字）
        data = data.replace(/:(\s*)(\d{16,})/g, ':"$2"')
        return JSON.parse(data)
      } catch (e) {
        return data
      }
    }
    return data
  }]
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const loginUserStr = localStorage.getItem('loginUser')
    let loginUser = null
    if (loginUserStr) {
      try {
        loginUser = JSON.parse(loginUserStr)
      } catch (e) {
        console.error('解析loginUser失败:', e)
      }
    }

    if (loginUser && loginUser.token) {
      config.headers.authorization = loginUser.token
    } else {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.authorization = token
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      ElMessage.error('请先登录')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
