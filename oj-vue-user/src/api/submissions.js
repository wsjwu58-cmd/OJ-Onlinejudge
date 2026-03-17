import request from '../utils/request'

// 获取当前用户的提交记录（传 problemId 查该题的，不传查全部）
export const getSubmissionsApi = (problemId) => {
  if (problemId) {
    return request.get('/user/submission', { params: { problemId } })
  }
  return request.get('/user/submission')
}

// 获取提交记录详情
export const getSubmissionDetailApi = (id) => {
  return request.get(`/user/submissions/${id}`)
}

/**
 * 轮询提交列表检查判题结果（降级方案，WebSocket 断连时使用）
 * 后端异步判题（redis+mq+lua）流程：
 *   提交 → 立即返回 Pending → MQ异步判题 → WebSocket推送结果 → 写入DB
 * 当 WebSocket 不可用时，通过轮询提交列表获取最新结果
 *
 * @param {number} problemId 题目ID
 * @param {object} options 配置项
 * @param {number} options.interval 轮询间隔(ms)，默认 2000
 * @param {number} options.timeout 最大轮询时长(ms)，默认 120000（2分钟）
 * @param {function} options.onResult 获取到最终结果时的回调
 * @param {function} options.onError 错误回调
 * @returns {{ cancel: Function }} 返回可取消的对象
 */
export const createSubmissionPoller = (problemId, options = {}) => {
  const {
    interval = 2000,
    timeout = 120000,
    submitToken = null,  // 保留参数兼容调用方，暂未使用（DB未存token）
    onResult = null,
    onError = null
  } = options

  let timer = null
  let stopped = false
  const startTime = Date.now()

  // AI 判题专用状态列表，轮询时需跳过这些记录
  const AI_STATUSES = ['AI Error', 'AI Accepted', 'AI Wrong Answer', 'AI Judge']

  const poll = async () => {
    if (stopped) return
    try {
      const res = await getSubmissionsApi(problemId)
      const list = res.data || res || []

      if (stopped) return

      // 过滤掉 AI 判题产生的记录，只保留正常判题流程的记录
      const normalList = list.filter(item => !AI_STATUSES.includes(item.status))

      if (normalList.length > 0) {
        const latest = normalList[0] // 后端默认按时间倒序，取最新正常记录
        const status = latest.status
        if (status !== 'Pending' && status !== 'Judging') {
          // 判题完成，回调结果
          if (onResult) onResult(latest)
          return
        }
      }

      // 超时检测
      if (Date.now() - startTime > timeout) {
        if (onError) onError(new Error('判题超时，请稍后在提交记录中查看结果'))
        return
      }

      // 继续轮询
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

  // 延迟启动（给后端一点处理时间）
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
