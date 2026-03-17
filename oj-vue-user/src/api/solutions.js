import request from '../utils/request'

// 滚动分页查询某题的题解列表
export const getSolutionListApi = (problemId, lastId, offset = 0) => {
  return request.get('/user/comment/list', {
    params: { problemId, lastId, offset }
  })
}

// 查询题解详情
export const getSolutionDetailApi = (id) => {
  return request.get(`/user/comment/${id}`)
}

// 滚动分页查询题解下的评论
export const getSolutionCommentsApi = (solutionId, lastId, offset = 0) => {
  return request.get('/user/comment/comments', {
    params: { solutionId, lastId, offset }
  })
}

// 发布题解/评论（统一接口）
export const postSolutionApi = (data) => {
  return request.post('/user/comment', data)
}

// 发布评论（新增评论专用接口）
export const postCommentApi = (data) => {
  return request.post('/user/comment/newcomment', data)
}

// 获取题目讨论评论（滚动分页）
export const getProblemCommentsApi = (problemId, lastId, offset = 0) => {
  return request.get('/user/comment/comments', {
    params: { problemId, lastId, offset }
  })
}

// 点赞/取消点赞
export const likeSolutionApi = (id) => {
  return request.put(`/user/comment/like/${id}`)
}

export default {
  getSolutionListApi,
  getSolutionDetailApi,
  getSolutionCommentsApi,
  postSolutionApi,
  postCommentApi,
  getProblemCommentsApi,
  likeSolutionApi
}
