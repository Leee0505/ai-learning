# P0: RegisterView + Admin Cross-Tenant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** (Phase 1) Build the complete user self-registration flow with tenant selection. (Phase 2) Enable admin users to view and manage data across all tenants.

**Architecture:** Phase 1 adds a public `/api/public/tenants` endpoint (no auth), then builds a `RegisterView.vue` page following the exact patterns of `LoginView.vue` — same `AuthLayout`, same CSS variables, same validation style. Phase 2 adds a single-line admin check in `MyBatisPlusConfig.getTenantId()` returning `null` to skip SQL tenant injection for admins, plus four supporting bypasses in services that use manual tenant filters.

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus 3.5 + Vue 3.4 + Element Plus + Pinia

---

## Phase 1: RegisterView — User Self-Registration with Tenant Selection

### Task 1: Backend — Add public tenant list endpoint

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/PublicController.java`
- Create: `ticket-system/backend/src/test/java/com/ticket/controller/PublicControllerTest.java`

- [ ] **Step 1: Create PublicController**

```java
package com.ticket.controller;

import com.ticket.dto.response.ApiResult;
import com.ticket.entity.Tenant;
import com.ticket.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Public endpoints for registration and discovery")
public class PublicController {

    private final TenantMapper tenantMapper;

    public PublicController(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @GetMapping("/tenants")
    @Operation(summary = "List enabled tenants (no auth required)",
               description = "Returns all active tenants for the registration form dropdown. No authentication required.")
    public ApiResult<List<Tenant>> listTenants() {
        List<Tenant> tenants = tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getStatus, 1)
                        .orderByAsc(Tenant::getName));
        return ApiResult.success(tenants);
    }
}
```

- [ ] **Step 2: Create PublicControllerTest**

```java
package com.ticket.controller;

