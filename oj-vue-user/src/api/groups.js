import request from '../utils/request'

// 获取题单列表（分页查询）
export const getGroupsApi = (params) => {
  return request.get('/user/group/page', { params })
}

// 获取题单详情
export const getGroupDetailApi = (id) => {
  return request.get(`/user/group/${id}`)
}

// 获取题单中的题目列表
export const getGroupProblemsApi = (id, params) => {
  return request.get(`/user/group/${id}/problems`, { params })
}

export default {
  getGroupsApi,
  getGroupDetailApi,
  getGroupProblemsApi
}
