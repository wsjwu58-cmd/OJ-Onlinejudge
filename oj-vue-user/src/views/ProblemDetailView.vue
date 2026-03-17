<template>
  <div class="problem-detail-container">
    <section class="problem-content">
      <div class="content-wrapper-oj">
        <!-- 左侧：题目信息 -->
        <div class="problem-info" ref="problemInfoRef" @scroll="handleScroll">
          <div v-if="loading" class="loading-state">
            <el-skeleton :rows="5" animated></el-skeleton>
          </div>
          <div v-else-if="error" class="error-state">
            <el-alert type="error" :title="error" show-icon></el-alert>
            <el-button type="primary" @click="fetchProblemDetail" class="retry-btn">重试</el-button>
          </div>
          <div v-else>
            <!-- 题目基本信息 -->
            <div class="problem-header">
              <div class="problem-title-section">
                <h1 class="problem-title">{{ problemDetail.title }}</h1>
                <div class="problem-id">#{{ problemDetail.id }}</div>
              </div>
              <div class="problem-stats">
                <el-tag
                  :type="getDifficultyType(problemDetail.difficulty)"
                  size="small"
                  class="difficulty-tag"
                >
                  {{ getDifficultyLabel(problemDetail.difficulty) }}
                </el-tag>
                <span class="acceptance-rate">
                  {{ problemDetail.acceptance || 0 }}%
                </span>
              </div>
            </div>

            <!-- 题目标签 -->
            <div class="problem-tags" v-if="problemDetail.typeList && problemDetail.typeList.length > 0">
              <el-tag
                v-for="tag in problemDetail.typeList"
                :key="tag.id || tag"
                size="small"
                type="info"
                class="problem-tag"
              >
                {{ tag.name || tag }}
              </el-tag>
            </div>

            <!-- 顶部菜单栏：题目描述 / 题解 / 提交记录 -->
            <el-tabs v-model="leftTab" class="left-tabs" stretch>
              <el-tab-pane label="题目描述" name="description">
                <div class="tab-content">
                  <div class="problem-description" v-highlight v-html="processedContent"></div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="题解" name="solution">
                <div class="tab-content">
                  <!-- 写题解按钮 -->
                  <div class="solution-header">
                    <el-button type="primary" size="small" @click="goCreateSolution">
                      <el-icon><Edit /></el-icon> 写题解
                    </el-button>
                  </div>

                  <div v-if="solutionLoading && solutionList.length === 0">
                    <el-skeleton :rows="5" animated></el-skeleton>
                  </div>
                  <div v-else-if="solutionList.length === 0">
                    <el-empty description="暂无题解，快来写第一篇吧" />
                  </div>
                  <div v-else class="solution-list">
                    <!-- 力扣风格题解卡片 -->
                    <div
                      v-for="item in solutionList"
                      :key="item.id"
                      class="solution-card"
                      @click="goSolutionDetail(item.id)"
                    >
                      <div class="solution-card-header">
                        <div class="solution-author">
                          <el-avatar :size="28" :style="{ backgroundColor: '#409eff' }">
                            {{ (item.username || '用户').charAt(0) }}
                          </el-avatar>
                          <span class="author-name">{{ item.username || '匿名用户' }}</span>
                        </div>
                      </div>
                      <div class="solution-card-title">{{ item.title }}</div>
                      <div class="solution-card-preview" v-html="item.contentHtml || item.content"></div>
                      <div class="solution-card-footer">
                        <span class="footer-item">
                          <el-icon><CaretTop /></el-icon> {{ item.likeCount || 0 }}
                        </span>
                        <span class="footer-item">
                          <el-icon><View /></el-icon> {{ item.viewCount || 0 }}
                        </span>
                        <span class="footer-item">
                          <el-icon><ChatDotRound /></el-icon> {{ item.commentCount || 0 }}
                        </span>
                      </div>
                    </div>

                    <!-- 底部加载状态 -->
                    <div class="load-more" v-if="solutionLoading">
                      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
                    </div>
                    <div class="load-more" v-else-if="!hasMoreSolutions">
                      <span style="color: #999; font-size: 13px;">没有更多题解了</span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="提交记录" name="submissions">
                <div class="tab-content">
                  <div v-if="submissionsLoading">
                    <el-skeleton :rows="5" animated></el-skeleton>
                  </div>
                  <div v-else-if="submissionsList.length === 0">
                    <el-empty description="暂无提交记录" />
                  </div>
                  <div v-else>
                    <el-table :data="submissionsList" style="width: 100%" size="small">
                      <el-table-column prop="submissionId" label="提交ID" width="80" />
                      <el-table-column prop="status" label="状态" width="130">
                        <template #default="scope">
                          <span :style="{ color: scope.row.status === 'Accepted' ? '#00b8a3' : '#ff375f', fontWeight: 600 }">
                            {{ getStatusLabel(scope.row.status) }}
                          </span>
                        </template>
                      </el-table-column>
                      <el-table-column label="通过用例" width="100">
                        <template #default="scope">
                          {{ scope.row.testCasesPassed }} / {{ scope.row.testCasesTotal }}
                        </template>
                      </el-table-column>
                      <el-table-column label="执行用时" width="100">
                        <template #default="scope">
                          {{ scope.row.runtimeMs != null ? scope.row.runtimeMs + ' ms' : '-' }}
                        </template>
                      </el-table-column>
                      <el-table-column label="内存消耗" width="110">
                        <template #default="scope">
                          {{ scope.row.memoryKb != null ? (scope.row.memoryKb / 1024).toFixed(1) + ' MB' : '-' }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="submitTime" label="提交时间" min-width="160" />
                    </el-table>
                  </div>
                </div>
              </el-tab-pane>

              <!-- 讨论 tab -->
              <el-tab-pane label="讨论" name="discussion">
                <div class="tab-content">
                  <!-- 发表评论输入框 -->
                  <div class="discussion-input">
                    <el-input
                      v-model="discussionContent"
                      type="textarea"
                      :rows="3"
                      placeholder="参与讨论，写下你的想法..."
                      maxlength="500"
                      show-word-limit
                    />
                    <div class="discussion-input-actions">
                      <el-button type="primary" size="small" :loading="discussionSubmitting" @click="handlePostDiscussion">
                        发表评论
                      </el-button>
                    </div>
                  </div>

                  <!-- 评论列表 -->
                  <div v-if="discussionLoading && discussionList.length === 0">
                    <el-skeleton :rows="5" animated></el-skeleton>
                  </div>
                  <div v-else-if="discussionList.length === 0">
                    <el-empty description="暂无讨论，快来发表第一条评论吧" />
                  </div>
                  <div v-else class="discussion-list">
                    <div v-for="item in discussionList" :key="item.id" class="discussion-item">
                      <el-avatar :size="32" :style="{ backgroundColor: '#409eff' }">
                        {{ (item.username || '用户').charAt(0) }}
                      </el-avatar>
                      <div class="discussion-body">
                        <div class="discussion-header">
                          <span class="discussion-username">{{ item.username || '匿名用户' }}</span>
                          <span class="discussion-time">{{ item.createTime }}</span>
                        </div>
                        <div class="discussion-text">{{ item.content }}</div>
                      </div>
                    </div>

                    <!-- 底部加载状态 -->
                    <div class="load-more" v-if="discussionLoading">
                      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
                    </div>
                    <div class="load-more" v-else-if="!hasMoreDiscussions">
                      <span style="color: #999; font-size: 13px;">没有更多评论了</span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>

            <!-- 操作按钮 -->
            <div class="problem-actions">
              <el-button type="primary" size="small" class="action-btn">
                <i class="el-icon-star-off"></i> 收藏
              </el-button>
              <el-button size="small" class="action-btn">
                <i class="el-icon-share"></i> 分享
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧：代码编辑器 + AI 面板 -->
        <div class="right-section">
          <div class="code-editor-section" :class="{ 'with-ai': showAiPanel }">
            <div v-if="!loading && problemDetail" class="editor-header">
              <h2>代码编辑器</h2>
              <el-button
                :type="showAiPanel ? 'warning' : 'primary'"
                size="small"
                @click="showAiPanel = !showAiPanel"
                class="ai-toggle-btn"
              >
                <span class="ai-btn-icon">🤖</span>
                {{ showAiPanel ? '收起 AI' : 'AI 判题' }}
              </el-button>
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
            ></CodeEditor>
            <div v-else-if="loading" class="loading-editor">
              <el-skeleton :rows="10" animated></el-skeleton>
            </div>
          </div>

          <!-- AI 对话面板 -->
          <transition name="ai-slide">
            <div v-if="showAiPanel && !loading && problemDetail" class="ai-panel-wrapper">
              <AiChatPanel
                :problem-id="problemDetail.id"
                :problem-title="problemDetail.title"
                :problem-difficulty="problemDetail.difficulty"
                :code="code"
                :language="selectedLanguage"
                :last-judge-result="editorResult"
                @close="showAiPanel = false"
                @judgeComplete="onAiJudgeComplete"
              />
            </div>
          </transition>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { getProblemDetail, runCode, submitCode } from '../services/api'
import { getSubmissionsApi, createSubmissionPoller } from '../api/submissions'
import { getSolutionListApi, getProblemCommentsApi, postCommentApi } from '../api/solutions'
import { submitAiJudge, createEventSource } from '../api/ai'
import { Edit, CaretTop, View, ChatDotRound, Loading } from '@element-plus/icons-vue'
import CodeEditor from '../components/CodeEditor.vue'
import AiChatPanel from '../components/AiChatPanel.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const problemDetail = ref(null)
const loading = ref(true)
const error = ref('')
const code = ref('')
const selectedLanguage = ref('Java')
const editorResult = ref(null)
const leftTab = ref('description')
const solutionLoading = ref(false)
const solutionList = ref([])
const solutionLastId = ref(Date.now())
const solutionOffset = ref(0)
const hasMoreSolutions = ref(true)
const submissionsLoading = ref(false)
const submissionsList = ref([])
const problemInfoRef = ref(null)

// AI 面板
const showAiPanel = ref(false)

// 异步判题轮询降级（WebSocket 断连时使用）
let judgePoller = null

// 讨论评论相关
const discussionContent = ref('')
const discussionSubmitting = ref(false)
const discussionLoading = ref(false)
const discussionList = ref([])
const discussionLastId = ref(Date.now())
const discussionOffset = ref(0)
const hasMoreDiscussions = ref(true)

const getStatusLabel = (status) => {
  const map = {
    'Accepted': '通过',
    'Wrong Answer': '答案错误',
    'Compile Error': '编译错误',
    'Runtime Error': '运行时错误',
    'Time Limit Exceeded': '超时',
    'Memory Limit Exceeded': '内存超限'
  }
  return map[status] || status
}

// 加载当前题目的提交记录
const loadSubmissions = async () => {
  const id = route.params.id
  if (!id) return
  try {
    submissionsLoading.value = true
    const res = await getSubmissionsApi(id)
    submissionsList.value = res.data || []
  } catch (e) {
    console.error('获取提交记录失败:', e)
    submissionsList.value = []
  } finally {
    submissionsLoading.value = false
  }
}

// 切换到"提交记录"tab 时自动加载
watch(leftTab, (val) => {
  if (val === 'submissions') {
    loadSubmissions()
  }
  if (val === 'solution' && solutionList.value.length === 0) {
    loadSolutions()
  }
  if (val === 'discussion' && discussionList.value.length === 0) {
    loadDiscussions()
  }
})

// 加载题解列表（滚动分页）
const loadSolutions = async () => {
  const problemId = route.params.id
  if (!problemId) return
  try {
    solutionLoading.value = true
    const res = await getSolutionListApi(problemId, solutionLastId.value, solutionOffset.value)
    const data = res.data
    if (!data || !data.list || data.list.length === 0) {
      hasMoreSolutions.value = false
      return
    }
    solutionList.value.push(...data.list)
    solutionLastId.value = data.minTime
    solutionOffset.value = data.offset
    // 不足一页说明没有更多了（后端默认每页5条）
    if (data.list.length < 5) {
      hasMoreSolutions.value = false
    }
    // 内容不足以撑满滚动容器时，自动加载下一页
    nextTick(() => {
      const el = problemInfoRef.value
      if (el && el.scrollHeight <= el.clientHeight && hasMoreSolutions.value) {
        loadSolutions()
      }
    })
  } catch (e) {
    console.error('获取题解列表失败:', e)
  } finally {
    solutionLoading.value = false
  }
}

// 加载更多题解
const loadMoreSolutions = () => {
  loadSolutions()
}

// 滚动到底部自动加载更多
const handleScroll = () => {
  const el = problemInfoRef.value
  if (!el) return
  // 距离底部不足 100px 时触发加载
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 100) {
    if (leftTab.value === 'solution' && !solutionLoading.value && hasMoreSolutions.value) {
      loadMoreSolutions()
    }
    if (leftTab.value === 'discussion' && !discussionLoading.value && hasMoreDiscussions.value) {
      loadDiscussions()
    }
  }
}

