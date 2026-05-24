import request from '../util/request'

//登录
export const loginApi=(data)=>{
  return request.post('/admin/user/login',data)
}

export default {
  loginApi
}