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

export const submitAgentChat = (data) => request.post('/user/agent/chat', data)
export const getAgentTools = () => request.get('/user/agent/tools')

export const agentChat = (data) => request.post('/user/agent/chat', data)
export const agentChatStream = (data) => request.post('/user/agent/chat/stream', data)
export const generateSolution = (problemId, language) => request.get(`/user/agent/solution/${problemId}`, { params: { language } })
export const generateSolutionStream = (problemId, language) => request.get(`/user/agent/solution/${problemId}/stream`, { params: { language } })
export const analyzeLearning = (userId, days) => request.get(`/user/agent/learning/${userId}`, { params: { days } })
export const analyzeLearningStream = (userId, days) => request.get(`/user/agent/learning/${userId}/stream`, { params: { days } })
export const aiJudgeCode = (data) => request.post('/user/agent/judge', null, { params: data })
export const aiJudgeCodeStream = (data) => request.post('/user/agent/judge/stream', null, { params: data })
export const clearAgentSession = (sessionId) => request.delete(`/user/agent/session/${sessionId}`)

export const createEventSource = (path) => {
  const token = getToken()
  const url = `${import.meta.env.VITE_API_BASE_URL || ''}/api${path}?authorization=${encodeURIComponent(token)}`
  return new EventSource(url)
}

export const createPostEventSource = async (path, data) => {
  const token = getToken()
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}/api${path}?authorization=${encodeURIComponent(token)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data)
  })
  return response.body
}
