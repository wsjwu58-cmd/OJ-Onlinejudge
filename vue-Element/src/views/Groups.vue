<template>
  <div class="groups-container">
    <div class="groups-header">
      <h2>题单管理</h2>
      <div>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedGroups.length === 0">批量删除</el-button>
        <el-button type="primary" @click="handleAddGroup" style="margin-left: 10px;">新增题单</el-button>
      </div>
    </div>
    
    <div class="groups-filter">
      <el-input
        v-model="searchQuery"
        placeholder="搜索题单"
        prefix-icon="el-icon-search"
        style="width: 300px; margin-right: 10px;"
      ></el-input>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
    
    <el-table :data="groupsList" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="id" label="题单ID" width="80"></el-table-column>
      <el-table-column prop="title" label="题单名称" min-width="200"></el-table-column>
      <el-table-column prop="description" label="题单描述" min-width="300"></el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficultyRange" label="难度范围" width="120"></el-table-column>
      <el-table-column prop="estimatedDurationMinutes" label="预计时长(分钟)" width="120"></el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleViewGroup(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEditGroup(scope.row)">编辑</el-button>
          <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" @click="handleStatusChange(scope.row)">
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDeleteGroup(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="groups-pagination">
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
    
    <!-- 题单编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="80%"
    >
      <el-form
        ref="groupFormRef"
        :model="groupForm"
        :rules="groupRules"
        label-width="120px"
      >
        <el-form-item label="题单名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入题单名称"></el-input>
        </el-form-item>
        <el-form-item label="题单描述" prop="description">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入题单描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="题单状态" prop="status">
          <el-select v-model="groupForm.status" placeholder="请选择题单状态">
            <el-option label="上架" value="1"></el-option>
            <el-option label="下架" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="难度范围" prop="difficultyRange">
          <el-select v-model="groupForm.difficultyRange" placeholder="请选择难度范围">
            <el-option label="简单" value="Easy"></el-option>
            <el-option label="中等" value="Medium"></el-option>
            <el-option label="困难" value="Hard"></el-option>
            <el-option label="全部" value="All"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="预计时长(分钟)" prop="estimatedDurationMinutes">
          <el-input-number v-model="groupForm.estimatedDurationMinutes" :min="0" :step="5" placeholder="请输入预计时长"></el-input-number>
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="groupForm.isPublic"></el-switch>
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
                <span class="problem-difficulty">难度：{{ item.difficulty }}</span>
                <span class="problem-type">类型：{{ item.problemType }}</span>
                <el-button type="danger" size="small" @click="handleRemoveProblem(index)">移除</el-button>
              </div>
            </el-card>
          </div>
          <div v-else class="no-problems">
            暂无题目，点击上方按钮添加
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
    
    <!-- 题单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="题单详情"
      width="80%"
    >
      <div class="group-detail">
        <h3>{{ groupDetail.title }}</h3>
        <div class="group-detail-info">
          <span class="status">{{ groupDetail.status === 1 ? '已上架' : '已下架' }}</span>
          <span class="difficulty">难度范围: {{ groupDetail.difficultyRange }}</span>
          <span class="duration">预计时长: {{ groupDetail.estimatedDurationMinutes }}分钟</span>
          <span class="public">是否公开: {{ groupDetail.isPublic ? '是' : '否' }}</span>
        </div>
        <div class="group-detail-description">
          <h4>描述:</h4>
          <p>{{ groupDetail.description }}</p>
        </div>
        <div class="group-detail-other">
          <h4>其他信息:</h4>
          <p>创建者ID: {{ groupDetail.creatorId }}</p>
        </div>
        <div class="group-detail-problems">
          <h4>包含题目:</h4>
          <div v-if="groupDetail.problemList && groupDetail.problemList.length > 0">
            <el-table :data="groupDetail.problemList" style="width: 100%">
              <el-table-column prop="id" label="题目ID" width="80"></el-table-column>
              <el-table-column prop="title" label="题目名称" min-width="300"></el-table-column>
              <el-table-column prop="difficulty" label="难度" width="100"></el-table-column>
              <el-table-column prop="problemType" label="类型" width="120"></el-table-column>
              <el-table-column prop="acceptance" label="通过率" width="100">
                <template #default="scope">
                  {{ scope.row.acceptance }}%
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else class="no-problems">
            该题单暂无题目
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveGroupApi, pageGroupApi, getGroupByIdApi, updateGroupApi, deleteGroupApi, updateGroupStatusApi } from '../api'
import { getAllProblemsApi } from '../api/problems'

const groupsList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const selectedGroups = ref([])

const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dialogTitle = ref('新增题单')
const groupFormRef = ref(null)
const groupDetail = ref({})

// 题目相关变量，参照比赛管理模块
const problemsList = ref([])
const selectedProblems = ref([])
const selectedProblemId = ref(null)

const groupForm = reactive({
  id: null,
  name: '',
  description: '',
  status: 1,
  difficultyRange: '',
  estimatedDurationMinutes: 0,
  isPublic: false,
  problemList: []
})

const groupRules = {
  name: [{ required: true, message: '请输入题单名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入题单描述', trigger: 'blur' }],
  status: [{ required: true, message: '请选择题单状态', trigger: 'change' }]
}

const handleSelectionChange = (selection) => {
  selectedGroups.value = selection
}

const getGroups = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      title: searchQuery.value // 使用 title 而不是 name，后端期望的参数名
    }
    console.log('请求题单列表参数:', params)
    const response = await pageGroupApi(params)
    console.log('题单列表数据:', response)
    // 处理响应数据结构
    const pageResult = response.data || response
    groupsList.value = pageResult.records || []
    console.log('题单列表:', groupsList.value)
    total.value = pageResult.total || 0
  } catch (error) {
    ElMessage.error('获取题单列表失败')
    console.error('获取题单列表失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getGroups()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getGroups()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getGroups()
}

const handleAddGroup = () => {
  dialogTitle.value = '新增题单'
  Object.assign(groupForm, {
    id: null,
    name: '',
    description: '',
    status: 1,
    difficultyRange: '',
    estimatedDurationMinutes: 0,
    isPublic: false,
    problemList: []
  })
  // 重置题目相关变量，参照比赛管理模块
  selectedProblems.value = []
  selectedProblemId.value = null
  dialogVisible.value = true
}

// 从题单中移除题目
const removeProblem = (index) => {
  groupForm.problemList.splice(index, 1)
}

// 获取题目列表，参照比赛管理模块
const getProblems = async () => {
  try {
    // 使用新的查询所有题目的接口
    const response = await getAllProblemsApi()
    console.log('获取所有题目响应:', response)
    const problems = response.data || response
    console.log('获取所有题目数据:', problems)
    problemsList.value = problems
  } catch (error) {
    ElMessage.error('获取题目列表失败')
    console.error('获取题目列表失败:', error)
  }
}

// 添加题目，参照比赛管理模块
const handleAddProblem = () => {
  if (!selectedProblemId.value) {
    ElMessage.warning('请选择题目')
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
      difficulty: problem.difficulty,
      problemType: problem.problemType
    })
    
    // 重置选择
    selectedProblemId.value = null
  }
}

