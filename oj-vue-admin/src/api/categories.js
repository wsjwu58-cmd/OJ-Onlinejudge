import request from '../util/request'

//获取分类列表（分页查询）
export const getCategoriesApi=(params)=>{
  return request.get('/admin/problem/type/page',{params})
}

//获取所有分类（下拉框查询）
export const getAllCategoriesApi=()=>{
  return request.get('/admin/problem/type/all')
}

//获取分类详情
export const getCategoryApi=(id)=>{
  return request.get(`/admin/problem/type/${id}`)
}

//创建分类
export const createCategoryApi=(data)=>{
  return request.post('/admin/problem/type',data)
}

//更新分类
export const updateCategoryApi=(data)=>{
  return request.put('/admin/problem/type',data)
}

//修改分类状态
export const updateCategoryStatusApi=(id, isActive)=>{
  // 将布尔值转换为整数：true -> 1, false -> 0
  const status = isActive ? 1 : 0
  return request.post(`/admin/problem/type/status/${status}/${id}`)
}

//删除分类
export const deleteCategoryApi=(id)=>{
  return request.delete('/admin/problem/type', { params: { id } })
}

export default {
  getCategoriesApi,
  getAllCategoriesApi,
  getCategoryApi,
  createCategoryApi,
  updateCategoryApi,
  updateCategoryStatusApi,
  deleteCategoryApi
}