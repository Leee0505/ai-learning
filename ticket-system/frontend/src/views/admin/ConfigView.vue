<template>
  <div class="config-page">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Configuration</h1>
        <p class="page-subtitle">Manage custom ticket fields and SLA rules</p>
      </div>
    </header>

    <div class="config-tabs" role="tablist">
      <button
        role="tab"
        :aria-selected="activeTab === 'fields'"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'fields' }"
        @click="activeTab = 'fields'"
      >Custom Fields</button>
      <button
        role="tab"
        :aria-selected="activeTab === 'sla'"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'sla' }"
        @click="activeTab = 'sla'"
      >SLA Rules</button>
    </div>

    <!-- ── Custom Fields Tab ── -->
    <div v-if="activeTab === 'fields'" role="tabpanel" class="tab-panel">
      <div v-if="store.loading && store.fields.length === 0" class="skeleton-list">
        <div v-for="i in 4" :key="i" class="skeleton-row">
          <div class="skeleton" style="width:24px;height:24px"></div>
          <div class="skeleton" style="flex:1;height:20px"></div>
          <div class="skeleton" style="width:100px;height:20px"></div>
          <div class="skeleton" style="width:48px;height:32px"></div>
        </div>
      </div>

      <div v-else-if="!store.loading && store.fields.length === 0" class="empty-state">
        <p class="empty-title">No custom fields configured</p>
        <p class="empty-desc">Add your first field to enhance ticket forms</p>
        <button class="btn-primary" @click="openAddDialog">Add Field</button>
      </div>

      <div v-else class="card">
        <div class="card-header">
          <span class="card-count">{{ store.fields.length }} field{{ store.fields.length !== 1 ? 's' : '' }}</span>
          <button class="btn-primary" @click="openAddDialog">+ Add Field</button>
        </div>
        <div class="field-list">
          <template v-for="field in store.fields" :key="field.id">
            <div
              class="field-row"
              :class="{ 'field-row--selected': selectedField?.id === field.id }"
              @click="selectedField = selectedField?.id === field.id ? null : field"
            >
              <span class="drag-handle" title="Drag to reorder" aria-label="Drag to reorder">
              <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><circle cx="9" cy="5" r="2"/><circle cx="15" cy="5" r="2"/><circle cx="9" cy="12" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="9" cy="19" r="2"/><circle cx="15" cy="19" r="2"/></svg>
            </span>
              <div class="field-info">
                <span class="field-name">{{ field.name }}</span>
                <span class="field-key">{{ field.fieldKey }}</span>
              </div>
              <span class="badge badge--type">{{ field.fieldType }}</span>
              <el-switch
                :model-value="field.active"
                @change="(val) => handleToggleActive(field, val)"
                @click.stop
                :aria-label="`${field.active ? 'Disable' : 'Enable'} ${field.name}`"
              />
              <button
                class="act-btn act-btn--edit"
                title="Edit"
                @click.stop="openEditDialog(field)"
                aria-label="Edit field"
              ><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
              <button
                class="act-btn act-btn--delete"
                title="Delete"
                @click.stop="handleDelete(field)"
                aria-label="Delete field"
              ><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
            </div>

            <!-- Inline preview below selected field -->
            <div v-if="selectedField?.id === field.id" class="field-preview">
              <p class="preview-label">Preview — how this field appears in the ticket form</p>
              <div class="preview-box">
                <label class="preview-field-label">{{ selectedField.name }}
                  <span v-if="selectedField.required" class="required">*</span>
                </label>
                <input v-if="selectedField.fieldType === 'TEXT'" type="text" class="input" disabled :placeholder="`Enter ${selectedField.name.toLowerCase()}`" />
                <select v-else-if="selectedField.fieldType === 'SINGLE_SELECT'" class="input" disabled>
                  <option v-for="item in parseSelectOptions(selectedField.options)" :key="item">{{ item }}</option>
                </select>
                <input v-else-if="selectedField.fieldType === 'NUMBER'" type="number" class="input" disabled :placeholder="`Enter ${selectedField.name.toLowerCase()}`" />
                <input v-else-if="selectedField.fieldType === 'DATE'" type="date" class="input" disabled />
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- ── SLA Rules Tab ── -->
    <div v-if="activeTab === 'sla'" role="tabpanel" class="tab-panel">
      <div class="card">
        <table class="sla-table">
          <thead>
            <tr>
              <th>Priority</th><th>Response Time</th><th>Resolution Time</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in store.slaRules" :key="rule.id">
              <td><span class="badge" :class="'badge--' + rule.priority.toLowerCase()">{{ rule.priority }}</span></td>
              <td>{{ formatDuration(rule.responseMinutes) }}</td>
              <td>{{ formatDuration(rule.resolutionMinutes) }}</td>
              <td>
                <button class="act-btn act-btn--edit" title="Edit SLA" @click="openSlaEdit(rule)" aria-label="Edit SLA rule"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── Add/Edit Field Dialog ── -->
    <transition name="modal-fade">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingField ? 'Edit Field' : 'Add Field' }}</h2>
            <button class="modal-close" @click="dialogVisible = false" aria-label="Close"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="field-name">Display Name <span class="required">*</span></label>
              <input id="field-name" v-model="form.name" type="text" class="input" placeholder="e.g. Environment" maxlength="100" />
            </div>
            <div class="form-group">
              <label class="form-label" for="field-key">Field Key <span class="required">*</span></label>
              <input id="field-key" v-model="form.fieldKey" type="text" class="input" placeholder="e.g. environment" maxlength="50" :disabled="!!editingField" />
              <p class="form-hint">Lowercase letters, numbers, and underscores only. Cannot change after creation.</p>
            </div>
            <div class="form-group">
              <label class="form-label" for="field-type">Type</label>
              <select id="field-type" v-model="form.fieldType" class="input">
                <option value="TEXT">Text</option>
                <option value="SINGLE_SELECT">Single Select</option>
                <option value="NUMBER">Number</option>
                <option value="DATE">Date</option>
              </select>
            </div>
            <div v-if="form.fieldType === 'SINGLE_SELECT'" class="form-group">
              <label class="form-label">Options (comma-separated)</label>
              <input v-model="optionsText" type="text" class="input" placeholder="Production, Staging, Development" />
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input v-model="form.required" type="checkbox" />
                Required field
              </label>
            </div>
            <p v-if="dialogError" class="form-error">{{ dialogError }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="dialogVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!formValid" @click="handleSaveField">
              {{ editingField ? 'Save Changes' : 'Create Field' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ── Edit SLA Dialog ── -->
    <transition name="modal-fade">
      <div v-if="slaDialogVisible" class="modal-overlay" @click.self="slaDialogVisible = false">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">Edit SLA — {{ slaForm.priority }}</h2>
            <button class="modal-close" @click="slaDialogVisible = false" aria-label="Close"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Response Time</label>
              <div class="duration-input-row">
                <div class="duration-field">
                  <input id="sla-resp-h" v-model.number="slaForm.respHours" type="number" class="input" min="0" max="720" placeholder="0" />
                  <span class="duration-label">hours</span>
                </div>
                <div class="duration-field">
                  <input id="sla-resp-m" v-model.number="slaForm.respMins" type="number" class="input" min="0" max="59" placeholder="0" />
                  <span class="duration-label">minutes</span>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Resolution Time</label>
              <div class="duration-input-row">
                <div class="duration-field">
                  <input id="sla-res-h" v-model.number="slaForm.resHours" type="number" class="input" min="0" max="1440" placeholder="0" />
                  <span class="duration-label">hours</span>
                </div>
                <div class="duration-field">
                  <input id="sla-res-m" v-model.number="slaForm.resMins" type="number" class="input" min="0" max="59" placeholder="0" />
                  <span class="duration-label">minutes</span>
                </div>
              </div>
            </div>
            <p v-if="slaDialogError" class="form-error">{{ slaDialogError }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="slaDialogVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!slaFormValid" @click="handleSaveSla">Save Changes</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { messageSuccess, messageError, messageWarning, messageInfo } from '@/utils/message'
import { useConfigStore } from '@/stores/config'

const store = useConfigStore()

const activeTab = ref('fields')
const selectedField = ref(null)

// Field dialog
const dialogVisible = ref(false)
const editingField = ref(null)
const form = ref({ name: '', fieldKey: '', fieldType: 'TEXT', required: false })
const optionsText = ref('')
const dialogError = ref('')

const formValid = computed(() => {
  if (!form.value.name.trim()) return false
  if (!editingField.value && !form.value.fieldKey.trim()) return false
  if (form.value.fieldType === 'SINGLE_SELECT' && !optionsText.value.trim()) return false
  return true
})

// SLA dialog
const slaDialogVisible = ref(false)
const slaForm = ref({ id: null, priority: '', respHours: 0, respMins: 0, resHours: 0, resMins: 0 })
const slaDialogError = ref('')
const slaFormValid = computed(() => {
  const respTotal = slaForm.value.respHours * 60 + slaForm.value.respMins
  const resTotal = slaForm.value.resHours * 60 + slaForm.value.resMins
  return respTotal > 0 && resTotal > 0 && respTotal < resTotal
})

onMounted(() => {
  store.fetchFields()
  store.fetchSla()
})

function parseSelectOptions(optionsJson) {
  try {
    const obj = JSON.parse(optionsJson || '{}')
    return obj.items || []
  } catch { return [] }
}

// Format minutes to human-readable: 90 → "1h 30min", 60 → "1h", 30 → "30min"
function formatDuration(totalMinutes) {
  if (!totalMinutes || totalMinutes <= 0) return '0min'
  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  if (h === 0) return m + 'min'
  if (m === 0) return h + 'h'
  return h + 'h ' + m + 'min'
}

// ── Field Actions ──
function openAddDialog() {
  editingField.value = null
  form.value = { name: '', fieldKey: '', fieldType: 'TEXT', required: false }
  optionsText.value = ''
  dialogError.value = ''
  dialogVisible.value = true
}

function openEditDialog(field) {
  editingField.value = field
  form.value = {
    name: field.name,
    fieldKey: field.fieldKey,
    fieldType: field.fieldType,
    required: field.required
  }
  optionsText.value = parseSelectOptions(field.options).join(', ')
  dialogError.value = ''
  dialogVisible.value = true
}

async function handleSaveField() {
  dialogError.value = ''
  const payload = {
    name: form.value.name.trim(),
    fieldType: form.value.fieldType,
    required: form.value.required
  }
  if (form.value.fieldType === 'SINGLE_SELECT') {
    const items = optionsText.value.split(',').map(s => s.trim()).filter(Boolean)
    payload.options = JSON.stringify({ items })
  }
  if (!editingField.value) {
    payload.fieldKey = form.value.fieldKey.trim()
    const result = await store.createField(payload)
    if (result.code === 200) {
      dialogVisible.value = false
      messageSuccess('Field created')
    } else {
      dialogError.value = result.message || 'Create failed'
    }
  } else {
    const result = await store.updateField(editingField.value.id, payload)
    if (result.code === 200) {
      dialogVisible.value = false
      messageSuccess('Field updated')
    } else {
      dialogError.value = result.message || 'Update failed'
    }
  }
}

async function handleDelete(field) {
  try {
    await ElMessageBox.confirm(
      `Delete field "${field.name}"? This will remove the field from ticket forms.`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return }
  try {
    const result = await store.deleteField(field.id)
    if (result.code === 200) {
      if (selectedField.value?.id === field.id) selectedField.value = null
      messageSuccess('Field deleted')
    }
  } catch (e) {
    messageError(e.response?.data?.message || 'Failed to delete field')
  }
}

async function handleToggleActive(field, active) {
  try {
    const result = await store.updateField(field.id, { active })
    if (result.code !== 200) {
      messageError(result.message)
    }
  } catch (e) {
    messageError(e.response?.data?.message || 'Failed to update field')
  }
}

// ── SLA Actions ──
function openSlaEdit(rule) {
  slaForm.value = {
    id: rule.id,
    priority: rule.priority,
    respHours: Math.floor(rule.responseMinutes / 60),
    respMins: rule.responseMinutes % 60,
    resHours: Math.floor(rule.resolutionMinutes / 60),
    resMins: rule.resolutionMinutes % 60
  }
  slaDialogError.value = ''
  slaDialogVisible.value = true
}

async function handleSaveSla() {
  slaDialogError.value = ''
  const respTotal = slaForm.value.respHours * 60 + slaForm.value.respMins
  const resTotal = slaForm.value.resHours * 60 + slaForm.value.resMins
  try {
    await ElMessageBox.confirm(
      `Change SLA for ${slaForm.value.priority}: response ${formatDuration(respTotal)} / resolution ${formatDuration(resTotal)}?`,
      'Confirm SLA Update',
      { confirmButtonText: 'Update', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return }
  try {
    const result = await store.updateSla(slaForm.value.id, {
      responseMinutes: respTotal,
      resolutionMinutes: resTotal
    })
    if (result.code === 200) {
      slaDialogVisible.value = false
      messageSuccess('SLA updated')
    } else {
      slaDialogError.value = result.message || 'Update failed'
    }
  } catch (e) {
    messageError(e.response?.data?.message || 'Failed to update SLA')
  }
}
</script>

<style scoped>
/* ── Page Layout ── */
.config-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

/* ── Tabs ── */
.config-tabs { display: flex; gap: 0; border-bottom: 2px solid var(--color-gray-200); margin-bottom: var(--space-lg); }
.tab-btn { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -2px; cursor: pointer; transition: color 150ms, border-color 150ms; }
.tab-btn:hover { color: var(--color-primary); }
.tab-btn--active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.tab-panel { animation: fadeIn 200ms ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

/* ── Card ── */
.card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-gray-100); }
.card-count { font-size: var(--text-sm); color: var(--color-text-muted); }

/* ── Field List ── */
.field-row { display: flex; align-items: center; gap: var(--space-md); padding: 12px var(--space-lg); border-bottom: 1px solid var(--color-gray-100); cursor: pointer; transition: background var(--transition-fast); }
.field-row:hover { background: var(--color-primary-bg); }
.field-row--selected { background: #F5F3FF; }
.drag-handle { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; color: var(--color-gray-400); cursor: grab; user-select: none; }
.drag-handle svg { width: 20px; height: 20px; }
.drag-handle:active { cursor: grabbing; transform: scale(0.95); }
.field-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.field-name { font-weight: 500; font-size: var(--text-sm); color: var(--color-text-primary); }
.field-key { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); }
.badge--type { font-size: var(--text-xs); font-weight: 600; color: var(--color-primary-dark); background: var(--color-primary-bg); padding: 2px 8px; border-radius: var(--radius-full); }

/* Priority badges */
.badge--urgent { background: #FEE2E2; color: #991B1B; font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.badge--high { background: #FEF3C7; color: #92400E; font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.badge--medium { background: #DBEAFE; color: #1E40AF; font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.badge--low { background: #F3F4F6; color: #374151; font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }

/* ── Field Preview ── */
.field-preview { background: #F5F3FF; padding: var(--space-md) var(--space-lg) var(--space-md) calc(var(--space-lg) + 44px + var(--space-md)); border-bottom: 1px solid var(--color-gray-100); animation: fadeIn 200ms ease; }
.preview-label { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0 0 var(--space-sm); }
.preview-box { max-width: 360px; padding: var(--space-md); background: var(--color-gray-50); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); }
.preview-field-label { display: block; font-size: var(--text-sm); font-weight: 500; margin-bottom: 6px; color: var(--color-text-secondary); }

/* ── SLA Table ── */
.sla-table { width: 100%; border-collapse: collapse; }
.sla-table thead { background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); }
.sla-table th { padding: 12px 20px; font-size: var(--text-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-secondary); text-align: left; }
.sla-table td { padding: 12px 20px; font-size: var(--text-sm); color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); font-family: var(--font-mono); }

/* Action buttons */
.act-btn { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; background: none; border: none; border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; transition: color 150ms, background 150ms; }
.act-btn svg { width: 18px; height: 18px; }
.act-btn--edit:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.act-btn--delete:hover { color: var(--color-danger); background: #FEE2E2; }

/* ── Skeleton ── */
.skeleton-list { padding: var(--space-md); display: flex; flex-direction: column; gap: 10px; }
.skeleton-row { display: flex; align-items: center; gap: var(--space-md); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* ── Empty state ── */
.empty-state { display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); text-align: center; }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-lg); }

/* ── Modal ── */
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
.form-hint { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0; }
.form-error { font-size: var(--text-xs); color: var(--color-danger); margin: 0; }
.form-checkbox { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.form-checkbox input[type="checkbox"] { width: 16px; height: 16px; cursor: pointer; }
.required { color: var(--color-danger); }
.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }

/* Duration input: hours + minutes side by side */
.duration-input-row { display: flex; gap: var(--space-sm); }
.duration-field { flex: 1; display: flex; align-items: center; gap: 8px; }
.duration-field .input { width: 80px; }
.duration-label { font-size: var(--text-sm); color: var(--color-text-muted); white-space: nowrap; }

.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; }
.btn-primary:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124,58,237,0.35); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-secondary:hover { border-color: var(--color-primary); color: var(--color-primary); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 150ms; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .config-page { padding: var(--space-lg) var(--space-md); }
  .field-row { flex-wrap: wrap; padding: 12px var(--space-md); }
}
</style>
