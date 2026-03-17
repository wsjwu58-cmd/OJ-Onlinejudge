<template>
  <div class="groups-page">
    <div class="groups-container">
      <div class="groups-header-section">
        <h1 class="page-title">题单</h1>
        <p class="page-desc">精选题目集合，系统性刷题提升能力</p>
      </div>

      <!-- 搜索和筛选 -->
      <div class="groups-toolbar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索题单名称"
          prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="difficultyFilter" placeholder="难度范围" clearable class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="简单" value="Easy" />
          <el-option label="中等" value="Medium" />
          <el-option label="困难" value="Hard" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <!-- 题单卡片列表 -->
      <div class="groups-grid">
        <div 
          v-for="group in groupsList" 
          :key="group.id" 
          class="group-card"
          @click="goToGroup(group.id)"
        >
          <div class="group-card-header">
            <div class="group-icon">
              <el-icon :size="24"><Collection /></el-icon>
            </div>
            <div class="group-difficulty" v-if="group.difficultyRange">
              <span :class="['diff-badge', getDifficultyClass(group.difficultyRange)]">
                {{ getDifficultyLabel(group.difficultyRange) }}
              </span>
            </div>
          </div>
          <h3 class="group-title">{{ group.title }}</h3>
          <p class="group-desc">{{ group.description || '暂无描述' }}</p>
          <div class="group-footer">
            <div class="group-stats">
              <span class="stat">
                <el-icon><Document /></el-icon>
                {{ group.problemCount || 0 }} 题
              </span>
              <span class="stat" v-if="group.estimatedDurationMinutes">
                <el-icon><Timer /></el-icon>
                {{ group.estimatedDurationMinutes }} 分钟
              </span>
            </div>
            <div class="group-progress" v-if="group.progress !== undefined">
              <el-progress 
                :percentage="group.progress" 
                :stroke-width="6"
                :show-text="false"
                color="#00b8a3"
              />
              <span class="progress-text">{{ group.progress }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="groupsList.length === 0" class="empty-state">
        <el-empty description="暂无题单" />
      </div>

      <!-- 分页 -->
      <div class="groups-pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 48]"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Document, Timer } from '@element-plus/icons-vue'
import { getGroupsApi } from '../api/groups'

const router = useRouter()

const searchQuery = ref('')
const difficultyFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const groupsList = ref([])

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

const handleSizeChange = (val) => {
  pageSize.value = val
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
  }
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped>
.groups-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 32px 0;
}

.groups-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.groups-header-section {
  margin-bottom: 32px;
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

.groups-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  max-width: 350px;
}

.filter-select {
  width: 140px;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.group-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

.group-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #d9d9d9;
}

.group-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.group-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #f0f5ff;
  color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.diff-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.diff-badge.easy {
  color: #00b8a3;
  background: #e6faf8;
}

.diff-badge.medium {
  color: #ffa116;
  background: #fff7e6;
}

.diff-badge.hard {
  color: #ff375f;
  background: #ffebee;
}

.diff-badge.all {
  color: #722ed1;
  background: #f9f0ff;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.group-desc {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.6;
  margin: 0 0 16px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.group-footer {
  border-top: 1px solid #f5f5f5;
  padding-top: 12px;
}

.group-stats {
  display: flex;
  gap: 16px;
}

.stat {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.group-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.group-progress .el-progress {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #00b8a3;
  font-weight: 600;
  min-width: 36px;
}

.groups-pagination {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.empty-state {
  padding: 80px 0;
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
}
</style>
