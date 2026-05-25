import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const unreadCount = ref(0)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.accountStatus === 'NORMAL')

  async function login(phone, password) {
    const res = await api.post('/auth/login', {
      phone, password, loginType: 'password', captcha: 'mock', captchaId: 'mock'
    })
    token.value = res.data.data.accessToken
    user.value = res.data.data.userInfo
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res.data
  }

  async function register(data) {
    const res = await api.post('/auth/register', { ...data, agreeTerms: true })
    return res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, unreadCount, isLoggedIn, isAdmin, login, register, logout }
})
