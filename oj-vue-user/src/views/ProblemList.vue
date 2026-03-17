<template>
  <div class="problems-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-pattern"></div>
      <div class="bg-code-symbols">
        <span class="symbol">{ }</span>
        <span class="symbol">&lt;/&gt;</span>
        <span class="symbol">[ ]</span>
        <span class="symbol">( )</span>
        <span class="symbol">=&gt;</span>
      </div>
    </div>

    <div class="problems-container">
      <!-- 左侧筛选栏 -->
      <aside class="problems-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-icon">🎯</span>
          <span class="sidebar-text">筛选条件</span>
          <el-button 
            v-if="hasActiveFilters" 
            type="primary" 
            link 
            size="small" 
            @click="resetFilters"
            class="reset-btn"
          >
            重置
          </el-button>
        </div>

        <div class="sidebar-section">
          <h4 class="sidebar-title">
            <span class="title-icon">⚡</span>
            难度
          </h4>
          <div class="filter-tags">
            <button 
              v-for="item in difficultyOptions" 
              :key="item.value"
              :class="['filter-btn', 'difficulty-btn', { active: difficultyFilter === item.value }, item.class]"
              @click="handleDifficultyChange(item.value)"
            >
              <span v-if="item.value" class="star-icon">{{ getDifficultyStars(item.value) }}</span>
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="sidebar-section">
          <h4 class="sidebar-title">
            <span class="title-icon">🏷️</span>
            分类
          </h4>
          <div class="filter-tags category-tags">
            <button 
              v-for="cat in categories" 
              :key="cat.id"
              :class="['filter-btn', 'category-btn', { active: categoryFilter === cat.id }]"
              @click="handleCategoryChange(cat.id)"
            >
              <span v-if="cat.id" class="category-icon">{{ getCategoryIcon(cat.name) }}</span>
              {{ cat.name }}
            </button>
          </div>
        </div>

        <div class="sidebar-section">
          <h4 class="sidebar-title">
            <span class="title-icon">📊</span>
            状态
          </h4>
          <div class="filter-tags status-tags">
            <button 
              v-for="item in statusOptions" 
              :key="item.value"
              :class="['filter-btn', 'status-btn', { active: statusFilter === item.value }]"
              @click="handleStatusChange(item.value)"
            >
              <span v-if="item.value !== ''" class="status-dot" :class="item.value === 1 ? 'online' : 'offline'"></span>
              {{ item.label }}
            </button>
          </div>
        </div>


      </aside>

      <!-- 右侧题目列表 -->
      <div class="problems-main">
        <!-- 签到日历 - 绝对定位到右上角 -->
        <div class="sign-calendar" v-if="$router.currentRoute.value.path === '/problems'">
          <div class="sign-header">
            <h4 class="sidebar-title">
              <span class="title-icon">📅</span>
              每日签到
            </h4>
            <div class="sign-stats">
              <span class="streak">🔥 连续 {{ signCount }} 天</span>
            </div>
          </div>
          
          <div class="calendar-container">
            <div class="calendar-header">
              <span class="month-title">{{ currentYear }}年{{ currentMonth }}月</span>
            </div>
            
            <div class="calendar-weekdays">
              <span v-for="day in weekDays" :key="day" class="weekday">{{ day }}</span>
            </div>
            
            <div class="calendar-days">
              <div 
                v-for="(item, index) in getCalendarDays()" 
                :key="index"
                :class="['calendar-day', { 
                  'empty': !item.day, 
                  'signed': item.signed, 
                  'today': item.isToday,
                  'past': item.day && item.day < new Date().getDate() && !item.signed
                }]"
              >
                <span v-if="item.day" class="day-number">{{ item.day }}</span>
                <span v-if="item.signed && item.day" class="check-icon">✓</span>
              </div>
            </div>
          </div>
          
          <el-button 
            :type="signedToday ? 'info' : 'primary'" 
            :disabled="signedToday"
            class="sign-btn"
            @click="handleSign"
          >
            <el-icon v-if="!signedToday"><Calendar /></el-icon>
            <el-icon v-else><Check /></el-icon>
            {{ signedToday ? '今日已签到' : '立即签到' }}
          </el-button>
        </div>
        
        <!-- 搜索和排序 -->
        <div class="problems-toolbar">
          <div class="search-box">
            <el-input
              v-model="searchQuery"
              placeholder="搜索题目名称或编号..."
              prefix-icon="Search"
              clearable
              class="search-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-button type="primary" class="search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </div>
          <div class="toolbar-stats" v-if="total > 0">
            <span class="stat-item">
              <el-icon><Document /></el-icon>
              共 <strong>{{ total }}</strong> 道题目
            </span>
          </div>
        </div>

        <!-- 题目卡片列表 -->
        <div class="problems-list">
          <!-- 骨架屏 -->
          <template v-if="loading">
            <div v-for="i in 5" :key="i" class="problem-card skeleton-card">
              <div class="skeleton-row">
                <el-skeleton-item variant="text" style="width: 40px" />
                <el-skeleton-item variant="text" style="width: 60%; margin-left: 12px" />
              </div>
              <div class="skeleton-row">
                <el-skeleton-item variant="text" style="width: 60px" />
                <el-skeleton-item variant="text" style="width: 80px" />
                <el-skeleton-item variant="text" style="width: 100px" />
              </div>
            </div>
          </template>

          <!-- 实际题目列表 -->
          <template v-else-if="problemsList.length > 0">
            <div 
              v-for="(problem, index) in problemsList" 
              :key="problem.id" 
              class="problem-card"
              :class="{ 'new-problem': problem.isNew, 'hot-problem': problem.isHot }"
              @click="goToProblem(problem.id)"
            >
              <div class="card-left">
                <div class="problem-status">
                  <el-icon v-if="problem.solved" class="status-icon solved"><CircleCheckFilled /></el-icon>
                  <el-icon v-else-if="problem.attempted" class="status-icon attempted"><WarningFilled /></el-icon>
                  <span v-else class="status-dot empty"></span>
                </div>
                <div class="problem-info">
                  <div class="problem-title-row">
                    <span class="problem-id">{{ problem.id }}.</span>
                    <span class="problem-name">{{ problem.title }}</span>
                    <span v-if="problem.isNew" class="badge new">新题</span>
                    <span v-if="problem.isHot" class="badge hot">🔥 热门</span>
                  </div>
                  <div class="problem-meta">
                    <div class="difficulty-stars">
                      <span v-for="i in getDifficultyStarCount(problem.difficulty)" :key="i" class="star filled">⭐</span>
                      <span v-for="i in (3 - getDifficultyStarCount(problem.difficulty))" :key="'e'+i" class="star empty">☆</span>
                      <span :class="['difficulty-text', getDifficultyClass(problem.difficulty)]">
                        {{ getDifficultyLabel(problem.difficulty) }}
                      </span>
                    </div>
                    <div class="acceptance-bar">
                      <div class="progress-wrapper">
                        <div 
                          class="progress-fill" 
                          :style="{ width: (problem.acceptance || 0) + '%' }"
                          :class="getAcceptanceClass(problem.acceptance)"
                        ></div>
                      </div>
                      <span class="progress-text">{{ (problem.acceptance || 0).toFixed(1) }}%</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="card-right">
                <div class="category-tags">
                  <span 
                    v-for="tag in (problem.typeList || []).slice(0, 3)" 
                    :key="tag.id" 
                    class="category-tag"
                  >
                    <span class="tag-icon">{{ getCategoryIcon(tag.name) }}</span>
                    {{ tag.name || tag }}
                  </span>
                  <span v-if="(problem.typeList || []).length > 3" class="more-tags">
                    +{{ problem.typeList.length - 3 }}
                  </span>
                </div>
                <div class="card-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </template>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <div class="empty-illustration">
              <span class="empty-icon">🔍</span>
              <div class="empty-decorations">
                <span class="deco">?</span>
                <span class="deco">...</span>
                <span class="deco">404</span>
              </div>
            </div>
            <h3 class="empty-title">没有找到匹配的题目</h3>
            <p class="empty-desc">试试调整筛选条件，或者换个关键词搜索</p>
            <el-button type="primary" @click="resetFilters" round>
              <el-icon><RefreshRight /></el-icon>
              重置筛选
            </el-button>
          </div>
        </div>

        <!-- 分页 -->
        <div class="problems-pagination" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled, WarningFilled, Search, Document, ArrowRight, RefreshRight, Calendar, Check } from '@element-plus/icons-vue'