// 移除题目，参照比赛管理模块
const handleRemoveProblem = (index) => {
  selectedProblems.value.splice(index, 1)
}

const handleEditGroup = async (group) => {
  dialogTitle.value = '编辑题单'
  
  try {
    const response = await getGroupByIdApi(group.id)
    console.log('编辑题单响应数据:', response)
    
    // 处理响应数据结构，确保获取正确的数据
    const actualData = response.data || response
    console.log('编辑题单实际数据:', actualData)
    
    // 使用实际数据中的ID，确保参数对应正确
    groupForm.id = actualData.id || group.id
    groupForm.name = actualData.title || ''
    groupForm.description = actualData.description || ''
    groupForm.status = actualData.status || 1
    groupForm.difficultyRange = actualData.difficultyRange || ''
    groupForm.estimatedDurationMinutes = actualData.estimatedDurationMinutes || 0
    groupForm.isPublic = actualData.isPublic || false
    groupForm.problemList = actualData.problemList || []
    
    // 处理已选题目，参照比赛管理模块
    if (actualData.problemList && actualData.problemList.length > 0) {
      selectedProblems.value = actualData.problemList.map(problem => {
        // 查找题目信息
        const problemInfo = problemsList.value.find(p => p.id === problem.id)
        return {
          id: problem.id,
          title: problemInfo ? problemInfo.title : problem.title,
          difficulty: problemInfo ? problemInfo.difficulty : problem.difficulty,
          problemType: problemInfo ? problemInfo.problemType : problem.problemType
        }
      })
    } else {
      selectedProblems.value = []
    }
    
    // 重置选择
    selectedProblemId.value = null
    
    console.log('赋值后groupForm:', groupForm)
    console.log('赋值后selectedProblems:', selectedProblems.value)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取题单详情失败')
    console.error('获取题单详情失败:', error)
  }
}

