<template>
  <div class="contest-detail-page">
    <div class="contest-detail-container">
      <!-- 返回导航 -->
      <div class="breadcrumb">
        <router-link to="/contests" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
          返回竞赛列表
        </router-link>
      </div>

      <!-- 比赛信息卡片 -->
      <div class="contest-hero">
        <div class="hero-left">
          <div class="contest-type-icon">
            <el-icon :size="28"><Trophy /></el-icon>
          </div>
          <div class="hero-info">
            <h1 class="contest-title">{{ contest.title }}</h1>
            <div class="contest-meta">
              <span :class="['status-badge', getStatusClass(contest.status)]">
                {{ getStatusName(contest.status) }}
              </span>
              <span class="meta-item" v-if="contest.type">{{ contest.type }}</span>
            </div>
          </div>
        </div>
        <div class="hero-right">
          <!-- 倒计时 -->
          <div class="countdown" v-if="countdownText">
            <span class="countdown-label">{{ countdownLabel }}</span>
            <span class="countdown-value">{{ countdownText }}</span>
          </div>
          <!-- 报名按钮 -->
          <el-button 
            v-if="(contest.status === 'Upcoming' || contest.status === 0) && !contest.registered"
            type="warning" 
            size="large"
            round
            @click="handleJoin"
          >
            报名参赛
          </el-button>
          <el-tag 
            v-else-if="contest.registered && (contest.status === 'Upcoming' || contest.status === 0)"
            type="success" size="large" effect="dark" round
          >
            ✅ 已报名
          </el-tag>
          <el-button 
            v-else-if="contest.status === 'Running' || contest.status === 1"
            type="success" 
            size="large"
            round
            disabled
          >
            比赛进行中
          </el-button>
        </div>
      </div>

      <!-- 比赛详情信息 -->
      <div class="contest-info-grid">
        <div class="info-card">
          <div class="info-label">开始时间</div>
          <div class="info-value">{{ formatDateTime(contest.startTime) }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">结束时间</div>
          <div class="info-value">{{ formatDateTime(contest.endTime) }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">比赛时长</div>
          <div class="info-value">{{ calculateDuration(contest.startTime, contest.endTime) }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">参赛人数</div>
          <div class="info-value">{{ contest.participantCount || 0 }}</div>
        </div>
      </div>

      <!-- 比赛描述 -->
      <div class="contest-description" v-if="contest.description">
        <h2 class="section-title">比赛说明</h2>
        <div class="desc-content" v-html="contest.description"></div>
      </div>

      <!-- 比赛Tab内容 -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="题目列表" name="problems">
          <!-- 比赛未开始时隐藏题目 -->
          <template v-if="contest.status === 'Running' || contest.status === 1 || contest.status === 'Ended' || contest.status === 2">
            <div class="problems-table" v-if="problems.length > 0">
              <div class="table-header">
                <span class="col-order">#</span>
                <span class="col-title">题目</span>
                <span class="col-difficulty">难度</span>
                <span class="col-score">分值</span>
                <span class="col-status">状态</span>
              </div>
              <div 
                v-for="(problem, index) in problems" 
                :key="problem.id" 
                class="table-row"
                @click="goToProblem(problem.id)"
              >
                <span class="col-order">{{ String.fromCharCode(65 + index) }}</span>
                <span class="col-title">
                  <span class="problem-name">{{ problem.title }}</span>
                </span>
                <span class="col-difficulty">
                  <span :class="['diff-tag', getDifficultyClass(problem.difficulty)]">
                    {{ getDifficultyLabel(problem.difficulty) }}
                  </span>
                </span>
                <span class="col-score">{{ problem.score || 100 }}</span>
                <span class="col-status">
                  <el-icon v-if="problem.solved" color="#00b8a3"><CircleCheckFilled /></el-icon>
                  <span v-else>-</span>
                </span>
              </div>
            </div>
            <el-empty v-else description="暂无题目数据" />
          </template>
          <el-empty v-else description="比赛开始后才能查看题目" />
        </el-tab-pane>

        <el-tab-pane label="排行榜" name="rank">
          <el-table :data="rankList" style="width: 100%" size="small" stripe>
            <el-table-column prop="rank" label="排名" width="80" align="center">
              <template #default="scope">
                <span :class="['rank-num', scope.row.rank <= 3 ? 'top-rank rank-' + scope.row.rank : '']">
                  {{ scope.row.rank }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户" min-width="150" />
            <el-table-column prop="score" label="总分" width="100" align="center">
              <template #default="scope">
                <span class="score-value">{{ scope.row.score }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="solvedCount" label="通过题数" width="100" align="center" />
          </el-table>
          <el-empty v-if="rankList.length === 0" description="暂无排行数据" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Trophy, CircleCheckFilled } from '@element-plus/icons-vue'
import { getContestDetailApi, joinContestApi, getContestProblemsApi, getContestRankApi } from '../api/contests'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const contestId = route.params.id

const contest = ref({})
const problems = ref([])
const rankList = ref([])
const activeTab = ref('problems')

// ===== 倒计时 =====
const countdownText = ref('')
const countdownLabel = ref('')
let countdownTimer = null

const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    const now = new Date().getTime()
    const status = contest.value.status
    let target = null

    if (status === 'Upcoming' || status === 0) {
      target = new Date(contest.value.startTime).getTime()
      countdownLabel.value = '距开始：'
    } else if (status === 'Running' || status === 1) {
      target = new Date(contest.value.endTime).getTime()
      countdownLabel.value = '距结束：'
    } else {
      countdownText.value = ''
      countdownLabel.value = ''
      clearInterval(countdownTimer)
      return
    }

    const diff = target - now
    if (diff <= 0) {
      countdownText.value = ''
      countdownLabel.value = ''
      clearInterval(countdownTimer)
      // 状态可能变了，重新加载
      loadContest()
      loadProblems()
      return
    }

    const h = Math.floor(diff / 3600000)
    const m = Math.floor((diff % 3600000) / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    countdownText.value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }, 1000)
}

// ===== 工具函数 =====
const getStatusClass = (status) => {
  if (status === 'Upcoming' || status === 0) return 'upcoming'
  if (status === 'Running' || status === 1) return 'running'
  return 'ended'
}

const getStatusName = (status) => {
  if (status === 'Upcoming' || status === 0) return '即将开始'
  if (status === 'Running' || status === 1) return '进行中'
  return '已结束'
}

const getDifficultyClass = (d) => ({ 'Easy': 'easy', 'Medium': 'medium', 'Hard': 'hard' }[d] || '')
const getDifficultyLabel = (d) => ({ 'Easy': '简单', 'Medium': '中等', 'Hard': '困难' }[d] || d)

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const calculateDuration = (start, end) => {
  if (!start || !end) return '-'
  const diff = new Date(end) - new Date(start)
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  return h > 0 ? `${h}小时${m > 0 ? m + '分钟' : ''}` : `${m}分钟`
}

// 比赛题目跳转到比赛做题页面
const goToProblem = (problemId) => {
  router.push(`/contests/${contestId}/problems/${problemId}`)
}

const handleJoin = async () => {
  try {
    await ElMessageBox.confirm('确定要报名参加此比赛吗？', '报名确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
    })
    const res = await joinContestApi(contestId)
    if (res.code === 1) {
      ElMessage.success('报名成功！')
      loadContest()
    } else {
      ElMessage.error(res.msg || '报名失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('报名请求失败')
  }
}

const loadContest = async () => {
  try {
    const res = await getContestDetailApi(contestId)
    if (res.code === 1) {
      contest.value = res.data || {}
      // 如果详情接口返回了 problemList，也同步到 problems
      if (res.data?.problemList && res.data.problemList.length > 0) {
        problems.value = res.data.problemList
      }
      startCountdown()
    }
  } catch (e) {
    console.error('加载比赛详情失败:', e)
  }
}

const loadProblems = async () => {
  try {
    const res = await getContestProblemsApi(contestId)
    if (res.code === 1) {
      // 后端返回的是 ContestVO，题目列表在 problemList 字段中
      problems.value = res.data?.problemList || []
    }
  } catch (e) {
    console.error('加载比赛题目失败:', e)
  }
}

const loadRank = async () => {
  try {
    const res = await getContestRankApi(contestId)
    if (res.code === 1) rankList.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载排行榜失败:', e)
  }
}

onMounted(() => {
  loadContest()
  loadProblems()
  loadRank()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.contest-detail-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 24px 0;
}

.contest-detail-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.back-link {
  color: #8c8c8c;
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

.contest-hero {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 16px;
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.contest-type-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff9500, #ff6b00);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.contest-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.contest-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.status-badge.upcoming { color: #1890ff; background: rgba(24,144,255,0.2); }
.status-badge.running { color: #52c41a; background: rgba(82,196,26,0.2); }
.status-badge.ended { color: #8c8c8c; background: rgba(140,140,140,0.2); }

.meta-item {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}

.contest-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  border: 1px solid #f0f0f0;
}

.info-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.contest-description {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #f0f0f0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
}

.desc-content {
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}

.detail-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f0f0;
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
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.table-row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5f5f5;
}

.table-row:last-child { border-bottom: none; }
.table-row:hover { background: #f7f8fa; }

.col-order { width: 50px; font-weight: 600; color: #595959; text-align: center; }
.col-title { flex: 1; }
.col-difficulty { width: 80px; text-align: center; }
.col-score { width: 80px; text-align: center; font-size: 14px; color: #262626; font-weight: 500; }
.col-status { width: 60px; text-align: center; }

.problem-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.diff-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.diff-tag.easy { color: #00b8a3; background: #e6faf8; }
.diff-tag.medium { color: #ffa116; background: #fff7e6; }
.diff-tag.hard { color: #ff375f; background: #ffebee; }

.rank-num {
  font-weight: 500;
}

.top-rank {
  font-weight: 700;
}

.rank-1 { color: #faad14; }
.rank-2 { color: #8c8c8c; }
.rank-3 { color: #d48806; }

.score-value {
  font-weight: 600;
  color: #1890ff;
}

.countdown {
  text-align: center;
  margin-bottom: 8px;
}

.countdown-label {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
  display: block;
  margin-bottom: 4px;
}

.countdown-value {
  font-size: 22px;
  font-weight: 700;
  color: #ffd666;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}
</style>
