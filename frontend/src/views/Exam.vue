<template>
  <div class="exam-container">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="openDialog()">+ 添加考试</el-button>
        <el-button type="danger" :disabled="selectedIds.size === 0"
          @click="batchDel">批量删除 ({{ selectedIds.size }})</el-button>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="filterType" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="upcoming">未开始</el-radio-button>
        </el-radio-group>
        <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable style="width: 180px; margin-left: 8px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </div>

    <!-- 倒计时横幅（最近一场考试） -->
    <div v-if="nextExam" class="next-exam-banner">
      <div class="next-exam-icon">📅</div>
      <div class="next-exam-info">
        <div class="next-exam-label">最近考试</div>
        <div class="next-exam-name">{{ nextExam.name }}</div>
        <div class="next-exam-meta" v-if="nextExam.courseName || nextExam.location">
          {{ nextExam.courseName }} {{ nextExam.location ? '· '+nextExam.location : '' }}
        </div>
      </div>
      <div class="next-exam-countdown">
        <div class="countdown-num">{{ nextExam.countdown }}</div>
        <div class="countdown-unit">{{ nextExam.countdownUnit }}</div>
      </div>
    </div>

    <!-- 考试卡片列表 -->
    <div v-if="filteredList.length === 0" class="empty-state">
      <p>📝 暂无考试，点击上方按钮添加</p>
    </div>

    <div class="exam-grid">
      <div v-for="item in filteredList" :key="item.id" class="exam-card"
        :class="{
          selected: selectedIds.has(item.id),
          'is-over': item.isOver,
          'is-soon': item.isSoon
        }">
        <div class="exam-card-top">
          <el-checkbox :model-value="selectedIds.has(item.id)"
            @change="(val) => toggleSelect(item.id, val)" />
          <span class="exam-course" v-if="getCourseName(item.courseId)">
            📖 {{ getCourseName(item.courseId) }}
          </span>
        </div>

        <h3 class="exam-name" @click="openDialog(item)">{{ item.name }}</h3>

        <div class="exam-details">
          <div class="exam-detail" v-if="item.examDate">
            📅 {{ formatDate(item.examDate) }}
          </div>
          <div class="exam-detail" v-if="item.location">
            📍 {{ item.location }}
          </div>
          <div class="exam-detail" v-if="item.remark">
            📝 {{ item.remark }}
          </div>
        </div>

        <!-- 倒计时 -->
        <div class="exam-countdown" v-if="item.examDate">
          <div class="cd-block" :class="item.isOver ? 'over' : (item.isSoon ? 'soon' : 'normal')">
            <span class="cd-num">{{ item.countdown }}</span>
            <span class="cd-unit">{{ item.countdownUnit }}</span>
          </div>
          <div class="cd-label">
            {{ item.isOver ? '已结束' : '距离考试' }}
          </div>
        </div>

        <!-- 操作 -->
        <div class="exam-actions">
          <el-button size="small" @click="openDialog(item)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑考试' : '添加考试'" width="500px" top="40px">
      <el-form label-width="80px" :model="form">
        <el-form-item label="考试名称">
          <el-input v-model="form.name" placeholder="如：数据库期末考试" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择课程" clearable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时间">
          <el-date-picker v-model="form.examDate" type="datetime"
            placeholder="选择日期时间" format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考试地点">
          <el-input v-model="form.location" placeholder="如：A301" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3"
            placeholder="座位号、注意事项等..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const courses = ref([])
const filterCourseId = ref(null)
const filterType = ref('all')
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedIds = ref(new Set())

const now = new Date()

// 格式化日期
const formatDate = (d) => {
  if (!d) return ''
  return d.replace('T', ' ').substring(0, 16)
}

// 计算倒计时
const calcCountdown = (examDate) => {
  if (!examDate) return { countdown: '-', countdownUnit: '', isOver: false, isSoon: false }
  const due = new Date(examDate.replace('T', ' '))
  const diff = due - now
  const absDiff = Math.abs(diff)
  const days = Math.floor(absDiff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((absDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))

  if (diff < 0) {
    return { countdown: days > 0 ? `${days}` : hours, countdownUnit: days > 0 ? '天前' : '小时前', isOver: true, isSoon: false }
  }
  if (days === 0) {
    return { countdown: hours, countdownUnit: '小时', isOver: false, isSoon: true }
  }
  return { countdown: days, countdownUnit: '天', isOver: false, isSoon: days <= 3 }
}

// 增强的列表（带倒计时）
const enrichedList = computed(() => {
  return list.value.map(item => ({
    ...item,
    ...calcCountdown(item.examDate),
    courseName: getCourseName(item.courseId)
  })).sort((a, b) => {
    if (!a.examDate) return 1
    if (!b.examDate) return -1
    return new Date(a.examDate.replace('T',' ')) - new Date(b.examDate.replace('T',' '))
  })
})

// 筛选
const filteredList = computed(() => {
  let result = enrichedList.value
  if (filterCourseId.value) {
    result = result.filter(e => e.courseId === filterCourseId.value)
  }
  if (filterType.value === 'upcoming') {
    result = result.filter(e => !e.isOver)
  }
  return result
})

// 最近一场未结束的考试
const nextExam = computed(() => {
  return enrichedList.value.find(e => !e.isOver) || null
})

const getCourseName = (id) => {
  if (!id) return ''
  const c = courses.value.find(c => c.id === id)
  return c ? c.name : ''
}

const load = async () => {
  try {
    const res = await axios.get('/api/exam')
    list.value = res.data
  } catch (e) { list.value = [] }
}

const loadCourses = async () => {
  try {
    const res = await axios.get('/api/course')
    courses.value = res.data
  } catch (e) { courses.value = [] }
}

const toggleSelect = (id, checked) => {
  if (checked) {
    selectedIds.value = new Set([...selectedIds.value, id])
  } else {
    const next = new Set(selectedIds.value)
    next.delete(id)
    selectedIds.value = next
  }
}

const batchDel = async () => {
  if (selectedIds.value.size === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.size} 场考试吗？`, '批量删除', { type: 'warning' })
    await axios.delete('/api/exam/batch', { data: [...selectedIds.value] })
    ElMessage.success('批量删除成功')
    selectedIds.value = new Set()
    load()
  } catch (e) { /* cancel */ }
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该考试吗？', '提示', { type: 'warning' })
    await axios.delete(`/api/exam/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e) { /* cancel */ }
}

