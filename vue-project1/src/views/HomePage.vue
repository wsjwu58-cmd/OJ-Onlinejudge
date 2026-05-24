<template>
  <div class="home-page">
    <div class="water-bg">
      <div class="water-drop water-drop-1"></div>
      <div class="water-drop water-drop-2"></div>
      <div class="water-drop water-drop-3"></div>
      <div class="ripple ripple-1"></div>
      <div class="ripple ripple-2"></div>
    </div>
    
    <main class="main-content">
      <section class="hero-section">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span class="badge-text">在线评测平台</span>
        </div>
        
        <h1 class="hero-title">
          <span class="title-line">在线评测系统</span>
        </h1>
        
        <p class="hero-description">
          高效刷题，系统提升编程能力。提供海量题目、精选题单、定期竞赛，助你成为算法高手。
        </p>
        
        <div class="hero-actions">
          <a class="action-primary" @click="$router.push('/problems')">
            <span>开始刷题</span>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
          </a>
          <a class="action-secondary" @click="$router.push('/contests')">
            <span>参加竞赛</span>
          </a>
        </div>
        
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.totalProblems }}+</span>
            <span class="hero-stat-label">题目</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.totalUsers }}+</span>
            <span class="hero-stat-label">用户</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.totalContests }}</span>
            <span class="hero-stat-label">竞赛</span>
          </div>
        </div>
      </section>

      <div class="content-section">
        <section class="profile-section">
          <h2 class="section-title">关于平台</h2>
          <div class="section-content">
            <p>这是一个专注于 <strong>算法训练</strong> 的在线评测平台，提供海量题目、精选题单、定期竞赛，帮助你系统提升编程能力。</p>
            <p>平台支持多种编程语言，包括 Java、Python、JavaScript、C++ 等，实时评测反馈，让你随时了解代码运行状态。</p>
          </div>
          <a class="section-link" @click="$router.push('/problems')">
            <span>浏览题库</span>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
          </a>
        </section>

        <section class="profile-section">
          <h2 class="section-title">快速入口</h2>
          <div class="links-grid">
            <a class="link-card" @click="$router.push('/problems')">
              <div class="link-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <line x1="10" y1="9" x2="8" y2="9"/>
                </svg>
              </div>
              <div class="link-content">
                <div class="link-title">题库</div>
                <div class="link-desc">海量算法题目，按分类、难度筛选</div>
              </div>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-arrow">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </a>
            <a class="link-card" @click="$router.push('/groups')">
              <div class="link-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
                </svg>
              </div>
              <div class="link-content">
                <div class="link-title">题单</div>
                <div class="link-desc">精选题目组合，系统性学习提升</div>
              </div>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-arrow">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </a>
            <a class="link-card" @click="$router.push('/contests')">
              <div class="link-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
                  <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
                  <path d="M4 22h16"/>
                  <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
                  <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
                  <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>
                </svg>
              </div>
              <div class="link-content">
                <div class="link-title">竞赛</div>
                <div class="link-desc">周赛、双周赛，检验你的实力</div>
              </div>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-arrow">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </a>
            <a class="link-card" @click="$router.push('/submissions')">
              <div class="link-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 3v18h18"/>
                  <path d="m19 9-5 5-4-4-3 3"/>
                </svg>
              </div>
              <div class="link-content">
                <div class="link-title">提交记录</div>
                <div class="link-desc">查看历史提交，复盘提升</div>
              </div>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-arrow">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </a>
          </div>
        </section>

        <section class="profile-section">
          <h2 class="section-title">技术栈</h2>
          <div class="stack-grid">
            <div class="stack-item">
              <span class="stack-text">Vue 3</span>
            </div>
            <div class="stack-item">
              <span class="stack-text">Element Plus</span>
            </div>
            <div class="stack-item">
              <span class="stack-text">Spring Boot</span>
            </div>
            <div class="stack-item">
              <span class="stack-text">MySQL</span>
            </div>
            <div class="stack-item">
              <span class="stack-text">Redis</span>
            </div>
          </div>
        </section>

        <section class="profile-section">
          <h2 class="section-title">为什么选择我们</h2>
          <div class="features-list">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <div class="feature-content">
                <div class="feature-title">海量题库</div>
                <div class="feature-desc">覆盖常见算法与数据结构，从入门到进阶</div>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <div class="feature-content">
                <div class="feature-title">精选题单</div>
                <div class="feature-desc">按主题打卡，体系化提升，告别盲目刷题</div>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <div class="feature-content">
                <div class="feature-title">定期竞赛</div>
                <div class="feature-desc">模拟真实场景，检验学习成果，激发学习动力</div>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <div class="feature-content">
                <div class="feature-title">社区交流</div>
                <div class="feature-desc">题解讨论，经验分享，一起成长</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <footer class="page-footer">
        <div class="footer-content">
          <span>© 2024 在线评测系统</span>
          <span class="footer-divider">·</span>
          <span>Powered by Vue & Spring Boot</span>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const stats = ref({
  totalProblems: 200,
  totalContests: 15,
  totalGroups: 30,
  totalUsers: 1000
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

.home-page {
  min-height: 100vh;
  background: #fafafa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  position: relative;
  overflow-x: hidden;
}

.water-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.water-drop {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(100, 181, 246, 0.35), rgba(59, 130, 246, 0.2));
  box-shadow: 
    inset 0 0 60px rgba(255, 255, 255, 0.4),
    0 0 40px rgba(100, 181, 246, 0.15);
  animation: float 12s ease-in-out infinite;
}

.water-drop-1 {
  width: 500px;
  height: 500px;
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

.water-drop-2 {
  width: 350px;
  height: 350px;
  bottom: 5%;
  left: -120px;
  animation-delay: 4s;
}

.water-drop-3 {
  width: 250px;
  height: 250px;
  top: 25%;
  right: 3%;
  animation-delay: 8s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-50px) rotate(5deg);
  }
}

