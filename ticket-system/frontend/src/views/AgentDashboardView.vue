<template>
  <div class="perf">
    <header class="page-header">
      <h1 class="page-title">Performance</h1>
      <p class="page-subtitle">Your ticket handling statistics</p>
    </header>

    <div v-if="error" class="perf-error">Failed to load performance data. <a @click="fetchStats">Retry</a></div>

    <div v-if="loading" class="stats-row">
      <div v-for="i in 3" :key="i" class="perf-card skeleton-card"><div class="skeleton skeleton-bar"></div><div class="skeleton skeleton-text"></div></div>
    </div>
    <div v-else class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ stats.todayDone }}</div>
        <div class="stat-label">Done Today</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.avgProcessingMinutes }}<span class="stat-unit"> min</span></div>
        <div class="stat-label">Avg Processing Time</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-label">Active Tickets</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.pendingCount }}</div>
        <div class="stat-label">Pending Queue</div>
      </div>
    </div>

    <!-- 7-Day Chart -->
    <div class="chart-card">
      <h2 class="chart-title">Last 7 Days — Completed Tickets</h2>
      <div class="chart-bars">
        <div v-for="(val, i) in stats.chartValues" :key="i" class="chart-bar-col">
          <div class="chart-bar-wrapper">
            <div class="chart-bar" :style="{ height: barHeight(val) + '%' }">
              <span v-if="val > 0" class="chart-bar-val">{{ val }}</span>
            </div>
          </div>
          <span class="chart-bar-label">{{ stats.chartLabels?.[i] }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(true)
const error = ref(false)
const stats = ref({
  todayDone: 0,
  avgProcessingMinutes: 0,
  pendingCount: 0,
  activeCount: 0,
  chartLabels: [],
  chartValues: []
})

onMounted(() => fetchStats())
async function fetchStats() {
  loading.value = true; error.value = false
  try {
    const { data } = await request.get('/tickets/stats/agent')
    if (data.code === 200) stats.value = data.data
  } catch { error.value = true }
  finally { loading.value = false }
}

function barHeight(val) {
  const max = Math.max(...stats.value.chartValues, 1)
  return Math.round((val / max) * 100)
}
</script>

<style scoped>
.perf { max-width: 900px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { margin-bottom: var(--space-xl); }
.page-title { margin: 0 0 var(--space-xs); font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); }
.page-subtitle { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-md); margin-bottom: var(--space-xl); }
.stat-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); text-align: center; box-shadow: var(--shadow-sm); }
.stat-value { font-family: var(--font-heading); font-size: var(--text-3xl); font-weight: 700; color: var(--color-primary); line-height: 1.2; }
.stat-unit { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-muted); }
.stat-label { margin-top: var(--space-xs); font-size: var(--text-xs); color: var(--color-text-secondary); font-weight: 500; }

/* Chart */
.chart-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-xl); box-shadow: var(--shadow-sm); }
.chart-title { margin: 0 0 var(--space-xl); font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); }
.chart-bars { display: flex; align-items: flex-end; gap: var(--space-md); height: 160px; }
.chart-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.chart-bar-wrapper { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.chart-bar {
  width: 100%; max-width: 48px; min-height: 2px;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  display: flex; align-items: flex-start; justify-content: center;
  transition: height var(--transition-slow);
}
.chart-bar-val { font-size: var(--text-xs); font-weight: 600; color: var(--color-white); margin-top: 4px; }
.chart-bar-label { margin-top: var(--space-sm); font-size: var(--text-xs); color: var(--color-text-secondary); font-family: var(--font-mono); }

@media (max-width: 640px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
.perf-error { padding: var(--space-md) var(--space-lg); margin-bottom: var(--space-lg); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-lg); color: #991B1B; font-size: var(--text-sm); }
.perf-error a { color: #B91C1C; text-decoration: underline; cursor: pointer; }
.skeleton-card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); padding: var(--space-lg); text-align: center; display: flex; flex-direction: column; gap: var(--space-sm); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-bar { height: 32px; width: 48px; margin: 0 auto; }
.skeleton-text { height: 14px; width: 80px; margin: 0 auto; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
</style>
