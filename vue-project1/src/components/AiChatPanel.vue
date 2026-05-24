<template>
  <div class="ai-chat-panel">
    <div class="ai-chat-header">
      <div class="ai-header-left">
        <div class="ai-brand">
          <span class="ai-icon">🤖</span>
          <span class="ai-title">{{ isAgentMode ? 'AI Agent 助手' : 'AI 判题助手' }}</span>
        </div>
      </div>
      <div class="ai-header-right">
        <div class="user-info">
          <el-avatar size="32" :icon="UserFilled" :bg-color="'#409EFF'" :text-color="'#fff'" class="user-avatar"></el-avatar>
          <span class="user-name">{{ username }}</span>
        </div>
        <div class="mode-switch-container">
          <el-switch
            v-model="isAgentMode"
            active-text="Agent"
            inactive-text="普通"
            @change="handleModeChange"
            class="custom-mode-switch"
          />
        </div>
        <el-dropdown v-if="isAgentMode" @command="handleAgentTypeChange" trigger="click">
          <el-button type="text" size="small">
            {{ agentTypeLabels[agentType] }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="traditional">
                🤖 传统Agent
              </el-dropdown-item>
              <el-dropdown-item command="langgraph">
                🔗 LangGraph4j 多Agent
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="text" size="small" circle @click="$emit('close')" class="close-button">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="ai-chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="ai-welcome">
        <div class="welcome-icon">🧠</div>
        <h3>AI 判题助手</h3>
        <p>我可以帮助你：</p>
        <ul class="feature-list">
          <li>🔍 错误分析 - 分析判题失败的原因</li>
          <li>💡 获取提示 - 给出解题思路引导</li>
          <li>🤖 AI判题 - 无测试用例时AI分析代码</li>
          <li>💬 自由提问 - 和AI讨论解题方法</li>
        </ul>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="chat-message" :class="msg.role">
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="message-content">
          <div v-if="msg.role === 'user'" class="message-body user-body">
            <div v-if="msg.type === 'code'" class="user-code-info">
              <el-tag size="small" type="info">{{ msg.language }}</el-tag>
              <span>{{ msg.action }}</span>
            </div>
            <div v-else class="user-text">{{ msg.content }}</div>
          </div>
          <div v-else class="message-body ai-body" v-highlight v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.role === 'ai' && msg.loading" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <div class="ai-chat-footer">
      <div class="quick-actions">
        <el-button size="small" @click="handleErrorAnalysis" :loading="analyzing" :disabled="!props.code.trim()">
          🔍 错误分析
        </el-button>
        <el-button size="small" @click="handleGetHint" :loading="gettingHint">
          💡 获取提示
        </el-button>
        <el-button size="small" type="primary" @click="handleAiJudge" :loading="judging" :disabled="!props.code.trim()">
          🤖 AI判题
        </el-button>
      </div>
      
      <div class="chat-input-area">
        <el-input
          v-model="chatInput"
          placeholder="输入问题，和AI讨论解题思路..."
          @keyup.enter="handleChat"
          :disabled="chatting"
        >
          <template #append>
            <el-button @click="handleChat" :loading="chatting" :disabled="!chatInput.trim()">
              <el-icon><ChatLineRound /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Close, UserFilled, ChatLineRound, ArrowDown } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { 
  submitAiJudge, 
  submitAiHint, 
  submitAiErrorAnalysis, 
  submitAiChat,
  agentChat,
  createEventSource,
  createPostEventSource
} from '../api/ai'

const props = defineProps({
  problemId: {
    type: [Number, String],
    required: true
  },
  code: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'Java'
  },
  lastJudgeResult: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'judgeComplete'])

const messagesRef = ref(null)
const messages = ref([])
const chatInput = ref('')
const isAgentMode = ref(false)
const username = ref('我')
const agentType = ref('traditional')
const agentTypeLabels = {
  'traditional': '传统Agent',
  'langgraph': 'LangGraph4j 多Agent'
}

const judging = ref(false)
const analyzing = ref(false)
const gettingHint = ref(false)
const chatting = ref(false)

