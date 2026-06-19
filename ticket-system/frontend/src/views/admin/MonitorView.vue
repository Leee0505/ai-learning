<template>
  <div class="monitor-page">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">System Monitoring</h1>
        <p class="page-subtitle">Real-time infrastructure health</p>
      </div>
      <div class="page-header-right">
        <select v-model="pollSeconds" class="poll-select" @change="restartPolling" aria-label="Refresh interval">
          <option :value="10">10s</option>
          <option :value="30">30s</option>
          <option :value="60">60s</option>
        </select>
        <button class="pause-btn" @click="togglePause" :aria-label="paused ? 'Resume polling' : 'Pause polling'">
          <svg v-if="paused" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polygon points="5,3 19,12 5,21"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
        </button>
        <span class="last-updated" v-if="lastUpdated">Updated {{ lastUpdated }}</span>
      </div>
    </header>

    <!-- ── Error Banner ── -->
    <div v-if="error" class="error-banner" role="alert">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <span>{{ error }}</span>
      <button class="error-retry-btn" @click="fetchAll">Retry</button>
    </div>

    <!-- ── Loading Skeleton ── -->
    <template v-if="loading && !hasData">
      <div class="status-cards">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton skeleton-dot"></div>
          <div class="skeleton skeleton-text"></div>
          <div class="skeleton skeleton-badge-sm"></div>
        </div>
      </div>
      <div class="metrics-grid">
        <div v-for="i in 3" :key="i" class="card">
          <div class="card-header"><div class="skeleton skeleton-text-short"></div></div>
          <div class="kv-list">
            <div v-for="j in 3" :key="j" class="kv-row"><div class="skeleton skeleton-text"></div><div class="skeleton skeleton-text-short"></div></div>
          </div>
        </div>
      </div>
    </template>

    <!-- ── Status Cards ── -->
    <div v-else class="status-cards">
      <div v-for="svc in services" :key="svc.key" class="status-card" :class="'status-card--' + svc.status">
        <span class="status-dot" :class="'status-dot--' + svc.status"></span>
        <div class="status-info">
          <span class="status-name">{{ svc.label }}</span>
          <span class="status-detail">{{ svc.detail }}</span>
        </div>
        <span class="status-badge" :class="'status-badge--' + svc.status">{{ svc.status }}</span>
      </div>
    </div>

    <!-- ── Metrics Cards ── -->
    <div v-if="!loading || hasData" class="metrics-grid">
      <!-- Kafka -->
      <div class="card">
        <div class="card-header">
          <h3>Kafka</h3>
          <span class="card-badge" :class="kafka.connected ? 'card-badge--ok' : 'card-badge--err'">
            {{ kafka.connected ? 'Connected' : 'Disconnected' }}
          </span>
        </div>
        <div class="kv-list">
          <div class="kv-row"><span class="kv-key">Active Consumers</span><span class="kv-value">{{ kafka.activeConsumers }}</span></div>
          <div class="kv-row"><span class="kv-key">Partitions</span><span class="kv-value">{{ kafka.totalPartitions }}</span></div>
        </div>
      </div>

      <!-- Redis -->
      <div class="card">
        <div class="card-header">
          <h3>Redis</h3>
          <span class="card-badge" :class="redis.connected ? 'card-badge--ok' : 'card-badge--err'">
            {{ redis.connected ? 'Connected' : 'Disconnected' }}
          </span>
        </div>
        <div class="kv-list">
          <div class="kv-row"><span class="kv-key">Cache Hit Rate</span><span class="kv-value">{{ (redis.hitRate * 100).toFixed(1) }}%</span></div>
          <div class="kv-row"><span class="kv-key">Total Keys</span><span class="kv-value">{{ redis.totalKeys.toLocaleString() }}</span></div>
          <div class="kv-row"><span class="kv-key">Memory Used</span><span class="kv-value">{{ formatBytes(redis.usedMemoryBytes) }}</span></div>
        </div>
      </div>

      <!-- API -->
      <div class="card card--wide">
        <div class="card-header">
          <h3>API Response Time</h3>
          <span class="card-badge" :class="api.p95Ms < 200 ? 'card-badge--ok' : api.p95Ms < 500 ? 'card-badge--warn' : 'card-badge--err'">
            P95 {{ api.p95Ms }}ms
          </span>
        </div>
        <div class="kv-list kv-list--cols">
          <div class="kv-row"><span class="kv-key">Requests</span><span class="kv-value">{{ api.requestCount.toLocaleString() }}</span></div>
          <div class="kv-row"><span class="kv-key">Errors</span><span class="kv-value">{{ api.errorCount.toLocaleString() }}</span></div>
          <div class="kv-row"><span class="kv-key">P50</span><span class="kv-value" :class="latencyClass(api.p50Ms)">{{ api.p50Ms }}ms</span></div>
          <div class="kv-row"><span class="kv-key">P95</span><span class="kv-value" :class="latencyClass(api.p95Ms)">{{ api.p95Ms }}ms</span></div>
          <div class="kv-row"><span class="kv-key">P99</span><span class="kv-value" :class="latencyClass(api.p99Ms)">{{ api.p99Ms }}ms</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { getMonitorOverviewApi, getKafkaMetricsApi, getRedisMetricsApi, getApiMetricsApi } from '@/api/monitor'

