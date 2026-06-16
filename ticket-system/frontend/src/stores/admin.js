import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getUsersApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  changeUserRoleApi,
  toggleUserStatusApi
} from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  // State
  const users = ref([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)

  // Filters
  const keyword = ref('')
  const roleFilter = ref('')
  const statusFilter = ref(null)

  // Getters
  const totalPages = computed(() => Math.ceil(total.value / size.value) || 1)

  // Actions
  async function fetchUsers() {
    loading.value = true
    try {
      const params = { page: page.value, size: size.value }
      if (keyword.value) params.keyword = keyword.value
      if (roleFilter.value) params.role = roleFilter.value
      if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value

      const { data } = await getUsersApi(params)
      if (data?.code === 200 && data.data) {
        users.value = data.data.records || []
        total.value = data.data.total || 0
      } else {
        users.value = []
        total.value = 0
      }
    } catch (err) {
      console.error('[admin store] fetchUsers error:', err)
      users.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  async function createUser(userData) {
    const { data } = await createUserApi(userData)
    if (data.code === 200) {
      await fetchUsers()
    }
    return data
  }

  async function updateUser(id, updateData) {
    const { data } = await updateUserApi(id, updateData)
    if (data.code === 200) {
      // Refresh current page
      await fetchUsers()
    }
    return data
  }

  async function deleteUser(id) {
    const { data } = await deleteUserApi(id)
    if (data.code === 200) {
      await fetchUsers()
    }
    return data
  }

  async function changeRole(id, role) {
    const { data } = await changeUserRoleApi(id, { role })
    if (data.code === 200) {
      await fetchUsers()
    }
    return data
  }

  async function toggleStatus(id, status) {
    const { data } = await toggleUserStatusApi(id, { status: String(status) })
    if (data.code === 200) {
      await fetchUsers()
    }
    return data
  }

  function setPage(p) {
    page.value = p
    fetchUsers()
  }

  function setSize(s) {
    size.value = s
    page.value = 1
    fetchUsers()
  }

  function setKeyword(k) {
    keyword.value = k
    page.value = 1
    fetchUsers()
  }

  function setRoleFilter(r) {
    roleFilter.value = r
    page.value = 1
    fetchUsers()
  }

  function setStatusFilter(s) {
    statusFilter.value = s
    page.value = 1
    fetchUsers()
  }

  function resetFilters() {
    keyword.value = ''
    roleFilter.value = ''
    statusFilter.value = null
    page.value = 1
    fetchUsers()
  }

  return {
    users, total, page, size, loading,
    keyword, roleFilter, statusFilter,
    totalPages,
    fetchUsers, createUser, updateUser, deleteUser, changeRole, toggleStatus,
    setPage, setSize, setKeyword, setRoleFilter, setStatusFilter,
    resetFilters
  }
})