// 跳转发布题解
const goCreateSolution = () => {
  router.push(`/solution/create/${route.params.id}`)
}

// 跳转题解详情
const goSolutionDetail = (id) => {
  router.push(`/solution/${id}`)
}

// ========== 讨论（题目评论）==========
// 加载讨论评论（滚动分页）
const loadDiscussions = async () => {
  const problemId = route.params.id
  if (!problemId) return
  try {
    discussionLoading.value = true
    const res = await getProblemCommentsApi(problemId, discussionLastId.value, discussionOffset.value)
    const data = res.data
    if (!data || !data.list || data.list.length === 0) {
      hasMoreDiscussions.value = false
      return
    }
    discussionList.value.push(...data.list)
    discussionLastId.value = data.minTime
    discussionOffset.value = data.offset
    if (data.list.length < 5) {
      hasMoreDiscussions.value = false
    }
    // 内容不足以撑满滚动容器时，自动加载下一页
    nextTick(() => {
      const el = problemInfoRef.value
      if (el && el.scrollHeight <= el.clientHeight && hasMoreDiscussions.value) {
        loadDiscussions()
      }
    })
  } catch (e) {
    console.error('获取讨论评论失败:', e)
  } finally {
    discussionLoading.value = false
  }
}

// 发表讨论评论
const handlePostDiscussion = async () => {
  if (!discussionContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    discussionSubmitting.value = true
    await postCommentApi({
      problemId: route.params.id,
      parentId: 0,
      content: discussionContent.value.trim()
    })
    ElMessage.success('评论发表成功')
    discussionContent.value = ''
    // 重新加载评论列表
    discussionList.value = []
    discussionLastId.value = Date.now()
    discussionOffset.value = 0
    hasMoreDiscussions.value = true
    loadDiscussions()
  } catch (e) {
    ElMessage.error(e.message || '评论失败')
  } finally {
    discussionSubmitting.value = false
  }
}

