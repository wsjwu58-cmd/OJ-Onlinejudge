<template>
  <div class="problems-container">
    <div class="problems-header">
      <h2>题目管理</h2>
      <div>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedProblems.length === 0">批量删除</el-button>
        <el-dropdown style="margin-left: 10px;">
          <el-button type="primary">
            新增题目
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleAddProblem">创建题目</el-dropdown-item>
              <el-dropdown-item @click="navigateToAddProblemPage">单独页面添加</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <div class="problems-filter">
      <el-input
        v-model="searchQuery"
        placeholder="搜索题目"
        prefix-icon="el-icon-search"
        style="width: 300px; margin-right: 10px;"
      ></el-input>
      <el-select
        v-model="difficultyFilter"
        placeholder="难度筛选"
        style="width: 120px; margin-right: 10px;"
      >
        <el-option label="全部" value=""></el-option>
        <el-option label="简单" value="Easy"></el-option>
        <el-option label="中等" value="Medium"></el-option>
        <el-option label="困难" value="Hard"></el-option>
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
    
    <el-table :data="problemsList" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="id" label="题目ID" width="80"></el-table-column>
      <el-table-column prop="title" label="题目名称" min-width="200">
        <template #default="scope">
          <span class="problem-title">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="scope">
          <el-tag :type="getDifficultyType(scope.row.difficulty)">
            {{ scope.row.difficulty }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大类" width="120">
        <template #default="scope">
          <span>{{ scope.row.problemType || '未分类' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="小类" width="180">
        <template #default="scope">
          <div class="problem-subtypes">
            <el-tag v-for="(typeId, index) in scope.row.typeList" :key="index" size="small" style="margin-right: 4px;">
              {{ getTypeName(typeId) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="acceptance" label="通过率" width="100">
        <template #default="scope">
          {{ scope.row.acceptance }}%
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作" width="400" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleViewProblem(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEditProblem(scope.row)">编辑</el-button>
          <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" @click="handleStatusChange(scope.row)">
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="info" @click="handleTestCases(scope.row)">测试用例</el-button>
          <el-button size="small" type="danger" @click="handleDeleteProblem(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="problems-pagination">
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
    
    <!-- 题目编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="80%"
    >
      <el-form
        ref="problemFormRef"
        :model="problemForm"
        :rules="problemRules"
        label-width="100px"
      >
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="problemForm.title" placeholder="请输入题目标题"></el-input>
        </el-form-item>
        <el-form-item label="题目描述" prop="content">
          <el-input
            v-model="problemForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入题目描述 (Markdown格式)"
          ></el-input>
        </el-form-item>
        <el-form-item label="题目难度" prop="difficulty">
          <el-select v-model="problemForm.difficulty" placeholder="请选择题目难度">
            <el-option label="简单" value="Easy"></el-option>
            <el-option label="中等" value="Medium"></el-option>
            <el-option label="困难" value="Hard"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="题目大类" prop="problemType">
          <el-select v-model="problemForm.problemType" placeholder="请选择题目大类">
            <el-option label="算法" value="Algorithm"></el-option>
            <el-option label="数据库" value="Database"></el-option>
            <el-option label="Shell" value="Shell"></el-option>
            <el-option label="并发" value="Concurrency"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="题目小类" prop="typeList">
          <el-select v-model="problemForm.typeList" placeholder="请选择题目小类" multiple>
            <template v-if="categoriesList.length > 0">
              <el-option v-for="category in categoriesList" :key="category.id" :label="category.name" :value="category.id"></el-option>
            </template>
            <template v-else>
              <el-option label="无分类数据" value="" disabled></el-option>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="内存限制" prop="memoryLimitMb">
          <el-input-number v-model="problemForm.memoryLimitMb" :min="1" placeholder="请输入内存限制 (MB)"></el-input-number>
        </el-form-item>

        <el-form-item label="Java模板" prop="templateCode.java">
          <el-input
            v-model="problemForm.templateCode.java"
            type="textarea"
            :rows="6"
            placeholder="请输入Java代码模板"
          ></el-input>
        </el-form-item>
        <el-form-item label="Python模板" prop="templateCode.python">
          <el-input
            v-model="problemForm.templateCode.python"
            type="textarea"
            :rows="6"
            placeholder="请输入Python代码模板"
          ></el-input>
        </el-form-item>
        <el-form-item label="C++模板" prop="templateCode.cpp">
          <el-input
            v-model="problemForm.templateCode.cpp"
            type="textarea"
            :rows="6"
            placeholder="请输入C++代码模板"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 题目详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="题目详情"
      width="80%"
    >
      <div class="problem-detail">
        <h3>{{ problemDetail.title }}</h3>
        <div class="problem-detail-info">
          <span class="difficulty">难度: {{ problemDetail.difficulty }}</span>
          <span class="main-type">大类: {{ problemDetail.problemType }}</span>
          <span class="status">{{ problemDetail.status === 1 ? '已上架' : '已下架' }}</span>
        </div>
        <div class="problem-detail-content" v-highlight v-html="processedContent"></div>
        <div class="problem-detail-subtypes">
          <h4>小类:</h4>
          <div class="subtype-tags">
            <el-tag v-for="(type, index) in problemDetail.typeList" :key="index" size="small" style="margin-right: 4px;">
              {{ typeof type === 'object' ? type.name : getTypeName(type) }}
            </el-tag>
          </div>
        </div>
        <div class="problem-detail-templates">
          <h4>代码模板:</h4>
          <div v-if="problemDetail.templateCode">
            <div class="template-item">
              <h5>Java:</h5>
              <pre>{{ problemDetail.templateCode.java }}</pre>
            </div>
            <div class="template-item">
              <h5>Python:</h5>
              <pre>{{ problemDetail.templateCode.python }}</pre>
            </div>
            <div class="template-item">
              <h5>C++:</h5>
              <pre>{{ problemDetail.templateCode.cpp }}</pre>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 测试用例管理对话框 -->
    <el-dialog
      v-model="testCasesDialogVisible"
      :title="`${currentProblem?.title || ''} - 测试用例管理`"
      width="80%"
    >
      <div class="test-cases-container">
        <!-- 测试用例列表 -->
        <div class="test-cases-list" v-if="testCases.length > 0">
          <el-card v-for="(testCase, index) in testCases" :key="testCase.id" class="test-case-card">
            <div class="test-case-header">
              <span class="test-case-index">测试用例 {{ index + 1 }}</span>
              <span class="test-case-type" :class="{ 'sample': testCase.isSample }">
                {{ testCase.isSample ? '示例' : '普通' }}
              </span>
            </div>
            <div class="test-case-content">
              <div class="test-case-input">
                <label>输入：</label>
                <pre>{{ testCase.inputData }}</pre>
              </div>
              <div class="test-case-output">
                <label>期望输出：</label>
                <pre>{{ testCase.outputData }}</pre>
              </div>
              <div class="test-case-extra" v-if="testCase.timeLimitMs || testCase.memoryLimitMb || testCase.scoreWeight !== 1.00">
                <div v-if="testCase.timeLimitMs">时间限制：{{ testCase.timeLimitMs }}ms</div>
                <div v-if="testCase.memoryLimitMb">内存限制：{{ testCase.memoryLimitMb }}MB</div>
                <div v-if="testCase.scoreWeight !== 1.00">分数权重：{{ testCase.scoreWeight }}</div>
              </div>
            </div>
            <div class="test-case-actions">
              <el-button size="small" @click="handleEditTestCase(testCase)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteTestCase(testCase.id)">删除</el-button>
            </div>
          </el-card>
        </div>
        <div v-else class="no-test-cases">
          暂无测试用例，点击下方按钮添加
        </div>
        
        <!-- 添加测试用例按钮 -->
        <el-button type="primary" @click="handleAddTestCase">添加测试用例</el-button>
      </div>
      
      <!-- 测试用例编辑对话框 -->
      <el-dialog
        v-model="editTestCaseDialogVisible"
        :title="editingTestCase && editingTestCase.id ? '编辑测试用例' : '添加测试用例'"
        width="60%"
      >
        <el-form
          ref="testCaseFormRef"
          :model="testCaseForm"
          :rules="testCaseRules"
          label-width="100px"
        >
          <el-form-item label="输入" prop="inputData">
            <el-input
              v-model="testCaseForm.inputData"
              type="textarea"
              :rows="4"
              placeholder="请输入测试用例输入数据"
            ></el-input>
          </el-form-item>
          <el-form-item label="期望输出" prop="outputData">
            <el-input
              v-model="testCaseForm.outputData"
              type="textarea"
              :rows="4"
              placeholder="请输入测试用例期望输出"
            ></el-input>
          </el-form-item>
          <el-form-item label="是否示例">
            <el-switch v-model="testCaseForm.isSample"></el-switch>
          </el-form-item>
          <el-form-item label="顺序">
            <el-input-number v-model="testCaseForm.orderNum" :min="0" :step="1"></el-input-number>
          </el-form-item>
          <el-form-item label="时间限制(ms)">
            <el-input-number v-model="testCaseForm.timeLimitMs" :min="0" :step="100"></el-input-number>
          </el-form-item>
          <el-form-item label="内存限制(MB)">
            <el-input-number v-model="testCaseForm.memoryLimitMb" :min="0" :step="16"></el-input-number>
          </el-form-item>
          <el-form-item label="分数权重">
            <el-input-number v-model="testCaseForm.scoreWeight" :min="0.01" :max="10.00" :step="0.01" :precision="2"></el-input-number>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="editTestCaseDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmitTestCase">确定</el-button>
          </span>
        </template>
      </el-dialog>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="testCasesDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { getProblemsApi, createProblemApi, updateProblemApi, deleteProblemApi, getProblemDetailApi } from '../api'
import { getAllCategoriesApi } from '../api/categories'
import { getTestCasesApi, createTestCaseApi, updateTestCaseApi, deleteTestCaseApi } from '../api/testcases'
import request from '../util/request'

const route = useRoute()

const problemsList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const difficultyFilter = ref('')
const categoriesList = ref([])
const selectedProblems = ref([])

const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dialogTitle = ref('新增题目')
const problemFormRef = ref(null)
const problemDetail = ref({})

// 测试用例相关
const testCasesDialogVisible = ref(false)
const editTestCaseDialogVisible = ref(false)
const currentProblem = ref(null)
const testCases = ref([])
const testCaseFormRef = ref(null)
const editingTestCase = ref(null)

// 测试用例表单
const testCaseForm = reactive({
  id: null,
  problemId: null,
  inputData: '',
  outputData: '',
  isSample: false,
  orderNum: 0,
  timeLimitMs: null,
  memoryLimitMb: null,
  scoreWeight: 1.00,
  status: 1
})

// 测试用例表单验证规则
const testCaseRules = {
  inputData: [{ required: true, message: '请输入测试用例输入数据', trigger: 'blur' }],
  outputData: [{ required: true, message: '请输入测试用例期望输出', trigger: 'blur' }]
}

// 计算属性：处理后的contentHtml
const processedContent = computed(() => {
  const content = problemDetail.value.contentHtml || problemDetail.value.content
  if (!content) return ''
  
  // 移除反引号
  let result = content.split('`').join('')
  
  // 将图片的src从https://gitee.com替换为/gitee，通过代理访问
  result = result.replace(/src="https:\/\/gitee\.com\//g, 'src="/gitee/')
  
  // 将assets.gitee.com的链接替换为/gitee-assets，通过代理访问
  result = result.replace(/https:\/\/assets\.gitee\.com\//g, '/gitee-assets/')
  
  return result
})

const problemForm = reactive({
  id: null,
  title: '',
  content: '',
  difficulty: '',
  // 大类：固定类别
  problemType: '',
  // 小类：与数据库表关联，支持多选
  typeList: [],
  timeLimitMs: 1000,
  memoryLimitMb: 128,
  status: 1,
  templateCode: {
    java: '',
    python: '',
    cpp: ''
  }
})

const problemRules = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入题目描述', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择题目难度', trigger: 'change' }],
  problemType: [{ required: true, message: '请选择题目大类', trigger: 'change' }],
  typeList: [{ required: true, message: '请选择题目小类', trigger: 'change' }],
  timeLimitMs: [{ required: true, message: '请输入时间限制', trigger: 'blur' }],
  memoryLimitMb: [{ required: true, message: '请输入内存限制', trigger: 'blur' }]
}

const getDifficultyType = (difficulty) => {
  switch (difficulty) {
    case 'Easy': return 'success'
    case 'Medium': return 'warning'
    case 'Hard': return 'danger'
    default: return ''
  }
}

// 根据分类ID获取分类名称
const getTypeName = (typeId) => {
  // 确保typeId存在
  if (!typeId) return '未知'
  
  // 尝试直接从typeId中获取name属性（如果typeId是对象）
  if (typeof typeId === 'object' && typeId.name) {
    return typeId.name
  }
  
  // 查找分类
  const category = categoriesList.value.find(cat => cat && cat.id === typeId)
  
  return category ? category.name : '未知'
}

const handleSelectionChange = (selection) => {
  selectedProblems.value = selection
}

const getProblems = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      title: searchQuery.value,
      difficulty: difficultyFilter.value
    }
    const response = await getProblemsApi(params)
    // 处理响应数据
    const pageResult = response.data || response
    problemsList.value = pageResult.records || []
    total.value = pageResult.total || 0
  } catch (error) {
    ElMessage.error('获取题目列表失败')
    console.error('获取题目列表失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getProblems()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getProblems()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getProblems()
}

const handleAddProblem = () => {
  dialogTitle.value = '新增题目'
  Object.assign(problemForm, {
    id: null,
    title: '',
    content: '',
    difficulty: '',
    mainType: '',
    typeList: [],
    timeLimitMs: 1000,
    memoryLimitMb: 128,
    status: 1,
    templateCode: {
      java: '',
      python: '',
      cpp: ''
    }
  })
  dialogVisible.value = true
}

// 获取所有分类
const getCategories = async () => {
  try {
    const response = await getAllCategoriesApi()
    console.log('分类列表响应:', response)
    // 直接检查response.data
    console.log('response.data:', response.data)
    let categories = []
    
    // 处理不同的响应结构
    if (response.data) {
      if (Array.isArray(response.data)) {
        // 情况1: response.data直接是数组
        categories = response.data
      } else if (response.data.data && Array.isArray(response.data.data)) {
        // 情况2: response.data包含data字段
        categories = response.data.data
      }
    }
    
    categoriesList.value = categories
  } catch (error) {
    ElMessage.error('获取分类列表失败')
    console.error('获取分类列表失败:', error)
  }
}

const handleEditProblem = async (problem) => {
  dialogTitle.value = '编辑题目'
  
  try {
    console.log('编辑题目，传入的problem对象:', problem)
    console.log('编辑题目，传入的problem.id:', problem.id)
    
    // 调用后端接口获取题目详情（包含HTML内容）
    const problemDetail = await getProblemDetailApi(problem.id)
    console.log('获取题目详情响应:', problemDetail)
    
    // 检查返回数据是否有效
    if (!problemDetail || !problemDetail.data) {
      console.error('获取题目详情失败：返回数据为空')
      ElMessage.error('获取题目详情失败：返回数据为空')
      return
    }
    
    // 获取实际的题目数据
    const actualData = problemDetail.data
    console.log('实际的题目数据:', actualData)
    
    // 逐个设置属性，确保正确赋值给reactive对象
    problemForm.id = problem.id
    problemForm.title = actualData.title || ''
    problemForm.content = actualData.content || ''
    problemForm.difficulty = actualData.difficulty || ''
    problemForm.problemType = actualData.problemType || ''
    problemForm.typeList = actualData.typeList || []
    problemForm.timeLimitMs = actualData.timeLimitMs || 1000
    problemForm.memoryLimitMb = actualData.memoryLimitMb || 128
    problemForm.status = actualData.status || 1
    problemForm.templateCode = actualData.templateCode || {
      java: '',
      python: '',
      cpp: ''
    }
    
    console.log('赋值后problemForm:', problemForm)
    console.log('赋值后problemForm.id:', problemForm.id)
    console.log('赋值后problemForm.title:', problemForm.title)
    console.log('赋值后problemForm.content:', problemForm.content)
    
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取题目详情失败')
    console.error('获取题目详情失败:', error)
  }
}

const handleViewProblem = async (problem) => {
  try {
    // 调用后端接口获取题目详情（包含HTML内容）
    const response = await getProblemDetailApi(problem.id)
    
    // 处理响应数据
    const detail = response.data || response
    
    // 赋值给题目详情变量
    Object.assign(problemDetail.value, detail)
    
    // 显示题目详情对话框
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取题目详情失败')
    console.error('获取题目详情失败:', error)
  }
}

const handleDeleteProblem = (id) => {
  ElMessageBox.confirm('确定要删除这个题目吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 调用后端接口删除题目 - DELETE /admin/problem?id={id}
      await request.delete('/admin/problem', { params: { id } })
      ElMessage.success('删除成功')
      getProblems()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  })
}

const handleBatchDelete = () => {
  if (selectedProblems.value.length === 0) {
    ElMessage.warning('请选择要删除的题目')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的${selectedProblems.value.length}个题目吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 收集选中题目的ID
      const ids = selectedProblems.value.map(problem => problem.id)
      console.log('批量删除题目IDs:', ids)
      
      // 调用后端接口批量删除题目 - DELETE /admin/problem/deleteAll
      // 直接传递数组，让axios处理参数转换
      await request.delete('/admin/problem/deleteAll', {
        params: { ids },
        paramsSerializer: (params) => {
          // 自定义参数序列化，确保数组正确传递
          return Object.entries(params)
            .map(([key, value]) => {
              if (Array.isArray(value)) {
                return value.map(v => `${key}=${v}`).join('&');
              }
              return `${key}=${value}`;
            })
            .join('&');
        }
      })
      
      ElMessage.success('批量删除成功')
      // 清空选择
      selectedProblems.value = []
      // 重新获取题目列表
      getProblems()
    } catch (error) {
      ElMessage.error('批量删除失败')
      console.error('批量删除失败:', error)
    }
  })
}

