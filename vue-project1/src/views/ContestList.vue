<template>
  <div class="contests-page">
    <div class="water-bg">
      <div class="water-drop water-drop-1"></div>
      <div class="water-drop water-drop-2"></div>
      <div class="water-drop water-drop-3"></div>
      <div class="ripple ripple-1"></div>
      <div class="ripple ripple-2"></div>
    </div>

    <main class="main-content">
      <section class="page-header">
        <div class="page-badge">
          <span class="badge-dot"></span>
          <span class="badge-text">竞赛</span>
        </div>
        <h1 class="page-title">编程竞赛</h1>
        <p class="page-desc">参加编程竞赛，挑战自我，与他人同台竞技</p>
      </section>

      <div class="content-wrapper">
        <div class="tabs-wrapper">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            :class="['tab-btn', { active: activeTab === tab.key }]"
            @click="handleTabChange(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="contests-list">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>

          <template v-else-if="contestsList.length > 0">
            <a 
              v-for="contest in contestsList" 
              :key="contest.id" 
              class="contest-card"
              @click="goToContest(contest.id)"
            >
              <div class="card-left">
                <div class="contest-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
                    <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
                    <path d="M4 22h16"/>
                    <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
                    <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
                    <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>
                  </svg>
                </div>
                <div class="contest-info">
                  <div class="contest-title-row">
                    <span class="contest-name">{{ contest.title }}</span>
                    <span :class="['status-tag', getStatusClass(contest.status)]">
                      {{ getStatusName(contest.status) }}
                    </span>
                  </div>
                  <p class="contest-desc">{{ contest.description || '暂无描述' }}</p>
                  <div class="contest-meta">
                    <span class="meta-item">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/>
                        <line x1="16" x2="16" y1="2" y2="6"/>
                        <line x1="8" x2="8" y1="2" y2="6"/>
                        <line x1="3" x2="21" y1="10" y2="10"/>
                      </svg>
                      {{ formatDateTime(contest.startTime) }}
                    </span>
                    <span class="meta-item">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"/>
                        <polyline points="12 6 12 12 16 14"/>
                      </svg>
                      {{ contest.duration || calculateDuration(contest.startTime, contest.endTime) }}
                    </span>
                    <span class="meta-item" v-if="contest.participantCount !== undefined">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                        <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                      </svg>
                      {{ contest.participantCount }} 人报名
                    </span>
                  </div>
                </div>
              </div>
              <div class="card-right">
                <button 
                  v-if="contest.status === 'Upcoming' || contest.status === 0"
                  class="action-btn warning"
                  @click.stop="handleRegister(contest)"
                >
                  报名参赛
                </button>
                <button 
                  v-else-if="contest.status === 'Running' || contest.status === 1"
                  class="action-btn success"
                  @click.stop="goToContest(contest.id)"
                >
                  进入比赛
                </button>
                <button 
                  v-else
                  class="action-btn"
                  @click.stop="goToContest(contest.id)"
                >
                  查看详情
                </button>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="card-arrow">
                  <line x1="5" y1="12" x2="19" y2="12"/>
                  <polyline points="12 5 19 12 12 19"/>
                </svg>
              </div>
            </a>
          </template>

          <div v-else class="empty-state">
            <span class="empty-icon">🏆</span>
            <h3>{{ emptyDesc }}</h3>
            <p>敬请期待更多精彩比赛</p>
          </div>
        </div>

        <div class="pagination" v-if="total > 0">
          <button 
            class="page-btn" 
            :disabled="currentPage === 1"
            @click="handleCurrentChange(currentPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">第 {{ currentPage }} 页</span>
          <button 
            class="page-btn" 
            :disabled="contestsList.length < pageSize"
            @click="handleCurrentChange(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getContestsApi, joinContestApi } from '../api/contests'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const activeTab = ref('upcoming')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const contestsList = ref([])
const loading = ref(false)

const tabs = [
  { key: 'upcoming', label: '即将开始' },
  { key: 'running', label: '进行中' },
  { key: 'ended', label: '已结束' }
]

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

const handleTabChange = (key) => {
  activeTab.value = key
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
  loading.value = true
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
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadContests()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

.contests-page {
  min-height: 100vh;
  background: #fafafa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  position: relative;
  overflow-x: hidden;
}

.water-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.water-drop {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(100, 181, 246, 0.35), rgba(59, 130, 246, 0.2));
  box-shadow: 
    inset 0 0 60px rgba(255, 255, 255, 0.4),
    0 0 40px rgba(100, 181, 246, 0.15);
  animation: float 12s ease-in-out infinite;
}

.water-drop-1 {
  width: 500px;
  height: 500px;
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

.water-drop-2 {
  width: 350px;
  height: 350px;
  bottom: 5%;
  left: -120px;
  animation-delay: 4s;
}

.water-drop-3 {
  width: 250px;
  height: 250px;
  top: 25%;
  right: 3%;
  animation-delay: 8s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-50px) rotate(5deg);
  }
}

.ripple {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(100, 181, 246, 0.45);
  box-shadow: 0 0 20px rgba(100, 181, 246, 0.15);
  animation: ripple-expand 6s ease-out infinite;
}

.ripple-1 {
  width: 200px;
  height: 200px;
  top: 15%;
  left: 8%;
  animation-delay: 0s;
}

.ripple-2 {
  width: 160px;
  height: 160px;
  bottom: 20%;
  right: 12%;
  animation-delay: 3s;
}

@keyframes ripple-expand {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(3);
    opacity: 0;
  }
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 40px 20px;
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 100px;
  background: white;
  margin-bottom: 24px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.badge-text {
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
}

.page-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(2rem, 5vw, 2.5rem);
  font-weight: 700;
  color: #000;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: 1rem;
  color: rgba(0, 0, 0, 0.6);
  margin: 0;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tabs-wrapper {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.tab-btn {
  padding: 8px 20px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: white;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  border-color: rgba(0, 0, 0, 0.15);
}

.tab-btn.active {
  background: #000;
  border-color: #000;
  color: #fff;
}

.contests-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: rgba(0, 0, 0, 0.5);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-top-color: #000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.contest-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.contest-card:hover {
  border-color: rgba(0, 0, 0, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.card-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.contest-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 10px;
  color: rgba(0, 0, 0, 0.7);
  flex-shrink: 0;
}

.contest-card:hover .contest-icon {
  background: rgba(0, 0, 0, 0.05);
  color: #000;
}

.contest-info {
  flex: 1;
  min-width: 0;
}

.contest-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.contest-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #000;
}

.status-tag {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-tag.upcoming {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.status-tag.running {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.status-tag.ended {
  background: rgba(0, 0, 0, 0.05);
  color: rgba(0, 0, 0, 0.5);
}

.contest-desc {
  font-size: 0.8125rem;
  color: rgba(0, 0, 0, 0.6);
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.contest-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: white;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: rgba(0, 0, 0, 0.15);
}

.action-btn.warning {
  background: #f59e0b;
  border-color: #f59e0b;
  color: white;
}

.action-btn.warning:hover {
  background: #d97706;
}

.action-btn.success {
  background: #22c55e;
  border-color: #22c55e;
  color: white;
}

.action-btn.success:hover {
  background: #16a34a;
}

.card-arrow {
  color: rgba(0, 0, 0, 0.25);
  transition: all 0.2s ease;
}

.contest-card:hover .card-arrow {
  color: #000;
  transform: translateX(3px);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #000;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.6);
  margin: 0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: white;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: rgba(0, 0, 0, 0.15);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.6);
}

@media (max-width: 640px) {
  .card-right {
    display: none;
  }
  
  .contest-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
