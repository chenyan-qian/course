<template>
  <div class="dashboard">
    <!-- 顶部日期横幅 -->
    <div class="dash-banner">
      <div class="dash-date-info">
        <div class="dash-day-label">{{ todayLabel }}</div>
        <div class="dash-date">{{ todayDate }}</div>
      </div>
      <div class="dash-summary">
        <div class="dash-stat">
          <span class="dash-stat-num">{{ todayCourses.length }}</span>
          <span class="dash-stat-label">今日课程</span>
        </div>
        <div class="dash-stat warn">
          <span class="dash-stat-num">{{ dueSoonCount }}</span>
          <span class="dash-stat-label">即将截止作业</span>
        </div>
        <div class="dash-stat info">
          <span class="dash-stat-num">{{ unfinishedCount }}</span>
          <span class="dash-stat-label">待完成作业</span>
        </div>
      </div>
    </div>

    <!-- ========== 数据统计卡片 ========== -->
    <div class="stats-cards">
      <div class="stats-card courses">
        <div class="stats-card-icon">📚</div>
        <div class="stats-card-body">
          <span class="stats-card-num">{{ stats.totalCourses }}</span>
          <span class="stats-card-label">课程总数</span>
        </div>
      </div>
      <div class="stats-card points">
        <div class="stats-card-icon">📖</div>
        <div class="stats-card-body">
          <span class="stats-card-num">{{ stats.totalKnowledgePoints }}</span>
          <span class="stats-card-label">知识点总数</span>
        </div>
      </div>
      <div class="stats-card mastered">
        <div class="stats-card-icon">✅</div>
        <div class="stats-card-body">
          <span class="stats-card-num">{{ stats.mastered }}</span>
          <span class="stats-card-label">已掌握</span>
        </div>
      </div>
      <div class="stats-card tolearn">
        <div class="stats-card-icon">📝</div>
        <div class="stats-card-body">
          <span class="stats-card-num">{{ stats.notStarted + stats.learning }}</span>
          <span class="stats-card-label">待学习</span>
        </div>
      </div>
    </div>

    <!-- ========== ECharts 图表区 ========== -->
    <div class="charts-row">
      <div class="chart-panel">
        <h3 class="chart-title">📊 课程分布（按星期）</h3>
        <div ref="courseChartRef" class="chart-box"></div>
      </div>
      <div class="chart-panel">
        <h3 class="chart-title">📈 学习进度</h3>
        <div ref="progressChartRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 今日课程区域 -->
    <div class="dash-section">
      <h2 class="dash-section-title">📖 今日课程</h2>

      <div v-if="todayCourses.length === 0" class="dash-empty">
        <div class="dash-empty-icon">🎉</div>
        <p>今天没有课程安排，享受自由时光吧~</p>
      </div>

      <div v-else class="today-timeline">
        <div v-for="(c, idx) in sortedTodayCourses" :key="c.id" class="today-card-wrapper">
          <div class="timeline-connector" v-if="idx < sortedTodayCourses.length - 1"></div>

          <div class="today-card"
            :class="{ 'is-now': c.isOngoing, 'is-next': idx === nextCourseIndex }"
            :style="{ borderLeftColor: c.palette.border, background: c.palette.bg }">
            <div class="today-time">
              <div class="time-start">{{ c.startTime }}</div>
              <div class="time-divider"></div>
              <div class="time-end">{{ c.endTime }}</div>
            </div>
            <div class="today-info">
              <div class="today-name">
                {{ c.name }}
                <el-tag v-if="c.isOngoing" type="danger" size="small" effect="dark" class="ongoing-tag">
                  正在进行
                </el-tag>
                <el-tag v-else-if="idx === nextCourseIndex" type="warning" size="small" effect="dark" class="ongoing-tag">
                  即将开始
                </el-tag>
              </div>
              <div class="today-details">
                <span v-if="c.teacher" class="today-detail">👨‍🏫 {{ c.teacher }}</span>
                <span v-if="c.classroom" class="today-detail">🏫 {{ c.classroom }}</span>
                <span class="today-detail">⏱ {{ c.duration }}</span>
              </div>
            </div>
            <div class="today-progress" v-if="c.isOngoing">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: c.progressPercent + '%' }"></div>
              </div>
              <span class="progress-text">{{ c.progressPercent }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

const courses = ref([])
const homeworks = ref([])
const stats = ref({
  totalCourses: 0,
  totalKnowledgePoints: 0,
  mastered: 0,
  learning: 0,
  notStarted: 0
})

// ECharts refs
const courseChartRef = ref(null)
const progressChartRef = ref(null)
let courseChart = null
let progressChart = null

const COURSE_COLORS = [
  { bg: '#e8f4fd', border: '#2196f3' },
  { bg: '#fff3e0', border: '#ff9800' },
  { bg: '#e8f5e9', border: '#4caf50' },
  { bg: '#fce4ec', border: '#e91e63' },
  { bg: '#f3e5f5', border: '#9c27b0' },
  { bg: '#e0f2f1', border: '#009688' },
  { bg: '#fff8e1', border: '#ffc107' },
  { bg: '#e3f2fd', border: '#1976d2' },
  { bg: '#fbe9e7', border: '#ff5722' },
  { bg: '#e0f7fa', border: '#00bcd4' },
  { bg: '#f1f8e9', border: '#8bc34a' },
  { bg: '#ede7f6', border: '#673ab7' },
]
const colorMap = {}
let colorIdx = 0
const getColor = (id) => {
  if (!colorMap[id]) {
    colorMap[id] = COURSE_COLORS[colorIdx % COURSE_COLORS.length]
    colorIdx++
  }
  return colorMap[id]
}

const parseTime = (timeSlot) => {
  try {
    const [start, end] = timeSlot.split('-')
    const toMin = (t) => { const [h, m] = t.trim().split(':').map(Number); return h * 60 + m }
    return [toMin(start), toMin(end), start.trim(), end.trim()]
  } catch {
    return [480, 570, '8:00', '9:30']
  }
}

// 今日日期信息
const DAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const now = new Date()
const todayLabel = `星期${['日','一','二','三','四','五','六'][now.getDay()]}`
const todayDate = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
const todayWeekday = `周${['日','一','二','三','四','五','六'][now.getDay()]}`

const todayCourses = computed(() => {
  return courses.value
    .filter(c => c.weekday === todayWeekday)
    .map(c => {
      const [startMin, endMin, startTime, endTime] = parseTime(c.timeSlot)
      const durationMin = endMin - startMin
      const duration = durationMin >= 60
        ? `${Math.floor(durationMin / 60)}小时${durationMin % 60 > 0 ? durationMin % 60 + '分钟' : ''}`
        : `${durationMin}分钟`

      const currentMin = now.getHours() * 60 + now.getMinutes()
      const isOngoing = currentMin >= startMin && currentMin < endMin
      const progressPercent = isOngoing
        ? Math.min(100, Math.round(((currentMin - startMin) / durationMin) * 100))
        : 0

      return {
        ...c,
        startTime,
        endTime,
        startMin,
        endMin,
        duration,
        isOngoing,
        progressPercent,
        palette: getColor(c.id)
      }
    })
})

const sortedTodayCourses = computed(() => {
  return [...todayCourses.value].sort((a, b) => a.startMin - b.startMin)
})

const nextCourseIndex = computed(() => {
  const currentMin = now.getHours() * 60 + now.getMinutes()
  let idx = -1
  let minDiff = Infinity
  sortedTodayCourses.value.forEach((c, i) => {
    if (!c.isOngoing && c.startMin > currentMin) {
      const diff = c.startMin - currentMin
      if (diff < minDiff) {
        minDiff = diff
        idx = i
      }
    }
  })
  return idx
})

const dueSoonCount = computed(() => {
  return homeworks.value.filter(h => {
    if (!h.deadline || h.status === 1) return false
    const due = new Date(h.deadline.replace('T', ' '))
    const diff = due - now
    return diff > 0 && diff < 24 * 60 * 60 * 1000
  }).length
})

const unfinishedCount = computed(() => {
  return homeworks.value.filter(h => h.status === 0).length
})

// ========== 渲染图表 ==========
function renderCourseChart(data) {
  if (!courseChartRef.value) return
  if (!courseChart) {
    courseChart = echarts.init(courseChartRef.value)
  }
  const weekdays = Object.keys(data)
  const values = Object.values(data)

  courseChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: weekdays,
      axisLabel: { fontSize: 12 },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { fontSize: 12 }
    },
    series: [{
      name: '课程数',
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#79bbff' }
        ]),
        borderRadius: [6, 6, 0, 0]
      },
      barWidth: '50%',
      label: {
        show: true,
        position: 'top',
        fontSize: 13,
        fontWeight: 'bold',
        color: '#333'
      }
    }]
  })
}

