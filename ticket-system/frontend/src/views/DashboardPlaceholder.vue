<template>
  <div class="dashboard">
    <!-- Top navigation bar -->
    <header class="dashboard-nav">
      <div class="dashboard-nav-inner">
        <!-- Brand -->
        <div class="dashboard-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="dashboard-brand-icon" aria-hidden="true">
            <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
            <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
          </svg>
          <span class="dashboard-brand-text">Ticket System</span>
        </div>

        <!-- User section -->
        <div class="dashboard-user">
          <div class="dashboard-user-avatar" aria-hidden="true">
            {{ userInitial }}
          </div>
          <span class="dashboard-user-name">{{ authStore.user?.username }}</span>
          <span class="dashboard-user-role">{{ roleLabel }}</span>
          <button
            class="dashboard-logout-btn"
            @click="handleLogout"
            title="Sign out"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              class="dashboard-logout-icon" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16,17 21,12 16,7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span class="dashboard-logout-text">Sign Out</span>
          </button>
        </div>
      </div>
    </header>

    <!-- Main content area -->
    <main class="dashboard-main">
      <div class="dashboard-welcome">
        <h1 class="dashboard-welcome-title">
          Welcome back, {{ authStore.user?.username }}
        </h1>
        <p class="dashboard-welcome-subtitle">
          Phase 2 dashboard is coming soon. Your ticket management workspace is being built.
        </p>
      </div>

      <!-- Quick stats placeholder cards -->
      <div class="dashboard-stats">
        <div class="dashboard-stat-card">
          <div class="dashboard-stat-icon dashboard-stat-icon--tickets">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
              <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
            </svg>
          </div>
          <div class="dashboard-stat-body">
            <span class="dashboard-stat-value">—</span>
            <span class="dashboard-stat-label">My Tickets</span>
          </div>
        </div>

        <div class="dashboard-stat-card">
          <div class="dashboard-stat-icon dashboard-stat-icon--pending">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12,6 12,12 16,14" />
            </svg>
          </div>
          <div class="dashboard-stat-body">
            <span class="dashboard-stat-value">—</span>
            <span class="dashboard-stat-label">Pending</span>
          </div>
        </div>

        <div class="dashboard-stat-card">
          <div class="dashboard-stat-icon dashboard-stat-icon--completed">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22,4 12,14.01 9,11.01" />
            </svg>
          </div>
          <div class="dashboard-stat-body">
            <span class="dashboard-stat-value">—</span>
            <span class="dashboard-stat-label">Completed</span>
          </div>
        </div>

        <div class="dashboard-stat-card">
          <div class="dashboard-stat-icon dashboard-stat-icon--response">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <polyline points="22,12 18,12 15,21 9,3 6,12 2,12" />
            </svg>
          </div>
          <div class="dashboard-stat-body">
            <span class="dashboard-stat-value">—</span>
            <span class="dashboard-stat-label">Avg Response</span>
          </div>
        </div>
      </div>
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
.dashboard {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-gray-50);
}

/* ── Navigation ── */
.dashboard-nav {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-gray-200);
  box-shadow: var(--shadow-sm);
}

.dashboard-nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 56px;
}

/* Brand */
.dashboard-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.dashboard-brand-icon {
  width: 24px;
  height: 24px;
  color: var(--color-primary);
}

.dashboard-brand-text {
  font-family: var(--font-heading);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* User */
.dashboard-user {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.dashboard-user-avatar {
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

.dashboard-user-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

.dashboard-user-role {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.dashboard-logout-btn {
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

.dashboard-logout-btn:hover {
  color: var(--color-danger);
  background: #FEF2F2;
  border-color: #FECACA;
}

.dashboard-logout-icon {
  width: 16px;
  height: 16px;
}

/* ── Main Content ── */
.dashboard-main {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg);
}

/* Welcome */
.dashboard-welcome {
  margin-bottom: var(--space-xl);
}

.dashboard-welcome-title {
  margin: 0 0 var(--space-xs);
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
}

.dashboard-welcome-subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

/* Stats Grid */
.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
}

.dashboard-stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
  cursor: pointer;
}

.dashboard-stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.dashboard-stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.dashboard-stat-icon svg {
  width: 24px;
  height: 24px;
}

.dashboard-stat-icon--tickets {
  background: #EDE9FE;
  color: var(--color-primary);
}

.dashboard-stat-icon--pending {
  background: #FEF3C7;
  color: var(--color-warning);
}

.dashboard-stat-icon--completed {
  background: #D1FAE5;
  color: var(--color-success);
}

.dashboard-stat-icon--response {
  background: #DBEAFE;
  color: var(--color-info);
}

.dashboard-stat-body {
  display: flex;
  flex-direction: column;
}

.dashboard-stat-value {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.dashboard-stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .dashboard-nav-inner {
    padding: 0 var(--space-md);
  }

  .dashboard-main {
    padding: var(--space-lg) var(--space-md);
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
  }

  .dashboard-user-role {
    display: none;
  }

  .dashboard-logout-text {
    display: none;
  }

  .dashboard-welcome-title {
    font-size: var(--text-xl);
  }
}
</style>