import com.ticket.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listTenantsShouldReturn200WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listTenantsShouldReturnArray() throws Exception {
        mockMvc.perform(get("/api/public/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
```

- [ ] **Step 3: Run backend tests**

```bash
cd ticket-system/backend && mvn test -pl . -Dtest="PublicControllerTest" -Dspring.profiles.active=test
```

Expected: 2 tests PASS

- [ ] **Step 4: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/controller/PublicController.java \
        ticket-system/backend/src/test/java/com/ticket/controller/PublicControllerTest.java
git commit -m "feat: add public tenant list endpoint for registration form"
```

---

### Task 2: Frontend — Add tenant API + auth store register action

**Files:**
- Modify: `ticket-system/frontend/src/api/tenant.js`
- Modify: `ticket-system/frontend/src/stores/auth.js`

- [ ] **Step 1: Add public tenant API to tenant.js**

File: `ticket-system/frontend/src/api/tenant.js`

Replace the entire file content with:

```javascript
import request from './request'

// Admin-only: list all tenants (for user management dropdowns)
export function getTenantsApi() {
  return request.get('/admin/tenants')
}

// Public: list enabled tenants (for registration form dropdown)
export function getPublicTenantsApi() {
  return request.get('/public/tenants')
}
```

- [ ] **Step 2: Add register action to auth store**

File: `ticket-system/frontend/src/stores/auth.js`

Change the import line (line 3) from:
```javascript
import { loginApi, logoutApi, getCurrentUserApi } from '@/api/auth'
```
to:
```javascript
import { loginApi, registerApi, logoutApi, getCurrentUserApi } from '@/api/auth'
```

Add the `register` action after the `login` action (after line 33). Insert:

```javascript
  async function register(registerData) {
    const { data } = await registerApi(registerData)
    if (data.code === 200) {
      setTokens(data.data.accessToken, data.data.refreshToken)
      setUser(data.data.user)
    }
    return data
  }
```

Update the return statement (line 62-66) to include `register`:

```javascript
    return {
      accessToken, refreshToken, user,
      isLoggedIn, isAdmin, isAgent,
      setTokens, setUser, login, register, fetchUser, logout, clearAuth
    }
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/api/tenant.js \
        ticket-system/frontend/src/stores/auth.js
git commit -m "feat: add public tenant API and auth store register action"
```

---

### Task 3: Frontend — Create RegisterView.vue

**Files:**
- Create: `ticket-system/frontend/src/views/RegisterView.vue`

- [ ] **Step 1: Create RegisterView.vue**

```vue
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
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/frontend/src/views/RegisterView.vue
git commit -m "feat: add RegisterView with tenant selection and validation"
```

---

### Task 4: Frontend — Add route + update LoginView link

**Files:**
- Modify: `ticket-system/frontend/src/router/index.js`
- Modify: `ticket-system/frontend/src/views/LoginView.vue`

- [ ] **Step 1: Add /register route to router**

File: `ticket-system/frontend/src/router/index.js`

In the `routes` array, add after the `/login` route (after line 10):

```javascript
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
```

Also, in the `beforeEach` guard, the existing logic already redirects logged-in users away from `/login`. Add the same treatment for `/register` by changing line 115:

Change:
```javascript
  if (to.path === '/login' && authStore.accessToken) {
```
To:
```javascript
  if ((to.path === '/login' || to.path === '/register') && authStore.accessToken) {
```

- [ ] **Step 2: Update LoginView handleRegister**

File: `ticket-system/frontend/src/views/LoginView.vue`

Change line 199-201 from:
```javascript
function handleRegister() {
  ElMessage.info('Registration page will be available in a future update.')
}
```
To:
```javascript
function handleRegister() {
  router.push('/register')
}
```

Remove the unused `ElMessage` import on line 118 if it is only used by `handleRegister` and `handleForgotPassword`. Since `handleForgotPassword` (line 196) still uses `ElMessage.info(...)`, keep the import.

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/router/index.js \
        ticket-system/frontend/src/views/LoginView.vue
git commit -m "feat: add /register route and wire LoginView link to registration page"
```

---

### Task 5: Build & verify Phase 1

- [ ] **Step 1: Build backend**

```bash
cd ticket-system/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Build frontend**

```bash
cd ticket-system/frontend && npm run build 2>&1 | tail -5
```

Expected: No TypeScript/Vite errors

- [ ] **Step 3: Run all backend tests**

```bash
cd ticket-system/backend && mvn test -Dspring.profiles.active=test 2>&1 | tail -20
```

Expected: All tests PASS (including new PublicControllerTest)

---

## Phase 2: Admin Cross-Tenant Viewing

### Task 6: MyBatisPlusConfig — Admin bypass in getTenantId()

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/config/MyBatisPlusConfig.java`

- [ ] **Step 1: Add admin check to getTenantId()**

File: `ticket-system/backend/src/main/java/com/ticket/config/MyBatisPlusConfig.java`

Change line 25-27 from:
```java
            @Override
            public Expression getTenantId() {
                return new LongValue(SecurityUtils.getCurrentTenantId());
            }
```
To:
```java
            @Override
            public Expression getTenantId() {
                // Admin users bypass tenant isolation to see cross-tenant data.
                // Returning null tells MyBatis-Plus to skip injecting WHERE tenant_id = ?
                if (SecurityUtils.isAdmin()) {
                    return null;
                }
                return new LongValue(SecurityUtils.getCurrentTenantId());
            }
```

- [ ] **Step 2: Run backend tests to catch regressions**

```bash
cd ticket-system/backend && mvn test -Dspring.profiles.active=test 2>&1 | tail -10
```

Expected: Existing tests PASS

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/config/MyBatisPlusConfig.java
git commit -m "feat: admin bypass tenant isolation in MyBatis-Plus interceptor"
```

---

### Task 7: AgentController — Admin cross-tenant agent listing

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/controller/AgentController.java`

- [ ] **Step 1: Add admin bypass to listAgents()**

File: `ticket-system/backend/src/main/java/com/ticket/controller/AgentController.java`

Change lines 42-43 from:
```java
        var agents = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, SecurityUtils.getCurrentTenantId())
```
To:
```java
        var wrapper = new LambdaQueryWrapper<User>();
        // Admin sees agents from all tenants; others see only their own tenant
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(User::getTenantId, SecurityUtils.getCurrentTenantId());
        }
        var agents = userMapper.selectList(wrapper
```

And add the closing parenthesis for the `selectList` call. The full method becomes:

```java
    @GetMapping
    @Operation(summary = "List all active agents",
               description = "Returns enabled users with ROLE_AGENT, ordered by username. Used for ticket assignment dropdowns.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of agent users (id + username)"),
        @ApiResponse(responseCode = "403", description = "Requires AGENT or ADMIN role")
    })
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    public ApiResult<List<UserResponse>> listAgents() {
        var wrapper = new LambdaQueryWrapper<User>();
        // Admin sees agents from all tenants; others see only their own tenant
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(User::getTenantId, SecurityUtils.getCurrentTenantId());
        }
        var agents = userMapper.selectList(wrapper
                .eq(User::getRole, RoleConstants.ROLE_AGENT)
                .eq(User::getStatus, 1) // only enabled agents
                .orderByAsc(User::getUsername))
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ApiResult.success(agents);
    }
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/controller/AgentController.java
git commit -m "feat: admin sees agents from all tenants in assignment dropdown"
```

---

### Task 8: ReplyTemplateServiceImpl — Admin cross-tenant template listing

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/impl/ReplyTemplateServiceImpl.java`

