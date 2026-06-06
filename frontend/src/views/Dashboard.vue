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

    <!-- 今日课程区域 -->
    <div class="dash-section">
      <h2 class="dash-section-title">📖 今日课程</h2>

      <div v-if="todayCourses.length === 0" class="dash-empty">
        <div class="dash-empty-icon">🎉</div>
        <p>今天没有课程安排，享受自由时光吧~</p>
      </div>

      <div v-else class="today-timeline">
        <div v-for="(c, idx) in sortedTodayCourses" :key="c.id" class="today-card-wrapper">
          <!-- 时间线连接 -->
          <div class="timeline-connector" v-if="idx < sortedTodayCourses.length - 1"></div>

          <div class="today-card"
            :class="{ 'is-now': c.isOngoing, 'is-next': idx === nextCourseIndex }"
            :style="{ borderLeftColor: c.palette.border, background: c.palette.bg }">
            <!-- 时间标签 -->
            <div class="today-time">
              <div class="time-start">{{ c.startTime }}</div>
              <div class="time-divider"></div>
              <div class="time-end">{{ c.endTime }}</div>
            </div>
            <!-- 课程信息 -->
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
            <!-- 进度条 -->
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
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const courses = ref([])
const homeworks = ref([])

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

// 今日课程
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

// 按开始时间排序
const sortedTodayCourses = computed(() => {
  return [...todayCourses.value].sort((a, b) => a.startMin - b.startMin)
})

// 找到 "即将开始" 的课程索引（当前时间之后、最近的一节课）
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

// 即将截止作业数
const dueSoonCount = computed(() => {
  return homeworks.value.filter(h => {
    if (!h.deadline || h.status === 1) return false
    const due = new Date(h.deadline.replace('T', ' '))
    const diff = due - now
    return diff > 0 && diff < 24 * 60 * 60 * 1000
  }).length
})

// 未完成作业数
const unfinishedCount = computed(() => {
  return homeworks.value.filter(h => h.status === 0).length
})

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
})
</script>

<style scoped>
.dashboard {
  height: 100%;
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

/* ========== section ========== */
.dash-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
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

/* 时间列 */
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

/* 课程信息 */
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

/* 进度条 */
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
