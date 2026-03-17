<template>
  <div class="profile-container">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <template v-else>
      <div class="profile-header">
        <div class="avatar-section">
          <img :src="userInfo.avatarUrl || defaultAvatar" alt="Avatar" class="avatar">
          <button class="avatar-upload-btn" @click="showEditModal = true">更换头像</button>
        </div>
        <div class="info-section">
          <h1 class="name">{{ userInfo.nickName || userInfo.username || '用户' }}</h1>
          <div class="title">
            <span class="emoji">�</span>
            {{ getRoleName(userInfo.role) }}
          </div>
          <div class="motto">
            <span class="emoji">�</span>
            {{ userInfo.email || '未设置邮箱' }}
          </div>
          <div class="stats">
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.points || 0 }}</span>
              <span class="stat-label">积分</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.rating || 0 }}</span>
              <span class="stat-label">评分</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.totalSubmissions || 0 }}</span>
              <span class="stat-label">提交</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.dailyQuestionStreak || 0 }}</span>
              <span class="stat-label">连续天数</span>
            </div>
          </div>
        </div>
        <div class="action-section">
          <button class="edit-btn" @click="showEditModal = true">
            <span>✏️</span> 编辑资料
          </button>
          <button class="sign-btn" @click="handleSign" :disabled="signing">
            <span>{{ signing ? '签到中...' : '📅 签到' }}</span>
          </button>
          <div v-if="signCount > 0" class="sign-count">
            已连续签到 <span class="count">{{ signCount }}</span> 天
          </div>
        </div>
      </div>

      <div class="profile-content">
        <div class="tags-section">
          <h3>个人标签</h3>
          <div class="tags-list">
            <div v-for="tag in userTags" :key="tag" class="tag">{{ tag }}</div>
            <button class="add-tag-btn" @click="showTagInput = true">+ 添加标签</button>
          </div>
        </div>

        <div class="activity-chart">
          <h3>刷题活动</h3>
          <div class="chart-container">
            <div class="pixel-chart">
              <div v-for="(row, rowIndex) in activityData" :key="rowIndex" class="pixel-row">
                <div 
                  v-for="(cell, colIndex) in row" 
                  :key="colIndex" 
                  class="pixel" 
                  :class="{ 'active': cell > 0 }"
                  :style="{ backgroundColor: getPixelColor(cell) }"
                  :title="`提交: ${cell}次`"
                ></div>
              </div>
            </div>
            <div class="chart-legend">
              <span>少</span>
              <div class="legend-colors">
                <div class="legend-item" style="background: #ebedf0"></div>
                <div class="legend-item" style="background: #9be9a8"></div>
                <div class="legend-item" style="background: #40c463"></div>
                <div class="legend-item" style="background: #30a14e"></div>
                <div class="legend-item" style="background: #216e39"></div>
              </div>
              <span>多</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-footer">
        <div class="timeline-section">
          <h3>最近活动</h3>
          <div class="timeline">
            <div v-if="recentActivities.length === 0" class="empty-activity">
              暂无活动记录
            </div>
            <div v-for="(activity, index) in recentActivities" :key="index" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="timeline-date">{{ formatDate(activity.time) }}</div>
                <div class="timeline-text">{{ activity.content }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="quick-links">
          <h3>快速入口</h3>
          <div class="links-grid">
            <div class="link-card" @click="$router.push('/problems')">
              <div class="link-icon">📝</div>
              <div class="link-name">题库</div>
              <div class="link-desc">开始刷题</div>
            </div>
            <div class="link-card" @click="$router.push('/submissions')">
              <div class="link-icon">📊</div>
              <div class="link-name">提交记录</div>
              <div class="link-desc">查看历史</div>
            </div>
            <div class="link-card" @click="$router.push('/contests')">
              <div class="link-icon">🏆</div>
              <div class="link-name">竞赛</div>
              <div class="link-desc">参与比赛</div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>编辑个人资料</h3>
          <button class="close-btn" @click="showEditModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="editForm.username" type="text" disabled placeholder="用户名不可修改">
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input v-model="editForm.nickName" type="text" placeholder="请输入昵称">
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="editForm.email" type="email" placeholder="请输入邮箱">
          </div>
          <div class="form-group">
            <label>头像URL</label>
            <input v-model="editForm.avatarUrl" type="text" placeholder="请输入头像图片URL">
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showEditModal = false">取消</button>
          <button class="save-btn" @click="handleSaveProfile" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserProfileApi, updateUserProfileApi, userSignApi, getSignCountApi } from '../api/user'
