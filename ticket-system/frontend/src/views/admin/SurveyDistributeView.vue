<template>
  <div class="distribute">
    <!-- Top bar -->
    <div class="distribute-topbar">
      <button class="distribute-back" @click="$router.push('/admin/surveys')" aria-label="Back to templates">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Back
      </button>
      <h1 class="distribute-title">{{ template?.title || 'Loading...' }}</h1>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
    </div>

    <div v-if="loading" class="distribute-loading">Loading...</div>

    <div v-else-if="template" class="distribute-layout">
      <!-- Left: Template Summary -->
      <aside class="distribute-summary">
        <h3 class="summary-title">Template Summary</h3>
        <div class="summary-stats">
          <div class="summary-stat">
            <span class="summary-stat-value">{{ template.pages?.length || 0 }}</span>
            <span class="summary-stat-label">Pages</span>
          </div>
          <div class="summary-stat">
            <span class="summary-stat-value">{{ totalSections }}</span>
            <span class="summary-stat-label">Sections</span>
          </div>
          <div class="summary-stat">
            <span class="summary-stat-value">{{ totalQuestions }}</span>
            <span class="summary-stat-label">Questions</span>
          </div>
        </div>
        <div class="summary-pages">
          <div v-for="(page, pi) in template.pages" :key="'sp'+page.id" class="summary-page">
            <div class="summary-page-title">{{ pi + 1 }}. {{ page.title }}</div>
            <div v-for="section in page.sections" :key="'ss'+section.id" class="summary-section">
              {{ section.title }} ({{ section.questions?.length || 0 }} Q)
            </div>
          </div>
        </div>
      </aside>

      <!-- Right: Instance Management -->
      <main class="distribute-main">
        <!-- Create Instance -->
        <section class="distribute-section">
          <h3 class="section-title">Distribute Survey</h3>
          <div class="distribute-form">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Assign To User</label>
                <select v-model="form.assignedTo" class="input" :disabled="creating">
                  <option :value="null" disabled>Select a user...</option>
                  <optgroup v-for="group in groupedUsers" :key="group.role" :label="group.label">
                    <option v-for="u in group.users" :key="u.id" :value="u.id">
                      {{ u.username }} — {{ u.email }}
                    </option>
                  </optgroup>
                </select>
                <div v-if="usersLoading" class="form-hint">Loading users...</div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Trigger Type</label>
                <select v-model="form.triggerType" class="input" style="max-width:200px">
                  <option value="MANUAL">Manual</option>
                  <option value="TICKET">Ticket</option>
                </select>
              </div>
              <div v-if="form.triggerType === 'TICKET'" class="form-group">
                <label class="form-label">Ticket ID (optional)</label>
                <input v-model.number="form.ticketId" type="number" class="input" style="max-width:160px" placeholder="e.g. 42" />
              </div>
            </div>

            <!-- Per-page assignee overrides -->
            <div v-if="template?.pages?.length" class="form-section">
              <h4 class="form-section-title">Page Assignees</h4>
              <p class="form-section-desc">Override assignee for specific pages. Leave as "Inherit" to use the survey assignee.</p>
              <div v-for="(page, pi) in template.pages" :key="'pa'+page.id" class="page-assignee-row">
                <span class="page-assignee-label">{{ pi + 1 }}. {{ page.title }}</span>
                <select v-model="form.pageAssignees[page.id]" class="input input-sm">
                  <option :value="null">↳ Inherit from survey</option>
                  <optgroup v-for="group in pageAssigneeUsers" :key="group.role" :label="group.label">
                    <option v-for="u in group.users" :key="u.id" :value="u.id">{{ u.username }}</option>
                  </optgroup>
                </select>
                <button type="button" class="btn-apply-all"
                  :disabled="!form.pageAssignees[page.id] || creating"
                  @click="applyPageToAll(page.id)"
                  title="Apply this assignee to all other pages">
                  Apply to all
                </button>
              </div>
            </div>

            <button class="btn-primary" :disabled="!form.assignedTo || creating" @click="distributeSurvey">
              {{ creating ? 'Distributing...' : 'Distribute Survey' }}
            </button>
          </div>
        </section>

        <!-- Existing Instances -->
        <section class="distribute-section">
          <h3 class="section-title">Existing Instances ({{ instances.length }})</h3>
          <div v-if="instancesLoading" class="distribute-loading">Loading instances...</div>
          <div v-else-if="instances.length === 0" class="distribute-empty">
            No instances have been distributed yet.
          </div>
          <table v-else class="instances-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Assigned To</th>
                <th>Status</th>
                <th>Progress</th>
                <th>Type</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="inst in instances" :key="'i'+inst.id">
                <td class="cell-mono">#{{ inst.id }}</td>
                <td>{{ getUserName(inst.assignedTo) }}</td>
                <td><span :class="['instance-status', 'istatus-' + (inst.status || '').toLowerCase()]">{{ inst.status }}</span></td>
                <td>{{ inst.completedPages || 0 }} / {{ inst.totalPages || 0 }} pages</td>
                <td>{{ inst.triggerType }}</td>
                <td>{{ formatDateTime(inst.createdDate) }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getTemplateApi, createInstanceApi, getTemplateInstancesApi } from '@/api/survey'
