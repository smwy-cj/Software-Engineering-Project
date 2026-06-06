<template>
  <main class="business-page notification-page glass-page" :style="notificationPageStyle">
    <section class="notification-shell">
      <div class="notification-main-column">
        <section
          class="notification-hero notification-image-hero glass-surface"
          :style="{ backgroundImage: `url(${notificationHeroCard})` }"
          aria-label="每一次互动都在让校园更温暖"
        >
        </section>

        <section class="glass-surface notification-panel">
          <div class="notification-toolbar">
            <div class="notification-tabs">
              <button
                v-for="item in filterTabs"
                :key="item.value"
                class="notice-tab"
                :class="{ active: activeFilter === item.value }"
                @click="activeFilter = item.value"
              >{{ item.label }}</button>
            </div>
            <div class="notification-tools">
              <button class="glass-button-primary notice-read-all" :disabled="actionLoading" @click="markAllRead">
                <span class="notice-check-icon" aria-hidden="true"></span>
                全部已读
              </button>
              <span class="notice-sort-pill">
                <span class="notice-sort-icon" aria-hidden="true"></span>
                最新优先
              </span>
            </div>
          </div>

          <p v-if="listError" class="form-error">{{ listError }}</p>
          <div v-if="listLoading" class="empty-state empty-notification notice-empty">
            <strong>正在加载消息...</strong>
          </div>
          <div v-else-if="filteredNotifs.length === 0" class="empty-state empty-notification notice-empty">
            <span class="notice-empty-envelope" aria-hidden="true"></span>
            <strong>暂时没有新消息</strong>
            <p>校园生活正在路上，新的回应会安静地落在这里。</p>
          </div>
          <div v-else class="notification-list">
            <article
              v-for="n in filteredNotifs"
              :key="n.id"
              class="notification-item glass-mini-card"
              :class="{ read: n.isRead, selected: selectedNotif?.id === n.id }"
              tabindex="0"
              @click="openNotification(n)"
              @keydown.enter="openNotification(n)"
            >
              <span class="notification-unread-marker" aria-hidden="true"></span>
              <div class="notification-avatar" :class="notificationIconClass(n)" aria-hidden="true">
                <span></span>
              </div>
              <div class="notification-body">
                <div class="post-header">
                  <strong>{{ n.title }}</strong>
                  <span class="post-time">{{ formatTime(n.createdAt) }}</span>
                </div>
                <p>{{ notificationContent(n) }}</p>
                <div class="notification-meta-row">
                  <span class="glass-tag notice-type-tag" :class="notificationTagClass(n)">{{ notificationTypeLabel(n) }}</span>
                  <span class="notice-state">{{ n.isRead ? '已读' : '未读' }}</span>
                </div>
              </div>
              <button class="glass-button-secondary notice-view-button" v-if="!n.isRead" :disabled="actionLoading" @click.stop="openNotification(n)">查看详情</button>
              <span class="notice-view-button read-view" v-else>已查看</span>
            </article>
          </div>
        </section>

        <section class="glass-surface notification-detail-card notice-detail-card" v-if="selectedNotif">
          <div class="notification-detail-head">
            <div class="notification-detail-title">
              <span class="glass-tag notice-type-tag" :class="notificationTagClass(selectedNotif)">通知详情</span>
              <h2>{{ selectedNotif.title }}</h2>
              <p>{{ notificationContent(selectedNotif) }}</p>
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
                <button class="glass-button-primary" :disabled="actionLoading" @click="reviewMatch('ACCEPTED')">接受</button>
                <button class="glass-button-secondary" :disabled="actionLoading" @click="reviewMatch('REJECTED')">拒绝</button>
              </template>
            </div>
          </div>
        </section>
      </div>

      <aside class="notification-aside">
        <section class="glass-mini-card notice-side-card overview-card">
          <div class="notice-side-head">
            <span class="notice-side-icon calendar-icon" aria-hidden="true"></span>
            <strong>今日动态速览</strong>
          </div>
          <div class="notice-overview-grid">
            <div>
              <strong>{{ unreadCount }}</strong>
              <small>未读消息</small>
            </div>
            <div>
              <strong>{{ interactionCount }}</strong>
              <small>互动提醒</small>
            </div>
            <div>
              <strong>{{ reviewCount }}</strong>
              <small>审核通知</small>
            </div>
            <div>
              <strong>{{ systemCount }}</strong>
              <small>系统公告</small>
            </div>
          </div>
          <span class="notice-card-clip" aria-hidden="true"></span>
        </section>
        <section class="glass-mini-card notice-side-card chart-card">
          <div class="notice-side-head">
            <span class="notice-side-icon star-badge-icon" aria-hidden="true"></span>
            <strong>消息分类统计</strong>
          </div>
          <div class="notice-chart-row">
            <div class="notice-donut" :style="{ '--read-ratio': readRatio }" aria-hidden="true"></div>
            <div class="notice-chart-legend">
              <span><i class="legend-interaction"></i>互动提醒 {{ interactionCount }}</span>
              <span><i class="legend-review"></i>审核通知 {{ reviewCount }}</span>
              <span><i class="legend-system"></i>系统公告 {{ systemCount }}</span>
              <span><i class="legend-read"></i>已读消息 {{ readCount }}</span>
            </div>
          </div>
          <p>总计 {{ notifs.length }} 条消息</p>
        </section>
        <section class="glass-mini-card notice-side-card warm-tip-card">
          <div class="notice-side-head">
            <span class="notice-side-icon bulb-warm-icon" aria-hidden="true"></span>
            <strong>小隅寄语</strong>
          </div>
          <p>慢下来，认真生活，好事总会在不经意间发生。</p>
          <span class="notice-window-plant" aria-hidden="true"></span>
        </section>
        <section class="notice-corner-image-card" aria-hidden="true">
          <img :src="notificationCornerCard" alt="" />
        </section>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import api, { unwrapData, unwrapPage } from '../api'
