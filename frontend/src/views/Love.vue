<template>
  <main class="business-page love-page glass-page" :style="lovePageStyle">
    <section class="love-shell">
      <div class="love-main-column">
        <section class="love-hero love-image-hero glass-surface" aria-label="在校园里，遇见心动的你">
          <div class="love-hero-actions">
            <div class="hero-actions">
              <button class="glass-button-secondary love-match-button" @click="$router.push('/love/matches')">
                <span class="love-line-icon love-heart-line" aria-hidden="true"></span>
                我的匹配
              </button>
              <button class="glass-button-primary" @click="showCreate = true">
                <span class="love-line-icon love-send-line" aria-hidden="true"></span>
                发布交友需求
              </button>
            </div>
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
              <button class="glass-button-primary" @click="createLoveReq" :disabled="actionLoading || !loveForm.description">发布</button>
              <button class="glass-button-secondary" @click="showCreate = false">取消</button>
            </div>
          </div>
        </section>

        <section class="love-toolbar glass-surface">
          <div class="love-sort-tabs">
            <button class="love-sort-tab" :class="{ active: sortBy === 'published' }" @click="switchSort('published')">最新发布</button>
            <button class="love-sort-tab" :class="{ active: sortBy === 'interaction' }" @click="switchSort('interaction')">最新互动</button>
            <button class="love-sort-tab" :class="{ active: sortBy === 'popular' }" @click="switchSort('popular')">热门推荐</button>
          </div>
          <div class="love-category-scroll">
            <button
              v-for="item in categoryTabs"
              :key="item.value"
              class="love-category-pill"
              :class="{ active: selectedCategory === item.value }"
              @click="selectedCategory = item.value"
            >{{ item.label }}</button>
          </div>
        </section>

        <section class="love-feed-panel glass-surface">
          <div class="section-heading">
            <div>
              <h2>{{ currentTitle }}</h2>
              <p>{{ currentDescription }}</p>
            </div>
            <span class="love-feed-count">{{ visibleRequests.length }} 条</span>
          </div>
          <p v-if="listError" class="form-error">{{ listError }}</p>
          <div v-if="listLoading" class="empty-state empty-love">
            <span class="love-empty-envelope" aria-hidden="true"></span>
            <strong>正在加载交友需求...</strong>
          </div>
          <div v-else-if="visibleRequests.length === 0" class="empty-state empty-love">
            <span class="love-empty-envelope" aria-hidden="true"></span>
            <strong>暂时没有公开的交友需求</strong>
            <p>发布一条真诚的自我介绍，也许会遇见刚好同频的人。</p>
          </div>
          <div v-else class="love-profile-grid">
            <article v-for="item in visibleRequests" :key="item.requestId" class="love-profile-card glass-mini-card">
              <button class="love-card-heart" type="button" aria-label="收藏交友卡片"></button>
              <div class="love-card-head">
                <img v-if="item.publisherInfo?.avatar" :src="item.publisherInfo.avatar" alt="" />
                <span v-else class="love-avatar">{{ publisherInitial(item) }}</span>
                <div>
                  <div class="feed-name">{{ item.publisherInfo?.nickname || '同学' }}</div>
                  <div class="feed-date">{{ personLine(item) }}</div>
                </div>
              </div>
              <p class="feed-content">{{ item.description }}</p>
              <div class="love-interest-tags">
                <span v-for="tag in interestTags(item)" :key="`${item.requestId}-${tag}`">{{ tag }}</span>
              </div>
              <div class="love-card-foot">
                <div class="love-card-meta">
                  <span>发布 {{ formatTime(item.createdAt) }}</span>
                  <span>有效 {{ daysLeft(item.expireAt) }} 天</span>
                  <span>{{ scopeLabel(item.scope) }}</span>
                </div>
                <div class="love-card-actions">
                  <button
                    v-if="authStore.isLoggedIn && !isOwner(item)"
                    class="glass-button-primary love-heart-button"
                    :disabled="actionLoading"
                    @click="sendHeart(item.requestId)"
                  >
                    <span class="love-line-icon love-heart-solid" aria-hidden="true"></span>
                    打个招呼
                  </button>
                  <span v-else-if="isOwner(item)" class="glass-tag">我发布的</span>
                  <span class="glass-tag">{{ statusLabel(item.status) }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>

      <aside class="love-aside">
        <section class="love-side-card love-tags-card glass-mini-card">
          <div class="love-side-head">
            <span class="love-side-icon tag-icon" aria-hidden="true"></span>
            <strong>热门标签</strong>
          </div>
          <div class="love-side-tags">
            <span v-for="tag in sideTags" :key="tag.label">{{ tag.label }} <small>{{ tag.count }}</small></span>
          </div>
        </section>

        <section class="love-side-card love-index-card glass-mini-card">
          <div class="love-side-head">
            <span class="love-side-icon pulse-icon" aria-hidden="true"></span>
            <strong>今日心动指数</strong>
          </div>
          <div class="love-index-number"><strong>{{ heartScore }}</strong><span>/100</span></div>
          <span class="love-pulse-line" aria-hidden="true"></span>
          <p>今天也是充满期待的一天。</p>
        </section>

        <section class="love-side-card love-tip-card glass-mini-card">
          <div class="love-side-head">
            <span class="love-side-icon note-icon" aria-hidden="true"></span>
            <strong>温柔提示</strong>
          </div>
          <p>真诚是最好的名片，尊重彼此，才能源源不断地靠近。</p>
          <span class="love-tip-heart" aria-hidden="true"></span>
        </section>

        <section class="love-side-card love-rank-card glass-mini-card">
          <div class="love-side-head">
            <span class="love-side-icon crown-icon" aria-hidden="true"></span>
            <strong>心动排行榜</strong>
          </div>
          <ol>
            <li v-for="(item, index) in rankedList" :key="`rank-${item.requestId}`">
              <span>{{ index + 1 }}</span>
              <strong>{{ item.publisherInfo?.nickname || '同学' }}</strong>
              <small>{{ 98 - index * 11 }}</small>
            </li>
          </ol>
        </section>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api, { unwrapPage } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'
import loveHeroCard from '../assets/love/love-hero-card.png'
import lovePageBg from '../assets/love/love-page-bg.png'

const authStore = useAuthStore()
const requests = ref([])
const sortBy = ref('published')
const selectedCategory = ref('all')
const showCreate = ref(false)
const loveForm = ref({ description: '', validDays: 7, scope: 'sameSchool' })
const toast = useToast()
const { loading: listLoading, error: listError, run: runList } = useAsyncState('交友需求加载失败')
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
let refreshTimer = null
const lovePageStyle = computed(() => ({
  '--love-hero-card-image': `url(${loveHeroCard})`,
  '--love-page-bg-image': `url(${lovePageBg})`
}))

const categoryTabs = [
  { value: 'all', label: '全部' },
  { value: 'study', label: '学习交流' },
  { value: 'hobby', label: '兴趣爱好' },
  { value: 'activity', label: '一起运动' },
  { value: 'movie', label: '电影音乐' },
  { value: 'travel', label: '旅行探索' },
  { value: 'other', label: '其他' }
]

const sideTags = [
  { label: '一起自习', count: 1284 },
  { label: '羽毛球', count: 986 },
  { label: '看电影', count: 872 },
  { label: '散步', count: 642 },
  { label: '旅行搭子', count: 593 },
  { label: '摄影', count: 467 }
]

const currentTitle = computed(() => {
  if (sortBy.value === 'interaction') return '最新互动'
  if (sortBy.value === 'popular') return '热门推荐'
  return '最新发布'
})
const currentDescription = computed(() =>
  sortBy.value === 'interaction'
    ? '优先展示最近收到心动回应的交友需求。'
    : sortBy.value === 'popular'
      ? '优先展示更容易开启对话的温柔交友卡片。'
    : '按发布时间查看同校同学的交友需求。'
)
const activeCount = computed(() => requests.value.length)
const heartScore = computed(() => Math.min(99, 68 + requests.value.length * 4))
const rankedList = computed(() => requests.value.slice(0, 3))
const visibleRequests = computed(() => {
  if (selectedCategory.value === 'all') return requests.value
  return requests.value.filter(item => inferCategory(item.description) === selectedCategory.value)
})

async function loadRequests() {
  await runList(async () => {
    const res = await api.get('/love/requests', {
      params: { sortBy: sortBy.value === 'popular' ? 'published' : sortBy.value, size: 20 }
    })
    requests.value = unwrapPage(res).content
  }, { preventOverlap: true })
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
  const ok = await runAction(async () => {
    await api.post('/love/requests', loveForm.value)
    return true
  }, { fallback: '发布失败' })
  if (!ok) return toast.error('发布失败')
  showCreate.value = false
  loveForm.value = { description: '', validDays: 7, scope: 'sameSchool' }
  await loadRequests()
  toast.success('交友需求已提交审核')
}

async function sendHeart(requestId) {
  const ok = await runAction(async () => {
    await api.post(`/love/requests/${requestId}/heart`)
    return true
  }, { fallback: '发送失败' })
  if (!ok) return toast.error('发送失败')
  if (sortBy.value === 'interaction') await loadRequests()
  toast.success('心动已发送')
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

function publisherInitial(item) {
  return (item.publisherInfo?.nickname || '青').slice(0, 1)
}

function inferCategory(description = '') {
  if (/自习|学习|考试|考研|课程/.test(description)) return 'study'
  if (/电影|音乐|演出|展|摄影|画|书/.test(description)) return 'movie'
  if (/旅行|旅游|探店|散步|逛/.test(description)) return 'travel'
  if (/运动|跑步|羽毛球|篮球|健身|活动/.test(description)) return 'activity'
  if (/游戏|咖啡|美食|兴趣|聊天/.test(description)) return 'hobby'
  return 'other'
}

function interestTags(item) {
  const category = inferCategory(item.description)
  const map = {
    study: ['学习', '自习', '成长'],
    hobby: ['兴趣', '聊天', '轻松'],
    activity: ['运动', '活动', '陪伴'],
    movie: ['电影', '音乐', '审美'],
    travel: ['旅行', '散步', '探索'],
    other: ['真诚', '同校', '了解']
  }
  return map[category]
}

onMounted(() => {
  document.body.classList.add('love-route-theme')
  document.body.style.setProperty('--love-page-bg-image', `url(${lovePageBg})`)
  loadRequests()
  startAutoRefresh()
})
onUnmounted(() => {
  document.body.classList.remove('love-route-theme')
  document.body.style.removeProperty('--love-page-bg-image')
  stopAutoRefresh()
})
</script>
