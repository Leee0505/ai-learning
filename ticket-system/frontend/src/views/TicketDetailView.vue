<template>
  <div class="detail" v-if="store.currentTicket">
    <!-- Header -->
    <div class="detail-header">
      <button class="detail-back" @click="$router.push('/tickets')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12,19 5,12 12,5" />
        </svg>
        Back to Tickets
      </button>
      <div class="detail-title-row">
        <h1 class="detail-title">{{ store.currentTicket.title }}</h1>
        <span :class="['detail-badge', statusClass(store.currentTicket.status)]">
          {{ statusLabel(store.currentTicket.status) }}
        </span>
      </div>
      <p class="detail-meta-text">
        #{{ store.currentTicket.id }} · Created by {{ store.currentTicket.createdByName }}
        · {{ formatDate(store.currentTicket.createdDate) }}
      </p>
    </div>

    <div class="detail-grid">
      <!-- Left Column -->
      <div class="detail-main">
        <!-- Description Card -->
        <div class="detail-card">
          <h2 class="detail-card-title">Description</h2>
          <p class="detail-desc">{{ store.currentTicket.description || 'No description provided.' }}</p>
        </div>

        <!-- Reply Timeline -->
        <div class="detail-card">
          <h2 class="detail-card-title">Activity</h2>
          <div v-if="visibleReplies.length === 0" class="detail-empty">
            <p>No replies yet.</p>
          </div>
          <div v-else class="detail-timeline">
            <div
              v-for="reply in visibleReplies"
              :key="reply.id"
              :class="['detail-reply', reply.isInternal ? 'detail-reply--internal' : '']"
            >
              <div class="detail-reply-avatar">{{ reply.username?.charAt(0)?.toUpperCase() || '?' }}</div>
              <div class="detail-reply-body">
                <div class="detail-reply-meta">
                  <span class="detail-reply-author">{{ reply.username }}</span>
                  <span v-if="reply.isInternal" class="detail-reply-internal-badge">Internal Note</span>
                  <span class="detail-reply-time">{{ formatDateTime(reply.createdDate) }}</span>
                </div>
                <p class="detail-reply-content">{{ reply.content }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Reply Input -->
        <div class="detail-card">
          <h2 class="detail-card-title">Add Reply</h2>
          <textarea
            v-model="replyContent"
            class="detail-reply-input"
            placeholder="Type your reply..."
            rows="4"
            :disabled="replyLoading"
          />
          <div class="detail-reply-actions">
            <label v-if="authStore.isAgent || authStore.isAdmin" class="detail-reply-internal">
              <input v-model="isInternal" type="checkbox" /> Internal Note (not visible to user)
            </label>
            <button class="detail-reply-submit" :disabled="replyLoading || !replyContent.trim()" @click="handleReply">
              <span v-if="!replyLoading">Send Reply</span>
              <span v-else>Sending...</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Right Sidebar -->
      <div class="detail-sidebar">
        <!-- Info Card -->
        <div class="detail-card">
          <h2 class="detail-card-title">Details</h2>
          <dl class="detail-info-list">
            <div class="detail-info-item">
              <dt>Priority</dt>
              <dd><span :class="['detail-badge', priorityClass(store.currentTicket.priority)]">{{ store.currentTicket.priority }}</span></dd>
            </div>
            <div class="detail-info-item">
              <dt>Category</dt>
              <dd>{{ store.currentTicket.category }}</dd>
            </div>
            <div class="detail-info-item">
              <dt>Assignee</dt>
              <dd>{{ store.currentTicket.assignedToName || 'Unassigned' }}</dd>
            </div>
            <div v-if="store.currentTicket.resolvedDate" class="detail-info-item">
              <dt>Resolved</dt>
              <dd>{{ formatDate(store.currentTicket.resolvedDate) }}</dd>
            </div>
            <div v-if="store.currentTicket.closedDate" class="detail-info-item">
              <dt>Closed</dt>
              <dd>{{ formatDate(store.currentTicket.closedDate) }}</dd>
            </div>
          </dl>
        </div>

        <!-- Agent/Admin Actions -->
        <div v-if="authStore.isAgent || authStore.isAdmin" class="detail-card">
          <h2 class="detail-card-title">Actions</h2>

          <!-- Change Status -->
          <div class="detail-action">
            <label class="detail-action-label">Change Status</label>
            <select v-model="selectedStatus" class="detail-action-select" @change="handleStatusChange">
              <option value="">— Select —</option>
              <option v-for="s in allowedTransitions" :key="s" :value="s">{{ statusLabel(s) }}</option>
            </select>
          </div>

          <!-- Assign -->
          <div class="detail-action">
            <label class="detail-action-label">Assign</label>
            <p class="detail-action-hint">Currently: {{ store.currentTicket.assignedToName || 'Unassigned' }}</p>
            <div class="detail-assign-row">
              <input
                v-model="assignTargetId"
                type="number"
                class="detail-action-input"
                placeholder="Agent user ID"
              />
              <button class="detail-action-btn" @click="handleAssign">Assign</button>
            </div>
          </div>

          <!-- Delete (Admin only) -->
          <div v-if="authStore.isAdmin" class="detail-action">
            <button class="detail-delete-btn" @click="handleDelete">Delete Ticket</button>
          </div>
        </div>

        <!-- Attachments -->
        <div v-if="store.currentTicket.attachments?.length" class="detail-card">
          <h2 class="detail-card-title">
            Attachments
            <span class="detail-card-count">{{ store.currentTicket.attachments.length }}</span>
          </h2>
          <div class="detail-attach-scroll">
            <div
              v-for="att in store.currentTicket.attachments"
              :key="att.id"
              class="detail-attach-item"
              :class="{ 'detail-attach-item--image': isImage(att.contentType) }"
            >
              <!-- Image thumbnail -->
              <img
                v-if="isImage(att.contentType)"
                :src="thumbnails[att.id]"
                :alt="att.originalFilename"
                class="detail-attach-thumb"
                @click="openLightbox(att)"
              />
              <!-- Non-image file type badge -->
              <div v-else class="detail-attach-filetype" :class="fileTypeClass(att.contentType)" aria-hidden="true">
                {{ fileTypeLabel(att.contentType) }}
              </div>
              <div class="detail-attach-info">
                <span class="detail-attach-name" :title="att.originalFilename">{{ att.originalFilename }}</span>
                <span class="detail-attach-meta">{{ formatSize(att.fileSize) }}</span>
              </div>
              <button
                class="detail-attach-download"
                @click="isImage(att.contentType) ? openLightbox(att) : handleDownload(att)"
                :disabled="downloadingId === att.id"
                :title="isImage(att.contentType) ? 'View full size' : 'Download ' + att.originalFilename"
              >
                <svg v-if="isImage(att.contentType)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="detail-attach-dl-icon" aria-hidden="true">
                  <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="detail-attach-dl-icon" aria-hidden="true">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7,10 12,15 17,10" /><line x1="12" y1="15" x2="12" y2="3" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Loading / Not Found -->
  <div v-else class="detail-center">
    <p v-if="store.loading">Loading...</p>
    <p v-else>Ticket not found.</p>
  </div>

  <!-- Lightbox — after v-if/v-else to avoid breaking adjacency -->
  <Teleport to="body">
    <div v-if="lightboxAtt" class="lightbox" @click="closeLightbox">
      <img
        v-if="lightboxSrc"
        :src="lightboxSrc"
        :alt="lightboxAtt.originalFilename"
        class="lightbox-img"
        @click.stop
      />
      <button class="lightbox-close" @click="closeLightbox" title="Close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
      <button class="lightbox-download" @click="handleLightboxDownload" title="Download">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7,10 12,15 17,10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTicketStore } from '@/stores/tickets'
