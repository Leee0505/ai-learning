<template>
  <div class="app-layout">
    <!-- Top navigation bar — persistent across all authenticated pages -->
    <header class="app-nav">
      <div class="app-nav-inner">
        <!-- Brand -->
        <router-link to="/" class="app-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="app-brand-icon" aria-hidden="true">
            <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
            <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
          </svg>
          <span class="app-brand-text">Ticket System</span>
        </router-link>

        <!-- Nav links -->
        <nav class="app-nav-links">
          <router-link to="/" class="app-nav-link" active-class="app-nav-link--active" exact>
            Dashboard
          </router-link>
          <router-link v-if="authStore.isAgent || authStore.isAdmin" to="/workbench" class="app-nav-link" active-class="app-nav-link--active">
            Workbench
          </router-link>
          <router-link to="/tickets" class="app-nav-link" active-class="app-nav-link--active">
            Tickets
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/users" class="app-nav-link" active-class="app-nav-link--active">
            Users
          </router-link>
        </nav>

        <!-- User section -->
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
    </header>

    <!-- Page content -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const userInitial = computed(() => {
  return authStore.user?.username?.charAt(0)?.toUpperCase() || 'U'
})

const roleLabel = computed(() => {
  const role = authStore.user?.role
  if (role === 'ROLE_ADMIN') return 'Admin'
  if (role === 'ROLE_AGENT') return 'Agent'
  return 'User'
})

async function handleLogout() {
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
  justify-content: space-between;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 56px;
}

/* Brand */
.app-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-decoration: none;
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
  gap: var(--space-sm);
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