const processedContent = computed(() => {
  const content = problemDetail.value?.contentHtml || problemDetail.value?.content
  if (!content) return ''
  
  let result = content
  
  result = result.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
  result = result.replace(/https:\/\/assets\.gitee\.com\//g, '/gitee-assets/')
  
  return result
})

const handleLanguageChange = (language) => {
  selectedLanguage.value = language
  const template = getLanguageTemplate(language, problemDetail.value?.templateCode)
  if (template) code.value = template
}

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
    Java: `class Solution {
    public Object solve() {
        // 在这里编写你的代码
        return null;
    }
}`,
    Python: `class Solution:
    def solve(self):
        # 在这里编写你的代码
        pass`,
    'C++': `class Solution {
public:
    void solve() {
        // 在这里编写你的代码
    }
};`,
    JavaScript: `/**
 * @return {any}
 */
var solve = function() {
    // 在这里编写你的代码
    return null;
};`
  }
  return defaultTemplates[language] || ''
}

const getDifficultyType = (difficulty) => {
  const typeMap = {
    'Easy': 'success',
    'Medium': 'warning',
    'Hard': 'danger'
  }
  return typeMap[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty) => {
  const map = { 'Easy': '简单', 'Medium': '中等', 'Hard': '困难' }
  return map[difficulty] || difficulty
}

const fetchProblemDetail = async () => {
  try {
    loading.value = true
    error.value = ''
    const id = route.params.id
    const data = await getProblemDetail(id)
    problemDetail.value = data
    if (data?.templateCode) {
      code.value = getLanguageTemplate(selectedLanguage.value, data.templateCode)
    }
  } catch (err) {
    error.value = err.message || '获取题目详情失败'
  } finally {
    loading.value = false
  }
}

const handleRunCode = async ({ problemId, code, language }) => {
  try {
    editorResult.value = null
    const data = await runCode({ problemId, code, language })
    editorResult.value = {
      status: data.status || 'Result',
      message: data.stdout || data.errorInfo || '执行完成',
      runtime: data.runtimeMs != null ? data.runtimeMs + ' ms' : undefined,
      memory: data.memoryKb != null ? data.memoryKb + ' KB' : undefined
    }
  } catch (err) {
    editorResult.value = {
      status: 'Error',
      message: err?.message || '运行失败'
    }
  }
}

const handleSubmitCode = async ({ problemId, code, language }) => {
  try {
    editorResult.value = null
    // 取消上一次轮询（如果有）
    if (judgePoller) {
      judgePoller.cancel()
      judgePoller = null
    }

    // 判断是否有测试用例
    const hasTestCases = problemDetail.value?.testCaseList && problemDetail.value.testCaseList.length > 0

    if (!hasTestCases) {
      // 无测试用例，调用 AI 判题接口
      await handleAiJudgeSubmit({ problemId, code, language })
      return
    }

    // 有测试用例，调用正常判题接口
    const data = await submitCode({ problemId, code, language })

    // 语法错误：AI 检测到语法问题，直接返回，不进入判题机
    if (data.status === 'Syntax Error') {
      editorResult.value = {
        status: 'Syntax Error',
        message: data.errorInfo || '代码存在语法错误',
        statusType: 'syntax-error'
      }
      return
    }

    // 限流：60秒内提交次数超过限制
    if (data.status === 'Rate Limited') {
      editorResult.value = {
        status: 'Rate Limited',
        message: data.errorInfo || '提交过于频繁，请稍后再试',
        statusType: 'rate-limited'
      }
      ElMessage.warning(data.errorInfo || '提交过于频繁，请稍后再试')
      return
    }

    // 有提交正在判题中
    if (data.status === 'Processing') {
      editorResult.value = {
        status: 'Processing',
        message: data.errorInfo || '有提交正在判题中，请等待判题完成后再提交',
        statusType: 'processing',
        submitToken: data.submitToken
      }
      ElMessage.info(data.errorInfo || '有提交正在判题中，请等待判题完成后再提交')
      return
    }

    // 异步判题：后端返回 Pending，等 WebSocket 推送最终结果
    // 同时启动轮询降级，防止 WebSocket 断连导致收不到结果
    if (data.status === 'Pending') {
      editorResult.value = {
        status: 'Judging',
        message: data.errorInfo || '代码已提交，正在排队判题中...',
        statusType: 'judging'
      }

      // 启动轮询降级：如果 WebSocket 未在超时内推送结果，通过轮询提交列表获取
      judgePoller = createSubmissionPoller(problemId, {
        interval: 2500,
        timeout: 120000,
        submitToken: data.submitToken,  // 精确匹配本次提交，防止拿到旧的 AI Error 记录
        onResult: (result) => {
          // 只有当前还处于 Judging 状态时才更新（避免与 WebSocket 冲突）
          if (editorResult.value?.statusType === 'judging') {
            const passInfo = result.testCasesPassed != null && result.testCasesTotal != null
              ? `通过 ${result.testCasesPassed}/${result.testCasesTotal} 个测试用例`
              : ''
            editorResult.value = {
              status: result.status || 'Unknown',
              message: result.errorInfo || passInfo || '判题完成',
              runtime: result.runtimeMs != null ? result.runtimeMs + ' ms' : undefined,
              memory: result.memoryKb != null ? result.memoryKb + ' KB' : undefined
            }
            // 刷新提交记录
            loadSubmissions()
            // 弹出提示
            if (result.status === 'Accepted') {
              ElMessage.success('🎉 恭喜，代码通过了所有测试用例！')
            } else {
              ElMessage.warning(`判题结果: ${result.status}`)
            }
          }
          judgePoller = null
        },
        onError: (err) => {
          if (editorResult.value?.statusType === 'judging') {
            editorResult.value = {
              status: 'Error',
              message: err?.message || '判题超时，请在提交记录中查看结果'
            }
          }
          judgePoller = null
        }
      })
      return
    }

    // 兜底：同步返回结果（比如 AI 判题等场景）
    const passInfo = data.testCasesPassed != null && data.testCasesTotal != null
      ? `通过 ${data.testCasesPassed}/${data.testCasesTotal} 个测试用例`
      : ''
    editorResult.value = {
      status: data.status || 'Submitted',
      message: data.errorInfo || passInfo || '提交完成',
      runtime: data.runtimeMs != null ? data.runtimeMs + ' ms' : undefined,
      memory: data.memoryKb != null ? data.memoryKb + ' KB' : undefined
    }
  } catch (err) {
    editorResult.value = {
      status: 'Error',
      message: err?.message || '提交失败'
    }
  }
}

// AI 判题提交处理（无测试用例时调用）
const handleAiJudgeSubmit = async ({ problemId, code, language }) => {
  editorResult.value = {
    status: 'Judging',
    message: '正在使用 AI 判题中...',
    statusType: 'ai-judging'
  }

  let eventSource = null
  try {
    const res = await submitAiJudge({ problemId, code, language })
    const token = res.data

    eventSource = createEventSource('/user/ai/judge/stream/' + token)

    let resultContent = ''

    eventSource.onmessage = (event) => {
      const chunk = event.data
      if (chunk && chunk !== '[DONE]') {
        resultContent += chunk
        editorResult.value = {
          status: 'AI Judging',
          message: resultContent || 'AI 正在分析代码...',
          statusType: 'ai-judging'
        }
      }
    }

    eventSource.onerror = () => {
      eventSource.close()
      if (resultContent) {
        editorResult.value = {
          status: 'AI Judged',
          message: resultContent,
          statusType: 'ai-judged'
        }
      } else {
        editorResult.value = {
          status: 'Error',
          message: 'AI 判题服务暂时不可用，请稍后再试',
          statusType: 'error'
        }
      }
    }

    eventSource.addEventListener('close', () => {
      eventSource.close()
      if (resultContent) {
        editorResult.value = {
          status: 'AI Judged',
          message: resultContent,
          statusType: 'ai-judged'
        }
      }
    })

    setTimeout(() => {
      if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
        eventSource.close()
        if (!resultContent) {
          editorResult.value = {
            status: 'Error',
            message: 'AI 判题超时，请稍后再试',
            statusType: 'error'
          }
        }
      }
    }, 120000)

  } catch (err) {
    if (eventSource) eventSource.close()
    editorResult.value = {
      status: 'Error',
      message: err?.message || 'AI 判题提交失败'
    }
  }
}