const handleStatusChange = async (problem) => {
  try {
    const newStatus = problem.status === 1 ? 0 : 1
    
    // 调用后端接口修改状态 - POST /admin/problem/status/{status}/{id}
    const response = await request.post(`/admin/problem/status/${newStatus}/${problem.id}`)
    
    ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
    // 重新获取题目列表
    getProblems()
  } catch (error) {
    ElMessage.error('修改状态失败')
    console.error('修改状态失败:', error)
  }
}

const handleTestCases = async (problem) => {
  currentProblem.value = problem
  console.log('测试用例:', problem.id)
  
  try {
    // 调用后端接口获取测试用例列表
    const response = await getTestCasesApi(problem.id)
    console.log('获取测试用例响应:', response)
    
    if (response.data) {
      testCases.value = response.data
    }
  } catch (error) {
    console.error('获取测试用例失败:', error)
    ElMessage.error('获取测试用例失败')
  }
  
  testCasesDialogVisible.value = true
}

const handleAddTestCase = () => {
  editingTestCase.value = null
  Object.assign(testCaseForm, {
    id: null,
    problemId: currentProblem.value.id,
    inputData: '',
    outputData: '',
    isSample: false,
    orderNum: 0,
    timeLimitMs: null,
    memoryLimitMb: null,
    scoreWeight: 1.00,
    status: 1
  })
  editTestCaseDialogVisible.value = true
}

