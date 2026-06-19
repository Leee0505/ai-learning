<template>
  <div class="user-management">
    <!-- ── Page Header ── -->
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">User Management</h1>
        <p class="page-subtitle">Manage users, roles, and account status</p>
      </div>
      <div class="page-header-right">
        <span class="user-count">{{ adminStore.total }} users</span>
        <button class="btn-primary" @click="openCreateDialog">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          New User
        </button>
      </div>
    </header>

    <!-- ── Filter Bar ── -->
    <div class="filter-bar">
      <div class="filter-search">
        <svg class="filter-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
          v-model="searchText"
          type="text"
          class="filter-search-input"
          placeholder="Search users..."
          @keyup.enter="adminStore.setKeyword(searchText)"
        />
      </div>

      <div class="filter-controls">
        <select v-model="roleValue" class="filter-select" @change="adminStore.setRoleFilter(roleValue)">
          <option value="">All Roles</option>
          <option value="ROLE_USER">User</option>
          <option value="ROLE_AGENT">Agent</option>
          <option value="ROLE_ADMIN">Admin</option>
        </select>

        <select v-model="statusValue" class="filter-select" @change="adminStore.setStatusFilter(statusValue)">
          <option :value="null">All Status</option>
          <option value="1">Enabled</option>
          <option value="0">Disabled</option>
        </select>

        <button class="filter-clear-btn" @click="handleClearFilters" v-if="hasActiveFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="filter-clear-icon" aria-hidden="true">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14" />
          </svg>
          Clear
        </button>
      </div>
    </div>

    <!-- ── User Table ── -->
    <div class="table-card">
      <div v-if="adminStore.loading && adminStore.users.length === 0" class="table-loading" role="status">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton skeleton-cell-short"></div>
          <div class="skeleton skeleton-cell"></div>
          <div class="skeleton skeleton-cell"></div>
          <div class="skeleton skeleton-cell-short"></div>
          <div class="skeleton skeleton-cell-short"></div>
          <div class="skeleton skeleton-cell-short"></div>
        </div>
      </div>

      <div v-else-if="!adminStore.loading && adminStore.users.length === 0" class="table-empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon" aria-hidden="true">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        <p class="empty-title">No users found</p>
        <p class="empty-desc">Try adjusting your search or filter criteria</p>
        <button class="btn-secondary" @click="handleClearFilters">Clear Filters</button>
      </div>

      <div v-else class="table-wrapper">
        <table class="user-table">
          <thead>
            <tr>
              <th>User</th><th>Email</th><th class="col-hide">Phone</th><th>Role</th><th>Status</th><th class="col-hide">Created</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in adminStore.users" :key="user.id" class="user-row">
              <td>
                <div class="user-cell">
                  <div class="user-avatar" aria-hidden="true">{{ user.username?.charAt(0)?.toUpperCase() }}</div>
                  <span class="user-name">{{ user.username }}</span>
                </div>
              </td>
              <td class="td-muted">{{ user.email || '—' }}</td>
              <td class="col-hide td-muted">{{ user.phone || '—' }}</td>
              <td><span class="badge" :class="'badge--' + roleClass(user.role)">{{ roleLabel(user.role) }}</span></td>
              <td><span class="badge" :class="user.status === 1 ? 'badge--enabled' : 'badge--disabled'">{{ user.status === 1 ? 'Enabled' : 'Disabled' }}</span></td>
              <td class="col-hide td-muted td-mono">{{ formatDate(user.createdDate) }}</td>
              <td class="td-actions" @click.stop>
                <button class="act-btn act-btn--edit" title="Edit" @click="openEditDialog(user)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button v-if="user.id !== authStore.user?.id" class="act-btn" :class="user.status === 1 ? 'act-btn--disable' : 'act-btn--enable'" :title="user.status === 1 ? 'Disable' : 'Enable'" @click="handleToggleStatus(user)">
                  <svg v-if="user.status === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="8,12 11,15 16,9"/></svg>
                </button>
                <button v-if="user.id !== authStore.user?.id" class="act-btn act-btn--delete" title="Delete" @click="handleDelete(user)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3,6 5,6 21,6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="adminStore.total > 0" class="table-pagination">
        <span class="pg-info">{{ (adminStore.page - 1) * adminStore.size + 1 }}–{{ Math.min(adminStore.page * adminStore.size, adminStore.total) }} of {{ adminStore.total }}</span>
        <div class="pg-ctrls">
          <button class="pg-btn" :disabled="adminStore.page <= 1" @click="adminStore.setPage(adminStore.page - 1)"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polyline points="15,18 9,12 15,6"/></svg></button>
          <span class="pg-cur">{{ adminStore.page }} / {{ adminStore.totalPages }}</span>
          <button class="pg-btn" :disabled="adminStore.page >= adminStore.totalPages" @click="adminStore.setPage(adminStore.page + 1)"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polyline points="9,18 15,12 9,6"/></svg></button>
        </div>
        <select v-model.number="pageSize" class="pg-size" @change="adminStore.setSize(pageSize)">
          <option :value="10">10/page</option><option :value="20">20/page</option><option :value="50">50/page</option><option :value="100">100/page</option>
        </select>
      </div>
    </div>

    <!-- ── Edit User Dialog ── -->
    <transition name="modal-fade">
      <div v-if="editVisible" class="modal-overlay" @click.self="editVisible = false">
        <div class="modal" role="dialog" aria-labelledby="edit-dialog-title" aria-modal="true">
          <div class="modal-header">
            <h2 id="edit-dialog-title" class="modal-title">Edit User</h2>
            <button class="modal-close" @click="editVisible = false" aria-label="Close dialog">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="edit-username">Username</label>
              <input
                id="edit-username"
                v-model="editForm.username"
                type="text"
                class="input"
                placeholder="Enter username"
                maxlength="64"
              />
              <p v-if="editError" class="form-error">{{ editError }}</p>
            </div>

            <div class="form-group">
              <label class="form-label" for="edit-email">Email</label>
              <input
                id="edit-email"
                v-model="editForm.email"
                type="email"
                class="input"
                placeholder="Enter email address"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="edit-phone">Phone</label>
              <input
                id="edit-phone"
                v-model="editForm.phone"
                type="text"
                class="input"
                placeholder="Enter phone number"
                maxlength="32"
              />
            </div>

            <!-- Role change -->
            <div class="form-group">
              <label class="form-label" for="edit-role">Role</label>
              <div class="role-change-row">
                <select id="edit-role" v-model="editForm.role" class="filter-select" style="flex:1">
                  <option value="ROLE_USER">User</option>
                  <option value="ROLE_AGENT">Agent</option>
                  <option value="ROLE_ADMIN">Admin</option>
                </select>
                <button
                  class="btn-secondary-sm"
                  :disabled="editForm.role === editingUser?.role"
                  @click="handleRoleChange"
                >
                  Apply
                </button>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-secondary" @click="editVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!hasEditChanges" @click="handleSaveEdit">
              Save Changes
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ── Create User Dialog ── -->
    <transition name="modal-fade">
      <div v-if="createVisible" class="modal-overlay" @click.self="createVisible = false">
        <div class="modal" role="dialog" aria-labelledby="create-dialog-title" aria-modal="true">
          <div class="modal-header">
            <h2 id="create-dialog-title" class="modal-title">Create User</h2>
            <button class="modal-close" @click="createVisible = false" aria-label="Close dialog">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="create-username">Username <span class="required">*</span></label>
              <input id="create-username" v-model="createForm.username" type="text" class="input" placeholder="Enter username" maxlength="64" />
            </div>

            <div class="form-group">
              <label class="form-label" for="create-email">Email <span class="required">*</span></label>
              <input id="create-email" v-model="createForm.email" type="email" class="input" placeholder="Enter email address" />
            </div>

            <div class="form-group">
              <label class="form-label" for="create-phone">Phone</label>
              <input id="create-phone" v-model="createForm.phone" type="text" class="input" placeholder="Enter phone number" maxlength="32" />
            </div>

            <div class="form-group">
              <label class="form-label" for="create-password">Password <span class="required">*</span></label>
              <input id="create-password" v-model="createForm.password" type="password" class="input" placeholder="Min 6 characters" minlength="6" maxlength="128" />
            </div>

            <div class="form-group">
              <label class="form-label" for="create-role">Role</label>
              <select id="create-role" v-model="createForm.role" class="filter-select" style="width:100%">
                <option value="ROLE_USER">User</option>
                <option value="ROLE_AGENT">Agent</option>
                <option value="ROLE_ADMIN">Admin</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="create-tenant">Tenant</label>
              <select id="create-tenant" v-model="createForm.tenantId" class="filter-select" style="width:100%">
                <option v-for="t in tenants" :key="t.id" :value="t.id">{{ t.name }} ({{ t.slug }})</option>
              </select>
            </div>

            <p v-if="createError" class="form-error">{{ createError }}</p>
          </div>

          <div class="modal-footer">
            <button class="btn-secondary" @click="createVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!createFormValid" @click="handleCreateUser">
              Create User
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import { getTenantsApi } from '@/api/tenant'
import { formatDate } from '@/utils/date'

