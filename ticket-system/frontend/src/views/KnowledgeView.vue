<template>
  <div class="kb">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Knowledge Base</h1>
        <p class="page-subtitle">Frequently asked questions and documentation</p>
      </div>
      <button class="btn-primary" @click="openCreate">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        New Article
      </button>
    </header>

    <div class="kb-grid">
      <!-- Sidebar -->
      <aside class="kb-sidebar">
        <div class="kb-search-box">
          <input v-model="keyword" class="kb-search" placeholder="Search..." @keyup.enter="fetchList" />
        </div>
        <ul class="kb-cats">
          <li :class="{ 'kb-cat--active': selectedCat === '' }" @click="selectCat('')">All Categories</li>
          <li v-for="cat in categories" :key="cat" :class="{ 'kb-cat--active': selectedCat === cat }" @click="selectCat(cat)">{{ cat }}</li>
        </ul>
      </aside>

      <!-- Article list -->
      <div class="kb-main">
        <div v-if="loading" class="kb-loading">Loading...</div>
        <div v-else-if="articles.length === 0" class="kb-empty">No articles found</div>
        <div v-for="a in articles" :key="a.id" class="kb-article" :class="{ 'kb-article--open': openId === a.id }">
          <div class="kb-article-header" @click="toggleArticle(a.id)">
            <span class="kb-article-title">{{ a.title }}</span>
            <span class="kb-article-cat">{{ a.category }}</span>
          </div>
          <div v-if="openId === a.id" class="kb-article-body">
            <div class="kb-article-content" v-html="renderMarkdown(a.content)"></div>
            <div class="kb-article-actions">
              <button class="kb-insert-btn" @click="insertToReply(a.content)">Insert to Reply</button>
              <button v-if="canEdit(a)" class="kb-edit-btn" @click.stop="openEdit(a)">Edit</button>
              <button v-if="canEdit(a)" class="kb-delete-btn" @click.stop="handleDelete(a)">Delete</button>
              <span v-else class="sys-badge">System Default</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog-card">
        <h2 class="dialog-title">{{ editingId ? 'Edit' : 'New' }} Article</h2>
        <div class="dialog-field">
          <label class="dialog-label">Title</label>
          <input v-model="form.title" class="dialog-input" placeholder="Article title" />
        </div>
        <div class="dialog-field">
          <label class="dialog-label">Category</label>
          <select v-model="form.category" class="dialog-input">
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div class="dialog-field">
          <label class="dialog-label">Content (Markdown)</label>
          <textarea v-model="form.content" class="dialog-textarea" rows="10" placeholder="Write your article..."></textarea>
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
import { useAuthStore } from '@/stores/auth'
import { renderMarkdown } from '@/utils/markdown'
import request from '@/api/request'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const canEdit = (a) => a.tenantId !== null || authStore.user?.tenantId == null

const router = useRouter()
const articles = ref([])
const loading = ref(true)
const keyword = ref('')
const selectedCat = ref('')
const openId = ref(null)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)
const form = reactive({ title: '', category: 'GENERAL', content: '' })

const categories = ['GENERAL', 'BUG', 'FEATURE_REQUEST', 'GENERAL_QUESTION', 'ACCOUNT_ISSUE', 'OTHER']

async function fetchList() {
  loading.value = true
  try {
    const { data } = await request.get('/knowledge', { params: { keyword: keyword.value || undefined, category: selectedCat.value || undefined } })
    if (data.code === 200) articles.value = data.data
  } catch { /* keep stale data */ }
  finally { loading.value = false }
}

function selectCat(cat) { selectedCat.value = cat; openId.value = null; fetchList() }

async function toggleArticle(id) {
  if (openId.value === id) { openId.value = null; return }
  try {
    const { data } = await request.get(`/knowledge/${id}`)
    if (data.code === 200) {
      openId.value = id
      // Update the article content with fresh data
      const idx = articles.value.findIndex(a => a.id === id)
      if (idx >= 0) articles.value[idx] = data.data
    }
  } catch { /* ignore */ }
}

function openCreate() { editingId.value = null; form.title = ''; form.category = 'GENERAL'; form.content = ''; showDialog.value = true }
function openEdit(a) { editingId.value = a.id; form.title = a.title; form.category = a.category; form.content = a.content; showDialog.value = true }
function closeDialog() { showDialog.value = false }