import { formatDateTime } from '@/utils/date'
import { getUsersApi } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const template = ref(null)
const loading = ref(true)
const instances = ref([])
const instancesLoading = ref(false)
const users = ref([])
const usersLoading = ref(false)
const creating = ref(false)

const form = reactive({
  assignedTo: null,
  triggerType: 'MANUAL',
  ticketId: null,
  pageAssignees: {} // { pageId: userId | null }
})

const totalSections = computed(() => {
  if (!template.value?.pages) return 0
  return template.value.pages.reduce((sum, p) => sum + (p.sections?.length || 0), 0)
})
const groupedUsers = computed(() => {
  const order = [
    { role: 'ROLE_ADMIN', label: 'Administrators' },
    { role: 'ROLE_AGENT', label: 'Agents' },
    { role: 'ROLE_USER', label: 'Users' }
  ]
  const currentUsername = authStore.user?.username
  return buildGroupedUsers(users.value, currentUsername, order)
})

// Tenant of the selected instance assignee
const selectedAssigneeTenant = computed(() => {
  if (!form.assignedTo) return null
  const u = users.value.find(u => u.id === form.assignedTo)
  return u?.tenantId ?? null
})

// Users filtered by selected assignee's tenant (for page assignee dropdowns)
const pageAssigneeUsers = computed(() => {
  const order = [
    { role: 'ROLE_ADMIN', label: 'Administrators' },
    { role: 'ROLE_AGENT', label: 'Agents' },
    { role: 'ROLE_USER', label: 'Users' }
  ]
  const currentUsername = authStore.user?.username
  const tid = selectedAssigneeTenant.value
  const filtered = tid != null ? users.value.filter(u => u.tenantId === tid) : users.value
  return buildGroupedUsers(filtered, currentUsername, order)
})

function buildGroupedUsers(source, currentUsername, order) {
  const groups = []
  for (const g of order) {
    const groupUsers = source.filter(u => u.role === g.role && u.username !== currentUsername)
    if (groupUsers.length > 0) groups.push({ role: g.role, label: g.label + ' (' + groupUsers.length + ')', users: groupUsers })
  }
  const knownRoles = order.map(g => g.role)
  const otherUsers = source.filter(u => !knownRoles.includes(u.role) && u.username !== currentUsername)
  if (otherUsers.length > 0) groups.push({ role: 'OTHER', label: 'Other (' + otherUsers.length + ')', users: otherUsers })
  return groups
}
const totalQuestions = computed(() => {
  if (!template.value?.pages) return 0
  return template.value.pages.reduce((sum, p) => {
    return sum + (p.sections || []).reduce((sSum, s) => sSum + (s.questions?.length || 0), 0)
  }, 0)
})

onMounted(async () => {
  const id = route.params.id
  try {
    const [templateRes, usersRes, instancesRes] = await Promise.all([
      getTemplateApi(id),
      getUsersApi({ page: 1, size: 200 }),
      getTemplateInstancesApi(id)
    ])
    if (templateRes.data?.code === 200) {
      template.value = templateRes.data.data
      // Pre-initialize per-page assignee overrides
      if (template.value?.pages) {
        template.value.pages.forEach(p => {
          if (!(p.id in form.pageAssignees)) form.pageAssignees[p.id] = null
        })
      }
    }
    if (usersRes.data?.code === 200) {
      users.value = usersRes.data.data?.records || usersRes.data.data || []
    }
    if (instancesRes.data?.code === 200) {
      instances.value = instancesRes.data.data || []
    }
  } catch (e) {
    console.error('[Distribute] Failed to load data:', e)
    ElMessage.error('Failed to load survey data')
  } finally {
    loading.value = false
  }
})

