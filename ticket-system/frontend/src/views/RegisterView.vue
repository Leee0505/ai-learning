<template>
  <AuthLayout>
    <form class="register-form" @submit.prevent="handleRegister" novalidate>
      <!-- Server-side error banner -->
      <div v-if="serverError" class="register-error-banner" role="alert">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          class="register-error-icon" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
        <span>{{ serverError }}</span>
      </div>

      <!-- Username -->
      <div class="register-field">
        <label for="reg-username" class="register-label">Username</label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <circle cx="12" cy="8" r="4" />
            <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
          </svg>
          <input
            id="reg-username"
            v-model="form.username"
            type="text"
            class="register-input"
            :class="{ 'register-input--error': errors.username }"
            placeholder="Choose a username (3-50 characters)"
            autocomplete="username"
            :disabled="loading"
            @input="clearFieldError('username')"
          />
        </div>
        <p v-if="errors.username" class="register-field-error" role="alert">{{ errors.username }}</p>
      </div>

      <!-- Email -->
      <div class="register-field">
        <label for="reg-email" class="register-label">Email</label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
          </svg>
          <input
            id="reg-email"
            v-model="form.email"
            type="email"
            class="register-input"
            :class="{ 'register-input--error': errors.email }"
            placeholder="your@email.com"
            autocomplete="email"
            :disabled="loading"
            @input="clearFieldError('email')"
          />
        </div>
        <p v-if="errors.email" class="register-field-error" role="alert">{{ errors.email }}</p>
      </div>

      <!-- Phone (optional) -->
      <div class="register-field">
        <label for="reg-phone" class="register-label">Phone <span class="register-label-optional">(optional)</span></label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
            <line x1="12" y1="18" x2="12.01" y2="18" />
          </svg>
          <input
            id="reg-phone"
            v-model="form.phone"
            type="tel"
            class="register-input"
            placeholder="+86 13800138000"
            autocomplete="tel"
            :disabled="loading"
          />
        </div>
      </div>

      <!-- Tenant selection -->
      <div class="register-field">
        <label for="reg-tenant" class="register-label">Organization</label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <path d="M3 21h18" />
            <path d="M5 21V7l8-4v18" />
            <path d="M19 21V11l-6-4" />
            <path d="M9 9v.01" /><path d="M9 12v.01" /><path d="M9 15v.01" /><path d="M9 18v.01" />
          </svg>
          <select
            id="reg-tenant"
            v-model="form.tenantSlug"
            class="register-input register-select"
            :class="{ 'register-input--error': errors.tenantSlug }"
            :disabled="loading || tenantsLoading"
            @change="clearFieldError('tenantSlug')"
          >
            <option value="" disabled>{{ tenantsLoading ? 'Loading...' : 'Select your organization' }}</option>
            <option v-for="t in tenants" :key="t.slug" :value="t.slug">
              {{ t.name }}
            </option>
          </select>
        </div>
        <p v-if="errors.tenantSlug" class="register-field-error" role="alert">{{ errors.tenantSlug }}</p>
      </div>

      <!-- Password -->
      <div class="register-field">
        <label for="reg-password" class="register-label">Password</label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          <input
            id="reg-password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="register-input register-input--password"
            :class="{ 'register-input--error': errors.password }"
            placeholder="At least 6 characters"
            autocomplete="new-password"
            :disabled="loading"
            @input="clearFieldError('password')"
          />
          <button
            type="button"
            class="register-password-toggle"
            :aria-label="showPassword ? 'Hide password' : 'Show password'"
            @click="showPassword = !showPassword"
          >
            <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" class="register-toggle-icon" aria-hidden="true">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" class="register-toggle-icon" aria-hidden="true">
              <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
              <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </button>
        </div>
        <p v-if="errors.password" class="register-field-error" role="alert">{{ errors.password }}</p>
      </div>

      <!-- Confirm password -->
      <div class="register-field">
        <label for="reg-confirm" class="register-label">Confirm Password</label>
        <div class="register-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            class="register-input-icon" aria-hidden="true">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          <input
            id="reg-confirm"
            v-model="form.confirmPassword"
            type="password"
            class="register-input"
            :class="{ 'register-input--error': errors.confirmPassword }"
            placeholder="Re-enter your password"
            autocomplete="new-password"
            :disabled="loading"
            @input="clearFieldError('confirmPassword')"
          />
        </div>
        <p v-if="errors.confirmPassword" class="register-field-error" role="alert">{{ errors.confirmPassword }}</p>
      </div>

      <!-- Submit -->
      <button
        type="submit"
        class="register-submit"
        :disabled="loading"
      >
        <svg v-if="loading" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" class="register-spinner" aria-hidden="true">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
        <span v-else>Create Account</span>
      </button>

      <!-- Login link -->
      <p class="register-login">
        Already have an account?
        <router-link to="/login">Sign in</router-link>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getPublicTenantsApi } from '@/api/tenant'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const showPassword = ref(false)
