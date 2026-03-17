<template>
  <div class="home-page">
    <div class="home-container">
      <!-- Hero区域 -->
      <section class="hero-section">
        <div class="hero-bg" aria-hidden="true">
          <span class="blob blob-1"></span>
          <span class="blob blob-2"></span>
          <span class="grid"></span>
        </div>

        <div class="hero-content">
          <div class="hero-badge">
            <el-icon :size="16"><Lightning /></el-icon>
            <span>更快的刷题体验</span>
          </div>

          <h1 class="hero-title">在线评测系统</h1>
          <p class="hero-desc">刷题、练习、参加竞赛，从今天开始系统提升你的编程能力</p>

          <div class="hero-points">
            <div class="point">
              <el-icon class="point-icon"><CircleCheck /></el-icon>
              <span>代码在线运行与提交评测</span>
            </div>
            <div class="point">
              <el-icon class="point-icon"><CircleCheck /></el-icon>
              <span>题解与讨论沉淀，快速复盘</span>
            </div>
            <div class="point">
              <el-icon class="point-icon"><CircleCheck /></el-icon>
              <span>竞赛训练，检验真实水平</span>
            </div>
          </div>

          <div class="hero-actions">
            <el-button type="warning" size="large" round @click="$router.push('/problems')">
              <el-icon><ArrowRight /></el-icon>
              开始刷题
            </el-button>
            <el-button size="large" round @click="$router.push('/contests')">
              <el-icon><Trophy /></el-icon>
              参加竞赛
            </el-button>
            <el-button text size="large" class="ghost-btn" @click="$router.push('/groups')">
              浏览题单
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </section>

      <!-- 快捷入口 -->
      <section class="quick-entry">
        <div class="section-title">
          <h2>快速开始</h2>
          <p>从题库到题单，再到竞赛训练，按你的节奏进步</p>
        </div>

        <div class="entry-cards">
          <div class="entry-card" @click="$router.push('/problems')">
            <div class="entry-icon problems-icon">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="entry-main">
              <h3>题库</h3>
              <p>海量算法题目，按分类、难度筛选</p>
            </div>
            <el-icon class="entry-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="entry-card" @click="$router.push('/groups')">
            <div class="entry-icon groups-icon">
              <el-icon :size="32"><Collection /></el-icon>
            </div>
            <div class="entry-main">
              <h3>题单</h3>
              <p>精选题目组合，系统性学习提升</p>
            </div>
            <el-icon class="entry-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="entry-card" @click="$router.push('/contests')">
            <div class="entry-icon contests-icon">
              <el-icon :size="32"><Trophy /></el-icon>
            </div>
            <div class="entry-main">
              <h3>竞赛</h3>
              <p>周赛、双周赛，检验你的实力</p>
            </div>
            <el-icon class="entry-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <!-- 统计信息 -->
      <section class="stats-section">
        <div class="section-title">
          <h2>平台数据</h2>
          <p>持续更新的题目与训练场景，陪你把每一次提交变成进步</p>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon stat-orange">
              <el-icon :size="18"><Document /></el-icon>
            </div>
            <div class="stat-number">{{ stats.totalProblems }}</div>
            <div class="stat-label">题目总数</div>
            <div class="stat-sub">覆盖常见算法与数据结构</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon stat-gold">
              <el-icon :size="18"><Trophy /></el-icon>
            </div>
            <div class="stat-number">{{ stats.totalContests }}</div>
            <div class="stat-label">竞赛场次</div>
            <div class="stat-sub">定期训练，模拟真实场景</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon stat-green">
              <el-icon :size="18"><Collection /></el-icon>
            </div>
            <div class="stat-number">{{ stats.totalGroups }}</div>
            <div class="stat-label">精选题单</div>
            <div class="stat-sub">按主题打卡，体系化提升</div>
          </div>
          <div class="stat-item">
            <div class="stat-icon stat-blue">
              <el-icon :size="18"><User /></el-icon>
            </div>
            <div class="stat-number">{{ stats.totalUsers }}</div>
            <div class="stat-label">注册用户</div>
            <div class="stat-sub">一起成长，交流更高效</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, Collection, Trophy, ArrowRight, Lightning, CircleCheck, User } from '@element-plus/icons-vue'

