<template>
  <div class="survey-list">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Survey Templates</h1>
        <p class="page-subtitle">Create and manage survey questionnaires</p>
      </div>
      <button class="btn-primary" @click="openCreateDialog">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        New Template
      </button>
    </header>

    <!-- Table -->
    <div class="table-card">
      <div v-if="store.loading" class="loading-state">Loading...</div>
      <div v-else-if="store.templates.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon" aria-hidden="true">
          <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
        </svg>
        <p class="empty-title">No survey templates yet</p>
        <button class="btn-primary" @click="openCreateDialog">Create your first template</button>
      </div>

      <table v-else class="survey-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Status</th>
            <th>Pages</th>
            <th>Questions</th>
            <th>Version</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in store.templates" :key="t.id" class="survey-row">
            <td class="survey-title-cell">
              <span class="survey-title">{{ t.title }}</span>
              <span v-if="t.description" class="survey-desc">{{ t.description }}</span>
            </td>
            <td>
              <span :class="['status-badge', 'status-' + t.status.toLowerCase()]">{{ t.status }}</span>
            </td>
            <td>{{ t.pages?.length || 0 }}</td>
            <td>{{ countQuestions(t) }}</td>
            <td>v{{ t.version }}</td>
            <td class="survey-date">{{ formatDate(t.createdDate) }}</td>
            <td class="survey-actions">
              <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--edit" @click="editTemplate(t.id)" aria-label="Edit template">Edit</button>
              <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--publish" @click="publishTemplate(t.id)" aria-label="Publish template">Publish</button>
              <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--delete" @click="handleDelete(t)" aria-label="Delete template">Delete</button>
              <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--view" @click="viewTemplate(t.id)" aria-label="View template">View</button>
              <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--distribute" @click="distributeTemplate(t)" aria-label="Distribute survey">Distribute</button>
              <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--archive" @click="archiveTemplate(t.id)" aria-label="Archive template">Archive</button>
              <button v-if="t.status === 'ARCHIVED'" class="act-btn act-btn--view" @click="viewTemplate(t.id)" aria-label="View template">View</button>
              <button v-if="t.status === 'DRAFT' || t.status === 'PUBLISHED'" class="act-btn act-btn--clone" @click="cloneTemplate(t.id)" aria-label="Clone template">Clone</button>
              <button v-if="t.status === 'ARCHIVED'" class="act-btn act-btn--clone" @click="cloneTemplate(t.id)" aria-label="Clone template">Clone</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination-bar">
      <button :disabled="store.page <= 1" class="page-btn" @click="store.fetchTemplates(store.page - 1)">Previous</button>
      <span class="page-info">Page {{ store.page }} of {{ store.totalPages }} ({{ store.total }} total)</span>
      <select class="size-select" :value="store.size" @change="store.size = Number($event.target.value); store.fetchTemplates(1)">
        <option :value="10">10 / page</option>
        <option :value="20">20 / page</option>
        <option :value="50">50 / page</option>
      </select>
      <button :disabled="store.page >= store.totalPages" class="page-btn" @click="store.fetchTemplates(store.page + 1)">Next</button>
    </div>

    <!-- Create Dialog -->
    <transition name="modal-fade">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">New Survey Template</h2>
            <button class="modal-close" @click="dialogVisible = false" aria-label="Close"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="s-title">Title <span class="required">*</span></label>
              <input id="s-title" v-model="form.title" type="text" class="input" placeholder="e.g. Q2 Team Satisfaction Survey" maxlength="255" />
            </div>
            <div class="form-group">
              <label class="form-label" for="s-desc">Description</label>
              <textarea id="s-desc" v-model="form.description" class="input textarea" placeholder="Optional description..." rows="3"></textarea>
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input v-model="form.allowResubmit" type="checkbox" :true-value="1" :false-value="0" />
                Allow users to resubmit after submission
              </label>
            </div>
            <p v-if="dialogError" class="form-error">{{ dialogError }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="dialogVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!form.title.trim()" @click="handleCreate">Create Template</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSurveyStore } from '@/stores/survey'
import { getTemplateApi, updateTemplateApi, cloneTemplateApi } from '@/api/survey'
import { formatDate } from '@/utils/date'

const store = useSurveyStore()
const router = useRouter()