import { ElMessage } from 'element-plus'

const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=cartoon%20girl%20avatar%20with%20rabbit%20ears%20hat%20happy%20expression%20anime%20style&image_size=square'

const loading = ref(true)
const saving = ref(false)
const signing = ref(false)
const showEditModal = ref(false)
const showTagInput = ref(false)
const signCount = ref(0)

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

const userTags = ref(['前端', 'Vue', '算法学习'])

const recentActivities = ref([])

const activityData = ref([])

const getRoleName = (role) => {
  const roleMap = {
    'student': '学生',
    'teacher': '教师',
    'admin': '管理员'
  }
  return roleMap[role] || '用户'
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}

const getPixelColor = (count) => {
  if (count === 0) return '#ebedf0'
  if (count < 3) return '#9be9a8'
  if (count < 6) return '#40c463'
  if (count < 9) return '#30a14e'
  return '#216e39'
}

const generateMockActivityData = () => {
  const data = []
  for (let i = 0; i < 7; i++) {
    const row = []
    for (let j = 0; j < 52; j++) {
      row.push(Math.floor(Math.random() * 10))
    }
    data.push(row)
  }
  return data
}

const fetchUserProfile = async () => {
  try {
    loading.value = true
    const res = await getUserProfileApi()
    if (res.data) {
      userInfo.value = res.data
      editForm.username = res.data.username || ''
      editForm.nickName = res.data.nickName || ''
      editForm.email = res.data.email || ''
      editForm.avatarUrl = res.data.avatarUrl || ''
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const fetchSignCount = async () => {
  try {
    const res = await getSignCountApi()
    if (res.data) {
      signCount.value = res.data
    }
  } catch (error) {
    console.error('获取签到信息失败:', error)
  }
}

const handleSign = async () => {
  try {
    signing.value = true
    const res = await userSignApi()
    ElMessage.success('签到成功！')
    signCount.value++
    userInfo.value.points = (userInfo.value.points || 0) + 10
  } catch (error) {
    console.error('签到失败:', error)
    ElMessage.error(error.response?.data?.msg || '签到失败')
  } finally {
    signing.value = false
  }
}

const handleSaveProfile = async () => {
  try {
    saving.value = true
    await updateUserProfileApi({
      nickName: editForm.nickName,
      email: editForm.email,
      avatarUrl: editForm.avatarUrl
    })
    ElMessage.success('保存成功')
    showEditModal.value = false
    await fetchUserProfile()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchUserProfile()
  fetchSignCount()
  activityData.value = generateMockActivityData()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
  color: #333;
  font-family: 'Arial', sans-serif;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 40px;
  gap: 40px;
  flex-wrap: wrap;
  background: white;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.avatar-section {
  flex-shrink: 0;
  text-align: center;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 4px solid #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  object-fit: cover;
}

.avatar-upload-btn {
  margin-top: 10px;
  padding: 8px 16px;
  background: #f0f2f5;
  border: none;
  border-radius: 20px;
  color: #667eea;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.avatar-upload-btn:hover {
  background: #e8ebf0;
}

.info-section {
  flex: 1;
  min-width: 300px;
}

.name {
  font-size: 2.2rem;
  margin: 0 0 15px 0;
  font-weight: bold;
  color: #1a1a1a;
}

.title {
  font-size: 1.2rem;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
}

.motto {
  font-size: 1rem;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #888;
}

.stats {
  display: flex;
  gap: 30px;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px 20px;
  background: #f8f9fc;
  border-radius: 12px;
}

.stat-value {
  display: block;
  font-size: 1.8rem;
  font-weight: bold;
  color: #667eea;
}

.stat-label {
  font-size: 0.9rem;
  color: #888;
}

.action-section {
  flex-shrink: 0;
  text-align: center;
}

.edit-btn, .sign-btn {
  display: block;
  width: 100%;
  padding: 12px 30px;
  margin-bottom: 15px;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-btn {
  background: #f0f2f5;
  color: #667eea;
  border: 2px solid #667eea;
}

.edit-btn:hover {
  background: #667eea;
  color: white;
  transform: translateY(-2px);
}

.sign-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: bold;
}

.sign-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.sign-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.sign-count {
  font-size: 0.9rem;
  color: #888;
}

.sign-count .count {
  color: #667eea;
  font-weight: bold;
  font-size: 1.2rem;
}

.profile-content {
  margin-bottom: 40px;
}

.tags-section, .activity-chart {
  background: white;
  padding: 25px;
  border-radius: 16px;
  margin-bottom: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tags-section h3, .activity-chart h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.2rem;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag {
  padding: 8px 16px;
  background: #f0f2f5;
  border-radius: 20px;
  font-size: 0.9rem;
  color: #667eea;
  transition: all 0.3s ease;
  cursor: pointer;
}

.tag:hover {
  background: #667eea;
  color: white;
  transform: translateY(-2px);
}

.add-tag-btn {
  padding: 8px 16px;
  background: transparent;
  border: 2px dashed #ddd;
  border-radius: 20px;
  color: #999;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.add-tag-btn:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f8f9fc;
}

.chart-container {
  overflow-x: auto;
}

.pixel-chart {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 0;
}

.pixel-row {
  display: flex;
  gap: 3px;
}

.pixel {
  width: 12px;
  height: 12px;
  background: #ebedf0;
  border-radius: 2px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.pixel:hover {
  transform: scale(1.3);
}

.chart-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 15px;
  font-size: 0.8rem;
  color: #888;
}

.legend-colors {
  display: flex;
  gap: 3px;
}

.legend-item {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.profile-footer {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.timeline-section, .quick-links {
  flex: 1;
  min-width: 300px;
  background: white;
  padding: 25px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.timeline-section h3, .quick-links h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.2rem;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.empty-activity {
  text-align: center;
  padding: 30px;
  color: #999;
}

.timeline {
  position: relative;
}

.timeline-item {
  display: flex;
  margin-bottom: 20px;
  position: relative;
  padding-left: 30px;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 20px;
  bottom: -20px;
  width: 2px;
  background: #e0e0e0;
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 5px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #667eea;
  z-index: 1;
}

.timeline-content {
  flex: 1;
}

.timeline-date {
  font-size: 0.8rem;
  color: #999;
  margin-bottom: 5px;
}

.timeline-text {
  font-size: 1rem;
  color: #333;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 15px;
}

.link-card {
  background: #f8f9fc;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.link-card:hover {
  background: #667eea;
  color: white;
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.3);
}

.link-card:hover .link-name,
.link-card:hover .link-desc {
  color: white;
}

.link-icon {
  font-size: 2rem;
  margin-bottom: 10px;
}

.link-name {
  font-size: 1.1rem;
  font-weight: bold;
  margin-bottom: 5px;
  color: #333;
}

.link-desc {
  font-size: 0.8rem;
  color: #888;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  color: #333;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.3rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}

.cancel-btn, .save-btn {
  padding: 10px 25px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.save-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .stats {
    justify-content: center;
  }
  
  .profile-footer {
    flex-direction: column;
  }
}
</style>