const adminStore = useAdminStore()
const authStore = useAuthStore()

// Search debounce
const searchText = ref('')
let searchTimer = null
watch(searchText, (val) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    adminStore.setKeyword(val)
  }, 400)
})

// Filter bindings
const roleValue = ref('')
const statusValue = ref(null)
const pageSize = ref(20)

const hasActiveFilters = computed(() =>
  adminStore.keyword || adminStore.roleFilter || adminStore.statusFilter !== null
)

// Edit dialog
const editVisible = ref(false)
const editingUser = ref(null)
const editForm = ref({ username: '', email: '', phone: '', role: '' })
const editError = ref('')

const hasEditChanges = computed(() => {
  if (!editingUser.value) return false
  const u = editingUser.value
  return editForm.value.username !== u.username
    || editForm.value.email !== (u.email || '')
    || editForm.value.phone !== (u.phone || '')
})

// Create dialog
const createVisible = ref(false)
const createForm = ref({ username: '', email: '', phone: '', password: '', role: 'ROLE_USER', tenantId: null })
const createError = ref('')
const tenants = ref([])

const createFormValid = computed(() =>
  createForm.value.username.trim().length >= 2 &&
  createForm.value.email.trim() &&
  createForm.value.password.length >= 6
)

// ── Lifecycle ──
onMounted(() => {
  adminStore.fetchUsers()
})

