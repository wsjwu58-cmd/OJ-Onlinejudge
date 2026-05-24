<template>
  <div class="slide-verify" :style="{ width: canvasWidth + 'px' }">
    <div
      v-if="isLoading"
      class="img-loading"
      :style="{ height: canvasHeight + 'px' }"
    ></div>
    <div
      v-if="verifySuccess"
      class="success-hint"
      :style="{ height: canvasHeight + 'px' }"
    >
      {{ successHint }}
    </div>
    <div class="refresh-icon" @click="refresh"></div>
    <img
      ref="canvasRef"
      class="slide-canvas"
      :width="canvasWidth"
      :height="canvasHeight"
    />
    <img
      ref="blockRef"
      :class="['slide-block', { 'verify-fail': verifyFail }]"
    />
    <div
      class="slider"
      :class="{
        'verify-active': verifyActive,
        'verify-success': verifySuccess,
        'verify-fail': verifyFail
      }"
    >
      <div class="slider-box" :style="{ width: sliderBoxWidth }">
        <div
          ref="sliderButtonRef"
          class="slider-button"
          :style="{ left: sliderButtonLeft }"
        >
          <div class="slider-button-icon"></div>
        </div>
      </div>
      <span class="slider-hint">{{ sliderHint }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getCaptcha } from '../services/api'

const props = defineProps({
  canvasWidth: {
    type: Number,
    default: 320
  },
  canvasHeight: {
    type: Number,
    default: 155
  },
  sliderHint: {
    type: String,
    default: '向右滑动'
  },
  accuracy: {
    type: Number,
    default: 3
  }
})

const emit = defineEmits(['success', 'fail', 'again'])

const canvasRef = ref(null)
const blockRef = ref(null)
const sliderButtonRef = ref(null)

const isLoading = ref(true)
const verifyActive = ref(false)
const verifySuccess = ref(false)
const verifyFail = ref(false)
const sliderBoxWidth = ref('0px')
const sliderButtonLeft = ref('0px')
const successHint = ref('')
const nonceStr = ref('')

const originX = ref(0)
const isMouseDown = ref(false)
const timestamp = ref(0)
const dragDistanceList = ref([])

const fetchCaptcha = async () => {
  try {
    isLoading.value = true
    const data = await getCaptcha()
    nonceStr.value = data.nonceStr
    blockRef.value.src = data.blockSrc
    blockRef.value.style.top = data.blockY + 'px'
    canvasRef.value.src = data.canvasSrc
  } catch (e) {
    console.error('获取验证码失败:', e)
  } finally {
    isLoading.value = false
  }
}

const startEvent = (clientX, clientY) => {
  if (isLoading.value || verifySuccess.value) return
  originX.value = clientX
  isMouseDown.value = true
  timestamp.value = Date.now()
  dragDistanceList.value = []
}

const moveEvent = (clientX, clientY) => {
  if (!isMouseDown.value) return
  const moveX = clientX - originX.value
  const moveY = clientY - originX.value
  if (moveX < 0 || moveX + 40 >= props.canvasWidth) return
  sliderButtonLeft.value = moveX + 'px'
  const blockLeft = ((props.canvasWidth - 40 - 20) / (props.canvasWidth - 40)) * moveX
  blockRef.value.style.left = blockLeft + 'px'
  verifyActive.value = true
  sliderBoxWidth.value = moveX + 'px'
  dragDistanceList.value.push(moveY)
}

const endEvent = (clientX) => {
  if (!isMouseDown.value) return
  isMouseDown.value = false
  if (clientX === originX.value) return

  isLoading.value = true
  verifyActive.value = false
  timestamp.value = Date.now() - timestamp.value
  const moveLength = parseInt(blockRef.value.style.left) || 0

  if (timestamp.value > 10000) {
    verifyFailEvent()
  } else if (!turingTest()) {
    verifyFail.value = true
    emit('again')
  } else {
    emit('success', { nonceStr: nonceStr.value, value: moveLength })
  }
}

const turingTest = () => {
  const arr = dragDistanceList.value
  if (arr.length === 0) return false
  const average = arr.reduce((a, b) => a + b, 0) / arr.length
  const deviations = arr.map((x) => x - average)
  const stdDev = Math.sqrt(deviations.map((x) => x * x).reduce((a, b) => a + b, 0) / arr.length)
  return average !== stdDev
}

const verifySuccessEvent = () => {
  isLoading.value = false
  verifySuccess.value = true
  const elapsedTime = (timestamp.value / 1000).toFixed(1)
  if (elapsedTime < 1) {
    successHint.value = `仅仅${elapsedTime}S，你的速度快如闪电`
  } else if (elapsedTime < 2) {
    successHint.value = `只用了${elapsedTime}S，这速度简直完美`
  } else {
    successHint.value = `耗时${elapsedTime}S，争取下次再快一点`
  }
}

const verifyFailEvent = (msg) => {
  verifyFail.value = true
  emit('fail', msg)
  refresh()
}