// ============ WebSocket 接收判题结果 ============
let ws = null

const initWebSocket = () => {
  const username = localStorage.getItem('username') || 'user_' + Date.now()
  const wsUrl = `ws://localhost:8080/ws/${username}`
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('用户端 WebSocket 连接已建立')
  }

  ws.onmessage = (event) => {
    console.log('收到 WebSocket 消息:', event.data)
    try {
      const data = JSON.parse(event.data)

      // 只处理 judge_result 类型的消息
      if (data.type === 'judge_result') {
        // 检查是否是当前题目的判题结果
        const currentProblemId = problemDetail.value?.id
        if (currentProblemId && String(data.problemId) === String(currentProblemId)) {
          // WebSocket 收到结果，取消轮询降级
          if (judgePoller) {
            judgePoller.cancel()
            judgePoller = null
          }

          const passInfo = data.testCasesPassed != null && data.testCasesTotal != null
            ? `通过 ${data.testCasesPassed}/${data.testCasesTotal} 个测试用例`
            : ''
          
          // 直接设置最终结果，避免中间状态
          editorResult.value = {
            status: data.status || 'Unknown',
            message: data.errorInfo || passInfo || '判题完成',
            runtime: data.runtimeMs != null ? data.runtimeMs + ' ms' : undefined,
            memory: data.memoryKb != null ? data.memoryKb + ' KB' : undefined
          }

          // 自动刷新提交记录
          loadSubmissions()

          // 根据结果弹出提示
          if (data.status === 'Accepted') {
            ElMessage.success('🎉 恭喜，代码通过了所有测试用例！')
          } else {
            ElMessage.warning(`判题结果: ${data.status}`)
          }
        }
      }
    } catch (e) {
      // 非 JSON 消息（如定时心跳消息），忽略
      console.log('收到非 JSON 消息，忽略:', event.data)
    }
  }

  ws.onclose = () => {
    console.log('WebSocket 连接已关闭，3秒后重连...')
    setTimeout(() => {
      initWebSocket()
    }, 3000)
  }

  ws.onerror = (error) => {
    console.error('WebSocket 错误:', error)
  }
}

