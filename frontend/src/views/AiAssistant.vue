<template>
  <div class="ai-assistant">
    <el-card class="chat-container">
      <template #header>
        <div class="chat-header">
          <span>🤖 AI 学习助手</span>
          <el-select v-model="selectedModel" size="small" style="width: 150px">
            <el-option label="快速模式" value="qwen-turbo" />
            <el-option label="标准模式" value="qwen-plus" />
            <el-option label="高性能模式" value="qwen-max" />
          </el-select>
        </div>
      </template>

      <!-- 聊天区域 -->
      <div ref="chatBoxRef" class="chat-box">
        <div v-for="(msg, idx) in messages" :key="idx"
             :class="['message', msg.role === 'user' ? 'user-msg' : 'ai-msg']">
          <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="msg-content">
            <div class="msg-text">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.time }}</div>
          </div>
        </div>
        <div v-if="loading" class="message ai-msg">
          <div class="msg-avatar">🤖</div>
          <div class="msg-content">
            <el-skeleton :rows="2" animated />
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题，Ctrl+Enter 发送..."
          @keydown.ctrl.enter="sendMessage"
        />
        <div class="input-actions">
          <el-button-group>
            <el-button @click="quickAction('add-course')">📖 智能添加课程</el-button>
            <el-button @click="quickAction('summarize')">📝 总结知识点</el-button>
            <el-button @click="quickAction('questions')">❓ 生成题目</el-button>
            <el-button @click="quickAction('feedback')">💡 作业反馈</el-button>
          </el-button-group>
          <el-button type="primary" :loading="loading" @click="sendMessage">
            发送 (Ctrl+Enter)
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 快捷功能对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px">
      <el-form :model="dialogForm" label-width="80px">
        <template v-if="dialogType === 'add-course'">
          <!-- 模式切换 -->
          <el-form-item label="输入方式">
            <el-radio-group v-model="courseMode" size="small">
              <el-radio-button value="text">📝 文字描述</el-radio-button>
              <el-radio-button value="file">📁 上传文件</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 文字描述模式 -->
          <el-form-item v-if="courseMode === 'text'" label="课程描述">
            <el-input v-model="dialogForm.courseText" type="textarea" :rows="4"
                      placeholder="用自然语言描述课程，例如：&#10;高等数学，王建国老师，在博学楼301，每周一早上8点到9点半" />
            <div style="color:#999;font-size:12px;margin-top:4px;">
              💡 描述中包含课程名、教师、教室、星期、时间即可自动解析
            </div>
          </el-form-item>

          <!-- 文件上传模式 -->
          <el-form-item v-if="courseMode === 'file'" label="选择文件">
            <el-upload
              class="course-file-upload"
              drag
              :auto-upload="false"
              :limit="1"
              accept=".png,.jpg,.jpeg,.pdf,.docx"
              :on-change="onCourseFileChange"
              :file-list="courseFileList"
            >
              <div class="el-upload__text">
                <span style="font-size:36px;">📂</span>
                <p>点击或拖拽文件到此区域</p>
                <p style="color:#999;font-size:12px;">支持 PNG / JPG / PDF / DOCX</p>
              </div>
            </el-upload>
            <div style="color:#999;font-size:12px;margin-top:4px;">
              💡 支持课程表截图、Word课表、PDF文件，AI 自动识别并导入
            </div>
          </el-form-item>
        </template>
        <el-form-item v-if="dialogType === 'summarize'" label="学习内容">
          <el-input v-model="dialogForm.text" type="textarea" :rows="5"
                    placeholder="粘贴需要总结的知识点内容..." />
        </el-form-item>
        <el-form-item v-if="dialogType === 'questions'" label="主题">
          <el-input v-model="dialogForm.topic" placeholder="例如：Java基础、数据结构..." />
        </el-form-item>
        <el-form-item v-if="dialogType === 'questions'" label="题目数量">
          <el-input-number v-model="dialogForm.count" :min="1" :max="10" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'feedback'" label="学生答案">
          <el-input v-model="dialogForm.studentAnswer" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'feedback'" label="正确答案">
          <el-input v-model="dialogForm.correctAnswer" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeQuickAction" :loading="loading">
          执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const messages = ref([
  { role: 'ai', content: '你好！我是 AI 学习助手，可以帮你总结知识点、生成题目、批改作业等。有什么可以帮你的？', time: new Date().toLocaleTimeString() }
])
const userInput = ref('')
const loading = ref(false)
const selectedModel = ref('qwen-turbo')
const chatBoxRef = ref(null)

// 快捷功能对话框
const dialogVisible = ref(false)
const dialogType = ref('')
const dialogTitle = ref('')
const dialogForm = ref({
  text: '',
  topic: '',
  count: 5,
  studentAnswer: '',
  correctAnswer: '',
  courseText: ''
})

// 智能添加课程 - 文件上传
const courseMode = ref('text')
const courseFile = ref(null)
const courseFileList = ref([])