import { useAuthStore } from '../store/auth'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'
import notificationHeroCard from '../assets/notification/notification-hero-card.png'
import notificationCornerCard from '../assets/notification/notification-cornor-card.png'
import notificationPageBg from '../assets/notification/notification-page-bg.png'

const authStore = useAuthStore()
const notifs = ref([])
const selectedNotif = ref(null)
const selectedMatch = ref(null)
const activeFilter = ref('all')
const toast = useToast()
const { loading: listLoading, error: listError, run: runList } = useAsyncState('通知加载失败')
const { loading: detailLoading, error: detailError, run: runDetail } = useAsyncState('详情加载失败')
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
const notificationPageStyle = computed(() => ({
  '--notification-hero-card-image': `url(${notificationHeroCard})`,
  '--notification-page-bg-image': `url(${notificationPageBg})`
}))
const unreadCount = computed(() => notifs.value.filter(n => !n.isRead).length)
const readCount = computed(() => notifs.value.filter(n => n.isRead).length)
const interactionCount = computed(() => notifs.value.filter(n => notificationGroup(n) === 'interaction').length)
const reviewCount = computed(() => notifs.value.filter(n => notificationGroup(n) === 'review').length)
const systemCount = computed(() => notifs.value.filter(n => notificationGroup(n) === 'system').length)
const readRatio = computed(() => `${notifs.value.length ? Math.round((readCount.value / notifs.value.length) * 100) : 0}%`)
const filteredNotifs = computed(() => {
  if (activeFilter.value === 'all') return notifs.value
  if (activeFilter.value === 'read') return notifs.value.filter(n => n.isRead)
  return notifs.value.filter(n => notificationGroup(n) === activeFilter.value)
})
const canReview = computed(() =>
  selectedMatch.value?.status === 'PENDING' &&
  selectedMatch.value?.publisher?.userId === authStore.user?.userId
)
const filterTabs = [
  { value: 'all', label: '全部' },
  { value: 'interaction', label: '互动' },
  { value: 'review', label: '审核' },
  { value: 'system', label: '系统' },
  { value: 'read', label: '已读' }
]
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
  await runList(async () => {
    const res = await api.get('/notifications', { params: { size: 50 } })
    const page = unwrapPage(res)
    notifs.value = page.content
    authStore.unreadCount = page.unreadCount
  })
}

async function markAllRead() {
  const ok = await runAction(async () => {
    await api.put('/notifications/read-all')
    return true
  }, { fallback: '全部已读失败' })
  if (!ok) return toast.error('全部已读失败')
  await loadNotifs()
  await authStore.refreshUnreadCount()
  toast.success('已全部标记为已读')
}

async function openNotification(notif) {
  selectedNotif.value = notif
  selectedMatch.value = null
  detailError.value = ''
  if (!notif.isRead) {
    await runAction(async () => {
      await api.put(`/notifications/${notif.id}/read`)
      return true
    }, { fallback: '标记已读失败' })
    notif.isRead = true
    await authStore.refreshUnreadCount()
  }
  if (notif.relatedType !== 'partnerMatch' || !notif.relatedId) return

  await runDetail(async () => {
    const res = await api.get(`/partner/matches/${notif.relatedId}`)
    selectedMatch.value = unwrapData(res)
  }, { fallback: '申请详情加载失败' })
}

async function reviewMatch(status) {
  if (!selectedMatch.value) return
  const ok = await runAction(async () => {
    await api.put(`/partner/matches/${selectedMatch.value.matchId}`, { status })
    return true
  }, { fallback: '处理失败' })
  if (!ok) return toast.error('处理失败')
  await openNotification(selectedNotif.value)
  await loadNotifs()
  toast.success(status === 'ACCEPTED' ? '已接受申请' : '已拒绝申请')
}

function closeDetail() {
  selectedNotif.value = null
  selectedMatch.value = null
  detailError.value = ''
}

onMounted(() => {
  document.body.classList.add('notification-route-theme')
  document.body.style.setProperty('--notification-page-bg-image', `url(${notificationPageBg})`)
  loadNotifs()
})
onBeforeUnmount(() => {
  document.body.classList.remove('notification-route-theme')
  document.body.style.removeProperty('--notification-page-bg-image')
})
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function typeLabel(t) { return typeMap[t] || t || '其他' }
function statusLabel(status) {
  return ({ PENDING: '等待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELED: '已取消', ENDED: '已结束' })[status] || status
}
function notificationGroup(notif) {
  const value = notif?.type || ''
  if (['admin', 'feedback', 'review'].includes(value)) return 'review'
  if (['system', 'notice'].includes(value)) return 'system'
  return 'interaction'
}
function notificationTypeLabel(notif) {
  return ({ interaction: '互动', review: '审核', system: '系统' })[notificationGroup(notif)]
}
function notificationTagClass(notif) {
  return `notice-type-${notificationGroup(notif)}`
}
function notificationIconClass(notif) {
  return `notice-icon-${notificationGroup(notif)}`
}
function notificationContent(notif) {
  if (!notif?.content) return ''
  return notif.content
    .replaceAll('PASSED', '已通过')
    .replaceAll('REJECTED', '未通过')
    .replaceAll('WARNING', '已通过，请注意社区规范')
    .replaceAll('WARNED', '警告')
    .replaceAll('MUTED', '禁言')
    .replaceAll('BANNED', '封禁')
}
</script>
