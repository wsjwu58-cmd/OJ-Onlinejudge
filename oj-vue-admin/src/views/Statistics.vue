<template>
  <div class="statistics-container">
    <div class="statistics-header">
      <h2>数据统计</h2>
      <div class="time-range-selector">
        <el-radio-group v-model="selectedTimeRange" @change="handleTimeRangeChange" size="small">
          <el-radio-button label="7d">近七日</el-radio-button>
          <el-radio-button label="30d">近30日</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="week">本周</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    

    
    <!-- 趋势统计 -->
    <div class="trend-statistics">
      <div class="dashboard-charts">
        <!-- 用户注册趋势 -->
        <el-card class="chart-card" hoverable>
          <template #header>
            <div class="card-header">
              <span class="header-title">用户注册趋势</span>
              <el-button size="small" type="text" @click="fetchUserRegisterTrend">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="userRegisterTrend.length > 0" class="chart-content">
              <div ref="userRegisterChartRef" class="trend-chart"></div>
            </div>
            <div v-else class="no-data">
              <el-empty description="暂无数据" />
            </div>
          </div>
        </el-card>
        
        <!-- 题目数量趋势 -->
        <el-card class="chart-card" hoverable>
          <template #header>
            <div class="card-header">
              <span class="header-title">题目数量趋势</span>
              <el-button size="small" type="text" @click="fetchProblemCountTrend">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="problemCountTrend.length > 0" class="chart-content">
              <div ref="problemCountChartRef" class="trend-chart"></div>
            </div>
            <div v-else class="no-data">
              <el-empty description="暂无数据" />
            </div>
          </div>
        </el-card>
        
        <!-- 提交记录趋势 -->
        <el-card class="chart-card" hoverable>
          <template #header>
            <div class="card-header">
              <span class="header-title">提交记录趋势</span>
              <el-button size="small" type="text" @click="fetchSubmissionTrend">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="submissionTrend.length > 0" class="chart-content">
              <div ref="submissionChartRef" class="trend-chart"></div>
            </div>
            <div v-else class="no-data">
              <el-empty description="暂无数据" />
            </div>
          </div>
        </el-card>
        
        <!-- 题目通过率排行 -->
        <el-card class="chart-card" hoverable>
          <template #header>
            <div class="card-header">
              <span class="header-title">题目通过率排行</span>
              <el-button size="small" type="text" @click="fetchProblemAcceptanceRanking">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="problemAcceptanceRanking.length > 0" class="ranking-list">
              <div v-for="(problem, index) in problemAcceptanceRanking" :key="index" class="ranking-item" :class="{ 'top-ranking': index < 3 }">
                <div class="ranking-number">{{ index + 1 }}</div>
                <div class="ranking-problem">
                  <div class="problem-title">{{ problem.title }}</div>
                  <div class="problem-acceptance">
                    <span class="acceptance-value">{{ problem.acceptanceRate }}%</span>
                    <span class="acceptance-detail">({{ problem.acceptedCount }}/{{ problem.totalCount }})</span>
                  </div>
                </div>
                <div class="ranking-badge" v-if="index < 3">
                  <el-icon v-if="index === 0"><Trophy /></el-icon>
                  <el-icon v-else-if="index === 1"><Medal /></el-icon>
                  <el-icon v-else-if="index === 2"><Flag /></el-icon>
                </div>
              </div>
            </div>
            <div v-else class="no-data">
              <el-empty description="暂无数据" />
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Trophy, Medal, Flag } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { 
  getUserRegisterTrendApi,
  getProblemTrendApi,
  getRecordTrendApi,
  getProblemAcceptanceTop10Api
} from '../api/common'

// 响应式数据
const dateRange = ref([])
const activeTab = ref('user')
const selectedTimeRange = ref('7d') // 默认近七日

// 趋势数据
const userRegisterTrend = ref([])
const problemCountTrend = ref([])
const submissionTrend = ref([])
const problemAcceptanceRanking = ref([])

// 图表容器引用
const userRegisterChartRef = ref(null)
const problemCountChartRef = ref(null)
const submissionChartRef = ref(null)

// 图表实例
let userRegisterChart = null
let problemCountChart = null
let submissionChart = null

// 方法

const handleTimeRangeChange = (val) => {
  console.log('时间范围改变:', val)
  // 根据时间范围重新获取趋势数据
  fetchUserRegisterTrend()
  fetchProblemCountTrend()
  fetchSubmissionTrend()
}

const fetchStatisticsData = () => {
  // 实际项目中会调用API获取数据
  console.log('获取统计数据，标签页:', activeTab.value)
  
  // 模拟API调用
  setTimeout(() => {
    console.log('统计数据获取成功')
  }, 500)
}