const dialogVisible = ref(false)
const dialogError = ref('')
const form = reactive({ title: '', description: '', allowResubmit: 0 })

onMounted(() => store.fetchTemplates())

function countQuestions(template) {
  if (!template.pages) return 0
  let count = 0
  for (const page of template.pages) {
    for (const section of page.sections || []) count += (section.questions || []).length
  }
  return count
}

function openCreateDialog() {
  form.title = ''; form.description = ''; form.allowResubmit = 0
  dialogError.value = ''
  dialogVisible.value = true
}

async function handleCreate() {
  dialogError.value = ''
  const result = await store.createTemplate({ ...form })
  if (result.code === 200) {
    dialogVisible.value = false
    ElMessage.success('Template created')
  } else {
    dialogError.value = result.message || 'Create failed'
  }
}

function editTemplate(id) { router.push(`/admin/surveys/${id}/builder`) }
function viewTemplate(id) { router.push(`/admin/surveys/${id}/results`) }

async function publishTemplate(id) {
  try {
    await ElMessageBox.confirm('Publish this template? Once published, instances can be distributed.', 'Publish Template', { confirmButtonText: 'Publish', type: 'info' })
  } catch { return }
  // Validate before publishing
  const { data: tpl } = await getTemplateApi(id)
  if (tpl.code !== 200) { ElMessage.error('Failed to load template'); return }
  const errors = []
  for (const page of tpl.data.pages || []) {
    for (const section of page.sections || []) {
      if (!section.questions || section.questions.length === 0) {
        errors.push(`Section "${section.title}" in "${page.title}" has no questions`)
        continue
      }
      for (const q of section.questions) {
        if (q.type === 'SINGLE_CHOICE' || q.type === 'MULTI_CHOICE' || q.type === 'DROPDOWN') {
          const opts = parseOpts(q.options)
          if (opts.length === 0) errors.push(`"${q.title}" has no options configured`)
        }
        if (q.type === 'RATING') {
          const max = getMax(q.options)
          if (!max || max < 2) errors.push(`"${q.title}" rating max must be at least 2`)
        }
        if (q.type === 'TABLE') {
          const cols = JSON.parse(q.options || '{}').columns || []
          for (const col of cols) {
            if (col.type === 'DROPDOWN' && (!col.options || col.options.length === 0)) {
              errors.push(`Table "${q.title}" column "${col.label}" is DROPDOWN but has no options`)
            }
          }
        }
      }
    }
  }
  if (errors.length > 0) {
    ElMessage.warning('Cannot publish: ' + errors.slice(0, 3).join('; ') + (errors.length > 3 ? ` ...and ${errors.length - 3} more` : ''))
    return
  }
  const { data } = await updateTemplateApi(id, { status: 'PUBLISHED' })
  if (data.code === 200) { ElMessage.success('Published'); store.fetchTemplates() }
  else ElMessage.error(data.message)
}
function parseOpts(json) { try { return JSON.parse(json || '{}').options || [] } catch { return [] } }
function getMax(json) { try { return JSON.parse(json || '{}').max } catch { return null } }

async function archiveTemplate(id) {
  try {
    await ElMessageBox.confirm('Archive this template? Existing instances will remain active.', 'Archive Template', { confirmButtonText: 'Archive', type: 'warning' })
  } catch { return }
  const { data } = await updateTemplateApi(id, { status: 'ARCHIVED' })
  if (data.code === 200) { ElMessage.success('Archived'); store.fetchTemplates() }
  else ElMessage.error(data.message)
}

function distributeTemplate(template) { router.push(`/admin/surveys/${template.id}/distribute`) }
function cloneTemplate(id) { ElMessage.info('Clone will be available soon') }

async function handleDelete(template) {
  try {
    await ElMessageBox.confirm(`Delete "${template.title}"? This cannot be undone.`, 'Delete Template', { confirmButtonText: 'Delete', type: 'warning' })
  } catch { return }
  await store.deleteTemplate(template.id)
  ElMessage.success('Deleted')
}

