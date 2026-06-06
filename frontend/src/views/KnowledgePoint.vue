<template>
  <div class="kp-container">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-dropdown @command="openDialogByType">
          <el-button type="primary">
            + 添加 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="chapter">📁 添加章节</el-dropdown-item>
              <el-dropdown-item command="kp">📄 添加知识点</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="toolbar-right">
        <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable style="width: 200px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </div>

    <!-- 课程树 -->
    <div v-if="filteredTree.length === 0" class="empty-state">
      <p>📝 暂无知识点，点击上方按钮添加</p>
    </div>

    <div class="kp-tree">
      <div v-for="node in filteredTree" :key="'c'+node.courseId" class="course-card">
        <!-- 课程头部 -->
        <div class="course-header" @click="node._expanded = !node._expanded">
          <div class="course-expand">
            <span class="expand-icon">{{ node._expanded ? '▼' : '▶' }}</span>
          </div>
          <div class="course-info">
            <div class="course-title">{{ getCourseName(node.courseId) || '未关联课程' }}</div>
            <div class="course-stats">
              <span>📄 {{ node.total || 0 }} 个知识点</span>
              <span>✅ 已掌握 {{ node.mastered || 0 }}/{{ node.total || 0 }}</span>
            </div>
          </div>
          <div class="course-progress-wrapper">
            <div class="progress-bar-sm">
              <div class="progress-fill-sm" :style="{ width: progressPct(node) + '%' }"></div>
            </div>
            <span class="progress-pct">{{ progressPct(node) }}%</span>
          </div>
        </div>

        <!-- 课程展开：章节列表 -->
        <div v-show="node._expanded" class="course-children">
          <div v-if="(!node.children || node.children.length === 0)" class="no-chapter">
            暂无章节，请先添加章节
          </div>

          <div v-for="ch in node.children" :key="'ch'+ch.id" class="chapter-card"
            :class="{ expanded: ch._expanded }">
            <!-- 章节头部 -->
            <div class="chapter-header" @click="ch._expanded = !ch._expanded">
              <span class="expand-icon">{{ ch._expanded ? '▼' : '▶' }}</span>
              <span class="chapter-icon">📁</span>
              <span class="chapter-title">{{ ch.title }}</span>
              <el-tag v-if="ch.total > 0" size="small" :type="ch.mastered === ch.total ? 'success' : 'info'">
                {{ ch.mastered }}/{{ ch.total }}
              </el-tag>
              <div class="ch-progress-wrap" v-if="ch.total > 0">
                <div class="progress-bar-xs">
                  <div class="progress-fill-xs" :style="{ width: progressPct(ch) + '%' }"></div>
                </div>
                <span class="progress-pct-xs">{{ progressPct(ch) }}%</span>
              </div>
              <div class="chapter-actions" @click.stop>
                <el-button size="small" text @click="openAddKpDialog(ch)">+知识点</el-button>
                <el-button size="small" text @click="openDialog(ch)">✎</el-button>
                <el-button size="small" text type="danger" @click="del(ch.id)">✕</el-button>
              </div>
            </div>

            <!-- 章节展开：知识点列表 -->
            <div v-show="ch._expanded" class="chapter-children">
              <div v-if="(!ch.children || ch.children.length === 0)" class="no-kp">
                暂无知识点，点击 "+知识点" 添加
              </div>
              <div v-for="kp in ch.children" :key="'kp'+kp.id" class="kp-item">
                <!-- 状态指示点（可点击切换） -->
                <div class="status-dot" :class="statusClass(kp.status)"
                  :title="statusLabel(kp.status)"
                  @click.stop="cycleStatus(kp)">
                </div>
                <div class="kp-content" @click="openDialog(kp)">
                  <div class="kp-title" :class="{ 'kp-mastered': kp.status === 2 }">
                    {{ kp.title }}
                  </div>
                  <div class="kp-meta" v-if="kp.content">
                    {{ kp.content.substring(0, 80) }}{{ kp.content.length > 80 ? '...' : '' }}
                  </div>
                </div>
                <div class="kp-tags" v-if="kp.tags">
                  <el-tag v-for="tag in (kp.tags||'').split(',')" :key="tag" size="small" class="tag-item">
                    {{ tag.trim() }}
                  </el-tag>
                </div>
                <div class="kp-actions" @click.stop>
                  <el-button size="small" text @click="cycleStatus(kp)">
                    {{ statusLabel(kp.status) }}
                  </el-button>
                  <el-button size="small" text type="danger" @click="del(kp.id)">✕</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" top="30px">
      <el-form label-width="80px" :model="form">
        <el-form-item v-if="form.parentId === 0" label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择课程" clearable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.parentId === 0" label="类型">
          <el-radio-group v-model="form.isChapter">
            <el-radio :value="true">📁 章节</el-radio>
            <el-radio :value="false">📄 知识点</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!form.isChapter && form.parentId === 0" label="所属章节">
          <el-select v-model="form.parentId" placeholder="选择章节（可选）" clearable style="width: 100%">
            <el-option
              v-for="ch in availableChapters"
              :key="ch.id" :label="ch.title" :value="ch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="状态">
          <div class="status-switch">
            <div class="status-option" :class="{ active: form.status === 0 }" @click="form.status = 0">
              <span class="dot red"></span> 未学习
            </div>
            <div class="status-option" :class="{ active: form.status === 1 }" @click="form.status = 1">
              <span class="dot yellow"></span> 学习中
            </div>
            <div class="status-option" :class="{ active: form.status === 2 }" @click="form.status = 2">
              <span class="dot green"></span> 已掌握
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：重点,必考" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="知识点详细内容..." />
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
import { ArrowDown } from '@element-plus/icons-vue'