const pollSeconds = ref(30)
const paused = ref(false)
const lastUpdated = ref('')
const loading = ref(true)
const error = ref('')
const hasData = ref(false)
let pollTimer = null

const kafka = reactive({ connected: false, activeConsumers: 0, totalPartitions: 0 })
const redis = reactive({ connected: false, hitRate: 0, totalKeys: 0, usedMemoryBytes: 0 })
const api = reactive({ requestCount: 0, errorCount: 0, p50Ms: 0, p95Ms: 0, p99Ms: 0 })

const services = ref([
  { key: 'kafka', label: 'Kafka', status: 'healthy', detail: '' },
  { key: 'redis', label: 'Redis', status: 'healthy', detail: '' },
  { key: 'api', label: 'API', status: 'healthy', detail: '' }
])

onMounted(() => { fetchAll(); startPolling() })
onBeforeUnmount(() => clearInterval(pollTimer))

function startPolling() {
  clearInterval(pollTimer)
  if (!paused.value) pollTimer = setInterval(fetchAll, pollSeconds.value * 1000)
}
function restartPolling() { if (!paused.value) startPolling() }
function togglePause() { paused.value = !paused.value; paused.value ? clearInterval(pollTimer) : startPolling() }

async function fetchAll() {
  error.value = ''
  try {
    const [overviewRes, kafkaRes, redisRes, apiRes] = await Promise.all([
      getMonitorOverviewApi(), getKafkaMetricsApi(), getRedisMetricsApi(), getApiMetricsApi()
    ])
    if (overviewRes.data?.data) {
      updateService('kafka', overviewRes.data.data.kafkaStatus)
      updateService('redis', overviewRes.data.data.redisStatus)
      updateService('api', overviewRes.data.data.apiStatus)
    }
    if (kafkaRes.data?.data) Object.assign(kafka, kafkaRes.data.data)
    if (redisRes.data?.data) Object.assign(redis, redisRes.data.data)
    if (apiRes.data?.data) Object.assign(api, apiRes.data.data)
    lastUpdated.value = new Date().toLocaleTimeString()
    hasData.value = true
  } catch (e) {
    console.error('Monitor fetch error:', e)
    if (!hasData.value) {
      error.value = 'Failed to load metrics. The monitoring service may be unavailable.'
    }
  } finally {
    loading.value = false
  }
}

function updateService(key, status) {
  const svc = services.value.find(s => s.key === key)
  if (!svc) return
  const s = status ? status.toLowerCase() : 'critical'
  svc.status = s
  if (key === 'kafka') svc.detail = kafka.connected ? `${kafka.activeConsumers} consumers` : 'Unreachable'
  if (key === 'redis') svc.detail = redis.connected ? `${(redis.hitRate * 100).toFixed(1)}% hit rate` : 'Unreachable'
  if (key === 'api') svc.detail = `P95 ${api.p95Ms}ms`
}

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0; let v = bytes
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++ }
  return v.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