- [ ] **Step 1: Add admin bypass to listTemplates()**

File: `ticket-system/backend/src/main/java/com/ticket/service/impl/ReplyTemplateServiceImpl.java`

Change lines 35-39 from:
```java
        Long tenantId = SecurityUtils.getCurrentTenantId();
        LambdaQueryWrapper<ReplyTemplate> wrapper = new LambdaQueryWrapper<>();
        // System defaults (NULL) + current tenant's templates
        wrapper.and(w -> w.isNull(ReplyTemplate::getTenantId)
                .or().eq(ReplyTemplate::getTenantId, tenantId));
```
To:
```java
        LambdaQueryWrapper<ReplyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (SecurityUtils.isAdmin()) {
            // Admin sees all templates across all tenants (no tenant filter)
        } else {
            Long tenantId = SecurityUtils.getCurrentTenantId();
            // System defaults (NULL) + current tenant's templates
            wrapper.and(w -> w.isNull(ReplyTemplate::getTenantId)
                    .or().eq(ReplyTemplate::getTenantId, tenantId));
        }
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/impl/ReplyTemplateServiceImpl.java
git commit -m "feat: admin sees all reply templates across tenants"
```

---

### Task 9: KnowledgeServiceImpl — Admin cross-tenant article listing

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/impl/KnowledgeServiceImpl.java`

- [ ] **Step 1: Add admin bypass to search()**

File: `ticket-system/backend/src/main/java/com/ticket/service/impl/KnowledgeServiceImpl.java`

Change lines 32-36 from:
```java
        Long tenantId = SecurityUtils.getCurrentTenantId();
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        // System defaults (NULL) + current tenant's articles
        wrapper.and(w -> w.isNull(KnowledgeArticle::getTenantId)
                .or().eq(KnowledgeArticle::getTenantId, tenantId));
```
To:
```java
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (SecurityUtils.isAdmin()) {
            // Admin sees all articles across all tenants (no tenant filter)
        } else {
            Long tenantId = SecurityUtils.getCurrentTenantId();
            // System defaults (NULL) + current tenant's articles
            wrapper.and(w -> w.isNull(KnowledgeArticle::getTenantId)
                    .or().eq(KnowledgeArticle::getTenantId, tenantId));
        }
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/impl/KnowledgeServiceImpl.java
git commit -m "feat: admin sees all knowledge articles across tenants"
```

---

### Task 10: AdminServiceImpl — Fix updateUser uniqueness check across tenants

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/impl/AdminServiceImpl.java`

- [ ] **Step 1: Fix updateUser to use target user's tenant for uniqueness checks**

File: `ticket-system/backend/src/main/java/com/ticket/service/impl/AdminServiceImpl.java`

Change lines 123-124 (username check) from:
```java
            // Check uniqueness within tenant
            Long tenantId = SecurityUtils.getCurrentTenantId();
```
To:
```java
            // Check uniqueness within the target user's tenant (not admin's own)
            Long tenantId = user.getTenantId();
```

Change lines 137 (email check) from:
```java
            Long tenantId = SecurityUtils.getCurrentTenantId();
```
To:
```java
            Long tenantId = user.getTenantId();
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/impl/AdminServiceImpl.java
git commit -m "fix: use target user's tenant for uniqueness checks in updateUser"
```

---

### Task 11: Build & verify Phase 2

- [ ] **Step 1: Build backend**

```bash
cd ticket-system/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run all backend tests**

```bash
cd ticket-system/backend && mvn test -Dspring.profiles.active=test 2>&1 | tail -20
```

Expected: All tests PASS

- [ ] **Step 3: Full project build**

```bash
cd ticket-system/backend && mvn clean package -DskipTests -q
```

Expected: BUILD SUCCESS
