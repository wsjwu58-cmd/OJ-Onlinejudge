<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <span class="logo-icon">OJ</span>
          </div>
          <h1>{{ isRegister ? '注册账号' : '欢迎回来' }}</h1>
          <p>{{ isRegister ? '创建你的 Online Judge 账号' : '登录你的 Online Judge 账号' }}</p>
        </div>

        <el-form 
          ref="formRef" 
          :model="form" 
          :rules="rules" 
          label-width="0"
          size="large"
        >
          <el-form-item prop="username">
            <el-input 
              v-model="form.username" 
              placeholder="用户名"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item v-if="isRegister" prop="confirmPassword">
            <el-input 
              v-model="form.confirmPassword" 
              type="password" 
              placeholder="确认密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button 
              type="warning" 
              class="login-btn" 
              @click="handleSubmit" 
              :loading="loading"
            >
              {{ isRegister ? '注册' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span v-if="!isRegister">
            没有账号？
            <el-button type="primary" link @click="toggleMode">立即注册</el-button>
          </span>
          <span v-else>
            已有账号？
            <el-button type="primary" link @click="toggleMode">立即登录</el-button>
          </span>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showSliderVerify"
      title="请拖动滑块完成拼图"
      width="360px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <SliderVerify
        ref="sliderVerifyRef"
        @success="onSliderSuccess"
        @fail="onSliderFail"
        @again="onSliderAgain"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register } from '../services/api'
import { ElMessage } from 'element-plus'
import SliderVerify from '../components/SliderVerify.vue'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const sliderVerifyRef = ref(null)
const loading = ref(false)
const isRegister = ref(route.path === '/register')
const showSliderVerify = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nonceStr: '',
  value: ''
})

const validateConfirm = (rule, value, callback) => {
  if (isRegister.value && value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不少于6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
}

const toggleMode = () => {
  isRegister.value = !isRegister.value
  router.replace(isRegister.value ? '/register' : '/login')
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (isRegister.value) {
      loading.value = true
      const res = await register(form.username, form.username + '@example.com', form.password)
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      router.replace('/login')
      loading.value = false
    } else {
      showSliderVerify.value = true
    }
  } catch (e) {
    console.log('表单验证失败')
  }
}

const onSliderSuccess = async (captcha) => {
  form.nonceStr = captcha.nonceStr
  form.value = String(captcha.value)
  await doLogin()
}

const onSliderFail = (msg) => {
  ElMessage.error(msg || '验证失败，请控制拼图对齐缺口')
}

const onSliderAgain = () => {
  ElMessage.error('滑动操作异常，请重试')
}

const handleDialogClosed = () => {
  if (sliderVerifyRef.value) {
    sliderVerifyRef.value.refresh()
  }
}

const doLogin = async () => {
  try {
    loading.value = true
    const res = await login(form.username, form.password, form.nonceStr, form.value)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', form.username)
    localStorage.setItem('loginUser', JSON.stringify(res.user))
    
    if (sliderVerifyRef.value) {
      sliderVerifyRef.value.verifySuccessEvent()
    }
    
    setTimeout(() => {
      showSliderVerify.value = false
      ElMessage.success('登录成功')
      router.push('/')
    }, 500)
  } catch (e) {
    if (sliderVerifyRef.value) {
      sliderVerifyRef.value.verifyFailEvent()
    }
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container {
  width: 100%;
  max-width: 420px;
  padding: 24px;
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  margin-bottom: 16px;
}

.logo-icon {
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  padding: 8px 16px;
  border-radius: 10px;
  letter-spacing: 2px;
}

.login-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
  margin-bottom: 4px;
}

.login-header p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
}
</style>
