<template>
  <div class="categories-container">
    <div class="categories-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="handleAddCategory">新增分类</el-button>
    </div>
    
    <div class="categories-filter">
      <el-input
        v-model="searchQuery"
        placeholder="搜索分类名称"
        clearable
        style="width: 300px; margin-right: 10px;"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <span class="el-input__icon el-icon-search"></span>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">
        <span class="el-icon-search"></span> 查询
      </el-button>
    </div>
    
    <el-table :data="categoriesList" style="width: 100%">
      <el-table-column prop="name" label="分类名称" width="150">
        <template #default="scope">
          <span class="category-name">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="分类描述" min-width="200"></el-table-column>
      <el-table-column prop="isActive" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isActive ? 'success' : 'danger'">
            {{ scope.row.isActive ? '激活' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序权重" width="100"></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button size="small" :type="scope.row.isActive ? 'warning' : 'success'" @click="handleToggleStatus(scope.row)">{{ scope.row.isActive ? '禁用' : '激活' }}</el-button>
          <el-button size="small" type="primary" @click="handleEditCategory(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDeleteCategory(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="categories-pagination">
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
    
    <!-- 分类编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="60%"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="100px"
        class="category-form"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-switch v-model="categoryForm.isActive"></el-switch>
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" placeholder="请输入排序权重"></el-input-number>
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
import { getCategoriesApi, getCategoryApi, createCategoryApi, updateCategoryApi, updateCategoryStatusApi, deleteCategoryApi } from '../api'

const categoriesList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const categoryFormRef = ref(null)
const categoryForm = reactive({
  id: null,
  name: '',
  description: '',
  isActive: true,
  sortOrder: 0
})

const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入分类描述', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序权重', trigger: 'blur' }]
}

const getCategories = async () => {
  try {
    // 构建ProblemTypeQueryDTO格式的参数
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      name: searchQuery.value
    }
    console.log('分类分页查询参数:', params)
    const response = await getCategoriesApi(params)
    console.log('分类列表响应:', response)
    // 处理响应数据，适配后端Result<PageResult>结构
    const pageResult = response.data || response
    console.log('提取的pageResult:', pageResult)
    categoriesList.value = pageResult.records || []
    total.value = pageResult.total || 0
    console.log('赋值后的categoriesList:', categoriesList.value)
  } catch (error) {
    ElMessage.error('获取分类列表失败')
    console.error('获取分类列表失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getCategories()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getCategories()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getCategories()
}

const handleAddCategory = () => {
  dialogTitle.value = '新增分类'
  Object.assign(categoryForm, {
    id: null,
    name: '',
    description: '',
    isActive: true,
    sortOrder: 0
  })
  dialogVisible.value = true
}

const handleEditCategory = async (category) => {
  dialogTitle.value = '编辑分类'
  try {
    console.log('编辑分类传递的参数:', category)
    console.log('传递的分类ID:', category.id)
    // 根据ID查询分类详情，确保获取最新数据
    const categoryDetail = await getCategoryApi(category.id)
    console.log('分类详情:', categoryDetail)
    // 处理响应数据，适配后端Result<Category>结构
    const categoryData = categoryDetail.data || categoryDetail
    console.log('提取的分类数据:', categoryData)
    console.log('提取的分类ID:', categoryData.id)
    Object.assign(categoryForm, categoryData)
    console.log('赋值后的categoryForm:', categoryForm)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取分类详情失败')
    console.error('获取分类详情失败:', error)
  }
}

const handleViewCategory = async (category) => {
  dialogTitle.value = '分类详情'
  try {
    console.log('查看分类传递的参数:', category)
    console.log('传递的分类ID:', category.id)
    // 根据ID查询分类详情，确保获取最新数据
    const categoryDetail = await getCategoryApi(category.id)
    console.log('分类详情:', categoryDetail)
    // 处理响应数据，适配后端Result<Category>结构
    const categoryData = categoryDetail.data || categoryDetail
    console.log('提取的分类数据:', categoryData)
    console.log('提取的分类ID:', categoryData.id)
    Object.assign(categoryForm, categoryData)
    console.log('赋值后的categoryForm:', categoryForm)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取分类详情失败')
    console.error('获取分类详情失败:', error)
  }
}

const handleDeleteCategory = (id) => {
  ElMessageBox.confirm('确定要删除这个分类吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      console.log('删除分类传递的ID:', id)
      await deleteCategoryApi(id)
      ElMessage.success('删除成功')
      getCategories()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error('删除分类失败:', error)
    }
  })
}

// 切换分类激活状态
const handleToggleStatus = async (category) => {
  try {
    const newStatus = !category.isActive
    console.log('切换分类状态:', category.id, '从', category.isActive, '到', newStatus)
    // 调用修改分类状态的API
    await updateCategoryStatusApi(category.id, newStatus)
    ElMessage.success(category.isActive ? '禁用成功' : '激活成功')
    getCategories()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('切换状态失败:', error)
  }
}

const handleSubmitForm = async () => {
  if (!categoryFormRef.value) return
  
  try {
    await categoryFormRef.value.validate()
    
    if (categoryForm.id) {
      console.log('更新分类传递的参数:', categoryForm)
      await updateCategoryApi(categoryForm)
      ElMessage.success('更新成功')
    } else {
      console.log('创建分类传递的参数:', categoryForm)
      await createCategoryApi(categoryForm)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getCategories()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('操作失败:', error)
  }
}

onMounted(() => {
  getCategories()
})
</script>

<style scoped>
.categories-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.categories-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.categories-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.categories-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.categories-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.category-name {
  font-weight: 500;
  color: #3498db;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.category-form {
  padding: 20px 0;
}

.category-form .el-form-item {
  margin-bottom: 20px;
}

.category-form .el-input,
.category-form .el-textarea,
.category-form .el-input-number {
  width: 100%;
  max-width: 500px;
}
</style>