<template>
  <main class="business-page notification-page glass-page">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">消息通知</span>
        <h1>新的回应都在这里</h1>
        <p>集中查看互动、审核和系统提醒，不错过校园里的每一次反馈。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">未读提醒</span>
        <strong>{{ unreadCount }}</strong>
        <p>条消息等待处理</p>
      </aside>
    </section>

    <section class="glass-surface notification-panel">
      <div class="section-heading">
        <div>
          <h2>通知列表</h2>
          <p>已读消息会自动降低视觉权重，方便先处理重要内容。</p>
        </div>
        <button class="glass-button-secondary" @click="markAllRead">全部已读</button>
      </div>

      <div v-if="notifs.length === 0" class="empty-state empty-notification">暂时没有新消息，校园生活正在路上。</div>
      <div v-else class="notification-list">
        <article
          v-for="n in notifs"
          :key="n.id"
          class="notification-item glass-mini-card"
          :class="{ read: n.isRead, selected: selectedNotif?.id === n.id }"
          tabindex="0"
          @click="openNotification(n)"
          @keydown.enter="openNotification(n)"
        >
          <div class="notification-dot" aria-hidden="true"></div>
          <div class="notification-body">
            <div class="post-header">
              <strong>{{ n.title }}</strong>
              <span class="post-time">{{ formatTime(n.createdAt) }}</span>
            </div>
            <p>{{ n.content }}</p>
          </div>
          <button class="glass-button-secondary" v-if="!n.isRead" @click.stop="markRead(n.id)">标记已读</button>
        </article>
      </div>
    </section>

    <section class="glass-surface notification-detail-card" v-if="selectedNotif">
      <div class="notification-detail-head">
        <div class="notification-detail-title">
          <span class="glass-tag">通知详情</span>
          <h2>{{ selectedNotif.title }}</h2>
          <p>{{ selectedNotif.content }}</p>
        </div>
        <div class="notification-detail-tools">
          <span class="glass-tag">{{ formatTime(selectedNotif.createdAt) }}</span>
          <button class="detail-close-button" type="button" aria-label="关闭通知详情" @click="closeDetail">×</button>
        </div>
      </div>

      <div v-if="detailLoading" class="empty-state">正在加载申请信息...</div>
      <div v-else-if="detailError" class="empty-state">{{ detailError }}</div>
      <div v-else-if="selectedMatch" class="partner-apply-detail">
        <div class="apply-detail-grid">
          <article class="glass-mini-card apply-detail-block">
            <span class="glass-tag">申请人</span>
            <strong>{{ selectedMatch.applicant?.nickname || '同学' }}</strong>
            <p>{{ selectedMatch.applicant?.grade || '年级未填' }} · {{ selectedMatch.applicant?.major || '专业未填' }}</p>
          </article>
          <article class="glass-mini-card apply-detail-block">
            <span class="glass-tag">搭子需求</span>
            <strong>{{ typeLabel(selectedMatch.request?.type) }}</strong>
            <p>{{ selectedMatch.request?.description }}</p>
          </article>
        </div>
        <div class="apply-message glass-mini-card">
          <span class="glass-tag">申请附言</span>
          <p>{{ selectedMatch.applyMessage || '对方没有填写附言' }}</p>
        </div>
        <div class="detail-actions">
          <span class="glass-tag">{{ statusLabel(selectedMatch.status) }}</span>
          <template v-if="canReview">
            <button class="glass-button-primary" @click="reviewMatch('ACCEPTED')">接受</button>
            <button class="glass-button-secondary" @click="reviewMatch('REJECTED')">拒绝</button>
          </template>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const authStore = useAuthStore()
const notifs = ref([])
const selectedNotif = ref(null)
const selectedMatch = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
const unreadCount = computed(() => notifs.value.filter(n => !n.isRead).length)
const canReview = computed(() =>
  selectedMatch.value?.status === 'PENDING' &&
  selectedMatch.value?.publisher?.userId === authStore.user?.userId
)
const typeMap = {
  study: '学习',
  sport: '运动',
  meal: '吃饭',
  exam: '考试',
  travel: '出行',
  game: '游戏',
  other: '其他'
}

async function loadNotifs() {
  try {
    const res = await api.get('/notifications', { params: { size: 50 } })
    notifs.value = res.data.data.content || []
    authStore.unreadCount = res.data.data.unreadCount || 0
  } catch (e) { notifs.value = [] }
}

async function markRead(id) {
  await api.put(`/notifications/${id}/read`)
  await loadNotifs()
  await authStore.refreshUnreadCount()
}

async function markAllRead() {
  await api.put('/notifications/read-all')
  await loadNotifs()
  await authStore.refreshUnreadCount()
}

async function openNotification(notif) {
  selectedNotif.value = notif
  selectedMatch.value = null
  detailError.value = ''
  if (!notif.isRead) {
    await api.put(`/notifications/${notif.id}/read`)
    notif.isRead = true
    await authStore.refreshUnreadCount()
  }
  if (notif.relatedType !== 'partnerMatch' || !notif.relatedId) return

  detailLoading.value = true
  try {
    const res = await api.get(`/partner/matches/${notif.relatedId}`)
    selectedMatch.value = res.data.data
  } catch (e) {
    detailError.value = e.response?.data?.message || '申请详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

async function reviewMatch(status) {
  if (!selectedMatch.value) return
  try {
    await api.put(`/partner/matches/${selectedMatch.value.matchId}`, { status })
    await openNotification(selectedNotif.value)
    await loadNotifs()
  } catch (e) { alert(e.response?.data?.message || '处理失败') }
}

function closeDetail() {
  selectedNotif.value = null
  selectedMatch.value = null
  detailError.value = ''
  detailLoading.value = false
}

onMounted(loadNotifs)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function typeLabel(t) { return typeMap[t] || t || '其他' }
function statusLabel(status) {
  return ({ PENDING: '等待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELED: '已取消', ENDED: '已结束' })[status] || status
}
</script>
