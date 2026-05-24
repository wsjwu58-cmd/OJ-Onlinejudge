<template>
  <div class="solution-detail-page">
    <div class="solution-detail-container">
      <!-- 返回按钮 -->
      <div class="back-bar">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回题目
        </el-button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading">
        <el-skeleton :rows="10" animated />
      </div>

      <!-- 题解内容 -->
      <div v-else-if="solution" class="solution-content-card">
        <div class="solution-meta">
          <div class="meta-left">
            <el-avatar :size="36" :style="{ backgroundColor: '#409eff' }">
              {{ (solution.username || '用户').charAt(0) }}
            </el-avatar>
            <div class="meta-info">
              <span class="meta-username">{{ solution.username || '匿名用户' }}</span>
              <span class="meta-time">{{ solution.createTime }}</span>
            </div>
          </div>
          <div class="meta-right">
            <el-button
              :type="solution.isLiked ? 'primary' : 'default'"
              size="small"
              @click="handleLike"
            >
              <el-icon><CaretTop /></el-icon>
              {{ solution.likeCount || 0 }}
            </el-button>
          </div>
        </div>

        <h1 class="solution-title">{{ solution.title }}</h1>

        <div class="solution-stats">
          <span><el-icon><View /></el-icon> {{ solution.viewCount || 0 }} 次浏览</span>
          <span><el-icon><ChatDotRound /></el-icon> {{ solution.commentCount || 0 }} 条评论</span>
        </div>

        <el-divider />

        <div class="solution-body problem-description" v-highlight v-html="processedSolutionHtml"></div>

        <el-divider />

        <!-- 评论区 -->
        <div class="comment-section">
          <h3>评论 ({{ solution.commentCount || 0 }})</h3>

          <!-- 发表评论 -->
          <div class="comment-input">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论..."
              maxlength="500"
              show-word-limit
            />
            <div class="comment-input-actions">
              <el-button type="primary" size="small" :loading="commentSubmitting" @click="handlePostComment">
                发表评论
              </el-button>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="commentLoading && commentList.length === 0" style="margin-top: 20px;">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="commentList.length === 0" style="margin-top: 20px;">
            <el-empty description="暂无评论" :image-size="60" />
          </div>
          <div v-else class="comment-list">
            <div v-for="comment in commentList" :key="comment.id" class="comment-item">
              <el-avatar :size="28" :style="{ backgroundColor: '#67c23a' }">
                {{ (comment.username || '用户').charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-username">{{ comment.username || '匿名用户' }}</span>
                  <span v-if="comment.replyToUsername" class="comment-reply">
                    回复 <span class="reply-target">{{ comment.replyToUsername }}</span>
                  </span>
                  <span class="comment-time">{{ comment.createTime }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
              </div>
            </div>

            <div class="load-more" v-if="hasMoreComments">
              <el-button :loading="commentLoading" text @click="loadMoreComments">加载更多评论</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 错误 -->
      <div v-else>
        <el-empty description="题解不存在或已被删除" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CaretTop, View, ChatDotRound } from '@element-plus/icons-vue'
import { getSolutionDetailApi, getSolutionCommentsApi, postCommentApi, likeSolutionApi } from '../api/solutions'

const route = useRoute()
const router = useRouter()

const solutionId = route.params.id
const loading = ref(true)
const solution = ref(null)

// 评论相关
const commentContent = ref('')
const commentSubmitting = ref(false)
const commentLoading = ref(false)
const commentList = ref([])
const commentLastId = ref(Date.now())
const commentOffset = ref(0)
const hasMoreComments = ref(true)

// 加载题解详情
const loadSolutionDetail = async () => {
  try {
    loading.value = true
    const res = await getSolutionDetailApi(solutionId)
    solution.value = res.data
  } catch (e) {
    console.error('获取题解详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 处理题解内容：gitee 图片代理
const processedSolutionHtml = computed(() => {
  const html = solution.value?.contentHtml || solution.value?.content || ''
  if (!html) return ''
  let result = html
  result = result.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
  result = result.replace(/https:\/\/assets\.gitee\.com\//g, '/gitee-assets/')
  return result
})

// 加载评论（滚动分页）
const loadComments = async () => {
  try {
    commentLoading.value = true
    const res = await getSolutionCommentsApi(solutionId, commentLastId.value, commentOffset.value)
    const data = res.data
    if (!data || !data.list || data.list.length === 0) {
      hasMoreComments.value = false
      return
    }
    commentList.value.push(...data.list)
    commentLastId.value = data.minTime
    commentOffset.value = data.offset
    if (data.list.length < 10) {
      hasMoreComments.value = false
    }
  } catch (e) {
    console.error('获取评论失败:', e)
  } finally {
    commentLoading.value = false
  }
}

const loadMoreComments = () => {
  loadComments()
}

// 发表评论
const handlePostComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    commentSubmitting.value = true
    await postCommentApi({
      problemId: solution.value.problemId,
      parentId: solutionId,
      content: commentContent.value.trim()
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    // 重新加载评论
    commentList.value = []
    commentLastId.value = Date.now()
    commentOffset.value = 0
    hasMoreComments.value = true
    loadComments()
    // 评论数 +1
    if (solution.value) {
      solution.value.commentCount = (solution.value.commentCount || 0) + 1
    }
  } catch (e) {
    ElMessage.error(e.message || '评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

// 点赞
const handleLike = async () => {
  try {
    await likeSolutionApi(solutionId)
    if (solution.value.isLiked) {
      solution.value.isLiked = false
      solution.value.likeCount = Math.max(0, (solution.value.likeCount || 1) - 1)
    } else {
      solution.value.isLiked = true
      solution.value.likeCount = (solution.value.likeCount || 0) + 1
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadSolutionDetail()
  loadComments()
})
</script>

<style scoped>
.solution-detail-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 24px 0;
}

.solution-detail-container {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 24px;
}

.back-bar {
  margin-bottom: 16px;
}

.solution-content-card {
  background: #fff;
  border-radius: 10px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  max-width: 100%;
  box-sizing: border-box;
}

.solution-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-username {
  font-weight: 600;
  color: #262626;
  font-size: 15px;
}

.meta-time {
  font-size: 12px;
  color: #999;
}

.solution-title {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.solution-stats {
  display: flex;
  gap: 20px;
  color: #8c8c8c;
  font-size: 13px;
}

.solution-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.solution-body {
  line-height: 1.8;
  color: #333;
  font-size: 15px;
  margin-bottom: 0;
  overflow-x: auto;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.solution-body h1, .solution-body h2, .solution-body h3 {
  margin-top: 24px;
  margin-bottom: 12px;
}

.solution-body p {
  margin-bottom: 12px;
}

.solution-body code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.solution-body pre {
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

.solution-body pre code {
  color: #333 !important;
  background-color: transparent !important;
  padding: 0 !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  display: block !important;
  width: 100% !important;
}

.solution-body ul, .solution-body ol {
  margin-bottom: 16px;
  padding-left: 24px;
}

.solution-body img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 10px 0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.solution-body table {
  width: 100%;
  max-width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  display: block;
  overflow-x: auto;
}

.solution-body th, .solution-body td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.solution-body th {
  background-color: #f5f5f5;
}

.solution-body blockquote {
  border-left: 4px solid #dfe2e5;
  margin: 16px 0;
  padding: 8px 16px;
  color: #6a737d;
  background-color: #f8f9fa;
  border-radius: 0 4px 4px 0;
}

/* 评论区 */
.comment-section h3 {
  font-size: 18px;
  color: #262626;
  margin: 0 0 16px 0;
}

.comment-input {
  margin-bottom: 24px;
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-username {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.comment-reply {
  font-size: 13px;
  color: #999;
}

.reply-target {
  color: #409eff;
}

.comment-time {
  font-size: 12px;
  color: #bbb;
  margin-left: auto;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.load-more {
  text-align: center;
  padding: 12px 0;
}
</style>