.ripple {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(100, 181, 246, 0.45);
  box-shadow: 0 0 20px rgba(100, 181, 246, 0.15);
  animation: ripple-expand 6s ease-out infinite;
}

.ripple-1 {
  width: 200px;
  height: 200px;
  top: 15%;
  left: 8%;
  animation-delay: 0s;
}

.ripple-2 {
  width: 160px;
  height: 160px;
  bottom: 20%;
  right: 12%;
  animation-delay: 3s;
}

@keyframes ripple-expand {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(3);
    opacity: 0;
  }
}

.main-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.hero-section {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 100px;
  margin-bottom: 24px;
  background: white;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.badge-text {
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  letter-spacing: 0.02em;
}

.hero-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(2.5rem, 8vw, 4rem);
  font-weight: 700;
  color: #000;
  margin: 0 0 20px 0;
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.title-line {
  display: block;
}

.hero-description {
  font-size: 1.0625rem;
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.6);
  max-width: 520px;
  margin: 0 0 32px 0;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-bottom: 48px;
}

.action-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #000;
  color: #fff;
  font-size: 0.9375rem;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.action-primary:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.action-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  color: rgba(0, 0, 0, 0.8);
  font-size: 0.9375rem;
  font-weight: 500;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.action-secondary:hover {
  background: rgba(0, 0, 0, 0.03);
  border-color: rgba(0, 0, 0, 0.25);
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 32px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.hero-stat-value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.5rem;
  font-weight: 600;
  color: #000;
}

.hero-stat-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.hero-stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(0, 0, 0, 0.1);
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 48px;
  max-width: 640px;
  margin: 0 auto;
  padding: 0 20px 64px;
}

.profile-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0;
}

.section-content {
  color: rgba(0, 0, 0, 0.7);
  line-height: 1.8;
}

.section-content p {
  margin: 0 0 12px 0;
}

.section-content p:last-child {
  margin-bottom: 0;
}

.section-content strong {
  font-weight: 600;
  color: #000;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.section-link:hover {
  color: #000;
}

.section-link:hover svg {
  transform: translateX(3px);
}

.section-link svg {
  transition: transform 0.2s ease;
}

.links-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.link-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.link-card:hover {
  border-color: rgba(0, 0, 0, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.link-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 10px;
  color: rgba(0, 0, 0, 0.7);
}

.link-card:hover .link-icon {
  background: rgba(0, 0, 0, 0.05);
  color: #000;
}

.link-content {
  flex: 1;
}

.link-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #000;
  margin-bottom: 2px;
}

.link-desc {
  font-size: 0.8125rem;
  color: rgba(0, 0, 0, 0.5);
}

.link-arrow {
  color: rgba(0, 0, 0, 0.25);
  transition: all 0.2s ease;
}

.link-card:hover .link-arrow {
  color: #000;
  transform: translateX(3px);
}

.stack-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stack-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.stack-item:hover {
  border-color: rgba(0, 0, 0, 0.15);
}

.stack-text {
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.7);
  text-transform: capitalize;
}

.stack-item:hover .stack-text {
  color: #000;
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.feature-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background: #000;
  margin-top: 6px;
  transition: transform 0.2s ease;
}

.feature-item:hover .feature-dot {
  transform: scale(1.3);
}

.feature-content {
  flex: 1;
}

.feature-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #000;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 0.875rem;
  color: rgba(0, 0, 0, 0.6);
  line-height: 1.6;
}

.page-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding: 24px 20px;
  max-width: 640px;
  margin: 0 auto;
}

.footer-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 0.8125rem;
  color: rgba(0, 0, 0, 0.5);
}

.footer-divider {
  color: rgba(0, 0, 0, 0.2);
}

@media (max-width: 640px) {
  .hero-stats {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }
  
  .hero-stat-divider {
    width: 48px;
    height: 1px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .content-section {
    padding: 0 16px 48px;
  }
}

@media (max-width: 480px) {
  .hero-actions {
    flex-direction: column;
    width: 100%;
    max-width: 280px;
  }
  
  .action-primary,
  .action-secondary {
    width: 100%;
    justify-content: center;
  }
}
</style>
