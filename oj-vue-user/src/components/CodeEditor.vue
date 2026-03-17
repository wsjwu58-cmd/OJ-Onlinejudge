<template>
  <div class="code-editor-container">
    <div class="editor-toolbar">
      <div class="language-selector">
        <el-select 
          v-model="selectedLanguage" 
          placeholder="选择语言" 
          size="small"
          @change="handleLanguageChange"
        >
          <el-option label="Java" value="Java"></el-option>
          <el-option label="Python" value="Python"></el-option>
          <el-option label="C++" value="C++"></el-option>
          <el-option label="JavaScript" value="JavaScript"></el-option>
        </el-select>
      </div>
      <div class="editor-actions">
        <el-button 
          type="primary" 
          size="small" 
          @click="handleRun"
          :loading="running"
          :disabled="!canRun"
        >
          运行代码
        </el-button>
        <el-button 
          type="success" 
          size="small" 
          @click="handleSubmit"
          :loading="submitting"
          :disabled="!canSubmit"
        >
          提交解答
        </el-button>
      </div>
    </div>
    <div ref="editorContainer" class="editor-content"></div>
    <!-- 结果展示区域 -->
    <div v-if="props.result" class="result-container" :class="resultClass">
      <div class="result-header">
        <div class="result-status">
          <!-- Judging 状态旋转动画 -->
          <el-icon v-if="isJudging" class="is-loading result-loading-icon"><Loading /></el-icon>
          <span v-if="props.result.status === 'Accepted'" class="result-emoji">✅</span>
          <span v-else-if="props.result.status === 'Syntax Error'" class="result-emoji">⚠️</span>
          <span v-else-if="props.result.status === 'Rate Limited'" class="result-emoji">⏱️</span>
          <span v-else-if="props.result.status === 'Processing'" class="result-emoji">⏳</span>
          <span v-else-if="props.result.status === 'AI Judging' || props.result.status === 'AI Judged'" class="result-emoji">🤖</span>
          <span v-else-if="isJudging" class="result-emoji">⏳</span>
          <span v-else-if="isError" class="result-emoji">❌</span>
          <h4>{{ resultLabel }}</h4>
        </div>
        <span class="result-meta" v-if="props.result.runtime || props.result.memory">
          {{ props.result.runtime }} | {{ props.result.memory }}
        </span>
      </div>
      <div class="result-content" :class="{ 'judging-text': isJudging, 'ai-result': isAiResult }" v-html="renderedMessage">
      </div>
      <!-- Judging 进度条 -->
      <div v-if="isJudging" class="judging-progress">
        <el-progress :percentage="judgingProgress" :show-text="false" :stroke-width="3" color="#409eff" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, onMounted, onBeforeUnmount, watch, computed, toRaw } from 'vue'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'
import { Loading } from '@element-plus/icons-vue'
import { marked } from 'marked'

// Props
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  result: {
    type: Object,
    default: null
  },
  problemId: {
    type: Number,
    required: true
  },
  language: {
    type: String,
    default: 'Java'
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'run', 'submit', 'languageChange'])

// Refs
const editorContainer = ref(null)
const editor = shallowRef(null)
const selectedLanguage = ref(props.language)
const running = ref(false)
const submitting = ref(false)
const disposables = []

// Computed
const canRun = computed(() => !!props.modelValue.trim() && !running.value && !submitting.value)
const canSubmit = computed(() => !!props.modelValue.trim() && !running.value && !submitting.value)

// 判题状态判断
const isJudging = computed(() => {
  const s = props.result?.status
  return s === 'Pending' || s === 'Judging'
})
const isError = computed(() => {
  const s = props.result?.status
  return s && !['Accepted', 'Pending', 'Judging', 'Result', 'Rate Limited', 'Processing', 'AI Judging', 'AI Judged'].includes(s)
})

const isAiResult = computed(() => {
  const s = props.result?.status
  return s === 'AI Judging' || s === 'AI Judged'
})

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

const renderedMessage = computed(() => {
  const msg = props.result?.message || ''
  if (isAiResult.value) {
    try {
      const cleanedText = cleanAiResponse(msg)
      return marked(cleanedText, { breaks: true })
    } catch (e) {
      return msg
    }
  }
  return msg
})

