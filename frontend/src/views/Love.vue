<template>
  <main class="business-page love-page glass-page">
    <section class="business-hero love-hero glass-surface">
      <div>
        <span class="hero-kicker">恋爱交友</span>
        <h1>在校园里认真认识一个人</h1>
        <p>发布清晰的交友期待，浏览同校同学的真实表达。互相尊重边界，用轻量、真诚的方式开始了解。</p>
        <div class="hero-actions">
          <button class="glass-button-secondary" @click="$router.push('/love/matches')">我的匹配</button>
          <button class="glass-button-primary" @click="showCreate = true">发布交友需求</button>
        </div>
      </div>
      <div class="love-orb-card glass-mini-card">
        <span class="glass-tag">交友需求</span>
        <strong>{{ requests.length }}</strong>
        <p>条正在展示</p>
      </div>
    </section>

    <section class="composer-panel love-composer glass-surface" v-if="showCreate">
      <div class="composer-head">
        <div>
          <h2>发布交友需求</h2>
          <p>写清楚你期待的相处方式，系统会在审核后展示。</p>
        </div>
      </div>
      <div class="form-group">
        <label>交友描述</label>
        <textarea
          class="glass-input composer-textarea"
          v-model="loveForm.description"
          placeholder="例如：希望认识愿意一起自习、散步、看展的同校同学..."
          maxlength="200"
        ></textarea>
      </div>
      <div class="form-group">
        <label>有效天数（1-14）</label>
        <input class="glass-input compact-input" v-model.number="loveForm.validDays" type="number" min="1" max="14" />
      </div>
      <div class="composer-actions">
        <span class="helper-text">发布前请先完善个人资料；发布后会进入内容审核。</span>
        <div>
          <button class="glass-button-primary" @click="createLoveReq" :disabled="!loveForm.description">发布</button>
          <button class="glass-button-secondary" @click="showCreate = false">取消</button>
        </div>
      </div>
    </section>

    <section class="filter-bar love-feed-tabs glass-surface">
      <div class="segmented-scroll">
        <button class="nav-pill" :class="{ active: sortBy === 'published' }" @click="switchSort('published')">最新发布</button>
        <button class="nav-pill" :class="{ active: sortBy === 'interaction' }" @click="switchSort('interaction')">最新互动</button>
      </div>
    </section>

    <section class="feed-section love-feed glass-surface">
      <div class="section-heading">
        <div>
          <h2>{{ currentTitle }}</h2>
          <p>{{ currentDescription }}</p>
        </div>
      </div>
      <div v-if="requests.length === 0" class="empty-state empty-love">暂时没有公开的交友需求，发布一条真诚的自我介绍吧。</div>
      <div v-else class="love-profile-grid">
        <article v-for="item in requests" :key="item.requestId" class="love-profile-card glass-mini-card">
          <div class="feed-header">
            <div class="feed-avatar"></div>
            <div>
              <div class="feed-name">{{ item.publisherInfo?.nickname || '同学' }}</div>
              <div class="feed-date">{{ personLine(item) }}</div>
            </div>
          </div>
          <p class="feed-content">{{ item.description }}</p>
          <div class="partner-facts">
            <span>发布时间 {{ formatTime(item.createdAt) }}</span>
            <span>有效期 {{ daysLeft(item.expireAt) }} 天</span>
            <span>{{ scopeLabel(item.scope) }}</span>
          </div>
          <div class="feed-meta">
            <span class="glass-tag">{{ statusLabel(item.status) }}</span>
            <button
              v-if="authStore.isLoggedIn && !isOwner(item)"
              class="glass-button-secondary"
              @click="sendHeart(item.requestId)"
            >发送心动</button>
            <span v-else-if="isOwner(item)" class="glass-tag">我发布的</span>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const requests = ref([])
const sortBy = ref('published')
const showCreate = ref(false)
const loveForm = ref({ description: '', validDays: 7, scope: 'sameSchool' })
let refreshTimer = null

const currentTitle = computed(() => sortBy.value === 'interaction' ? '最新互动' : '最新发布')
const currentDescription = computed(() =>
  sortBy.value === 'interaction'
    ? '优先展示最近收到心动回应的交友需求。'
    : '按发布时间查看同校同学的交友需求。'
)

async function loadRequests() {
  try {
    const res = await api.get('/love/requests', {
      params: { sortBy: sortBy.value, size: 20 }
    })
    requests.value = res.data.data.content || []
  } catch (e) {
    requests.value = []
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(loadRequests, 8000)
  window.addEventListener('focus', loadRequests)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
  window.removeEventListener('focus', loadRequests)
}

function switchSort(nextSort) {
  sortBy.value = nextSort
  loadRequests()
}

async function createLoveReq() {
  try {
    await api.post('/love/requests', loveForm.value)
    showCreate.value = false
    loveForm.value = { description: '', validDays: 7, scope: 'sameSchool' }
    await loadRequests()
    alert('交友需求已提交审核')
  } catch (e) {
    alert(e.response?.data?.message || '发布失败')
  }
}

async function sendHeart(requestId) {
  try {
    await api.post(`/love/requests/${requestId}/heart`)
    alert('心动已发送')
    if (sortBy.value === 'interaction') await loadRequests()
  } catch (e) {
    alert(e.response?.data?.message || '发送失败')
  }
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN') : ''
}

function daysLeft(t) {
  if (!t) return 0
  return Math.max(0, Math.ceil((new Date(t) - new Date()) / 86400000))
}

function scopeLabel(scope) {
  return scope === 'all' ? '不限学校' : '同校可见'
}

function statusLabel(status) {
  return status === 'PUBLISHED' ? '展示中' : '审核中'
}

function isOwner(item) {
  return item.publisherInfo?.userId === authStore.user?.userId
}

function personLine(item) {
  const info = item.publisherInfo || {}
  const bits = []
  if (info.age) bits.push(`${info.age}岁`)
  if (info.major) bits.push(info.major)
  if (info.university) bits.push(info.university)
  return bits.join(' · ') || '校园同学'
}

onMounted(() => {
  loadRequests()
  startAutoRefresh()
})
onUnmounted(stopAutoRefresh)
</script>