const cleanAiResponse = (text) => {
  if (!text) return ''
  
  const codeBlockRegex = /```[\s\S]*?```/g
  const codeBlocks = []
  let result = text
  
  result = result.replace(codeBlockRegex, (match) => {
    let cleaned = match
    cleaned = cleaned.replace(/data:/gi, '')
    cleaned = cleaned.replace(/\\n/g, '\n')
    cleaned = cleaned.replace(/\\t/g, '  ')
    cleaned = cleaned.replace(/\\\"/g, '"')
    
    cleaned = cleaned.replace(/"/g, '')
    
    cleaned = cleaned.replace(/\n\s*\n/g, '\n')
    codeBlocks.push(cleaned)
    return `__CODE_BLOCK_${codeBlocks.length - 1}__`
  })
  
  result = result.replace(/data\s*:\s*/gi, '')
  
  result = result.replace(/\\n/g, '\n')
  result = result.replace(/\\t/g, '  ')
  result = result.replace(/\\\"/g, '"')
  
  result = result.replace(/^\s*"\s*/gm, '')
  result = result.replace(/\s*"\s*$/gm, '')
  result = result.replace(/""/g, '')
  
  result = result.replace(/"(\*\*[^*]+\*\*)"/g, '$1')
  result = result.replace(/"([^"\n]{1,50})"/g, '$1')
  
  result = result.replace(/\n\s*\n\s*\n+/g, '\n\n')
  result = result.replace(/^\s+|\s+$/g, '')
  
  codeBlocks.forEach((block, index) => {
    result = result.replace(`__CODE_BLOCK_${index}__`, '\n' + block + '\n')
  })
  
  return result
}

const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    let cleanedText = cleanAiResponse(text)
    let html = marked(cleanedText, { breaks: true })
    html = html.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
    return html
  } catch (e) {
    return text
  }
}

const getNow = () => {
  const d = new Date()
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleSseStream = async (submitFn, streamPath, data, aiIndex) => {
  let eventSource = null
  try {
    const res = await submitFn(data)
    const token = res.data
    
    eventSource = createEventSource(streamPath + token)
    
    eventSource.onmessage = (event) => {
      const chunk = event.data
      if (chunk && chunk !== '[DONE]') {
        messages.value[aiIndex].content += chunk
        scrollToBottom()
      }
    }
    
    let isCompleted = false
    
    eventSource.onerror = (event) => {
      const readyStateFromEvent = event?.target?.readyState
      const readyStateFromSource = eventSource?.readyState
      console.log('SSE onerror 触发, event.target.readyState:', readyStateFromEvent, 'eventSource.readyState:', readyStateFromSource)
      
      if (isCompleted) {
        console.log('SSE 已完成，忽略错误')
        return
      }
      
      isCompleted = true
      
      const isClosed = readyStateFromEvent === EventSource.CLOSED || 
                       readyStateFromEvent === 2 ||
                       readyStateFromSource === EventSource.CLOSED || 
                       readyStateFromSource === 2
      
      if (isClosed) {
        messages.value[aiIndex].loading = false
        console.log('SSE 正常关闭，不显示错误消息')
      } else {
        console.error('SSE 错误:', event)
        
        messages.value[aiIndex].loading = false
      }
      
      if (eventSource) {
        try {
          eventSource.close()
        } catch (e) {
          console.error('关闭 EventSource 时出错:', e)
        }
      }
    }
    
    eventSource.addEventListener('close', () => {
      messages.value[aiIndex].loading = false
      isCompleted = true
      if (eventSource) {
        try {
          eventSource.close()
        } catch (e) {
          console.error('关闭 EventSource 时出错:', e)
        }
      }
      console.log('SSE 关闭事件触发')
    })
    
    setTimeout(() => {
      if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
        try {
          eventSource.close()
        } catch (e) {
          console.error('关闭 EventSource 时出错:', e)
        }
        messages.value[aiIndex].loading = false
        console.log('SSE 超时关闭')
      }
    }, 180000)
    
  } catch (e) {
    console.error('请求失败:', e)
    messages.value[aiIndex].content += `\n\n❌ 请求失败: ${e.message}`
    messages.value[aiIndex].loading = false
  }
}

const handleErrorAnalysis = async () => {
  if (!props.code.trim()) return

  analyzing.value = true

  messages.value.push({
    role: 'user',
    type: 'code',
    language: props.language,
    action: '分析判题错误',
    time: getNow()
  })

  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: getNow(),
    loading: true
  })

  scrollToBottom()

  await handleSseStream(
    submitAiErrorAnalysis,
    '/user/ai/analyze-error/stream/',
    {
      problemId: props.problemId,
      code: props.code,
      language: props.language
    },
    aiIndex
  )
  
  analyzing.value = false
  scrollToBottom()
}

