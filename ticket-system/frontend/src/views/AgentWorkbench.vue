<template>
  <div class="workbench">
    <!-- ── Page Header ── -->
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Workbench</h1>
        <p class="page-subtitle">Manage and respond to support tickets</p>
      </div>
    </header>

    <!-- ── Stats Cards ── -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ pendingTotal }}</div>
        <div class="stat-label">Pending Queue</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ activeTotal }}</div>
        <div class="stat-label">My Active</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ todayDone }}</div>
        <div class="stat-label">Done Today</div>
      </div>
    </div>

    <!-- ── Pending Queue ── -->
    <div class="section">
      <h2 class="section-title">Pending Queue</h2>
      <p class="section-subtitle">Unassigned tickets waiting to be claimed</p>

      <div class="table-card">
        <div v-if="pendingLoading" class="table-loading" role="status">
          <div v-for="i in 3" :key="i" class="skeleton-row">
            <div class="skeleton skeleton-cell"></div>
            <div class="skeleton skeleton-cell-short"></div>
            <div class="skeleton skeleton-cell-short"></div>
            <div class="skeleton skeleton-cell-short"></div>
          </div>
        </div>
        <div v-else-if="pending.length === 0" class="table-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon" aria-hidden="true">
            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <p class="empty-title">All clear!</p>
          <p class="empty-desc">No unassigned tickets in the queue</p>
        </div>
        <div v-else class="table-wrapper">
          <table class="wb-table">
            <thead>
              <tr>
                <th>Title</th><th>Priority</th><th>Category</th><th>Created</th><th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in pending" :key="t.id" class="wb-row" @click="goDetail(t.id)">
                <td class="wb-title">{{ t.title }}</td>
                <td><span class="badge" :class="'badge--' + priorityClass(t.priority)">{{ t.priority }}</span></td>
                <td class="td-muted">{{ t.category }}</td>
                <td class="td-muted td-mono">{{ formatDate(t.createdDate) }}</td>
                <td @click.stop>
                  <button class="btn-take" @click="handleTake(t.id)">Take</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="pendingTotal > pending.length" class="table-footer">
          <a class="view-all" @click="goTickets('OPEN&assignedTo=unassigned')">View all {{ pendingTotal }} →</a>
        </div>
      </div>
    </div>

    <!-- ── My Active ── -->
    <div class="section">
      <h2 class="section-title">My Active</h2>
      <p class="section-subtitle">Tickets you're currently working on</p>

      <div class="table-card">
        <div v-if="activeLoading" class="table-loading" role="status">
          <div v-for="i in 3" :key="i" class="skeleton-row">
            <div class="skeleton skeleton-cell"></div>
            <div class="skeleton skeleton-cell-short"></div>
            <div class="skeleton skeleton-cell-short"></div>
            <div class="skeleton skeleton-cell-short"></div>
          </div>
        </div>
        <div v-else-if="active.length === 0" class="table-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon" aria-hidden="true">
            <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z"/>
            <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4"/>
          </svg>
          <p class="empty-title">No active tickets</p>
          <p class="empty-desc">Claim a ticket from the pending queue above</p>
        </div>
        <div v-else class="table-wrapper">
          <table class="wb-table">
            <thead>
              <tr>
                <th>Title</th><th>Priority</th><th>Status</th><th>Updated</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in active" :key="t.id" class="wb-row" @click="goDetail(t.id)">
                <td class="wb-title">{{ t.title }}</td>
                <td><span class="badge" :class="'badge--' + priorityClass(t.priority)">{{ t.priority }}</span></td>
                <td><span class="badge" :class="'badge--status badge--s-' + (t.status || '').toLowerCase()">{{ statusLabel(t.status) }}</span></td>
                <td class="td-muted td-mono">{{ formatDate(t.lastModifiedDate || t.createdDate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="activeTotal > active.length" class="table-footer">
          <a class="view-all" @click="goTickets('IN_PROGRESS')">View all {{ activeTotal }} →</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listTickets, assignTicket, getDashboardStats } from '@/api/tickets'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/date'

const router = useRouter()
const authStore = useAuthStore()

const pending = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(true)
const active = ref([])
const activeTotal = ref(0)
const activeLoading = ref(true)
const todayDone = ref(0)

onMounted(async () => {
  await Promise.all([fetchPending(), fetchActive(), fetchStats()])
})

async function fetchPending() {
  pendingLoading.value = true
  try {
    const { data } = await listTickets({ status: 'OPEN', assignedTo: 'unassigned', size: 5, sortOrder: 'desc' })
    if (data.code === 200) {
      pending.value = data.data.records
      pendingTotal.value = data.data.total
    }
  } finally { pendingLoading.value = false }
}

async function fetchActive() {
  activeLoading.value = true
  try {
    const id = authStore.user?.id
    const { data } = await listTickets({ status: 'IN_PROGRESS', assignedTo: String(id), size: 5, sortOrder: 'desc' })
    if (data.code === 200) {
      active.value = data.data.records
      activeTotal.value = data.data.total
    }
  } finally { activeLoading.value = false }
}

async function fetchStats() {
  try {
    const { data } = await getDashboardStats()
    if (data.code === 200) {
      todayDone.value = (data.data.resolved || 0) + (data.data.closed || 0)
    }
  } catch { /* ignore */ }
}

async function handleTake(id) {
  try {
    const { data } = await assignTicket(id, { assignedTo: authStore.user?.id })
    if (data.code === 200) {
      ElMessage.success('Ticket claimed')
      fetchPending()
      fetchActive()
    }
  } catch { /* ignore */ }
}

function goDetail(id) { router.push(`/tickets/${id}`) }
function goTickets(filter) { router.push(`/tickets?status=${filter}`) }

// ── Badge helpers (consistent with TicketListView) ──
function priorityClass(p) {
  return { 'LOW': 'low', 'MEDIUM': 'medium', 'HIGH': 'high', 'URGENT': 'urgent' }[p] || ''
}
function statusLabel(s) {
  return { 'OPEN': 'Open', 'IN_PROGRESS': 'In Progress', 'RESOLVED': 'Resolved', 'CLOSED': 'Closed' }[s] || s
}
</script>

<style scoped>
/* ── Page Layout ── */
.workbench { max-width: 1280px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }

/* ── Header ── */
.page-header { display: flex; align-items: flex-start; margin-bottom: var(--space-lg); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-secondary); margin: var(--space-xs) 0 0; }

/* ── Stats ── */
.stats-row { display: flex; gap: var(--space-md); margin-bottom: var(--space-xl); }
.stat-card { flex: 1; background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); padding: var(--space-lg); text-align: center; transition: box-shadow var(--transition-base); }
.stat-card:hover { box-shadow: var(--shadow-lg); }
.stat-value { font-family: var(--font-heading); font-size: var(--text-3xl); font-weight: 700; color: var(--color-primary); }
.stat-label { font-size: var(--text-sm); color: var(--color-text-secondary); margin-top: var(--space-xs); }

