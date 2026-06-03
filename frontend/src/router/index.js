import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { guestOnly: true } },
  { path: '/treehole', component: () => import('../views/TreeHole.vue') },
  { path: '/treehole/:id', component: () => import('../views/TreeHoleDetail.vue') },
  { path: '/partner', component: () => import('../views/Partner.vue') },
  { path: '/partner/create', component: () => import('../views/PartnerCreate.vue'), meta: { requiresAuth: true } },
  { path: '/partner/match/:id', component: () => import('../views/PartnerMatch.vue'), meta: { requiresAuth: true } },
  { path: '/love', component: () => import('../views/Love.vue') },
  { path: '/love/matches', component: () => import('../views/LoveMatches.vue'), meta: { requiresAuth: true } },
  { path: '/notifications', component: () => import('../views/Notifications.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(to => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return '/'
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return '/'
  }

  return true
})

export default router