const handleEditTestCase = (testCase) => {
  editingTestCase.value = testCase
  Object.assign(testCaseForm, testCase)
  editTestCaseDialogVisible.value = true
}

const handleDeleteTestCase = (id) => {
  ElMessageBox.confirm('确定要删除这个测试用例吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 调用后端接口删除测试用例
      await deleteTestCaseApi(id)
      ElMessage.success('删除成功')
      // 重新获取测试用例列表
      const response = await getTestCasesApi(currentProblem.value.id)
      if (response.data) {
        testCases.value = response.data
      }
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('删除测试用例失败:', error)
    }
  })
}

const handleSubmitTestCase = async () => {
  if (!testCaseFormRef.value) return
  
  try {
    await testCaseFormRef.value.validate()
    
    if (testCaseForm.id) {
      // 更新测试用例
      await updateTestCaseApi(testCaseForm)
      ElMessage.success('更新成功')
    } else {
      // 创建测试用例
      await createTestCaseApi(testCaseForm)
      ElMessage.success('创建成功')
    }
    
    // 重新获取测试用例列表
    const response = await getTestCasesApi(currentProblem.value.id)
    if (response.data) {
      testCases.value = response.data
    }
    
    editTestCaseDialogVisible.value = false
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('提交测试用例失败:', error)
  }
}