const serverError = ref('')
const tenantsLoading = ref(true)
const tenants = ref([])

const form = reactive({
  username: '',
  email: '',
  phone: '',
  tenantSlug: '',
  password: '',
  confirmPassword: ''
})

const errors = reactive({
  username: '',
  email: '',
  tenantSlug: '',
  password: '',
  confirmPassword: ''
})

function clearFieldError(field) {
  errors[field] = ''
  serverError.value = ''
}

function validateForm() {
  let valid = true

  if (!form.username.trim()) {
    errors.username = 'Please enter a username'
    valid = false
  } else if (form.username.trim().length < 3) {
    errors.username = 'Username must be at least 3 characters'
    valid = false
  } else {
    errors.username = ''
  }

  if (!form.email.trim()) {
    errors.email = 'Please enter your email'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = 'Please enter a valid email address'
    valid = false
  } else {
    errors.email = ''
  }

  if (!form.tenantSlug) {
    errors.tenantSlug = 'Please select an organization'
    valid = false
  } else {
    errors.tenantSlug = ''
  }

  if (!form.password) {
    errors.password = 'Please enter a password'
    valid = false
  } else if (form.password.length < 6) {
    errors.password = 'Password must be at least 6 characters'
    valid = false
  } else {
    errors.password = ''
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = 'Please confirm your password'
    valid = false
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match'
    valid = false
  } else {
    errors.confirmPassword = ''
  }

  return valid
}

async function handleRegister() {
  serverError.value = ''

  if (!validateForm()) return

  loading.value = true
  try {
    const data = await authStore.register({
      username: form.username.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || undefined,
      tenantSlug: form.tenantSlug,
      password: form.password
    })
    if (data.code === 200) {
      router.push('/')
    } else {
      serverError.value = data.message || 'Registration failed'
    }
  } catch (error) {
    if (error.response?.data?.message) {
      serverError.value = error.response.data.message
    } else {
      serverError.value = 'Network error, please try again'
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await getPublicTenantsApi()
    if (data.code === 200) {
      tenants.value = data.data
    }
  } catch {
    serverError.value = 'Failed to load organizations. Please refresh the page.'
  } finally {
    tenantsLoading.value = false
  }
})
</script>

<style scoped>
/* ── Form ── */
.register-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* ── Error Banner ── */
.register-error-banner {
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
  animation: register-shake 0.4s ease;
}

.register-error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

@keyframes register-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-4px); }
  40% { transform: translateX(4px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(2px); }
}

/* ── Field ── */
.register-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.register-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: default;
}

.register-label-optional {
  font-weight: 400;
  color: var(--color-text-muted);
}

.register-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.register-input-icon {
  position: absolute;
  left: 12px;
  width: 18px;
  height: 18px;
  color: var(--color-text-muted);
  pointer-events: none;
  transition: color var(--transition-fast);
}

.register-input-wrapper:focus-within .register-input-icon {
  color: var(--color-primary);
}

.register-input {
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

.register-input--password {
  padding-right: 44px;
}

.register-input::placeholder {
  color: var(--color-text-muted);
}

.register-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
  background: var(--color-white);
}

.register-input--error {
  border-color: var(--color-danger);
}

.register-input--error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.register-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Select variant ── */
.register-select {
  appearance: none;
  cursor: pointer;
  padding-right: 36px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
}

.register-select:invalid {
  color: var(--color-text-muted);
}

.register-field-error {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-danger);
}

/* ── Password Toggle ── */
.register-password-toggle {
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

.register-password-toggle:hover {
  color: var(--color-text-secondary);
  background: var(--color-gray-100);
}

.register-toggle-icon {
  width: 18px;
  height: 18px;
}

/* ── Submit Button ── */
.register-submit {
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

.register-submit:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35);
}

.register-submit:active:not(:disabled) {
  transform: translateY(0);
}

.register-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.register-spinner {
  width: 20px;
  height: 20px;
  animation: register-spin 0.8s linear infinite;
}

@keyframes register-spin {
  to { transform: rotate(360deg); }
}

/* ── Login Link ── */
.register-login {
  margin: 0;
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.register-login a {
  font-weight: 600;
  color: var(--color-primary);
  transition: color var(--transition-fast);
}

.register-login a:hover {
  color: var(--color-primary-dark);
}

/* ── Responsive ── */
@media (max-width: 480px) {
  .register-form {
    gap: var(--space-md);
  }
}
</style>
