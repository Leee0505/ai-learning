<template>
  <div class="app-layout">
    <!-- Top navigation bar — persistent across all authenticated pages -->
    <header class="app-nav">
      <div class="app-nav-inner">
        <!-- Left group: brand + nav links -->
        <div class="nav-left">
          <router-link to="/" class="app-brand">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
              class="app-brand-icon" aria-hidden="true">
              <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
              <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
            </svg>
            <span class="app-brand-text">Ticket System</span>
          </router-link>

          <nav class="app-nav-links">
            <router-link to="/" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path === '/' }">Dashboard</router-link>
            <router-link v-if="authStore.isAgent || authStore.isAdmin" to="/workbench" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/workbench') }">Workbench</router-link>
            <router-link to="/tickets" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/tickets') }">Tickets</router-link>
            <router-link v-if="authStore.isAgent || authStore.isAdmin" to="/templates" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/templates') }">Templates</router-link>
            <router-link v-if="authStore.isAgent || authStore.isAdmin" to="/knowledge" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/knowledge') }">Knowledge</router-link>
            <!-- Admin dropdown -->
            <div v-if="authStore.isAdmin" class="admin-dropdown" ref="adminDropdownRef">
              <button
                class="admin-nav-btn"
                :class="{ 'admin-nav-btn--active': $route.path.startsWith('/admin') }"
                @click="adminMenuOpen = !adminMenuOpen"
                aria-haspopup="true"
                :aria-expanded="adminMenuOpen"
              >
                Admin
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="admin-chevron" :class="{ 'admin-chevron--open': adminMenuOpen }" aria-hidden="true">
                  <polyline points="6,9 12,15 18,9" />
                </svg>
              </button>
              <transition name="dropdown-fade">
                <div v-if="adminMenuOpen" class="admin-menu" role="menu">
                  <router-link to="/admin/users" class="admin-menu-item" :class="{ 'admin-menu-item--active': $route.path.startsWith('/admin/users') }" @click="adminMenuOpen = false">Users</router-link>
                  <router-link to="/admin/config" class="admin-menu-item" :class="{ 'admin-menu-item--active': $route.path.startsWith('/admin/config') }" @click="adminMenuOpen = false">Config</router-link>
                  <router-link to="/admin/monitor" class="admin-menu-item" :class="{ 'admin-menu-item--active': $route.path.startsWith('/admin/monitor') }" @click="adminMenuOpen = false">Monitor</router-link>
                </div>
              </transition>
            </div>
          </nav>
        </div>

        <!-- Spacer: pushes user section to the right, bell stays near nav -->
        <NotificationBell />
        <div class="nav-spacer"></div>
        <div class="app-user">
          <div class="app-user-avatar" aria-hidden="true">
            {{ userInitial }}
          </div>
          <span class="app-user-name">{{ authStore.user?.username }}</span>
          <span class="app-user-role">{{ roleLabel }}</span>
          <router-link to="/profile" class="app-profile-link" title="Profile">Profile</router-link>
          <button
            class="app-logout-btn"
            @click="handleLogout"
            title="Sign out"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              class="app-logout-icon" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16,17 21,12 16,7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span class="app-logout-text">Sign Out</span>
          </button>
        </div>
        </div>
      </div>
    </header>

    <!-- Page content -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import NotificationBell from '@/components/NotificationBell.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const authStore = useAuthStore()
const notifStore = useNotificationStore()
const router = useRouter()

const adminMenuOpen = ref(false)
const adminDropdownRef = ref(null)

function handleClickOutside(e) {
  if (adminDropdownRef.value && !adminDropdownRef.value.contains(e.target)) {
    adminMenuOpen.value = false
  }
}
onMounted(() => { document.addEventListener('click', handleClickOutside) })
onBeforeUnmount(() => { document.removeEventListener('click', handleClickOutside) })

const userInitial = computed(() => {
  return authStore.user?.username?.charAt(0)?.toUpperCase() || 'U'
})