const refresh = () => {
  setTimeout(() => {
    verifyFail.value = false
  }, 500)
  isLoading.value = true
  verifyActive.value = false
  verifySuccess.value = false
  blockRef.value.style.left = 0
  sliderBoxWidth.value = '0px'
  sliderButtonLeft.value = '0px'
  fetchCaptcha()
}

const handleMouseDown = (e) => startEvent(e.clientX, e.clientY)
const handleMouseMove = (e) => moveEvent(e.clientX, e.clientY)
const handleMouseUp = (e) => endEvent(e.clientX)
const handleTouchStart = (e) => startEvent(e.changedTouches[0].pageX, e.changedTouches[0].pageY)
const handleTouchMove = (e) => moveEvent(e.changedTouches[0].pageX, e.changedTouches[0].pageY)
const handleTouchEnd = (e) => endEvent(e.changedTouches[0].pageX)

onMounted(() => {
  fetchCaptcha()
  const btn = sliderButtonRef.value
  if (btn) {
    btn.addEventListener('mousedown', handleMouseDown)
    btn.addEventListener('touchstart', handleTouchStart)
  }
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.addEventListener('touchmove', handleTouchMove)
  document.addEventListener('touchend', handleTouchEnd)
})

onBeforeUnmount(() => {
  const btn = sliderButtonRef.value
  if (btn) {
    btn.removeEventListener('mousedown', handleMouseDown)
    btn.removeEventListener('touchstart', handleTouchStart)
  }
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('touchmove', handleTouchMove)
  document.removeEventListener('touchend', handleTouchEnd)
})

defineExpose({
  verifySuccessEvent,
  verifyFailEvent,
  refresh
})
</script>

<style scoped>
.slide-verify {
  position: relative;
  user-select: none;
}

.img-loading {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  bottom: 0;
  z-index: 999;
  animation: loading 1.5s infinite;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='40' fill='none' stroke='%23fff' stroke-width='6' stroke-dasharray='60 200' stroke-linecap='round'%3E%3CanimateTransform attributeName='transform' type='rotate' from='0 50 50' to='360 50 50' dur='1s' repeatCount='indefinite'/%3E%3C/circle%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center center;
  background-size: 60px;
  background-color: #737c8e;
  border-radius: 5px;
}

@keyframes loading {
  0% { opacity: 0.7; }
  100% { opacity: 0.9; }
}

.success-hint {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  color: #2cd000;
  font-size: 16px;
  font-weight: 500;
}

.refresh-icon {
  position: absolute;
  right: 5px;
  top: 5px;
  width: 30px;
  height: 30px;
  cursor: pointer;
  z-index: 10;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-icon::before {
  content: '↻';
  font-size: 18px;
  color: #fff;
}

.refresh-icon:hover {
  background: rgba(0, 0, 0, 0.2);
}

.slide-canvas {
  border-radius: 5px;
  display: block;
}

.slide-block {
  position: absolute;
  left: 0;
  top: 0;
}

.slide-block.verify-fail {
  transition: left 0.5s linear;
}

.slider {
  position: relative;
  text-align: center;
  width: 100%;
  height: 40px;
  line-height: 40px;
  margin-top: 15px;
  background: #f7f9fa;
  color: #45494c;
  border: 1px solid #e4e7eb;
  border-radius: 5px;
}

.slider-box {
  position: absolute;
  left: 0;
  top: 0;
  height: 40px;
  border: 0 solid #1991fa;
  background: #d1e9fe;
  border-radius: 5px;
}

.slider-button {
  position: absolute;
  top: 0;
  left: 0;
  width: 40px;
  height: 40px;
  background: #fff;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: background 0.2s linear;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slider-button:hover {
  background: #1991fa;
}

.slider-button:hover .slider-button-icon {
  border-color: #fff;
}

.slider-button-icon {
  width: 0;
  height: 0;
  border-left: 6px solid #1991fa;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  transition: border-color 0.2s linear;
}

.verify-active .slider-button {
  height: 38px;
  top: -1px;
  border: 1px solid #1991fa;
}

.verify-active .slider-box {
  height: 38px;
  border-width: 1px;
}

.verify-success .slider-box {
  height: 38px;
  border: 1px solid #52ccba;
  background-color: #d2f4ef;
}

.verify-success .slider-button {
  height: 38px;
  top: -1px;
  border: 1px solid #52ccba;
  background-color: #52ccba !important;
}

.verify-success .slider-button-icon {
  border-left-color: #fff !important;
}

.verify-fail .slider-box {
  height: 38px;
  border: 1px solid #f57a7a;
  background-color: #fce1e1;
  transition: width 0.5s linear;
}

.verify-fail .slider-button {
  height: 38px;
  top: -1px;
  border: 1px solid #f57a7a;
  background-color: #f57a7a !important;
  transition: left 0.5s linear;
}

.verify-active .slider-hint,
.verify-success .slider-hint,
.verify-fail .slider-hint {
  display: none;
}
</style>
