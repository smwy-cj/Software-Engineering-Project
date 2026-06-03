<template>
  <main
    class="business-page treehole-page glass-page"
    :style="{
      '--treehole-page-bg-image': `url(${treeholePageBg})`,
      '--treehole-hero-card-image': `url(${treeholeHeroCard})`
    }"
  >
    <section class="treehole-hero glass-surface" aria-label="把心事放进青隅">
      <aside class="treehole-hero-stat glass-mini-card">
        <span class="glass-tag">今日新增树洞</span>
        <strong>{{ posts.length }}</strong>
        <p>条湿漉漉的心事</p>
      </aside>
    </section>

    <section class="treehole-layout">
      <div class="treehole-main">
        <section class="composer-panel treehole-composer glass-surface">
          <div class="composer-head">
            <div class="composer-title-row">
              <span class="composer-illustration" aria-hidden="true"></span>
              <div>
              <h2>发布新动态</h2>
              <p>默认匿名发布，保留一点安全感。</p>
              </div>
            </div>
            <button v-if="authStore.isLoggedIn && !showCreate" class="glass-button-primary compose-launch" @click="showCreate = true">
              <span class="button-pencil" aria-hidden="true"></span>
              写下心事
            </button>
          </div>

          <div v-if="showCreate" class="composer-body">
            <div class="composer-tools">
              <select class="glass-input compact-input" v-model="newPost.category">
                <option value="other">其他</option>
                <option value="emotion">情感</option>
                <option value="study">学习</option>
                <option value="life">生活</option>
                <option value="fun">趣味</option>
              </select>
              <label class="glass-check">
                <input type="checkbox" v-model="newPost.anonymous" />
                <span>匿名发布</span>
              </label>
            </div>
            <textarea class="glass-input composer-textarea" v-model="newPost.content" placeholder="说点什么吧..." maxlength="500"></textarea>
            <div class="composer-actions">
              <span class="helper-text">{{ newPost.content.length }}/500 字，适合写下此刻真实的心情。</span>
              <div>
                <button class="glass-button-primary" @click="createPost" :disabled="postSaving || !newPost.content">发布</button>
                <button class="glass-button-secondary" @click="showCreate = false">取消</button>
              </div>
            </div>
          </div>
        </section>

        <section class="filter-bar glass-surface">
          <select class="glass-input" v-model="category" @change="loadPosts">
            <option value="">全部分类</option>
            <option value="emotion">情感</option>
            <option value="study">学习</option>
            <option value="life">生活</option>
            <option value="fun">趣味</option>
            <option value="other">其他</option>
          </select>
          <input class="glass-input" v-model="keyword" placeholder="搜索树洞内容..." @keyup.enter="loadPosts" />
          <select class="glass-input" v-model="sortBy" @change="loadPosts">
            <option value="publishTime">最新发布</option>
            <option value="hot">最热</option>
          </select>
        </section>

        <section class="feed-list">
          <p v-if="listError" class="form-error">{{ listError }}</p>
          <div v-if="listLoading" class="empty-state empty-treehole glass-surface">正在寻找树洞里的新心情...</div>
          <div v-else-if="posts.length === 0" class="empty-state empty-treehole glass-surface">这里还很安静，写下第一条校园心情吧。</div>
          <article v-for="post in posts" :key="post.postId" class="feed-item treehole-post glass-mini-card" @click="$router.push(`/treehole/${post.postId}`)">
            <div class="feed-header">
              <div class="feed-avatar treehole-avatar">{{ avatarInitial(post) }}</div>
              <div>
                <div class="feed-name">{{ post.anonymousName || '匿名小友' }}</div>
                <div class="feed-date">{{ formatTime(post.createdAt) }}</div>
              </div>
              <button class="post-more-button" type="button" aria-label="更多操作" @click.stop>...</button>
            </div>
            <p class="feed-content">{{ post.content }}</p>
            <div class="feed-meta">
              <span class="metric-pill">喜欢 {{ post.likeCount }}</span>
              <span class="metric-pill">评论 {{ post.commentCount }}</span>
              <span class="glass-tag category-tag" :class="`category-${post.category || 'other'}`">{{ categoryLabel(post.category) }}</span>
            </div>
          </article>

          <div class="pagination glass-surface" v-if="totalPages > 1">
            <button class="glass-button-secondary" :disabled="page <= 1 || listLoading" @click="setPostPage(page - 1)">上一页</button>
            <button class="glass-button-primary">{{ page }}</button>
            <button class="glass-button-secondary" :disabled="page >= totalPages || listLoading" @click="setPostPage(page + 1)">下一页</button>
          </div>
        </section>
      </div>

      <aside class="business-aside">
        <div class="glass-mini-card insight-card tag-panel">
          <div class="aside-card-head">
            <span class="aside-icon fire-icon" aria-hidden="true"></span>
            <strong>热门标签</strong>
          </div>
          <div class="tag-cloud">
            <span v-for="item in tagItems" :key="item.value" class="glass-tag category-tag" :class="`category-${item.value}`">{{ item.label }}</span>
            <span class="glass-tag category-tag category-other">成长</span>
          </div>
        </div>
        <div class="glass-mini-card insight-card gentle-card">
          <div class="aside-card-head">
            <span class="aside-icon butterfly-icon" aria-hidden="true"></span>
            <strong>温柔提示</strong>
          </div>
          <p>如果今天很累，可以先写一句话。被理解常常从一句话开始。</p>
        </div>
        <div class="glass-mini-card insight-card mood-card">
          <div class="aside-card-head">
            <span class="aside-icon umbrella-icon" aria-hidden="true"></span>
            <strong>今日心情晴雨表</strong>
          </div>
          <p>大家的情绪怎么样呢？</p>
          <div class="mood-rain">
            <span>🙂<small>12</small></span>
            <span>😌<small>8</small></span>
            <span>😐<small>5</small></span>
            <span>😢<small>1</small></span>
          </div>
        </div>
        <div class="glass-mini-card insight-card glow-card">
          <div class="aside-card-head">
            <span class="aside-icon star-icon" aria-hidden="true"></span>
            <strong>校园微光榜</strong>
          </div>
          <ol class="glow-list">
            <li v-for="(post, index) in topPosts" :key="post.postId">
              <span>{{ index + 1 }}</span>
              <strong>{{ post.anonymousName || '匿名小友' }}</strong>
              <em>{{ post.likeCount || 0 }}</em>
            </li>
          </ol>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import treeholePageBg from '../assets/treehole/treehole-page-bg.png'