const handleGetHint = async () => {
  gettingHint.value = true

  messages.value.push({
    role: 'user',
    type: 'code',
    language: props.language,
    action: '获取解题提示',
    time: getNow()
  })

  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: getNow(),
    loading: true
  })

  scrollToBottom()

  await handleSseStream(
    submitAiHint,
    '/user/ai/hint/stream/',
    {
      problemId: props.problemId,
      code: props.code,
      language: props.language
    },
    aiIndex
  )
  
  gettingHint.value = false
  scrollToBottom()
}

const handleAiJudge = async () => {
  if (!props.code.trim()) return

  judging.value = true

  messages.value.push({
    role: 'user',
    type: 'code',
    language: props.language,
    action: '提交AI判题',
    time: getNow()
  })

  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: getNow(),
    loading: true
  })

  scrollToBottom()

  await handleSseStream(
    submitAiJudge,
    '/user/ai/judge/stream/',
    {
      problemId: props.problemId,
      code: props.code,
      language: props.language
    },
    aiIndex
  )
  
  judging.value = false
  scrollToBottom()
  emit('judgeComplete')
}

const handleChat = async () => {
  if (!chatInput.value.trim()) return

  if (isAgentMode.value) {
    await handleAgentChat()
  } else {
    await handleNormalChat()
  }
}

const handleNormalChat = async () => {
  const userMessage = chatInput.value.trim()
  chatInput.value = ''
  chatting.value = true

  messages.value.push({
    role: 'user',
    type: 'text',
    content: userMessage,
    time: getNow()
  })

  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: getNow(),
    loading: true
  })

  scrollToBottom()

  await handleSseStream(
    submitAiChat,
    '/user/ai/chat/stream/',
    {
      problemId: props.problemId,
      code: props.code,
      language: props.language,
      message: userMessage
    },
    aiIndex
  )
  
  chatting.value = false
  scrollToBottom()
}

const handleAgentChat = async () => {
  const userMessage = chatInput.value.trim()
  chatInput.value = ''
  chatting.value = true

  messages.value.push({
    role: 'user',
    type: 'text',
    content: userMessage,
    time: getNow()
  })

  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: getNow(),
    loading: true
  })

  scrollToBottom()

  try {
    const loginUserStr = localStorage.getItem('loginUser')
    let sessionId = 'default'
    let userId = null
    if (loginUserStr) {
      try {
        const loginUser = JSON.parse(loginUserStr)
        sessionId = 'user_' + (loginUser.id || loginUser.userId || 'default')
        userId = loginUser.id || loginUser.userId
      } catch (e) {}
    }

    const requestBody = {
      task: userMessage,
      sessionId: sessionId,
      userId: userId,
      problemId: props.problemId,
      code: props.code,
      language: props.language,
      agentType: agentType.value
    }

    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}/api/user/agent/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('token') || ''
      },
      body: JSON.stringify(requestBody)
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    let fullResponse = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      fullResponse += chunk
      messages.value[aiIndex].content += chunk
      scrollToBottom()
    }

    // 检查是否是JSON格式响应
    try {
      const parsedResponse = JSON.parse(fullResponse)
      if (parsedResponse.code === 1 && parsedResponse.data) {
        messages.value[aiIndex].content = parsedResponse.data
      }
    } catch (e) {
      // 不是JSON格式，保持原样
    }

    messages.value[aiIndex].loading = false
  } catch (error) {
    messages.value[aiIndex].content = '❌ Agent对话出错: ' + error.message
    messages.value[aiIndex].loading = false
  }

  chatting.value = false
  scrollToBottom()
}

