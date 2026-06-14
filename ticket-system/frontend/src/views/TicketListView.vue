<template>
  <div class="ticket-list">
    <!-- Page Header -->
    <div class="ticket-list-header">
      <div>
        <h1 class="ticket-list-title">Tickets</h1>
        <p class="ticket-list-subtitle">
          {{ authStore.isAdmin || authStore.isAgent ? 'All tickets in the system' : 'Your submitted tickets' }}
        </p>
      </div>
      <div class="ticket-list-header-btns">
        <div class="ticket-list-export" ref="exportRef">
          <button class="ticket-list-export-btn" @click="showExportMenu = !showExportMenu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="ticket-list-export-icon" aria-hidden="true">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7,10 12,15 17,10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Export
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="ticket-list-export-caret" aria-hidden="true">
              <polyline points="6,9 12,15 18,9"/>
            </svg>
          </button>
          <div v-if="showExportMenu" class="ticket-list-export-menu">
            <button @click="handleExport('csv'); showExportMenu = false">CSV (.csv)</button>
            <button @click="handleExport('excel'); showExportMenu = false">Excel (.xlsx)</button>
          </div>
        </div>
        <button class="ticket-list-create-btn" @click="goCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          New Ticket
        </button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="ticket-list-filters">
      <select v-model="store.filters.status" class="ticket-list-select" @change="store.fetchTickets()">
        <option value="">All Statuses</option>
        <option value="OPEN">Open</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="RESOLVED">Resolved</option>
        <option value="CLOSED">Closed</option>
      </select>
      <select v-model="store.filters.priority" class="ticket-list-select" @change="store.fetchTickets()">
        <option value="">All Priorities</option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
        <option value="URGENT">Urgent</option>
      </select>
      <select v-model="store.filters.category" class="ticket-list-select" @change="store.fetchTickets()">
        <option value="">All Categories</option>
        <option value="BUG">Bug</option>
        <option value="FEATURE_REQUEST">Feature Request</option>
        <option value="GENERAL_QUESTION">General Question</option>
        <option value="ACCOUNT_ISSUE">Account Issue</option>
        <option value="OTHER">Other</option>
      </select>
      <div class="ticket-list-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="ticket-list-search-icon" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="store.filters.keyword"
          placeholder="Search by title..."
          class="ticket-list-search-input"
          @keyup.enter="store.fetchTickets()"
        />
      </div>
      <button class="ticket-list-filter-btn" @click="store.fetchTickets()">Search</button>
      <button class="ticket-list-reset-btn" @click="store.resetFilters()">Reset</button>
    </div>

    <!-- Batch action bar — outside card to avoid layout shift when appearing -->
    <div v-if="authStore.isAdmin && selectedIds.length > 0" class="ticket-list-batch-bar">
      <span>{{ selectedIds.length }} selected</span>
      <button class="ticket-list-batch-delete" :disabled="batchLoading" @click="handleBatchDelete">
        {{ batchLoading ? 'Deleting...' : 'Delete Selected' }}
      </button>
    </div>

    <!-- Table -->
    <div class="ticket-list-table-card">
      <div v-if="store.loading" class="ticket-list-loading">Loading...</div>
      <div v-else-if="store.tickets.length === 0" class="ticket-list-empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="ticket-list-empty-icon" aria-hidden="true">
          <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
          <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
        </svg>
        <p>No tickets found</p>
        <button class="ticket-list-create-btn" @click="goCreate">Create your first ticket</button>
      </div>

      <table v-else class="ticket-list-table">
        <thead>
          <tr>
            <th v-if="authStore.isAdmin" class="ticket-list-check-col">
              <span class="check-box" :class="{ 'check-box--on': allChecked }" @click.stop="toggleSelectAll"></span>
            </th>
            <th>ID</th>
            <th>Title</th>
            <th>Status</th>
            <th>Priority</th>
            <th>Category</th>
            <th>Created By</th>
            <th>Assignee</th>
            <th class="ticket-list-sortable" @click="store.toggleSort()">
              Created
              <span class="sort-arrow">{{ store.sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="ticket in store.tickets"
            :key="ticket.id"
            class="ticket-list-row"
            @click="goDetail(ticket.id, $event)"
          >
            <td v-if="authStore.isAdmin" class="ticket-list-check-col">
              <span class="check-box" :class="{ 'check-box--on': selectedIds.includes(ticket.id) }" @click.stop="toggleSelect(ticket.id)"></span>
            </td>
            <td class="ticket-list-id">#{{ ticket.id }}</td>
            <td class="ticket-list-title-cell">{{ ticket.title }}</td>
            <td>
              <span :class="['ticket-list-badge', statusClass(ticket.status)]">{{ statusLabel(ticket.status) }}</span>
            </td>
            <td>
              <span :class="['ticket-list-badge', priorityClass(ticket.priority)]">{{ ticket.priority }}</span>
            </td>
            <td>{{ ticket.category }}</td>
            <td>{{ ticket.createdByName }}</td>
            <td class="ticket-list-assignee">
              <span v-if="ticket.assignedToName">{{ ticket.assignedToName }}</span>
              <span v-else class="ticket-list-unassigned">Unassigned</span>
            </td>
            <td class="ticket-list-date">{{ formatDate(ticket.createdDate) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="ticket-list-pagination">
      <button
        :disabled="store.page <= 1"
        class="ticket-list-page-btn"
        @click="store.setPage(store.page - 1)"
      >Previous</button>
      <span class="ticket-list-page-info">
        Page {{ store.page }} of {{ totalPages }} ({{ store.total }} total)
      </span>
      <select class="ticket-list-size-select" :value="store.size" @change="store.setSize(Number($event.target.value))">
        <option :value="10">10 / page</option>
        <option :value="20">20 / page</option>
        <option :value="50">50 / page</option>
        <option :value="100">100 / page</option>
      </select>
      <button
        :disabled="store.page >= totalPages"
        class="ticket-list-page-btn"
        @click="store.setPage(store.page + 1)"
      >Next</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/tickets'
import { useAuthStore } from '@/stores/auth'
import { deleteBatchTickets } from '@/api/tickets'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useTicketStore()
const authStore = useAuthStore()
const router = useRouter()

const selectedIds = ref([])
const batchLoading = ref(false)
const showExportMenu = ref(false)
const exportRef = ref(null)
const allChecked = computed(() => store.tickets.length > 0 && selectedIds.value.length === store.tickets.length)
const totalPages = computed(() => Math.max(1, Math.ceil(store.total / store.size)))

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll() {
  if (selectedIds.value.length === store.tickets.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = store.tickets.map(t => t.id)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `Delete ${selectedIds.value.length} selected ticket(s)? This action cannot be undone.`,
      'Batch Delete', { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    batchLoading.value = true
    const { data } = await deleteBatchTickets(selectedIds.value)
    if (data.code === 200) {
      ElMessage.success(`${data.data} ticket(s) deleted`)
      selectedIds.value = []
      store.fetchTickets()
    }
  } catch { /* cancelled or error */ }
  finally { batchLoading.value = false }
}

onMounted(() => {
  store.fetchTickets()
  document.addEventListener('click', onClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

function onClickOutside(e) {
  if (exportRef.value && !exportRef.value.contains(e.target)) {
    showExportMenu.value = false
  }
}

function goCreate() { router.push('/tickets/new') }

async function handleExport(format) {
  const params = {
    format,
    sortOrder: store.sortOrder,
    status: store.filters.status || undefined,
    priority: store.filters.priority || undefined,
    category: store.filters.category || undefined,
    keyword: store.filters.keyword || undefined
  }
  try {
    const res = await request.get('/tickets/export', { params, responseType: 'blob' })
    const ext = format === 'excel' ? 'xlsx' : 'csv'
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `tickets.${ext}`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('Export failed')
  }
}
function goDetail(id, event) {
  // Ignore clicks from the checkbox column
  if (event.target.closest('.ticket-list-check-col')) return
  router.push(`/tickets/${id}`)
}

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function statusClass(status) {
  return {
    'OPEN': 'badge-open',
    'IN_PROGRESS': 'badge-progress',
    'RESOLVED': 'badge-resolved',
    'CLOSED': 'badge-closed'
  }[status] || ''
}

function statusLabel(status) {
  return {
    'OPEN': 'Open',
    'IN_PROGRESS': 'In Progress',
    'RESOLVED': 'Resolved',
    'CLOSED': 'Closed'
  }[status] || status
}

function priorityClass(priority) {
  return {
    'LOW': 'badge-low',
    'MEDIUM': 'badge-medium',
    'HIGH': 'badge-high',
    'URGENT': 'badge-urgent'
  }[priority] || ''
}
</script>

<style scoped>
/* ── Layout ── */
.ticket-list {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg);
}

/* ── Header ── */
.ticket-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
}

.ticket-list-title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
}

.ticket-list-subtitle {
  margin: var(--space-xs) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* ── Header buttons ── */
.ticket-list-header-btns { display: flex; align-items: center; gap: var(--space-sm); }

/* Export dropdown */
.ticket-list-export { position: relative; }
.ticket-list-export-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 14px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body);
  color: var(--color-text-secondary); background: var(--color-white);
  border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer;
  transition: all var(--transition-fast);
}
.ticket-list-export-btn:hover { background: var(--color-gray-50); color: var(--color-primary); border-color: var(--color-primary); }
.ticket-list-export-icon, .ticket-list-export-caret { width: 16px; height: 16px; }
.ticket-list-export-caret { width: 14px; height: 14px; }

.ticket-list-export-menu {
  position: absolute; top: 100%; right: 0; margin-top: 4px; z-index: 20;
  background: var(--color-white); border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md); box-shadow: var(--shadow-md);
  overflow: hidden; min-width: 140px;
}
.ticket-list-export-menu button {
  display: block; width: 100%; padding: 10px 14px; font-size: var(--text-sm);
  font-family: var(--font-body); color: var(--color-text-primary);
  background: none; border: none; cursor: pointer; text-align: left;
  transition: background var(--transition-fast);
}
.ticket-list-export-menu button:hover { background: var(--color-primary-bg); color: var(--color-primary); }

.ticket-list-create-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px 20px;
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-body);
  color: var(--color-white);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: opacity var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}
.ticket-list-create-btn svg { width: 16px; height: 16px; }
.ticket-list-create-btn:hover { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35); }
.ticket-list-create-btn:active:not(:disabled) { transform: translateY(0); opacity: 0.85; }

