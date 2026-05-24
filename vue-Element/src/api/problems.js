import request from '../util/request'

//获取题目列表（分页查询）
export const getProblemsApi=(params)=>{
  return request.get('/admin/problem/page/of',{params})
}

//获取题目详情
export const getProblemApi=(id)=>{
  return request.get(`/admin/problem/${id}`)
}

//获取题目详情（包含HTML内容）
export const getProblemDetailApi=(id)=>{
  return request.get(`/admin/problem/${id}`)
}

//创建题目
export const createProblemApi=(data)=>{
  return request.post('/admin/problem',data)
}

//更新题目
export const updateProblemApi=(data)=>{
  return request.put(`/admin/problem`,data)
}

//删除题目
export const deleteProblemApi=(id)=>{
  return request.delete(`/admin/problem/${id}`)
}

//查询所有题目
export const getAllProblemsApi=()=>{
  return request.get('/admin/problem/all')
}

export default {
  getProblemsApi,
  getProblemApi,
  createProblemApi,
  updateProblemApi,
  deleteProblemApi,
  getAllProblemsApi
}