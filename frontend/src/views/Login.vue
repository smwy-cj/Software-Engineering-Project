<template>
  <div class="auth-page glass-page">
    <section class="auth-layout">
      <aside class="auth-intro glass-surface">
        <span class="hero-kicker">Campus mutual aid</span>
        <h1><span class="liquid-highlight">青隅</span> CampusHub</h1>
        <p>把倾诉、搭子、心动和通知放在一个清晰可信的校园互助空间里。</p>
        <div class="auth-points">
          <span class="glass-tag">匿名树洞</span>
          <span class="glass-tag">同校搭子</span>
          <span class="glass-tag">温和匹配</span>
        </div>
      </aside>

      <div class="auth-card glass-card">
        <div class="auth-brand">
          <h2>欢迎回来</h2>
          <p>登录后继续查看校园里的新回应</p>
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input class="glass-input" v-model="phone" placeholder="请输入手机号" autocomplete="tel" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input class="glass-input" v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="auth-submit glass-button-primary" @click="handleLogin" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="auth-switch">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
        <p class="auth-demo">
          演示账号：13800138001 / Abc12345
        </p>
      </div>
    </section>
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
