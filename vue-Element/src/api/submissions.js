import request from '../util/request'

//分页查询提交记录（管理端）
export const getSubmissionsPageApi=(params)=>{
  return request.get('/admin/submissions/page',{params})
}

//获取提交记录详情
export const getSubmissionApi=(id)=>{
  return request.get(`/admin/submissions/${id}`)
}

//删除提交记录
export const deleteSubmissionApi=(id)=>{
  return request.delete(`/admin/submissions/${id}`)
}

//获取当前用户的提交记录（用户端接口）
export const getUserSubmissionsApi=()=>{
  return request.get('/user/submission')
}

export default {
  getSubmissionsPageApi,
  getSubmissionApi,
  deleteSubmissionApi,
  getUserSubmissionsApi
}