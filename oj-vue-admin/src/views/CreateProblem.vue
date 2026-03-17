<template>
  <div class="create-problem-container">
    <div class="create-problem-header">
      <h2>新增题目</h2>
      <el-button @click="goBack">返回题目列表</el-button>
    </div>
    
    <el-card class="create-problem-card">
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
        <el-form-item label="时间限制(ms)" prop="timeLimitMs">
          <el-input-number v-model="problemForm.timeLimitMs" :min="1" placeholder="请输入时间限制"></el-input-number>
        </el-form-item>
        <el-form-item label="内存限制(MB)" prop="memoryLimitMb">
          <el-input-number v-model="problemForm.memoryLimitMb" :min="1" placeholder="请输入内存限制"></el-input-number>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="problemForm.status" active-text="上架" inactive-text="下架"></el-switch>
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
        <el-form-item>
          <el-button type="primary" @click="handleSubmitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createProblemApi } from '../api'
import { getAllCategoriesApi } from '../api/categories'

const router = useRouter()
const categoriesList = ref([])
const problemFormRef = ref(null)

const problemForm = reactive({
  id: null,
  title: '',
  content: '',
  difficulty: '',
  problemType: '',
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

const getCategories = async () => {
  try {
    const response = await getAllCategoriesApi()
    console.log('分类列表响应:', response)
    let categories = []
    
    // 处理不同的响应结构
    if (response.data) {
      if (Array.isArray(response.data)) {
        categories = response.data
      } else if (response.data.data && Array.isArray(response.data.data)) {
        categories = response.data.data
      }
    }
    
    categoriesList.value = categories
  } catch (error) {
    ElMessage.error('获取分类列表失败')
    console.error('获取分类列表失败:', error)
  }
}

const handleSubmitForm = async () => {
  if (!problemFormRef.value) return
  
  try {
    await problemFormRef.value.validate()
    
    // 构建提交数据
    const submitData = { ...problemForm }
    console.log('提交的题目数据:', submitData)
    
    // 处理status：将布尔值转换为整数
    submitData.status = submitData.status ? 1 : 0
    
    // 处理typeList：将ID数组转换为完整的ProblemType对象数组
    if (submitData.typeList && submitData.typeList.length > 0) {
      submitData.typeList = submitData.typeList.map(typeId => {
        // 确保typeId是数字类型
        const id = typeof typeId === 'object' && typeId.id ? typeId.id : typeId
        return { id: parseInt(id) }
      })
    }
    
    // 调用后端接口创建题目
    await createProblemApi(submitData)
    ElMessage.success('创建成功')
    
    // 重置表单
    resetForm()
    
    // 可以选择跳转到题目列表
    // router.push('/problems')
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('提交题目失败:', error)
  }
}

const resetForm = () => {
  if (problemFormRef.value) {
    problemFormRef.value.resetFields()
  }
  
  // 手动重置嵌套的templateCode
  problemForm.templateCode = {
    java: '',
    python: '',
    cpp: ''
  }
}

const goBack = () => {
  router.push('/problems')
}

onMounted(() => {
  getCategories()
})
</script>

<style scoped>
.create-problem-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  min-height: calc(100vh - 60px);
}

.create-problem-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.create-problem-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.create-problem-card {
  margin-top: 20px;
}

.el-form {
  max-width: 100%;
}

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