const form = ref({
  id: null, name: '', courseId: null, examDate: '', location: '', remark: ''
})

const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    form.value = {
      id: row.id, name: row.name, courseId: row.courseId,
      examDate: row.examDate ? row.examDate.replace('T',' ').substring(0,19) : '',
      location: row.location || '', remark: row.remark || ''
    }
  } else {
    isEdit.value = false
    form.value = { id: null, name: '', courseId: null, examDate: '', location: '', remark: '' }
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name) { ElMessage.warning('请输入考试名称'); return }
  const data = { ...form.value, examDate: form.value.examDate || null }
  try {
    if (isEdit.value) {
      const res = await axios.put('/api/exam', data)
      if (res.data.code === 200) ElMessage.success('修改成功')
      else { ElMessage.error(res.data.msg); return }
    } else {
      const res = await axios.post('/api/exam', data)
      if (res.data.code === 200) ElMessage.success('添加成功')
      else { ElMessage.error(res.data.msg); return }
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '请求失败')
  }
}

onMounted(() => { loadCourses(); load() })
</script>

<style scoped>
.exam-container { /* 由父容器控制 */ }

.toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.toolbar-right { display: flex; align-items: center; }

.empty-state {
  text-align: center; color: #999; padding: 60px 0; font-size: 16px;
}

/* ========== 倒计时横幅 ========== */
.next-exam-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; padding: 24px 28px;
  display: flex; align-items: center; gap: 20px;
  color: #fff; margin-bottom: 20px;
}
.next-exam-icon { font-size: 40px; }
.next-exam-info { flex: 1; }
.next-exam-label { font-size: 13px; opacity: 0.8; }
.next-exam-name { font-size: 20px; font-weight: 700; margin-top: 2px; }
.next-exam-meta { font-size: 13px; opacity: 0.75; margin-top: 4px; }
.next-exam-countdown {
  text-align: center; background: rgba(255,255,255,0.2);
  border-radius: 12px; padding: 14px 24px; min-width: 80px;
}
.countdown-num { font-size: 36px; font-weight: 700; line-height: 1; }
.countdown-unit { font-size: 13px; opacity: 0.85; margin-top: 2px; }

/* ========== 卡片网格 ========== */
.exam-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.exam-card {
  background: #fff; border-radius: 10px; padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #eee;
  transition: box-shadow 0.2s; display: flex; flex-direction: column;
}
.exam-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.exam-card.selected { border-color: #409eff; background: #ecf5ff; }
.exam-card.is-over { opacity: 0.6; }
.exam-card.is-soon { border-left: 4px solid #e6a23c; }

.exam-card-top {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}
.exam-course { font-size: 12px; color: #888; }

.exam-name {
  margin: 0 0 10px; font-size: 16px; color: #333;
  cursor: pointer; transition: color 0.15s;
}
.exam-name:hover { color: #409eff; }

.exam-details {
  display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px;
}
.exam-detail { font-size: 13px; color: #666; }

/* 倒计时块 */
.exam-countdown {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: #f8f9fa; border-radius: 8px;
  margin-bottom: 12px;
}
.cd-block {
  display: flex; flex-direction: column; align-items: center;
  min-width: 56px; padding: 6px 12px; border-radius: 8px;
}
.cd-block.normal { background: #e8f4fd; }
.cd-block.soon { background: #fef0e6; }
.cd-block.over { background: #f0f0f0; }
.cd-num { font-size: 24px; font-weight: 700; color: #333; line-height: 1; }
.cd-block.soon .cd-num { color: #e6a23c; }
.cd-block.over .cd-num { color: #999; }
.cd-unit { font-size: 11px; color: #888; margin-top: 2px; }
.cd-label { font-size: 12px; color: #999; }

.exam-actions {
  display: flex; gap: 6px; justify-content: flex-end;
}
</style>
