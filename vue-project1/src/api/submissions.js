import request from '../utils/request'

export const getSubmissionsApi = (problemId) => {
  if (problemId) {
    return request.get('/user/submission', { params: { problemId } })
  }
  return request.get('/user/submission')
}

export const getSubmissionDetailApi = (id) => {
  return request.get(`/user/submissions/${id}`)
}

export const createSubmissionPoller = (problemId, options = {}) => {
  const {
    interval = 2000,
    timeout = 120000,
    since = 0,
    onResult = null,
    onError = null
  } = options

  let timer = null
  let stopped = false
  const startTime = Date.now()
  const AI_STATUSES = ['AI Error', 'AI Accepted', 'AI Wrong Answer', 'AI Judge']

  const poll = async () => {
    if (stopped) return
    try {
      const res = await getSubmissionsApi(problemId)
      const list = res.data || res || []

      if (stopped) return

      const normalList = list
        .filter(item => !AI_STATUSES.includes(item.status))
        .filter(item => {
          const t = item.submitTime ? new Date(item.submitTime).getTime() : 0
          return t > since
        })

      if (normalList.length > 0) {
        const latest = normalList[0]
        if (latest.status && latest.status !== 'Pending' && latest.status !== 'Judging') {
          if (onResult) onResult(latest)
          return
        }
      }

      if (Date.now() - startTime > timeout) {
        if (onError) onError(new Error('判题超时'))
        return
      }

      timer = setTimeout(poll, interval)
    } catch (err) {
      if (stopped) return
      if (Date.now() - startTime > timeout) {
        if (onError) onError(err)
      } else {
        timer = setTimeout(poll, interval * 2)
      }
    }
  }

  timer = setTimeout(poll, 1500)

  return {
    cancel: () => {
      stopped = true
      if (timer) clearTimeout(timer)
    }
  }
}

export default {
  getSubmissionsApi,
  getSubmissionDetailApi,
  createSubmissionPoller
}