// ── Helpers ──
function roleClass(role) {
  if (role === 'ROLE_ADMIN') return 'admin'
  if (role === 'ROLE_AGENT') return 'agent'
  return 'user'
}

function roleLabel(role) {
  if (role === 'ROLE_ADMIN') return 'Admin'
  if (role === 'ROLE_AGENT') return 'Agent'
  return 'User'
}

// formatDate is imported from @/utils/date

// ── Actions ──
async function openCreateDialog() {
  createForm.value = { username: '', email: '', phone: '', password: '', role: 'ROLE_USER', tenantId: null }
  createError.value = ''
  createVisible.value = true
  if (tenants.value.length === 0) {
    try {
      const { data } = await getTenantsApi()
      if (data?.code === 200) tenants.value = data.data || []
    } catch {}
  }
}

async function handleCreateUser() {
  createError.value = ''
  if (!createForm.value.username.trim() || createForm.value.username.trim().length < 2) {
    createError.value = 'Username must be at least 2 characters'
    return
  }
  if (!createForm.value.email.trim()) {
    createError.value = 'Email is required'
    return
  }
  if (createForm.value.password.length < 6) {
    createError.value = 'Password must be at least 6 characters'
    return
  }
  const result = await adminStore.createUser({
    username: createForm.value.username.trim(),
    email: createForm.value.email.trim(),
    phone: createForm.value.phone.trim() || undefined,
    password: createForm.value.password,
    role: createForm.value.role,
    tenantId: createForm.value.tenantId || undefined
  })
  if (result.code === 200) {
    createVisible.value = false
    ElMessage.success('User created successfully')
  } else {
    createError.value = result.message || 'Create failed'
  }
}

function handleClearFilters() {
  searchText.value = ''
  roleValue.value = ''
  statusValue.value = null
  adminStore.resetFilters()
}

function openEditDialog(user) {
  editingUser.value = user
  editForm.value = {
    username: user.username || '',
    email: user.email || '',
    phone: user.phone || '',
    role: user.role || 'ROLE_USER'
  }
  editError.value = ''
  editVisible.value = true
}

async function handleSaveEdit() {
  editError.value = ''
  if (!editForm.value.username.trim()) {
    editError.value = 'Username is required'
    return
  }
  const result = await adminStore.updateUser(editingUser.value.id, {
    username: editForm.value.username.trim(),
    email: editForm.value.email.trim() || undefined,
    phone: editForm.value.phone.trim() || undefined
  })
  if (result.code === 200) {
    editVisible.value = false
    ElMessage.success('User updated successfully')
  } else {
    editError.value = result.message || 'Update failed'
  }
}