function latencyClass(ms) {
  if (ms < 200) return 'kv-value--ok'
  if (ms < 500) return 'kv-value--warn'
  return 'kv-value--err'
}
</script>

<style scoped>
.monitor-page { max-width: 1280px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }

.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-header-right { display: flex; align-items: center; gap: var(--space-sm); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

.poll-select { padding: 8px 12px; font-size: var(--text-sm); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); background: var(--color-white); cursor: pointer; }
.pause-btn { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.pause-btn svg { width: 18px; height: 18px; color: var(--color-text-secondary); }
.pause-btn:hover { border-color: var(--color-primary); }
.pause-btn:hover svg { color: var(--color-primary); }
.last-updated { font-size: var(--text-xs); color: var(--color-text-muted); }

/* Error banner */
.error-banner { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-md) var(--space-lg); margin-bottom: var(--space-lg); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-lg); color: #991B1B; font-size: var(--text-sm); }
.error-banner svg { width: 20px; height: 20px; flex-shrink: 0; }
.error-retry-btn { margin-left: auto; padding: 6px 14px; font-size: var(--text-xs); font-weight: 600; color: #991B1B; background: #FEE2E2; border: 1px solid #FECACA; border-radius: var(--radius-md); cursor: pointer; }
.error-retry-btn:hover { background: #FECACA; }

/* Skeleton */
.skeleton-card { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); border-left: 4px solid var(--color-gray-200); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.skeleton-text { flex: 1; height: 16px; }
.skeleton-text-short { width: 80px; height: 16px; }
.skeleton-badge-sm { width: 60px; height: 20px; border-radius: var(--radius-full); }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* Status Cards */
.status-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-md); margin-bottom: var(--space-lg); }
.status-card { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); border-left: 4px solid var(--color-gray-300); }
.status-card--healthy { border-left-color: #22C55E; }
.status-card--warning { border-left-color: #F59E0B; }
.status-card--critical { border-left-color: #EF4444; }
.status-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; background: var(--color-gray-300); }
.status-dot--healthy { background: #22C55E; }
.status-dot--warning { background: #F59E0B; }
.status-dot--critical { background: #EF4444; }
.status-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.status-name { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.status-detail { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); }
.status-badge { font-family: var(--font-mono); font-size: var(--text-xs); font-weight: 600; text-transform: uppercase; }
.status-badge--healthy { color: #15803D; }
.status-badge--warning { color: #B45309; }
.status-badge--critical { color: #B91C1C; }

/* Metrics Grid */
.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-md); }
.card--wide { grid-column: 1 / -1; }

.card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-gray-100); }
.card-header h3 { font-size: var(--text-sm); font-weight: 600; margin: 0; }
.card-badge { font-size: var(--text-xs); font-weight: 500; padding: 2px 10px; border-radius: var(--radius-full); }
.card-badge--ok { background: #D1FAE5; color: #065F46; }
.card-badge--warn { background: #FEF3C7; color: #92400E; }
.card-badge--err { background: #FEE2E2; color: #991B1B; }

/* KV list */
.kv-list { padding: var(--space-sm) var(--space-lg) var(--space-md); }
.kv-list--cols { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0; }
.kv-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--color-gray-50); }
.kv-row:last-child { border-bottom: none; }
.kv-key { font-size: var(--text-sm); color: var(--color-text-secondary); }
.kv-value { font-family: var(--font-mono); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.kv-value--ok { color: #15803D; }
.kv-value--warn { color: #B45309; }
.kv-value--err { color: #B91C1C; }

@media (max-width: 768px) {
  .monitor-page { padding: var(--space-lg) var(--space-md); }
  .status-cards { grid-template-columns: 1fr; }
  .metrics-grid { grid-template-columns: 1fr; }
  .card--wide { grid-column: 1; }
  .kv-list--cols { grid-template-columns: repeat(2, 1fr); }
}
</style>
