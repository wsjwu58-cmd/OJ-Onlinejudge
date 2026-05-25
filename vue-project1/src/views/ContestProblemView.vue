<template>
  <div class="contest-problem-page">
    <!-- 顶部比赛导航栏 -->
    <div class="contest-nav-bar">
      <router-link :to="`/contests/${contestId}`" class="back-link">
        <el-icon><ArrowLeft /></el-icon>
        返回比赛
      </router-link>
      <div class="contest-timer" v-if="countdownText">
        <el-icon><Timer /></el-icon>
        <span class="timer-label">距结束：</span>
        <span class="timer-value">{{ countdownText }}</span>
      </div>
    </div>

    <!-- 主体：左侧题目 + 右侧编辑器 -->
    <div class="content-wrapper">
      <!-- 左侧：题目信息 -->
      <div class="problem-info-panel">
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="8" animated />
        </div>
        <div v-else-if="problemDetail">
          <div class="problem-header">
            <h1 class="problem-title">{{ problemDetail.title }}</h1>
            <div class="problem-stats">
              <el-tag
                :type="getDifficultyType(problemDetail.difficulty)"
                size="small"
              >
                {{ getDifficultyLabel(problemDetail.difficulty) }}
              </el-tag>
              <span class="acceptance-rate">通过率 {{ problemDetail.acceptance || 0 }}%</span>
            </div>
          </div>

          <el-divider />

          <div class="problem-description" v-highlight v-html="processedContent"></div>

          <!-- Hack 区域 -->
          <div class="hack-section" v-if="lastJudgeStatus === 'Accepted' || isLocked">
            <el-divider />
            <div class="hack-header">
              <h3>Hack 挑战</h3>
              <el-button
                v-if="!isLocked"
                type="warning" size="small"
                @click="handleLock"
                :loading="lockLoading"
              >
                锁定题目
              </el-button>
              <el-button
                v-else
                type="info" size="small"
                @click="handleUnlock"
                :loading="unlockLoading"
              >
                解锁题目
              </el-button>
            </div>
            <p class="hack-tip">
              锁定后将无法再提交此题，但可查看其他AC选手的代码并发起Hack挑战。
            </p>

            <!-- AC提交列表（锁定后可见） -->
            <div v-if="isLocked && acSubmissions.length > 0" class="ac-submissions">
              <h4>其他选手的AC提交（{{ acSubmissions.length }}条）</h4>
              <div v-for="sub in acSubmissions" :key="sub.id" class="submission-item">
                <div class="sub-header">
                  <span>用户 #{{ sub.userId }}</span>
                  <span class="sub-lang">{{ sub.language }}</span>
                  <span class="sub-time">{{ formatTime(sub.submitTime) }}</span>
                  <el-button type="danger" size="small" @click="openHackDialog(sub)">
                    发起Hack
                  </el-button>
                </div>
                <pre class="sub-code" v-highlight><code>{{ sub.code }}</code></pre>
              </div>
            </div>
            <el-empty v-else-if="isLocked" description="暂无其他AC提交" :image-size="60" />
          </div>
        </div>
        <div v-else class="error-state">
          <el-empty description="题目加载失败" />
        </div>
      </div>

      <!-- 右侧：代码编辑器 -->
      <div class="code-editor-panel">
        <div class="editor-header">
          <h2>代码编辑器</h2>
        </div>
        <CodeEditor
          v-if="!loading && problemDetail"
          v-model="code"
          :problem-id="problemDetail.id"
          :language="selectedLanguage"
          :result="editorResult"
          @languageChange="handleLanguageChange"
          @run="handleRunCode"
          @submit="handleSubmitCode"
        />
        <div v-else-if="loading" class="loading-editor">
          <el-skeleton :rows="10" animated />
        </div>
      </div>
    </div>

    <!-- Hack 提交弹窗 -->
    <el-dialog v-model="hackDialogVisible" title="发起 Hack" width="600px" destroy-on-close>
      <div class="hack-dialog-body">
        <p>目标用户: <strong>#{{ hackTarget?.userId }}</strong></p>
        <p>目标提交ID: <strong>{{ hackTarget?.id }}</strong></p>
        <p>目标语言: <strong>{{ hackTarget?.language }}</strong></p>
        <el-divider />
        <div class="hack-input-area">
          <label>测试数据（Hack Input）：</label>
          <el-input
            v-model="hackInput"
            type="textarea"
            :rows="6"
            placeholder="请输入要测试的输入数据..."
          />
          <p class="input-note">数据将对目标代码和标准答案同时运行，若目标失败而标准通过则Hack成功。</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="hackDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleSubmitHack" :loading="hackSubmitting">
          {{ hackSubmitting ? '提交中...' : '确认Hack' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Hack 结果弹窗 -->
    <el-dialog v-model="hackResultVisible" title="Hack 结果" width="500px">
      <div class="hack-result-body">
        <el-result
          :icon="hackResult?.status === 'HackSuccess' ? 'success' : 'error'"
          :title="hackResult?.status === 'HackSuccess' ? 'Hack 成功！' : 'Hack 失败'"
          :sub-title="hackResult?.errorInfo || ''"
        >
          <template #extra v-if="hackResult">
            <p>状态: {{ hackResult.status }}</p>
            <p v-if="hackResult.targetResult">目标代码结果: {{ hackResult.targetResult }}</p>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Timer } from '@element-plus/icons-vue'
import { getProblemDetail, runCode } from '../services/api'
import { contestSubmitApi, getContestDetailApi, lockProblemApi, unlockProblemApi, getAcSubmissionsApi, submitHackApi, getHackResultApi, getHackStatusApi } from '../api/contests'
import { createSubmissionPoller } from '../api/submissions'
import CodeEditor from '../components/CodeEditor.vue'

const route = useRoute()
const router = useRouter()
const contestId = route.params.contestId
const problemId = route.params.problemId

const problemDetail = ref(null)
const contest = ref({})
const loading = ref(true)
const code = ref('')
const selectedLanguage = ref('Java')
const editorResult = ref(null)
const lastJudgeStatus = ref(null)
let judgePoller = null

// ===== Hack 相关状态 =====
const isLocked = ref(false)
const lockLoading = ref(false)
const unlockLoading = ref(false)
const acSubmissions = ref([])
const hackDialogVisible = ref(false)
const hackTarget = ref(null)
const hackInput = ref('')
const hackSubmitting = ref(false)
const hackResultVisible = ref(false)
const hackResult = ref(null)
const currentHackId = ref(null)
let hackPollTimer = null

// ===== 倒计时 =====
const countdownText = ref('')
let countdownTimer = null

const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    const now = new Date().getTime()
    const endTime = new Date(contest.value.endTime).getTime()
    const diff = endTime - now

    if (diff <= 0) {
      countdownText.value = '已结束'
      clearInterval(countdownTimer)
      ElMessage.warning('比赛已结束')
      return
    }

    const h = Math.floor(diff / 3600000)
    const m = Math.floor((diff % 3600000) / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    countdownText.value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }, 1000)
}

