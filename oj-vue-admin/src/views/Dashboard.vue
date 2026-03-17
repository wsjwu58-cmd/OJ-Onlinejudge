<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="dashboard-title">工作台</h2>
        <p class="dashboard-subtitle">欢迎回来，这里是您的运营数据概览</p>
      </div>
      <div class="header-right">
        <div class="time-range-selector">
          <el-radio-group v-model="selectedTimeRange" @change="handleTimeRangeChange" size="small">
            <el-radio-button label="7d">近七日</el-radio-button>
            <el-radio-button label="30d">近30日</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
          </el-radio-group>
        </div>
        <el-button 
          size="small" 
          type="primary" 
          plain 
          @click="refreshAllData"
          class="refresh-btn"
        >
          <el-icon><Refresh /></el-icon>
          刷新全部
        </el-button>
      </div>
    </div>
    
    <!-- 运营数据卡片 -->
    <div class="dashboard-cards">
      <el-card class="dashboard-card" hoverable>
        <div class="card-content">
          <div class="card-icon user-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">总用户数</div>
            <div class="card-value">{{ stats.totalUsers }}</div>
            <div class="card-change" :class="{ positive: stats.userChange > 0, negative: stats.userChange < 0 }">
              {{ stats.userChange > 0 ? '+' : '' }}{{ stats.userChange }}%
              <span class="change-label">较昨日</span>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="dashboard-card" hoverable>
        <div class="card-content">
          <div class="card-icon active-icon">
            <el-icon><View /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">今日活跃用户</div>
            <div class="card-value">{{ stats.activeUsersToday }}</div>
            <div class="card-change" :class="{ positive: stats.activeChange > 0, negative: stats.activeChange < 0 }">
              {{ stats.activeChange > 0 ? '+' : '' }}{{ stats.activeChange }}%
              <span class="change-label">较昨日</span>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="dashboard-card" hoverable>
        <div class="card-content">
          <div class="card-icon submit-icon">
            <el-icon><DocumentChecked /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">今日提交数</div>
            <div class="card-value">{{ stats.submissionsToday }}</div>
            <div class="card-change" :class="{ positive: stats.submissionChange > 0, negative: stats.submissionChange < 0 }">
              {{ stats.submissionChange > 0 ? '+' : '' }}{{ stats.submissionChange }}%
              <span class="change-label">较昨日</span>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="dashboard-card" hoverable>
        <div class="card-content">
          <div class="card-icon problem-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">题目总数</div>
            <div class="card-value">{{ stats.totalProblems }}</div>
            <div class="card-change" :class="{ positive: stats.problemChange > 0, negative: stats.problemChange < 0 }">
              {{ stats.problemChange > 0 ? '+' : '' }}{{ stats.problemChange }}%
              <span class="change-label">较昨日</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 题目和比赛数据 -->
    <div class="dashboard-stats">
      <!-- 题目数据 -->
      <el-card class="stats-card" hoverable>
        <template #header>
          <div class="card-header">
            <span class="header-title">题目数据</span>
            <el-button size="small" type="text" @click="fetchProblemData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon active">
              <el-icon><DocumentChecked /></el-icon>
            </div>
            <div class="stat-label">上架题目</div>
            <div class="stat-value">{{ problemData.send }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon inactive">
              <el-icon><Close /></el-icon>
            </div>
            <div class="stat-label">未上架题目</div>
            <div class="stat-value">{{ problemData.disSend }}</div>
          </div>
        </div>
      </el-card>
      
      <!-- 比赛数据 -->
      <el-card class="stats-card" hoverable>
        <template #header>
          <div class="card-header">
            <span class="header-title">比赛数据</span>
            <el-button size="small" type="text" @click="fetchContestData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon pending">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-label">未开始</div>
            <div class="stat-value">{{ contestData.disSend }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon active">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="stat-label">进行中</div>
            <div class="stat-value">{{ contestData.send }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon completed">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stat-label">已结束</div>
            <div class="stat-value">{{ contestData.finalSend }}</div>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 最近活动 -->
    <div class="dashboard-activity">
      <el-card hoverable>
        <template #header>
          <div class="card-header">
            <span class="header-title">最近活动</span>
            <el-button size="small" type="primary" @click="refreshActivity">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>
        <div class="activity-list">
          <div v-if="recentActivity.length > 0">
            <el-timeline>
              <el-timeline-item
                v-for="(item, index) in recentActivity"
                :key="index"
                :timestamp="item.time"
                :type="item.type"
                :icon="getActivityIcon(item.type)"
                :size="20"
                class="timeline-item"
              >
                <div class="activity-content">
                  <div class="activity-title">{{ item.title }}</div>
                  <div class="activity-desc">{{ item.description }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="no-activity">
            <el-empty description="暂无最近活动" />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, UserFilled, View, DocumentChecked, DataAnalysis, Check, Close, DataLine, Clock, VideoPlay, Timer, Trophy, Medal, Flag } from '@element-plus/icons-vue'
import { 
  getRecentActivitiesApi, 
  getWorkDataApi, 
  getProblemDataApi, 
  getContestDataApi
} from '../api/common'

// 响应式数据
const dateRange = ref([])
const stats = reactive({
  totalUsers: 0,
  activeUsersToday: 0,
  submissionsToday: 0,
  totalProblems: 0,
  userChange: 0,
  activeChange: 0,
  submissionChange: 0,
  problemChange: 0
})

const recentActivity = ref([])
const problemData = reactive({
  send: 0, // 上架题目
  disSend: 0 // 未上架题目
})

const contestData = reactive({
  send: 0, // 进行中
  disSend: 0, // 未开始
  finalSend: 0 // 已结束
})

// 模拟数据，实际项目中会从API获取
const mockStats = {
  totalUsers: 12580,
  activeUsersToday: 342,
  submissionsToday: 1256,
  totalProblems: 328,
  userChange: 12.5,
  activeChange: 8.3,
  submissionChange: 15.2,
  problemChange: 5.1
}

// 映射活动类型到前端标签类型
const mapActivityType = (type) => {
  switch (type) {
    case "USER_REGISTER": return "primary"
    case "PROBLEM_ACCEPT": return "success"
    case "PROBLEM_ADD": return "info"
    case "USER_LOGIN": return "warning"
    default: return "info"
  }
}

// 方法


const refreshActivity = () => {
  fetchRecentActivity()
  ElMessage.success('活动列表已刷新')
}

const refreshAllData = async () => {
  try {
    await Promise.all([
      fetchDashboardData(),
      fetchProblemData(),
      fetchContestData(),
      fetchRecentActivity()
    ])
    ElMessage.success('全部数据已刷新')
  } catch (error) {
    console.error('刷新数据失败:', error)
    ElMessage.error('刷新数据失败，请稍后重试')
  }
}

const getActivityIcon = (type) => {
  switch (type) {
    case 'primary': return UserFilled
    case 'success': return Check
    case 'warning': return Clock
    case 'info': return DocumentChecked
    default: return DocumentChecked
  }
}





const fetchDashboardData = async () => {
  try {
    console.log('获取运营数据')
    const response = await getWorkDataApi()
    
    // 打印完整的响应数据，以便调试
    console.log('运营数据响应:', response)
    
    // 转换后端数据为前端需要的格式
    if (response.code === 1 && response.data) {
      console.log('运营数据:', response.data)
      Object.assign(stats, response.data)
    } else {
      console.log('响应格式不正确:', response)
      // 使用模拟数据作为 fallback
      Object.assign(stats, mockStats)
    }
  } catch (error) {
    console.error('获取运营数据失败:', error)
    ElMessage.error('获取运营数据失败，请稍后重试')
    // 使用模拟数据作为 fallback
    Object.assign(stats, mockStats)
  }
}

const fetchProblemData = async () => {
  try {
    console.log('获取题目数据')
    const response = await getProblemDataApi()
    
    // 打印完整的响应数据，以便调试
    console.log('题目数据响应:', response)
    
    // 转换后端数据为前端需要的格式
    if (response.code === 1 && response.data) {
      console.log('题目数据:', response.data)
      Object.assign(problemData, response.data)
    } else {
      console.log('响应格式不正确:', response)
    }
  } catch (error) {
    console.error('获取题目数据失败:', error)
    ElMessage.error('获取题目数据失败，请稍后重试')
  }
}

const fetchContestData = async () => {
  try {
    console.log('获取比赛数据')
    const response = await getContestDataApi()
    
    // 打印完整的响应数据，以便调试
    console.log('比赛数据响应:', response)
    
    // 转换后端数据为前端需要的格式
    if (response.code === 1 && response.data) {
      console.log('比赛数据:', response.data)
      Object.assign(contestData, response.data)
    } else {
      console.log('响应格式不正确:', response)
    }
  } catch (error) {
    console.error('获取比赛数据失败:', error)
    ElMessage.error('获取比赛数据失败，请稍后重试')
  }
}

const fetchRecentActivity = async () => {
  try {
    console.log('获取最近活动')
    const response = await getRecentActivitiesApi(10)
    
    // 打印完整的响应数据，以便调试
    console.log('最近活动响应:', response)
    
    // 转换后端数据为前端需要的格式
    if (response.code === 1 && response.data) {
      console.log('最近活动数据:', response.data)
      recentActivity.value = response.data.map(item => {
        console.log('单个活动数据:', item)
        return {
          title: item.title || '',
          description: item.description || '',
          time: item.createTime || '',
          type: mapActivityType(item.activityType || '')
        }
      })
      console.log('转换后的活动数据:', recentActivity.value)
    } else {
      console.log('响应格式不正确:', response)
    }
  } catch (error) {
    console.error('获取最近活动失败:', error)
    ElMessage.error('获取最近活动失败，请稍后重试')
  }
}

// 生命周期钩子
onMounted(() => {
  fetchDashboardData()
  fetchProblemData()
  fetchContestData()
  fetchRecentActivity()
})
</script>

<style scoped>
.dashboard-container {
  background-color: #f7f8fa;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  width: 100%;
  overflow-x: auto;
  min-height: 100vh;
}

/* 头部样式 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dashboard-title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #1f2329;
  background: linear-gradient(135deg, #409eff, #69c0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dashboard-subtitle {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 400;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-range-selector {
  display: flex;
  align-items: center;
}

.el-radio-group {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.el-radio-button__inner {
  border-radius: 0;
  border-right: 1px solid #e8e8e8;
}

.el-radio-button:last-child .el-radio-button__inner {
  border-right: none;
}

.el-radio-button__orig-radio:checked + .el-radio-button__inner {
  background-color: #409eff;
  border-color: #409eff;
}

.refresh-btn {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 数据卡片样式 */
.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 36px;
}

/* 题目和比赛数据样式 */
.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 24px;
  margin-bottom: 36px;
}

.stats-card {
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stats-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.dashboard-card {
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dashboard-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 30px 24px;
  background: linear-gradient(135deg, #f9f9f9, #ffffff);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.card-icon:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.user-icon {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.active-icon {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.submit-icon {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.problem-icon {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
  font-weight: 500;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2329;
  margin-bottom: 8px;
  line-height: 1.2;
}

.card-change {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.card-change:hover {
  transform: scale(1.05);
}

.card-change.positive {
  color: #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
}

.card-change.negative {
  color: #f56c6c;
  background-color: rgba(245, 108, 108, 0.1);
}

.change-label {
  font-size: 11px;
  opacity: 0.8;
}

/* 图表样式 */
.dashboard-charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 24px;
  margin-bottom: 36px;
}

.chart-card {
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chart-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.chart-container {
  height: 320px;
  padding: 24px;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.trend-chart {
  width: 100%;
  height: 100%;
}

.chart-placeholder {
  width: 100%;
  height: 100%;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16px;
}

.chart-data {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100% - 30px);
  overflow-y: auto;
}

.chart-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.chart-item-date {
  width: 80px;
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}

.chart-item-bar {
  flex: 1;
  height: 20px;
  background-color: #f3f4f6;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.chart-item-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #69c0ff);
  border-radius: 10px;
  transition: width 0.5s ease;
}

.chart-item-bar-fill.problem-bar {
  background: linear-gradient(90deg, #43e97b, #38f9d7);
}

.chart-item-bar-fill.submission-bar {
  background: linear-gradient(90deg, #fa709a, #fee140);
}

.chart-item-value {
  width: 60px;
  font-size: 12px;
  font-weight: 600;
  color: #1f2329;
  flex-shrink: 0;
  text-align: right;
}

.no-data {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 排行榜样式 */
.ranking-list {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid #e8e8e8;
}

.ranking-item:hover {
  transform: translateX(8px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.ranking-item.top-ranking {
  background-color: #f0f9ff;
  border-color: #60a5fa;
}

.ranking-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  flex-shrink: 0;
}

.ranking-item.top-ranking:nth-child(1) .ranking-number {
  background-color: #fbbf24;
  color: white;
}

.ranking-item.top-ranking:nth-child(2) .ranking-number {
  background-color: #9ca3af;
  color: white;
}

.ranking-item.top-ranking:nth-child(3) .ranking-number {
  background-color: #d97706;
  color: white;
}

.ranking-problem {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.problem-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
  line-height: 1.4;
}

.problem-acceptance {
  display: flex;
  align-items: center;
  gap: 8px;
}

.acceptance-value {
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
}

.acceptance-detail {
  font-size: 11px;
  color: #6b7280;
}

.ranking-badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ranking-item.top-ranking:nth-child(1) .ranking-badge {
  background-color: #fbbf24;
  color: white;
}

.ranking-item.top-ranking:nth-child(2) .ranking-badge {
  background-color: #9ca3af;
  color: white;
}

/* 数据统计样式 */
.stats-grid {
  display: flex;
  justify-content: space-around;
  align-items: stretch;
  width: 100%;
  height: 100%;
  gap: 20px;
}

.stat-card {
  flex: 1;
  background: linear-gradient(135deg, #f9f9f9, #ffffff);
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #409eff, #69c0ff);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.stat-card:hover::before {
  transform: scaleX(1);
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.stat-card.total {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.stat-icon:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.stat-icon.active {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
}

.stat-icon.inactive {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.stat-icon.pending {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.stat-icon.completed {
  background: linear-gradient(135deg, #fa709a, #fee140);
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.2;
}

.stat-percentage {
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
  background-color: rgba(64, 158, 255, 0.1);
  padding: 4px 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat-percentage:hover {
  background-color: rgba(64, 158, 255, 0.2);
  transform: scale(1.05);
}

/* 活动列表样式 */
.dashboard-activity {
  margin-bottom: 30px;
}

.activity-list {
  max-height: 480px;
  overflow-y: auto;
  padding: 0 12px;
}

.activity-list::-webkit-scrollbar {
  width: 6px;
}

.activity-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.activity-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
  transition: background 0.3s ease;
}

.activity-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

.timeline-item {
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.timeline-item:hover {
  transform: translateX(8px);
}

.activity-content {
  padding: 16px 20px;
  background-color: #f9fafb;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.activity-content:hover {
  background-color: #f3f4f6;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.activity-title {
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8px;
  font-size: 14px;
}

.activity-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.no-activity {
  padding: 60px 0;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .dashboard-charts {
    grid-template-columns: 1fr;
  }
  
  .chart-card {
    height: 320px;
  }
  
  .chart-container {
    height: 260px;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 20px;
    border-radius: 8px;
  }
  
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .header-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .dashboard-cards {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .dashboard-card {
    padding: 24px 20px;
  }
  
  .card-content {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .data-stats {
    flex-direction: column;
    gap: 16px;
  }
  
  .stat-item {
    width: 100%;
    margin: 0;
  }
  
  .timeline-item {
    margin-bottom: 20px;
  }
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.dashboard-card, .chart-card {
  animation: pulse 0.6s ease-in-out;
}

/* 文字渐变效果 */
.text-gradient {
  background: linear-gradient(135deg, #409eff, #69c0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>