async function cloneTemplate(id) {
  try {
    await ElMessageBox.confirm('Clone this template with all pages, questions, and rules?', 'Clone Template', { confirmButtonText: 'Clone', type: 'info' })
  } catch { return }
  try {
    const { data } = await cloneTemplateApi(id)
    if (data.code === 200) {
      ElMessage.success('Cloned — new template created as DRAFT')
      store.fetchTemplates()
    } else {
      ElMessage.error(data.message || 'Clone failed')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Clone failed')
  }
}
</script>

<style scoped>
.survey-list { max-width: 1280px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

/* Table card */
.table-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow-x: auto; }

/* Table */
.survey-table { width: 100%; border-collapse: collapse; font-size: var(--text-sm); }
.survey-table th { text-align: left; padding: 12px 16px; font-weight: 600; color: var(--color-text-secondary); background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); white-space: nowrap; }
.survey-table td { padding: 12px 16px; color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); }
.survey-row { transition: background var(--transition-fast); }
.survey-row:hover { background: var(--color-primary-bg); }
.survey-row:last-child td { border-bottom: none; }

.survey-title-cell { max-width: 300px; }
.survey-title { font-weight: 500; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.survey-desc { font-size: var(--text-xs); color: var(--color-text-muted); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }
.survey-date { white-space: nowrap; color: var(--color-text-secondary); font-size: var(--text-xs); }
.survey-actions { white-space: nowrap; }

/* Status badges */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); white-space: nowrap; }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Action buttons */
.act-btn { padding: 5px 12px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); border: none; border-radius: var(--radius-md); cursor: pointer; transition: opacity var(--transition-fast); margin-right: 4px; }
.act-btn:hover { opacity: 0.85; }
.act-btn--edit { background: var(--color-primary-bg); color: var(--color-primary); }
.act-btn--publish { background: #D1FAE5; color: #047857; }
.act-btn--view { background: var(--color-primary-bg); color: var(--color-primary); }
.act-btn--distribute { background: #FEF3C7; color: #92400E; }
.act-btn--archive { background: var(--color-gray-100); color: var(--color-text-secondary); }
.act-btn--clone { background: var(--color-gray-100); color: var(--color-text-secondary); }
.act-btn--delete { background: #FEE2E2; color: #B91C1C; }

/* Loading / Empty */
.loading-state { text-align: center; padding: var(--space-2xl); color: var(--color-text-secondary); }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); text-align: center; }
.empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-lg); }

/* Pagination */
.pagination-bar { display: flex; align-items: center; justify-content: center; gap: var(--space-md); margin-top: var(--space-lg); }
.page-btn { padding: 8px 16px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.page-btn:hover:not(:disabled) { background: var(--color-gray-50); }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { font-size: var(--text-sm); color: var(--color-text-secondary); }
.size-select { padding: 6px 8px; font-size: var(--text-xs); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); cursor: pointer; }

/* Buttons */
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; transition: opacity var(--transition-fast), transform var(--transition-fast); }
.btn-primary svg { width: 16px; height: 16px; }
.btn-primary:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; z-index: var(--z-modal); display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); }
.modal { background: var(--color-white); border-radius: var(--radius-xl); box-shadow: var(--shadow-xl); width: 90%; max-width: 480px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-lg) var(--space-lg) 0; }
.modal-title { font-family: var(--font-heading); font-size: var(--text-xl); font-weight: 600; margin: 0; }
.modal-close { display: flex; align-items: center; justify-content: center; min-width: 44px; min-height: 44px; padding: 0; background: none; border: none; border-radius: var(--radius-md); cursor: pointer; }
.modal-close svg { width: 18px; height: 18px; }
.modal-close:hover { background: var(--color-gray-100); }
.modal-body { padding: var(--space-lg); display: flex; flex-direction: column; gap: var(--space-md); }
.modal-footer { display: flex; justify-content: flex-end; gap: var(--space-sm); padding: 0 var(--space-lg) var(--space-lg); }

.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-secondary); }
.form-error { font-size: var(--text-xs); color: var(--color-danger); margin: 0; }
.form-checkbox { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.form-checkbox input[type="checkbox"] { width: 16px; height: 16px; cursor: pointer; }
.required { color: var(--color-danger); }
.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.textarea { resize: vertical; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 150ms; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .survey-list { padding: var(--space-lg) var(--space-md); }
  .survey-title-cell { max-width: 180px; }
}
</style>
