<template>
  <div class="solution-create-page">
    <div class="solution-create-container">
      <div class="create-header">
        <h1>写题解</h1>
        <div class="header-actions">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">发布题解</el-button>
        </div>
      </div>

      <div class="create-form">
        <el-input
          v-model="title"
          placeholder="请输入题解标题"
          size="large"
          class="title-input"
          maxlength="100"
          show-word-limit
        />

        <div class="editor-area">
          <div class="editor-toolbar">
            <span class="toolbar-tip">支持 Markdown 格式</span>
            <el-radio-group v-model="previewMode" size="small">
              <el-radio-button label="edit">编辑</el-radio-button>
              <el-radio-button label="preview">预览</el-radio-button>
              <el-radio-button label="both">双栏</el-radio-button>
            </el-radio-group>
          </div>

          <div class="editor-body" :class="previewMode">
            <!-- 编辑区 -->
            <div class="editor-pane" v-show="previewMode !== 'preview'">
              <el-input
                v-model="content"
                type="textarea"
                placeholder="请输入题解内容（支持 Markdown）&#10;&#10;例如：&#10;## 思路&#10;使用哈希表...&#10;&#10;## 代码&#10;```java&#10;class Solution {&#10;    ...&#10;}&#10;```"
                :autosize="{ minRows: 20, maxRows: 40 }"
                resize="none"
              />
            </div>
            <!-- 预览区 -->
            <div class="preview-pane problem-description" v-show="previewMode !== 'edit'" v-highlight v-html="previewHtml">
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postSolutionApi } from '../api/solutions'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()

const problemId = route.params.problemId
const title = ref('')
const content = ref('')
const submitting = ref(false)
const previewMode = ref('edit')

// 配置 marked
marked.setOptions({
  breaks: true,       // 支持换行
  gfm: true           // 支持 GitHub 风格 Markdown
})

// Markdown 预览：marked 渲染 + gitee 图片代理
const previewHtml = computed(() => {
  if (!content.value) return '<p style="color:#999">预览区域</p>'
  let html = marked(content.value)
  // gitee 图片代理（和题目详情保持一致）
  html = html.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
  html = html.replace(/https:\/\/assets\.gitee\.com\//g, '/gitee-assets/')
  return html
})

const handleSubmit = async () => {
  if (!title.value.trim()) {
    ElMessage.warning('请输入题解标题')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入题解内容')
    return
  }

  try {
    submitting.value = true
    await postSolutionApi({
      problemId: problemId,
      parentId: 0,
      title: title.value.trim(),
      content: content.value.trim()
    })
    ElMessage.success('发布成功')
    router.back()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.solution-create-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 32px 0;
}

.solution-create-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
}

.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.create-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.create-form {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.title-input {
  margin-bottom: 20px;
}

.title-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-tip {
  font-size: 13px;
  color: #999;
}

.editor-body {
  display: flex;
  gap: 16px;
}

.editor-body.edit .editor-pane {
  width: 100%;
}

.editor-body.preview .preview-pane {
  width: 100%;
}

.editor-body.both .editor-pane,
.editor-body.both .preview-pane {
  width: 50%;
}

.editor-pane {
  flex: 1;
}

.editor-pane :deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
  padding: 16px;
  border-radius: 8px;
}

.preview-pane {
  flex: 1;
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  min-height: 400px;
  line-height: 1.6;
  overflow-y: auto;
  font-size: 14px;
  color: #333;
}

/* ===== 与题目详情一致的 Markdown 渲染样式 ===== */
.preview-pane.problem-description {
  margin-bottom: 0;
}

.preview-pane.problem-description h1,
.preview-pane.problem-description h2,
.preview-pane.problem-description h3 {
  margin-top: 20px;
  margin-bottom: 10px;
}

.preview-pane.problem-description p {
  margin-bottom: 10px;
}

.preview-pane.problem-description code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.preview-pane.problem-description pre {
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
  box-sizing: border-box !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
}

.preview-pane.problem-description pre code {
  color: #333 !important;
  background-color: transparent !important;
  padding: 0 !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  display: block !important;
  width: 100% !important;
}

.preview-pane.problem-description ul,
.preview-pane.problem-description ol {
  margin-bottom: 16px;
  padding-left: 24px;
}

.preview-pane.problem-description img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 10px 0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.preview-pane.problem-description table {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
}

.preview-pane.problem-description th,
.preview-pane.problem-description td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.preview-pane.problem-description th {
  background-color: #f5f5f5;
}

.preview-pane.problem-description blockquote {
  border-left: 4px solid #dfe2e5;
  margin: 16px 0;
  padding: 8px 16px;
  color: #6a737d;
  background-color: #f8f9fa;
  border-radius: 0 4px 4px 0;
}

.preview-pane.problem-description hr {
  border: none;
  border-top: 1px solid #e1e4e8;
  margin: 24px 0;
}
</style>