/* ── Section ── */
.section { margin-bottom: var(--space-xl); }
.section-title { font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0; }
.section-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: var(--space-xs) 0 var(--space-md); }

/* ── Table Card ── */
.table-card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.table-wrapper { overflow-x: auto; }
.wb-table { width: 100%; border-collapse: collapse; }
.wb-table thead { background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); }
.wb-table th { padding: 12px 16px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-heading); color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; text-align: left; white-space: nowrap; }
.wb-table td { padding: 14px 16px; font-size: var(--text-sm); color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); vertical-align: middle; }
.wb-row { cursor: pointer; transition: background var(--transition-fast); }
.wb-row:hover { background: var(--color-primary-bg); }
.wb-row:last-child td { border-bottom: none; }
.wb-title { font-weight: 500; max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Footer */
.table-footer { padding: 12px 16px; border-top: 1px solid var(--color-gray-100); text-align: center; }
.view-all { color: var(--color-primary); font-size: var(--text-sm); font-weight: 500; cursor: pointer; transition: color var(--transition-fast); }
.view-all:hover { color: var(--color-primary-dark); }

/* ── Badges (match TicketListView colors) ── */
.badge { display: inline-block; padding: 2px 8px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); border-radius: var(--radius-full); white-space: nowrap; }
.badge--low { background: var(--color-gray-100); color: var(--color-text-secondary); }
.badge--medium { background: #DBEAFE; color: #1D4ED8; }
.badge--high { background: #FED7AA; color: #C2410C; }
.badge--urgent { background: #FEE2E2; color: #B91C1C; }
.badge--status { min-width: 72px; text-align: center; }
.badge--s-open { background: #DBEAFE; color: #1D4ED8; }
.badge--s-in_progress { background: #FEF3C7; color: #B45309; }
.badge--s-resolved { background: #D1FAE5; color: #047857; }
.badge--s-closed { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Cell helpers */
.td-muted { color: var(--color-text-secondary); }
.td-mono { font-family: var(--font-mono); font-size: var(--text-xs); }

/* ── Take Button ── */
.btn-take { padding: 8px 18px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; min-width: 60px; min-height: 44px; transition: opacity var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast); }
.btn-take:hover { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35); }
.btn-take:active { transform: translateY(0); opacity: 0.85; }

/* ── Loading ── */
.table-loading { padding: var(--space-md); display: flex; flex-direction: column; gap: 10px; }
.skeleton-row { display: flex; gap: var(--space-md); padding: 12px 0; }
.skeleton { height: 20px; background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-cell { flex: 1; }
.skeleton-cell-short { flex: 0 0 80px; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* ── Empty ── */
.table-empty { display: flex; flex-direction: column; align-items: center; padding: var(--space-2xl) var(--space-lg); text-align: center; }
.empty-icon { width: 40px; height: 40px; color: var(--color-gray-300); margin-bottom: var(--space-sm); }
.empty-title { font-family: var(--font-heading); font-size: var(--text-base); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

/* ── Responsive ── */
@media (max-width: 768px) {
  .workbench { padding: var(--space-lg) var(--space-md); }
  .stats-row { flex-direction: column; }
}
</style>
