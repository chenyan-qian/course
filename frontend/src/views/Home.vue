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

    <!-- 功能 Tabs -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ==================== 课程管理 ==================== -->
      <el-tab-pane label="📖 课程管理" name="course">
        <div class="toolbar">
          <el-button type="primary" @click="openCourseDialog()">+ 添加课程</el-button>
          <el-button type="danger" :disabled="selectedCourses.length === 0"
            @click="batchDelCourse">批量删除 ({{ selectedCourses.length }})</el-button>
        </div>

        <el-table :data="courses" border stripe style="width: 100%"
          @selection-change="onCourseSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="id" label="ID" width="60" />
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
      </el-tab-pane>

      <!-- ==================== 知识点管理 ==================== -->
      <el-tab-pane label="📝 知识点管理" name="kp">
        <KnowledgePoint />
      </el-tab-pane>
    </el-tabs>

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
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import KnowledgePoint from './KnowledgePoint.vue'

const router = useRouter()
const activeTab = ref('course')

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
  if (isCourseEdit.value) {
    await axios.put('/api/course', courseForm.value)
  } else {
    await axios.post('/api/course', courseForm.value)
  }
  courseDialogVisible.value = false
  loadCourses()
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
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  color: #333;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
