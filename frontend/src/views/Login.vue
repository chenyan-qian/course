<template>
  <div class="login-wrapper">
    <div class="login-card">
      <h2>📚 个人课程管理系统</h2>

      <!-- 切换：登录 / 注册 -->
      <div class="tab-switch">
        <span :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</span>
        <span :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</span>
      </div>

      <el-form label-width="70px" :model="form">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password
            @keyup.enter="submit" />
        </el-form-item>

        <!-- 注册模式下多一个昵称 -->
        <el-form-item v-if="mode === 'register'" label="昵称">
          <el-input v-model="form.nickname" placeholder="给自己起个名字（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="submit">
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const mode = ref('login')       // 'login' | 'register'
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const switchMode = (m) => {
  mode.value = m
  form.username = ''
  form.password = ''
  form.nickname = ''
}

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    if (mode.value === 'login') {
      const res = await axios.post('/api/user/login', {
        username: form.username,
        password: form.password
      })
      if (res.data.code === 200) {
        localStorage.setItem('token', res.data.token)
        ElMessage.success('登录成功')
        router.push('/home')
      } else {
        ElMessage.error(res.data.msg || '登录失败')
      }
    } else {
      // 注册
      const res = await axios.post('/api/user/register', {
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username   // 不填昵称就用账号名
      })
      if (res.data.code === 200) {
        ElMessage.success('注册成功，请登录')
        switchMode('login')
        // 自动填回表单，方便直接登录
        form.username = form.username
        form.password = ''
      } else {
        ElMessage.error(res.data.msg || '注册失败')
      }
    }
  } catch (e) {
    console.error('请求失败:', e)
    if (e.response) {
      // 后端返回了错误
      ElMessage.error(`服务器错误: ${e.response.status}`)
    } else if (e.code === 'ERR_NETWORK') {
      ElMessage.error('无法连接后端，请确认 Spring Boot 已启动 (端口 8080)')
    } else {
      ElMessage.error('请求失败: ' + e.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px 35px 30px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-card h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
}

.tab-switch {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 24px;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  user-select: none;
}

.tab-switch span {
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-switch span.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: bold;
}

.tab-switch span:hover {
  color: #667eea;
}
</style>
