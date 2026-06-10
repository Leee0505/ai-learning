<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">T</div>
        <h2>Ticket System</h2>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup>
// Wraps auth pages in a centered card layout
</script>

<style scoped>
.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  width: 48px;
  height: 48px;
  background: #667eea;
  color: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin: 0 auto 12px;
}

.auth-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
}
</style>
