<template>
  <div class="auth-layout">
    <!-- Background decoration -->
    <div class="auth-bg-decor">
      <div class="auth-bg-circle auth-bg-circle--1" />
      <div class="auth-bg-circle auth-bg-circle--2" />
      <div class="auth-bg-circle auth-bg-circle--3" />
    </div>

    <div class="auth-card">
      <!-- Brand header -->
      <div class="auth-brand">
        <div class="auth-logo" aria-hidden="true">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="auth-logo-icon"
          >
            <!-- Ticket icon: Lucide style -->
            <path d="M19.5 12.572V8.5a2 2 0 0 0-2-2h-12a2 2 0 0 0-2 2v4.072a2 2 0 0 1 0 3.856V20.5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4.072a2 2 0 0 1 0-3.856Z" />
            <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h4" />
          </svg>
        </div>
        <h1 class="auth-title">Ticket System</h1>
        <p class="auth-subtitle">Enterprise Helpdesk Platform</p>
      </div>

      <slot />
    </div>

    <!-- Footer -->
    <p class="auth-footer">
      &copy; {{ currentYear }} Ticket Management System
    </p>
  </div>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
/* ── Layout ── */
.auth-layout {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--space-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  overflow: hidden;
}

/* ── Background Decorative Circles ── */
.auth-bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.auth-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}

.auth-bg-circle--1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -150px;
}

.auth-bg-circle--2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -100px;
}

.auth-bg-circle--3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.03);
}

/* ── Card ── */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: var(--color-white);
  border-radius: var(--radius-xl);
  padding: var(--space-2xl) var(--space-xl);
  box-shadow: var(--shadow-xl);
  animation: auth-card-enter 0.4s ease;
}

@keyframes auth-card-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Brand / Logo ── */
.auth-brand {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: var(--color-white);
  border-radius: var(--radius-lg);
  margin: 0 auto var(--space-md);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35);
}

.auth-logo-icon {
  width: 28px;
  height: 28px;
}

.auth-title {
  margin: 0 0 var(--space-xs);
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--color-text-primary);
}

.auth-subtitle {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-weight: 400;
}

/* ── Footer ── */
.auth-footer {
  position: relative;
  z-index: 1;
  margin-top: var(--space-lg);
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
}

/* ── Responsive ── */
@media (max-width: 480px) {
  .auth-layout {
    padding: var(--space-md);
  }

  .auth-card {
    padding: var(--space-lg) var(--space-lg);
    border-radius: var(--radius-lg);
  }

  .auth-logo {
    width: 48px;
    height: 48px;
  }

  .auth-logo-icon {
    width: 24px;
    height: 24px;
  }

  .auth-title {
    font-size: var(--text-lg);
  }
}
</style>
