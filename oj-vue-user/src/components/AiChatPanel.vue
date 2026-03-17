<template>
  <div class="ai-chat-panel">
    <div class="ai-chat-header">
      <div class="ai-header-left">
        <span class="ai-icon">🤖</span>
        <span class="ai-title">AI 判题助手</span>
      </div>
      <el-button type="" size="small" circle @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
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
          <div class="message-header">
            <span class="message-sender">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</span>
            <span class="message-time">{{ msg.time }}</span>
          </div>
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
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { 
  submitAiJudge, 
  submitAiHint, 
  submitAiErrorAnalysis, 
  submitAiChat,
  createEventSource 
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
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.ai-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-icon {
  font-size: 20px;
}

.ai-title {
  font-size: 15px;
  font-weight: 600;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8f9fc;
  min-height: 0;
}

.ai-welcome {
  text-align: center;
  padding: 30px 20px;
  color: #666;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.ai-welcome h3 {
  font-size: 18px;
  color: #333;
  margin-bottom: 12px;
}

.ai-welcome p {
  font-size: 14px;
  margin: 4px 0;
}

.feature-list {
  text-align: left;
  display: inline-block;
  margin-top: 12px;
  font-size: 13px;
  line-height: 2;
}

.feature-list li {
  list-style: none;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
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
  background: #e8ecf1;
}

.chat-message.user .message-avatar {
  background: #e3f2fd;
}

.message-content {
  max-width: 85%;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.chat-message.user .message-header {
  flex-direction: row-reverse;
}

.message-sender {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #aaa;
}

.user-body {
  background: #409eff;
  color: #fff;
  padding: 10px 14px;
  border-radius: 12px 2px 12px 12px;
  font-size: 14px;
}

.user-code-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-text {
  word-break: break-word;
}

.ai-body {
  background: #fff;
  padding: 14px 16px;
  border-radius: 2px 12px 12px 12px;
  border: 1px solid #e8e8e8;
  font-size: 14px;
  line-height: 1.7;
  color: #333;
  word-break: break-word;
  overflow-x: auto;
}

.ai-body :deep(h1),
.ai-body :deep(h2),
.ai-body :deep(h3) {
  margin: 12px 0 8px;
  color: #333;
}

.ai-body :deep(h1) { font-size: 18px; }
.ai-body :deep(h2) { font-size: 16px; }
.ai-body :deep(h3) { font-size: 15px; }

.ai-body :deep(p) {
  margin: 6px 0;
}

.ai-body :deep(strong) {
  color: #333;
}

.ai-body :deep(code) {
  background: #f5f5f5;
  padding: 2px 5px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #c7254e;
}

.ai-body :deep(pre) {
  background: #f6f8fa !important;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.ai-body :deep(pre code) {
  background: transparent !important;
  color: #333 !important;
  padding: 0 !important;
}

.ai-body :deep(ul),
.ai-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.ai-body :deep(li) {
  margin: 3px 0;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.ai-chat-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  flex: 1;
  min-width: 80px;
}

.chat-input-area {
  margin-top: 8px;
}

.ai-chat-messages::-webkit-scrollbar {
  width: 5px;
}

.ai-chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.ai-chat-messages::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}
</style>
