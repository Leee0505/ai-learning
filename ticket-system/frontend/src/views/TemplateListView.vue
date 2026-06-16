<template>
  <div class="template-list">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Reply Templates</h1>
        <p class="page-subtitle">Manage quick-reply templates for faster ticket responses</p>
      </div>
      <button class="btn-primary" @click="openCreate">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        New Template
      </button>
    </header>

    <!-- Table -->
    <div class="table-card">
      <div v-if="loading" class="table-loading">Loading...</div>
      <div v-else-if="templates.length === 0" class="table-empty">
        <p>No templates yet — create your first one</p>
      </div>
      <table v-else class="tmpl-table">
        <thead>
          <tr>
            <th>Title</th><th>Category</th><th>Content Preview</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in templates" :key="t.id">
            <td class="tmpl-title">{{ t.title }}</td>
            <td><span class="badge badge--cat">{{ t.category }}</span></td>
            <td class="tmpl-preview">{{ truncate(t.content, 80) }}</td>
            <td class="tmpl-actions">
              <button class="action-btn" @click="openEdit(t)">Edit</button>
              <button class="action-btn action-btn--danger" @click="handleDelete(t)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create / Edit Dialog -->
    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog-card">
        <h2 class="dialog-title">{{ editingId ? 'Edit' : 'New' }} Template</h2>
        <div class="dialog-field">
          <label for="tmpl-title" class="dialog-label">Title</label>
          <input id="tmpl-title" v-model="form.title" class="dialog-input" placeholder="e.g. Request screenshot" />
        </div>
        <div class="dialog-field">
          <label for="tmpl-category" class="dialog-label">Category</label>
          <select id="tmpl-category" v-model="form.category" class="dialog-input">
            <option value="GENERAL">General</option>
            <option value="BUG">Bug</option>
            <option value="FEATURE_REQUEST">Feature Request</option>
            <option value="GENERAL_QUESTION">General Question</option>
            <option value="ACCOUNT_ISSUE">Account Issue</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div class="dialog-field">
          <label for="tmpl-content" class="dialog-label">Content (Markdown)</label>
          <textarea id="tmpl-content" v-model="form.content" class="dialog-textarea" rows="6"
                    placeholder="Template content... Markdown supported"></textarea>
        </div>
        <div class="dialog-actions">
          <button class="dialog-cancel" @click="closeDialog">Cancel</button>
          <button class="dialog-save" :disabled="!form.title.trim() || !form.content.trim() || saving" @click="handleSave">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTemplates, createTemplate, updateTemplate, deleteTemplate } from '@/api/templates'

const templates = ref([])
const loading = ref(true)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)

const form = reactive({ title: '', category: 'GENERAL', content: '' })

async function fetchList() {
  loading.value = true
  try {
    const { data } = await listTemplates()
    if (data.code === 200) templates.value = data.data
  } finally { loading.value = false }
}

function openCreate() { editingId.value = null; resetForm(); showDialog.value = true }
function openEdit(t) { editingId.value = t.id; form.title = t.title; form.category = t.category; form.content = t.content; showDialog.value = true }
function closeDialog() { showDialog.value = false; editingId.value = null }
function resetForm() { form.title = ''; form.category = 'GENERAL'; form.content = '' }

async function handleSave() {
  saving.value = true
  try {
    const payload = { title: form.title.trim(), category: form.category, content: form.content.trim() }
    const { data } = editingId.value
      ? await updateTemplate(editingId.value, payload)
      : await createTemplate(payload)
    if (data.code === 200) {
      ElMessage.success(editingId.value ? 'Template updated' : 'Template created')
      closeDialog()
      await fetchList()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Save failed')
  } finally { saving.value = false }
}

async function handleDelete(t) {
  try {
    await ElMessageBox.confirm(`Delete template "${t.title}"?`, 'Confirm Delete', {
      confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning'
    })
  } catch { return /* cancelled */ }
  try {
    const { data } = await deleteTemplate(t.id)
    if (data.code === 200) { ElMessage.success('Template deleted'); await fetchList() }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Delete failed')
  }
}

function truncate(text, max) { return text && text.length > max ? text.slice(0, max) + '...' : text }

onMounted(fetchList)
</script>

<style scoped>
.template-list { max-width: 900px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--space-lg); }
.page-title { margin: 0 0 var(--space-xs); font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); }
.page-subtitle { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

.btn-primary {
  display: inline-flex; align-items: center; gap: var(--space-sm);
  padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body);
  color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none; border-radius: var(--radius-md); cursor: pointer;
}
.btn-primary svg { width: 16px; height: 16px; }
.btn-primary:hover { opacity: 0.92; }

.table-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow-x: auto; }
.table-loading, .table-empty { text-align: center; padding: var(--space-2xl); color: var(--color-text-secondary); }

.tmpl-table { width: 100%; border-collapse: collapse; }
.tmpl-table th { text-align: left; padding: 12px 16px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-heading); color: var(--color-text-secondary); background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); text-transform: uppercase; letter-spacing: 0.05em; }
.tmpl-table td { padding: 12px 16px; font-size: var(--text-sm); color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); }
.tmpl-title { font-weight: 600; }
.tmpl-preview { color: var(--color-text-secondary); max-width: 350px; font-family: var(--font-mono); font-size: var(--text-xs); }
.tmpl-actions { white-space: nowrap; }

.badge { display: inline-block; padding: 3px 10px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); border-radius: var(--radius-full); }
.badge--cat { background: var(--color-gray-100); color: var(--color-text-secondary); }

.action-btn { padding: 5px 12px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: 1px solid transparent; border-radius: var(--radius-sm); cursor: pointer; margin-right: 4px; }
.action-btn:hover { border-color: var(--color-primary); }
.action-btn--danger { color: var(--color-danger); background: #FEF2F2; }
.action-btn--danger:hover { border-color: var(--color-danger); }

/* Dialog */
.dialog-overlay { position: fixed; inset: 0; z-index: var(--z-modal); background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; }
.dialog-card { background: var(--color-white); border-radius: var(--radius-lg); padding: var(--space-xl); width: 560px; max-width: 90vw; box-shadow: var(--shadow-xl); }
.dialog-title { margin: 0 0 var(--space-lg); font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); }
.dialog-field { margin-bottom: var(--space-md); }
.dialog-label { display: block; margin-bottom: var(--space-xs); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.dialog-input, .dialog-textarea { width: 100%; padding: 10px 12px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-gray-50); border: 1.5px solid var(--color-gray-200); border-radius: var(--radius-md); outline: none; box-sizing: border-box; }
.dialog-input:focus, .dialog-textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124,58,237,0.12); }
.dialog-textarea { resize: vertical; font-family: var(--font-mono); }
.dialog-actions { display: flex; justify-content: flex-end; gap: var(--space-sm); margin-top: var(--space-lg); }
.dialog-cancel { padding: 8px 16px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.dialog-cancel:hover { background: var(--color-gray-50); }
.dialog-save { padding: 8px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; }
.dialog-save:hover:not(:disabled) { opacity: 0.9; }
.dialog-save:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
