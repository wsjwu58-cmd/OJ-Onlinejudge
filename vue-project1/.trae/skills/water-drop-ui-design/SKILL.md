---
name: "water-drop-ui-design"
description: "水滴动态效果UI设计系统，包含视觉规范、排版布局、色彩方案。适用于需要清新、现代、动态效果的页面设计任务。"
---

# 水滴动态效果 UI 设计系统

## 一、水滴效果视觉设计规范

### 1.1 水滴基础样式

```css
.water-drop {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(100, 181, 246, 0.35), rgba(59, 130, 246, 0.2));
  box-shadow: 
    inset 0 0 60px rgba(255, 255, 255, 0.4),
    0 0 40px rgba(100, 181, 246, 0.15);
  animation: float 12s ease-in-out infinite;
}
```

**设计要点：**
- 使用径向渐变（radial-gradient）创建立体感
- 光源位置在左上角（30% 30%）
- 双层阴影：内阴影增加深度，外阴影增加发光效果
- 动画周期 12 秒，使用 ease-in-out 缓动函数

### 1.2 水滴尺寸与位置

```css
/* 大水滴 - 右上角 */
.water-drop-1 {
  width: 500px;
  height: 500px;
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

/* 中水滴 - 左下角 */
.water-drop-2 {
  width: 350px;
  height: 350px;
  bottom: 5%;
  left: -120px;
  animation-delay: 4s;
}

/* 小水滴 - 右侧 */
.water-drop-3 {
  width: 250px;
  height: 250px;
  top: 25%;
  right: 3%;
  animation-delay: 8s;
}
```

**布局原则：**
- 大中小三种尺寸，形成视觉层次
- 分布在页面三个角落，避免遮挡主要内容
- 动画延迟错开（0s、4s、8s），避免同步运动

### 1.3 浮动动画

```css
@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-50px) rotate(5deg);
  }
}
```

**动画参数：**
- 垂直移动：50px
- 旋转角度：5度
- 缓动函数：ease-in-out
- 循环：无限循环（infinite）

### 1.4 涟漪效果

```css
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
```

**涟漪设计要点：**
- 边框颜色略深于水滴
- 添加发光效果（box-shadow）
- 扩散比例：1 → 3
- 透明度渐变：0.6 → 0

### 1.5 HTML 结构

```html
<div class="water-bg">
  <div class="water-drop water-drop-1"></div>
  <div class="water-drop water-drop-2"></div>
  <div class="water-drop water-drop-3"></div>
  <div class="ripple ripple-1"></div>
  <div class="ripple ripple-2"></div>
</div>
```

```css
.water-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
```

---

## 二、排版布局规范

### 2.1 网格系统

```css
/* 内容区域最大宽度 */
.content-section {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 20px 64px;
}

/* 响应式容器 */
@media (min-width: 768px) {
  .content-section {
    width: 80%;
  }
}

@media (min-width: 1024px) {
  .content-section {
    width: 83.333%;
  }
}
```

### 2.2 间距系统

```css
/* 基础间距单位：8px */
--spacing-xs: 4px;    /* 0.5x */
--spacing-sm: 8px;    /* 1x */
--spacing-md: 16px;   /* 2x */
--spacing-lg: 24px;   /* 3x */
--spacing-xl: 32px;   /* 4x */
--spacing-2xl: 48px;  /* 6x */
--spacing-3xl: 64px;  /* 8x */

/* 区块间距 */
.section-gap {
  margin-bottom: 48px;
}

/* 卡片内边距 */
.card-padding {
  padding: 16px;
}

/* 网格间距 */
.grid-gap {
  gap: 12px;
}
```

### 2.3 响应式断点

```css
/* 移动端优先 */
/* 默认：< 480px */

/* 小屏手机 */
@media (max-width: 480px) {
  .hero-actions {
    flex-direction: column;
  }
}

/* 大屏手机 */
@media (max-width: 640px) {
  .hero-stats {
    flex-direction: column;
  }
}

/* 平板 */
@media (min-width: 768px) {
  .content-section {
    width: 80%;
  }
}

/* 桌面 */
@media (min-width: 1024px) {
  .content-section {
    width: 83.333%;
  }
}
```