// 结果区域 CSS 类
const resultClass = computed(() => {
  const s = (props.result?.status || '').toLowerCase()
  if (s === 'accepted') return 'accepted'
  if (s === 'syntax error') return 'syntax-error'
  if (s === 'rate limited') return 'rate-limited'
  if (s === 'processing') return 'processing'
  if (s === 'ai judging' || s === 'ai judged') return 'ai-judging'
  if (s === 'pending' || s === 'judging') return 'pending'
  if (s === 'result') return 'result'
  if (isError.value) return 'error'
  return ''
})

// 结果标签文字
const resultLabel = computed(() => {
  const map = {
    'Accepted': '🎉 通过',
    'Wrong Answer': '答案错误',
    'Compile Error': '编译错误',
    'Runtime Error': '运行时错误',
    'Time Limit Exceeded': '超时',
    'Memory Limit Exceeded': '内存超限',
    'Syntax Error': '语法错误 (AI 检测)',
    'Rate Limited': '⏱️ 提交过于频繁',
    'Processing': '⏳ 判题中',
    'AI Judging': '🤖 AI 判题中',
    'AI Judged': '🤖 AI 判题完成',
    'Pending': '排队中...',
    'Judging': '判题中...',
    'Error': '出错了'
  }
  return map[props.result?.status] || props.result?.status
})

// Judging 模拟进度条
const judgingProgress = ref(0)
let judgingTimer = null
watch(isJudging, (val) => {
  if (val) {
    judgingProgress.value = 0
    judgingTimer = setInterval(() => {
      if (judgingProgress.value < 90) {
        judgingProgress.value += Math.random() * 8
      }
    }, 500)
  } else {
    if (judgingTimer) clearInterval(judgingTimer)
    judgingProgress.value = 100
    setTimeout(() => { judgingProgress.value = 0 }, 300)
  }
})

// Watch for language changes from props
watch(() => props.language, (newLanguage) => {
  selectedLanguage.value = newLanguage
  if (editor.value) {
    const model = editor.value.getModel()
    if (model) monaco.editor.setModelLanguage(model, mapLanguageToMonaco(newLanguage))
  }
})

// Watch for modelValue changes from parent (e.g. template reset)
watch(
  () => props.modelValue,
  (newVal) => {
    if (!editor.value) return
    const current = editor.value.getValue()
    if (newVal !== current) {
      editor.value.setValue(newVal || '')
    }
  }
)

// Methods
const mapLanguageToMonaco = (language) => {
  const map = {
    'Java': 'java',
    'Python': 'python',
    'C++': 'cpp',
    'JavaScript': 'javascript'
  }
  return map[language] || 'java'
}

const initEditor = () => {
  if (!editorContainer.value) return
  
  setTimeout(() => {
    try {
      editor.value = monaco.editor.create(editorContainer.value, {
        value: props.modelValue,
        language: mapLanguageToMonaco(props.language),
        theme: 'vs-dark',
        automaticLayout: true,
        minimap: {
          enabled: true
        },
        scrollBeyondLastLine: false,
        fontSize: 14,
        lineHeight: 22,
        tabSize: 2,
        wordWrap: 'on',
        fontFamily: "JetBrains Mono, Fira Code, Consolas, 'Courier New', monospace",
        fontLigatures: true,
        suggestOnTriggerCharacters: true,
        quickSuggestions: true,
        renderWhitespace: 'selection',
        bracketPairColorization: { enabled: true },
        guides: {
          bracketPairs: true,
          indentation: true
        }
      })
      
      // 监听内容变化，使用toRaw包装编辑器实例
      const disposable = editor.value.onDidChangeModelContent(() => {
        const value = toRaw(editor.value).getValue()
        emit('update:modelValue', value)
      })

      disposables.push(disposable)
    } catch (error) {
      console.error('初始化编辑器失败:', error)
    }
  }, 100)
}

const handleLanguageChange = (language) => {
  // el-select change 已把 v-model 更新到 selectedLanguage，但这里显式同步一次，避免边界状态
  selectedLanguage.value = language

  if (editor.value) {
    const model = editor.value.getModel()
    if (model) monaco.editor.setModelLanguage(model, mapLanguageToMonaco(language))
  }

  emit('languageChange', language)
}

