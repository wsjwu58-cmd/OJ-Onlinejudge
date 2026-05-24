<template>
  <div class="layout">
    <el-container>
      <el-aside width="180px" class="sidebar">
        <div class="logo">
          <img src="../assets/1.png" alt="logo" class="logo-img" />
          <h2>刷题后台管理</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/">
            <template #title>
              <el-icon><House /></el-icon>
              <span>工作台</span>
            </template>
          </el-menu-item>
          <el-sub-menu index="/problems">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>题目管理</span>
            </template>
            <el-menu-item index="/problems">题目列表</el-menu-item>
            <el-menu-item index="/problems/create">新增题目</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/users">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/contests">
            <template #title>
              <el-icon><Trophy /></el-icon>
              <span>比赛管理</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/categories">
            <template #title>
              <el-icon><Files /></el-icon>
              <span>分类管理</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/submissions">
            <template #title>
              <el-icon><Timer /></el-icon>
              <span>提交记录</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/groups">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>题单管理</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <template #title>
              <el-icon><DataLine /></el-icon>
              <span>数据统计</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>知识库管理</span>
            </template>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header height="60px" class="header">
          <div class="header-title">
            {{ pageTitle }}
          </div>
          <div class="header-user">
            <el-dropdown>
              <span class="user-dropdown">
                <el-avatar size="small" :icon="UserFilled" :bg-color="'#409EFF'" :text-color="'#fff'"></el-avatar>
                <span class="user-name">{{ username }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowDown, UserFilled, Document, User, Trophy, Files, Timer, Collection, House, DataLine, Reading } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'

const router = useRouter()
const route = useRoute()

// ============ WebSocket 接收提交通知 ============
let ws = null

const initWebSocket = () => {
  const sid = localStorage.getItem('username') || 'admin'
  const wsUrl = `ws://localhost:8080/ws/${sid}`
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立, sid:', sid)
  }

  ws.onmessage = (event) => {
    console.log('收到 WebSocket 消息:', event.data)
    try {
      const data = JSON.parse(event.data)
      // 后端推送的格式: { userId, problemId, content }
      ElNotification({
        title: '新的提交通知',
        message: `用户ID: ${data.userId}，题目ID: ${data.problemId}，${data.content}`,
        type: 'info',
        duration: 5000,
        position: 'top-right'
      })
    } catch (e) {
      console.error('WebSocket 消息解析失败:', e)
    }
  }

  ws.onclose = () => {
    console.log('WebSocket 连接已关闭，3秒后重连...')
    setTimeout(() => {
      initWebSocket()
    }, 3000)
  }

  ws.onerror = (error) => {
    console.error('WebSocket 错误:', error)
  }
}

onMounted(() => {
  initWebSocket()
})

onUnmounted(() => {
  if (ws) {
    ws.onclose = null   // 防止触发重连
    ws.close()
    ws = null
  }
})

const activeMenu = ref('/problems')
const pageTitle = ref('题目管理')
const username = ref('管理员')

const menuTitles = {
  '/': '工作台',
  '/problems': '题目管理',
  '/problems/create': '新增题目',
  '/users': '用户管理',
  '/contests': '比赛管理',
  '/categories': '分类管理',
  '/submissions': '提交记录',
  '/groups': '题单管理',
  '/statistics': '数据统计',
  '/knowledge': '知识库管理'
}

const handleMenuSelect = (key) => {
  router.push(key)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  updatePageInfo()
})

// 监听路由变化，实时更新页面信息
watch(
  () => route.path,
  (newPath) => {
    updatePageInfo()
  }
)

// 更新页面信息的方法
const updatePageInfo = () => {
  activeMenu.value = route.path
  pageTitle.value = menuTitles[route.path] || '题目管理'
  const storedUsername = localStorage.getItem('username')
  console.log('从localStorage获取的用户名:', storedUsername)
  username.value = storedUsername || '管理员'
  console.log('设置的用户名:', username.value)
}
</script>

<style scoped>
.layout {
  height: 100vh;
  width: 100%;
}

.sidebar {
  background-color: #2c3e50;
  color: white;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #34495e;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.logo-img {
  width: 24px;
  height: 24px;
  border-radius: 4px;
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.sidebar-menu {
  flex: 1;
  background-color: transparent;
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item) {
  color: white;
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  margin: 5px 0;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  margin-right: 12px;
  font-size: 16px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #34495e;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #3498db;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: white;
  height: 60px;
  line-height: 60px;
  margin: 5px 0;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #34495e;
}

.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  margin-right: 12px;
  font-size: 16px;
}

.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background-color: #2c3e50;
  border-right: none;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  margin: 0;
  border-radius: 0;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: #34495e;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: #3498db;
}

.header {
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.header-user {
  font-size: 14px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f7fa;
}

.user-name {
  margin: 0 8px;
}

.user-dropdown .el-avatar {
  margin-right: 8px;
}

.main-content {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>