<template>
  <div class="problems-page">
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
          <span class="badge-text">题库</span>
        </div>
        <h1 class="page-title">题目列表</h1>
        <p class="page-desc">海量算法题目，按分类、难度筛选，系统提升编程能力</p>
      </section>

      <div class="content-wrapper">
        <aside class="sidebar">
          <div class="sidebar-section">
            <h3 class="sidebar-title">难度</h3>
            <div class="filter-tags">
              <button 
                v-for="item in difficultyOptions" 
                :key="item.value"
                :class="['filter-btn', { active: difficultyFilter === item.value }]"
                @click="handleDifficultyChange(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="sidebar-section">
            <h3 class="sidebar-title">分类</h3>
            <div class="filter-tags">
              <button 
                v-for="cat in categories" 
                :key="cat.id"
                :class="['filter-btn', { active: categoryFilter === cat.id }]"
                @click="handleCategoryChange(cat.id)"
              >
                {{ cat.name }}
              </button>
            </div>
          </div>

          <div class="sidebar-section">
            <h3 class="sidebar-title">状态</h3>
            <div class="filter-tags">
              <button 
                v-for="item in statusOptions" 
                :key="item.value"
                :class="['filter-btn', { active: statusFilter === item.value }]"
                @click="handleStatusChange(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>

          <button v-if="hasActiveFilters" class="reset-btn" @click="resetFilters">
            重置筛选
          </button>
        </aside>

        <div class="main-area">
          <div class="toolbar">
            <div class="search-box">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索题目名称或编号..."
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
            <div class="stats-info" v-if="total > 0">
              <span>共 <strong>{{ total }}</strong> 道题目</span>
            </div>
          </div>

          <div class="problems-list">
            <div v-if="loading" class="loading-state">
              <div class="loading-spinner"></div>
              <span>加载中...</span>
            </div>

            <template v-else-if="problemsList.length > 0">
              <a 
                v-for="problem in problemsList" 
                :key="problem.id" 
                class="problem-card"
                @click="goToProblem(problem.id)"
              >
                <div class="card-left">
                  <div class="problem-status">
                    <span v-if="problem.solved" class="status-icon solved">✓</span>
                    <span v-else-if="problem.attempted" class="status-icon attempted">!</span>
                    <span v-else class="status-dot"></span>
                  </div>
                  <div class="problem-info">
                    <div class="problem-title-row">
                      <span class="problem-id">{{ problem.id }}.</span>
                      <span class="problem-name">{{ problem.title }}</span>
                    </div>
                    <div class="problem-meta">
                      <span :class="['difficulty-tag', getDifficultyClass(problem.difficulty)]">
                        {{ getDifficultyLabel(problem.difficulty) }}
                      </span>
                      <span class="acceptance-text">
                        通过率 {{ (problem.acceptance || 0).toFixed(1) }}%
                      </span>
                    </div>
                  </div>
                </div>
                <div class="card-right">
                  <div class="category-tags">
                    <span 
                      v-for="tag in (problem.typeList || []).slice(0, 2)" 
                      :key="tag.id" 
                      class="category-tag"
                    >
                      {{ tag.name || tag }}
                    </span>
                  </div>
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="card-arrow">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                    <polyline points="12 5 19 12 12 19"/>
                  </svg>
                </div>
              </a>
            </template>

            <div v-else class="empty-state">
              <span class="empty-icon">🔍</span>
              <h3>没有找到匹配的题目</h3>
              <p>试试调整筛选条件，或者换个关键词搜索</p>
              <button class="action-btn" @click="resetFilters">重置筛选</button>
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
              :disabled="problemsList.length < pageSize"
              @click="handleCurrentChange(currentPage + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProblems, getProblemTypes } from '../services/api'

const router = useRouter()

const searchQuery = ref('')
const difficultyFilter = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const problemsList = ref([])
const categories = ref([])
const loading = ref(false)

const hasActiveFilters = computed(() => {
  return difficultyFilter.value || categoryFilter.value || statusFilter.value !== '' || searchQuery.value
})

