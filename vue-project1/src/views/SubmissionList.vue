<template>
  <div class="submissions-page">
    <div class="submissions-container">
      <h1 class="page-title">提交记录</h1>

      <div class="submissions-toolbar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="通过" value="Accepted" />
          <el-option label="答案错误" value="Wrong Answer" />
          <el-option label="编译错误" value="Compile Error" />
          <el-option label="运行时错误" value="Runtime Error" />
          <el-option label="超时" value="Time Limit Exceeded" />
          <el-option label="内存超限" value="Memory Limit Exceeded" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <div class="submissions-table">
        <el-table :data="submissionsList" style="width: 100%">
          <el-table-column prop="submissionId" label="提交ID" width="90" />
          <el-table-column prop="title" label="题目" min-width="200">
            <template #default="scope">
              <router-link :to="`/problems/${scope.row.problemId}`" class="problem-link">
                {{ scope.row.title || `题目#${scope.row.problemId}` }}
              </router-link>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="140">
            <template #default="scope">
              <span :class="['status-text', scope.row.status === 'Accepted' ? 'accepted' : 'rejected']">
                {{ getStatusLabel(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="通过用例" width="110">
            <template #default="scope">
              {{ scope.row.testCasesPassed }} / {{ scope.row.testCasesTotal }}
            </template>
          </el-table-column>
          <el-table-column label="执行用时" width="110">
            <template #default="scope">
              {{ scope.row.runtimeMs != null ? scope.row.runtimeMs + ' ms' : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="内存消耗" width="110">
            <template #default="scope">
              {{ scope.row.memoryKb != null ? (scope.row.memoryKb / 1024).toFixed(1) + ' MB' : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="180" />
        </el-table>
      </div>

      <div v-if="submissionsList.length === 0" class="empty-state">
        <el-empty description="暂无提交记录" />
      </div>

      <div class="submissions-pagination" v-if="total > 0">
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSubmissionsApi } from '../api/submissions'
import { ElMessage } from 'element-plus'

const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const submissionsList = ref([])
const allSubmissions = ref([])

const getStatusLabel = (status) => {
  const map = {
    'Accepted': '通过',
    'Wrong Answer': '答案错误',
    'Compile Error': '编译错误',
    'Runtime Error': '运行时错误',
    'Time Limit Exceeded': '超时',
    'Memory Limit Exceeded': '内存超限'
  }
  return map[status] || status
}

const handleSearch = () => {
  currentPage.value = 1
  applyFilter()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  applyFilter()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  applyFilter()
}

// 前端筛选 + 分页
const applyFilter = () => {
  let data = [...allSubmissions.value]

  if (statusFilter.value) {
    data = data.filter(item => item.status === statusFilter.value)
  }

  total.value = data.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  submissionsList.value = data.slice(start, end)
}

// 调用后端接口获取全部提交记录（不传 problemId）
const loadSubmissions = async () => {
  try {
    const res = await getSubmissionsApi()
    allSubmissions.value = res.data || []
    applyFilter()
  } catch (error) {
    ElMessage.error('获取提交记录失败')
    console.error('获取提交记录失败:', error)
  }
}

onMounted(() => {
  loadSubmissions()
})
</script>

<style scoped>
.submissions-page {
  min-height: calc(100vh - 50px);
  background: #f7f8fa;
  padding: 32px 0;
}

.submissions-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  margin-bottom: 24px;
}

.submissions-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-select {
  width: 160px;
}

.submissions-table {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.problem-link {
  color: #262626;
  text-decoration: none;
  font-weight: 500;
}

.problem-link:hover {
  color: #409eff;
}

.status-text {
  font-weight: 600;
  font-size: 13px;
}

.status-text.accepted {
  color: #00b8a3;
}

.status-text.rejected {
  color: #ff375f;
}

.submissions-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.empty-state {
  padding: 60px 0;
}
</style>
