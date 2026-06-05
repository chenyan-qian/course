<template>
  <div class="kp-container">
    <!-- 操作区 -->
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">+ 添加知识点</el-button>
      <el-button type="danger" :disabled="selectedIds.size === 0"
        @click="batchDel">批量删除 ({{ selectedIds.size }})</el-button>

      <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable style="width: 200px; margin-left: 10px"
        @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <!-- 知识点卡片列表 -->
    <div v-if="list.length === 0" style="text-align: center; color: #999; padding: 60px 0;">
      暂无知识点，点击上方按钮添加
    </div>

    <div class="kp-list">
      <div v-for="item in list" :key="item.id" class="kp-card" :class="{ selected: selectedIds.has(item.id) }">
        <div class="kp-header">
          <el-checkbox :model-value="selectedIds.has(item.id)"
            @change="(val) => toggleSelect(item.id, val)" style="margin-right: 10px;" />
          <h3>{{ item.title }}</h3>
          <div class="kp-actions">
            <el-button size="small" @click="openDialog(item)">编辑</el-button>
            <el-button size="small" type="danger" @click="del(item.id)">删除</el-button>
          </div>
        </div>
        <div class="kp-meta">
          <el-tag v-if="item.tags" v-for="tag in (item.tags || '').split(',')" :key="tag" size="small"
            style="margin-right: 4px;">
            {{ tag.trim() }}
          </el-tag>
          <span style="margin-left: 8px; color: #999; font-size: 13px;">
            关联课程：{{ getCourseName(item.courseId) }}
          </span>
          <span style="margin-left: 12px; color: #bbb; font-size: 12px;">
            {{ item.updateTime || item.createTime }}
          </span>
        </div>
        <div class="kp-content" v-if="item.content">
          {{ item.content.substring(0, 200) }}{{ item.content.length > 200 ? '...' : '' }}
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识点' : '添加知识点'" width="600px" top="40px">
      <el-form label-width="80px" :model="form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="知识点标题" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择课程（可选）" clearable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：重点,必考,公式" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="知识点详细内容..." />
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
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const courses = ref([])
const filterCourseId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedIds = ref(new Set())

// 切换选中
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
      `确定删除选中的 ${selectedIds.value.size} 个知识点吗？`,
      '批量删除',
      { type: 'warning' }
    )
    await axios.delete('/api/knowledge-point/batch', { data: [...selectedIds.value] })
    ElMessage.success('批量删除成功')
    selectedIds.value = new Set()
    load()
  } catch (e) {
    // 取消
  }
}

const form = ref({
  id: null,
  title: '',
  courseId: null,
  tags: '',
  content: ''
})

// 加载知识点列表
const load = async () => {
  const params = {}
  if (filterCourseId.value) params.courseId = filterCourseId.value
  const res = await axios.get('/api/knowledge-point', { params })
  list.value = res.data
}

// 加载课程列表（用于下拉选择）
const loadCourses = async () => {
  const res = await axios.get('/api/course')
  courses.value = res.data
}

// 根据 courseId 取课程名
const getCourseName = (courseId) => {
  const c = courses.value.find(c => c.id === courseId)
  return c ? c.name : '未关联'
}

// 打开弹窗
const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    form.value = {
      id: row.id,
      title: row.title,
      courseId: row.courseId,
      tags: row.tags || '',
      content: row.content || ''
    }
  } else {
    isEdit.value = false
    form.value = { id: null, title: '', courseId: null, tags: '', content: '' }
  }
  dialogVisible.value = true
}

// 保存
const save = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  const data = {
    id: form.value.id,
    title: form.value.title,
    courseId: form.value.courseId,
    tags: form.value.tags,
    content: form.value.content
  }
  if (isEdit.value) {
    await axios.put('/api/knowledge-point', data)
    ElMessage.success('修改成功')
  } else {
    await axios.post('/api/knowledge-point', data)
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
  load()
}

// 删除
const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该知识点吗？', '提示', { type: 'warning' })
    await axios.delete(`/api/knowledge-point/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 取消删除
  }
}

onMounted(() => {
  loadCourses()
  load()
})
</script>

<style scoped>
.kp-container {
  /* 由父组件控制 */
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.kp-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kp-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px 20px;
  background: #fff;
  transition: box-shadow 0.2s;
}

.kp-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.kp-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.kp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kp-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.kp-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}

.kp-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.kp-content {
  margin-top: 10px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  white-space: pre-wrap;
}
</style>
