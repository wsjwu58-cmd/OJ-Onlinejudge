import request from '../util/request'

//获取提交记录列表（管理端）
export const getSubmissionsApi=(params)=>{
  return request.get('/admin/submissions',{params})
}

//获取当前用户的提交记录（用户端接口）
export const getUserSubmissionsApi=()=>{
  return request.get('/user/submission')
}

//获取提交记录详情
export const getSubmissionApi=(id)=>{
  return request.get(`/admin/submissions/${id}`)
}

//删除提交记录
export const deleteSubmissionApi=(id)=>{
  return request.delete(`/admin/submissions/${id}`)
}

export default {
  getSubmissionsApi,
  getUserSubmissionsApi,
  getSubmissionApi,
  deleteSubmissionApi
}