const treeData = ref([])
const courses = ref([])
const filterCourseId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogType = ref('chapter') // 'chapter' | 'kp'

const dialogTitle = computed(() => {
  if (isEdit.value) return form.value.parentId === 0 ? '编辑章节' : '编辑知识点'
  return form.value.parentId === 0 ? '添加章节' : '添加知识点'
})

const form = ref({
  id: null,
  courseId: null,
  parentId: 0,
  title: '',
  content: '',
  tags: '',
  status: 0,
  isChapter: true
})

// 可选的章节列表（用于新增知识点时选择归属章节）
const availableChapters = computed(() => {
  if (!form.value.courseId) return []
  const node = treeData.value.find(n => n.courseId === form.value.courseId)
  return node ? (node.children || []) : []
})

// 课程筛选
const filteredTree = computed(() => {
  if (!filterCourseId.value) return treeData.value
  return treeData.value.filter(n => n.courseId === filterCourseId.value)
})

const getCourseName = (id) => {
  if (!id) return ''
  const c = courses.value.find(c => c.id === id)
  return c ? c.name : ''
}

const progressPct = (node) => {
  if (!node.total || node.total === 0) return 0
  return Math.round((node.mastered || 0) / node.total * 100)
}

// 状态相关
const statusClass = (s) => {
  if (s === 0) return 'st-unlearned'
  if (s === 1) return 'st-learning'
  return 'st-mastered'
}

const statusLabel = (s) => {
  if (s === 0) return '未学习'
  if (s === 1) return '学习中'
  return '已掌握'
}

// 加载树
const loadTree = async () => {
  try {
    const res = await axios.get('/api/knowledge-point/tree')
    // 给每个节点加上展开状态
    const data = res.data || []
    data.forEach(n => {
      n._expanded = true
      if (n.children) {
        n.children.forEach(ch => {
          ch._expanded = false
        })
      }
    })
    treeData.value = data
  } catch (e) {
    treeData.value = []
  }
}

const loadCourses = async () => {
  try {
    const res = await axios.get('/api/course')
    courses.value = res.data
  } catch (e) {
    courses.value = []
  }
}

// 打开弹窗
const openDialogByType = (type) => {
  dialogType.value = type
  if (type === 'chapter') {
    openDialog()
  } else {
    openAddKpDialog()
  }
}

const openDialog = (row) => {
  if (row && row.id) {
    isEdit.value = true
    // 有 children 字段的是章节，否则是知识点
    const isChapterNode = row.children !== undefined
    form.value = {
      id: row.id,
      courseId: row.courseId,
      parentId: isChapterNode ? 0 : (row.parentId || 0),
      title: row.title,
      content: row.content || '',
      tags: row.tags || '',
      status: row.status || 0,
      isChapter: isChapterNode
    }
  } else {
    isEdit.value = false
    form.value = {
      id: null,
      courseId: filterCourseId.value || (courses.value[0]?.id) || null,
      parentId: 0,
      title: '',
      content: '',
      tags: '',
      status: 0,
      isChapter: true
    }
  }
  dialogVisible.value = true
}

const openAddKpDialog = (chapter) => {
  isEdit.value = false
  form.value = {
    id: null,
    courseId: chapter?.courseId || filterCourseId.value || (courses.value[0]?.id) || null,
    parentId: chapter ? chapter.id : 0,
    title: '',
    content: '',
    tags: '',
    status: 0,
    isChapter: false
  }
  dialogVisible.value = true
}

