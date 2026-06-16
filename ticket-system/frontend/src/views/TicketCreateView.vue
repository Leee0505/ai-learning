<template>
  <div class="create-ticket">
    <!-- Header -->
    <div class="create-header">
      <button class="create-back" @click="$router.push('/tickets')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12,19 5,12 12,5" />
        </svg>
        Back to Tickets
      </button>
      <h1 class="create-title">Create New Ticket</h1>
    </div>

    <!-- Form Card -->
    <div class="create-card">
      <form @submit.prevent="handleSubmit" novalidate>
        <!-- Title -->
        <div class="create-field">
          <label for="title-input" class="create-label">Title <span class="create-required">*</span></label>
          <input
            id="title-input"
            v-model="form.title"
            type="text"
            class="create-input"
            :class="{ 'create-input--error': errors.title }"
            placeholder="Brief summary of your issue"
            maxlength="255"
            :disabled="loading"
          />
          <p v-if="errors.title" class="create-error">{{ errors.title }}</p>
        </div>

        <!-- Category + Priority row -->
        <div class="create-row">
          <div class="create-field create-field--half">
            <label for="category-select" class="create-label">Category <span class="create-required">*</span></label>
            <select
              id="category-select"
              v-model="form.category"
              class="create-select"
              :class="{ 'create-select--error': errors.category }"
              :disabled="loading"
            >
              <option value="">Select category</option>
              <option value="BUG">Bug</option>
              <option value="FEATURE_REQUEST">Feature Request</option>
              <option value="GENERAL_QUESTION">General Question</option>
              <option value="ACCOUNT_ISSUE">Account Issue</option>
              <option value="OTHER">Other</option>
            </select>
            <p v-if="errors.category" class="create-error">{{ errors.category }}</p>
          </div>

          <div class="create-field create-field--half">
            <label for="priority-select" class="create-label">Priority <span class="create-required">*</span></label>
            <select
              id="priority-select"
              v-model="form.priority"
              class="create-select"
              :class="{ 'create-select--error': errors.priority }"
              :disabled="loading"
            >
              <option value="">Select priority</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
            <p v-if="errors.priority" class="create-error">{{ errors.priority }}</p>
          </div>
        </div>

        <!-- Description -->
        <div class="create-field">
          <label for="desc-input" class="create-label">Description <span class="create-required">*</span></label>
          <div ref="vditorRef" class="create-vditor-container" :class="{ 'create-vditor-container--error': errors.description }"></div>
          <p v-if="errors.description" class="create-error">{{ errors.description }}</p>
        </div>

        <!-- Attachments -->
        <div class="create-field">
          <label class="create-label">Attachments</label>
          <div
            class="create-upload-zone"
            :class="{ 'create-upload-zone--dragover': isDragOver }"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="handleDrop"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="create-upload-icon" aria-hidden="true">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17,8 12,3 7,8" /><line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            <p class="create-upload-text">Drag & drop files here, or click to browse</p>
            <p class="create-upload-hint">Max 10 MB per file. Images, documents, archives, videos.</p>
            <input
              type="file"
              ref="fileInput"
              class="create-upload-input"
              @change="handleFileSelect"
              multiple
              :disabled="loading"
            />
          </div>
          <ul v-if="selectedFiles.length > 0" class="create-file-list">
            <li v-for="(f, i) in selectedFiles" :key="i" class="create-file-item">
              <span class="create-file-name">{{ f.name }}</span>
              <span class="create-file-size">{{ formatSize(f.size) }}</span>
              <button type="button" class="create-file-remove" @click="removeFile(i)" title="Remove">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </li>
          </ul>
        </div>

        <!-- Error Banner -->
        <div v-if="serverError" class="create-server-error" role="alert">{{ serverError }}</div>

        <!-- Buttons -->
        <div class="create-actions">
          <button type="submit" class="create-submit" :disabled="loading">
            <span v-if="!loading">Create Ticket</span>
            <span v-else>Creating...</span>
          </button>
          <button type="button" class="create-cancel" @click="$router.push('/tickets')" :disabled="loading">
            Cancel
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTicketStore } from '@/stores/tickets'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const router = useRouter()
const store = useTicketStore()

const loading = ref(false)
const isDragOver = ref(false)
const serverError = ref('')
const fileInput = ref(null)
const selectedFiles = ref([])

const vditorRef = ref(null)
const vditorInstance = ref(null)

const form = reactive({ title: '', category: '', priority: 'MEDIUM' })
const errors = reactive({ title: '', category: '', priority: '', description: '' })

onMounted(async () => {
  await nextTick()
  if (vditorRef.value) {
    vditorInstance.value = new Vditor(vditorRef.value, {
      mode: 'ir',
      height: 200,
      placeholder: 'Detailed description of your issue... (Markdown supported)',
      toolbar: ['bold', 'italic', 'strikethrough', '|', 'quote', 'list', 'ordered-list', 'code', '|', 'link', '|', 'undo', 'redo'],
      cache: { enable: false }
    })
  }
})

onUnmounted(() => {
  if (vditorInstance.value) vditorInstance.value.destroy()
})

function clearErrors() {
  errors.title = ''; errors.category = ''; errors.priority = ''; errors.description = ''
  serverError.value = ''
}

function validate() {
  clearErrors()
  let valid = true
  if (!form.title.trim()) { errors.title = 'Title is required'; valid = false }
  if (!form.category) { errors.category = 'Category is required'; valid = false }
  if (!form.priority) { errors.priority = 'Priority is required'; valid = false }
  if (!vditorInstance.value?.getValue()?.trim()) { errors.description = 'Description is required'; valid = false }
  return valid
}

