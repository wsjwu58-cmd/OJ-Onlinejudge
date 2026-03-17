<template>
  <div class="group-detail-page">
    <div class="group-detail-container">
      <!-- 题单信息头部 -->
      <div class="group-info-card">
        <div class="group-info-left">
          <div class="breadcrumb">
            <router-link to="/groups" class="back-link">
              <el-icon><ArrowLeft /></el-icon>
              返回题单列表
            </router-link>
          </div>
          <h1 class="group-title">{{ group.title }}</h1>
          <p class="group-desc">{{ group.description }}</p>
          <div class="group-meta">
            <span class="meta-item" v-if="group.difficultyRange">
              <span :class="['diff-badge', getDifficultyClass(group.difficultyRange)]">
                {{ getDifficultyLabel(group.difficultyRange) }}
              </span>
            </span>
            <span class="meta-item">
              <el-icon><Document /></el-icon>
              {{ group.problemCount || problems.length }} 题
            </span>
            <span class="meta-item" v-if="group.estimatedDurationMinutes">
              <el-icon><Timer /></el-icon>
              预计 {{ group.estimatedDurationMinutes }} 分钟
            </span>
          </div>
        </div>
        <div class="group-info-right" v-if="group.progress !== undefined">
          <el-progress 
            type="circle" 
            :percentage="group.progress" 
            :width="100"
            color="#00b8a3"
          />
          <span class="progress-label">完成进度</span>
        </div>
      </div>

      <!-- 题目列表 -->
      <div class="problems-section">
        <h2 class="section-title">题目列表</h2>
        <div class="problems-table">
          <div class="table-header">
            <span class="col-order">#</span>
            <span class="col-status">状态</span>
            <span class="col-title">题目</span>
            <span class="col-difficulty">难度</span>
            <span class="col-acceptance">通过率</span>
          </div>
          <div 
            v-for="(problem, index) in problems" 
            :key="problem.id" 
            class="table-row"
            @click="goToProblem(problem.id)"
          >
            <span class="col-order">{{ index + 1 }}</span>
            <span class="col-status">
              <el-icon v-if="problem.solved" class="solved-icon" color="#00b8a3"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="problem.attempted" class="attempted-icon" color="#ffa116"><WarningFilled /></el-icon>
              <span v-else class="unsolved-dot"></span>
            </span>
            <span class="col-title">
              <span class="problem-id">{{ problem.id }}.</span>
              <span class="problem-name">{{ problem.title }}</span>
            </span>
            <span class="col-difficulty">
              <span :class="['difficulty-tag', getDifficultyClass(problem.difficulty)]">
                {{ getDifficultyLabel(problem.difficulty) }}
              </span>
            </span>
            <span class="col-acceptance">{{ problem.acceptance || 0 }}%</span>
          </div>
          <div v-if="problems.length === 0" class="empty-state">
            <el-empty description="暂无题目" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Document, Timer, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { getGroupDetailApi, getGroupProblemsApi } from '../api/groups'

const route = useRoute()
const router = useRouter()
const groupId = route.params.id

const group = ref({})
const problems = ref([])

const getDifficultyClass = (difficulty) => {
  const map = { 'Easy': 'easy', 'Medium': 'medium', 'Hard': 'hard', 'All': 'all' }
  return map[difficulty] || ''
}

const getDifficultyLabel = (difficulty) => {
  const map = { 'Easy': '简单', 'Medium': '中等', 'Hard': '困难', 'All': '综合' }
  return map[difficulty] || difficulty
}

const goToProblem = (id) => {
  router.push(`/problems/${id}`)
}

const loadGroupDetail = async () => {
  try {
    const res = await getGroupDetailApi(groupId)
    if (res.code === 1) {
      group.value = res.data || {}
    }
  } catch (error) {
    console.error('加载题单详情失败:', error)
  }
}

const loadGroupProblems = async () => {
  try {
    const res = await getGroupProblemsApi(groupId)
    if (res.code === 1) {
      problems.value = res.data?.records || res.data || []
    }
  } catch (error) {
    console.error('加载题单题目失败:', error)
  }
}

onMounted(() => {
  loadGroupDetail()
  loadGroupProblems()
})
</script>

<style scoped>
.group-detail-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 24px 0;
}

.group-detail-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.group-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border: 1px solid #f0f0f0;
}

.breadcrumb {
  margin-bottom: 16px;
}

.back-link {
  color: #8c8c8c;
  text-decoration: none;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.back-link:hover {
  color: #409eff;
}

.group-title {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
  margin-bottom: 8px;
}

.group-desc {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-bottom: 16px;
}

.group-meta {
  display: flex;
  align-items: center;
  gap: 20px;
}

.meta-item {
  font-size: 13px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.diff-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.diff-badge.easy { color: #00b8a3; background: #e6faf8; }
.diff-badge.medium { color: #ffa116; background: #fff7e6; }
.diff-badge.hard { color: #ff375f; background: #ffebee; }
.diff-badge.all { color: #722ed1; background: #f9f0ff; }

.group-info-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.progress-label {
  font-size: 12px;
  color: #8c8c8c;
}

.problems-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #f0f0f0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
}

.problems-table {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.table-header {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.table-row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5f5f5;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: #f7f8fa;
}

.col-order {
  width: 40px;
  font-size: 13px;
  color: #8c8c8c;
  text-align: center;
}

.col-status {
  width: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.col-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}

.col-difficulty {
  width: 80px;
  text-align: center;
}

.col-acceptance {
  width: 80px;
  text-align: center;
  font-size: 13px;
  color: #595959;
}

.problem-id {
  color: #8c8c8c;
  font-size: 13px;
}

.problem-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.difficulty-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.difficulty-tag.easy { color: #00b8a3; background: #e6faf8; }
.difficulty-tag.medium { color: #ffa116; background: #fff7e6; }
.difficulty-tag.hard { color: #ff375f; background: #ffebee; }

.solved-icon, .attempted-icon {
  font-size: 18px;
}

.unsolved-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
}

.empty-state {
  padding: 60px 0;
}
</style>
