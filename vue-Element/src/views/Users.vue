<template>
  <div class="users-container">
    <div class="users-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="handleAddUser">新增用户</el-button>
    </div>
    
    <div class="users-filter">
      <el-input
        v-model="searchQuery"
        placeholder="搜索用户"
        prefix-icon="el-icon-search"
        style="width: 300px; margin-right: 10px;"
      ></el-input>
      <el-select
        v-model="roleFilter"
        placeholder="角色筛选"
        style="width: 120px; margin-right: 10px;"
      >
        <el-option label="全部" value=""></el-option>
        <el-option label="学生" value="student"></el-option>
        <el-option label="教师" value="teacher"></el-option>
        <el-option label="管理员" value="admin"></el-option>
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
    
    <el-table :data="usersList" style="width: 100%">
      <el-table-column prop="username" label="用户名" width="150"></el-table-column>
      <el-table-column prop="nickname" label="昵称" width="150"></el-table-column>
      <el-table-column prop="email" label="邮箱" width="200"></el-table-column>
      <el-table-column prop="avatarUrl" label="头像" width="100">
        <template #default="scope">
          <el-avatar :size="32" :src="scope.row.avatarUrl" fit="cover">
            <img v-if="scope.row.avatarUrl" :src="scope.row.avatarUrl" alt="头像" />
            <span v-else>{{ scope.row.username.charAt(0).toUpperCase() }}</span>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="getRoleType(scope.row.role)">
            {{ getRoleName(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="80"></el-table-column>
      <el-table-column prop="rating" label="评分" width="80"></el-table-column>
      <el-table-column prop="lastLoginTime" label="上次登录" width="180"></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button size="small" :type="scope.row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(scope.row)">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button size="small" type="primary" @click="handleEditUser(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDeleteUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="users-pagination">
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
    
    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="60%"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" type="email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="头像" prop="avatarUrl">
          <div class="avatar-upload">
            <el-avatar :size="80" :src="userForm.avatarUrl" fit="cover" style="margin-bottom: 10px;">
              <img v-if="userForm.avatarUrl" :src="userForm.avatarUrl" alt="头像" />
              <span v-else>{{ userForm.username ? userForm.username.charAt(0).toUpperCase() : '?' }}</span>
            </el-avatar>
            <el-upload
              class="avatar-uploader"
              :action="''"
              :auto-upload="false"
              :on-change="handleAvatarChange"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" type="primary">选择图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="学生" value="student"></el-option>
            <el-option label="教师" value="teacher"></el-option>
            <el-option label="管理员" value="admin"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="userForm.status" active-value="1" inactive-value="0"></el-switch>
        </el-form-item>
        <el-form-item label="积分" prop="points">
          <el-input-number v-model="userForm.points" :min="0" placeholder="请输入积分"></el-input-number>
        </el-form-item>
        <el-form-item label="评分" prop="rating">
          <el-input-number v-model="userForm.rating" :min="0" placeholder="请输入评分"></el-input-number>
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
import { getUsersApi, getUserApi, createUserApi, updateUserApi, deleteUserApi, updateUserStatusApi } from '../api'
import { uploadFileApi } from '../api/common'

const usersList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const roleFilter = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const userFormRef = ref(null)
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  avatarUrl: '',
  role: 'student',
  status: 1,
  points: 0,
  rating: 0
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const getRoleType = (role) => {
  switch (role) {
    case 'admin': return 'danger'
    case 'teacher': return 'warning'
    case 'student': return 'success'
    default: return ''
  }
}

const getRoleName = (role) => {
  switch (role) {
    case 'admin': return '管理员'
    case 'teacher': return '教师'
    case 'student': return '学生'
    default: return role
  }
}

const getUsers = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      username: searchQuery.value,
      role: roleFilter.value
    }
    console.log('分页查询参数:', params)
    const response = await getUsersApi(params)
    console.log('用户列表响应:', response)
    // 处理响应数据，适配后端Result<PageResult>结构
    const pageResult = response.data || response
    console.log('提取的pageResult:', pageResult)
    console.log('提取的records:', pageResult.records)
    if (pageResult.records && pageResult.records.length > 0) {
      console.log('第一个用户对象:', pageResult.records[0])
      console.log('第一个用户的ID:', pageResult.records[0].id)
      console.log('第一个用户的所有属性:', Object.keys(pageResult.records[0]))
    }
    usersList.value = pageResult.records || []
    total.value = pageResult.total || 0
    console.log('赋值后的usersList:', usersList.value)
  } catch (error) {
    ElMessage.error('获取用户列表失败')
    console.error('获取用户列表失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getUsers()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  getUsers()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  getUsers()
}

const handleAddUser = () => {
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    id: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    avatarUrl: '',
    role: 'student',
    status: 1,
    points: 0,
    rating: 0
  })
  dialogVisible.value = true
}