// AI 判题完成回调 — 刷新提交记录
const onAiJudgeComplete = () => {
  if (leftTab.value === 'submissions') {
    loadSubmissions()
  }
}

watch(
  () => route.params.id,
  () => {
    fetchProblemDetail()
  }
)

onMounted(() => {
  userStore.restoreUser()
  fetchProblemDetail()
  initWebSocket()
})

onUnmounted(() => {
  // 取消轮询
  if (judgePoller) {
    judgePoller.cancel()
    judgePoller = null
  }
  // 关闭 WebSocket
  if (ws) {
    ws.onclose = null  // 防止触发重连
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.problem-detail-container {
  height: calc(100vh - 50px);
  display: flex;
  flex-direction: column;
  background-color: white;
  overflow: hidden;
}

.problem-content {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.content-wrapper-oj {
  width: 100%;
  max-width: 1800px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.2fr 1.8fr;
  gap: 20px;
  height: 100%;
}

.tab-content {
  margin-top: 16px;
}

.submissions-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.submissions-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #f0f2f5;
  border-radius: 6px;
  background: #fafafa;
}

.submissions-time {
  color: #666;
  font-size: 13px;
}

.submissions-status {
  font-weight: 600;
  color: #333;
}

.problem-info {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.loading-state, .loading-editor {
  padding: 40px;
}

.error-state {
  padding: 20px;
  text-align: center;
}

.retry-btn {
  margin-top: 16px;
}

.problem-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.problem-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.problem-title {
  font-size: 20px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.problem-id {
  font-weight: 600;
  color: #666;
}

.problem-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.difficulty-tag {
  margin-right: 8px;
}

.acceptance-rate {
  font-size: 14px;
  color: #4caf50;
  font-weight: 500;
}

.problem-tags {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.problem-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.problem-description {
  margin-bottom: 24px;
  line-height: 1.6;
  color: #333;
}

.problem-description h1, .problem-description h2, .problem-description h3 {
  margin-top: 20px;
  margin-bottom: 10px;
}

.problem-description p {
  margin-bottom: 10px;
}

.problem-description code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.problem-description pre {
  background-color: #f6f8fa !important;
  border: 1px solid #e1e4e8 !important;
  border-radius: 6px !important;
  padding: 16px !important;
  overflow-x: auto !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  margin: 16px 0 !important;
  display: block !important;
  width: 100% !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
}

.problem-description pre code {
  color: #333 !important;
  background-color: transparent !important;
  padding: 0 !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  display: block !important;
  width: 100% !important;
}

.problem-description ul, .problem-description ol {
  margin-bottom: 16px;
  padding-left: 24px;
}

.problem-description img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 10px 0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.problem-description table {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
}

.problem-description th, .problem-description td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.problem-description th {
  background-color: #f5f5f5;
}

.problem-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  margin-bottom: 8px;
}

/* ========== 右侧区域（编辑器 + AI 面板）========== */
.right-section {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.code-editor-section {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  flex: 1;
  min-width: 0;
  transition: flex 0.3s ease;
}

.code-editor-section.with-ai {
  flex: 1.2;
}

.ai-panel-wrapper {
  width: 360px;
  min-width: 320px;
  max-width: 400px;
  height: 100%;
  flex-shrink: 0;
}

/* AI 面板滑入动画 */
.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: all 0.3s ease;
}

.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
  width: 0;
  min-width: 0;
  overflow: hidden;
}

.code-editor-section :deep(.code-editor-container) {
  height: 500px;
  flex: 1;
}

.editor-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
  background-color: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-header h2 {
  font-size: 18px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.ai-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 20px;
  font-weight: 500;
}

.ai-btn-icon {
  font-size: 16px;
}

@media (max-width: 1200px) {
  .content-wrapper-oj {
    grid-template-columns: 1fr;
    height: auto;
    overflow-y: visible;
  }

  .problem-info {
    position: static;
    height: auto;
    max-height: 60vh;
    overflow-y: auto;
  }

  .right-section {
    flex-direction: column;
    height: auto;
  }

  .code-editor-section {
    position: static;
    height: auto;
    max-height: none;
    overflow-y: visible;
  }

  .ai-panel-wrapper {
    width: 100%;
    min-width: unset;
    max-width: none;
    height: 400px;
  }

  .problem-detail-container {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }

  .problem-content {
    overflow: visible;
  }
}

/* ========== 力扣风格题解卡片 ========== */
.solution-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.solution-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.solution-card {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.solution-card:hover {
  background: #fafafa;
  border-color: #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.solution-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.solution-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 13px;
  color: #666;
}

.solution-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
  line-height: 1.4;
}

.solution-card-preview {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  max-height: 4.8em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  margin-bottom: 12px;
}

.solution-card-preview p {
  margin: 0;
}

.solution-card-preview pre {
  display: none;
}

.solution-card-preview img {
  display: none;
}

.solution-card-footer {
  display: flex;
  gap: 20px;
  color: #8c8c8c;
  font-size: 13px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.load-more {
  text-align: center;
  padding: 12px 0;
}

/* ========== 讨论区样式 ========== */
.discussion-input {
  margin-bottom: 24px;
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.discussion-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.discussion-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.discussion-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;
}

.discussion-item:last-child {
  border-bottom: none;
}

.discussion-body {
  flex: 1;
  min-width: 0;
}

.discussion-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.discussion-username {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.discussion-time {
  font-size: 12px;
  color: #bbb;
  margin-left: auto;
}

.discussion-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  word-break: break-word;
}
</style>
