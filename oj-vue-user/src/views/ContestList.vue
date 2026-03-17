<template>
  <div class="contests-page">
    <div class="contests-container">
      <div class="contests-header-section">
        <h1 class="page-title">竞赛</h1>
        <p class="page-desc">参加编程竞赛，挑战自我，与他人同台竞技</p>
      </div>

      <!-- 比赛状态Tab -->
      <el-tabs v-model="activeTab" class="contest-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="即将开始" name="upcoming" />
        <el-tab-pane label="进行中" name="running" />
        <el-tab-pane label="已结束" name="ended" />
      </el-tabs>

      <!-- 比赛卡片列表 -->
      <div class="contests-list">
        <div 
          v-for="contest in contestsList" 
          :key="contest.id" 
          class="contest-card"
          @click="goToContest(contest.id)"
        >
          <div class="contest-card-left">
            <div class="contest-type-badge">
              <el-icon :size="20"><Trophy /></el-icon>
            </div>
          </div>
          <div class="contest-card-body">
            <div class="contest-top">
              <h3 class="contest-title">{{ contest.title }}</h3>
              <span :class="['contest-status-tag', getStatusClass(contest.status)]">
                {{ getStatusName(contest.status) }}
              </span>
            </div>
            <p class="contest-desc">{{ contest.description || '暂无描述' }}</p>
            <div class="contest-info">
              <span class="info-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDateTime(contest.startTime) }}
              </span>
              <span class="info-item">
                <el-icon><Timer /></el-icon>
                {{ contest.duration || calculateDuration(contest.startTime, contest.endTime) }}
              </span>
              <span class="info-item" v-if="contest.type">
                <el-icon><Flag /></el-icon>
                {{ contest.type }}
              </span>
              <span class="info-item" v-if="contest.participantCount !== undefined">
                <el-icon><User /></el-icon>
                {{ contest.participantCount }} 人报名
              </span>
            </div>
          </div>
          <div class="contest-card-right">
            <el-button 
              v-if="contest.status === 'Upcoming' || contest.status === 0"
              type="warning" 
              round 
              size="small"
              @click.stop="handleRegister(contest)"
            >
              报名参赛
            </el-button>
            <el-button 
              v-else-if="contest.status === 'Running' || contest.status === 1"
              type="success" 
              round 
              size="small"
              @click.stop="goToContest(contest.id)"
            >
              进入比赛
            </el-button>
            <el-button 
              v-else
              round 
              size="small"
              @click.stop="goToContest(contest.id)"
            >
              查看详情
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="contestsList.length === 0" class="empty-state">
        <el-empty :description="emptyDesc" />
      </div>

      <!-- 分页 -->
      <div class="contests-pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Trophy, Calendar, Timer, Flag, User } from '@element-plus/icons-vue'
import { getContestsApi, joinContestApi } from '../api/contests'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const activeTab = ref('upcoming')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const contestsList = ref([])

const emptyDesc = computed(() => {
  const map = {
    upcoming: '暂无即将开始的比赛',
    running: '暂无进行中的比赛',
    ended: '暂无已结束的比赛'
  }
  return map[activeTab.value] || '暂无比赛'
})

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

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const calculateDuration = (start, end) => {
  if (!start || !end) return '-'
  const diff = new Date(end) - new Date(start)
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  if (hours > 0) return `${hours}小时${minutes > 0 ? minutes + '分钟' : ''}`
  return `${minutes}分钟`
}

const handleTabChange = () => {
  currentPage.value = 1
  loadContests()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadContests()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadContests()
}

const goToContest = (id) => {
  router.push(`/contests/${id}`)
}

const handleRegister = async (contest) => {
  try {
    await ElMessageBox.confirm(
      `确定要报名参加「${contest.title}」吗？`,
      '报名确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    const res = await joinContestApi(contest.id)
    if (res.code === 1) {
      ElMessage.success('报名成功！')
      loadContests()
    } else {
      ElMessage.error(res.msg || '报名失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('报名请求失败')
    }
  }
}

const loadContests = async () => {
  try {
    const statusMap = {
      upcoming: 'Upcoming',
      running: 'Running',
      ended: 'Ended'
    }
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusMap[activeTab.value]
    }
    const res = await getContestsApi(params)
    if (res.code === 1) {
      contestsList.value = res.data?.records || res.data || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载比赛列表失败:', error)
  }
}

onMounted(() => {
  loadContests()
})
</script>

<style scoped>
.contests-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 32px 0;
}

.contests-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.contests-header-section {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

.contest-tabs {
  margin-bottom: 24px;
}

.contest-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}

.contests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contest-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}

.contest-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: #d9d9d9;
}

.contest-card-left {
  flex-shrink: 0;
}

.contest-type-badge {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff9500, #ff6b00);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.contest-card-body {
  flex: 1;
  min-width: 0;
}

.contest-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.contest-title {
  font-size: 17px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.contest-status-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
  white-space: nowrap;
}

.contest-status-tag.upcoming {
  color: #1890ff;
  background: #e6f7ff;
}

.contest-status-tag.running {
  color: #52c41a;
  background: #f6ffed;
}

.contest-status-tag.ended {
  color: #8c8c8c;
  background: #f5f5f5;
}

.contest-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 12px;
  line-height: 1.5;
}

.contest-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.info-item {
  font-size: 13px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.contest-card-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.contests-pagination {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.empty-state {
  padding: 80px 0;
}
</style>
