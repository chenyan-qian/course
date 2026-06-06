<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <div class="header">
      <h1>📚 个人课程管理系统</h1>
      <div>
        <el-button type="warning" @click="openPwdDialog">修改密码</el-button>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </div>

    <!-- 主体：左侧导航 + 右侧内容 -->
    <div class="main-body">
      <!-- 左侧导航 -->
      <div class="sidebar">
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'dashboard' }"
          @click="activeTab = 'dashboard'"
        >
          <span class="sidebar-icon">🏠</span>
          <span class="sidebar-text">首页</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'course' }"
          @click="activeTab = 'course'"
        >
          <span class="sidebar-icon">📖</span>
          <span class="sidebar-text">课程管理</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'kp' }"
          @click="activeTab = 'kp'"
        >
          <span class="sidebar-icon">📝</span>
          <span class="sidebar-text">知识点管理</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'hw' }"
          @click="activeTab = 'hw'"
        >
          <span class="sidebar-icon">📋</span>
          <span class="sidebar-text">作业管理</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeTab === 'exam' }"
          @click="activeTab = 'exam'"
        >
          <span class="sidebar-icon">📝</span>
          <span class="sidebar-text">考试管理</span>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="content">
        <!-- ==================== 首页 ==================== -->
        <div v-show="activeTab === 'dashboard'" class="content-page content-page-dashboard">
          <Dashboard />
        </div>

        <!-- ==================== 课程管理 ==================== -->
        <div v-show="activeTab === 'course'">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="openCourseDialog()">+ 添加课程</el-button>
              <el-button type="danger" :disabled="selectedCourses.length === 0"
                @click="batchDelCourse">批量删除 ({{ selectedCourses.length }})</el-button>
            </div>
            <div class="toolbar-right">
              <el-radio-group v-model="viewMode" size="small">
                <el-radio-button value="table">📋 列表</el-radio-button>
                <el-radio-button value="timetable">📅 课表</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- ========== 列表视图 ========== -->
          <div v-show="viewMode === 'table'">
            <el-table :data="courses" border stripe style="width: 100%"
              @selection-change="onCourseSelectionChange">
              <el-table-column type="selection" width="45" />
              <el-table-column prop="name" label="课程名称" />
              <el-table-column prop="teacher" label="授课教师" />
              <el-table-column prop="classroom" label="教室" />
              <el-table-column prop="weekday" label="星期" />
              <el-table-column prop="timeSlot" label="时间" />
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button size="small" @click="openCourseDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="delCourse(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- ========== 课表视图 ========== -->
          <div v-show="viewMode === 'timetable'" class="timetable-wrapper">
            <div class="timetable-grid" v-if="timetableCourses.length > 0">
              <!-- 顶部星期头 -->
              <div class="timetable-corner">时间</div>
              <div class="timetable-day-header" v-for="day in DAYS" :key="day">
                {{ day }}
              </div>
              <!-- 时间标签列（左） + 课程卡片区 -->
              <template v-for="(time, idx) in TIME_SLOTS" :key="idx">
                <div
                  class="timetable-time-label"
                  v-if="idx % 2 === 0"
                  :style="{ gridRow: idx + 2 }"
                >
                  {{ time }}
                </div>
                <div
                  class="timetable-cell"
                  :class="{ 'timetable-cell-odd': Math.floor(idx / 2) % 2 === 0 }"
                  :style="{ gridRow: idx + 2, gridColumn: dayIdx + 2 }"
                  v-for="dayIdx in 7"
                  :key="dayIdx"
                ></div>
              </template>
              <!-- 课程卡片（覆盖在格子上方） -->
              <div
                v-for="c in timetableCourses"
                :key="c.id"
                class="timetable-course"
                :style="{
                  gridRow: (c.startRow) + ' / span ' + c.span,
                  gridColumn: (c.dayIndex + 2),
                  backgroundColor: c.color,
                  borderLeftColor: c.borderColor
                }"
                @click="openCourseDialog(c)"
                :title="`${c.name}\n教师：${c.teacher}\n教室：${c.classroom}\n${c.weekday} ${c.timeSlot}`"
              >
                <div class="tc-name">{{ c.name }}</div>
                <div class="tc-room" v-if="c.classroom">🏫 {{ c.classroom }}</div>
                <div class="tc-teacher" v-if="c.teacher">👨‍🏫 {{ c.teacher }}</div>
                <div class="tc-time">{{ c.timeSlot }}</div>
              </div>
            </div>
            <div v-else class="timetable-empty">
              <p>暂无课程，点击 "+ 添加课程" 开始吧 📚</p>
            </div>
          </div>

          <!-- 课程弹窗 -->
          <el-dialog v-model="courseDialogVisible" :title="isCourseEdit ? '编辑课程' : '添加课程'" width="450px">
            <el-form label-width="80px" :model="courseForm">
              <el-form-item label="课程名称">
                <el-input v-model="courseForm.name" />
              </el-form-item>
              <el-form-item label="授课教师">
                <el-input v-model="courseForm.teacher" />
              </el-form-item>
              <el-form-item label="教室">
                <el-input v-model="courseForm.classroom" />
              </el-form-item>
              <el-form-item label="星期">
                <el-select v-model="courseForm.weekday" style="width: 100%">
                  <el-option v-for="d in ['周一','周二','周三','周四','周五','周六','周日']" :key="d" :label="d" :value="d" />
                </el-select>
              </el-form-item>
              <el-form-item label="时间">
                <el-input v-model="courseForm.timeSlot" placeholder="如：8:00-9:30" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="courseDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="saveCourse">保存</el-button>
            </template>
          </el-dialog>
        </div>

        <!-- ==================== 知识点管理 ==================== -->
        <div v-show="activeTab === 'kp'">
          <KnowledgePoint />
        </div>

        <!-- ==================== 作业管理 ==================== -->
        <div v-show="activeTab === 'hw'">
          <Homework />
        </div>

        <!-- ==================== 考试管理 ==================== -->
        <div v-show="activeTab === 'exam'">
          <Exam />
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px">
      <el-form label-width="80px" :model="pwdForm">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import KnowledgePoint from './KnowledgePoint.vue'
