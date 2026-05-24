import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
//创建axios实例对象
const request = axios.create({
  baseURL: '/api',
  timeout: 600000
})
//axios的请求 request 拦截器,获取localStorage中的token,请求头中添加token
request.interceptors.request.use(
  (config) => { //config: 配置对象
    //获取token,将json格式字符串转为对象
    const loginUserStr = localStorage.getItem('loginUser')
    console.log('loginUser字符串:', loginUserStr)
    
    let loginUser = null
    if (loginUserStr) {
      try {
        loginUser = JSON.parse(loginUserStr)
        console.log('解析后的loginUser:', loginUser)
      } catch (e) {
        console.error('解析loginUser失败:', e)
      }
    }
    
    //判断token是否存在
    if (loginUser && loginUser.token) {
      //请求头添加token
      config.headers.token = loginUser.token;
      console.log('添加token到请求头:', loginUser.token)
    } else {
      //尝试直接从localStorage获取token
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.token = token;
        console.log('从localStorage直接获取token:', token)
      } else {
        console.log('未找到token')
      }
    }
    
    return config;
  },
  (error) => { //失败回调
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

//axios的响应 response 拦截器
request.interceptors.response.use(
  (response) => { //成功回调
    return response.data
  },
  (error) => { //失败回调
    //如果响应码为401,则跳转到登录页面
    if (error.response.status === 401) {
      //提示信息
      ElMessage.error('请先登录')
      //跳转登录界面
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request