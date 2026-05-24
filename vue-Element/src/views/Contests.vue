<template>
  <div class="contests-container">
    <div class="contests-header">
      <h2>比赛管理</h2>
      <el-button type="primary" @click="handleAddContest">新增比赛</el-button>
    </div>
    
    <div class="contests-filter">
      <el-input
        v-model="searchQuery"
        placeholder="搜索比赛"
        prefix-icon="el-icon-search"
        style="width: 300px; margin-right: 10px;"
      ></el-input>
      <el-select
        v-model="statusFilter"
        placeholder="状态筛选"
        style="width: 120px; margin-right: 10px;"
      >
        <el-option label="全部" value=""></el-option>
        <el-option label="即将开始" value="Upcoming"></el-option>
        <el-option label="进行中" value="Running"></el-option>
        <el-option label="已结束" value="Ended"></el-option>
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
    
    <el-table :data="contestsList" style="width: 100%">
      <el-table-column prop="id" label="比赛ID" width="80"></el-table-column>
      <el-table-column prop="title" label="比赛名称" min-width="200">
        <template #default="scope">
          <span class="contest-title">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="比赛类型" width="150">
        <template #default="scope">
          <el-tag type="info">{{ scope.row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusName(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="180"></el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="180"></el-table-column>
      <el-table-column prop="createdBy" label="创建者" width="100"></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <div style="display: flex; gap: 8px;">
            <el-button size="small" @click="handleViewContest(scope.row)">查看</el-button>
            <el-button size="small" type="primary" @click="handleEditContest(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteContest(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="contests-pagination">
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
    
    <!-- 比赛编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="70%"
    >
      <el-form
        ref="contestFormRef"
        :model="contestForm"
        :rules="contestRules"
        label-width="100px"
      >
        <el-form-item prop="title">
          <el-input v-model="contestForm.title" placeholder="比赛名称"></el-input>
        </el-form-item>
        <el-form-item prop="description">
          <el-input
            v-model="contestForm.description"
            type="textarea"
            :rows="4"
            placeholder="比赛描述"
          ></el-input>
        </el-form-item>
        <el-form-item prop="type">
          <el-select v-model="contestForm.type" placeholder="比赛类型">
            <el-option label="周赛" value="Weekly Contest"></el-option>
            <el-option label="双周赛" value="Biweekly Contest"></el-option>
            <el-option label="模拟面试" value="Mock Interview"></el-option>
            <el-option label="企业竞赛" value="Company Contest"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="startTime">
          <el-date-picker
            v-model="contestForm.startTime"
            type="datetime"
            placeholder="开始时间"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item prop="endTime">
          <el-date-picker
            v-model="contestForm.endTime"
            type="datetime"
            placeholder="结束时间"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <label>选择题目</label>
          <div class="problem-selector">
            <el-select
              v-model="selectedProblemId"
              placeholder="选择题目"
              style="width: 300px; margin-right: 10px;"
            >
              <el-option
                v-for="problem in problemsList"
                :key="problem.id"
                :label="problem.title"
                :value="problem.id"
              ></el-option>
            </el-select>
            <el-input-number
              v-model="problemScore"
              :min="1"
              :max="100"
              :step="1"
              placeholder="分数"
              style="width: 100px; margin-right: 10px;"
            ></el-input-number>
            <el-button type="primary" @click="handleAddProblem">添加</el-button>
          </div>
        </el-form-item>
        
        <el-form-item>
          <label>已选题目</label>
          <div v-if="selectedProblems.length > 0" class="selected-problems">
            <el-card
              v-for="(item, index) in selectedProblems"
              :key="item.id"
              class="selected-problem-card"
            >
              <div class="selected-problem-content">
                <span class="problem-title">{{ item.title }}</span>
                <div class="problem-score">
                  <span>分数：</span>
                  <el-input-number
                    v-model="item.score"
                    :min="1"
                    :max="100"
                    :step="1"
                    style="width: 100px;"
                  ></el-input-number>
                </div>
                <el-button type="danger" size="small" @click="handleRemoveProblem(index)">删除</el-button>
              </div>
            </el-card>
          </div>
          <div v-else class="no-problems">
            暂无已选题目
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addContestApi, pageContestApi, getContestByIdApi, updateContestApi, deleteContestApi } from '../api/contests'
import { getProblemsApi } from '../api/problems'

const contestsList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const statusFilter = ref('')

// 题目相关
const problemsList = ref([])
const selectedProblems = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增比赛')
const contestFormRef = ref(null)
const selectedProblemId = ref(null)
const problemScore = ref(10)
const contestForm = reactive({
  id: null,
  title: '',
  description: '',
  startTime: null,
  endTime: null,
  type: 'Weekly Contest',
  status: 'Upcoming',
  problemList: []
})

const contestRules = {
  title: [{ required: true, message: '请输入比赛名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入比赛描述', trigger: 'blur' }],
  type: [{ required: true, message: '请选择比赛类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const getStatusType = (status) => {
  switch (status) {
    case 'Upcoming': return 'info'
    case 'Running': return 'success'
    case 'Ended': return 'danger'
    default: return ''
  }
}

const getStatusName = (status) => {
  switch (status) {
    case 'Upcoming': return '即将开始'
    case 'Running': return '进行中'
    case 'Ended': return '已结束'
    default: return status
  }
}

const getContests = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value,
      title: searchQuery.value
    }
    const response = await pageContestApi(params)
    contestsList.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    ElMessage.error('获取比赛列表失败')
    console.error('获取比赛列表失败:', error)
  }
}

// 获取题目列表
const getProblems = async () => {
  try {
    const params = {
      page: 1,
      pageSize: 100 // 一次性获取较多题目，避免分页
    }
    const response = await getProblemsApi(params)
    problemsList.value = response.data.records || []
  } catch (error) {
    ElMessage.error('获取题目列表失败')
    console.error('获取题目列表失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getContests()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getContests()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getContests()
}

const handleAddContest = () => {
  dialogTitle.value = '新增比赛'
  Object.assign(contestForm, {
    id: null,
    title: '',
    description: '',
    startTime: null,
    endTime: null,
    type: 'Weekly Contest',
    status: 'Upcoming',
    problemList: []
  })
  selectedProblems.value = []
  selectedProblemId.value = null
  problemScore.value = 10
  dialogVisible.value = true
}

const handleEditContest = async (contest) => {
  dialogTitle.value = '编辑比赛'
  
  try {
    // 调用后端接口获取比赛详情
    const response = await getContestByIdApi(contest.id)
    console.log('获取比赛详情响应:', response)
    
    if (response.data) {
      // 使用后端返回的最新数据
      const contestDetail = response.data
      Object.assign(contestForm, contestDetail)
      
      // 处理已选题目
      if (contestDetail.problemList && contestDetail.problemList.length > 0) {
        selectedProblems.value = contestDetail.problemList.map(problem => {
          // 查找题目信息
          const problemInfo = problemsList.value.find(p => p.id === problem.id)
          return {
            id: problem.id,
            title: problemInfo ? problemInfo.title : `题目${problem.id}`,
            score: problem.score || 10
          }
        })
      } else {
        selectedProblems.value = []
      }
      
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取比赛详情失败')
    console.error('获取比赛详情失败:', error)
  }
}

const handleAddProblem = () => {
  if (!selectedProblemId.value) {
    ElMessage.warning('请选择题目')
    return
  }
  
  if (!problemScore.value) {
    ElMessage.warning('请输入分数')
    return
  }
  
  // 检查题目是否已添加
  const existingIndex = selectedProblems.value.findIndex(item => item.id === selectedProblemId.value)
  if (existingIndex !== -1) {
    ElMessage.warning('题目已添加')
    return
  }
  
  // 查找题目信息
  const problem = problemsList.value.find(p => p.id === selectedProblemId.value)
  if (problem) {
    selectedProblems.value.push({
      id: problem.id,
      title: problem.title,
      score: problemScore.value
    })
    
    // 重置选择
    selectedProblemId.value = null
    problemScore.value = 10
  }
}

const handleRemoveProblem = (index) => {
  selectedProblems.value.splice(index, 1)
}

const handleViewContest = (contest) => {
  dialogTitle.value = '比赛详情'
  Object.assign(contestForm, contest)
  
  // 处理已选题目
  if (contest.problemList && contest.problemList.length > 0) {
    selectedProblems.value = contest.problemList.map(problem => {
      // 查找题目信息
      const problemInfo = problemsList.value.find(p => p.id === problem.id)
      return {
        id: problem.id,
        title: problemInfo ? problemInfo.title : `题目${problem.id}`,
        score: problem.score || 10
      }
    })
  } else {
    selectedProblems.value = []
  }
  
  dialogVisible.value = true
}

const handleDeleteContest = (id) => {
  ElMessageBox.confirm('确定要删除这个比赛吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteContestApi(id)
      ElMessage.success('删除成功')
      getContests()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('删除比赛失败:', error)
    }
  })
}

const handleSubmitForm = async () => {
  if (!contestFormRef.value) return
  
  try {
    await contestFormRef.value.validate()
    
    // 处理selectedProblems，转换为problemList
    contestForm.problemList = selectedProblems.value.map(item => {
      return {
        id: item.id,
        score: item.score
      }
    })
    
    // 处理时间格式，转换为后端期望的LocalDateTime格式
    const submitData = { ...contestForm }
    if (submitData.startTime) {
      // 转换为LocalDateTime格式：yyyy-MM-dd HH:mm（不包含秒数）
      const date = new Date(submitData.startTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      submitData.startTime = `${year}-${month}-${day} ${hours}:${minutes}`
    }
    if (submitData.endTime) {
      const date = new Date(submitData.endTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      submitData.endTime = `${year}-${month}-${day} ${hours}:${minutes}`
    }
    
    if (contestForm.id) {
      await updateContestApi(submitData)
      ElMessage.success('更新成功')
    } else {
      await addContestApi(submitData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getContests()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('提交比赛失败:', error)
  }
}

onMounted(() => {
  getContests()
  getProblems()
})
</script>

<style scoped>
.contests-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.contests-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.contests-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.contests-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.contests-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.contest-title {
  font-weight: 500;
  color: #3498db;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

/* 题目选择器样式 */
.problem-selector {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

/* 已选题目样式 */
.selected-problems {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selected-problem-card {
  margin-bottom: 10px;
}

.selected-problem-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.problem-title {
  font-weight: 500;
  color: #333;
  flex: 1;
}

.problem-score {
  display: flex;
  align-items: center;
  margin-right: 20px;
}

.no-problems {
  text-align: center;
  padding: 20px;
  color: #999;
  background-color: #f9f9f9;
  border-radius: 4px;
}
</style>