<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">加载中...</p>
    </div>

    <div v-else-if="loadError" class="error-container">
      <div class="error-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <p class="error-text">{{ loadError }}</p>
      <button class="retry-btn" @click="retryLoad">重新加载</button>
    </div>

    <div v-else class="profile-container">
      <div class="profile-header">
        <div class="user-info">
          <div class="avatar-section">
            <img
              :src="userInfo.avatarUrl || defaultAvatar"
              alt="avatar"
              class="avatar"
              @error="handleAvatarError"
            />
            <div class="user-details">
              <h1 class="username">{{ displayName }}</h1>
              <div class="user-meta">
                <span class="meta-item">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  {{ getRoleName(userInfo.role) }}
                </span>
                <span class="meta-item" v-if="userInfo.email">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect width="20" height="16" x="2" y="4" rx="2"/>
                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                  </svg>
                  {{ userInfo.email }}
                </span>
              </div>
            </div>
          </div>
          <button class="edit-btn" @click="openEditModal">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
            </svg>
            编辑资料
          </button>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.totalSubmissions || 0 }}</span>
            <span class="stat-label">提交</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.points || 0 }}</span>
            <span class="stat-label">积分</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.dailyQuestionStreak || 0 }}</span>
            <span class="stat-label">连续</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ signCount }}</span>
            <span class="stat-label">本月签到</span>
          </div>
        </div>
      </div>

      <div class="content-sections">
        <section class="section-card">
          <div class="section-header">
            <h2 class="section-title">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              签到日历
            </h2>
            <button 
              class="sign-btn" 
              :class="{ 'signed': signedToday }"
              @click="handleSign" 
              :disabled="signing || signedToday"
            >
              <svg v-if="!signedToday" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              {{ signing ? '签到中...' : (signedToday ? '已签到' : '签到') }}
            </button>
          </div>

          <div class="calendar-wrapper">
            <div class="calendar-nav">
              <button class="nav-btn" @click="prevMonth">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="15 18 9 12 15 6"/>
                </svg>
              </button>
              <span class="calendar-month">{{ currentYear }}年{{ currentMonth + 1 }}月</span>
              <button class="nav-btn" @click="nextMonth">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </button>
            </div>

            <div class="calendar-grid">
              <div class="calendar-weekdays">
                <span v-for="day in weekdays" :key="day" class="weekday">{{ day }}</span>
              </div>
              <div class="calendar-days">
                <div 
                  v-for="(day, index) in calendarDays" 
                  :key="index"
                  :class="[
                    'day-cell',
                    { 
                      'empty': !day.day,
                      'today': day.isToday,
                      'signed': day.signed,
                      'future': day.isFuture
                    }
                  ]"
                  :title="day.day ? `${currentYear}/${currentMonth + 1}/${day.day}${day.signed ? ' - 已签到' : ''}` : ''"
                >
                  <span v-if="day.day" class="day-num">{{ day.day }}</span>
                  <span v-if="day.signed" class="check-mark">
                    <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </span>
                </div>
              </div>
            </div>

            <div class="calendar-legend">
              <span class="legend-item">
                <span class="legend-dot today-dot"></span>
                今天
              </span>
              <span class="legend-item">
                <span class="legend-dot signed-dot"></span>
                已签到
              </span>
            </div>
          </div>
        </section>

        <section class="section-card">
          <div class="section-header">
            <h2 class="section-title">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                <polyline points="15 3 21 3 21 9"/>
                <line x1="10" y1="14" x2="21" y2="3"/>
              </svg>
              快速入口
            </h2>
          </div>
          <div class="quick-links">
            <a class="link-item" @click="$router.push('/problems')">
              <div class="link-icon problems">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
              </div>
              <div class="link-info">
                <span class="link-title">题库</span>
                <span class="link-desc">开始刷题</span>
              </div>
            </a>
            <a class="link-item" @click="$router.push('/submissions')">
              <div class="link-icon submissions">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M3 3v18h18"/>
                  <path d="m19 9-5 5-4-4-3 3"/>
                </svg>
              </div>
              <div class="link-info">
                <span class="link-title">提交记录</span>
                <span class="link-desc">查看历史</span>
              </div>
            </a>
            <a class="link-item" @click="$router.push('/contests')">
              <div class="link-icon contests">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
                  <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
                  <path d="M4 22h16"/>
                  <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
                  <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
                  <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>
                </svg>
              </div>
              <div class="link-info">
                <span class="link-title">竞赛</span>
                <span class="link-desc">参与比赛</span>
              </div>
            </a>
            <a class="link-item" @click="$router.push('/groups')">
              <div class="link-icon groups">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
                </svg>
              </div>
              <div class="link-info">
                <span class="link-title">题单</span>
                <span class="link-desc">系统学习</span>
              </div>
            </a>
          </div>
        </section>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <div class="modal-header">
          <h3>编辑个人资料</h3>
          <button class="close-btn" @click="closeEditModal">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="editForm.username" type="text" disabled>
          </div>
          <div class="form-group">
            <label>昵称 <span class="required">*</span></label>
            <input 
              v-model="editForm.nickName" 
              type="text" 
              placeholder="请输入昵称（2-20个字符）"
              :class="{ 'error': formErrors.nickName }"
              @blur="validateNickName"
            >
            <span v-if="formErrors.nickName" class="error-msg">{{ formErrors.nickName }}</span>
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input 
              v-model="editForm.email" 
              type="email" 
              placeholder="请输入邮箱地址"
              :class="{ 'error': formErrors.email }"
              @blur="validateEmail"
            >
            <span v-if="formErrors.email" class="error-msg">{{ formErrors.email }}</span>
          </div>
          <div class="form-group">
            <label>头像URL</label>
            <input 
              v-model="editForm.avatarUrl" 
              type="text" 
              placeholder="请输入头像图片URL"
              :class="{ 'error': formErrors.avatarUrl }"
              @blur="validateAvatarUrl"
            >
            <span v-if="formErrors.avatarUrl" class="error-msg">{{ formErrors.avatarUrl }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn cancel" @click="closeEditModal">取消</button>
          <button class="btn confirm" @click="handleSaveProfile" :disabled="saving || hasFormErrors">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserProfileApi, updateUserProfileApi, userSignApi, getSignCountApi, getSignDaysApi } from '../api/user'