async function handleSubmit() {
  if (!validate()) return
  loading.value = true
  try {
    const { data } = await store.createNewTicket({
      title: form.title.trim(),
      category: form.category,
      priority: form.priority,
      description: vditorInstance.value.getValue().trim()
    })
    if (data.code === 200) {
      const ticketId = data.data.id
      for (const file of selectedFiles.value) {
        try { await store.uploadFile(ticketId, file) } catch {}
      }
      ElMessage.success('Ticket created successfully')
      router.push(`/tickets/${ticketId}`)
    } else {
      serverError.value = data.message || 'Failed to create ticket'
    }
  } catch (e) {
    serverError.value = e.response?.data?.message || 'Network error, please try again'
  } finally {
    loading.value = false
  }
}

function handleFileSelect(e) {
  const files = Array.from(e.target.files || [])
  for (const f of files) {
    if (f.size > 10 * 1024 * 1024) {
      ElMessage.warning(`File ${f.name} exceeds 10MB limit and was skipped`)
    } else {
      if (!selectedFiles.value.find(sf => sf.name === f.name && sf.size === f.size)) {
        selectedFiles.value.push(f)
      }
    }
  }
  if (fileInput.value) fileInput.value.value = ''
}

function handleDrop(e) {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  const fakeEvent = { target: { files } }
  handleFileSelect(fakeEvent)
}

function removeFile(i) { selectedFiles.value.splice(i, 1) }

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.create-ticket { max-width: 800px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.create-header { margin-bottom: var(--space-lg); }

.create-back {
  display: inline-flex; align-items: center; gap: var(--space-xs); padding: 0; border: none; background: none;
  color: var(--color-primary); font-size: var(--text-sm); font-family: var(--font-body); cursor: pointer;
  margin-bottom: var(--space-sm);
}
.create-back svg { width: 16px; height: 16px; }
.create-back:hover { color: var(--color-primary-dark); }

.create-title { margin: 0; font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); }

.create-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-xl); box-shadow: var(--shadow-sm); }

/* Fields */
.create-field { margin-bottom: var(--space-lg); }
.create-field--half { flex: 1; margin-bottom: 0; }

.create-row { display: flex; gap: var(--space-lg); margin-bottom: var(--space-lg); }

.create-label { display: block; margin-bottom: var(--space-xs); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.create-required { color: var(--color-danger); }

.create-input, .create-select {
  width: 100%; padding: 10px 12px; font-size: var(--text-base); font-family: var(--font-body);
  color: var(--color-text-primary); background: var(--color-gray-50); border: 1.5px solid var(--color-gray-200);
  border-radius: var(--radius-md); outline: none; box-sizing: border-box;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.create-input:focus, .create-select:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12); }
.create-input--error, .create-select--error { border-color: var(--color-danger); }
.create-select { cursor: pointer; }

.create-vditor-container {
  border: 1.5px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.create-vditor-container:focus-within { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12); }
.create-vditor-container--error { border-color: var(--color-danger); }
.create-vditor-container--error:focus-within { box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15); }

.create-error { margin: var(--space-xs) 0 0; font-size: var(--text-xs); color: var(--color-danger); }

/* Upload */
.create-upload-zone {
  position: relative; border: 2px dashed var(--color-gray-200); border-radius: var(--radius-lg);
  padding: var(--space-xl); text-align: center; cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}
.create-upload-zone:hover, .create-upload-zone--dragover { border-color: var(--color-primary); background: var(--color-primary-bg); }
.create-upload-icon { width: 32px; height: 32px; color: var(--color-text-muted); margin-bottom: var(--space-sm); }
.create-upload-text { margin: 0; font-size: var(--text-sm); color: var(--color-text-primary); }
.create-upload-hint { margin: var(--space-xs) 0 0; font-size: var(--text-xs); color: var(--color-text-muted); }
.create-upload-input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

.create-file-list { list-style: none; margin: var(--space-sm) 0 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-xs); }
.create-file-item { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-xs) var(--space-sm); background: var(--color-gray-50); border-radius: var(--radius-sm); font-size: var(--text-sm); }
.create-file-name { flex: 1; color: var(--color-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.create-file-size { color: var(--color-text-muted); white-space: nowrap; }
.create-file-remove { display: flex; align-items: center; padding: 2px; border: none; background: none; color: var(--color-text-muted); cursor: pointer; border-radius: var(--radius-sm); }
.create-file-remove:hover { color: var(--color-danger); background: #FEE2E2; }
.create-file-remove svg { width: 14px; height: 14px; }

/* Errors */
.create-server-error { padding: var(--space-sm) var(--space-md); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-md); color: var(--color-danger); font-size: var(--text-sm); margin-bottom: var(--space-lg); }

/* Actions */
.create-actions { display: flex; gap: var(--space-md); }
.create-submit {
  display: inline-flex; align-items: center; justify-content: center; padding: 12px 24px;
  font-size: var(--text-base); font-weight: 600; font-family: var(--font-body);
  color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none; border-radius: var(--radius-md); cursor: pointer;
  transition: opacity var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}
.create-submit:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35); }
.create-submit:disabled { opacity: 0.7; cursor: not-allowed; }

.create-cancel {
  padding: 12px 24px; font-size: var(--text-base); font-weight: 500; font-family: var(--font-body);
  color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md); cursor: pointer; transition: background var(--transition-fast);
}
.create-cancel:hover:not(:disabled) { background: var(--color-gray-50); }

@media (max-width: 640px) {
  .create-ticket { padding: var(--space-lg) var(--space-md); }
  .create-row { flex-direction: column; }
  .create-card { padding: var(--space-lg); }
}
</style>