<template>
  <div class="profile">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Profile</h1>
        <p class="page-subtitle">Manage your account settings</p>
      </div>
    </header>

    <div class="profile-card">
      <h2 class="profile-section-title">Change Password</h2>
      <form @submit.prevent="handleChangePassword" novalidate class="profile-form">
        <div class="profile-field">
          <label for="old-pw" class="profile-label">Current Password</label>
          <input id="old-pw" v-model="form.oldPassword" type="password" class="profile-input"
                 :class="{ 'profile-input--error': errors.oldPassword }"
                 placeholder="Enter current password" :disabled="loading" />
          <p v-if="errors.oldPassword" class="profile-error">{{ errors.oldPassword }}</p>
        </div>

        <div class="profile-field">
          <label for="new-pw" class="profile-label">New Password</label>
          <input id="new-pw" v-model="form.newPassword" type="password" class="profile-input"
                 :class="{ 'profile-input--error': errors.newPassword }"
                 placeholder="At least 6 characters" :disabled="loading" />
          <p v-if="errors.newPassword" class="profile-error">{{ errors.newPassword }}</p>
        </div>

        <div class="profile-field">
          <label for="confirm-pw" class="profile-label">Confirm New Password</label>
          <input id="confirm-pw" v-model="confirmPassword" type="password" class="profile-input"
                 :class="{ 'profile-input--error': errors.confirm }"
                 placeholder="Re-enter new password" :disabled="loading" />
          <p v-if="errors.confirm" class="profile-error">{{ errors.confirm }}</p>
        </div>

        <div v-if="serverError" class="profile-server-error" role="alert">{{ serverError }}</div>
        <div v-if="successMsg" class="profile-success">{{ successMsg }}</div>

        <button type="submit" class="profile-submit" :disabled="loading">
          {{ loading ? 'Updating...' : 'Change Password' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { messageSuccess, messageError, messageWarning, messageInfo } from "@/utils/message"
import request from '@/api/request'

const loading = ref(false)
const serverError = ref('')
const successMsg = ref('')
const confirmPassword = ref('')

const form = reactive({ oldPassword: '', newPassword: '' })
const errors = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function clearErrors() {
  errors.oldPassword = ''
  errors.newPassword = ''
  errors.confirm = ''
  serverError.value = ''
  successMsg.value = ''
}

function validate() {
  clearErrors()
  let valid = true
  if (!form.oldPassword) { errors.oldPassword = 'Current password is required'; valid = false }
  if (!form.newPassword || form.newPassword.length < 6) { errors.newPassword = 'At least 6 characters'; valid = false }
  if (form.newPassword !== confirmPassword.value) { errors.confirm = 'Passwords do not match'; valid = false }
  return valid
}

async function handleChangePassword() {
  if (!validate()) return
  loading.value = true
  try {
    const { data } = await request.put('/auth/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    if (data.code === 200) {
      successMsg.value = 'Password changed successfully'
      form.oldPassword = ''
      form.newPassword = ''
      confirmPassword.value = ''
      messageSuccess('Password changed')
    } else {
      serverError.value = data.message || 'Failed to change password'
    }
  } catch (e) {
    serverError.value = e.response?.data?.message || 'Failed to change password'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile { max-width: 600px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { margin-bottom: var(--space-lg); }
.page-title { margin: 0 0 var(--space-xs); font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); }
.page-subtitle { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

.profile-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-xl); box-shadow: var(--shadow-sm); }
.profile-section-title { margin: 0 0 var(--space-lg); font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); }

.profile-form { display: flex; flex-direction: column; gap: var(--space-md); }
.profile-field { display: flex; flex-direction: column; gap: var(--space-xs); }
.profile-label { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.profile-input {
  padding: 10px 12px; font-size: var(--text-base); font-family: var(--font-body);
  color: var(--color-text-primary); background: var(--color-gray-50);
  border: 1.5px solid var(--color-gray-200); border-radius: var(--radius-md);
  outline: none; transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.profile-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12); }
.profile-input--error { border-color: var(--color-danger); }
.profile-error { margin: 0; font-size: var(--text-xs); color: var(--color-danger); }

.profile-server-error { padding: var(--space-sm) var(--space-md); background: #FEF2F2; border: 1px solid #FECACA; border-radius: var(--radius-md); color: var(--color-danger); font-size: var(--text-sm); }
.profile-success { padding: var(--space-sm) var(--space-md); background: #ECFDF5; border: 1px solid #A7F3D0; border-radius: var(--radius-md); color: #047857; font-size: var(--text-sm); }

.profile-submit {
  padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body);
  color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none; border-radius: var(--radius-md); cursor: pointer; align-self: flex-start;
}
.profile-submit:hover:not(:disabled) { opacity: 0.92; }
.profile-submit:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