import { ElMessage } from 'element-plus'

const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=cartoon%20girl%20avatar%20with%20rabbit%20ears%20hat%20happy%20expression%20anime%20style&image_size=square'

const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const signing = ref(false)
const showEditModal = ref(false)
const signCount = ref(0)
const signedDays = ref([])
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth())

const userInfo = ref({
  id: null,
  username: '',
  nickName: '',
  email: '',
  avatarUrl: '',
  role: 'student',
  points: 0,
  rating: 0,
  totalSubmissions: 0,
  dailyQuestionStreak: 0
})

const editForm = reactive({
  username: '',
  nickName: '',
  email: '',
  avatarUrl: ''
})

const formErrors = reactive({
  nickName: '',
  email: '',
  avatarUrl: ''
})

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const displayName = computed(() => {
  return userInfo.value.nickName || userInfo.value.username || '用户'
})

const hasFormErrors = computed(() => {
  return Object.values(formErrors).some(error => error !== '')
})

const signedToday = computed(() => {
  const today = new Date().getDate()
  const currentMonthNow = new Date().getMonth()
  const currentYearNow = new Date().getFullYear()
  return currentYear.value === currentYearNow && 
         currentMonth.value === currentMonthNow && 
         signedDays.value.includes(today)
})

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()
  
  const days = []
  const today = new Date()
  
  for (let i = 0; i < startWeekday; i++) {
    days.push({ day: null })
  }
  
  for (let i = 1; i <= daysInMonth; i++) {
    const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === i
    const isFuture = today.getFullYear() === year && today.getMonth() === month && i > today.getDate()
    const signed = signedDays.value.includes(i)
    days.push({ 
      day: i, 
      isToday, 
      signed,
      isFuture
    })
  }
  
  return days
})

const getRoleName = (role) => {
  const roleMap = {
    'student': '学生',
    'teacher': '教师',
    'admin': '管理员'
  }
  return roleMap[role] || '用户'
}

const sanitizeInput = (input) => {
  if (!input) return ''
  return input.trim().replace(/[<>]/g, '')
}

const validateNickName = () => {
  const nickName = sanitizeInput(editForm.nickName)
  if (!nickName) {
    formErrors.nickName = '昵称不能为空'
  } else if (nickName.length < 2 || nickName.length > 20) {
    formErrors.nickName = '昵称长度应为2-20个字符'
  } else {
    formErrors.nickName = ''
  }
}

