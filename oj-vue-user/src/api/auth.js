import request from '../utils/request'

// 用户登录
export const loginApi = (data) => {
  return request.post('/user/login', data)
}

// 用户注册
export const registerApi = (data) => {
  return request.post('/user/register', data)
}

// 获取用户信息
export const getUserInfoApi = () => {
  return request.get('/user/info')
}

// 更新用户信息
export const updateUserInfoApi = (data) => {
  return request.put('/user/info', data)
}

export default {
  loginApi,
  registerApi,
  getUserInfoApi,
  updateUserInfoApi
}
