import request from '../utils/request'

const getToken = () => {
  const loginUserStr = localStorage.getItem('loginUser')
  let token = ''
  if (loginUserStr) {
    try {
      const loginUser = JSON.parse(loginUserStr)
      token = loginUser.token || ''
    } catch (e) {}
  }
  if (!token) {
    token = localStorage.getItem('token') || ''
  }
  return token
}

export const submitAiJudge = (data) => request.post('/user/ai/judge/submit', data)
export const submitAiSyntaxCheck = (data) => request.post('/user/ai/syntax-check/submit', data)
export const submitAiErrorAnalysis = (data) => request.post('/user/ai/analyze-error/submit', data)
export const submitAiChat = (data) => request.post('/user/ai/chat/submit', data)
export const submitAiHint = (data) => request.post('/user/ai/hint/submit', data)

export const createEventSource = (path) => {
  const token = getToken()
  const url = `${import.meta.env.VITE_API_BASE_URL || ''}/api${path}?authorization=${encodeURIComponent(token)}`
  return new EventSource(url)
}