// 保存
const save = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    const data = {
      id: form.value.id,
      courseId: form.value.courseId,
      parentId: form.value.isChapter ? 0 : (form.value.parentId || 0),
      title: form.value.title,
      content: form.value.content,
      tags: form.value.tags,
      status: form.value.status,
      sortOrder: 0
    }
    if (isEdit.value) {
      const res = await axios.put('/api/knowledge-point', data)
      if (res.data.code === 200) ElMessage.success('修改成功')
      else { ElMessage.error(res.data.msg); return }
    } else {
      const res = await axios.post('/api/knowledge-point', data)
      if (res.data.code === 200) ElMessage.success('添加成功')
      else { ElMessage.error(res.data.msg); return }
    }
    dialogVisible.value = false
    loadTree()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '请求失败')
  }
}

// 切换状态（轮转）
const cycleStatus = async (kp) => {
  const next = (kp.status + 1) % 3
  try {
    const res = await axios.put(`/api/knowledge-point/${kp.id}/status`, { status: next })
    if (res.data.code === 200) {
      kp.status = next
      loadTree() // 刷新统计
    }
  } catch (e) {
    // ignore
  }
}

// 删除
const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除吗？子内容也会一并删除。', '提示', { type: 'warning' })
    await axios.delete(`/api/knowledge-point/${id}`)
    ElMessage.success('删除成功')
    loadTree()
  } catch (e) { /* cancel */ }
}

onMounted(() => {
  loadCourses()
  loadTree()
})
</script>

<style scoped>
.kp-container {
  /* 由父容器控制 */
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

.empty-state {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 16px;
}

/* ========== 课程卡片 ========== */
.kp-tree {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}

.course-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2f7 100%);
  border-bottom: 1px solid #eee;
  transition: background 0.15s;
}

.course-header:hover {
  background: linear-gradient(135deg, #eef2f7 0%, #e2e8f0 100%);
}

.course-expand {
  margin-right: 10px;
  color: #888;
}

.expand-icon {
  font-size: 11px;
}

.course-info {
  flex: 1;
}

.course-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.course-stats {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  display: flex;
  gap: 12px;
}

.course-progress-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 160px;
}

.progress-bar-sm {
  width: 100px;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill-sm {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #409eff);
  border-radius: 4px;
  transition: width 0.5s;
}

.progress-pct {
  font-size: 14px;
  font-weight: 700;
  color: #409eff;
  min-width: 36px;
}

/* ========== 章节 ========== */
.course-children {
  padding: 8px 16px 16px;
}

.no-chapter {
  color: #ccc;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.chapter-card {
  margin-top: 6px;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.chapter-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  background: #fafbfc;
  gap: 8px;
  transition: background 0.15s;
}

.chapter-header:hover {
  background: #f0f4f8;
}

.chapter-icon {
  font-size: 16px;
}

.chapter-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #444;
}

.ch-progress-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.progress-bar-xs {
  width: 60px;
  height: 5px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill-xs {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #409eff);
  border-radius: 3px;
  transition: width 0.5s;
}

.progress-pct-xs {
  font-size: 11px;
  color: #888;
  min-width: 28px;
}

.chapter-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

/* ========== 知识点 ========== */
.chapter-children {
  padding: 4px 8px 8px;
}

.no-kp {
  color: #ddd;
  font-size: 12px;
  text-align: center;
  padding: 12px 0;
}

.kp-item {
  display: flex;
  align-items: center;
  padding: 8px 10px 8px 24px;
  border-radius: 6px;
  transition: background 0.15s;
  gap: 10px;
}

.kp-item:hover {
  background: #f8fafc;
}

/* 状态圆点 */
.status-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.status-dot:hover {
  transform: scale(1.3);
}

.status-dot.st-unlearned {
  background: #f56c6c;
  box-shadow: 0 0 6px rgba(245,108,108,0.4);
}

.status-dot.st-learning {
  background: #e6a23c;
  box-shadow: 0 0 6px rgba(230,162,60,0.4);
}

.status-dot.st-mastered {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103,194,58,0.4);
}

.kp-content {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.kp-title {
  font-size: 14px;
  color: #333;
}

.kp-mastered {
  text-decoration: line-through;
  color: #999 !important;
}

.kp-meta {
  font-size: 12px;
  color: #bbb;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kp-tags {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.tag-item {
  white-space: nowrap;
}

.kp-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

/* ========== 弹窗状态选择 ========== */
.status-switch {
  display: flex;
  gap: 10px;
}

.status-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  user-select: none;
}

.status-option:hover {
  border-color: #409eff;
}

.status-option.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.dot.red { background: #f56c6c; }
.dot.yellow { background: #e6a23c; }
.dot.green { background: #67c23a; }
</style>