const stats = ref({
  totalProblems: 200,
  totalContests: 15,
  totalGroups: 30,
  totalUsers: 1000
})
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 50px);
  background: radial-gradient(1200px 400px at 50% 0%, rgba(255, 149, 0, 0.10), transparent 60%),
    #f7f8fa;
}

.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.hero-section {
  position: relative;
  text-align: center;
  padding: 72px 0 56px;
  margin-top: 12px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.14), rgba(24, 144, 255, 0.10));
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.06);
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.blob {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.7;
}

.blob-1 {
  top: -180px;
  left: -120px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 107, 0, 0.55), rgba(255, 149, 0, 0.05));
}

.blob-2 {
  bottom: -220px;
  right: -140px;
  background: radial-gradient(circle at 30% 30%, rgba(24, 144, 255, 0.50), rgba(24, 144, 255, 0.05));
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, 0.18) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.18) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 50% 35%, rgba(0,0,0,0.9), transparent 70%);
  opacity: 0.55;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 880px;
  margin: 0 auto;
  padding: 0 16px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 149, 0, 0.25);
  color: #d46b08;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 14px;
  box-shadow: 0 8px 20px rgba(255, 149, 0, 0.12);
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 18px;
  color: #8c8c8c;
  margin-bottom: 32px;
}

.hero-points {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin: 0 auto 28px;
}

.point {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.70);
  border: 1px solid rgba(255, 255, 255, 0.65);
  color: #434343;
  font-size: 14px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);
}

.point-icon {
  color: #52c41a;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.ghost-btn {
  color: #595959;
}

.ghost-btn:hover {
  color: #262626;
}

.quick-entry {
  padding: 40px 0;
}

.section-title {
  text-align: center;
  margin-bottom: 18px;
}

.section-title h2 {
  font-size: 22px;
  margin: 0 0 6px;
  color: #262626;
  font-weight: 800;
}

.section-title p {
  margin: 0;
  color: #8c8c8c;
  font-size: 14px;
}

.entry-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.entry-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px 24px;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.06);
  display: flex;
  gap: 14px;
  align-items: flex-start;
  position: relative;
  overflow: hidden;
}

.entry-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(420px 180px at 20% 0%, rgba(255, 149, 0, 0.16), transparent 55%),
    radial-gradient(420px 180px at 80% 0%, rgba(24, 144, 255, 0.10), transparent 55%);
  opacity: 0;
  transition: opacity 0.3s;
}

.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
}

.entry-card:hover::before {
  opacity: 1;
}

.entry-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.06);
  flex: 0 0 auto;
}

.entry-main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding-top: 4px;
}

.entry-arrow {
  position: relative;
  z-index: 1;
  color: #bfbfbf;
  margin-top: 6px;
  transition: transform 0.3s, color 0.3s;
}

.entry-card:hover .entry-arrow {
  transform: translateX(4px);
  color: #8c8c8c;
}

.problems-icon {
  background: #e6f7ff;
  color: #1890ff;
}

.groups-icon {
  background: #f6ffed;
  color: #52c41a;
}

.contests-icon {
  background: #fff7e6;
  color: #fa8c16;
}

.entry-card h3 {
  font-size: 20px;
  color: #262626;
  margin-bottom: 8px;
}

.entry-card p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.stats-section {
  padding: 40px 0 80px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-item {
  background: #fff;
  border-radius: 12px;
  padding: 22px 20px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.stat-item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(360px 160px at 20% 0%, rgba(255, 149, 0, 0.10), transparent 55%);
  opacity: 0;
  transition: opacity 0.25s;
}

.stat-item:hover::before {
  opacity: 1;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.stat-orange {
  background: rgba(255, 149, 0, 0.15);
  color: #fa8c16;
}

.stat-gold {
  background: rgba(255, 215, 0, 0.18);
  color: #d48806;
}

.stat-green {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.stat-blue {
  background: rgba(24, 144, 255, 0.15);
  color: #1890ff;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: #ff6b00;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #ff6b00, #ff9500);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  position: relative;
  z-index: 1;
}

.stat-sub {
  margin-top: 6px;
  color: #bfbfbf;
  font-size: 12px;
  position: relative;
  z-index: 1;
}

@media (max-width: 900px) {
  .hero-title {
    font-size: 38px;
  }

  .entry-cards {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .home-container {
    padding: 0 16px;
  }

  .hero-section {
    padding: 56px 0 44px;
  }

  .hero-title {
    font-size: 32px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
