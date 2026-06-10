<template>
  <AuthLayout>
    <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
      <el-form-item prop="login">
        <el-input
          v-model="form.login"
          placeholder="Username or email"
          size="large"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="Password"
          size="large"
          show-password
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width:100%"
          @click="handleLogin"
        >
          Sign In
        </el-button>
      </el-form-item>
    </el-form>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  login: '',
  password: ''
})

const rules = {
  login: [
    { required: true, message: 'Enter username or email', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const data = await authStore.login(form.login, form.password)
    if (data.code === 200) {
      ElMessage.success('Login successful')
      router.push('/')
    } else {
      ElMessage.error(data.message || 'Login failed')
    }
  } catch (error) {
    const message = error.response?.data?.message || 'Network error, please try again'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>