// 创建折线图
const createLineChart = (chartRef, chartInstance, data, title, color) => {
  if (!chartRef.value) return
  
  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  // 创建新实例
  chartInstance = echarts.init(chartRef.value)
  
  // 提取日期和数值
  const dates = data.map(item => item.date)
  const counts = data.map(item => item.count)
  
  // 计算最大值用于动态调整
  const maxCount = Math.max(...counts, 1)
  
  // 辅助颜色（用于渐变）
  const lighterColor = color + '80'
  const lightestColor = color + '20'
  
  // 图表配置
  const option = {
    title: {
      show: false // 标题已在卡片头部显示
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: color,
      borderWidth: 2,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        color: '#1f2329',
        fontSize: 13
      },
      extraCssText: 'box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);',
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: color,
          width: 2,
          type: 'dashed',
          opacity: 0.6
        }
      },
      formatter: function(params) {
        const data = params[0]
        return `
          <div style="font-weight: 600; font-size: 14px; margin-bottom: 8px; color: #303133;">${data.name}</div>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center;">
              <span style="display: inline-block; width: 12px; height: 12px; border-radius: 50%; background: linear-gradient(135deg, ${color}, ${lighterColor}); margin-right: 10px; box-shadow: 0 2px 6px ${color}40;"></span>
              <span style="color: #606266;">数量</span>
            </div>
            <span style="font-weight: 700; font-size: 16px; color: ${color}; margin-left: 16px;">${data.value}</span>
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: {
        show: true,
        lineStyle: {
          color: '#e4e7ed',
          width: 1
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        rotate: dates.length > 10 ? 45 : 0,
        color: '#909399',
        fontSize: 11,
        interval: dates.length > 15 ? Math.floor(dates.length / 10) : 0,
        margin: 12,
        formatter: function(value) {
          // 简化日期显示
          const parts = value.split('-')
          return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : value
        }
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      min: 0,
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#909399',
        fontSize: 11,
        margin: 12,
        formatter: function(value) {
          return value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f2f5',
          type: [5, 5],
          width: 1
        }
      },
      splitNumber: 5
    },
    series: [
      {
        name: title,
        type: 'line',
        data: counts,
        smooth: 0.4,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: color },
            { offset: 0.5, color: lighterColor },
            { offset: 1, color: color }
          ]),
          shadowColor: color + '50',
          shadowBlur: 12,
          shadowOffsetY: 6,
          cap: 'round',
          join: 'round'
        },
        itemStyle: {
          color: color,
          borderColor: '#ffffff',
          borderWidth: 3,
          shadowColor: color + '60',
          shadowBlur: 10
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: color + '50' },
            { offset: 0.5, color: color + '20' },
            { offset: 1, color: color + '05' }
          ])
        },
        emphasis: {
          focus: 'series',
          scale: true,
          itemStyle: {
            color: color,
            borderColor: '#ffffff',
            borderWidth: 4,
            shadowColor: color,
            shadowBlur: 16
          },
          lineStyle: {
            width: 4
          }
        },
        markPoint: counts.length > 0 ? {
          symbol: 'pin',
          symbolSize: 50,
          itemStyle: {
            color: color,
            shadowColor: color + '60',
            shadowBlur: 8
          },
          label: {
            color: '#ffffff',
            fontWeight: 'bold',
            fontSize: 11
          },
          data: [
            { type: 'max', name: '最大值' },
            { type: 'min', name: '最小值' }
          ]
        } : null,
        markLine: {
          silent: true,
          symbol: ['none', 'none'],
          lineStyle: {
            color: color + '40',
            type: 'dashed',
            width: 1
          },
          label: {
            position: 'end',
            color: color,
            fontSize: 10,
            backgroundColor: color + '15',
            padding: [4, 8],
            borderRadius: 4
          },
          data: [
            { type: 'average', name: '平均值' }
          ]
        },
        animationDuration: 2000,
        animationEasing: 'elasticOut',
        animationDelay: function(idx) {
          return idx * 50
        }
      }
    ],
    // 数据缩放（数据量大时启用）
    dataZoom: dates.length > 20 ? [
      {
        type: 'inside',
        start: 0,
        end: 100,
        zoomLock: false
      },
      {
        type: 'slider',
        show: true,
        height: 20,
        bottom: 0,
        start: 0,
        end: 100,
        borderColor: '#e4e7ed',
        fillerColor: color + '30',
        handleStyle: {
          color: color,
          borderColor: color
        },
        textStyle: {
          color: '#909399',
          fontSize: 10
        }
      }
    ] : null
  }
  
  // 设置配置
  chartInstance.setOption(option)
  
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance.resize()
  })
  
  return chartInstance
}

// 更新折线图
const updateLineChart = (chartInstance, data) => {
  if (!chartInstance) return
  
  // 提取日期和数值
  const dates = data.map(item => item.date)
  const counts = data.map(item => item.count)
  
  // 更新配置
  chartInstance.setOption({
    xAxis: {
      data: dates
    },
    series: [
      {
        data: counts
      }
    ]
  })
}

// 根据时间范围计算开始和结束日期
const getDateRange = (timeRange) => {
  const end = new Date()
  const start = new Date()
  
  switch (timeRange) {
    case '7d':
      start.setDate(end.getDate() - 7)
      break
    case '30d':
      start.setDate(end.getDate() - 30)
      break
    case 'month':
      start.setMonth(end.getMonth() - 1)
      break
    case 'week':
      start.setDate(end.getDate() - 7)
      break
    default:
      start.setDate(end.getDate() - 7)
  }
  
  // 格式化为YYYY-MM-DD
  const formatDate = (date) => {
    return date.toISOString().split('T')[0]
  }
  
  return {
    begin: formatDate(start),
    end: formatDate(end)
  }
}

// 转换后端趋势数据格式
const transformTrendData = (data) => {
  if (!data || !data.dateList || !data.turnoverList) {
    return []
  }
  
  const dates = data.dateList.split(',')
  const counts = data.turnoverList.split(',').map(Number)
  
  return dates.map((date, index) => {
    return {
      date,
      count: counts[index] || 0
    }
  })
}

const fetchUserRegisterTrend = async () => {
  try {
    console.log('获取用户注册趋势:', selectedTimeRange.value)
    const { begin, end } = getDateRange(selectedTimeRange.value)
    console.log('日期范围:', begin, end)
    const response = await getUserRegisterTrendApi(begin, end)
    
    if (response.code === 1 && response.data) {
      console.log('用户注册趋势数据:', response.data)
      userRegisterTrend.value = transformTrendData(response.data)
      console.log('转换后的数据:', userRegisterTrend.value)
      // 创建或更新折线图
      userRegisterChart = createLineChart(userRegisterChartRef, userRegisterChart, userRegisterTrend.value, '用户注册趋势', '#409eff')
    } else {
      console.log('响应格式不正确:', response)
      userRegisterTrend.value = []
    }
  } catch (error) {
    console.error('获取用户注册趋势失败:', error)
    ElMessage.error('获取用户注册趋势失败，请稍后重试')
    userRegisterTrend.value = []
  }
}

const fetchProblemCountTrend = async () => {
  try {
    console.log('获取题目数量趋势:', selectedTimeRange.value)
    const { begin, end } = getDateRange(selectedTimeRange.value)
    console.log('日期范围:', begin, end)
    const response = await getProblemTrendApi(begin, end)
    
    if (response.code === 1 && response.data) {
      console.log('题目数量趋势数据:', response.data)
      problemCountTrend.value = transformTrendData(response.data)
      console.log('转换后的数据:', problemCountTrend.value)
      // 创建或更新折线图
      problemCountChart = createLineChart(problemCountChartRef, problemCountChart, problemCountTrend.value, '题目数量趋势', '#67c23a')
    } else {
      console.log('响应格式不正确:', response)
      problemCountTrend.value = []
    }
  } catch (error) {
    console.error('获取题目数量趋势失败:', error)
    ElMessage.error('获取题目数量趋势失败，请稍后重试')
    problemCountTrend.value = []
  }
}

const fetchSubmissionTrend = async () => {
  try {
    console.log('获取提交记录趋势:', selectedTimeRange.value)
    const { begin, end } = getDateRange(selectedTimeRange.value)
    console.log('日期范围:', begin, end)
    const response = await getRecordTrendApi(begin, end)
    
    if (response.code === 1 && response.data) {
      console.log('提交记录趋势数据:', response.data)
      submissionTrend.value = transformTrendData(response.data)
      console.log('转换后的数据:', submissionTrend.value)
      // 创建或更新折线图
      submissionChart = createLineChart(submissionChartRef, submissionChart, submissionTrend.value, '提交记录趋势', '#f56c6c')
    } else {
      console.log('响应格式不正确:', response)
      submissionTrend.value = []
    }
  } catch (error) {
    console.error('获取提交记录趋势失败:', error)
    ElMessage.error('获取提交记录趋势失败，请稍后重试')
    submissionTrend.value = []
  }
}

const fetchProblemAcceptanceRanking = async () => {
  try {
    console.log('获取题目通过率排行')
    const response = await getProblemAcceptanceTop10Api()
    
    if (response.code === 1 && response.data) {
      console.log('题目通过率排行数据:', response.data)
      problemAcceptanceRanking.value = response.data
    } else {
      console.log('响应格式不正确:', response)
      problemAcceptanceRanking.value = []
    }
  } catch (error) {
    console.error('获取题目通过率排行失败:', error)
    ElMessage.error('获取题目通过率排行失败，请稍后重试')
    problemAcceptanceRanking.value = []
  }
}

// 生命周期钩子
onMounted(() => {
  fetchUserRegisterTrend()
  fetchProblemCountTrend()
  fetchSubmissionTrend()
  fetchProblemAcceptanceRanking()
})
</script>

<style scoped>
.statistics-container {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e9f2 100%);
  min-height: 100vh;
}

.statistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3);
}

.statistics-header h2 {
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.time-range-selector {
  display: flex;
  gap: 10px;
}

.time-range-selector :deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  padding: 4px;
}

.time-range-selector :deep(.el-radio-button__inner) {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.time-range-selector :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #ffffff;
  color: #667eea;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.trend-statistics {
  margin-top: 8px;
}

.dashboard-charts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

@media (max-width: 1200px) {
  .dashboard-charts {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  background: #ffffff;
}

.chart-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.chart-card :deep(.el-card__header) {
  padding: 18px 24px;
  border-bottom: 1px solid #f0f2f5;
  background: linear-gradient(180deg, #fafbfc 0%, #ffffff 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  position: relative;
  padding-left: 14px;
}

.header-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #409eff 0%, #67c23a 100%);
  border-radius: 2px;
}

.card-header :deep(.el-button) {
  color: #909399;
  font-size: 13px;
  transition: all 0.3s;
}

.card-header :deep(.el-button:hover) {
  color: #409eff;
  transform: rotate(180deg);
}

.chart-container {
  padding: 20px 24px 24px;
}

.chart-content {
  position: relative;
}

.trend-chart {
  height: 320px;
  width: 100%;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 320px;
  color: #c0c4cc;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%);
  border-radius: 12px;
}

/* 排行榜样式 */
.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 8px;
}

.ranking-list::-webkit-scrollbar {
  width: 6px;
}

.ranking-list::-webkit-scrollbar-track {
  background: #f0f2f5;
  border-radius: 3px;
}

.ranking-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #c0c4cc 0%, #909399 100%);
  border-radius: 3px;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border: 1px solid #f0f2f5;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ranking-item:hover {
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border-color: #e4e7ed;
}

.ranking-item.top-ranking:nth-child(1) {
  background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%);
  border-color: #ffd666;
}

.ranking-item.top-ranking:nth-child(2) {
  background: linear-gradient(135deg, #f6f8fa 0%, #f0f2f5 100%);
  border-color: #d9d9d9;
}

.ranking-item.top-ranking:nth-child(3) {
  background: linear-gradient(135deg, #fff1e6 0%, #fff7e6 100%);
  border-color: #ffbb96;
}

.ranking-number {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  border-radius: 8px;
  margin-right: 14px;
  background: linear-gradient(135deg, #f0f2f5 0%, #e4e7ed 100%);
  color: #606266;
}

.ranking-item.top-ranking:nth-child(1) .ranking-number {
  background: linear-gradient(135deg, #ffc53d 0%, #fa8c16 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(250, 140, 22, 0.4);
}

.ranking-item.top-ranking:nth-child(2) .ranking-number {
  background: linear-gradient(135deg, #bfbfbf 0%, #8c8c8c 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(140, 140, 140, 0.4);
}

.ranking-item.top-ranking:nth-child(3) .ranking-number {
  background: linear-gradient(135deg, #d48806 0%, #ad6800 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(173, 104, 0, 0.4);
}

.ranking-problem {
  flex: 1;
  min-width: 0;
}

.problem-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.problem-acceptance {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
}

.acceptance-value {
  font-weight: 600;
  color: #67c23a;
  font-size: 14px;
}

.acceptance-detail {
  color: #c0c4cc;
}

.ranking-badge {
  margin-left: 12px;
  font-size: 20px;
}

.ranking-item.top-ranking:nth-child(1) .ranking-badge {
  color: #faad14;
}

.ranking-item.top-ranking:nth-child(2) .ranking-badge {
  color: #8c8c8c;
}

.ranking-item.top-ranking:nth-child(3) .ranking-badge {
  color: #d48806;
}
</style>