import treeholeHeroCard from '../assets/treehole/treehole-hero-card.png'

const authStore = useAuthStore()
const category = ref('')
const keyword = ref('')
const sortBy = ref('publishTime')
const showCreate = ref(false)
const newPost = ref({ content: '', category: 'other', anonymous: true })
const toast = useToast()
const { loading: postSaving, run: runPostSave } = useAsyncState('发布失败')
const {
  items: posts,
  page,
  totalPages,
  loading: listLoading,
  error: listError,
  load: loadPostPage,
  reset: resetPosts,
  setPage: setPage
} = usePagination(params => api.get('/treehole/posts', {
  params: {
    ...params,
    category: category.value || undefined,
    keyword: keyword.value || undefined,
    sortBy: sortBy.value
  }
}), { errorMessage: '树洞列表加载失败' })
const tagItems = [
  { value: 'emotion', label: '情感' },
  { value: 'study', label: '学习' },
  { value: 'life', label: '生活' },
  { value: 'fun', label: '趣味' },
  { value: 'other', label: '树洞' }
]

const treeholeBgUrl = `url(${treeholePageBg})`
const topPosts = computed(() => [...posts.value].sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0)).slice(0, 3))

async function loadPosts() {
  await resetPosts()
}

async function setPostPage(nextPage) {
  await setPage(nextPage)
}

async function createPost() {
  const ok = await runPostSave(async () => {
    await api.post('/treehole/posts', newPost.value)
    return true
  })
  if (!ok) return toast.error('发布失败')
  showCreate.value = false
  newPost.value = { content: '', category: 'other', anonymous: true }
  await loadPosts()
  toast.success('树洞发布成功')
}

onMounted(() => {
  document.body.classList.add('treehole-route-bg')
  document.body.style.setProperty('--treehole-route-bg-image', treeholeBgUrl)
  loadPostPage()
})

onBeforeUnmount(() => {
  document.body.classList.remove('treehole-route-bg')
  document.body.style.removeProperty('--treehole-route-bg-image')
})
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function categoryLabel(value) {
  return tagItems.find(item => item.value === value)?.label || '其他'
}
function avatarInitial(post) {
  return (post.anonymousName || '匿').slice(-1)
}
</script>