const difficultyOptions = [
  { label: '全部', value: '' },
  { label: '简单', value: 'Easy' },
  { label: '中等', value: 'Medium' },
  { label: '困难', value: 'Hard' }
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const getDifficultyClass = (difficulty) => {
  const map = { 'Easy': 'easy', 'Medium': 'medium', 'Hard': 'hard' }
  return map[difficulty] || ''
}

const getDifficultyLabel = (difficulty) => {
  const map = { 'Easy': '简单', 'Medium': '中等', 'Hard': '困难' }
  return map[difficulty] || difficulty
}

const handleDifficultyChange = (val) => {
  difficultyFilter.value = difficultyFilter.value === val ? '' : val
  currentPage.value = 1
  loadProblems()
}

const handleCategoryChange = (val) => {
  categoryFilter.value = categoryFilter.value === val ? '' : val
  currentPage.value = 1
  loadProblems()
}

const handleStatusChange = (val) => {
  statusFilter.value = statusFilter.value === val ? '' : val
  currentPage.value = 1
  loadProblems()
}

const handleSearch = () => {
  currentPage.value = 1
  loadProblems()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadProblems()
}

const resetFilters = () => {
  difficultyFilter.value = ''
  categoryFilter.value = ''
  statusFilter.value = ''
  searchQuery.value = ''
  currentPage.value = 1
  loadProblems()
}

const goToProblem = (id) => {
  router.push(`/problems/${id}`)
}

const loadCategories = async () => {
  try {
    const data = await getProblemTypes()
    categories.value = [{ id: '', name: '全部' }, ...data]
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const loadProblems = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (difficultyFilter.value) {
      params.difficulty = difficultyFilter.value
    }
    if (categoryFilter.value) {
      params.problemTypeId = categoryFilter.value
    }
    if (statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    
    const data = await getProblems(params)
    total.value = data.total || 0
    problemsList.value = data.records || []
  } catch (e) {
    console.error('加载题目失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadProblems()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

.problems-page {
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
  font-size: 1.5rem;
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
  gap: 24px;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
}

.sidebar-section {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}

.sidebar-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0 0 12px 0;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-btn {
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: white;
  border-radius: 6px;
  font-size: 0.8125rem;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: rgba(0, 0, 0, 0.7);
}

.filter-btn:hover {
  border-color: rgba(0, 0, 0, 0.15);
}

.filter-btn.active {
  background: #000;
  border-color: #000;
  color: #fff;
}

.reset-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: transparent;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.reset-btn:hover {
  background: rgba(0, 0, 0, 0.03);
}

.main-area {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.search-box {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 400px;
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

.stats-info {
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.6);
}

.stats-info strong {
  color: #000;
  font-weight: 600;
}

.problems-list {
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

.problem-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.problem-card:hover {
  border-color: rgba(0, 0, 0, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.card-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.problem-status {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon {
  font-size: 14px;
  font-weight: 600;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon.solved {
  background: #22c55e;
  color: white;
}

.status-icon.attempted {
  background: #f59e0b;
  color: white;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.1);
}

.problem-info {
  flex: 1;
  min-width: 0;
}

.problem-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.problem-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.5);
}

.problem-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #000;
}

.problem-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.difficulty-tag {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.difficulty-tag.easy {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.difficulty-tag.medium {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.difficulty-tag.hard {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.acceptance-text {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.5);
}

.card-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.category-tags {
  display: flex;
  gap: 6px;
}

.category-tag {
  font-size: 0.75rem;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
  color: rgba(0, 0, 0, 0.6);
}

.card-arrow {
  color: rgba(0, 0, 0, 0.25);
  transition: all 0.2s ease;
}

.problem-card:hover .card-arrow {
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
  margin: 0 0 20px 0;
}

.action-btn {
  padding: 10px 20px;
  background: #000;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.action-btn:hover {
  opacity: 0.85;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
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

@media (max-width: 768px) {
  .content-wrapper {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    display: flex;
    gap: 12px;
    overflow-x: auto;
  }
  
  .sidebar-section {
    min-width: 160px;
    flex-shrink: 0;
  }
  
  .card-right {
    display: none;
  }
}
</style>