import { useAuthStore } from '@/stores/auth'
import { downloadAttachment } from '@/api/tickets'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const store = useTicketStore()
const authStore = useAuthStore()

const replyContent = ref('')
const isInternal = ref(false)
const replyLoading = ref(false)
const downloadingId = ref(null)
const selectedStatus = ref('')
const assignTargetId = ref('')
const lightboxAtt = ref(null)
const lightboxSrc = ref('')
const thumbnails = ref({})  // attachmentId → blob URL (loaded via axios for JWT auth)

const ticketId = computed(() => route.params.id)

const TRANSITIONS = {
  'OPEN': ['IN_PROGRESS', 'CLOSED'],
  'IN_PROGRESS': ['RESOLVED', 'CLOSED'],
  'RESOLVED': ['CLOSED'],
  'CLOSED': []
}

const allowedTransitions = computed(() => {
  return TRANSITIONS[store.currentTicket?.status] || []
})

const visibleReplies = computed(() => {
  return (store.currentTicket?.replies || []).filter(r => {
    if (r.isInternal && !authStore.isAgent && !authStore.isAdmin) return false
    return true
  })
})

onMounted(async () => {
  await store.fetchTicketDetail(ticketId.value)
  await loadThumbnails()
})

async function loadThumbnails() {
  const images = (store.currentTicket?.attachments || []).filter(a => isImage(a.contentType))
  await Promise.all(images.map(async att => {
    try {
      const res = await request.get(`/attachments/${att.id}/thumbnail?size=200`, { responseType: 'blob' })
      thumbnails.value[att.id] = URL.createObjectURL(res.data)
    } catch { /* ignore failed thumbnail */ }
  }))
}

