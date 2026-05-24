import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 自定义 JSON 解析：把超过 JS 安全整数的数字转为字符串，防止精度丢失
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        data = data.replace(/:(\s*)(\d{16,})/g, ':"$2"')
        return JSON.parse(data)
      } catch (e) {
        return data
      }
    }
    return data
  }]
})

apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.authorization = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    let errorMessage = '请求失败'
    if (error.response) {
      if (error.response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        errorMessage = '登录已过期，请重新登录'
      } else {
        errorMessage = error.response.data?.msg || error.response.data?.message || `请求失败 (${error.response.status})`
      }
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接'
    }
    return Promise.reject(new Error(errorMessage))
  }
)

const login = async (username, password, nonceStr, value) => {
  const response = await apiClient.post('/user/userLogin/login', {
    username,
    passwordHash: password,
    nonceStr,
    value
  })
  if (response.code === 1 && response.data) {
    const { token, ...userInfo } = response.data
    return {
      token: token,
      user: userInfo
    }
  }
  throw new Error(response.msg || '登录失败')
}

const register = async (username, email, password) => {
  const response = await apiClient.post('/user/userRegister', {
    username,
    email,
    password
  })
  if (response.code === 1) {
    return { message: '注册成功' }
  }
  throw new Error(response.msg || '注册失败')
}

const getUserProfile = async () => {
  const response = await apiClient.get('/user/userInfo')
  if (response.code === 1 && response.data) {
    return response.data
  }
  throw new Error(response.msg || '获取用户信息失败')
}

const getProblems = async (params = {}) => {
  try {
    const response = await apiClient.get('/user/problem/type', {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || 20,
        difficulty: params.difficulty || undefined,
        status: params.status || undefined,
        problemTypeId: params.problemTypeId || undefined
      }
    })
    if (response.code === 1 && response.data) {
      return response.data
    }
  } catch (e) {
    console.log('使用模拟数据', e)
  }
  return {
    total: 3,
    records: getMockProblems()
  }
}

const getProblemTypes = async () => {
  try {
    const response = await apiClient.get('/user/problem/alltype')
    if (response.code === 1 && response.data) {
      return response.data
    }
  } catch (e) {
    console.log('获取分类失败', e)
  }
  return getMockProblemTypes()
}

function getMockProblemTypes() {
  return [
    { id: 1, name: '数组' },
    { id: 2, name: '链表' },
    { id: 3, name: '哈希表' },
    { id: 4, name: '字符串' },
    { id: 5, name: '动态规划' },
    { id: 6, name: '贪心算法' }
  ]
}

const getProblemDetail = async (id) => {
  try {
    const response = await apiClient.get(`/user/problem/${id}`)
    if (response.code === 1 && response.data) {
      return response.data
    }
  } catch (e) {
    console.log('获取题目详情失败', e)
  }
  return getMockProblemDetail(id)
}

const submitCode = async ({ problemId, code, language }) => {
  const response = await apiClient.post('/user/judge/submit', {
    problemId,
    code,
    language
  })
  if (response.code === 1 && response.data) {
    return response.data
  }
  throw new Error(response.msg || '提交失败')
}

const runCode = async ({ problemId, code, language }) => {
  const response = await apiClient.post('/user/judge/run', {
    problemId,
    code,
    language
  })
  if (response.code === 1 && response.data) {
    return response.data
  }
  throw new Error(response.msg || '运行失败')
}

function getMockProblems() {
  return [
    {
      id: 1,
      title: '两数之和',
      description: '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出 和为目标值 target 的那 两个 整数，并返回它们的数组下标。',
      difficulty: '简单',
      tags: ['数组', '哈希表'],
      acceptance: '52.7%',
      examples: [
        {
          input: 'nums = [2,7,11,15], target = 9',
          output: '[0,1]',
          explanation: '因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。'
        }
      ],
      constraints: [
        '2 <= nums.length <= 10^4',
        '-10^9 <= nums[i] <= 10^9',
        '-10^9 <= target <= 10^9',
        '只会存在一个有效答案'
      ]
    },
    {
      id: 2,
      title: '两数相加',
      description: '给你两个 非空 的链表，表示两个非负的整数。它们每位数字都是按照 逆序 的方式存储的，并且每个节点只能存储 一位 数字。',
      difficulty: '中等',
      tags: ['链表', '数学'],
      acceptance: '42.1%',
      examples: [
        {
          input: 'l1 = [2,4,3], l2 = [5,6,4]',
          output: '[7,0,8]',
          explanation: '342 + 465 = 807'
        }
      ],
      constraints: [
        '每个链表中的节点数在范围 [1, 100] 内',
        '0 <= Node.val <= 9'
      ]
    },
    {
      id: 3,
      title: '无重复字符的最长子串',
      description: '给定一个字符串 s ，请你找出其中不含有重复字符的 最长子串 的长度。',
      difficulty: '中等',
      tags: ['字符串', '滑动窗口'],
      acceptance: '39.2%',
      examples: [
        {
          input: 's = "abcabcbb"',
          output: '3',
          explanation: '因为无重复字符的最长子串是 "abc"，所以其长度为 3。'
        }
      ],
      constraints: [
        '0 <= s.length <= 5 * 10^4',
        's 由英文字母、数字、符号和空格组成'
      ]
    }
  ]
}

function getMockProblemDetail(id) {
  const problems = getMockProblems()
  return problems.find(p => p.id === parseInt(id)) || {
    id: parseInt(id),
    title: '题目不存在',
    description: '该题目不存在或已被删除',
    difficulty: '简单',
    tags: [],
    acceptance: '0%',
    examples: [],
    constraints: []
  }
}

const getCaptcha = async () => {
  const response = await apiClient.post('/user/userLogin/get-captcha', {
    canvasWidth: 320,
    canvasHeight: 155,
    blockWidth: 65,
    blockHeight: 55,
    blockRadius: 9,
    place: 0
  })
  if (response.code === 1 && response.data) {
    return response.data
  }
  throw new Error(response.msg || '获取验证码失败')
}

const userSign = async () => {
  const response = await apiClient.get('/user/userInfo/sign')
  if (response.code === 1) {
    return response.data
  }
  throw new Error(response.msg || '签到失败')
}

const getSignCount = async () => {
  const response = await apiClient.get('/user/userInfo/sign/count')
  if (response.code === 1) {
    return response.data
  }
  throw new Error(response.msg || '获取签到统计失败')
}

const updateUserProfile = async (data) => {
  const response = await apiClient.put('/user/userInfo', data)
  if (response.code === 1) {
    return response.data
  }
  throw new Error(response.msg || '更新用户信息失败')
}

export { login, register, getUserProfile, updateUserProfile, getProblems, getProblemDetail, submitCode, runCode, getCaptcha, getProblemTypes, userSign, getSignCount }
