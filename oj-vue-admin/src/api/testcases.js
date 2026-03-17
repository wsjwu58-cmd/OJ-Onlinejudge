import request from '../util/request'

// 获取指定题目的测试用例列表
export const getTestCasesApi = (problemId) => {
  return request.get(`/admin/test/${problemId}`)
}

// 创建测试用例
export const createTestCaseApi = (data) => {
  return request.post('/admin/test', data)
}

// 更新测试用例
export const updateTestCaseApi = (data) => {
  return request.put('/admin/test', data)
}

// 删除测试用例
export const deleteTestCaseApi = (id) => {
  return request.delete(`/admin/test/${id}`)
}

export default {
  getTestCasesApi,
  createTestCaseApi,
  updateTestCaseApi,
  deleteTestCaseApi
}