.ticket-list-filter-btn {
  padding: 8px 16px;
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-body);
  color: var(--color-white);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: opacity var(--transition-fast);
}
.ticket-list-filter-btn:hover { opacity: 0.9; }

.ticket-list-reset-btn {
  padding: 8px 16px;
  font-size: var(--text-sm);
  font-weight: 500;
  font-family: var(--font-body);
  color: var(--color-text-secondary);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}
.ticket-list-reset-btn:hover { background: var(--color-gray-50); }

/* ── Filters ── */
.ticket-list-filters {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}

.ticket-list-select {
  padding: 8px 12px;
  font-size: var(--text-sm);
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  cursor: pointer;
  outline: none;
}
.ticket-list-select:focus { border-color: var(--color-primary); }

.ticket-list-search {
  position: relative;
  flex: 1;
  min-width: 200px;
}
.ticket-list-search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: var(--color-text-muted);
  pointer-events: none;
}
.ticket-list-search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  font-size: var(--text-sm);
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  outline: none;
  box-sizing: border-box;
}
.ticket-list-search-input:focus { border-color: var(--color-primary); }
.ticket-list-search-input::placeholder { color: var(--color-text-muted); }

/* ── Table Card ── */
.ticket-list-table-card {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow-x: auto;
}

/* Batch action bar */
.ticket-list-batch-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; margin-bottom: var(--space-sm);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary); border-radius: var(--radius-md);
  font-size: var(--text-sm); color: var(--color-primary); font-weight: 500;
}
.ticket-list-batch-delete {
  padding: 6px 16px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body);
  color: var(--color-white); background: var(--color-danger);
  border: none; border-radius: var(--radius-md); cursor: pointer;
}
.ticket-list-batch-delete:hover:not(:disabled) { opacity: 0.9; }
.ticket-list-batch-delete:disabled { opacity: 0.6; cursor: not-allowed; }