import Dashboard from './Dashboard.vue'
import Homework from './Homework.vue'
import Exam from './Exam.vue'

const router = useRouter()
const activeTab = ref('dashboard')

// ========== 修改密码 ==========
const pwdDialogVisible = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const openPwdDialog = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

const changePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  try {
    const res = await axios.put('/api/user/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.data.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdDialogVisible.value = false
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('请求失败')
  }
}

// ========== 课程管理 ==========
const viewMode = ref('table') // 'table' | 'timetable'
const DAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 生成 30 分钟一档的时间槽（8:00 ~ 22:00）
const TIME_SLOTS = (() => {
  const slots = []
  for (let h = 8; h < 22; h++) {
    for (let m = 0; m < 60; m += 30) {
      slots.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }
  return slots
})()

// 课程色板
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
let colorIdx = 0
const colorMap = {}
const getCourseColor = (id) => {
  if (!colorMap[id]) {
    colorMap[id] = COURSE_COLORS[colorIdx % COURSE_COLORS.length]
    colorIdx++
  }
  return colorMap[id]
}

// 将 timeSlot "8:00-9:30" 转为以 08:00 为起点的分钟数
const parseTimeToMinutes = (timeSlot) => {
  try {
    const [start, end] = timeSlot.split('-')
    const toMin = (t) => {
      const [h, m] = t.trim().split(':').map(Number)
      return h * 60 + m
    }
    return [toMin(start), toMin(end)]
  } catch {
    return [480, 570] // fallback
  }
}

// 课表数据：把每个课程定位到 grid 坐标
const timetableCourses = computed(() => {
  const BASE = 480 // 08:00 = 480分钟
  return courses.value.map(c => {
    const [startMin, endMin] = parseTimeToMinutes(c.timeSlot)
    const dayIndex = DAYS.indexOf(c.weekday)
    // 每 30 分钟一个格子，+2 是因为第 1 行是 header
    const startRow = Math.round((startMin - BASE) / 30) + 2
    const span = Math.max(1, Math.round((endMin - startMin) / 30))
    const palette = getCourseColor(c.id)
    return {
      ...c,
      dayIndex,
      startRow,
      span,
      color: palette.bg,
      borderColor: palette.border
    }
  })
})

const courses = ref([])
const courseDialogVisible = ref(false)
const isCourseEdit = ref(false)

const courseForm = ref({
  id: null,
  name: '',
  teacher: '',
  classroom: '',
  weekday: '',
  timeSlot: ''
})

const loadCourses = async () => {
  const res = await axios.get('/api/course')
  courses.value = res.data
}

const openCourseDialog = (row) => {
  if (row) {
    isCourseEdit.value = true
    courseForm.value = { ...row }
  } else {
    isCourseEdit.value = false
    courseForm.value = { id: null, name: '', teacher: '', classroom: '', weekday: '', timeSlot: '' }
  }
  courseDialogVisible.value = true
}

const saveCourse = async () => {
  try {
    if (isCourseEdit.value) {
      const res = await axios.put('/api/course', courseForm.value)
      if (res.data.code === 200) {
        ElMessage.success('修改成功')
      } else {
        ElMessage.error(res.data.msg)
        return
      }
    } else {
      const res = await axios.post('/api/course', courseForm.value)
      if (res.data.code === 200) {
        ElMessage.success('添加成功')
      } else {
        ElMessage.error(res.data.msg)
        return
      }
    }
    courseDialogVisible.value = false
    loadCourses()
  } catch (e) {
    const msg = e.response?.data?.msg || '请求失败'
    ElMessage.error(msg)
  }
}

const delCourse = async (id) => {
  if (!confirm('确定删除该课程吗？')) return
  await axios.delete(`/api/course/${id}`)
  ElMessage.success('删除成功')
  loadCourses()
}

// 批量删除课程
const selectedCourses = ref([])
const onCourseSelectionChange = (rows) => {
  selectedCourses.value = rows
}
const batchDelCourse = async () => {
  if (selectedCourses.value.length === 0) return
  if (!confirm(`确定删除选中的 ${selectedCourses.value.length} 门课程吗？`)) return
  const ids = selectedCourses.value.map(r => r.id)
  await axios.delete('/api/course/batch', { data: ids })
  ElMessage.success('批量删除成功')
  loadCourses()
}

// ========== 退出 ==========
const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.home-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0;
}