async function distributeSurvey() {
  if (!form.assignedTo) return
  creating.value = true
  try {
    // Build pageAssignees payload — only send non-null overrides
    const pageAssignees = {}
    for (const [pid, uid] of Object.entries(form.pageAssignees)) {
      if (uid !== null && uid !== undefined) pageAssignees[pid] = uid
    }
    const { data } = await createInstanceApi({
      templateId: template.value.id,
      assignedTo: form.assignedTo,
      triggerType: form.triggerType,
      ticketId: form.ticketId || null,
      pageAssignees: Object.keys(pageAssignees).length > 0 ? pageAssignees : null
    })
    if (data.code === 200) {
      ElMessage.success('Survey distributed successfully')
      form.assignedTo = null
      form.triggerType = 'MANUAL'
      form.ticketId = null
      // Reset page assignees
      if (template.value?.pages) {
        template.value.pages.forEach(p => { form.pageAssignees[p.id] = null })
      }
      // Refresh instances
      const instRes = await getTemplateInstancesApi(template.value.id)
      if (instRes.data?.code === 200) {
        instances.value = instRes.data.data || []
      }
    } else {
      ElMessage.error(data.message || 'Failed to distribute')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to distribute survey')
  } finally {
    creating.value = false
  }
}

function applyPageToAll(sourcePageId) {
  const value = form.pageAssignees[sourcePageId]
  if (!value) return
  if (template.value?.pages) {
    template.value.pages.forEach(p => { form.pageAssignees[p.id] = value })
  }
}

function getUserName(userId) {
  const u = users.value.find(u => u.id === userId)
  return u ? u.username : 'User #' + userId
}
</script>

<style scoped>
.distribute { display: flex; flex-direction: column; height: calc(100vh - 64px); overflow: hidden; }
.distribute-topbar { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-bottom: 1px solid var(--color-gray-200); flex-shrink: 0; }
.distribute-back { display: flex; align-items: center; gap: 4px; padding: 8px 16px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; min-height: 44px; }
.distribute-back svg { width: 18px; height: 18px; }
.distribute-title { font-size: var(--text-lg); font-weight: 600; margin: 0; }
.distribute-loading { padding: var(--space-2xl); text-align: center; color: var(--color-text-muted); }

.distribute-layout { display: flex; flex: 1; overflow: hidden; }

/* Left: Summary */
.distribute-summary { width: 300px; flex-shrink: 0; border-right: 1px solid var(--color-gray-200); padding: var(--space-lg); overflow-y: auto; background: var(--color-gray-50); }
.summary-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-secondary); margin: 0 0 var(--space-md); text-transform: uppercase; letter-spacing: 0.5px; }
.summary-stats { display: flex; gap: var(--space-md); margin-bottom: var(--space-lg); }
.summary-stat { flex: 1; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); padding: var(--space-sm); text-align: center; }
.summary-stat-value { display: block; font-size: var(--text-xl); font-weight: 700; color: var(--color-primary); }
.summary-stat-label { display: block; font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 2px; }
.summary-pages { display: flex; flex-direction: column; gap: var(--space-sm); }
.summary-page { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); padding: var(--space-sm); }
.summary-page-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); margin-bottom: 4px; }
.summary-section { font-size: var(--text-xs); color: var(--color-text-muted); padding: 2px 0 2px 12px; border-left: 2px solid var(--color-gray-200); margin-top: 2px; }

/* Right: Main */
.distribute-main { flex: 1; overflow-y: auto; padding: var(--space-lg) var(--space-xl); }
.distribute-section { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); }
.section-title { font-size: var(--text-base); font-weight: 600; margin: 0 0 var(--space-md); color: var(--color-text-primary); }

/* Form */
.distribute-form { display: flex; flex-direction: column; gap: var(--space-md); }
.form-row { display: flex; gap: var(--space-lg); align-items: flex-end; flex-wrap: wrap; }
.form-group { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 200px; }
.form-label { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.3px; }
.form-hint { font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 4px; }

.distribute-empty { padding: var(--space-lg); text-align: center; color: var(--color-text-muted); font-size: var(--text-sm); }

/* Instances Table */
.instances-table { width: 100%; border-collapse: collapse; font-size: var(--text-sm); }
.instances-table th { text-align: left; padding: 10px 12px; font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); border-bottom: 2px solid var(--color-gray-200); text-transform: uppercase; letter-spacing: 0.3px; }
.instances-table td { padding: 10px 12px; border-bottom: 1px solid var(--color-gray-100); color: var(--color-text-primary); }
.instances-table tbody tr:hover { background: var(--color-gray-50); }
.cell-mono { font-family: 'SF Mono', 'Fira Code', monospace; font-size: var(--text-xs); color: var(--color-text-muted); }

/* Status */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-muted); }

.instance-status { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.istatus-ready_to_start { background: var(--color-gray-100); color: var(--color-text-muted); }
.istatus-in_progress { background: #FEF3C7; color: #92400E; }
.istatus-submitted { background: #DBEAFE; color: #1D4ED8; }
.istatus-completed { background: #D1FAE5; color: #047857; }

/* Shared */
.input { padding: 8px 10px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.input-sm { padding: 6px 8px; font-size: var(--text-xs); }
.btn-primary { padding: 10px 20px; font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; align-self: flex-start; }
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

/* Page assignee "apply to all" button */
.btn-apply-all { padding: 6px 10px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-primary); background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); cursor: pointer; white-space: nowrap; min-height: 36px; transition: all var(--transition-fast); }
.btn-apply-all:hover:not(:disabled) { border-color: var(--color-primary); background: var(--color-primary-bg); }
.btn-apply-all:disabled { opacity: 0.3; cursor: not-allowed; }

/* Page assignee section */
.form-section { border-top: 1px solid var(--color-gray-200); padding-top: var(--space-md); }
.form-section-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); margin: 0 0 4px; }
.form-section-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0 0 var(--space-md); }
.page-assignee-row { display: flex; align-items: center; gap: var(--space-md); padding: 8px 0; border-bottom: 1px solid var(--color-gray-100); }
.page-assignee-row:last-child { border-bottom: none; }
.page-assignee-label { flex: 1; font-size: var(--text-sm); font-weight: 500; color: var(--color-text-primary); min-width: 120px; }
.page-assignee-row .input-sm { max-width: 240px; }
</style>