/* Custom checkbox */
.ticket-list-check-col { width: 40px; text-align: center; }
.check-box {
  display: inline-block; width: 18px; height: 18px;
  border: 2px solid var(--color-gray-300); border-radius: 3px;
  background: var(--color-white); cursor: pointer;
  vertical-align: middle; transition: all var(--transition-fast);
}
.check-box:hover { border-color: var(--color-primary); }
.check-box--on {
  background: var(--color-primary); border-color: var(--color-primary);
  position: relative;
}
.check-box--on::after {
  content: ''; position: absolute; left: 5px; top: 2px;
  width: 5px; height: 9px; border: solid white; border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.ticket-list-loading {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-secondary);
}

.ticket-list-empty {
  text-align: center;
  padding: var(--space-3xl) var(--space-lg);
  color: var(--color-text-secondary);
}
.ticket-list-empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.ticket-list-empty p { margin: 0 0 var(--space-md); }

/* ── Table ── */
.ticket-list-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--text-sm);
}
.ticket-list-table th {
  text-align: left;
  padding: 12px 16px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-gray-50);
  border-bottom: 1px solid var(--color-gray-200);
  white-space: nowrap;
}
.ticket-list-table td {
  padding: 12px 16px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-gray-100);
}

.ticket-list-row {
  cursor: pointer;
  transition: background var(--transition-fast);
}
.ticket-list-row:hover { background: var(--color-primary-bg); }
.ticket-list-row:last-child td { border-bottom: none; }