async function handleRoleChange() {
  try {
    await ElMessageBox.confirm(
      `Change role to "${roleLabel(editForm.value.role)}"?`,
      'Confirm Role Change',
      { confirmButtonText: 'Change', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return /* cancelled */ }
  try {
    const result = await adminStore.changeRole(editingUser.value.id, editForm.value.role)
    if (result.code === 200) {
      editVisible.value = false
      ElMessage.success('Role updated successfully')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to update role')
  }
}

async function handleToggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  const actionLabel = newStatus === 0 ? 'Disable' : 'Enable'
  try {
    await ElMessageBox.confirm(
      `${actionLabel} account "${user.username}"?`,
      `Confirm ${actionLabel}`,
      { confirmButtonText: actionLabel, cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return /* cancelled */ }
  try {
    const result = await adminStore.toggleStatus(user.id, newStatus)
    if (result.code === 200) {
      ElMessage.success(`User ${newStatus === 0 ? 'disabled' : 'enabled'} successfully`)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to update status')
  }
}

async function handleDelete(user) {
  try {
    await ElMessageBox.confirm(
      `Permanently delete user "${user.username}"? This action cannot be undone.`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'error' }
    )
  } catch { return /* cancelled */ }
  try {
    const result = await adminStore.deleteUser(user.id)
    if (result.code === 200) {
      ElMessage.success('User deleted successfully')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to delete user')
  }
}
</script>

<style scoped>
/* ── Page Layout ── */
.user-management {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg);
}

/* ── Header ── */
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-header-right { display: flex; align-items: center; gap: var(--space-md); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }
.user-count { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-secondary); background: var(--color-gray-100); padding: 4px 12px; border-radius: var(--radius-full); }

/* ── Filter Bar ── */
.filter-bar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-md); flex-wrap: wrap; }
.filter-search { position: relative; flex: 1; min-width: 260px; }
.filter-search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); width: 18px; height: 18px; color: var(--color-gray-400); pointer-events: none; }
.filter-search-input { width: 100%; padding: 10px 14px 10px 38px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); transition: border-color var(--transition-fast), box-shadow var(--transition-fast); }
.filter-search-input::placeholder { color: var(--color-gray-400); }
.filter-search-input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.filter-controls { display: flex; align-items: center; gap: var(--space-sm); }
.filter-select { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: border-color var(--transition-fast); }
.filter-select:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.filter-clear-btn { display: flex; align-items: center; gap: 4px; padding: 10px 14px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-danger); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-md); cursor: pointer; transition: background var(--transition-fast); }
.filter-clear-btn:hover { background: #FEE2E2; }
.filter-clear-icon { width: 14px; height: 14px; }

/* ── Table Card ── */
.table-card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.table-wrapper { overflow-x: auto; border-radius: var(--radius-lg); }
.user-table { width: 100%; border-collapse: collapse; }
.user-table thead { background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); }
.user-table th { padding: 12px 16px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-heading); color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; text-align: left; white-space: nowrap; }
.user-table td { padding: 14px 16px; font-size: var(--text-sm); color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); vertical-align: middle; }
.user-row { cursor: pointer; transition: background var(--transition-fast); }
.user-row:hover { background: var(--color-primary-bg); }
.user-row:last-child td { border-bottom: none; }

/* Cell helpers */
.td-mono { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); }
.td-muted { color: var(--color-text-secondary); }
.user-cell { display: flex; align-items: center; gap: var(--space-sm); }
.user-avatar { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; font-family: var(--font-heading); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%); border-radius: var(--radius-full); flex-shrink: 0; }
.user-name { font-weight: 500; }
.td-actions { white-space: nowrap; }

