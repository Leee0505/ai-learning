<template>
  <div class="notif-bell" ref="bellRef">
    <button
      class="bell-btn"
      @click="toggle"
      :aria-label="unreadCount > 0 ? `${unreadCount} unread notifications` : 'Notifications'"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <transition name="popover-fade">
      <div v-if="open" class="popover" role="dialog" aria-label="Notifications">
        <div class="popover-header">
          <span class="popover-title">Notifications</span>
          <button v-if="unreadCount > 0" class="popover-action" @click="handleMarkAllRead">Mark all read</button>
        </div>
        <div class="popover-body">
          <div v-if="loading" class="popover-loading">Loading...</div>
          <div v-else-if="items.length === 0" class="popover-empty">No notifications yet</div>
          <div v-else class="popover-list">
            <div
              v-for="item in items"
              :key="item.id"
              class="popover-item"
              :class="{ 'popover-item--unread': !item.isRead }"
              @click="handleClick(item)"
            >
              <span v-if="!item.isRead" class="unread-dot"></span>
              <div class="item-content">
                <span class="item-type">{{ typeLabel(item.type) }}</span>
                <span class="item-title">{{ item.title }}</span>
              </div>
              <span class="item-time">{{ timeAgo(item.createdDate) }}</span>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useNotificationStore } from '@/stores/notifications'
import { getNotificationsApi } from '@/api/notifications'

const store = useNotificationStore()
const router = useRouter()
const { unreadCount } = storeToRefs(store)

const open = ref(false)
const loading = ref(false)
const items = ref([])
const bellRef = ref(null)

function toggle() {
  open.value = !open.value
  if (open.value) fetchItems()
}

function close() { open.value = false }

function handleClickOutside(e) {
  if (bellRef.value && !bellRef.value.contains(e.target)) close()
}

onMounted(() => { document.addEventListener('click', handleClickOutside) })
onBeforeUnmount(() => { document.removeEventListener('click', handleClickOutside) })

async function fetchItems() {
  loading.value = true
  try {
    const { data } = await getNotificationsApi({ page: 1, size: 20 })
    if (data?.code === 200) items.value = data.data || []
  } catch (e) { console.error('Fetch notifications error:', e) }
  finally { loading.value = false }
}

async function handleClick(item) {
  if (!item.isRead) await store.markRead(item.id)
  close()
  if (item.ticketId) router.push(`/tickets/${item.ticketId}`)
}

async function handleMarkAllRead() {
  await store.markAllRead()
  items.value.forEach(item => item.isRead = true)
}

function typeLabel(type) {
  const map = {
    TICKET_CREATED: 'New ticket',
    TICKET_ASSIGNED: 'Assigned',
    TICKET_REPLIED: 'Reply',
    TICKET_RESOLVED: 'Resolved',
    TICKET_OVERDUE: 'Overdue'
  }
  return map[type] || type
}

function timeAgo(ts) {
  if (!ts) return ''
  const diff = Date.now() - ts
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return mins + 'm ago'
  const hours = Math.floor(mins / 60)
  if (hours < 24) return hours + 'h ago'
  return Math.floor(hours / 24) + 'd ago'
}
</script>

<style scoped>
.notif-bell { position: relative; }

.bell-btn { position: relative; display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; background: none; border: none; border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; transition: color 150ms, background 150ms; }
.bell-btn:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.bell-btn svg { width: 20px; height: 20px; }

.bell-badge { position: absolute; top: 4px; right: 4px; min-width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; padding: 0 4px; font-size: 10px; font-weight: 700; color: #fff; background: #EF4444; border-radius: 9px; line-height: 1; }

.popover { position: absolute; top: calc(100% + 8px); right: 0; width: 360px; max-height: 480px; background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); border: 1px solid var(--color-gray-200); display: flex; flex-direction: column; z-index: var(--z-modal); }
.popover-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-gray-100); }
.popover-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.popover-action { font-size: var(--text-xs); font-weight: 500; color: var(--color-primary); background: none; border: none; cursor: pointer; }
.popover-action:hover { text-decoration: underline; }
.popover-body { flex: 1; overflow-y: auto; }
.popover-loading, .popover-empty { padding: var(--space-lg); text-align: center; font-size: var(--text-sm); color: var(--color-text-muted); }

.popover-list { display: flex; flex-direction: column; }
.popover-item { display: flex; align-items: flex-start; gap: var(--space-sm); padding: 12px var(--space-lg); border-bottom: 1px solid var(--color-gray-50); cursor: pointer; transition: background 150ms; }
.popover-item:hover { background: var(--color-gray-50); }
.popover-item--unread { background: #F5F3FF; }
.popover-item--unread:hover { background: #EDE9FE; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary); flex-shrink: 0; margin-top: 6px; }
.item-content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.item-type { font-size: var(--text-xs); font-weight: 600; color: var(--color-primary); }
.item-title { font-size: var(--text-sm); color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-time { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); flex-shrink: 0; margin-top: 2px; }

.popover-fade-enter-active, .popover-fade-leave-active { transition: opacity 150ms, transform 150ms; }
.popover-fade-enter-from, .popover-fade-leave-to { opacity: 0; transform: translateY(-4px); }

@media (max-width: 480px) {
  .popover { width: calc(100vw - 32px); right: -120px; }
}
</style>