.ticket-list-id { color: var(--color-text-muted); font-family: var(--font-mono); font-size: var(--text-xs); }
.ticket-list-title-cell { font-weight: 500; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ticket-list-date { white-space: nowrap; color: var(--color-text-secondary); font-size: var(--text-xs); }

.ticket-list-sortable {
  cursor: pointer; user-select: none;
}
.ticket-list-sortable:hover { color: var(--color-primary); }
.sort-arrow {
  color: var(--color-primary); font-size: var(--text-xs); margin-left: 2px;
}

/* Assignee */
.ticket-list-unassigned { color: var(--color-text-muted); font-style: italic; }

/* ── Badges ── */
.ticket-list-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.badge-open { background: #DBEAFE; color: #1D4ED8; }
.badge-progress { background: #FEF3C7; color: #B45309; }
.badge-resolved { background: #D1FAE5; color: #047857; }
.badge-closed { background: var(--color-gray-100); color: var(--color-text-secondary); }
.badge-low { background: var(--color-gray-100); color: var(--color-text-secondary); }
.badge-medium { background: #DBEAFE; color: #1D4ED8; }
.badge-high { background: #FED7AA; color: #C2410C; }
.badge-urgent { background: #FEE2E2; color: #B91C1C; }

/* ── Pagination ── */
.ticket-list-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}
.ticket-list-page-btn {
  padding: 8px 16px;
  font-size: var(--text-sm);
  font-weight: 500;
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}
.ticket-list-page-btn:hover:not(:disabled) { background: var(--color-gray-50); }
.ticket-list-page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ticket-list-page-info { font-size: var(--text-sm); color: var(--color-text-secondary); }
.ticket-list-size-select {
  padding: 6px 8px; font-size: var(--text-xs); font-family: var(--font-body);
  color: var(--color-text-secondary); border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-sm); background: var(--color-white); cursor: pointer;
}

@media (max-width: 768px) {
  .ticket-list { padding: var(--space-lg) var(--space-md); }
  .ticket-list-header { flex-direction: column; gap: var(--space-md); }
  .ticket-list-filters { flex-direction: column; }
  .ticket-list-search { min-width: 100%; }
}
</style>