async function handleReply() {
  if (!replyContent.value.trim()) return
  replyLoading.value = true
  try {
    await store.addReplyAction(ticketId.value, replyContent.value.trim(), isInternal.value)
    replyContent.value = ''
    isInternal.value = false
    ElMessage.success('Reply sent')
    await store.fetchTicketDetail(ticketId.value)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to send reply')
  } finally {
    replyLoading.value = false
  }
}

async function handleStatusChange() {
  if (!selectedStatus.value) return
  try {
    await store.changeTicketStatusAction(ticketId.value, selectedStatus.value)
    selectedStatus.value = ''
    ElMessage.success('Status updated')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to change status')
  }
}

async function handleAssign() {
  const id = parseInt(assignTargetId.value)
  if (!id || isNaN(id)) { ElMessage.warning('Enter a valid agent user ID'); return }
  try {
    await store.assignTicketAction(ticketId.value, id)
    assignTargetId.value = ''
    ElMessage.success('Ticket assigned')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to assign ticket')
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this ticket?', 'Confirm Delete', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await store.removeTicket(ticketId.value)
    ElMessage.success('Ticket deleted')
    router.push('/tickets')
  } catch {}
}

async function handleDownload(att) {
  downloadingId.value = att.id
  try {
    const response = await downloadAttachment(att.id)
    // Build blob URL from response data and trigger browser download
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', att.originalFilename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('Download failed')
  } finally {
    downloadingId.value = null
  }
}

function fileTypeLabel(contentType) {
  if (!contentType) return '?'
  if (contentType.startsWith('image/')) return 'IMG'
  if (contentType.includes('pdf')) return 'PDF'
  if (contentType.includes('word') || contentType.includes('document')) return 'DOC'
  if (contentType.includes('sheet') || contentType.includes('excel')) return 'XLS'
  if (contentType.includes('presentation') || contentType.includes('powerpoint')) return 'PPT'
  if (contentType.includes('zip') || contentType.includes('rar') || contentType.includes('tar') || contentType.includes('gz') || contentType.includes('7z')) return 'ZIP'
  if (contentType.startsWith('video/')) return 'VID'
  if (contentType.startsWith('text/')) return 'TXT'
  return '?'
}

