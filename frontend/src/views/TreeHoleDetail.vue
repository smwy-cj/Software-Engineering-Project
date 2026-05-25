<template>
  <div>
    <div class="card" v-if="post">
      <div class="post-header">
        <span class="post-user">{{ post.anonymousName }}</span>
        <span class="post-time">{{ formatTime(post.createdAt) }}</span>
      </div>
      <p style="font-size:16px;line-height:1.8;margin:12px 0;">{{ post.content }}</p>
      <div class="post-footer">
        <span>❤ {{ post.likeCount }}</span>
        <span>💬 {{ post.commentCount }}</span>
        <span class="tag">{{ post.category }}</span>
      </div>
      <div style="margin-top:12px;">
        <button class="btn-sm" @click="toggleLike">{{ post.likedByMe ? '取消点赞' : '点赞' }}</button>
      </div>
    </div>

    <div class="card">
      <h3>评论 ({{ comments.length }})</h3>
      <div v-if="authStore.isLoggedIn" style="margin-bottom:16px;">
        <textarea v-model="commentContent" placeholder="写评论..." maxlength="100" style="width:100%;padding:8px;border:1px solid #ddd;border-radius:6px;min-height:60px;"></textarea>
        <button class="btn-sm" style="margin-top:8px;" @click="submitComment" :disabled="!commentContent">发表评论</button>
      </div>
      <div v-if="comments.length === 0" class="empty-state">暂无评论</div>
      <div v-for="c in comments" :key="c.commentId" style="padding:12px 0;border-bottom:1px solid #f0f0f0;">
        <span style="color:#4a90d9;font-size:13px;">{{ c.anonymousName }}</span>
        <span style="color:#999;font-size:12px;margin-left:8px;">{{ formatTime(c.createdAt) }}</span>
        <p style="margin-top:4px;font-size:14px;">{{ c.content }}</p>
      </div>
    </div>
  </div>
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