// 处理contentHtml中的反引号问题
const processContentHtml = (content) => {
  if (!content) return ''
  console.log('处理前的content:', content)
  // 使用多种方法处理反引号
  let processedContent = content
  
  // 方法1: split和join
  processedContent = processedContent.split('`').join('')
  
  // 方法2: 正则表达式
  processedContent = processedContent.replace(/[`´`]/g, '')
  
  // 方法3: 逐个字符处理
  let result = ''
  for (let i = 0; i < processedContent.length; i++) {
    const char = processedContent[i]
    if (char !== '`') {
      result += char
    }
  }
  
  console.log('处理后的content:', result)
  return result
}

const handleSubmitForm = async () => {
  if (!problemFormRef.value) return
  
  try {
    await problemFormRef.value.validate()
    
    // 构建提交数据
    const submitData = { ...problemForm }
    console.log('提交的题目数据:', submitData)
    console.log('题目ID:', submitData.id)
    
    // 处理typeList：将ID数组转换为完整的ProblemType对象数组
    if (submitData.typeList && submitData.typeList.length > 0) {
      submitData.typeList = submitData.typeList.map(typeId => {
        // 确保typeId是数字类型
        const id = typeof typeId === 'object' && typeId.id ? typeId.id : typeId
        return { id: parseInt(id) }
      })
    }
    
    if (submitData.id) {
      console.log('调用修改接口')
      // 调用后端接口编辑题目 - PUT /admin/problem
      await updateProblemApi(submitData)
      ElMessage.success('更新成功')
    } else {
      console.log('调用创建接口')
      // 调用后端接口创建题目 - POST /admin/problem
      await createProblemApi(submitData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getProblems()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('提交题目失败:', error)
  }
}

// 监听路由变化，自动打开新增题目对话框
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/problems/create') {
      handleAddProblem()
    }
  },
  { immediate: true }
)