function fileTypeClass(contentType) {
  if (!contentType) return 'ft-unknown'
  if (contentType.startsWith('image/')) return 'ft-image'
  if (contentType.includes('pdf')) return 'ft-pdf'
  if (contentType.includes('word') || contentType.includes('document')) return 'ft-doc'
  if (contentType.includes('sheet') || contentType.includes('excel')) return 'ft-xls'
  if (contentType.includes('presentation') || contentType.includes('powerpoint')) return 'ft-ppt'
  if (contentType.includes('zip') || contentType.includes('rar') || contentType.includes('tar') || contentType.includes('gz') || contentType.includes('7z')) return 'ft-archive'
  if (contentType.startsWith('video/')) return 'ft-video'
  if (contentType.startsWith('text/')) return 'ft-text'
  return 'ft-unknown'
}

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}
function formatDateTime(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
function statusClass(s) { return { 'OPEN': 'badge-open', 'IN_PROGRESS': 'badge-progress', 'RESOLVED': 'badge-resolved', 'CLOSED': 'badge-closed' }[s] || '' }
function statusLabel(s) { return { 'OPEN': 'Open', 'IN_PROGRESS': 'In Progress', 'RESOLVED': 'Resolved', 'CLOSED': 'Closed' }[s] || s }
function priorityClass(p) { return { 'LOW': 'badge-low', 'MEDIUM': 'badge-medium', 'HIGH': 'badge-high', 'URGENT': 'badge-urgent' }[p] || '' }

function isImage(contentType) { return contentType && contentType.startsWith('image/') }

async function openLightbox(att) {
  lightboxAtt.value = att
  lightboxSrc.value = ''
  try {
    const res = await request.get(`/attachments/${att.id}`, { responseType: 'blob' })
    lightboxSrc.value = URL.createObjectURL(res.data)
  } catch {
    ElMessage.error('Failed to load image')
    lightboxAtt.value = null
  }
}
function closeLightbox() {
  if (lightboxSrc.value) URL.revokeObjectURL(lightboxSrc.value)
  lightboxAtt.value = null
  lightboxSrc.value = ''
}

function handleLightboxDownload() {
  if (lightboxAtt.value) handleDownload(lightboxAtt.value)
}
</script>

<style scoped>
.detail { max-width: 1200px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.detail-center { text-align: center; padding: var(--space-3xl); color: var(--color-text-secondary); }

/* Header */
.detail-header { margin-bottom: var(--space-lg); }
.detail-back { display: inline-flex; align-items: center; gap: var(--space-xs); padding: 0; border: none; background: none; color: var(--color-primary); font-size: var(--text-sm); font-family: var(--font-body); cursor: pointer; margin-bottom: var(--space-sm); }
.detail-back svg { width: 16px; height: 16px; }
.detail-back:hover { color: var(--color-primary-dark); }

.detail-title-row { display: flex; align-items: center; gap: var(--space-md); flex-wrap: wrap; }
.detail-title { margin: 0; font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); }
.detail-meta-text { margin: var(--space-xs) 0 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

/* Grid */
.detail-grid { display: grid; grid-template-columns: 1fr 300px; gap: var(--space-lg); }
.detail-main { display: flex; flex-direction: column; gap: var(--space-lg); min-width: 0; }
.detail-sidebar { display: flex; flex-direction: column; gap: var(--space-lg); }

/* Cards */
.detail-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); box-shadow: var(--shadow-sm); }
.detail-card-title { margin: 0 0 var(--space-md); font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); }
.detail-desc { margin: 0; font-size: var(--text-base); line-height: 1.7; color: var(--color-text-primary); white-space: pre-wrap; }
.detail-empty { text-align: center; color: var(--color-text-muted); padding: var(--space-lg) 0; }

