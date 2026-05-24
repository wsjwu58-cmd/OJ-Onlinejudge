import request from '../utils/request'

export const getUserProfileApi = () => {
  return request.get('/user/userInfo')
}

export const updateUserProfileApi = (data) => {
  return request.put('/user/userInfo', data)
}

export const userSignApi = () => {
  return request.get('/user/userInfo/sign')
}

export const getSignCountApi = () => {
  return request.get('/user/userInfo/sign/count')
}

export const getSignDaysApi = (year, month) => {
  return request.get('/user/userInfo/sign/days', { params: { year, month } })
}

export default {
  getUserProfileApi,
  updateUserProfileApi,
  userSignApi,
  getSignCountApi,
  getSignDaysApi
}
