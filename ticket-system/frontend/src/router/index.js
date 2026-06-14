import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false, layout: 'auth' }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardPlaceholder.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tickets',
    name: 'Tickets',
    component: () => import('@/views/TicketListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tickets/new',
    name: 'TicketNew',
    component: () => import('@/views/TicketCreateView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tickets/:id',
    name: 'TicketDetail',
    component: () => import('@/views/TicketDetailView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.accessToken) {
    if (authStore.refreshToken) {
      try {
        const { refreshApi } = await import('@/api/auth')
        const { data } = await refreshApi({ refreshToken: authStore.refreshToken })
        if (data.code === 200) {
          authStore.setTokens(data.data.accessToken, data.data.refreshToken)
          await authStore.fetchUser()
          next()
          return
        }
      } catch {
        authStore.clearAuth()
      }
    }
    next('/login')
    return
  }

  if (to.path === '/login' && authStore.accessToken) {
    next('/')
    return
  }

  next()
})

export default router
