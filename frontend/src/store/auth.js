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
    await refreshUnreadCount()
    return res.data
  }

  async function register(data) {
    const res = await api.post('/auth/register', { ...data, agreeTerms: true })
    return res.data
  }

  async function getCaptcha() {
    const res = await api.get('/auth/captcha')
    return res.data.data
  }

  async function refreshUnreadCount() {
    if (!token.value) {
      unreadCount.value = 0
      return 0
    }
    try {
      const res = await api.get('/notifications', { params: { page: 1, size: 1 } })
      unreadCount.value = res.data.data.unreadCount || 0
    } catch (e) {
      unreadCount.value = 0
    }
    return unreadCount.value
  }

  function logout() {
    token.value = ''
    user.value = null
    unreadCount.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function updateUserInfo(partial) {
    user.value = { ...(user.value || {}), ...partial }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { token, user, unreadCount, isLoggedIn, isAdmin, login, register, getCaptcha, refreshUnreadCount, logout, updateUserInfo }
})
