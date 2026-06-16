import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/DashboardPlaceholder.vue')
      },
      {
        path: 'tickets',
        name: 'Tickets',
        component: () => import('@/views/TicketListView.vue')
      },
      {
        path: 'workbench',
        name: 'Workbench',
        component: () => import('@/views/AgentWorkbench.vue'),
        meta: { requiresAuth: true, requiresAgent: true }
      },
      {
        path: 'tickets/new',
        name: 'TicketNew',
        component: () => import('@/views/TicketCreateView.vue')
      },
      {
        path: 'tickets/:id',
        name: 'TicketDetail',
        component: () => import('@/views/TicketDetailView.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue')
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/TemplateListView.vue'),
        meta: { requiresAuth: true, requiresAgent: true }
      },
      {
        path: 'performance',
        name: 'Performance',
        component: () => import('@/views/AgentDashboardView.vue'),
        meta: { requiresAuth: true, requiresAgent: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserListView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      }
    ]
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

  // Admin route guard
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/')
    return
  }

  // Agent/Admin route guard (agents and admins can access agent features)
  if (to.meta.requiresAgent && !authStore.isAgent && !authStore.isAdmin) {
    next('/')
    return
  }

  next()
})

export default router
