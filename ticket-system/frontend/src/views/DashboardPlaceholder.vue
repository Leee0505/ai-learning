<template>
  <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;">
    <div style="text-align:center;">
      <h1>Welcome, {{ authStore.user?.username }}</h1>
      <p>Dashboard coming in Phase 2</p>
      <el-button @click="handleLogout">Logout</el-button>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

const authStore = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>
