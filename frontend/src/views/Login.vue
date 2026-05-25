<template>
  <div class="auth-page">
    <div class="card">
      <h2>登录</h2>
      <div class="form-group">
        <label>手机号</label>
        <input v-model="phone" placeholder="请输入手机号" />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="请输入密码" />
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="btn" style="width:100%" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <p style="text-align:center;margin-top:16px;font-size:13px;">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
      <p style="text-align:center;margin-top:8px;font-size:12px;color:#999;">
        演示账号：13800138001 / Abc12345
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const phone = ref('13800138001')
const password = ref('Abc12345')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await authStore.login(phone.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