function onCourseFileChange(file) {
  courseFile.value = file.raw
  courseFileList.value = [file]
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (chatBoxRef.value) {
      chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
    }
  })
}

// 发送消息
async function sendMessage() {
  const content = userInput.value.trim()
  if (!content) return

  messages.value.push({
    role: 'user',
    content,
    time: new Date().toLocaleTimeString()
  })
  userInput.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await axios.post('/api/llm/chat', {
      message: content,
      model: selectedModel.value
    })

    messages.value.push({
      role: 'ai',
      content: res.data.data,
      time: new Date().toLocaleTimeString()
    })
  } catch (e) {
    ElMessage.error('AI 服务调用失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 快捷功能
function quickAction(type) {
  dialogType.value = type
  dialogForm.value = {
    text: '',
    topic: '',
    count: 5,
    studentAnswer: '',
    correctAnswer: '',
    courseText: ''
  }
  courseMode.value = 'text'
  courseFile.value = null
  courseFileList.value = []

  switch(type) {
    case 'add-course':
      dialogTitle.value = '📖 智能添加课程'
      break
    case 'summarize':
      dialogTitle.value = '📝 知识点总结'
      break
    case 'questions':
      dialogTitle.value = '❓ 生成题目'
      break
    case 'feedback':
      dialogTitle.value = '💡 作业反馈'
      break
  }
  dialogVisible.value = true
}

// 执行快捷功能
async function executeQuickAction() {
  loading.value = true
  try {
    let res
    if (dialogType.value === 'add-course') {
      if (courseMode.value === 'file' && courseFile.value) {
        // 文件上传模式
        const formData = new FormData()
        formData.append('file', courseFile.value)
        res = await axios.post('/api/llm/add-course-by-file', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        // 处理文件导入结果（可能多门课程）
        if (res.data.code === 200) {
          ElMessage.success(res.data.msg)
          let content = '✅ ' + res.data.msg + '\n\n'
          if (res.data.data && res.data.data.length > 0) {
            res.data.data.forEach((c, i) => {
              content += `📖 ${c.name || '?'} | 👨‍🏫 ${c.teacher || '-'} | 🏫 ${c.classroom || '-'} | 📅 ${c.weekday || '-'} ${c.timeSlot || '-'}\n`
            })
          }
          if (res.data.errors && res.data.errors.length > 0) {
            content += '\n⚠️ 以下课程导入失败:\n'
            res.data.errors.forEach(e => { content += '· ' + e + '\n' })
          }
          messages.value.push({
            role: 'ai',
            content,
            time: new Date().toLocaleTimeString()
          })
          dialogVisible.value = false
          loading.value = false
          scrollToBottom()
          return
        } else {
          ElMessage.error(res.data.msg)
        }
      } else {
        // 文字描述模式
        res = await axios.post('/api/llm/add-course', {
          text: dialogForm.value.courseText
        })
        if (res.data.code === 200) {
          ElMessage.success(res.data.msg)
          messages.value.push({
            role: 'ai',
            content: '✅ ' + res.data.msg + '\n\n' +
              '📖 课程: ' + (res.data.data?.name || '?') + '\n' +
              '👨‍🏫 教师: ' + (res.data.data?.teacher || '未指定') + '\n' +
              '🏫 教室: ' + (res.data.data?.classroom || '未指定') + '\n' +
              '📅 时间: ' + (res.data.data?.weekday || '?') + ' ' + (res.data.data?.timeSlot || '?'),
            time: new Date().toLocaleTimeString()
          })
          dialogVisible.value = false
          loading.value = false
          scrollToBottom()
          return
        } else if (res.data.code === 409) {
          ElMessage.warning(res.data.msg)
        } else {
          ElMessage.error(res.data.msg)
        }
      }
    } else if (dialogType.value === 'summarize') {
      res = await axios.post('/api/llm/summarize', {
        text: dialogForm.value.text
      })
    } else if (dialogType.value === 'questions') {
      res = await axios.post('/api/llm/generate-questions', {
        topic: dialogForm.value.topic,
        count: dialogForm.value.count
      })
    } else if (dialogType.value === 'feedback') {
      res = await axios.post('/api/llm/homework-feedback', {
        studentAnswer: dialogForm.value.studentAnswer,
        correctAnswer: dialogForm.value.correctAnswer
      })
    }

    messages.value.push({
      role: 'ai',
      content: res.data.data,
      time: new Date().toLocaleTimeString()
    })
    dialogVisible.value = false
    scrollToBottom()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-assistant {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  min-height: 300px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.user-msg {
  flex-direction: row-reverse;
}

.msg-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.msg-content {
  max-width: 70%;
  background: #fff;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.user-msg .msg-content {
  background: #409eff;
  color: #fff;
}

.msg-text {
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.msg-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.user-msg .msg-time {
  color: rgba(255,255,255,0.7);
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
</style>
