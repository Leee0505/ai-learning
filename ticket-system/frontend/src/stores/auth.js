import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, getCurrentUserApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref(null)
  const refreshToken = ref(localStorage.getItem('refreshToken') || null)
  const user = ref(null)

  // Getters
  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ROLE_ADMIN')
  const isAgent = computed(() => user.value?.role === 'ROLE_AGENT')

  // Actions
  function setTokens(newAccessToken, newRefreshToken) {
    accessToken.value = newAccessToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  function setUser(newUser) {
    user.value = newUser
  }

  async function login(login, password) {
    const { data } = await loginApi({ login, password })
    if (data.code === 200) {
      setTokens(data.data.accessToken, data.data.refreshToken)
      setUser(data.data.user)
    }
    return data
  }

  async function fetchUser() {
    try {
      const { data } = await getCurrentUserApi()
      if (data.code === 200) {
        setUser(data.data)
      }
    } catch {
      clearAuth()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuth()
    }
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken, refreshToken, user,
    isLoggedIn, isAdmin, isAgent,
    setTokens, setUser, login, fetchUser, logout, clearAuth
  }
})
