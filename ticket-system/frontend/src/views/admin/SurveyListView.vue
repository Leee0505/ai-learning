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

    <!-- Loading skeleton -->
    <div v-if="store.loading && store.templates.length === 0" class="survey-grid">
      <div v-for="i in 4" :key="i" class="survey-card skeleton-card">
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-bar"></div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!store.loading && store.templates.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon" aria-hidden="true">
        <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
      </svg>
      <p class="empty-title">No survey templates yet</p>
      <p class="empty-desc">Create your first template to start collecting feedback</p>
      <button class="btn-primary" @click="openCreateDialog">Create Template</button>
    </div>

    <!-- Template Cards -->
    <div v-else class="survey-grid">
      <div v-for="t in store.templates" :key="t.id" class="survey-card">
        <div class="survey-card-header">
          <h3 class="survey-card-title">{{ t.title }}</h3>
          <span :class="['status-badge', 'status-' + t.status.toLowerCase()]">{{ t.status }}</span>
        </div>
        <p v-if="t.description" class="survey-card-desc">{{ t.description }}</p>
        <div class="survey-card-meta">
          <span>v{{ t.version }}</span>
          <span>{{ t.pages?.length || 0 }} pages</span>
          <span>{{ countQuestions(t) }} questions</span>
        </div>
        <div class="survey-card-actions">
          <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--edit" title="Edit" @click="editTemplate(t.id)" aria-label="Edit template">Edit</button>
          <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--publish" title="Publish" @click="publishTemplate(t.id)" aria-label="Publish template">Publish</button>
          <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--view" title="View" @click="viewTemplate(t.id)" aria-label="View template">View</button>
          <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--distribute" title="Distribute" @click="distributeTemplate(t)" aria-label="Distribute survey">Distribute</button>
          <button v-if="t.status === 'PUBLISHED'" class="act-btn act-btn--archive" title="Archive" @click="archiveTemplate(t.id)" aria-label="Archive template">Archive</button>
          <button v-if="t.status === 'ARCHIVED'" class="act-btn act-btn--view" title="View" @click="viewTemplate(t.id)" aria-label="View template">View</button>
          <button v-if="t.status === 'ARCHIVED'" class="act-btn act-btn--clone" title="Clone" @click="cloneTemplate(t.id)" aria-label="Clone template">Clone</button>
          <button v-if="t.status === 'DRAFT'" class="act-btn act-btn--delete" title="Delete" @click="handleDelete(t)" aria-label="Delete template">Delete</button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="store.totalPages > 1" class="survey-pagination">
      <button :disabled="store.page <= 1" class="page-btn" @click="store.fetchTemplates(store.page - 1)">Previous</button>
      <span class="page-info">Page {{ store.page }} of {{ store.totalPages }} ({{ store.total }} total)</span>
      <select class="size-select" :value="store.size" @change="store.size = Number($event.target.value); store.fetchTemplates(1)">
        <option :value="12">12 / page</option>
        <option :value="24">24 / page</option>
        <option :value="48">48 / page</option>
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
import { updateTemplateApi } from '@/api/survey'

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
    for (const section of page.sections || []) {
      count += (section.questions || []).length
    }
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
function viewTemplate(id) { router.push(`/admin/surveys/${id}`) }

async function publishTemplate(id) {
  try {
    await ElMessageBox.confirm('Publish this template? Once published, instances can be distributed.', 'Publish Template', { confirmButtonText: 'Publish', type: 'info' })
  } catch { return }
  const { data } = await updateTemplateApi(id, { status: 'PUBLISHED' })
  if (data.code === 200) { ElMessage.success('Published'); store.fetchTemplates() }
  else ElMessage.error(data.message)
}

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
</script>

<style scoped>
.survey-list { max-width: 1280px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

/* Grid */
.survey-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: var(--space-lg); }

/* Card */
.survey-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); box-shadow: var(--shadow-sm); transition: box-shadow var(--transition-fast), transform var(--transition-fast); }
.survey-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.survey-card-header { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-sm); margin-bottom: var(--space-sm); }
.survey-card-title { font-size: var(--text-base); font-weight: 600; color: var(--color-text-primary); margin: 0; flex: 1; }
.survey-card-desc { font-size: var(--text-sm); color: var(--color-text-secondary); margin: 0 0 var(--space-md); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.survey-card-meta { display: flex; gap: var(--space-md); font-size: var(--text-xs); color: var(--color-text-muted); margin-bottom: var(--space-md); }
.survey-card-actions { display: flex; gap: var(--space-xs); flex-wrap: wrap; }

/* Status badges */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Action buttons */
.act-btn { padding: 6px 14px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); border: none; border-radius: var(--radius-md); cursor: pointer; transition: opacity var(--transition-fast); }
.act-btn:hover { opacity: 0.85; }
.act-btn--edit { background: var(--color-primary-bg); color: var(--color-primary); }
.act-btn--publish { background: #D1FAE5; color: #047857; }
.act-btn--view { background: var(--color-primary-bg); color: var(--color-primary); }
.act-btn--distribute { background: #FEF3C7; color: #92400E; }
.act-btn--archive { background: var(--color-gray-100); color: var(--color-text-secondary); }
.act-btn--clone { background: var(--color-gray-100); color: var(--color-text-secondary); }
.act-btn--delete { background: #FEE2E2; color: #B91C1C; }

/* Skeleton */
.skeleton-card { display: flex; flex-direction: column; gap: var(--space-sm); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
.skeleton-title { height: 20px; width: 60%; }
.skeleton-text { height: 14px; width: 90%; }
.skeleton-bar { height: 32px; width: 100%; margin-top: var(--space-sm); }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* Empty state */
.empty-state { display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); text-align: center; }
.empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-lg); }

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

.survey-pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-md); margin-top: var(--space-xl); }
.page-btn { padding: 8px 16px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.page-btn:hover:not(:disabled) { background: var(--color-gray-50); }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { font-size: var(--text-sm); color: var(--color-text-secondary); }
.size-select { padding: 6px 8px; font-size: var(--text-xs); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); cursor: pointer; }

@media (max-width: 768px) {
  .survey-list { padding: var(--space-lg) var(--space-md); }
  .survey-grid { grid-template-columns: 1fr; }
}
</style>