async function handleSave() {
  saving.value = true
  try {
    const payload = { title: form.title.trim(), category: form.category, content: form.content.trim() }
    const { data } = editingId.value
      ? await request.put(`/knowledge/${editingId.value}`, payload)
      : await request.post('/knowledge', payload)
    if (data.code === 200) {
      ElMessage.success(editingId.value ? 'Article updated' : 'Article created')
      closeDialog()
      await fetchList()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Save failed')
  } finally { saving.value = false }
}

async function handleDelete(a) {
  try {
    await ElMessageBox.confirm(`Delete "${a.title}"?`, 'Confirm Delete', { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' })
  } catch { return }
  try {
    const { data } = await request.delete(`/knowledge/${a.id}`)
    if (data.code === 200) { ElMessage.success('Article deleted'); await fetchList() }
  } catch (e) { ElMessage.error(e.response?.data?.message || 'Delete failed') }
}

function insertToReply(content) {
  router.push({ path: '/tickets', query: { kbContent: content } })
  ElMessage.success('Article copied — navigate to a ticket to paste')
  navigator.clipboard?.writeText(content)
}

onMounted(fetchList)
</script>

<style scoped>
.kb { max-width: 1100px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
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

/* Grid */
.kb-grid { display: grid; grid-template-columns: 220px 1fr; gap: var(--space-lg); }

/* Sidebar */
.kb-sidebar { display: flex; flex-direction: column; gap: var(--space-sm); }
.kb-search { width: 100%; padding: 8px 12px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1.5px solid var(--color-gray-200); border-radius: var(--radius-md); outline: none; box-sizing: border-box; }
.kb-search:focus { border-color: var(--color-primary); }
.kb-cats { list-style: none; margin: 0; padding: 0; }
.kb-cats li { padding: 8px 12px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; border-radius: var(--radius-sm); transition: all var(--transition-fast); }
.kb-cats li:hover { background: var(--color-gray-100); color: var(--color-text-primary); }
.kb-cat--active { background: var(--color-primary-bg) !important; color: var(--color-primary) !important; font-weight: 600; }

/* Articles */
.kb-main { display: flex; flex-direction: column; gap: var(--space-sm); min-width: 0; }
.kb-loading, .kb-empty { padding: var(--space-2xl); text-align: center; color: var(--color-text-secondary); }
.kb-article { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); overflow: hidden; }
.kb-article-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; cursor: pointer; transition: background var(--transition-fast); }
.kb-article-header:hover { background: var(--color-gray-50); }
.kb-article-title { font-weight: 600; font-size: var(--text-sm); color: var(--color-text-primary); }
.kb-article-cat { font-size: var(--text-xs); color: var(--color-text-muted); background: var(--color-gray-100); padding: 2px 8px; border-radius: var(--radius-full); }
.kb-article-body { padding: 0 16px 16px; border-top: 1px solid var(--color-gray-100); }
.kb-article-content { padding-top: var(--space-md); font-size: var(--text-sm); line-height: 1.7; color: var(--color-text-primary); }
.kb-article-content :deep(p) { margin: 0 0 0.5em; }
.kb-article-content :deep(h2) { font-size: var(--text-lg); margin: 1em 0 0.5em; }
.kb-article-content :deep(table) { width: 100%; border-collapse: collapse; margin: 0.5em 0; }
.kb-article-content :deep(th), .kb-article-content :deep(td) { border: 1px solid var(--color-gray-200); padding: 6px 10px; font-size: var(--text-sm); text-align: left; }
.kb-article-content :deep(th) { background: var(--color-gray-50); font-weight: 600; }
.kb-article-content :deep(code) { padding: 1px 4px; background: var(--color-gray-100); border-radius: 3px; font-family: var(--font-mono); font-size: 0.9em; }
.kb-article-content :deep(blockquote) { margin: 0.5em 0; padding: 0.4em 0.8em; border-left: 3px solid var(--color-primary); background: var(--color-primary-bg); border-radius: 0 var(--radius-sm) var(--radius-sm) 0; }

.kb-article-actions { display: flex; gap: var(--space-sm); margin-top: var(--space-lg); padding-top: var(--space-md); border-top: 1px solid var(--color-gray-100); }
.kb-insert-btn { padding: 6px 14px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; }
.kb-insert-btn:hover { opacity: 0.9; }
.kb-edit-btn { padding: 6px 14px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.kb-edit-btn:hover { background: var(--color-gray-50); }
.kb-delete-btn { padding: 6px 14px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-danger); background: var(--color-white); border: 1px solid var(--color-danger); border-radius: var(--radius-md); cursor: pointer; margin-left: auto; }
.kb-delete-btn:hover { background: #FEF2F2; }

/* Dialog (reuse pattern) */
.dialog-overlay { position: fixed; inset: 0; z-index: var(--z-modal); background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; }
.dialog-card { background: var(--color-white); border-radius: var(--radius-lg); padding: var(--space-xl); width: 640px; max-width: 90vw; box-shadow: var(--shadow-xl); }
.dialog-title { margin: 0 0 var(--space-lg); font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; }
.dialog-field { margin-bottom: var(--space-md); }
.dialog-label { display: block; margin-bottom: var(--space-xs); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.dialog-input, .dialog-textarea { width: 100%; padding: 10px 12px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-gray-50); border: 1.5px solid var(--color-gray-200); border-radius: var(--radius-md); outline: none; box-sizing: border-box; }
.dialog-input:focus, .dialog-textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124,58,237,0.12); }
.dialog-textarea { resize: vertical; font-family: var(--font-mono); }
.dialog-actions { display: flex; justify-content: flex-end; gap: var(--space-sm); margin-top: var(--space-lg); }
.dialog-cancel { padding: 8px 16px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.dialog-save { padding: 8px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; }
.dialog-save:hover:not(:disabled) { opacity: 0.9; }
.dialog-save:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .kb-grid { grid-template-columns: 1fr; }
  .kb-sidebar { flex-direction: row; flex-wrap: wrap; }
  .kb-cats { display: flex; gap: 4px; flex-wrap: wrap; }
}

.sys-badge { font-size: var(--text-xs); color: var(--color-text-muted); background: var(--color-gray-100); padding: 4px 10px; border-radius: var(--radius-full); }
</style>