const handleRun = async () => {
  if (!canRun.value) return
  
  running.value = true
  
  try {
    const value = toRaw(editor.value).getValue()
    emit('run', {
      problemId: props.problemId,
      code: value,
      language: selectedLanguage.value
    })
  } catch (error) {
    console.error('运行代码失败:', error)
  } finally {
    running.value = false
  }
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  
  submitting.value = true
  
  try {
    const value = toRaw(editor.value).getValue()
    emit('submit', {
      problemId: props.problemId,
      code: value,
      language: selectedLanguage.value
    })
  } catch (error) {
    console.error('提交代码失败:', error)
  } finally {
    submitting.value = false
  }
}

// Lifecycle hooks
onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.dispose()
  }

  disposables.forEach((d) => {
    try {
      d.dispose()
    } catch (e) {
      // ignore
    }
  })
})
</script>

<style scoped>
.code-editor-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  background-color: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;
  height: 100%;
  min-height: 0;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px;
  background-color: #252526;
  border-bottom: 1px solid #3c3c3c;
}

.language-selector {
  flex: 1;
  max-width: 200px;
  min-width: 160px;
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
  margin-left: auto;
  flex-wrap: wrap;
}

.editor-content {
  flex: 1;
  min-height: 0;
  width: 100%;
}

/* 自定义滚动条样式，解决深蓝色滚动条问题 */
.code-editor-container ::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.code-editor-container ::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.code-editor-container ::-webkit-scrollbar-thumb {
  background: #424242;
  border-radius: 4px;
}

.code-editor-container ::-webkit-scrollbar-thumb:hover {
  background: #666;
}

.result-container {
  padding: 12px;
  margin: 12px;
  border-radius: 8px;
  background-color: #fff;
  border: 1px solid #ddd;
  max-height: 35%;
  overflow: auto;
  transition: all 0.3s ease;
}

.result-container.accepted {
  background-color: #e8f5e8;
  border-color: #c8e6c9;
  color: #2e7d32;
}

.result-container.error {
  background-color: #ffebee;
  border-color: #ffcdd2;
  color: #c62828;
}

.result-container.syntax-error {
  background-color: #fff8e1;
  border-color: #ffe082;
  color: #e65100;
}

.result-container.rate-limited {
  background-color: #fff3e0;
  border-color: #ffcc80;
  color: #e65100;
}

.result-container.processing {
  background-color: #e8eaf6;
  border-color: #c5cae9;
  color: #3949ab;
}

.result-container.ai-judging {
  background-color: #f3e5f5;
  border-color: #ce93d8;
  color: #7b1fa2;
}

.result-container.pending {
  background-color: #e3f2fd;
  border-color: #bbdefb;
  color: #1565c0;
}

.result-container.result {
  background-color: #f3f4f6;
  border-color: #d1d5db;
  color: #374151;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-emoji {
  font-size: 16px;
}

.result-loading-icon {
  font-size: 16px;
  color: #409eff;
}

.result-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.result-meta {
  font-size: 12px;
  opacity: 0.8;
}

.result-content {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.result-content.judging-text {
  animation: pulse-text 1.5s ease-in-out infinite;
}

@keyframes pulse-text {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.judging-progress {
  margin-top: 8px;
}

.result-content.ai-result {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  white-space: normal;
}

.result-content.ai-result :deep(h1),
.result-content.ai-result :deep(h2),
.result-content.ai-result :deep(h3) {
  margin: 10px 0 6px;
  color: inherit;
}

.result-content.ai-result :deep(h1) { font-size: 16px; }
.result-content.ai-result :deep(h2) { font-size: 15px; }
.result-content.ai-result :deep(h3) { font-size: 14px; }

.result-content.ai-result :deep(p) {
  margin: 4px 0;
}

.result-content.ai-result :deep(code) {
  background: rgba(123, 31, 162, 0.1);
  padding: 2px 5px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.result-content.ai-result :deep(pre) {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  padding: 10px;
  overflow-x: auto;
  margin: 8px 0;
}

.result-content.ai-result :deep(pre code) {
  background: transparent;
  padding: 0;
}

.result-content.ai-result :deep(ul),
.result-content.ai-result :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

.result-content.ai-result :deep(li) {
  margin: 2px 0;
}

.result-content.ai-result :deep(strong) {
  font-weight: 600;
}
</style>