import request from '../util/request'

//获取用户列表（分页查询）
export const getUsersApi=(params)=>{
  return request.get('/admin/user/page',{params})
}

//获取用户详情
export const getUserApi=(id)=>{
  return request.get(`/admin/user/${id}`)
}

//创建用户
export const createUserApi=(data)=>{
  return request.post('/admin/user/add/admin',data)
}

//更新用户
export const updateUserApi=(data)=>{
  return request.put('/admin/user',data)
}

//删除用户
export const deleteUserApi=(id)=>{
  return request.delete(`/admin/users/${id}`)
}

//启用/停用用户账号
export const updateUserStatusApi=(status, id)=>{
  console.log('调用updateUserStatusApi:', { status, id })
  // 检查ID是否存在
  if (!id) {
    console.error('ID不存在:', id)
    return Promise.reject(new Error('ID不存在'))
  }
  // 尝试将ID作为路径参数传递
  return request.post(`/admin/user/status/${status}/${id}`)
}

export default {
  getUsersApi,
  getUserApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  updateUserStatusApi
}