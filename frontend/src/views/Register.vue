<template>
  <div class="auth-page">
    <div class="card">
      <h2>注册</h2>
      <div class="form-group">
        <label>手机号</label>
        <input v-model="phone" placeholder="请输入11位手机号" />
      </div>
      <div class="form-group">
        <label>用户名</label>
        <input v-model="username" placeholder="4-16位，中英文数字组合" />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="至少8位，包含字母和数字" />
      </div>
      <div class="form-group">
        <label>短信验证码</label>
        <input v-model="smsCode" placeholder="输入123456（演示环境）" />
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="btn" style="width:100%" @click="handleRegister" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>
      <p style="text-align:center;margin-top:16px;font-size:13px;">
        已有账号？<router-link to="/login">去登录</router-link>
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
const phone = ref('')
const username = ref('')
const password = ref('')
const smsCode = ref('123456')
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  loading.value = true
  error.value = ''
  try {
    await authStore.register({
      phone: phone.value,
      username: username.value,
      password: password.value,
      smsCode: smsCode.value
    })
    router.push('/login')
  } catch (e) {
    error.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
