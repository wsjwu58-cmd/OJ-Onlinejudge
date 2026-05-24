<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSubmissionDetailApi } from '../api/submissions'

const route = useRoute()
const router = useRouter()
const submissionId = ref(route.params.id)
const loading = ref(true)
const error = ref('')
const isJudging = ref(false)
let pollTimer = null
const submission = ref({
  submissionId: null,
  problemId: null,
  status: 'Pending',
  runtimeMs: 0,
  memoryKb: 0,
  errorInfo: '',
  code: '',
  language: '',
  submitTime: '',
  testCasesPassed: 0,
  testCasesTotal: 0,
  title: ''
})

// 状态映射（使用后端返回的字符串状态）
const statusMap = {
  'Pending': { text: '排队中', class: 'pending' },
  'Judging': { text: '判题中', class: 'judging' },
  'Accepted': { text: '通过', class: 'accepted' },
  'Wrong Answer': { text: '答案错误', class: 'failed' },
  'Compile Error': { text: '编译错误', class: 'compile-error' },
  'Runtime Error': { text: '运行时错误', class: 'runtime-error' },
  'Time Limit Exceeded': { text: '超时', class: 'failed' },
  'Memory Limit Exceeded': { text: '内存超限', class: 'failed' },
  'Syntax Error': { text: '语法错误', class: 'compile-error' }
}

// 获取状态显示信息
const getStatusInfo = (status) => {
  return statusMap[status] || { text: status || '未知', class: 'pending' }
}

// 获取提交结果
const fetchSubmissionResult = async () => {
  try {
    loading.value = true
    const res = await getSubmissionDetailApi(submissionId.value)
    const data = res.data || res
    submission.value = { ...submission.value, ...data }

    // 如果还在判题中，启动轮询
    if (data.status === 'Pending' || data.status === 'Judging') {
      isJudging.value = true
      startPolling()
    } else {
      isJudging.value = false
    }
  } catch (err) {
    error.value = err.message || '获取提交结果失败'
  } finally {
    loading.value = false
  }
}

// 轮询判题结果
const startPolling = () => {
  stopPolling()
  const startTime = Date.now()
  const maxTimeout = 120000 // 2分钟超时

  const poll = async () => {
    try {
      const res = await getSubmissionDetailApi(submissionId.value)
      const data = res.data || res
      submission.value = { ...submission.value, ...data }

      if (data.status === 'Pending' || data.status === 'Judging') {
        if (Date.now() - startTime > maxTimeout) {
          isJudging.value = false
          error.value = '判题超时，请稍后刷新页面查看结果'
          return
        }
        pollTimer = setTimeout(poll, 2000)
      } else {
        isJudging.value = false
      }
    } catch (err) {
      if (Date.now() - startTime > maxTimeout) {
        isJudging.value = false
        error.value = '获取判题结果失败'
      } else {
        pollTimer = setTimeout(poll, 3000)
      }
    }
  }

  pollTimer = setTimeout(poll, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

// 返回编辑页面
const backToEdit = () => {
  router.push(`/problems/${submission.value.problemId}`)
}

// 页面加载时获取数据
onMounted(() => {
  fetchSubmissionResult()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="submit-result-container">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-content">
        <div class="logo">
          <h1>LeetCode Clone</h1>
        </div>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/problems" class="nav-item">题库</router-link>
          <router-link to="/login" class="nav-item">登录</router-link>
          <router-link to="/profile" class="nav-item">个人中心</router-link>
        </nav>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="submit-result-main">
      <div class="container">
        <h2>提交结果</h2>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading">
          <p>加载中...</p>
        </div>
        
        <!-- 错误信息 -->
        <div v-else-if="error" class="error-message">
          {{ error }}
        </div>
        
        <!-- 提交结果内容 -->
        <div v-else class="submit-result-content">
          <!-- 结果概览 -->
          <div class="result-overview">
            <h3>结果</h3>
            <div class="result-status">
              <span :class="['status-badge', statusMap[submission.status].class]">
                {{ statusMap[submission.status].text }}
              </span>
            </div>
            <div class="result-stats">
              <div class="stat-item">
                <span class="stat-label">运行时间</span>
                <span class="stat-value">{{ submission.runtime }} ms</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">内存消耗</span>
                <span class="stat-value">{{ (submission.memory / 1024).toFixed(1) }} MB</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">语言</span>
                <span class="stat-value">{{ submission.language }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">提交时间</span>
                <span class="stat-value">{{ new Date(submission.submittedAt).toLocaleString() }}</span>
              </div>
            </div>
            <button class="btn-primary" @click="backToEdit">返回编辑</button>
          </div>
          
          <!-- 错误信息 -->
          <div v-if="submission.errorMessage" class="error-section">
            <h3>错误信息</h3>
            <pre class="error-message-content">{{ submission.errorMessage }}</pre>
          </div>
          
          <!-- 代码展示 -->
          <div class="code-section">
            <h3>提交的代码</h3>
            <pre class="code-content"><code>{{ submission.code }}</code></pre>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.submit-result-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 顶部导航栏 */
.app-header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.logo h1 {
  font-size: 24px;
  color: #ff6b6b;
  margin: 0;
}

.nav-menu {
  display: flex;
  gap: 30px;
}

.nav-item {
  text-decoration: none;
  color: #333;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #ff6b6b;
}

/* 主内容区 */
.submit-result-main {
  padding: 20px;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

.submit-result-main h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 24px;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

/* 错误信息 */
.error-message {
  background-color: #ffebee;
  color: #f44336;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 16px;
}

/* 提交结果内容 */
.submit-result-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 结果概览 */
.result-overview {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.result-overview h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 16px;
}

.result-status {
  margin-bottom: 24px;
}

.status-badge {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 18px;
}

.status-badge.pending {
  background-color: #e3f2fd;
  color: #2196f3;
}

.status-badge.judging {
  background-color: #fff3e0;
  color: #ff9800;
}

.status-badge.accepted {
  background-color: #e8f5e8;
  color: #4caf50;
}

.status-badge.failed {
  background-color: #ffebee;
  color: #f44336;
}

.status-badge.compile-error {
  background-color: #ffebee;
  color: #f44336;
}

.status-badge.runtime-error {
  background-color: #ffebee;
  color: #f44336;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

/* 错误信息部分 */
.error-section {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.error-section h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 16px;
}

.error-message-content {
  background-color: #ffebee;
  color: #f44336;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
}

/* 代码部分 */
.code-section {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.code-section h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 16px;
}

.code-content {
  background-color: #f5f7fa;
  border: 1px solid #e4e6eb;
  border-radius: 4px;
  padding: 16px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.5;
}

/* 按钮样式 */
.btn-primary {
  padding: 10px 20px;
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #ff5252;
}
</style>