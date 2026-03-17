import request from '../utils/request'

// 获取比赛列表（分页查询）
export const getContestsApi = (params) => {
  return request.get('/user/contest/page', { params })
}

// 获取比赛详情
export const getContestDetailApi = (id) => {
  return request.get(`/user/contest/${id}`)
}

// 报名比赛
export const joinContestApi = (id) => {
  return request.post(`/user/contest/${id}/join`)
}

// 获取比赛题目列表
export const getContestProblemsApi = (id) => {
  return request.get(`/user/contest/${id}/problems`)
}

// 获取比赛排名
export const getContestRankApi = (id, params) => {
  return request.get(`/user/contest/${id}/rank`, { params })
}

// 比赛提交代码（带 contestId）
export const contestSubmitApi = ({ contestId, problemId, code, language }) => {
  return request.post('/user/judge/submit', {
    contestId,
    problemId,
    code,
    language
  })
}

export default {
  getContestsApi,
  getContestDetailApi,
  joinContestApi,
  getContestProblemsApi,
  getContestRankApi,
  contestSubmitApi
}
