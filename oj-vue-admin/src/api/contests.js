import request from '../util/request'

// 新增比赛
export const addContestApi = (data) => {
  return request.post('/admin/Contest', data)
}

// 分页查询比赛
export const pageContestApi = (params) => {
  return request.get('/admin/Contest/page', { params })
}

// 根据ID查询比赛
export const getContestByIdApi = (id) => {
  return request.get(`/admin/Contest/${id}`)
}

// 编辑比赛
export const updateContestApi = (data) => {
  return request.put('/admin/Contest', data)
}

// 删除比赛
export const deleteContestApi = (id) => {
  return request.delete('/admin/Contest', { params: { id } })
}

export default {
  addContestApi,
  pageContestApi,
  getContestByIdApi,
  updateContestApi,
  deleteContestApi
}
