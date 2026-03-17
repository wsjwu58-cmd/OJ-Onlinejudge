<template>
  <div class="login-container">
    <div class="form-wrapper">
      <div class="header">
        OJ后台管理系统
      </div>
      <form class="login-form">
        <div class="input-wrapper">
          <div class="border-wrapper">
            <div class="border-item">
              <div class="custom-input">
                <span class="icon">
                  <i class="el-icon-user"></i>
                </span>
                <div class="input-content">
                  <input
                    v-model="loginForm.username"
                    type="text"
                    placeholder="username"
                    autocomplete="off"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="input-wrapper">
          <div class="border-wrapper">
            <div class="border-item">
              <div class="custom-input">
                <span class="icon">
                  <i class="el-icon-lock"></i>
                </span>
                <div class="input-content">
                  <input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="password"
                    autocomplete="off"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="action">
          <button
            type="button"
            class="btn"
            @click="handleLogin"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : 'login' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '../api'

const router = useRouter()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  // 简单的表单验证
  if (!loginForm.username || !loginForm.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  
  if (loginForm.username.length < 3 || loginForm.username.length > 20) {
    ElMessage.error('用户名长度在 3 到 20 之间')
    return
  }
  
  if (loginForm.password.length < 6) {
    ElMessage.error('密码长度至少为 6 位')
    return
  }
  
  try {
    loading.value = true
    
    // 调用真实的登录API
    const response = await loginApi(loginForm)
    
    // 登录成功后处理
    loading.value = false
    console.log('登录响应数据:', response)
    console.log('响应数据类型:', typeof response)
    console.log('响应数据结构:', JSON.stringify(response))
    
    // 提取用户名，尝试多种可能的结构
    let userName = ''
    if (response.username) {
      userName = response.username
    } else if (response.userName) {
      userName = response.userName
    } else if (response.name) {
      userName = response.name
    } else if (response.user && (response.user.username || response.user.userName || response.user.name)) {
      userName = response.user.username || response.user.userName || response.user.name
    } else if (response.data && (response.data.username || response.data.userName || response.data.name)) {
      userName = response.data.username || response.data.userName || response.data.name
    } else if (response.data && response.data.user && (response.data.user.username || response.data.user.userName || response.data.user.name)) {
      userName = response.data.user.username || response.data.user.userName || response.data.user.name
    }
    
    console.log('提取的用户名:', userName)
    
    // 存储登录信息
    localStorage.setItem('token', response.token || (response.data && response.data.token) || (response.user && response.user.token))
    localStorage.setItem('username', userName || '管理员')
    localStorage.setItem('loginUser', JSON.stringify(response))
    
    ElMessage.success('登录成功')
    
    // 跳转到工作台
    router.push({ name: 'Dashboard' })
  } catch (error) {
    loading.value = false
    ElMessage.error('登录失败，请检查用户名和密码')
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
}

html {
  height: 100%;
}

body {
  height: 100%;
  font-family: 'JetBrains Mono Medium', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: url('../assets/2.png') no-repeat;
  background-size: 100% 100%;
}

.form-wrapper {
  width: 400px;
  background-color: rgba(41, 45, 62, .8);
  color: #fff;
  border-radius: 8px;
  padding: 60px;
  transition: all 0.3s ease;
}

.form-wrapper:hover {
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.header {
  text-align: center;
  font-size: 32px;
  text-transform: uppercase;
  line-height: 1.5;
  margin-bottom: 40px;
  color: #fff;
  font-weight: 700;
  letter-spacing: 2px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.input-wrapper {
  margin-bottom: 20px;
}

.border-wrapper {
  background-image: linear-gradient(to right, #e8198b, #0eb4dd);
  width: 100%;
  height: 50px;
  border-radius: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.border-item {
  height: calc(100% - 4px);
  width: calc(100% - 4px);
  border-radius: 30px;
  background-color: rgba(41, 45, 62, 0.9);
  display: flex;
  align-items: center;
  padding: 0 20px;
}

/* 自定义输入框样式 */
.custom-input {
  width: 100%;
  position: relative;
}

.custom-input input {
  width: 100%;
  background-color: transparent !important;
  border: none !important;
  color: #fff !important;
  font-size: 16px;
  padding: 10px 0;
  outline: none;
}

.custom-input input::placeholder {
  color: rgba(255, 255, 255, 0.7) !important;
  text-transform: uppercase;
}

.custom-input .icon {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  color: #fff;
  margin-right: 10px;
}

.custom-input .input-content {
  padding-left: 30px;
  width: 100%;
}

.action {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.btn {
  width: 60%;
  height: 50px;
  text-transform: uppercase;
  text-align: center;
  line-height: 50px;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: transparent;
  border: 2px solid #0eb4dd;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.btn:hover {
  background-image: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
  color: #2c3e50;
  border-color: transparent;
}

.login-form {
  width: 100%;
}

.el-form-item {
  margin-bottom: 0;
}

.el-button--primary {
  background-color: transparent;
  border-color: #0eb4dd;
}

.el-button--primary:hover {
  background-image: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
  border-color: transparent;
  color: #2c3e50;
}

.el-form-item__error {
  color: #ff4d4f !important;
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
}
</style>