### 2.4 卡片布局

```html
<!-- 快速入口卡片 -->
<a class="link-card">
  <div class="link-icon">
    <svg><!-- 图标 --></svg>
  </div>
  <div class="link-content">
    <div class="link-title">标题</div>
    <div class="link-desc">描述文字</div>
  </div>
  <svg class="link-arrow"><!-- 箭头 --></svg>
</a>
```

```css
.link-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.link-card:hover {
  border-color: rgba(0, 0, 0, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
```

---

## 三、设计风格定义

### 3.1 色彩方案

```css
/* 主色调 - 黑白灰 */
--color-primary: #000000;
--color-background: #fafafa;
--color-surface: #ffffff;

/* 文字颜色 */
--color-text-primary: rgba(0, 0, 0, 1);
--color-text-secondary: rgba(0, 0, 0, 0.7);
--color-text-tertiary: rgba(0, 0, 0, 0.6);
--color-text-muted: rgba(0, 0, 0, 0.5);

/* 边框颜色 */
--color-border-light: rgba(0, 0, 0, 0.08);
--color-border-medium: rgba(0, 0, 0, 0.15);

/* 强调色 - 水滴蓝 */
--color-accent-light: rgba(100, 181, 246, 0.35);
--color-accent-medium: rgba(59, 130, 246, 0.2);
--color-accent-border: rgba(100, 181, 246, 0.45);

/* 状态色 */
--color-success: #22c55e;
```

**色彩使用原则：**
- 主色调：黑白灰，保持极简风格
- 强调色：水滴蓝，仅用于动态效果
- 不使用渐变色（水滴效果除外）
- 透明度层次：1、0.7、0.6、0.5、0.08

### 3.2 字体系统

```css
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

/* 字体家族 */
--font-display: 'Space Grotesk', sans-serif;  /* 标题 */
--font-body: 'Inter', sans-serif;              /* 正文 */
--font-mono: 'JetBrains Mono', monospace;      /* 数字/代码 */

/* 字体大小 */
--text-xs: 0.75rem;      /* 12px */
--text-sm: 0.8125rem;    /* 13px */
--text-base: 0.875rem;   /* 14px */
--text-md: 0.9375rem;    /* 15px */
--text-lg: 1rem;         /* 16px */
--text-xl: 1.0625rem;    /* 17px */
--text-2xl: 1.5rem;      /* 24px */
--text-3xl: 2rem;        /* 32px */
--text-4xl: clamp(2.5rem, 8vw, 4rem);  /* 响应式大标题 */

/* 字重 */
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;

/* 行高 */
--leading-tight: 1.1;
--leading-normal: 1.6;
--leading-relaxed: 1.7;
--leading-loose: 1.8;

/* 字母间距 */
--tracking-tight: -0.03em;
--tracking-normal: 0;
--tracking-wide: 0.02em;
--tracking-wider: 0.05em;
--tracking-widest: 0.1em;
```

**字体使用场景：**
- **Space Grotesk**：页面主标题、区块标题
- **Inter**：正文、描述、按钮文字
- **JetBrains Mono**：统计数据、数字、代码

### 3.3 视觉层次

```css
/* 标题层次 */
.hero-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(2.5rem, 8vw, 4rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.section-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(0, 0, 0, 0.5);
}

/* 内容层次 */
.section-content {
  color: rgba(0, 0, 0, 0.7);
  line-height: 1.8;
}

.section-content strong {
  font-weight: 600;
  color: #000;
}

/* 链接层次 */
.section-link {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
}
```

### 3.4 圆角系统

```css
--radius-sm: 6px;
--radius-md: 8px;
--radius-lg: 10px;
--radius-xl: 12px;
--radius-2xl: 20px;
--radius-full: 100px;
--radius-circle: 50%;
```

**使用场景：**
- 按钮：8px
- 卡片：10px
- 标签：6px
- 徽章：100px（全圆）
- 图标容器：10px
- 统计卡片：12px

### 3.5 阴影系统

