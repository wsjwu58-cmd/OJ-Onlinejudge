import request from '../utils/request'

// 用户登录
export const loginApi = (data) => {
  return request.post('/user/userLogin/login', data)
}

// 用户注册
export const registerApi = (data) => {
  return request.post('/user/userLogin/register', data)
}

// 获取图形验证码
export const getCaptchaApi = (data) => {
  return request.post('/user/userLogin/get-captcha', data)
}

// 获取用户信息
export const getUserInfoApi = () => {
  return request.get('/user/userInfo')
}

// 更新用户信息
export const updateUserInfoApi = (data) => {
  return request.put('/user/userInfo', data)
}

export default {
  loginApi,
  registerApi,
  getCaptchaApi,
  getUserInfoApi,
  updateUserInfoApi
}