import { getProblems, getProblemTypes, userSign, getSignCount } from '../services/api'
import { ElMessage } from 'element-plus'

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

const signCount = ref(0)
const signedToday = ref(false)
const currentMonth = ref(new Date().getMonth() + 1)
const currentYear = ref(new Date().getFullYear())

const hasActiveFilters = computed(() => {
  return difficultyFilter.value || categoryFilter.value || statusFilter.value !== '' || searchQuery.value
})

const difficultyOptions = [
  { label: '全部', value: '', class: 'all' },
  { label: '简单', value: 'Easy', class: 'easy' },
  { label: '中等', value: 'Medium', class: 'medium' },
  { label: '困难', value: 'Hard', class: 'hard' }
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const categoryIcons = {
  '数组': '[]',
  '链表': '🔗',
  '哈希表': '#️⃣',
  '字符串': '📝',
  '动态规划': '📊',
  '贪心算法': '🎯',
  '双指针': '👆👇',
  '二叉树': '🌳',
  '图': '🕸️',
  '栈': '📚',
  '队列': '🎫',
  '排序': '📋',
  '搜索': '🔎',
  '数学': '🔢',
  '位运算': '💻',
  '递归': '🔄',
  '回溯': '↩️',
  '分治': '✂️'
}

const getCategoryIcon = (name) => {
  return categoryIcons[name] || '📌'
}

const getDifficultyStars = (difficulty) => {
  const map = { 'Easy': '⭐', 'Medium': '⭐⭐', 'Hard': '⭐⭐⭐' }
  return map[difficulty] || ''
}

const getDifficultyStarCount = (difficulty) => {
  const map = { 'Easy': 1, 'Medium': 2, 'Hard': 3 }
  return map[difficulty] || 0
}

const getDifficultyClass = (difficulty) => {
  const map = { 'Easy': 'easy', 'Medium': 'medium', 'Hard': 'hard' }
  return map[difficulty] || ''
}

const getDifficultyLabel = (difficulty) => {
  const map = { 'Easy': '简单', 'Medium': '中等', 'Hard': '困难' }
  return map[difficulty] || difficulty
}

const getAcceptanceClass = (acceptance) => {
  if (acceptance >= 60) return 'high'
  if (acceptance >= 30) return 'medium'
  return 'low'
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

const handleSizeChange = (val) => {
  pageSize.value = val
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

const handleSign = async () => {
  try {
    await userSign()
    ElMessage.success('签到成功！')
    signedToday.value = true
    signCount.value++
  } catch (e) {
    ElMessage.error(e.message || '签到失败')
  }
}

const loadSignData = async () => {
  try {
    const count = await getSignCount()
    signCount.value = count || 0
    signedToday.value = signCount.value > 0
  } catch (e) {
    console.log('获取签到数据失败', e)
  }
}

const getCalendarDays = () => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month - 1, 1).getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const days = []
  
  for (let i = 0; i < firstDay; i++) {
    days.push({ day: '', signed: false, isToday: false })
  }
  
  const today = new Date()
  const todayDate = today.getDate()
  
  for (let i = 1; i <= daysInMonth; i++) {
    const isToday = todayDate === i && 
                    today.getMonth() + 1 === month && 
                    today.getFullYear() === year
    
    let isSigned = false
    if (signCount.value > 0) {
      const firstSignDay = todayDate - signCount.value + 1
      isSigned = i >= firstSignDay && i <= todayDate
    }
    
    days.push({
      day: i,
      signed: isSigned,
      isToday
    })
  }
  
  return days
}

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

onMounted(() => {
  loadCategories()
  loadProblems()
  const token = localStorage.getItem('token')
  if (token) {
    loadSignData()
  }
})
</script>

<style scoped>
.problems-page {
  min-height: calc(100vh - 50px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 24px 0;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 25% 25%, rgba(255, 107, 0, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(0, 184, 163, 0.03) 0%, transparent 50%);
}

.bg-code-symbols {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  padding-top: 100px;
}

.bg-code-symbols .symbol {
  font-family: 'Courier New', monospace;
  font-size: 120px;
  font-weight: bold;
  color: rgba(0, 0, 0, 0.02);
  animation: float 20s ease-in-out infinite;
}

.bg-code-symbols .symbol:nth-child(2) { animation-delay: -5s; }
.bg-code-symbols .symbol:nth-child(3) { animation-delay: -10s; }
.bg-code-symbols .symbol:nth-child(4) { animation-delay: -15s; }
.bg-code-symbols .symbol:nth-child(5) { animation-delay: -20s; }

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

.problems-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 24px;
  position: relative;
  z-index: 1;
}

/* 侧边栏 */
.problems-sidebar {
  width: 240px;
  flex-shrink: 0;
}

.sidebar-header {
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.3);
}

