<template>
  <div class="page">
    <section class="card auth-card">
      <h1>Photo Manager</h1>
      <p class="desc">Please login to manage your photos.</p>

      <h2>{{ authMode === 'login' ? 'Login' : 'Register' }}</h2>
      <form class="form" @submit.prevent="submitAuth">
        <input v-model.trim="authForm.username" type="text" placeholder="Username" required />
        <input v-model="authForm.password" type="password" placeholder="Password (min 6 chars)" required />
        <label v-if="authMode === 'register'" class="role">
          <span class="role-label">Role</span>
          <select v-model.number="authForm.roleCode" class="select" required>
            <option :value="3">3 - Normal (manage own)</option>
            <option :value="1">1 - Read-only (view all)</option>
            <option :value="0">0 - Super (all + manage)</option>
          </select>
        </label>
        <button type="submit" :disabled="authLoading">
          {{ authLoading ? 'Please wait...' : authMode === 'login' ? 'Login' : 'Register' }}
        </button>
      </form>

      <p v-if="authError" class="error">{{ authError }}</p>
      <button class="link-btn" @click="toggleAuthMode">
        {{ authMode === 'login' ? 'No account? Register' : 'Have an account? Login' }}
      </button>
    </section>
  </div>
</template>

<script>
import { login, registerWithRole, saveAuth } from '../services/api'

export default {
  name: 'LoginView',
  data() {
    return {
      authMode: 'login',
      authLoading: false,
      authError: '',
      authForm: {
        username: '',
        password: '',
        roleCode: 3
      }
    }
  },
  methods: {
    toggleAuthMode() {
      this.authMode = this.authMode === 'login' ? 'register' : 'login'
      this.authError = ''
    },
    async submitAuth() {
      this.authLoading = true
      this.authError = ''
      try {
        const action = this.authMode === 'login' ? login : registerWithRole
        const { data } =
          this.authMode === 'login'
            ? await action(this.authForm.username, this.authForm.password)
            : await action(this.authForm.username, this.authForm.password, this.authForm.roleCode)
        saveAuth(data.token, data.username, data.roleCode)
        this.$router.replace('/')
      } catch (e) {
        this.authError = e.response?.data?.message || 'Authentication failed'
      } finally {
        this.authLoading = false
      }
    }
  }
}
</script>

<style scoped>
:root {
  font-family: "Helvetica Neue", Arial, sans-serif;
}

* {
  box-sizing: border-box;
}

.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f3f4f6;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
}

.auth-card {
  width: 100%;
  max-width: 420px;
}

h1 {
  margin: 0 0 8px;
}

.desc {
  margin: 0 0 16px;
  color: #6b7280;
}

.form {
  display: grid;
  gap: 10px;
}

input,
button {
  font: inherit;
}

input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
}

.role {
  display: grid;
  gap: 6px;
}

.role-label {
  font-size: 12px;
  color: #6b7280;
}

.select {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
}

button {
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}

button:disabled {
  background: #93c5fd;
}

.link-btn {
  background: transparent;
  color: #2563eb;
  padding: 0;
  margin-top: 12px;
}

.error {
  color: #dc2626;
  margin: 10px 0 0;
}
</style>
