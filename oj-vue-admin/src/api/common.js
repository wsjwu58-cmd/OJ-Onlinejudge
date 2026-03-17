import request from '../util/request'

//文件上传
export const uploadFileApi=(file)=>{
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/common/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

//获取最近活动
export const getRecentActivitiesApi=(limit=10)=>{
  return request.get('/admin/workSpace/recent', {
    params: {
      limit
    }
  })
}

//获取营业数据
export const getWorkDataApi=()=>{
  return request.get('/admin/workSpace/data')
}

//获取题目数据
export const getProblemDataApi=()=>{
  return request.get('/admin/workSpace/problem')
}

//获取比赛数据
export const getContestDataApi=()=>{
  return request.get('/admin/workSpace/context')
}

//获取用户注册趋势
export const getUserRegisterTrendApi=(begin, end)=>{
  return request.get('/admin/report/userTrend', {
    params: {
      begin,
      end
    }
  })
}


// 获取题目数量趋势（新接口）
export const getProblemTrendApi = (begin, end) => {
  return request.get('/admin/report/problemTrend', {
    params: { begin, end }
  })
}

// 获取提交记录趋势
export const getRecordTrendApi = (begin, end) => {
  return request.get('/admin/report/ProblemRecord', {
    params: { begin, end }
  })
}

// 获取题目通过率排行
export const getProblemAcceptanceTop10Api = () => {
  return request.get('/admin/report/Percent')
}

//获取提交记录趋势
export const getSubmissionTrendApi=(begin, end)=>{
  return request.get('/admin/report/submissionTrend', {
    params: {
      begin,
      end
    }
  })
}

//获取题目通过率排行
export const getProblemAcceptanceRankingApi=(limit=10)=>{
  return request.get('/admin/report/problemAcceptanceRanking', {
    params: {
      limit
    }
  })
}

export default {
  uploadFileApi,
  getRecentActivitiesApi,
  getWorkDataApi,
  getProblemDataApi,
  getContestDataApi,
  getUserRegisterTrendApi,
  getProblemTrendApi,
  getSubmissionTrendApi,
  getProblemAcceptanceRankingApi
}