.sidebar-icon {
  font-size: 20px;
}

.sidebar-text {
  font-weight: 600;
  flex: 1;
}

.reset-btn {
  color: white !important;
  font-size: 12px;
}

.sidebar-section {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #595959;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-icon {
  font-size: 14px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-btn {
  border: none;
  background: #f5f7fa;
  border-radius: 20px;
  font-size: 12px;
  padding: 6px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}

.filter-btn:hover {
  background: #e8eaed;
  transform: translateY(-1px);
}

.filter-btn.active {
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.filter-btn.difficulty-btn.active.easy {
  background: linear-gradient(135deg, #00b8a3, #00d4bb);
}

.filter-btn.difficulty-btn.active.medium {
  background: linear-gradient(135deg, #ffa116, #ffb84d);
}

.filter-btn.difficulty-btn.active.hard {
  background: linear-gradient(135deg, #ff375f, #ff6b8a);
}

.filter-btn.category-btn.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.filter-btn.status-btn.active {
  background: linear-gradient(135deg, #11998e, #38ef7d);
}

.star-icon {
  font-size: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #52c41a;
  box-shadow: 0 0 6px rgba(82, 196, 26, 0.5);
}

.status-dot.offline {
  background: #d9d9d9;
}

/* 主内容区 */
.problems-main {
  flex: 1;
  min-width: 0;
  position: relative;
  padding-right: 220px;
}

.problems-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.search-box {
  display: flex;
  gap: 12px;
  flex: 1;
  max-width: 500px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.search-btn {
  border-radius: 24px;
  padding: 0 24px;
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  border: none;
  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.3);
}

.search-btn:hover {
  background: linear-gradient(135deg, #ff8533, #ffaa33);
}

.toolbar-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.stat-item strong {
  color: #ff6b00;
}

/* 题目卡片列表 */
.problems-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.problem-card {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.problem-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #ff6b00, #ff9500);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.problem-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.problem-card:hover::before {
  opacity: 1;
}

.problem-card.new-problem::after {
  content: 'NEW';
  position: absolute;
  top: 12px;
  right: -30px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 40px;
  transform: rotate(45deg);
}

.card-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.problem-status {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon {
  font-size: 24px;
}

.status-icon.solved {
  color: #00b8a3;
}

.status-icon.attempted {
  color: #ffa116;
}

.status-dot.empty {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e8eaed;
  border: 2px dashed #d0d0d0;
}

.problem-info {
  flex: 1;
  min-width: 0;
}

.problem-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.problem-id {
  color: #8c8c8c;
  font-size: 14px;
  font-weight: 500;
}

.problem-name {
  font-size: 16px;
  color: #262626;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.badge.new {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.badge.hot {
  background: linear-gradient(135deg, #ff416c, #ff4b2b);
  color: white;
}

.problem-meta {
  display: flex;
  align-items: center;
  gap: 24px;
}

.difficulty-stars {
  display: flex;
  align-items: center;
  gap: 2px;
}

.star {
  font-size: 12px;
}

.star.filled {
  filter: drop-shadow(0 1px 2px rgba(255, 193, 7, 0.4));
}

.star.empty {
  opacity: 0.3;
}

.difficulty-text {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 600;
}

.difficulty-text.easy {
  color: #00b8a3;
}

.difficulty-text.medium {
  color: #ffa116;
}

.difficulty-text.hard {
  color: #ff375f;
}

.acceptance-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-wrapper {
  width: 80px;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.progress-fill.high {
  background: linear-gradient(90deg, #52c41a, #73d13d);
}

.progress-fill.medium {
  background: linear-gradient(90deg, #faad14, #ffc53d);
}

.progress-fill.low {
  background: linear-gradient(90deg, #ff4d4f, #ff7875);
}

.progress-text {
  font-size: 12px;
  color: #8c8c8c;
  min-width: 45px;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.category-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  max-width: 200px;
}

.category-tag {
  background: linear-gradient(135deg, #f0f5ff, #e6f7ff);
  color: #1890ff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid rgba(24, 144, 255, 0.2);
}

.tag-icon {
  font-size: 10px;
}

.more-tags {
  font-size: 11px;
  color: #8c8c8c;
  padding: 4px 8px;
}

.card-arrow {
  color: #d0d0d0;
  transition: all 0.3s ease;
}

.problem-card:hover .card-arrow {
  color: #ff6b00;
  transform: translateX(4px);
}

/* 骨架屏 */
.skeleton-card {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.skeleton-row:last-child {
  margin-bottom: 0;
}

/* 空状态 */
.empty-state {
  background: white;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.empty-illustration {
  position: relative;
  margin-bottom: 24px;
}

.empty-icon {
  font-size: 80px;
  opacity: 0.8;
}

.empty-decorations {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
}

.empty-decorations .deco {
  font-size: 14px;
  color: #d0d0d0;
  animation: bounce 2s ease-in-out infinite;
}

.empty-decorations .deco:nth-child(2) {
  animation-delay: 0.3s;
}

.empty-decorations .deco:nth-child(3) {
  animation-delay: 0.6s;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: 18px;
  color: #262626;
  margin-bottom: 8px;
}

.empty-desc {
  color: #8c8c8c;
  margin-bottom: 24px;
}

/* 分页 */
.problems-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.problems-pagination :deep(.el-pagination) {
  background: white;
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* 响应式 */
@media (max-width: 900px) {
  .problems-container {
    flex-direction: column;
  }
  
  .problems-sidebar {
    width: 100%;
    display: flex;
    gap: 12px;
    overflow-x: auto;
  }
  
  .sidebar-section {
    min-width: 180px;
    flex-shrink: 0;
  }
  
  .problem-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .card-right {
    display: none;
  }
}

/* 签到日历样式 */
.sign-calendar {
  position: absolute;
  top: 0;
  right: -220px;
  background: white;
  color: #333;
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  width: 250px;
  z-index: 100;
  font-size: 12px;
}

.sign-calendar .sidebar-title {
  color: #595959;
  margin-bottom: 8px;
}

.sign-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sign-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.streak {
  font-size: 12px;
  background: #f5f7fa;
  padding: 4px 10px;
  border-radius: 12px;
  color: #666;
  border: 1px solid #e8eaed;
}

.calendar-container {
  background: #fafafa;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
}

.calendar-header {
  text-align: center;
  margin-bottom: 12px;
}

.month-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  color: #999;
  padding: 4px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 12px;
  position: relative;
  background: white;
  border: 1px solid #e8eaed;
  transition: all 0.2s ease;
}

.calendar-day:hover {
  background: #f5f7fa;
}

.calendar-day.empty {
  background: transparent;
  border: none;
}

.calendar-day.today {
  background: #e6f7ff;
  border-color: #91d5ff;
  box-shadow: 0 0 0 2px rgba(145, 213, 255, 0.3);
}

.calendar-day.signed {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  border-color: #b7eb8f;
  color: #52c41a;
}

.calendar-day.signed .day-number {
  font-weight: 600;
}

.calendar-day.past {
  opacity: 0.5;
}

.day-number {
  font-size: 11px;
  color: #333;
}

.check-icon {
  position: absolute;
  font-size: 10px;
  color: #52c41a;
  font-weight: bold;
  bottom: 2px;
  right: 2px;
}

.sign-btn {
  width: 100%;
  border-radius: 12px;
  font-weight: 600;
  border: 1px solid #d9d9d9;
  background: white;
  color: #1890ff;
}

.sign-btn:hover:not(:disabled) {
  background: #f0f9ff;
  border-color: #91d5ff;
  transform: translateY(-1px);
}

.sign-btn:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  border-color: #d9d9d9;
}

</style>