// ===== 题目相关 =====
const processedContent = computed(() => {
  const content = problemDetail.value?.contentHtml || problemDetail.value?.content
  if (!content) return ''
  let result = content
  result = result.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
  result = result.replace(/https:\/\/assets\.gitee\.com\//g, '/gitee-assets/')
  return result
})

const getDifficultyType = (d) => ({ 'Easy': 'success', 'Medium': 'warning', 'Hard': 'danger' }[d] || 'info')
const getDifficultyLabel = (d) => ({ 'Easy': '简单', 'Medium': '中等', 'Hard': '困难' }[d] || d)

const getLanguageTemplate = (language, templateCode) => {
  if (templateCode) {
    const templates = {
      'Java': templateCode.java || templateCode.Java,
      'Python': templateCode.python || templateCode.Python,
      'C++': templateCode.cpp || templateCode['C++'],
      'JavaScript': templateCode.javascript || templateCode.JavaScript
    }
    if (templates[language]) return templates[language]
  }
  const defaultTemplates = {
    Java: `class Solution {\n    public Object solve() {\n        // 在这里编写你的代码\n        return null;\n    }\n}`,
    Python: `class Solution:\n    def solve(self):\n        # 在这里编写你的代码\n        pass`,
    'C++': `class Solution {\npublic:\n    void solve() {\n        // 在这里编写你的代码\n    }\n};`,
    JavaScript: `var solve = function() {\n    // 在这里编写你的代码\n    return null;\n};`
  }
  return defaultTemplates[language] || ''
}

const handleLanguageChange = (language) => {
  selectedLanguage.value = language
  const template = getLanguageTemplate(language, problemDetail.value?.templateCode)
  if (template) code.value = template
}

// ===== 运行代码（调试，不计入比赛） =====
const handleRunCode = async ({ problemId: pid, code: c, language }) => {
  try {
    editorResult.value = null
    const data = await runCode({ problemId: pid, code: c, language })
    editorResult.value = {
      status: data.status || 'Result',
      message: data.stdout || data.errorInfo || '执行完成',
      runtime: data.runtimeMs != null ? data.runtimeMs + ' ms' : undefined,
      memory: data.memoryKb != null ? data.memoryKb + ' KB' : undefined
    }
  } catch (err) {
    editorResult.value = { status: 'Error', message: err?.message || '运行失败' }
  }
}

// ===== 提交代码（带 contestId） =====
const handleSubmitCode = async ({ problemId: pid, code: c, language }) => {
  try {
    editorResult.value = null
    if (judgePoller) { judgePoller.cancel(); judgePoller = null }

    const res = await contestSubmitApi({
      contestId: Number(contestId),
      problemId: pid,
      code: c,
      language
    })

    const data = res.code === 1 ? res.data : res

    // 语法错误
    if (data.status === 'Syntax Error') {
      editorResult.value = { status: 'Syntax Error', message: data.errorInfo || '代码存在语法错误', statusType: 'syntax-error' }
      return
    }
    // 限流
    if (data.status === 'Rate Limited') {
      editorResult.value = { status: 'Rate Limited', message: data.errorInfo || '提交过于频繁', statusType: 'rate-limited' }
      ElMessage.warning('提交过于频繁，请稍后再试')
      return
    }
    // 有正在处理的提交
    if (data.status === 'Processing') {
      editorResult.value = { status: 'Processing', message: data.errorInfo || '有提交正在判题中', statusType: 'processing' }
      ElMessage.info('有提交正在判题中，请等待')
      return
    }
    // 异步判题：Pending → 等 WebSocket / 轮询
    if (data.status === 'Pending') {
      editorResult.value = { status: 'Judging', message: '代码已提交，正在判题中...', statusType: 'judging' }
      const submitTime = Date.now()

      judgePoller = createSubmissionPoller(pid, {
        interval: 2500,
        timeout: 120000,
        since: submitTime,
        onResult: (latest) => {
          editorResult.value = {
            status: latest.status,
            message: latest.status === 'Accepted' ? '🎉 通过！' : (latest.errorInfo || latest.status),
            runtime: latest.runtimeMs != null ? latest.runtimeMs + ' ms' : undefined,
            memory: latest.memoryKb != null ? latest.memoryKb + ' KB' : undefined,
            statusType: latest.status === 'Accepted' ? 'accepted' : 'error'
          }
          if (latest.status === 'Accepted') {
            lastJudgeStatus.value = 'Accepted'
          }
        },
        onError: () => {
          editorResult.value = { status: 'Error', message: '获取判题结果超时，请刷新查看' }
        }
      })
      return
    }

    // 记录本次判题状态（用于判断是否可Hack）
    if (data.status === 'Accepted') {
      lastJudgeStatus.value = 'Accepted'
    }

    // 其他直接返回的结果
    editorResult.value = {
      status: data.status || 'Result',
      message: data.errorInfo || data.status,
      runtime: data.runtimeMs != null ? data.runtimeMs + ' ms' : undefined,
      memory: data.memoryKb != null ? data.memoryKb + ' KB' : undefined
    }
  } catch (err) {
    editorResult.value = { status: 'Error', message: err?.message || '提交失败' }
    ElMessage.error('提交失败')
  }
}

// ===== Hack 操作 =====
const handleLock = async () => {
  try {
    lockLoading.value = true
    const res = await lockProblemApi(contestId, problemId)
    if (res.code === 1) {
      isLocked.value = true
      ElMessage.success('已锁定题目，可以查看其他选手的AC代码')
      loadAcSubmissions()
    } else {
      ElMessage.error(res.msg || '锁定失败')
    }
  } catch (e) {
    ElMessage.error('锁定请求失败')
  } finally {
    lockLoading.value = false
  }
}

const handleUnlock = async () => {
  try {
    unlockLoading.value = true
    const res = await unlockProblemApi(contestId, problemId)
    if (res.code === 1) {
      isLocked.value = false
      ElMessage.success('已解锁题目')
    } else {
      ElMessage.error(res.msg || '解锁失败')
    }
  } catch (e) {
    ElMessage.error('解锁请求失败')
  } finally {
    unlockLoading.value = false
  }
}

const loadAcSubmissions = async () => {
  try {
    const res = await getAcSubmissionsApi(contestId, problemId)
    if (res.code === 1) {
      acSubmissions.value = res.data || []
    }
  } catch (e) {
    console.error('加载AC提交列表失败:', e)
  }
}

const openHackDialog = (sub) => {
  hackTarget.value = sub
  hackInput.value = ''
  hackDialogVisible.value = true
}

const handleSubmitHack = async () => {
  if (!hackInput.value.trim()) {
    ElMessage.warning('请输入测试数据')
    return
  }
  try {
    hackSubmitting.value = true
    const res = await submitHackApi(contestId, {
      contestId: Number(contestId),
      problemId: Number(problemId),
      targetUserId: hackTarget.value.userId,
      targetSubmissionId: hackTarget.value.id,
      hackInput: hackInput.value
    })
    if (res.code === 1) {
      const hackId = res.data
      currentHackId.value = hackId
      hackDialogVisible.value = false
      ElMessage.success('Hack已提交，等待判题结果...')
      startHackPolling(hackId)
    } else {
      ElMessage.error(res.msg || 'Hack提交失败')
    }
  } catch (e) {
    ElMessage.error('Hack提交请求失败')
  } finally {
    hackSubmitting.value = false
  }
}

const startHackPolling = (hackId) => {
  if (hackPollTimer) clearInterval(hackPollTimer)
  hackPollTimer = setInterval(async () => {
    try {
      const res = await getHackResultApi(contestId, hackId)
      if (res.code === 1) {
        const record = res.data
        if (record && record.status !== 'Pending' && record.status !== 'Validating') {
          clearInterval(hackPollTimer)
          hackPollTimer = null
          hackResult.value = record
          hackResultVisible.value = true
        }
      }
    } catch (e) {
      console.error('查询Hack结果失败:', e)
    }
  }, 2000)
  setTimeout(() => {
    if (hackPollTimer) {
      clearInterval(hackPollTimer)
      hackPollTimer = null
      ElMessage.warning('Hack判题超时，请稍后查看结果')
    }
  }, 30000)
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
}

// ===== 检查 Hack 状态（页面加载时自动查询） =====
const checkHackStatus = async () => {
  try {
    const res = await getHackStatusApi(contestId, problemId)
    if (res.code === 1 && res.data) {
      if (res.data.accepted) {
        lastJudgeStatus.value = 'Accepted'
      }
      if (res.data.locked) {
        isLocked.value = true
        loadAcSubmissions()
      }
    }
  } catch (e) {
    console.error('检查Hack状态失败:', e)
  }
}

// ===== 加载数据 =====
const loadData = async () => {
  try {
    loading.value = true

    // 并行加载题目详情和比赛信息
    const [problemData, contestRes] = await Promise.all([
      getProblemDetail(problemId),
      getContestDetailApi(contestId)
    ])

    problemDetail.value = problemData
    if (problemData?.templateCode) {
      code.value = getLanguageTemplate(selectedLanguage.value, problemData.templateCode)
    }

    if (contestRes.code === 1) {
      contest.value = contestRes.data || {}
      startCountdown()
    }

    // 检查 Hack 状态：是否已AC、是否已锁定
    checkHackStatus()
  } catch (err) {
    console.error('加载失败:', err)
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (judgePoller) judgePoller.cancel()
  if (hackPollTimer) clearInterval(hackPollTimer)
})
</script>

<style scoped>
.contest-problem-page {
  height: calc(100vh - 50px);
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

/* 顶部比赛导航 */
.contest-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  flex-shrink: 0;
}

.back-link {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.back-link:hover {
  color: #409eff;
}

.contest-timer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timer-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.timer-value {
  font-size: 18px;
  font-weight: 700;
  color: #ffd666;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

/* 主体布局 */
.content-wrapper {
  flex: 1;
  display: flex;
  overflow: hidden;
  gap: 2px;
}

/* 左侧题目面板 */
.problem-info-panel {
  width: 45%;
  overflow-y: auto;
  padding: 24px;
  background: #fff;
}

.problem-header {
  margin-bottom: 8px;
}

.problem-title {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
  margin-bottom: 8px;
}

.problem-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.acceptance-rate {
  font-size: 13px;
  color: #8c8c8c;
}

.problem-description {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.problem-description :deep(pre) {
  background: #f6f8fa;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
}

.problem-description :deep(code) {
  font-family: 'Consolas', 'Courier New', monospace;
}

/* 右侧代码编辑器 */
.code-editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

.editor-header {
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.editor-header h2 {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.loading-state, .error-state, .loading-editor {
  padding: 40px;
}

/* Hack 区域样式 */
.hack-section {
  margin-top: 8px;
}

.hack-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.hack-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e6a23c;
  margin: 0;
}

.hack-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.ac-submissions {
  margin-top: 12px;
}

.ac-submissions h4 {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
}

.submission-item {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.sub-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.sub-lang {
  color: #1890ff;
  font-size: 12px;
  background: #e6f7ff;
  padding: 1px 8px;
  border-radius: 4px;
}

.sub-time {
  color: #8c8c8c;
  font-size: 12px;
  flex: 1;
}

.sub-code {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  margin: 0;
}

.sub-code code {
  font-family: 'Consolas', 'Courier New', monospace;
}

.hack-dialog-body {
  font-size: 14px;
}

.hack-input-area {
  margin-top: 8px;
}

.hack-input-area label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 13px;
}

.input-note {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
}

.hack-result-body {
  text-align: center;
}
</style>
