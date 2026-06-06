<template>
  <div class="hw-container">
    <!-- 操作区 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="openDialog()">+ 添加作业</el-button>
        <el-button type="danger" :disabled="selectedIds.size === 0"
          @click="batchDel">批量删除 ({{ selectedIds.size }})</el-button>
      </div>
      <div class="toolbar-right">
        <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable style="width: 180px"
          @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </div>

    <!-- 快捷筛选标签 -->
    <div class="filter-tabs">
      <div class="filter-tab" :class="{ active: filterTab === 'all' }" @click="switchTab('all')">
        📋 全部 <span class="count">{{ stats.all }}</span>
      </div>
      <div class="filter-tab" :class="{ active: filterTab === 'unfinished' }" @click="switchTab('unfinished')">
        📝 未完成 <span class="count">{{ stats.unfinished }}</span>
      </div>
      <div class="filter-tab urgent" :class="{ active: filterTab === 'dueSoon' }" @click="switchTab('dueSoon')">
        ⏰ 即将截止 <span class="count">{{ stats.dueSoon }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">{{ stats.all }}</div>
        <div class="stat-label">全部作业</div>
      </div>
      <div class="stat-card unfinished">
        <div class="stat-num">{{ stats.unfinished }}</div>
        <div class="stat-label">未完成</div>
      </div>
      <div class="stat-card done">
        <div class="stat-num">{{ stats.finished }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card urgent">
        <div class="stat-num">{{ stats.dueSoon }}</div>
        <div class="stat-label">即将截止</div>
      </div>
    </div>

    <!-- 作业列表 -->
    <div v-if="list.length === 0" class="empty-state">
      <p>🎉 暂无相关作业</p>
    </div>

    <div class="hw-list">
      <div v-for="item in list" :key="item.id"
        class="hw-card"
        :class="{
          selected: selectedIds.has(item.id),
          done: item.status === 1,
          'due-soon': isDueSoon(item) && item.status === 0
        }">
        <div class="hw-left">
          <el-checkbox :model-value="selectedIds.has(item.id)"
            @change="(val) => toggleSelect(item.id, val)" style="margin-right: 12px;" />
        </div>
        <div class="hw-body" @click="openDialog(item)">
          <div class="hw-title-row">
            <h3 :class="{ 'text-done': item.status === 1 }">{{ item.title }}</h3>
            <el-tag v-if="item.status === 1" type="success" size="small">✓ 已完成</el-tag>
            <el-tag v-else-if="isDueSoon(item)" type="danger" size="small" effect="dark">
              ⚡ 即将截止
            </el-tag>
            <el-tag v-else type="warning" size="small">进行中</el-tag>
          </div>
          <div class="hw-meta">
            <span class="hw-course" v-if="getCourseName(item.courseId)">
              📖 {{ getCourseName(item.courseId) }}
            </span>
            <span class="hw-deadline" :class="{ 'text-danger': isDueSoon(item) && item.status === 0 }">
              📅 截止：{{ formatDeadline(item.deadline) }}
              <span v-if="item.status === 0" class="countdown">
                ({{ getCountdown(item.deadline) }})
              </span>
            </span>
          </div>
          <div class="hw-desc" v-if="item.description">
            {{ item.description.substring(0, 150) }}{{ item.description.length > 150 ? '...' : '' }}
          </div>
        </div>
        <div class="hw-actions">
          <el-button size="small"
            :type="item.status === 1 ? 'warning' : 'success'"
            @click.stop="toggleStatus(item)">
            {{ item.status === 1 ? '↩ 标记未完成' : '✓ 标记完成' }}
          </el-button>
          <el-button size="small" @click.stop="openDialog(item)">编辑</el-button>
          <el-button size="small" type="danger" @click.stop="del(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业' : '添加作业'" width="550px" top="40px">
      <el-form label-width="80px" :model="form">
        <el-form-item label="作业标题">
          <el-input v-model="form.title" placeholder="如：线性代数第三章习题" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择课程" clearable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            placeholder="选择截止日期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="完成状态">
          <el-switch v-model="form.statusBool" active-text="已完成" inactive-text="未完成" />
        </el-form-item>
        <el-form-item label="详情描述">
          <el-input v-model="form.description" type="textarea" :rows="6"
            placeholder="作业详细要求、注意事项等..." />
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
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const courses = ref([])
const filterCourseId = ref(null)
const filterTab = ref('all') // 'all' | 'unfinished' | 'dueSoon'
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedIds = ref(new Set())

// 格式化截止时间
const formatDeadline = (d) => {
  if (!d) return '无截止日期'
  return d.replace('T', ' ').substring(0, 16)
}

// 判断是否即将截止（< 1天）
const isDueSoon = (item) => {
  if (!item.deadline || item.status === 1) return false
  const now = new Date()
  const due = new Date(item.deadline.replace('T', ' '))
  const diff = due - now
  return diff > 0 && diff < 24 * 60 * 60 * 1000
}

// 倒计时文字
const getCountdown = (deadline) => {
  if (!deadline) return ''
  const d = deadline.replace('T', ' ')
  const now = new Date()
  const due = new Date(d)
  const diff = due - now
  if (diff < 0) return '已截止'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  if (hours >= 24) {
    const days = Math.floor(hours / 24)
    return `剩余 ${days} 天 ${hours % 24} 小时`
  }
  return `剩余 ${hours} 小时 ${mins} 分钟`
}

// 统计数据
const stats = computed(() => {
  const all = list.value.length
  const finished = list.value.filter(h => h.status === 1).length
  const unfinished = all - finished
  const dueSoon = list.value.filter(h => isDueSoon(h)).length
  return { all, finished, unfinished, dueSoon }
})

// 切换筛选标签
const switchTab = (tab) => {
  filterTab.value = tab
  load()
}

// 加载作业列表
const load = async () => {
  const params = {}
  if (filterCourseId.value) params.courseId = filterCourseId.value
  if (filterTab.value === 'unfinished') params.status = 0
  if (filterTab.value === 'dueSoon') params.dueSoon = true
  const res = await axios.get('/api/homework', { params })
  list.value = res.data
}

// 加载课程列表
const loadCourses = async () => {
  const res = await axios.get('/api/course')
  courses.value = res.data
}

const getCourseName = (courseId) => {
  const c = courses.value.find(c => c.id === courseId)
  return c ? c.name : ''
}

// 选择
const toggleSelect = (id, checked) => {
  if (checked) {
    selectedIds.value = new Set([...selectedIds.value, id])
  } else {
    const next = new Set(selectedIds.value)
    next.delete(id)
    selectedIds.value = next
  }
}

// 批量删除
const batchDel = async () => {
  if (selectedIds.value.size === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.size} 条作业吗？`,
      '批量删除',
      { type: 'warning' }
    )
    await axios.delete('/api/homework/batch', { data: [...selectedIds.value] })
    ElMessage.success('批量删除成功')
    selectedIds.value = new Set()
    load()
  } catch (e) { /* 取消 */ }
}

// 删除单条
const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该作业吗？', '提示', { type: 'warning' })
    await axios.delete(`/api/homework/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e) { /* 取消 */ }
}