const handleEditUser = async (user) => {
  dialogTitle.value = '编辑用户'
  try {
    console.log('编辑用户传递的参数:', user)
    console.log('传递的用户ID:', user.id)
    // 根据ID查询用户详情，确保获取最新数据
    const userDetail = await getUserApi(user.id)
    console.log('用户详情:', userDetail)
    // 处理响应数据，适配后端Result<User>结构
    const userData = userDetail.data || userDetail
    console.log('提取的用户数据:', userData)
    console.log('提取的用户ID:', userData.id)
    Object.assign(userForm, userData)
    console.log('赋值后的userForm:', userForm)
    // 确保avatarUrl字段存在
    if (!userForm.avatarUrl) {
      userForm.avatarUrl = ''
    }
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取用户详情失败')
    console.error('获取用户详情失败:', error)
  }
}

const handleViewUser = (user) => {
  dialogTitle.value = '用户详情'
  Object.assign(userForm, user)
  dialogVisible.value = true
}

const handleDeleteUser = (id) => {
  ElMessageBox.confirm('确定要删除这个用户吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(id)
      ElMessage.success('删除成功')
      getUsers()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleToggleStatus = async (user) => {
  try {
    // 计算新状态
    const newStatus = user.status === 1 ? 0 : 1
    console.log('切换用户状态:', user.id, '从', user.status, '到', newStatus)
    // 调用后端启用/停用用户账号接口
    await updateUserStatusApi(newStatus, user.id)
    ElMessage.success(user.status === 1 ? '禁用成功' : '启用成功')
    getUsers()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('切换状态失败:', error)
  }
}

const handleAvatarChange = async (file) => {
  try {
    const loading = ElMessage({
      message: '图片上传中...',
      duration: 0,
      showClose: false,
      type: 'info'
    })
    const response = await uploadFileApi(file.raw)
    console.log('图片上传响应:', response)
    // 正确提取URL字符串，处理后端返回的Result对象结构
    const avatarUrl = response.data || response
    userForm.avatarUrl = avatarUrl
    console.log('提取的头像URL:', avatarUrl)
    loading.close()
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
    console.error('图片上传失败:', error)
  }
}

const handleSubmitForm = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    
    if (userForm.id) {
      // 适配后端UserDTO格式
      const userDTO = {
        id: userForm.id,
        username: userForm.username,
        nickName: userForm.nickname,
        email: userForm.email,
        avatarUrl: userForm.avatarUrl,
        role: userForm.role,
        status: userForm.status,
        points: userForm.points,
        rating: userForm.rating
      }
      console.log('更新用户数据:', userDTO)
      await updateUserApi(userDTO)
      ElMessage.success('更新成功')
    } else {
      // 适配后端UserDTO格式
      const userDTO = {
        username: userForm.username,
        nickName: userForm.nickname,
        email: userForm.email,
        avatarUrl: userForm.avatarUrl,
        role: userForm.role,
        status: userForm.status,
        points: userForm.points,
        rating: userForm.rating
      }
      console.log('创建用户数据:', userDTO)
      await createUserApi(userDTO)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getUsers()
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('操作失败:', error)
  }
}

onMounted(() => {
  getUsers()
})
</script>

<style scoped>
.users-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  overflow-x: auto;
}

.users-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.users-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.users-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.users-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.avatar-uploader {
  margin-top: 10px;
}
</style>