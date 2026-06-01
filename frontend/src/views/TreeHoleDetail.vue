<template>
  <main class="business-page detail-page glass-page">
    <section class="detail-card glass-surface" v-if="post">
      <div class="post-header">
        <div class="feed-header">
          <div class="feed-avatar"></div>
          <div>
            <span class="feed-name">{{ post.anonymousName || '匿名小友' }}</span>
            <div class="feed-date">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <span class="glass-tag">{{ post.category }}</span>
      </div>
      <p class="detail-content">{{ post.content }}</p>
      <div class="detail-actions">
        <div class="feed-meta">
          <span class="metric-pill">喜欢 {{ post.likeCount }}</span>
          <span class="metric-pill">评论 {{ post.commentCount }}</span>
        </div>
        <button class="glass-button-primary" @click="toggleLike">{{ post.likedByMe ? '取消点赞' : '点赞' }}</button>
      </div>
    </section>

    <section class="comments-panel glass-surface">
      <div class="section-heading">
        <div>
          <h2>评论 {{ comments.length }}</h2>
          <p>把回应写得轻一点，也认真一点。</p>
        </div>
      </div>

      <div v-if="authStore.isLoggedIn" class="comment-composer glass-mini-card">
        <textarea class="glass-input" v-model="commentContent" placeholder="写评论..." maxlength="100"></textarea>
        <button class="glass-button-primary" @click="submitComment" :disabled="!commentContent">发表评论</button>
      </div>

      <div v-if="comments.length === 0" class="empty-state empty-treehole">还没有回应，给这条心情留下一句温柔的话吧。</div>
      <div v-else class="comment-list">
        <article v-for="c in comments" :key="c.commentId" class="comment-item glass-mini-card">
          <div class="post-header">
            <span class="feed-name">{{ c.anonymousName }}</span>
            <span class="post-time">{{ formatTime(c.createdAt) }}</span>
          </div>
          <p>{{ c.content }}</p>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import api from '../api'

const route = useRoute()
const authStore = useAuthStore()
const post = ref(null)
const comments = ref([])
const commentContent = ref('')

async function loadPost() {
  try {
    const res = await api.get(`/treehole/posts/${route.params.id}`)
    post.value = res.data.data
  } catch (e) { /* fail */ }
}

async function loadComments() {
  try {
    const res = await api.get(`/treehole/posts/${route.params.id}/comments`)
    comments.value = res.data.data.content || []
  } catch (e) { comments.value = [] }
}

async function toggleLike() {
  try {
    const res = await api.post(`/treehole/posts/${route.params.id}/like`)
    if (post.value) {
      post.value.likedByMe = res.data.data.liked
      post.value.likeCount = res.data.data.likeCount
    }
  } catch (e) { alert('请先登录') }
}

async function submitComment() {
  try {
    await api.post(`/treehole/posts/${route.params.id}/comments`, { content: commentContent.value })
    commentContent.value = ''
    loadComments()
  } catch (e) { alert(e.response?.data?.message || '评论失败') }
}

onMounted(() => { loadPost(); loadComments() })
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