// 表单
const form = ref({
  id: null,
  title: '',
  courseId: null,
  deadline: '',
  statusBool: false,
  description: ''
})

// 打开弹窗
const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    form.value = {
      id: row.id,
      title: row.title,
      courseId: row.courseId,
      deadline: row.deadline ? row.deadline.replace('T', ' ').substring(0, 19) : '',
      statusBool: row.status === 1,
      description: row.description || ''
    }
  } else {
    isEdit.value = false
    form.value = { id: null, title: '', courseId: null, deadline: '', statusBool: false, description: '' }
  }
  dialogVisible.value = true
}

// 保存
const save = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入作业标题')
    return
  }
  const data = {
    id: form.value.id,
    title: form.value.title,
    courseId: form.value.courseId,
    deadline: form.value.deadline || null,
    status: form.value.statusBool ? 1 : 0,
    description: form.value.description
  }
  try {
    if (isEdit.value) {
      const res = await axios.put('/api/homework', data)
      if (res.data.code === 200) {
        ElMessage.success('修改成功')
      } else {
        ElMessage.error(res.data.msg)
        return
      }
    } else {
      const res = await axios.post('/api/homework', data)
      if (res.data.code === 200) {
        ElMessage.success('添加成功')
      } else {
        ElMessage.error(res.data.msg)
        return
      }
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    const msg = e.response?.data?.msg || '请求失败'
    ElMessage.error(msg)
  }
}

// 切换完成状态
const toggleStatus = async (item) => {
  const res = await axios.put(`/api/homework/${item.id}/toggle`)
  if (res.data.code === 200) {
    ElMessage.success(res.data.msg)
    load()
  }
}

onMounted(() => {
  loadCourses()
  load()
})
</script>

<style scoped>
.hw-container {
  /* 由父容器控制 */
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-tab {
  padding: 8px 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  user-select: none;
}

.filter-tab:hover {
  border-color: #409eff;
  color: #409eff;
}

.filter-tab.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.filter-tab.urgent.active {
  background: #e53935;
  border-color: #e53935;
}

.filter-tab .count {
  display: inline-block;
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  border-radius: 10px;
  margin-left: 6px;
  font-size: 12px;
  text-align: center;
  background: rgba(0,0,0,0.1);
  padding: 0 6px;
}

.filter-tab.active .count {
  background: rgba(255,255,255,0.25);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-card.unfinished .stat-num { color: #e6a23c; }
.stat-card.done .stat-num { color: #67c23a; }
.stat-card.urgent .stat-num { color: #e53935; }

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 16px;
}

/* 作业列表 */
.hw-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hw-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  padding: 14px 18px;
  display: flex;
  align-items: flex-start;
  transition: box-shadow 0.2s;
}

.hw-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.hw-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.hw-card.done {
  opacity: 0.7;
}

.hw-card.due-soon {
  border-left: 4px solid #e53935;
  background: #fff5f5;
}

.hw-left {
  padding-top: 2px;
}

.hw-body {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.hw-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hw-title-row h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.text-done {
  text-decoration: line-through;
  color: #999 !important;
}

.hw-meta {
  margin-top: 6px;
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #888;
  flex-wrap: wrap;
}

.hw-deadline.text-danger {
  color: #e53935;
  font-weight: 600;
}

.countdown {
  font-size: 12px;
  color: #e53935;
}

.hw-desc {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  padding: 8px 10px;
  background: #fafafa;
  border-radius: 4px;
}

.hw-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
  align-items: center;
}
</style>
