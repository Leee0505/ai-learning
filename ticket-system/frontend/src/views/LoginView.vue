<template>
  <AuthLayout>
    <form class="login-form" @submit.prevent="handleLogin" novalidate>
      <!-- Server-side error banner -->
      <div v-if="serverError" class="login-error-banner" role="alert">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          class="login-error-icon" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
        <span>{{ serverError }}</span>
      </div>

      <!-- Login field -->
      <div class="login-field">
        <label for="login-input" class="login-label">Username or Email</label>
        <div class="login-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="login-input-icon" aria-hidden="true">
            <circle cx="12" cy="8" r="4" />
            <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
          </svg>
          <input
            id="login-input"
            v-model="form.login"
            type="text"
            class="login-input"
            :class="{ 'login-input--error': errors.login }"
            placeholder="Enter username or email"
            autocomplete="username"
            :disabled="loading"
            @input="clearFieldError('login')"
          />
        </div>
        <p v-if="errors.login" class="login-field-error" role="alert">{{ errors.login }}</p>
      </div>

      <!-- Password field -->
      <div class="login-field">
        <label for="password-input" class="login-label">Password</label>
        <div class="login-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="login-input-icon" aria-hidden="true">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          <input
            id="password-input"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="login-input login-input--password"
            :class="{ 'login-input--error': errors.password }"
            placeholder="Enter your password"
            autocomplete="current-password"
            :disabled="loading"
            @input="clearFieldError('password')"
          />
          <button
            type="button"
            class="login-password-toggle"
            :aria-label="showPassword ? 'Hide password' : 'Show password'"
            @click="showPassword = !showPassword"
          >
            <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" class="login-toggle-icon" aria-hidden="true">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" class="login-toggle-icon" aria-hidden="true">
              <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
              <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </button>
        </div>
        <p v-if="errors.password" class="login-field-error" role="alert">{{ errors.password }}</p>
      </div>

      <!-- Remember me + Forgot password -->
      <div class="login-options">
        <label class="login-remember">
          <input v-model="form.rememberMe" type="checkbox" class="login-checkbox" />
          <span>Remember me</span>
        </label>
        <a href="#" class="login-forgot" @click.prevent="handleForgotPassword">
          Forgot password?
        </a>
      </div>

      <!-- Submit button -->
      <button
        type="submit"
        class="login-submit"
        :disabled="loading"
      >
        <svg v-if="loading" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" class="login-spinner" aria-hidden="true">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
        <span v-else>Sign In</span>
      </button>

      <!-- Register link -->
      <p class="login-register">
        Don't have an account?
        <a href="#" @click.prevent="handleRegister">Create one</a>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const showPassword = ref(false)
const serverError = ref('')

const form = reactive({
  login: '',
  password: '',
  rememberMe: false
})

// Per-field inline error messages
const errors = reactive({
  login: '',
  password: ''
})

function clearFieldError(field) {
  errors[field] = ''
  serverError.value = ''
}

function validateForm() {
  let valid = true

  if (!form.login.trim()) {
    errors.login = 'Please enter your username or email'
    valid = false
  } else {
    errors.login = ''
  }

  if (!form.password) {
    errors.password = 'Please enter your password'
    valid = false
  } else if (form.password.length < 6) {
    errors.password = 'Password must be at least 6 characters'
    valid = false
  } else {
    errors.password = ''
  }

  return valid
}

async function handleLogin() {
  serverError.value = ''

  if (!validateForm()) return

  loading.value = true
  try {
    const data = await authStore.login(form.login.trim(), form.password)
    if (data.code === 200) {
      router.push('/')
    } else {
      serverError.value = data.message || 'Login failed'
    }
  } catch (error) {
    if (error.response?.status === 401) {
      serverError.value = 'Invalid username or password'
    } else if (error.response?.data?.message) {
      serverError.value = error.response.data.message
    } else {
      serverError.value = 'Network error, please try again'
    }
  } finally {
    loading.value = false
  }
}

function handleForgotPassword() {
  ElMessage.info('Password reset will be available in a future update.')
}

function handleRegister() {
  ElMessage.info('Registration page will be available in a future update.')
}
</script>

<style scoped>
/* ── Form ── */
.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* ── Error Banner ── */
.login-error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: var(--radius-md);
  color: var(--color-danger);
  font-size: var(--text-sm);
  line-height: 1.5;
  animation: login-shake 0.4s ease;
}

.login-error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

@keyframes login-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-4px); }
  40% { transform: translateX(4px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(2px); }
}

/* ── Field ── */
.login-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.login-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: default;
}

.login-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.login-input-icon {
  position: absolute;
  left: 12px;
  width: 18px;
  height: 18px;
  color: var(--color-text-muted);
  pointer-events: none;
  transition: color var(--transition-fast);
}

.login-input-wrapper:focus-within .login-input-icon {
  color: var(--color-primary);
}

.login-input {
  width: 100%;
  padding: 10px 12px 10px 38px;
  font-size: var(--text-base);
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-gray-50);
  border: 1.5px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
  outline: none;
}

.login-input--password {
  padding-right: 44px;
}

.login-input::placeholder {
  color: var(--color-text-muted);
}

.login-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
  background: var(--color-white);
}

.login-input--error {
  border-color: var(--color-danger);
}

.login-input--error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.login-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-field-error {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-danger);
}

/* ── Password Toggle ── */
.login-password-toggle {
  position: absolute;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: var(--color-text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: color var(--transition-fast), background var(--transition-fast);
}

.login-password-toggle:hover {
  color: var(--color-text-secondary);
  background: var(--color-gray-100);
}

.login-toggle-icon {
  width: 18px;
  height: 18px;
}

/* ── Options Row ── */
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-remember {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  user-select: none;
}

.login-checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.login-forgot {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-primary);
  transition: color var(--transition-fast);
}

.login-forgot:hover {
  color: var(--color-primary-dark);
}

/* ── Submit Button ── */
.login-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 12px 24px;
  font-size: var(--text-base);
  font-weight: 600;
  font-family: var(--font-body);
  color: var(--color-white);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: opacity var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}

.login-submit:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-spinner {
  width: 20px;
  height: 20px;
  animation: login-spin 0.8s linear infinite;
}

@keyframes login-spin {
  to { transform: rotate(360deg); }
}

/* ── Register Link ── */
.login-register {
  margin: 0;
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.login-register a {
  font-weight: 600;
  color: var(--color-primary);
  transition: color var(--transition-fast);
}

.login-register a:hover {
  color: var(--color-primary-dark);
}

/* ── Responsive ── */
@media (max-width: 480px) {
  .login-options {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-sm);
  }
}
</style>