const handleModeChange = (mode) => {
  messages.value = []
  if (mode) {
    let modeDescription = ''
    if (agentType.value === 'langgraph') {
      modeDescription = '🤖 **LangGraph4j 多Agent模式已启用**\n\n我现在由多个专业Agent组成的工作流驱动：\n\n**🔀 RouterAgent** - 意图识别\n自动分析你的问题，确定由哪个专业Agent处理\n\n**📝 SolutionAgent** - 题解代理\n处理题目相关任务，如题解、解题思路\n\n**🔍 CodeAgent** - 代码分析代理\n处理代码分析和优化任务\n\n**📊 LearningAgent** - 学习分析代理\n分析学习进度、薄弱点\n\n**📚 KnowledgeAgent** - 知识检索代理\n从知识库检索相关内容\n\n**👤 SupervisorAgent** - 监督代理\n整合各Agent结果，生成最终回复\n\n请直接告诉我你需要什么帮助，我会自动路由到最合适的Agent！'
    } else {
      modeDescription = '🤖 **AI Agent模式已启用**\n\n我现在可以自主调用以下工具：\n\n**题解生成**\n- `getProblemDetail`: 获取题目详情\n- `getTestCases`: 获取测试用例\n- `generateSolution`: 生成解题思路和代码\n\n**学情分析**\n- `getUserSubmissionStats`: 获取提交统计\n- `getUserProblemProgress`: 获取题目进度\n- `analyzeWeakness`: 分析薄弱点\n- `generateLearningReport`: 生成学习报告\n\n**AI判题**\n- `analyzeCodeCorrectness`: 分析代码正确性\n- `checkSyntax`: 检查语法错误\n- `analyzeComplexity`: 分析复杂度\n- `suggestImprovements`: 给出改进建议\n\n请告诉我你需要什么帮助！'
    }
    messages.value.push({
      role: 'ai',
      content: modeDescription,
      time: getNow()
    })
  } else {
    messages.value.push({
      role: 'ai',
      content: '🤖 **普通模式已启用**\n\n我可以帮助你：\n- 🔍 错误分析 - 分析判题失败的原因\n- 💡 获取提示 - 给出解题思路引导\n- 🤖 AI判题 - 无测试用例时AI分析代码\n- 💬 自由提问 - 和AI讨论解题方法',
      time: getNow()
    })
  }
  scrollToBottom()
}

const handleAgentTypeChange = (type) => {
  agentType.value = type
  if (isAgentMode.value) {
    messages.value.push({
      role: 'ai',
      content: `已切换到 ${agentTypeLabels[type]}，请继续提问！`,
      time: getNow()
    })
    scrollToBottom()
  }
}
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1a1a1a;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  border: 1px solid #333;
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #2d2d2d;
  color: #fff;
  border-bottom: 1px solid #444;
}

.ai-header-left {
  display: flex;
  align-items: center;
}

.ai-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.user-name {
  font-size: 14px;
  color: #e0e0e0;
  font-weight: 500;
}

.mode-switch-container {
  display: flex;
  align-items: center;
}

.custom-mode-switch {
  --el-switch-on-color: #409eff;
  --el-switch-on-border-color: #409eff;
  --el-switch-off-color: #333;
  --el-switch-off-border-color: #555;
  --el-switch-on-text-color: #fff;
  --el-switch-off-text-color: #999;
  --el-switch-font-size: 12px;
  --el-switch-height: 24px;
  --el-switch-button-size: 20px;
}

.custom-mode-switch :deep(.el-switch__core) {
  border: 1px solid #555;
  transition: all 0.3s ease;
}

.custom-mode-switch :deep(.el-switch__core:hover) {
  border-color: #409eff;
}

.custom-mode-switch :deep(.el-switch__action) {
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.close-button {
  color: #999;
  transition: all 0.3s ease;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.1);
}

.ai-icon {
  font-size: 24px;
}

.ai-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #e0e0e0;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #1a1a1a;
  min-height: 0;
}

