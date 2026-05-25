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

// Hack: 查询AC和锁定状态
export const getHackStatusApi = (contestId, problemId) => {
  return request.get(`/user/contest/${contestId}/problem/${problemId}/hack-status`)
}

// Hack: 锁定题目
export const lockProblemApi = (contestId, problemId) => {
  return request.post(`/user/contest/${contestId}/problem/${problemId}/lock`)
}

// Hack: 解锁题目
export const unlockProblemApi = (contestId, problemId) => {
  return request.post(`/user/contest/${contestId}/problem/${problemId}/unlock`)
}

// Hack: 获取AC提交列表（含代码）
export const getAcSubmissionsApi = (contestId, problemId) => {
  return request.get(`/user/contest/${contestId}/problem/${problemId}/ac-submissions`)
}

// Hack: 提交Hack
export const submitHackApi = (contestId, data) => {
  return request.post(`/user/contest/${contestId}/hack`, data)
}

// Hack: 查询Hack结果
export const getHackResultApi = (contestId, hackId) => {
  return request.get(`/user/contest/${contestId}/hack/${hackId}/result`)
}

// Hack: 查询Hack记录列表
export const getHackRecordsApi = (contestId) => {
  return request.get(`/user/contest/${contestId}/hack/records`)
}

export default {
  getContestsApi,
  getContestDetailApi,
  joinContestApi,
  getContestProblemsApi,
  getContestRankApi,
  contestSubmitApi,
  lockProblemApi,
  unlockProblemApi,
  getAcSubmissionsApi,
  submitHackApi,
  getHackResultApi,
  getHackRecordsApi
}