onMounted(() => {
  getProblems()
  getCategories()
  
  // 初始加载时检查路由
  if (route.path === '/problems/create') {
    handleAddProblem()
  }
})
</script>

<style scoped>
.problems-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.problems-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.problems-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.problems-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.problems-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.problem-title {
  font-weight: 500;
  color: #3498db;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

/* 题目详情样式 */
.problem-detail {
  padding: 20px 0;
}

.problem-detail h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 20px;
  color: #333;
}

.problem-detail-info {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.problem-detail-info span {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #f0f9ff;
  color: #339af0;
}

.problem-detail-content {
  margin-bottom: 30px;
  line-height: 1.6;
}

.problem-detail-content h1, .problem-detail-content h2, .problem-detail-content h3 {
  margin-top: 20px;
  margin-bottom: 10px;
}

.problem-detail-content p {
  margin-bottom: 10px;
}

/* 行内代码样式 */
.problem-detail-content code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

/* 代码块样式 */
.problem-detail-content pre {
  background-color: #f5f5f5 !important; /* 浅灰色背景 */
  border: 1px solid #ccc !important;     /* 灰色边框 */
  border-radius: 4px !important;         /* 圆角 */
  padding: 10px !important;              /* 内边距 */
  overflow-x: auto !important;           /* 水平滚动条 */
  font-family: 'Courier New', monospace !important; /* 使用等宽字体 */
  margin: 10px 0 !important;             /* 上下外边距 */
  display: block !important;
  width: 100% !important;
}

/* 如果你想让 code 标签内的文字更清晰，可以单独设置 */
.problem-detail-content pre code {
  color: #333 !important;                /* 深色文字 */
  background-color: transparent !important;
  padding: 0 !important;
  font-family: 'Courier New', monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  display: block !important;
  width: 100% !important;
}

.problem-detail-content ul, .problem-detail-content ol {
  margin-bottom: 16px;
  padding-left: 24px;
}

/* 确保图片能够正确显示 */
.problem-detail-content img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 10px 0;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.problem-detail-subtypes {
  margin-bottom: 30px;
}

.problem-detail-subtypes h4 {
  margin-bottom: 10px;
  font-size: 16px;
  color: #333;
}

.subtype-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.problem-detail-templates {
  margin-bottom: 20px;
}

.problem-detail-templates h4 {
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}

.template-item {
  margin-bottom: 20px;
}

.template-item h5 {
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.template-item pre {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
  border: 1px solid #e1e4e8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
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

/* 测试用例管理样式 */
.test-cases-container {
  padding: 20px 0;
}

.test-case-card {
  margin-bottom: 20px;
}

.test-case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.test-case-index {
  font-weight: 500;
  color: #333;
}

.test-case-type {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.test-case-type.sample {
  background-color: #e6f7ff;
  color: #1890ff;
}

.test-case-content {
  margin-bottom: 15px;
}

.test-case-input, .test-case-output {
  margin-bottom: 10px;
}

.test-case-input label, .test-case-output label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #666;
}

.test-case-input pre, .test-case-output pre {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.test-case-extra {
  margin-top: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.test-case-extra div {
  margin-bottom: 4px;
}

.test-case-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 15px;
}

.no-test-cases {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 16px;
}

/* 测试用例编辑表单样式 */
.el-form-item {
  margin-bottom: 20px;
}

.el-textarea {
  width: 100%;
}

.el-input-number {
  width: 100%;
}
</style>