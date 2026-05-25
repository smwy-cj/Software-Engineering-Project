<template>
  <div id="app">
    <nav class="navbar" v-if="authStore.isLoggedIn">
      <div class="nav-brand" @click="$router.push('/')">青隅 CampusHub</div>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/treehole">树洞</router-link>
        <router-link to="/partner">找搭子</router-link>
        <router-link to="/love">恋爱互助</router-link>
        <router-link to="/notifications">
          通知
          <span class="badge" v-if="authStore.unreadCount">{{ authStore.unreadCount }}</span>
        </router-link>
        <router-link to="/profile">我的</router-link>
        <router-link to="/admin" v-if="authStore.isAdmin">管理</router-link>
      </div>
      <div class="nav-user">
        <span>{{ authStore.user?.username }}</span>
        <button class="btn-sm" @click="logout">退出</button>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from './store/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>
