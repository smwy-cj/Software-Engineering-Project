<template>
  <div>
    <h1 class="page-title">青隅 CampusHub</h1>
    <p style="color:#666;margin-bottom:24px;">校园互助服务平台 — 树洞倾诉 · 找搭子 · 恋爱交友</p>

    <div class="card" v-if="!authStore.isLoggedIn">
      <h3>欢迎来到青隅</h3>
      <p style="margin:12px 0;color:#888;">登录后即可使用全部功能</p>
      <router-link to="/login" class="btn">登录</router-link>
      <router-link to="/register" class="btn" style="background:#fff;color:#4a90d9;border:1px solid #4a90d9;margin-left:12px;">注册</router-link>
    </div>

    <div class="card" v-if="authStore.isLoggedIn">
      <h3>欢迎回来，{{ authStore.user?.username }}</h3>
      <div style="display:flex;gap:16px;margin-top:16px;flex-wrap:wrap;">
        <router-link to="/treehole" class="btn" style="background:#8e44ad;">进入树洞</router-link>
        <router-link to="/partner" class="btn" style="background:#2980b9;">找搭子</router-link>
        <router-link to="/love" class="btn" style="background:#e74c3c;">恋爱互助</router-link>
      </div>
    </div>

    <div class="card">
      <h3>最新树洞动态</h3>
      <div v-if="posts.length === 0" class="empty-state">暂无动态</div>
      <div v-for="post in posts" :key="post.postId" class="post-item" @click="$router.push(`/treehole/${post.postId}`)">
        <div class="post-header">
          <span class="post-user">{{ post.anonymousName }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-footer">
          <span>❤ {{ post.likeCount }}</span>
          <span>💬 {{ post.commentCount }}</span>
          <span class="tag">{{ post.category }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const posts = ref([])

onMounted(async () => {
  try {
    const res = await api.get('/treehole/posts', { params: { size: 5 } })
    posts.value = res.data.data.content || []
  } catch (e) { /* silently fail for unauthenticated */ }
})

function formatTime(t) {
  return t ? new Date(t).toLocaleDateString('zh-CN') : ''
}
</script>
