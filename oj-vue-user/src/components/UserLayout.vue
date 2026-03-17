<template>
  <div class="user-layout">
    <!-- 顶部导航栏 -->
    <header class="navbar">
      <div class="navbar-inner">
        <div class="navbar-left">
          <router-link to="/" class="logo">
            <span class="logo-icon">OJ</span>
            <span class="logo-text">Online Judge</span>
          </router-link>
          <nav class="nav-links">
            <router-link 
              to="/problems" 
              class="nav-link"
              :class="{ active: isActive('/problems') }"
            >
              <el-icon><Document /></el-icon>
              <span>题库</span>
            </router-link>
            <router-link 
              to="/groups" 
              class="nav-link"
              :class="{ active: isActive('/groups') }"
            >
              <el-icon><Collection /></el-icon>
              <span>题单</span>
            </router-link>
            <router-link 
              to="/contests" 
              class="nav-link"
              :class="{ active: isActive('/contests') }"
            >
              <el-icon><Trophy /></el-icon>
              <span>竞赛</span>
            </router-link>
          </nav>
        </div>
        <div class="navbar-right">
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click">
              <div class="user-avatar">
                <el-avatar :size="32" :style="{ backgroundColor: '#409EFF' }">
                  {{ username.charAt(0).toUpperCase() }}
                </el-avatar>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goProfile">
                    <el-icon><User /></el-icon>个人主页
                  </el-dropdown-item>
                  <el-dropdown-item @click="goSubmissions">
                    <el-icon><Timer /></el-icon>提交记录
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="goKnowledgeManage">
                    <el-icon><Reading /></el-icon>知识库管理
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round size="small" @click="goLogin">登录</el-button>
            <el-button round size="small" @click="goRegister">注册</el-button>
          </template>
        </div>
      </div>
    </header>
    <!-- 主内容区域 -->
    <main class="main-container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Document, Collection, Trophy, User, Timer, SwitchButton, Reading } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const username = ref('')

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

const isActive = (path) => {
  return route.path.startsWith(path)
}

const goLogin = () => router.push('/login')
const goRegister = () => router.push('/register')
const goProfile = () => router.push('/profile')
const goSubmissions = () => router.push('/submissions')
const goKnowledgeManage = () => router.push('/admin/knowledge')

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('loginUser')
  localStorage.removeItem('username')
  username.value = ''
  router.push('/login')
}

const loadUserInfo = () => {
  const storedUsername = localStorage.getItem('username')
  username.value = storedUsername || '用户'
}

onMounted(() => {
  loadUserInfo()
})

watch(() => route.path, () => {
  loadUserInfo()
})
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background-color: #fff;
  border-bottom: 1px solid #e5e5e5;
  z-index: 1000;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.navbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  font-weight: 700;
}

.logo-icon {
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  padding: 4px 8px;
  border-radius: 6px;
  letter-spacing: 1px;
}

.logo-text {
  font-size: 18px;
  color: #262626;
  font-weight: 700;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border-radius: 20px;
  text-decoration: none;
  color: #595959;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-link:hover {
  color: #262626;
  background-color: #f0f0f0;
}

.nav-link.active {
  color: #ff6b00;
  background-color: #fff7e6;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.main-container {
  padding-top: 50px;
  min-height: calc(100vh - 50px);
}
</style>
