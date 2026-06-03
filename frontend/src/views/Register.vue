<template>
  <div class="auth-page glass-page">
    <section class="auth-layout">
      <aside class="auth-intro glass-surface">
        <span class="hero-kicker">Start with trust</span>
        <h1><span class="liquid-highlight">青隅</span> CampusHub</h1>
        <p>用更轻的方式建立连接，也用清晰规则保护每一次表达。</p>
        <div class="auth-points">
          <span class="glass-tag">学生身份</span>
          <span class="glass-tag">社区审核</span>
          <span class="glass-tag">隐私友好</span>
        </div>
      </aside>

      <div class="auth-card glass-card">
        <div class="auth-brand">
          <h2>创建账号</h2>
          <p>注册后即可发布树洞、寻找搭子和管理个人资料</p>
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input class="glass-input" v-model="phone" placeholder="请输入11位手机号" autocomplete="tel" />
        </div>
        <div class="form-group">
          <label>用户名</label>
          <input class="glass-input" v-model="username" placeholder="不超过16位，需保持唯一" autocomplete="username" maxlength="16" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input class="glass-input" v-model="password" type="password" placeholder="至少8位，包含字母和数字" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label>重复密码</label>
          <input class="glass-input" v-model="confirmPassword" type="password" placeholder="请再次输入密码" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label>验证码</label>
          <div class="captcha-row">
            <input class="glass-input" v-model="captchaCode" placeholder="输入右侧4位验证码" maxlength="4" autocomplete="off" />
            <button class="captcha-code" type="button" @click="loadCaptcha" :disabled="captchaLoading">
              <span>{{ captchaDisplay }}</span>
            </button>
          </div>
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="auth-submit glass-button-primary" @click="handleRegister" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="auth-switch">
          已有账号？<router-link to="/login">去登录</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { getApiErrorMessage } from '../api'

const router = useRouter()
const authStore = useAuthStore()
const phone = ref('')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const captchaId = ref('')
const captchaText = ref('----')
const captchaCode = ref('')
const captchaLoading = ref(false)
const loading = ref(false)
const error = ref('')

async function loadCaptcha() {
  captchaLoading.value = true
  error.value = ''
  try {
    const captcha = await authStore.getCaptcha()
    captchaId.value = captcha.captchaId
    captchaText.value = captcha.captchaCode || '刷新'
    captchaCode.value = ''
  } catch (e) {
    captchaText.value = '刷新'
    error.value = '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

async function handleRegister() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    await authStore.register({
      phone: phone.value,
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value
    })
    router.push('/login')
  } catch (e) {
    error.value = getApiErrorMessage(e, '注册失败')
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)

const captchaDisplay = computed(() => {
  if (captchaLoading.value) return '...'
  return captchaText.value || '刷新'
})
</script>

<style scoped>
.captcha-row {
  display: grid;
  grid-template-columns: 1fr 104px;
  gap: 10px;
  align-items: center;
}

.captcha-code {
  height: 40px;
  border: 1px solid rgba(77, 107, 168, 0.24);
  border-radius: 10px;
  background: rgba(238, 244, 255, 0.82);
  color: #183b7a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 17px;
  font-family: Consolas, "Courier New", monospace;
  line-height: 1;
  letter-spacing: 1.5px;
  text-align: center;
  white-space: nowrap;
  cursor: pointer;
}

.captcha-code span {
  display: block;
  min-width: 4.5em;
}

.captcha-code:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}
</style>
