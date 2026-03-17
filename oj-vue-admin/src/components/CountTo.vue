<!-- 数字滚动动画组件 - 参考 art-design-pro 实现 -->
<template>
  <span class="count-to" :class="{ 'is-running': isRunning }">
    {{ formattedValue }}
  </span>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  // 目标值
  target: {
    type: Number,
    default: 0
  },
  // 动画持续时间（毫秒）
  duration: {
    type: Number,
    default: 1300
  },
  // 小数位数
  decimals: {
    type: Number,
    default: 0
  },
  // 千分位分隔符
  separator: {
    type: String,
    default: ','
  },
  // 前缀
  prefix: {
    type: String,
    default: ''
  },
  // 后缀
  suffix: {
    type: String,
    default: ''
  },
  // 是否自动开始
  autoStart: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['started', 'finished'])

const currentValue = ref(0)
const isRunning = ref(false)
let animationFrameId = null
let startTime = null

// 缓动函数 - easeOutExpo（与 art-design-pro 一致）
const easeOutExpo = (t) => {
  return t === 1 ? 1 : 1 - Math.pow(2, -10 * t)
}

// 格式化数字
const formatNumber = (value) => {
  let result = props.decimals > 0 
    ? value.toFixed(props.decimals) 
    : Math.floor(value).toString()

  // 添加千分位分隔符
  if (props.separator) {
    const parts = result.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, props.separator)
    result = parts.join('.')
  }

  return result
}

// 格式化显示值
const formattedValue = computed(() => {
  const formatted = formatNumber(currentValue.value)
  return `${props.prefix}${formatted}${props.suffix}`
})

// 动画函数
const animate = (timestamp) => {
  if (!startTime) startTime = timestamp
  
  const elapsed = timestamp - startTime
  const progress = Math.min(elapsed / props.duration, 1)
  
  // 使用缓动函数
  const easedProgress = easeOutExpo(progress)
  currentValue.value = easedProgress * props.target
  
  if (progress < 1) {
    animationFrameId = requestAnimationFrame(animate)
  } else {
    currentValue.value = props.target
    isRunning.value = false
    emit('finished', props.target)
  }
}

// 开始动画
const start = () => {
  if (isRunning.value) return
  
  // 重置状态
  currentValue.value = 0
  startTime = null
  isRunning.value = true
  emit('started', props.target)
  
  animationFrameId = requestAnimationFrame(animate)
}

// 停止动画
const stop = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  isRunning.value = false
}

// 重置
const reset = () => {
  stop()
  currentValue.value = 0
}

// 监听目标值变化，重新播放动画
watch(() => props.target, (newVal, oldVal) => {
  if (newVal !== oldVal && props.autoStart) {
    reset()
    start()
  }
})

// 组件挂载时自动开始
onMounted(() => {
  if (props.autoStart && props.target > 0) {
    start()
  }
})

// 组件卸载时清理
onUnmounted(() => {
  stop()
})

// 暴露方法给父组件
defineExpose({
  start,
  stop,
  reset
})
</script>

<style scoped>
.count-to {
  font-variant-numeric: tabular-nums;
  transition: opacity 0.3s ease-in-out;
}

.count-to.is-running {
  opacity: 1;
}
</style>