```css
/* 卡片阴影 */
--shadow-card: 0 2px 8px rgba(0, 0, 0, 0.04);
--shadow-card-hover: 0 4px 12px rgba(0, 0, 0, 0.05);

/* 水滴阴影 */
--shadow-drop-inner: inset 0 0 60px rgba(255, 255, 255, 0.4);
--shadow-drop-outer: 0 0 40px rgba(100, 181, 246, 0.15);

/* 涟漪阴影 */
--shadow-ripple: 0 0 20px rgba(100, 181, 246, 0.15);
```

---

## 四、完整组件代码示例

### 4.1 Hero 区域

```html
<section class="hero-section">
  <div class="hero-badge">
    <span class="badge-dot"></span>
    <span class="badge-text">在线评测平台</span>
  </div>
  
  <h1 class="hero-title">
    <span class="title-line">在线评测系统</span>
  </h1>
  
  <p class="hero-description">
    高效刷题，系统提升编程能力。
  </p>
  
  <div class="hero-actions">
    <a class="action-primary">
      <span>开始刷题</span>
      <svg><!-- 箭头图标 --></svg>
    </a>
    <a class="action-secondary">
      <span>参加竞赛</span>
    </a>
  </div>
  
  <div class="hero-stats">
    <div class="hero-stat">
      <span class="hero-stat-value">200+</span>
      <span class="hero-stat-label">题目</span>
    </div>
    <div class="hero-stat-divider"></div>
    <div class="hero-stat">
      <span class="hero-stat-value">1000+</span>
      <span class="hero-stat-label">用户</span>
    </div>
  </div>
</section>
```

```css
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
  background: white;
  margin-bottom: 24px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
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
  transition: all 0.2s ease;
}

.action-primary:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.action-secondary {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  background: transparent;
  color: rgba(0, 0, 0, 0.8);
  font-size: 0.9375rem;
  font-weight: 500;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  cursor: pointer;
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
```

### 4.2 内容区块

```html
<section class="profile-section">
  <h2 class="section-title">关于平台</h2>
  <div class="section-content">
    <p>这是一个专注于 <strong>算法训练</strong> 的在线评测平台。</p>
  </div>
  <a class="section-link">
    <span>浏览题库</span>
    <svg><!-- 箭头 --></svg>
  </a>
</section>
```

```css
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
  transition: all 0.2s ease;
}

.section-link:hover {
  color: #000;
}

.section-link:hover svg {
  transform: translateX(3px);
}
```

---

## 五、使用指南

### 5.1 适用场景

- 需要清新、现代、动态效果的页面
- 在线教育平台、工具类网站
- 需要极简主义设计风格的项目
- 需要响应式布局的移动端优先设计

### 5.2 设计原则

1. **极简主义**：黑白灰为主色调，避免过多装饰
2. **动态点缀**：水滴效果作为视觉焦点，不过度使用
3. **层次分明**：通过字体大小、颜色透明度建立视觉层次
4. **响应式优先**：移动端优先，逐步增强桌面端体验
5. **无渐变设计**：除水滴效果外，不使用渐变色

### 5.3 注意事项

- 水滴效果使用 `position: fixed`，确保 `z-index` 低于内容层
- 动画使用 `pointer-events: none` 避免阻挡交互
- 字体加载使用 Google Fonts CDN
- 颜色使用 `rgba()` 格式确保一致性
- 响应式断点遵循移动端优先原则

---

## 六、快速启动模板

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>水滴动态效果 UI</title>
  <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
</head>
<body>
  <div class="page">
    <!-- 水滴背景 -->
    <div class="water-bg">
      <div class="water-drop water-drop-1"></div>
      <div class="water-drop water-drop-2"></div>
      <div class="water-drop water-drop-3"></div>
      <div class="ripple ripple-1"></div>
      <div class="ripple ripple-2"></div>
    </div>
    
    <!-- 主内容 -->
    <main class="main-content">
      <!-- Hero 区域 -->
      <!-- 内容区块 -->
    </main>
  </div>
</body>
</html>
```

---

**文档版本**: v1.0  
**最后更新**: 2024-03-28  
**维护者**: UI Design Team