const validateEmail = () => {
  const email = sanitizeInput(editForm.email)
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    formErrors.email = '请输入有效的邮箱地址'
  } else {
    formErrors.email = ''
  }
}

const validateAvatarUrl = () => {
  const url = sanitizeInput(editForm.avatarUrl)
  if (url && !/^https?:\/\/.+/.test(url)) {
    formErrors.avatarUrl = '请输入有效的URL地址'
  } else {
    formErrors.avatarUrl = ''
  }
}

const handleAvatarError = (e) => {
  e.target.src = defaultAvatar
}

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
  fetchSignDays()
}

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
  fetchSignDays()
}

const fetchUserProfile = async () => {
  try {
    loading.value = true
    loadError.value = ''
    const res = await getUserProfileApi()
    if (res.code === 1 && res.data) {
      userInfo.value = res.data
      editForm.username = res.data.username || ''
      editForm.nickName = res.data.nickName || ''
      editForm.email = res.data.email || ''
      editForm.avatarUrl = res.data.avatarUrl || ''
    } else {
      loadError.value = res.msg || '获取用户信息失败'
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    loadError.value = error.response?.data?.msg || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

const fetchSignData = async () => {
  try {
    const res = await getSignCountApi()
    if (res.code === 1) {
      signCount.value = res.data || 0
    }
  } catch (error) {
    console.error('获取签到统计失败:', error)
  }
}

const fetchSignDays = async () => {
  try {
    const res = await getSignDaysApi(currentYear.value, currentMonth.value + 1)
    if (res.code === 1 && Array.isArray(res.data)) {
      signedDays.value = res.data
    } else {
      signedDays.value = []
    }
  } catch (error) {
    console.error('获取签到日期失败:', error)
    signedDays.value = []
  }
}

const handleSign = async () => {
  if (signing.value || signedToday.value) return
  try {
    signing.value = true
    const res = await userSignApi()
    if (res.code === 1) {
      ElMessage.success('签到成功！积分+10')
      signCount.value++
      const today = new Date().getDate()
      if (!signedDays.value.includes(today)) {
        signedDays.value.push(today)
      }
      userInfo.value.points = (userInfo.value.points || 0) + 10
    } else {
      ElMessage.error(res.msg || '签到失败')
    }
  } catch (error) {
    console.error('签到失败:', error)
    ElMessage.error(error.response?.data?.msg || '签到失败，请稍后重试')
  } finally {
    signing.value = false
  }
}

const openEditModal = () => {
  editForm.username = userInfo.value.username || ''
  editForm.nickName = userInfo.value.nickName || ''
  editForm.email = userInfo.value.email || ''
  editForm.avatarUrl = userInfo.value.avatarUrl || ''
  formErrors.nickName = ''
  formErrors.email = ''
  formErrors.avatarUrl = ''
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
}

const handleSaveProfile = async () => {
  validateNickName()
  validateEmail()
  validateAvatarUrl()
  
  if (hasFormErrors.value) return
  
  try {
    saving.value = true
    const res = await updateUserProfileApi({
      nickName: sanitizeInput(editForm.nickName),
      email: sanitizeInput(editForm.email),
      avatarUrl: sanitizeInput(editForm.avatarUrl)
    })
    if (res.code === 1) {
      ElMessage.success('保存成功')
      showEditModal.value = false
      await fetchUserProfile()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.msg || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const retryLoad = () => {
  fetchUserProfile()
  fetchSignData()
  fetchSignDays()
}

onMounted(() => {
  fetchUserProfile()
  fetchSignData()
  fetchSignDays()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.profile-page {
  min-height: 100vh;
  background: #1a1a1a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: #fff;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #333;
  border-top-color: #ffa116;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 16px;
  color: #8a8a8a;
  font-size: 14px;
}

.error-icon { color: #666; margin-bottom: 16px; }
.error-text { color: #999; font-size: 15px; margin-bottom: 20px; }

.retry-btn {
  padding: 10px 24px;
  background: #ffa116;
  border: none;
  border-radius: 6px;
  color: #000;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover { background: #ffb84d; }

.profile-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px;
}

.profile-header {
  background: #282828;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid #ffa116;
  object-fit: cover;
}

.user-details { display: flex; flex-direction: column; gap: 6px; }

.username {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  color: #fff;
}

.user-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8a8a8a;
  font-size: 13px;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  border: 1px solid #444;
  border-radius: 6px;
  color: #ccc;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn:hover {
  border-color: #ffa116;
  color: #ffa116;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  background: #1f1f1f;
  border-radius: 8px;
  padding: 16px 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 32px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  color: #8a8a8a;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #333;
}

.content-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: #282828;
  border-radius: 12px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #fff;
}

.section-title svg { color: #ffa116; }

.sign-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #ffa116;
  border: none;
  border-radius: 6px;
  color: #000;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.sign-btn:hover:not(:disabled) { background: #ffb84d; }

.sign-btn:disabled { cursor: not-allowed; }

.sign-btn.signed {
  background: #2dba4e;
  color: #fff;
}

.calendar-wrapper {
  background: #1f1f1f;
  border-radius: 8px;
  padding: 16px;
}

.calendar-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.nav-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #333;
  border: none;
  border-radius: 6px;
  color: #ccc;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: #444;
  color: #fff;
}

.calendar-month {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  min-width: 100px;
  text-align: center;
}

.calendar-grid {}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  padding: 8px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  position: relative;
  transition: all 0.2s;
  cursor: default;
}

.day-cell.empty { background: transparent; }

.day-cell:not(.empty) { background: #2a2a2a; }

.day-cell:not(.empty):hover { background: #333; }

.day-cell.today {
  background: rgba(255, 161, 22, 0.2);
  border: 1px solid rgba(255, 161, 22, 0.5);
}

.day-cell.signed {
  background: rgba(45, 186, 78, 0.2);
}

.day-cell.signed .day-num { color: #2dba4e; }

.day-cell.future { opacity: 0.5; }

.day-num {
  font-size: 13px;
  font-weight: 500;
  color: #ccc;
}

.check-mark {
  position: absolute;
  bottom: 4px;
  color: #2dba4e;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #333;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8a8a8a;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.today-dot {
  background: rgba(255, 161, 22, 0.3);
  border: 1px solid rgba(255, 161, 22, 0.5);
}

.signed-dot {
  background: rgba(45, 186, 78, 0.3);
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  background: #1f1f1f;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}

.link-item:hover {
  background: #2a2a2a;
  transform: translateY(-2px);
}

.link-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.link-icon.problems { background: rgba(255, 161, 22, 0.15); color: #ffa116; }
.link-icon.submissions { background: rgba(45, 186, 78, 0.15); color: #2dba4e; }
.link-icon.contests { background: rgba(66, 133, 244, 0.15); color: #4285f4; }
.link-icon.groups { background: rgba(187, 134, 252, 0.15); color: #bb86fc; }

.link-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.link-title {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.link-desc {
  font-size: 11px;
  color: #666;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal {
  background: #282828;
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #333;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.close-btn {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: color 0.2s;
}

.close-btn:hover { color: #fff; }

.modal-body { padding: 20px; }

.form-group { margin-bottom: 16px; }

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #ccc;
}

.form-group .required { color: #ef4444; }

.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: #1f1f1f;
  border: 1px solid #333;
  border-radius: 6px;
  font-size: 14px;
  color: #fff;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #ffa116;
}

.form-group input:disabled {
  background: #2a2a2a;
  color: #666;
  cursor: not-allowed;
}

.form-group input.error { border-color: #ef4444; }

.error-msg {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #ef4444;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #333;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.cancel {
  background: #333;
  color: #ccc;
}

.btn.cancel:hover { background: #444; }

.btn.confirm {
  background: #ffa116;
  color: #000;
}

.btn.confirm:hover:not(:disabled) { background: #ffb84d; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .quick-links {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-row {
    flex-wrap: wrap;
  }
  
  .stat-divider {
    display: none;
  }
  
  .stat-item {
    flex: 1;
    min-width: 80px;
    padding: 12px;
  }
  
  .user-info {
    flex-direction: column;
    gap: 16px;
  }
  
  .edit-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .profile-header {
    padding: 16px;
  }
  
  .avatar {
    width: 56px;
    height: 56px;
  }
  
  .username {
    font-size: 18px;
  }
  
  .stat-value {
    font-size: 20px;
  }
}
</style>
