import request from '../util/request'

// 新增题单
export const saveGroupApi = (data) => {
  return request.post('/admin/group', data)
}

// 分页查询题单
export const pageGroupApi = (params) => {
  return request.get('/admin/group/page', { params })
}

// 根据ID查询题单
export const getGroupByIdApi = (id) => {
  return request.get(`/admin/group/${id}`)
}

// 编辑题单
export const updateGroupApi = (data) => {
  return request.put('/admin/group', data)
}

// 根据ID删除题单
export const deleteGroupApi = (id) => {
  return request.delete('/admin/group', { params: { id } })
}

// 上架/下架题单
export const updateGroupStatusApi = (status, id) => {
  return request.post(`/admin/group/status/${status}/${id}`)
}

export default {
  saveGroupApi,
  pageGroupApi,
  getGroupByIdApi,
  updateGroupApi,
  deleteGroupApi,
  updateGroupStatusApi
}