const handleViewGroup = async (group) => {
  try {
    const response = await getGroupByIdApi(group.id)
    console.log('获取题单详情响应:', response)
    
    // 处理响应数据结构，确保获取正确的数据
    const detail = response.data || response
    console.log('获取题单详情实际数据:', detail)
    
    // 直接赋值，确保数据结构正确
    groupDetail.value = detail
    console.log('赋值后groupDetail:', groupDetail.value)
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取题单详情失败')
    console.error('获取题单详情失败:', error)
  }
}

const handleDeleteGroup = (id) => {
  ElMessageBox.confirm('确定要删除这个题单吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteGroupApi(id)
      ElMessage.success('删除成功')
      getGroups()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  })
}

const handleBatchDelete = () => {
  if (selectedGroups.value.length === 0) {
    ElMessage.warning('请选择要删除的题单')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的${selectedGroups.value.length}个题单吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      for (const group of selectedGroups.value) {
        await deleteGroupApi(group.id)
      }
      ElMessage.success('批量删除成功')
      selectedGroups.value = []
      getGroups()
    } catch (error) {
      ElMessage.error('批量删除失败')
      console.error('批量删除失败:', error)
    }
  })
}

const handleStatusChange = async (group) => {
  try {
    const newStatus = group.status === 1 ? 0 : 1
    await updateGroupStatusApi(newStatus, group.id)
    ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
    getGroups()
  } catch (error) {
    ElMessage.error('修改状态失败')
    console.error('修改状态失败:', error)
  }
}

const handleSubmitForm = async () => {
  if (!groupFormRef.value) return
  
  try {
    await groupFormRef.value.validate()
    
    // 处理selectedProblems，转换为groupProblemsList，参照比赛管理模块
    const problemList = selectedProblems.value.map(item => {
      return {
        id: item.id,
        title: item.title,
        difficulty: item.difficulty,
        problemType: item.problemType
      }
    })
    
    // 构建提交数据，确保参数对应正确
    const submitData = {
      id: groupForm.id,
      title: groupForm.name, // 前端使用name，后端需要title
      description: groupForm.description,
      status: groupForm.status,
      difficultyRange: groupForm.difficultyRange,
      estimatedDurationMinutes: groupForm.estimatedDurationMinutes,
      isPublic: groupForm.isPublic,
      problemList: problemList // 后端期望的字段名是 problemList，不是 groupProblemsList
    }
    console.log('提交的题单数据:', submitData)
    
    if (submitData.id) {
      console.log('调用修改接口')
      await updateGroupApi(submitData)
      ElMessage.success('更新成功')
    } else {
      console.log('调用创建接口')
      await saveGroupApi(submitData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getGroups()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('提交题单失败:', error)
  }
}

onMounted(() => {
  getGroups()
  getProblems() // 参照比赛管理模块，在组件挂载时获取题目列表
})
</script>

<style scoped>
.groups-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.groups-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.groups-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.groups-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.groups-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

/* 题单详情样式 */
.group-detail {
  padding: 20px 0;
}

.group-detail h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 20px;
  color: #333;
}

.group-detail-info {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.group-detail-info span {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #f0f9ff;
  color: #339af0;
}

.group-detail-description {
  margin-bottom: 30px;
  line-height: 1.6;
}

.group-detail-description h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  color: #333;
}

.group-detail-description p {
  margin-bottom: 10px;
  white-space: pre-wrap;
}

/* 题目选择器样式，参照比赛管理模块 */
.problem-selector {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

/* 已选题目样式，参照比赛管理模块 */
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
  flex-wrap: wrap;
  gap: 15px;
}

.problem-title {
  font-weight: 500;
  color: #333;
  flex: 1;
  min-width: 200px;
}

.problem-difficulty {
  color: #666;
}

.problem-type {
  color: #666;
}

.no-problems {
  text-align: center;
  padding: 20px;
  color: #999;
  background-color: #f9f9f9;
  border-radius: 4px;
  margin: 10px 0;
}

.group-detail-problems {
  margin-top: 30px;
}

.group-detail-problems h4 {
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}

/* 操作按钮样式 */
.el-table .el-table__cell .el-button {
  margin-right: 6px;
  padding: 6px 12px;
  font-size: 12px;
}

/* 确保操作按钮在一行显示 */
.el-table .el-table__cell .cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 调整表格列间距 */
.el-table .el-table__cell {
  padding: 12px 8px;
}

/* 调整表格标题间距 */
.el-table .el-table__header-wrapper th {
  padding: 12px 8px;
}
</style>