.ai-welcome {
  text-align: center;
  padding: 40px 24px;
  color: #999;
}

.welcome-icon {
  font-size: 56px;
  margin-bottom: 20px;
}

.ai-welcome h3 {
  font-size: 22px;
  color: #e0e0e0;
  margin-bottom: 16px;
  font-weight: 600;
}

.ai-welcome p {
  font-size: 14px;
  margin: 8px 0;
  color: #999;
}

.feature-list {
  text-align: left;
  display: inline-block;
  margin-top: 20px;
  font-size: 14px;
  line-height: 2.2;
  color: #ccc;
  padding: 20px 24px;
  background: #2d2d2d;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
}

.feature-list li {
  list-style: none;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  margin-top: 4px;
}

.chat-message.user .message-avatar {
  background: #409eff;
}

.message-content {
  max-width: 85%;
  min-width: 0;
}

.user-body {
  background: #2d2d2d;
  color: #e0e0e0;
  padding: 14px 18px;
  border-radius: 18px 4px 18px 18px;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
}

.user-code-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-code-info .el-tag {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.user-text {
  word-break: break-word;
  line-height: 1.6;
  color: #e0e0e0;
}

.ai-body {
  background: #2d2d2d;
  padding: 16px 20px;
  border-radius: 4px 18px 18px 18px;
  border: 1px solid #333;
  font-size: 14px;
  line-height: 1.75;
  color: #e0e0e0;
  word-break: break-word;
  overflow-x: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.ai-body :deep(h1),
.ai-body :deep(h2),
.ai-body :deep(h3) {
  margin: 16px 0 12px;
  color: #e0e0e0;
  font-weight: 600;
}

.ai-body :deep(h1) { font-size: 19px; }
.ai-body :deep(h2) { font-size: 17px; }
.ai-body :deep(h3) { font-size: 15px; }

.ai-body :deep(p) {
  margin: 8px 0;
  color: #ccc;
}

.ai-body :deep(strong) {
  color: #e0e0e0;
  font-weight: 600;
}

.ai-body :deep(code) {
  background: #333;
  padding: 3px 6px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Monaco', monospace;
  font-size: 13px;
  color: #ff6b6b;
  border: 1px solid #444;
}

.ai-body :deep(pre) {
  background: #333 !important;
  border: 1px solid #444;
  border-radius: 8px;
  padding: 14px;
  overflow-x: auto;
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.ai-body :deep(pre code) {
  background: transparent !important;
  color: #e0e0e0 !important;
  padding: 0 !important;
  border: none !important;
}

.ai-body :deep(ul),
.ai-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
  color: #ccc;
}

.ai-body :deep(li) {
  margin: 4px 0;
  line-height: 1.7;
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 12px 0 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.4; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.ai-chat-footer {
  padding: 16px 20px;
  border-top: 1px solid #333;
  background: #2d2d2d;
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  flex: 1;
  min-width: 100px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
  border: 1px solid #444;
  background: #333;
  color: #e0e0e0;
}

.quick-actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
}

.quick-actions .el-button[type="primary"] {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.quick-actions .el-button[type="primary"]:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}

.chat-input-area {
  margin-top: 12px;
}

.chat-input-area :deep(.el-input-group__append) {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  padding: 0 16px;
  border-radius: 0 8px 8px 0;
}

.chat-input-area :deep(.el-input-group__append):hover {
  background: #66b1ff;
  border-color: #66b1ff;
}

.chat-input-area :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
  background: #333;
  border: 1px solid #444;
  box-shadow: none;
}

.chat-input-area :deep(.el-input__wrapper):hover {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff inset;
}

.chat-input-area :deep(.el-input__wrapper.is-focus) {
  border-color: #409eff !important;
  box-shadow: 0 0 0 1px #409eff inset !important;
}

.chat-input-area :deep(.el-input__inner) {
  color: #e0e0e0;
  background: #333;
}

.ai-chat-messages::-webkit-scrollbar {
  width: 8px;
}

.ai-chat-messages::-webkit-scrollbar-track {
  background: #2d2d2d;
}

.ai-chat-messages::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 4px;
}

.ai-chat-messages::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
