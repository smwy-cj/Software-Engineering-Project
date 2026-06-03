<template>
  <div id="app" :class="authStore.isLoggedIn ? 'premium-app-shell' : 'auth-shell'">
    <a v-if="authStore.isLoggedIn" class="skip-link" href="#main-content">跳到主要内容</a>
    <template v-if="authStore.isLoggedIn">
      <aside class="app-rail" aria-label="主导航">
        <div class="rail-brand" @click="$router.push('/')">
          <img class="rail-brand-logo" :src="logoQingyu" alt="青隅 CampusHub" />
        </div>
        <nav class="nav-links">
          <router-link class="nav-pill" active-class="active" to="/">
            <span class="nav-symbol nav-symbol-home" aria-hidden="true"></span>
            首页
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/treehole">
            <span class="nav-symbol nav-symbol-tree" aria-hidden="true"></span>
            树洞
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/partner">
            <span class="nav-symbol nav-symbol-partner" aria-hidden="true"></span>
            找搭子
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/love">
            <span class="nav-symbol nav-symbol-love" aria-hidden="true"></span>
            恋爱交友
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/notifications">
            <span class="nav-symbol nav-symbol-bell" aria-hidden="true"></span>
            通知
            <span class="badge" v-if="authStore.unreadCount">{{ authStore.unreadCount }}</span>
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/profile">
            <span class="nav-symbol nav-symbol-user" aria-hidden="true"></span>
            我的
          </router-link>
          <router-link class="nav-pill" active-class="active" to="/admin" v-if="authStore.isAdmin">
            <span class="nav-symbol nav-symbol-admin" aria-hidden="true"></span>
            管理后台
          </router-link>
        </nav>
        <div class="rail-campus-card" aria-hidden="true">
          <span class="sticker-paper-plane"></span>
          <strong>今天也要加油鸭</strong>
          <span class="rail-campus-date">{{ campusCardDate }}</span>
          <p>在青隅，遇见温暖的回应</p>
        </div>
        <button class="rail-logout" @click="logout">退出登录</button>
      </aside>
      <section class="app-workspace">
        <header class="topbar">
          <div class="topbar-mantra" aria-hidden="true">
            <span class="mantra-mark"></span>
            <span>把校园里的微光，整理成可以靠近的回应</span>
          </div>
          <div class="topbar-actions">
            <button class="topbar-icon-button theme-toggle" type="button" aria-label="主题切换">
              <span class="theme-sun" aria-hidden="true"></span>
            </button>
            <router-link class="topbar-icon-button" to="/notifications" aria-label="通知">
              <span class="nav-symbol nav-symbol-bell" aria-hidden="true"></span>
              <span class="badge" v-if="authStore.unreadCount">{{ authStore.unreadCount }}</span>
            </router-link>
            <router-link class="topbar-user" to="/profile">
              <span class="topbar-avatar">{{ authStore.user?.username?.slice(0, 1) || '青' }}</span>
              <span class="topbar-user-meta">
                <strong>{{ authStore.user?.username || '同学' }}</strong>
                <small>Lv.3</small>
              </span>
            </router-link>
          </div>
        </header>
        <main id="main-content" class="main-content">
          <router-view />
        </main>
      </section>
    </template>
    <main v-else class="auth-content">
      <router-view />
    </main>
    <ToastHost />
  </div>
</template>

<script setup>
import { useAuthStore } from './store/auth'
import { useRouter } from 'vue-router'
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import ToastHost from './components/ToastHost.vue'
import logoQingyu from './assets/home/logo-qingyu.png'

const authStore = useAuthStore()
const router = useRouter()
let unreadTimer = null
const campusCardDate = computed(() => {
  const today = new Date()
  const weekday = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][today.getDay()]

  return `${today.getMonth() + 1}月${today.getDate()}日 ${weekday}`
})

function startUnreadPolling() {
  if (!authStore.isLoggedIn) return
  authStore.refreshUnreadCount()
  if (unreadTimer) window.clearInterval(unreadTimer)
  unreadTimer = window.setInterval(() => {
    authStore.refreshUnreadCount()
  }, 15000)
}

function stopUnreadPolling() {
  if (unreadTimer) {
    window.clearInterval(unreadTimer)
    unreadTimer = null
  }
}

function logout() {
  stopUnreadPolling()
  authStore.logout()
  router.push('/login')
}

onMounted(startUnreadPolling)
onBeforeUnmount(stopUnreadPolling)
watch(() => authStore.isLoggedIn, loggedIn => {
  if (loggedIn) startUnreadPolling()
  else stopUnreadPolling()
})
</script>
