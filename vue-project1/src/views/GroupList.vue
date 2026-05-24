<template>
  <div class="groups-page">
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
          <span class="badge-text">题单</span>
        </div>
        <h1 class="page-title">精选题单</h1>
        <p class="page-desc">精选题目集合，系统性刷题提升能力</p>
      </section>

      <div class="content-wrapper">
        <div class="toolbar">
          <div class="search-box">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索题单名称..."
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </button>
          </div>
          <div class="filter-box">
            <select v-model="difficultyFilter" class="filter-select" @change="handleSearch">
              <option value="">全部难度</option>
              <option value="Easy">简单</option>
              <option value="Medium">中等</option>
              <option value="Hard">困难</option>
            </select>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>

        <div v-else-if="groupsList.length > 0" class="groups-grid">
          <a 
            v-for="group in groupsList" 
            :key="group.id" 
            class="group-card"
            @click="goToGroup(group.id)"
          >
            <div class="card-header">
              <div class="group-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
                </svg>
              </div>
              <span v-if="group.difficultyRange" :class="['diff-tag', getDifficultyClass(group.difficultyRange)]">
                {{ getDifficultyLabel(group.difficultyRange) }}
              </span>
            </div>
            
            <h3 class="group-title">{{ group.title }}</h3>
            <p class="group-desc">{{ group.description || '暂无描述' }}</p>
            
            <div class="card-footer">
              <div class="group-stats">
                <span class="stat-item">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  {{ group.problemCount || 0 }} 题
                </span>
                <span class="stat-item" v-if="group.estimatedDurationMinutes">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  {{ group.estimatedDurationMinutes }} 分钟
                </span>
              </div>
              
              <div class="progress-section" v-if="group.progress !== undefined">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: group.progress + '%' }"></div>
                </div>
                <span class="progress-text">{{ group.progress }}%</span>
              </div>
            </div>
            
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="card-arrow">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
          </a>
        </div>

        <div v-else class="empty-state">
          <span class="empty-icon">📚</span>
          <h3>暂无题单</h3>
          <p>敬请期待更多精选题单</p>
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
            :disabled="groupsList.length < pageSize"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGroupsApi } from '../api/groups'

const router = useRouter()

const searchQuery = ref('')
const difficultyFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const groupsList = ref([])
const loading = ref(false)

const getDifficultyClass = (difficulty) => {
  const map = { 'Easy': 'easy', 'Medium': 'medium', 'Hard': 'hard', 'All': 'all' }
  return map[difficulty] || ''
}

const getDifficultyLabel = (difficulty) => {
  const map = { 'Easy': '简单', 'Medium': '中等', 'Hard': '困难', 'All': '综合' }
  return map[difficulty] || difficulty
}

const handleSearch = () => {
  currentPage.value = 1
  loadGroups()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadGroups()
}

const goToGroup = (id) => {
  router.push(`/groups/${id}`)
}

const loadGroups = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      title: searchQuery.value || undefined,
      difficultyRange: difficultyFilter.value || undefined
    }
    const res = await getGroupsApi(params)
    if (res.code === 1) {
      groupsList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载题单列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

.groups-page {
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
  max-width: 1200px;
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
  gap: 32px;
}

.toolbar {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  gap: 8px;
  max-width: 400px;
  flex: 1;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  font-size: 0.875rem;
  background: white;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: rgba(0, 0, 0, 0.2);
}

.search-btn {
  padding: 10px 16px;
  background: #000;
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.search-btn:hover {
  opacity: 0.85;
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  font-size: 0.875rem;
  background: white;
  cursor: pointer;
  min-width: 140px;
}

.filter-select:focus {
  outline: none;
  border-color: rgba(0, 0, 0, 0.2);
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

.groups-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.group-card {
  position: relative;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
}

.group-card:hover {
  border-color: rgba(0, 0, 0, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.group-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 10px;
  color: rgba(0, 0, 0, 0.7);
}

.group-card:hover .group-icon {
  background: rgba(0, 0, 0, 0.05);
  color: #000;
}

.diff-tag {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
}

.diff-tag.easy {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.diff-tag.medium {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.diff-tag.hard {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.diff-tag.all {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.group-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #000;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.group-desc {
  font-size: 0.8125rem;
  color: rgba(0, 0, 0, 0.6);
  line-height: 1.6;
  margin: 0 0 16px 0;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding-top: 12px;
}

.group-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  gap: 6px;
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #22c55e;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.75rem;
  color: #22c55e;
  font-weight: 600;
  min-width: 36px;
}

.card-arrow {
  position: absolute;
  top: 20px;
  right: 20px;
  color: rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.group-card:hover .card-arrow {
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

@media (max-width: 900px) {
  .groups-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .groups-grid {
    grid-template-columns: 1fr;
  }
  
  .toolbar {
    flex-direction: column;
  }
  
  .search-box {
    max-width: none;
  }
}
</style>
