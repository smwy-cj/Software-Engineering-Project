<template>
  <main class="business-page detail-page treehole-detail-page glass-page">
    <section class="detail-card treehole-detail-card glass-surface" v-if="post">
      <div class="post-header">
        <div class="feed-header">
          <div class="feed-avatar treehole-avatar">{{ avatarInitial(post) }}</div>
          <div>
            <span class="feed-name">{{ post.anonymousName || '匿名小友' }}</span>
            <div class="feed-date">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <span class="glass-tag category-tag" :class="`category-${post.category || 'other'}`">{{ categoryLabel(post.category) }}</span>
      </div>
      <p class="detail-content">{{ post.content }}</p>
      <span class="detail-plane" aria-hidden="true"></span>
      <div class="detail-actions">
        <div class="feed-meta">
          <span class="metric-pill">喜欢 {{ post.likeCount }}</span>
          <span class="metric-pill">评论 {{ post.commentCount }}</span>
        </div>
        <button class="glass-button-primary like-button" :disabled="actionLoading" @click="toggleLike">
          <span aria-hidden="true">{{ post.likedByMe ? '♥' : '♡' }}</span>
          {{ post.likedByMe ? '取消点赞' : '点赞' }}
        </button>
      </div>
    </section>

    <section class="detail-card treehole-detail-card treehole-detail-empty glass-surface" v-else>
      <span class="detail-plane" aria-hidden="true"></span>
      <div>
        <span class="glass-tag category-tag category-study">树洞详情</span>
        <h1>暂时无法查看这条心事</h1>
        <p>{{ detailError || '可能需要登录后查看，或这条树洞正在等待审核。' }}</p>
      </div>
    </section>

    <section class="comments-panel treehole-comments-panel glass-surface">
      <div class="section-heading">
        <div>
          <h2>评论 {{ comments.length }}</h2>
          <p>把回应写得轻一点，也认真一点。</p>
        </div>
      </div>

      <div v-if="authStore.isLoggedIn" class="comment-composer treehole-comment-composer glass-mini-card">
        <textarea class="glass-input" v-model="commentContent" placeholder="写评论..." maxlength="100"></textarea>
        <button class="glass-button-primary" @click="submitComment" :disabled="actionLoading || !commentContent">发表评论</button>
      </div>

      <div v-if="comments.length === 0" class="empty-state empty-treehole">还没有回应，给这条心情留下一句温柔的话吧。</div>
      <div v-else class="comment-list">
        <article v-for="c in comments" :key="c.commentId" class="comment-item glass-mini-card">
          <div class="post-header">
            <div class="comment-author">
              <span class="comment-dot" aria-hidden="true"></span>
              <span class="feed-name">{{ c.anonymousName }}</span>
            </div>
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
import api, { unwrapData, unwrapPage } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'

const route = useRoute()
const authStore = useAuthStore()
const post = ref(null)
const comments = ref([])
const commentContent = ref('')
const detailError = ref('')
const toast = useToast()
const { run: runDetail } = useAsyncState('详情加载失败')
const { run: runComments } = useAsyncState('评论加载失败')
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
const categoryItems = [
  { value: 'emotion', label: '情感' },
  { value: 'study', label: '学习' },
  { value: 'life', label: '生活' },
  { value: 'fun', label: '趣味' },
  { value: 'other', label: '其他' }
]

async function loadPost() {
  await runDetail(async () => {
    const res = await api.get(`/treehole/posts/${route.params.id}`)
    post.value = unwrapData(res)
    detailError.value = ''
  }, { throwError: true }).catch(e => {
    post.value = null
    detailError.value = e.response?.status === 403 ? '这条树洞需要登录后查看完整内容。' : '详情加载失败，请稍后再试。'
  })
}

async function loadComments() {
  await runComments(async () => {
    const res = await api.get(`/treehole/posts/${route.params.id}/comments`)
    comments.value = unwrapPage(res).content
  })
}

async function toggleLike() {
  const result = await runAction(async () => {
    const res = await api.post(`/treehole/posts/${route.params.id}/like`)
    return unwrapData(res)
  }, { fallback: '点赞失败' })
  if (!result) return toast.error(authStore.isLoggedIn ? '点赞失败' : '请先登录')
  if (post.value) {
    post.value.likedByMe = result.liked
    post.value.likeCount = result.likeCount
  }
}

async function submitComment() {
  const ok = await runAction(async () => {
    await api.post(`/treehole/posts/${route.params.id}/comments`, { content: commentContent.value })
    return true
  }, { fallback: '评论失败' })
  if (!ok) return toast.error('评论失败')
  commentContent.value = ''
  await loadComments()
  toast.success('评论已发布')
}

onMounted(() => { loadPost(); loadComments() })
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function categoryLabel(value) {
  return categoryItems.find(item => item.value === value)?.label || '其他'
}
function avatarInitial(item) {
  return (item?.anonymousName || '匿').slice(-1)
}
</script>