function renderProgressChart(data) {
  if (!progressChartRef.value) return
  if (!progressChart) {
    progressChart = echarts.init(progressChartRef.value)
  }

  const colors = ['#67c23a', '#e6a23c', '#f56c6c']
  const total = data.reduce((sum, item) => sum + item.value, 0)

  progressChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const pct = total > 0 ? ((p.value / total) * 100).toFixed(1) : 0
        return `${p.name}: ${p.value} (${pct}%)`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { fontSize: 13 }
    },
    series: [{
      name: '学习进度',
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        position: 'inside',
        formatter: (p) => {
          const pct = total > 0 ? ((p.value / total) * 100).toFixed(1) : 0
          return `${pct}%`
        },
        fontSize: 13,
        fontWeight: 'bold'
      },
      emphasis: {
        label: { fontSize: 18, fontWeight: 'bold' }
      },
      data: data.map((item, i) => ({
        ...item,
        itemStyle: { color: colors[i] }
      }))
    }]
  })
}

// 获取统计数据
async function loadStatistics() {
  try {
    const res = await axios.get('/api/statistics')
    const data = res.data
    stats.value = {
      totalCourses: data.totalCourses || 0,
      totalKnowledgePoints: data.totalKnowledgePoints || 0,
      mastered: data.mastered || 0,
      learning: data.learning || 0,
      notStarted: data.notStarted || 0
    }

    await nextTick()
    renderCourseChart(data.courseDistribution || {})
    renderProgressChart(data.learningProgress || [])
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

// 窗口大小变化时重绘图表
function handleResize() {
  courseChart?.resize()
  progressChart?.resize()
}

onMounted(async () => {
  try {
    const [courseRes, hwRes] = await Promise.all([
      axios.get('/api/course'),
      axios.get('/api/homework')
    ])
    courses.value = courseRes.data
    homeworks.value = hwRes.data
  } catch (e) {
    // ignore
  }

  await loadStatistics()
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ========== 顶部横幅 ========== */
.dash-banner {
  background: linear-gradient(135deg, #409eff 0%, #2d6cdf 100%);
  border-radius: 12px;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.dash-day-label {
  font-size: 28px;
  font-weight: 700;
}

.dash-date {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 4px;
}

.dash-summary {
  display: flex;
  gap: 24px;
}

.dash-stat {
  text-align: center;
  background: rgba(255,255,255,0.15);
  border-radius: 10px;
  padding: 12px 20px;
  min-width: 80px;
}

.dash-stat-num {
  display: block;
  font-size: 26px;
  font-weight: 700;
}

.dash-stat-label {
  font-size: 12px;
  opacity: 0.85;
}

/* ========== 统计卡片 ========== */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.stats-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 12px;
  color: #fff;
  transition: transform 0.15s, box-shadow 0.15s;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.stats-card.courses {
  background: linear-gradient(135deg, #409eff, #2d6cdf);
}

.stats-card.points {
  background: linear-gradient(135deg, #e6a23c, #d48806);
}

.stats-card.mastered {
  background: linear-gradient(135deg, #67c23a, #3e8e41);
}

.stats-card.tolearn {
  background: linear-gradient(135deg, #f56c6c, #c62828);
}

.stats-card-icon {
  font-size: 36px;
  flex-shrink: 0;
}

.stats-card-body {
  display: flex;
  flex-direction: column;
}

.stats-card-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stats-card-label {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 2px;
}

/* ========== 图表区 ========== */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.chart-panel {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.chart-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #333;
}

.chart-box {
  width: 100%;
  height: 280px;
}

/* ========== section ========== */
.dash-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.dash-section-title {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
  flex-shrink: 0;
}

/* ========== 空状态 ========== */
.dash-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.dash-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

/* ========== 今日课程时间线 ========== */
.today-timeline {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.today-card-wrapper {
  position: relative;
}

.timeline-connector {
  width: 2px;
  height: 16px;
  background: #d0d7de;
  margin-left: 60px;
}

.today-card {
  display: flex;
  align-items: stretch;
  border-radius: 10px;
  border-left: 5px solid;
  padding: 16px 20px;
  transition: transform 0.15s, box-shadow 0.15s;
  position: relative;
}

.today-card:hover {
  transform: translateX(4px);
  box-shadow: 0 2px 10px rgba(0,0,0,0.08);
}

.today-card.is-now {
  box-shadow: 0 0 0 3px rgba(229, 57, 53, 0.25);
}

.today-card.is-next {
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.2);
}

.today-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 80px;
  padding-right: 16px;
  border-right: 1px dashed #e0e0e0;
  margin-right: 16px;
}

.time-start,
.time-end {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}

.time-divider {
  width: 20px;
  height: 2px;
  background: #d0d0d0;
  margin: 6px 0;
}

.today-info {
  flex: 1;
  min-width: 0;
}

.today-name {
  font-size: 17px;
  font-weight: 700;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ongoing-tag {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.today-details {
  margin-top: 6px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.today-detail {
  font-size: 13px;
  color: #777;
}

.today-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #409eff);
  border-radius: 3px;
  transition: width 1s;
}

.progress-text {
  font-size: 12px;
  color: #888;
  min-width: 36px;
}
</style>
