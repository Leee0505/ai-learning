import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCountApi, markReadApi, markAllReadApi } from '@/api/notifications'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const connected = ref(false)
  let stompClient = null

  async function fetchUnreadCount() {
    try {
      const { data } = await getUnreadCountApi()
      if (data?.code === 200) unreadCount.value = data.data || 0
    } catch (e) {
      console.error('[notif store] fetchUnreadCount:', e)
    }
  }

  function connect() {
    const authStore = useAuthStore()
    if (!authStore.accessToken) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${authStore.accessToken}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        stompClient.subscribe('/user/queue/notifications', (msg) => {
          const body = JSON.parse(msg.body)
          unreadCount.value++
          // Could show a toast notification here
        })
      },
      onDisconnect: () => { connected.value = false },
      onStompError: (frame) => { console.error('[STOMP] error:', frame.headers.message) }
    })
    stompClient.activate()
  }

  function disconnect() {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
      connected.value = false
    }
  }

  async function markRead(id) {
    try {
      const { data } = await markReadApi(id)
      if (data?.code === 200 && unreadCount.value > 0) unreadCount.value--
    } catch (e) { console.error('[notif store] markRead:', e) }
  }

  async function markAllRead() {
    try {
      const { data } = await markAllReadApi()
      if (data?.code === 200) unreadCount.value = 0
    } catch (e) { console.error('[notif store] markAllRead:', e) }
  }

  return { unreadCount, connected, fetchUnreadCount, connect, disconnect, markRead, markAllRead }
})
