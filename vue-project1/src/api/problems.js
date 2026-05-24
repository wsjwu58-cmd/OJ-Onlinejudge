import request from '../utils/request'

// 获取题目列表（分页查询）- 用户端
export const getProblemsApi = (params) => {
  return request.get('/user/problem/page', { params })
}

// 获取题目详情
export const getProblemDetailApi = (id) => {
  return request.get(`/user/problem/${id}`)
}

// 提交代码
export const submitCodeApi = (data) => {
  return request.post('/user/problem/submit', data)
}

// 运行代码（测试运行）
export const runCodeApi = (data) => {
  return request.post('/user/problem/run', data)
}

// 获取所有分类
export const getCategoriesApi = () => {
  return request.get('/user/problem/type/all')
}

export default {
  getProblemsApi,
  getProblemDetailApi,
  submitCodeApi,
  runCodeApi,
  getCategoriesApi
}
