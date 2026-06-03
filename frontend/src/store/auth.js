import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api, { unwrapData } from '../api'
import { getRoleFromToken, isTokenExpired } from '../utils/jwt'
import { readJson, writeJson } from '../utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(readJson('user', null))
  const unreadCount = ref(0)
  const role = computed(() => user.value?.role || getRoleFromToken(token.value))

  const isLoggedIn = computed(() => !!token.value && !isTokenExpired(token.value))
  const isAdmin = computed(() => role.value === 'ADMIN')

  function persistSession(nextToken, nextUser) {
    token.value = nextToken || ''
    user.value = nextUser || null
    if (token.value) localStorage.setItem('token', token.value)
    else localStorage.removeItem('token')
    writeJson('user', user.value)
  }

  async function login(phone, password, captcha = { captcha: 'mock', captchaId: 'mock' }) {
    const res = await api.post('/auth/login', {
      phone,
      password,
      loginType: 'password',
      captcha: captcha.captcha,
      captchaId: captcha.captchaId
    })
    const data = unwrapData(res)
    const accessToken = data.accessToken
    const tokenRole = getRoleFromToken(accessToken)
    persistSession(accessToken, { ...data.userInfo, role: tokenRole })
    await refreshUnreadCount()
    return res.data
  }

  async function register(data) {
    const res = await api.post('/auth/register', { ...data, agreeTerms: true })
    return res.data
  }

  async function getCaptcha() {
    const res = await api.get('/auth/captcha')
    return unwrapData(res)
  }

  async function refreshUnreadCount() {
    if (!token.value) {
      unreadCount.value = 0
      return 0
    }
    try {
      const res = await api.get('/notifications', { params: { page: 1, size: 1 } })
      unreadCount.value = unwrapData(res)?.unreadCount || 0
    } catch (e) {
      unreadCount.value = 0
    }
    return unreadCount.value
  }

  function logout() {
    unreadCount.value = 0
    persistSession('', null)
  }

  function updateUserInfo(partial) {
    user.value = { ...(user.value || {}), ...partial }
    writeJson('user', user.value)
  }

  if (token.value && isTokenExpired(token.value)) {
    logout()
  } else if (token.value && user.value && !user.value.role) {
    updateUserInfo({ role: getRoleFromToken(token.value) })
  }

  return { token, user, role, unreadCount, isLoggedIn, isAdmin, login, register, getCaptcha, refreshUnreadCount, logout, updateUserInfo }
})