/* Badges */
.detail-badge { display: inline-block; padding: 3px 10px; font-size: var(--text-xs); font-weight: 600; border-radius: var(--radius-full); }
.badge-open { background: #DBEAFE; color: #1D4ED8; }
.badge-progress { background: #FEF3C7; color: #B45309; }
.badge-resolved { background: #D1FAE5; color: #047857; }
.badge-closed { background: var(--color-gray-100); color: var(--color-text-secondary); }
.badge-low { background: var(--color-gray-100); color: var(--color-text-secondary); }
.badge-medium { background: #DBEAFE; color: #1D4ED8; }
.badge-high { background: #FED7AA; color: #C2410C; }
.badge-urgent { background: #FEE2E2; color: #B91C1C; }

/* Card count badge */
.detail-card-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 22px; height: 22px; padding: 0 6px;
  font-size: 12px; font-weight: 600; color: var(--color-white);
  background: var(--color-primary); border-radius: var(--radius-full);
  margin-left: var(--space-xs); vertical-align: middle;
}

/* Sidebar Attachments */
.detail-attach-scroll {
  max-height: 360px;
  overflow-y: auto;
  display: flex; flex-direction: column; gap: var(--space-xs);
  padding-right: 2px; /* room for scrollbar */
}
.detail-attach-scroll::-webkit-scrollbar { width: 4px; }
.detail-attach-scroll::-webkit-scrollbar-thumb { background: var(--color-gray-300); border-radius: 2px; }

.detail-attach-item {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-sm); background: var(--color-gray-50);
  border: 1px solid var(--color-gray-100); border-radius: var(--radius-md);
  font-size: var(--text-sm); transition: border-color var(--transition-fast);
}
.detail-attach-item:hover { border-color: var(--color-primary); }
.detail-attach-item--image { align-items: flex-start; }

/* Thumbnail */
.detail-attach-thumb {
  width: 48px; height: 48px; object-fit: cover;
  border-radius: var(--radius-sm); flex-shrink: 0; cursor: pointer;
  border: 1px solid var(--color-gray-200);
}
.detail-attach-thumb:hover { border-color: var(--color-primary); opacity: 0.85; }

/* File type badge (non-image) */
.detail-attach-filetype {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; font-family: var(--font-mono); letter-spacing: 0.5px;
  border-radius: var(--radius-sm); flex-shrink: 0; color: var(--color-white);
}
.ft-image { background: #8B5CF6; }
.ft-pdf { background: #EF4444; }
.ft-doc { background: #3B82F6; }
.ft-xls { background: #10B981; }
.ft-ppt { background: #F59E0B; }
.ft-archive { background: #6B7280; }
.ft-video { background: #EC4899; }
.ft-text { background: #6366F1; }
.ft-unknown { background: var(--color-gray-400); }

.detail-attach-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.detail-attach-name { color: var(--color-text-primary); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.detail-attach-meta { color: var(--color-text-muted); font-size: var(--text-xs); }

.detail-attach-download {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; padding: 0;
  background: var(--color-white); border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md); cursor: pointer; flex-shrink: 0;
  color: var(--color-primary); transition: all var(--transition-fast);
}
.detail-attach-download:hover:not(:disabled) {
  background: var(--color-primary); color: var(--color-white); border-color: var(--color-primary);
}
.detail-attach-download:disabled { opacity: 0.5; cursor: not-allowed; }
.detail-attach-dl-icon { width: 16px; height: 16px; }

/* Lightbox */
.lightbox {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  display: flex; align-items: center; justify-content: center;
}
.lightbox-img {
  max-width: 90vw; max-height: 90vh; object-fit: contain;
  border-radius: var(--radius-md);
}
.lightbox-close {
  position: absolute; top: var(--space-lg); right: var(--space-lg);
  display: flex; align-items: center; justify-content: center;
  width: 48px; height: 48px; padding: 0;
  background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3);
  border-radius: var(--radius-full); color: var(--color-white); cursor: pointer;
  transition: background var(--transition-fast);
}
.lightbox-close svg { width: 24px; height: 24px; }
.lightbox-close:hover { background: rgba(255,255,255,0.25); }
.lightbox-download {
  position: absolute; bottom: var(--space-xl); right: var(--space-xl);
  display: flex; align-items: center; justify-content: center;
  width: 48px; height: 48px; padding: 0;
  background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3);
  border-radius: var(--radius-full); color: var(--color-white); cursor: pointer;
  font-family: var(--font-body);
}
.lightbox-download svg { width: 24px; height: 24px; }
.lightbox-download:hover { background: rgba(255,255,255,0.25); }

/* Timeline */
.detail-timeline { display: flex; flex-direction: column; }
.detail-reply { display: flex; gap: var(--space-md); padding: var(--space-md) 0; border-bottom: 1px solid var(--color-gray-100); }
.detail-reply:last-child { border-bottom: none; }
.detail-reply--internal { background: #FFFBEB; margin: 0 calc(-1 * var(--space-lg)); padding-left: var(--space-lg); padding-right: var(--space-lg); border-left: 3px solid var(--color-warning); }

.detail-reply-avatar { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; font-size: var(--text-sm); font-weight: 700; font-family: var(--font-heading); color: var(--color-white); background: var(--color-primary); border-radius: var(--radius-full); flex-shrink: 0; }
.detail-reply--internal .detail-reply-avatar { background: var(--color-warning); }

.detail-reply-body { flex: 1; min-width: 0; }
.detail-reply-meta { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-xs); flex-wrap: wrap; }
.detail-reply-author { font-weight: 600; font-size: var(--text-sm); color: var(--color-text-primary); }
.detail-reply-internal-badge { padding: 1px 6px; font-size: 10px; font-weight: 600; background: #FEF3C7; color: #B45309; border-radius: var(--radius-full); }
.detail-reply-time { font-size: var(--text-xs); color: var(--color-text-muted); }
.detail-reply-content { margin: 0; font-size: var(--text-base); line-height: 1.6; color: var(--color-text-primary); white-space: pre-wrap; }

/* Reply Input */
.detail-reply-input { width: 100%; padding: 10px 12px; font-size: var(--text-base); font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-gray-50); border: 1.5px solid var(--color-gray-200); border-radius: var(--radius-md); outline: none; box-sizing: border-box; resize: vertical; min-height: 100px; }
.detail-reply-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12); }
.detail-reply-actions { display: flex; align-items: center; justify-content: space-between; margin-top: var(--space-md); gap: var(--space-md); }
.detail-reply-internal { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.detail-reply-internal input { accent-color: var(--color-warning); }
.detail-reply-submit { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; transition: opacity var(--transition-fast); }
.detail-reply-submit:hover:not(:disabled) { opacity: 0.92; }
.detail-reply-submit:disabled { opacity: 0.6; cursor: not-allowed; }

/* Sidebar */
.detail-info-list { margin: 0; }
.detail-info-item { display: flex; justify-content: space-between; align-items: center; padding: var(--space-sm) 0; border-bottom: 1px solid var(--color-gray-100); }
.detail-info-item:last-child { border-bottom: none; }
.detail-info-item dt { font-size: var(--text-sm); color: var(--color-text-secondary); }
.detail-info-item dd { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-primary); margin: 0; }

.detail-action { margin-bottom: var(--space-md); }
.detail-action:last-child { margin-bottom: 0; }
.detail-action-label { display: block; margin-bottom: var(--space-xs); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.detail-action-hint { margin: 0 0 var(--space-xs); font-size: var(--text-xs); color: var(--color-text-muted); }
.detail-action-select { width: 100%; padding: 8px 12px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.detail-action-input { width: 100%; padding: 8px 12px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); box-sizing: border-box; margin-bottom: var(--space-xs); }
.detail-assign-row { display: flex; gap: var(--space-sm); }
.detail-action-btn { padding: 8px 16px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; }
.detail-action-btn:hover { opacity: 0.9; }
.detail-delete-btn { padding: 8px 16px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-danger); background: var(--color-white); border: 1px solid var(--color-danger); border-radius: var(--radius-md); cursor: pointer; width: 100%; }
.detail-delete-btn:hover { background: #FEF2F2; }

@media (max-width: 900px) {
  .detail-grid { grid-template-columns: 1fr; }
  .detail { padding: var(--space-lg) var(--space-md); }
}
</style>