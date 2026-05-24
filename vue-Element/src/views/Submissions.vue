<template>
  <div class="submissions-container">
    <div class="submissions-header">
      <h2>提交记录管理</h2>
    </div>
    
    <div class="submissions-filter">
      <el-input
        v-model="userId"
        placeholder="用户ID"
        prefix-icon="el-icon-user"
        style="width: 200px; margin-right: 10px;"
        clearable
      ></el-input>
      <el-input
        v-model="problemId"
        placeholder="题目ID"
        prefix-icon="el-icon-search"
        style="width: 200px; margin-right: 10px;"
        clearable
      ></el-input>
      <el-select
        v-model="statusFilter"
        placeholder="状态筛选"
        style="width: 150px; margin-right: 10px;"
        clearable
      >
        <el-option label="通过" value="Accepted"></el-option>
        <el-option label="未通过" value="NotAccepted"></el-option>
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    
    <el-table :data="submissionsList" style="width: 100%">
      <el-table-column prop="id" label="提交ID" width="100"></el-table-column>
      <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
      <el-table-column prop="username" label="用户名" width="120"></el-table-column>
      <el-table-column prop="problemId" label="题目ID" width="80"></el-table-column>
      <el-table-column prop="problemTitle" label="题目标题" width="200" show-overflow-tooltip></el-table-column>
      <el-table-column prop="language" label="语言" width="100">
        <template #default="scope">
          <el-tag type="info">{{ scope.row.language }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusName(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="runtimeMs" label="运行时间" width="100">
        <template #default="scope">
          {{ scope.row.runtimeMs || '-' }} ms
        </template>
      </el-table-column>
      <el-table-column prop="memoryKb" label="内存消耗" width="100">
        <template #default="scope">
          {{ scope.row.memoryKb || '-' }} KB
        </template>
      </el-table-column>
      <el-table-column prop="testCasesPassed" label="通过测试用例" width="120">
        <template #default="scope">
          {{ scope.row.testCasesPassed || 0 }} / {{ scope.row.testCasesTotal || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180"></el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleViewSubmission(scope.row)">查看</el-button>
          <el-button size="small" type="danger" @click="handleDeleteSubmission(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="submissions-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      ></el-pagination>
    </div>
    
    <!-- 提交记录详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="80%"
    >
      <el-form :model="submissionForm" label-width="100px">
        <el-form-item label="提交ID">
          <el-input v-model="submissionForm.id" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="submissionForm.userId" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="submissionForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="题目ID">
          <el-input v-model="submissionForm.problemId" disabled></el-input>
        </el-form-item>
        <el-form-item label="题目标题">
          <el-input v-model="submissionForm.problemTitle" disabled></el-input>
        </el-form-item>
        <el-form-item label="编程语言">
          <el-input v-model="submissionForm.language" disabled></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="getStatusType(submissionForm.status)">
            {{ getStatusName(submissionForm.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="运行时间">
          <el-input :value="`${submissionForm.runtimeMs || '-'} ms`" disabled></el-input>
        </el-form-item>
        <el-form-item label="内存消耗">
          <el-input :value="`${submissionForm.memoryKb || '-'} KB`" disabled></el-input>
        </el-form-item>
        <el-form-item label="测试用例">
          <el-input :value="`${submissionForm.testCasesPassed || 0} / ${submissionForm.testCasesTotal || 0}`" disabled></el-input>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-input v-model="submissionForm.submitTime" disabled></el-input>
        </el-form-item>
        <el-form-item label="提交代码">
          <el-input
            v-model="submissionForm.code"
            type="textarea"
            :rows="10"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item label="错误信息" v-if="submissionForm.errorInfo">
          <el-input
            v-model="submissionForm.errorInfo"
            type="textarea"
            :rows="5"
            disabled
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSubmissionsPageApi, deleteSubmissionApi } from '../api'

const submissionsList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userId = ref('')
const problemId = ref('')
const statusFilter = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('提交记录详情')
const submissionForm = reactive({})

const getStatusType = (status) => {
  switch (status) {
    case 'Accepted': return 'success'
    case 'Wrong Answer': return 'danger'
    case 'Time Limit Exceeded': return 'warning'
    case 'Memory Limit Exceeded': return 'warning'
    case 'Runtime Error': return 'danger'
    case 'Compile Error': return 'danger'
    case 'Pending': return 'info'
    case 'Judging': return 'info'
    default: return ''
  }
}

const getStatusName = (status) => {
  switch (status) {
    case 'Accepted': return '通过'
    case 'Wrong Answer': return '答案错误'
    case 'Time Limit Exceeded': return '超时'
    case 'Memory Limit Exceeded': return '内存超限'
    case 'Runtime Error': return '运行错误'
    case 'Compile Error': return '编译错误'
    case 'Pending': return '等待中'
    case 'Judging': return '判题中'
    default: return status
  }
}

const getSubmissions = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      userId: userId.value,
      problemId: problemId.value,
      status: statusFilter.value
    }
    const response = await getSubmissionsPageApi(params)
    // 后端返回结构: { code: 1, data: { total, records } }
    const pageData = response.data || {}
    submissionsList.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error('获取提交记录失败')
    console.error('获取提交记录失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getSubmissions()
}

const handleReset = () => {
  userId.value = ''
  problemId.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  getSubmissions()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getSubmissions()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getSubmissions()
}

const handleViewSubmission = (submission) => {
  dialogTitle.value = '提交记录详情'
  Object.assign(submissionForm, submission)
  dialogVisible.value = true
}

const handleDeleteSubmission = (id) => {
  ElMessageBox.confirm('确定要删除这个提交记录吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSubmissionApi(id)
      ElMessage.success('删除成功')
      getSubmissions()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

onMounted(() => {
  getSubmissions()
})
</script>

<style scoped>
.submissions-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.submissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.submissions-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.submissions-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.submissions-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>