/* ========== 顶部导航 ========== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.header h1 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

/* ========== 主体布局 ========== */
.main-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ========== 左侧导航 ========== */
.sidebar {
  width: 200px;
  background: #304156;
  flex-shrink: 0;
  padding-top: 8px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  color: #bfcbd9;
  transition: all 0.2s;
  font-size: 15px;
}

.sidebar-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}

.sidebar-item.active {
  color: #fff;
  background: #409eff;
}

.sidebar-icon {
  margin-right: 10px;
  font-size: 18px;
}

.sidebar-text {
  white-space: nowrap;
}

/* ========== 右侧内容区 ========== */
.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f0f2f5;
}

.content-page-dashboard {
  height: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 课表视图 ========== */
.timetable-wrapper {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  overflow-x: auto;
  padding: 16px;
}

.timetable-grid {
  display: grid;
  grid-template-columns: 72px repeat(7, 1fr);
  grid-template-rows: 40px repeat(28, 28px);
  min-width: 800px;
  position: relative;
}

.timetable-corner {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #999;
  border-bottom: 2px solid #e0e0e0;
  background: #fafafa;
  font-weight: 600;
}

.timetable-day-header {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #555;
  border-bottom: 2px solid #e0e0e0;
  background: #fafafa;
}

/* 周六/周日不同色 */
.timetable-day-header:nth-child(7),
.timetable-day-header:nth-child(8) {
  color: #e57373;
}

.timetable-time-label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  font-size: 11px;
  color: #aaa;
  padding-top: 2px;
  border-right: 1px solid #f0f0f0;
}

.timetable-cell {
  border-bottom: 1px solid #f5f5f5;
  border-right: 1px solid #f0f0f0;
}

.timetable-cell-odd {
  border-bottom: 1px solid #e8e8e8;
}

/* 课程卡片 */
.timetable-course {
  border-radius: 6px;
  padding: 4px 6px;
  margin: 1px 3px;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.15s, box-shadow 0.15s;
  border-left: 4px solid;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.timetable-course:hover {
  transform: scale(1.03);
  box-shadow: 0 4px 12px rgba(0,0,0,0.18);
  z-index: 10;
}

.tc-name {
  font-size: 13px;
  font-weight: 700;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-room,
.tc-teacher,
.tc-time {
  font-size: 10px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.timetable-empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 16px;
}
</style>
