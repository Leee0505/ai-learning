<template>
  <div class="dashboard">
    <div class="dashboard-welcome">
      <h1 class="dashboard-welcome-title">
        Welcome back, {{ authStore.user?.username }}
      </h1>
      <p class="dashboard-welcome-subtitle">
        Your ticket management workspace. Manage and track all support tickets in one place.
      </p>
    </div>

    <!-- Error banner -->
    <div v-if="error" class="dash-error">Failed to load dashboard stats. <a @click="retry">Retry</a></div>

    <!-- Quick stats placeholder cards -->
    <div v-if="loading" class="dashboard-stats">
      <div v-for="i in 4" :key="i" class="dashboard-stat-card skeleton-card"><div class="skeleton skeleton-bar"></div><div class="skeleton skeleton-text"></div></div>
    </div>
    <div v-else class="dashboard-stats">
      <div class="dashboard-stat-card" @click="$router.push('/tickets')">
        <div class="dashboard-stat-icon dashboard-stat-icon--tickets">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
            <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
          </svg>
        </div>
        <div class="dashboard-stat-body">
          <span class="dashboard-stat-value">{{ stats.total }}</span>
          <span class="dashboard-stat-label">My Tickets</span>
        </div>
      </div>

      <div class="dashboard-stat-card" @click="$router.push('/tickets')">
        <div class="dashboard-stat-icon dashboard-stat-icon--pending">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12,6 12,12 16,14" />
          </svg>
        </div>
        <div class="dashboard-stat-body">
          <span class="dashboard-stat-value">{{ stats.open + stats.inProgress }}</span>
          <span class="dashboard-stat-label">Pending</span>
        </div>
      </div>

      <div class="dashboard-stat-card" @click="$router.push('/tickets')">
        <div class="dashboard-stat-icon dashboard-stat-icon--completed">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22,4 12,14.01 9,11.01" />
          </svg>
        </div>
        <div class="dashboard-stat-body">
          <span class="dashboard-stat-value">{{ stats.resolved + stats.closed }}</span>
          <span class="dashboard-stat-label">Completed</span>
        </div>
      </div>

      <div class="dashboard-stat-card" @click="$router.push('/tickets')">
        <div class="dashboard-stat-icon dashboard-stat-icon--response">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <polyline points="22,12 18,12 15,21 9,3 6,12 2,12" />
          </svg>
        </div>
        <div class="dashboard-stat-body">
          <span class="dashboard-stat-value">—</span>
          <span class="dashboard-stat-label">Avg Response</span>
        </div>
      </div>
    </div>

    <!-- Pending Surveys -->
    <div v-if="pendingSurveys.length > 0" class="dash-surveys">
      <h2 class="dash-surveys-title">Pending Surveys</h2>
      <div class="dash-surveys-list">
        <div v-for="inst in pendingSurveys" :key="inst.id" class="dash-survey-card" @click="$router.push('/surveys')">
          <div class="dash-survey-info">
            <span class="dash-survey-name">{{ inst.title }}</span>
            <span class="dash-survey-meta">{{ inst.completedPages }}/{{ inst.totalPages }} pages</span>
          </div>
          <span :class="['status-badge', 'status-' + inst.status.toLowerCase()]">{{ inst.status.replace(/_/g, ' ') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDashboardStats } from '@/api/tickets'
import { getMyInstancesApi } from '@/api/survey'

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(true)
const error = ref(false)
const stats = ref({ total: 0, open: 0, inProgress: 0, resolved: 0, closed: 0 })
const pendingSurveys = ref([])

onMounted(async () => {
  try {
    const { data } = await getDashboardStats()
    if (data.code === 200) {
      stats.value = data.data
    }
  } catch { error.value = true }
  finally { loading.value = false }

  try {
    const { data } = await getMyInstancesApi()
    if (data.code === 200) {
      pendingSurveys.value = (data.data || []).filter(i => i.status !== 'COMPLETED')
    }
  } catch { /* no surveys */ }
})

async function retry() {
  error.value = false; loading.value = true
  try {
    const { data } = await getDashboardStats()
    if (data.code === 200) stats.value = data.data
  } catch { error.value = true }
  finally { loading.value = false }
}
</script>

<style scoped>
/* ── Layout ── */
.dashboard {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg);
}

/* Welcome */
.dashboard-welcome {
  margin-bottom: var(--space-xl);
}

.dashboard-welcome-title {
  margin: 0 0 var(--space-xs);
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
}

.dashboard-welcome-subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

/* Stats Grid */
.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
}

.dashboard-stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
  cursor: pointer;
}

.dashboard-stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.dashboard-stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.dashboard-stat-icon svg {
  width: 24px;
  height: 24px;
}

.dashboard-stat-icon--tickets {
  background: #EDE9FE;
  color: var(--color-primary);
}

.dashboard-stat-icon--pending {
  background: #FEF3C7;
  color: var(--color-warning);
}

.dashboard-stat-icon--completed {
  background: #D1FAE5;
  color: var(--color-success);
}

.dashboard-stat-icon--response {
  background: #DBEAFE;
  color: var(--color-info);
}

.dashboard-stat-body {
  display: flex;
  flex-direction: column;
}

.dashboard-stat-value {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.dashboard-stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .dashboard {
    padding: var(--space-lg) var(--space-md);
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
  }

  .dashboard-welcome-title {
    font-size: var(--text-xl);
  }
}

/* Error & skeleton states */
.dash-error { padding: var(--space-md) var(--space-lg); margin-bottom: var(--space-lg); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-lg); color: #991B1B; font-size: var(--text-sm); }
.dash-error a { color: #B91C1C; text-decoration: underline; cursor: pointer; }
.skeleton-card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); padding: var(--space-lg); text-align: center; display: flex; flex-direction: column; gap: var(--space-sm); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-bar { height: 32px; width: 48px; margin: 0 auto; }
.skeleton-text { height: 14px; width: 80px; margin: 0 auto; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

.dash-surveys { margin-top: var(--space-xl); }
.dash-surveys-title { font-size: var(--text-lg); font-weight: 600; margin: 0 0 var(--space-md); }
.dash-surveys-list { display: flex; flex-direction: column; gap: var(--space-sm); }
.dash-survey-card { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: box-shadow var(--transition-fast); }
.dash-survey-card:hover { box-shadow: var(--shadow-sm); }
.dash-survey-info { display: flex; flex-direction: column; gap: 2px; }
.dash-survey-name { font-size: var(--text-sm); font-weight: 500; }
.dash-survey-meta { font-size: var(--text-xs); color: var(--color-text-muted); }
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-ready_to_start, .status-in_progress { background: #FEF3C7; color: #92400E; }
.status-submitted { background: #DBEAFE; color: #1D4ED8; }
</style>