const roleLabel = computed(() => {
  const role = authStore.user?.role
  if (role === 'ROLE_ADMIN') return 'Admin'
  if (role === 'ROLE_AGENT') return 'Agent'
  return 'User'
})

onMounted(() => {
  notifStore.fetchUnreadCount()
  notifStore.connect()
})
onBeforeUnmount(() => { notifStore.disconnect() })

async function handleLogout() {
  notifStore.disconnect()
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ── Layout ── */
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-gray-50);
}

/* ── Navigation ── */
.app-nav {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-gray-200);
  box-shadow: var(--shadow-sm);
}

.app-nav-inner {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 56px;
}

/* Left group: brand + nav links */
.nav-left { display: flex; align-items: center; gap: var(--space-xl); }
/* Push user section to the right, bell stays near nav links */
.nav-spacer { margin-left: auto; }

/* Brand */
.app-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-decoration: none;
  flex-shrink: 0;
}

.app-brand-icon {
  width: 24px;
  height: 24px;
  color: var(--color-primary);
}

.app-brand-text {
  font-family: var(--font-heading);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* Nav links */
.app-nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.app-nav-link {
  padding: 6px 12px;
  font-size: var(--text-sm);
  font-weight: 500;
  font-family: var(--font-body);
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: color var(--transition-fast), background var(--transition-fast);
  white-space: nowrap;
}

.app-nav-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.app-nav-link--active {
  color: var(--color-primary);
  background: var(--color-primary-bg);
  font-weight: 600;
}

/* Admin dropdown — standalone style, not inheriting app-nav-link */
.admin-dropdown { position: relative; display: flex; align-items: center; }

.admin-nav-btn {
  display: inline-flex; align-items: center; gap: 2px;
  padding: 6px 12px;
  font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body);
  color: var(--color-text-secondary);
  background: none;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  white-space: nowrap;
  transition: color 150ms;
}
.admin-nav-btn:hover { color: var(--color-primary); }
.admin-nav-btn--active { color: var(--color-primary); font-weight: 600; }

.admin-chevron { width: 14px; height: 14px; transition: transform 200ms; }
.admin-chevron--open { transform: rotate(180deg); }
.admin-menu { position: absolute; top: calc(100% + 6px); left: 0; min-width: 160px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); padding: 6px; z-index: var(--z-modal); }
.admin-menu-item { display: block; padding: 10px 14px; font-size: var(--text-sm); font-weight: 500; color: var(--color-text-secondary); text-decoration: none; border-radius: var(--radius-md); transition: color 150ms, background 150ms; }
.admin-menu-item:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.admin-menu-item--active { color: var(--color-primary); background: var(--color-primary-bg); font-weight: 600; }

.dropdown-fade-enter-active, .dropdown-fade-leave-active { transition: opacity 150ms, transform 150ms; }
.dropdown-fade-enter-from, .dropdown-fade-leave-to { opacity: 0; transform: translateY(-4px); }

/* User */
.app-user {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.app-user-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: 700;
  font-family: var(--font-heading);
  color: var(--color-white);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.app-user-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

.app-user-role {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.app-profile-link {
  font-size: var(--text-xs); color: var(--color-text-secondary); text-decoration: none;
  padding: 2px 8px; border-radius: var(--radius-sm);
  transition: color var(--transition-fast), background var(--transition-fast);
}
.app-profile-link:hover { color: var(--color-primary); background: var(--color-primary-bg); }

.app-logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: var(--text-sm);
  font-weight: 500;
  font-family: var(--font-body);
  color: var(--color-text-secondary);
  background: none;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: color var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast);
  margin-left: var(--space-sm);
}

.app-logout-btn:hover {
  color: var(--color-danger);
  background: #FEF2F2;
  border-color: #FECACA;
}

.app-logout-icon {
  width: 16px;
  height: 16px;
}

/* ── Main Content ── */
.app-main {
  flex: 1;
}

/* ── Responsive ── */
@media (max-width: 640px) {
  .app-nav-inner {
    padding: 0 var(--space-md);
  }

  .app-user-role {
    display: none;
  }

  .app-logout-text {
    display: none;
  }
}
</style>
