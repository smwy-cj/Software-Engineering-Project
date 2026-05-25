import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/treehole', component: () => import('../views/TreeHole.vue') },
  { path: '/treehole/:id', component: () => import('../views/TreeHoleDetail.vue') },
  { path: '/partner', component: () => import('../views/Partner.vue') },
  { path: '/partner/create', component: () => import('../views/PartnerCreate.vue') },
  { path: '/partner/match/:id', component: () => import('../views/PartnerMatch.vue') },
  { path: '/love', component: () => import('../views/Love.vue') },
  { path: '/love/matches', component: () => import('../views/LoveMatches.vue') },
  { path: '/notifications', component: () => import('../views/Notifications.vue') },
  { path: '/profile', component: () => import('../views/Profile.vue') },
  { path: '/admin', component: () => import('../views/Admin.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