/* Badges */
.badge { display: inline-block; padding: 3px 10px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); border-radius: var(--radius-full); }
.badge--admin { color: var(--color-primary-dark); background: var(--color-primary-bg); }
.badge--agent { color: #065F46; background: #D1FAE5; }
.badge--user { color: #1E40AF; background: #DBEAFE; }
.badge--enabled { color: #065F46; background: #D1FAE5; }
.badge--disabled { color: #991B1B; background: #FEE2E2; }

/* Action Buttons */
.act-btn { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; min-width: 44px; min-height: 44px; padding: 0; background: none; border: none; border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; transition: color var(--transition-fast), background var(--transition-fast); vertical-align: middle; }
.act-btn svg { width: 18px; height: 18px; }
.act-btn--edit:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.act-btn--enable:hover { color: var(--color-success); background: #D1FAE5; }
.act-btn--disable:hover { color: var(--color-warning); background: #FEF3C7; }
.act-btn--delete:hover { color: var(--color-danger); background: #FEE2E2; }

/* Pagination */
.table-pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-md); padding: 14px 16px; border-top: 1px solid var(--color-gray-100); flex-wrap: wrap; }
.pg-info { font-size: var(--text-sm); color: var(--color-text-muted); }
.pg-ctrls { display: flex; align-items: center; gap: var(--space-sm); }
.pg-btn { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; transition: border-color var(--transition-fast), color var(--transition-fast); }
.pg-btn:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.pg-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pg-cur { font-family: var(--font-mono); font-size: var(--text-sm); font-weight: 500; color: var(--color-text-primary); }
.pg-size { padding: 6px 10px; font-size: var(--text-xs); font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }

/* Loading Skeleton */
.table-loading { padding: var(--space-md); display: flex; flex-direction: column; gap: 10px; }
.skeleton-row { display: flex; gap: var(--space-md); padding: 12px 0; }
.skeleton { height: 20px; background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-cell { flex: 1; }
.skeleton-cell-short { flex: 0 0 80px; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* Empty State */
.table-empty { display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl) var(--space-lg); text-align: center; }
.empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.empty-title { font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-lg); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; z-index: var(--z-modal); display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.5); backdrop-filter: blur(4px); }
.modal { background: var(--color-white); border-radius: var(--radius-xl); box-shadow: var(--shadow-xl); width: 90%; max-width: 480px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-lg) var(--space-lg) 0; }
.modal-title { font-family: var(--font-heading); font-size: var(--text-xl); font-weight: 600; margin: 0; }
.modal-close { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; background: none; border: none; border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; transition: background var(--transition-fast); }
.modal-close svg { width: 18px; height: 18px; }
.modal-close:hover { background: var(--color-gray-100); }
.modal-body { padding: var(--space-lg); display: flex; flex-direction: column; gap: var(--space-md); }
.modal-footer { display: flex; justify-content: flex-end; gap: var(--space-sm); padding: 0 var(--space-lg) var(--space-lg); }

/* Form */
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-secondary); }
.form-label .required { color: var(--color-danger); }
.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); transition: border-color var(--transition-fast), box-shadow var(--transition-fast); }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.form-error { font-size: var(--text-xs); color: var(--color-danger); margin: 0; }
.role-change-row { display: flex; align-items: center; gap: var(--space-sm); }

/* Buttons */
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; transition: opacity var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast); }
.btn-primary svg { width: 16px; height: 16px; }
.btn-primary:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35); }
.btn-primary:active:not(:disabled) { transform: translateY(0); opacity: 0.85; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }
.btn-secondary { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: border-color var(--transition-fast), color var(--transition-fast); }
.btn-secondary:hover { border-color: var(--color-primary); color: var(--color-primary); }
.btn-secondary-sm { padding: 8px 14px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: 1px solid var(--color-primary-light); border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; transition: background var(--transition-fast); }
.btn-secondary-sm:hover:not(:disabled) { background: #EDE9FE; }
.btn-secondary-sm:disabled { opacity: 0.4; cursor: not-allowed; }

/* Modal Transition */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity var(--transition-base); }
.modal-fade-enter-active .modal, .modal-fade-leave-active .modal { transition: transform var(--transition-base); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .modal { transform: scale(0.95) translateY(8px); }
.modal-fade-leave-to .modal { transform: scale(0.95) translateY(8px); }

/* Responsive */
.col-hide {}
@media (max-width: 768px) {
  .user-management { padding: var(--space-lg) var(--space-md); }
  .filter-bar { flex-direction: column; }
  .filter-search { min-width: 100%; }
  .filter-controls { width: 100%; flex-wrap: wrap; }
  .col-hide { display: none; }
  .table-pagination { flex-direction: column; align-items: stretch; }
  .pg-ctrls { justify-content: center; }
}
</style>
