import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from './index'
import { useAuthStore } from '../store/auth'

function createToken(payload) {
  const encode = value => btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.`
}

function loginAs(role) {
  const token = createToken({ role, exp: Math.floor(Date.now() / 1000) + 3600 })
  const user = { userId: 1, username: 'tester', role }
  const authStore = useAuthStore()

  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
  authStore.token = token
  authStore.user = user
}

describe('router guards', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    await router.push('/')
    await router.isReady()
  })

  it('redirects guests away from protected pages', async () => {
    await router.push('/profile')

    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/profile')
  })

  it('blocks non-admin users from admin page', async () => {
    loginAs('USER')

    await router.push('/admin')

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('allows admin users into admin page', async () => {
    loginAs('ADMIN')

    await router.push('/admin')

    expect(router.currentRoute.value.path).toBe('/admin')
  })
})
