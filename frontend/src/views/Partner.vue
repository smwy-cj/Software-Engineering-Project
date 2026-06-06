<template>
  <main class="business-page partner-page glass-page" :style="partnerPageStyle">
    <section class="partner-shell">
      <div class="partner-main-column">
        <section class="partner-hero partner-image-hero glass-surface" aria-label="找到同频的同行者">
          <div class="partner-hero-controls">
            <button v-if="authStore.isLoggedIn" class="glass-button-primary partner-hero-action" @click="$router.push('/partner/create')">
              <span class="partner-send-icon" aria-hidden="true"></span>
              发起搭子邀请
            </button>
          </div>
        </section>

        <section class="filter-bar partner-filter glass-surface">
          <div class="segmented-scroll">
            <button
              v-for="item in typeTabs"
              :key="item.value"
              class="nav-pill"
              :class="{ active: type === item.value }"
              @click="setType(item.value)"
            >{{ item.label }}</button>
          </div>
          <label class="partner-search">
            <span class="partner-search-icon" aria-hidden="true"></span>
            <input class="glass-input" v-model="keyword" placeholder="搜索搭子需求..." @keyup.enter="loadList" />
          </label>
          <div class="partner-filter-tools" aria-label="搭子筛选方式">
            <button class="partner-filter-chip active" type="button">
              <span class="filter-chip-icon chip-leaf" aria-hidden="true"></span>
              最新发布
            </button>
            <button class="partner-filter-chip" type="button">
              <span class="filter-chip-icon chip-clock" aria-hidden="true"></span>
              即将开始
            </button>
            <button class="partner-filter-chip" type="button">
              <span class="filter-chip-icon chip-heart" aria-hidden="true"></span>
              最受欢迎
            </button>
          </div>
        </section>

        <section class="partner-grid">
          <p v-if="listError" class="form-error">{{ listError }}</p>
          <div v-if="listLoading" class="empty-state empty-partner partner-empty glass-surface">
            <strong>正在加载搭子邀请...</strong>
          </div>
          <div v-else-if="list.length === 0" class="empty-state empty-partner partner-empty glass-surface">
            <span class="partner-empty-glow" aria-hidden="true"></span>
            <span class="partner-empty-boat" aria-hidden="true"></span>
            <span class="partner-empty-plane" aria-hidden="true"></span>
            <strong>还没有新的搭子邀请，试着发起一个计划吧！</strong>
            <p>把时间、地点和期待写清楚，同频的人会更快靠近。</p>
            <button v-if="authStore.isLoggedIn" class="glass-button-primary" @click="$router.push('/partner/create')">
              <span class="partner-send-icon" aria-hidden="true"></span>
              发布我的需求
            </button>
          </div>
          <article v-for="item in list" :key="item.requestId" class="partner-card glass-surface">
            <div class="partner-card-head">
              <span class="glass-tag partner-type-tag" :class="`partner-type-${item.type || 'other'}`">{{ typeLabel(item.type) }}</span>
              <span class="glass-tag partner-days-tag">剩余 {{ daysLeft(item.expireAt) }} 天</span>
            </div>
            <div class="partner-card-body">
              <h2>{{ item.title || typeLabel(item.type) + '搭子' }}</h2>
              <p>{{ item.description }}</p>
            </div>
            <div class="partner-facts">
              <span><i class="partner-fact-icon fact-time" aria-hidden="true"></i>时间 {{ formatTime(item.createdAt) }}</span>
              <span><i class="partner-fact-icon fact-user" aria-hidden="true"></i>人数 {{ item.currentMatches }}/{{ item.maxMembers }}</span>
              <span><i class="partner-fact-icon fact-status" aria-hidden="true"></i>状态 {{ statusLabel(item) }}</span>
            </div>
            <div class="partner-card-foot">
              <div class="partner-publisher">
                <img v-if="item.publisherInfo?.avatar" :src="item.publisherInfo.avatar" alt="" />
                <span v-else class="publisher-avatar">{{ publisherInitial(item) }}</span>
                <span class="publisher">{{ item.publisherInfo?.nickname || '匿名同学' }}</span>
              </div>
              <button
                class="glass-button-secondary danger-action"
                :disabled="actionLoading"
                @click="cancelRequest(item.requestId)"
                v-if="isOwner(item) && item.status !== 'COMPLETED'"
              >撤销</button>
              <button
                class="glass-button-secondary partner-apply-button"
                :disabled="actionLoading"
                @click="openApplyPanel(item)"
                v-else-if="authStore.isLoggedIn && item.status !== 'COMPLETED'"
              >申请加入</button>
              <span class="glass-tag partner-complete-tag" v-else-if="item.status === 'COMPLETED'">已完成</span>
            </div>
          </article>
        </section>

        <section class="partner-bottom-cards" aria-label="搭子页数据与提醒">
          <article class="partner-bottom-card glass-mini-card partner-rate-card">
            <div>
              <span class="partner-side-icon sprout-icon" aria-hidden="true"></span>
              <strong>今日出发率</strong>
            </div>
            <p><b>78%</b><span>比昨日 +15%</span></p>
            <span class="partner-rate-line" aria-hidden="true"></span>
          </article>
          <article class="partner-bottom-card glass-mini-card partner-friendly-card">
            <div>
              <span class="partner-side-icon bulb-icon" aria-hidden="true"></span>
              <strong>友善小提醒</strong>
            </div>
            <p>真诚邀请，尊重彼此时间，让每一次搭伴都成为美好的回忆。</p>
            <span class="partner-friendly-megaphone" aria-hidden="true"></span>
          </article>
        </section>

        <section v-if="selectedApplyRequest" class="glass-surface admin-panel">
          <h3>申请加入搭子</h3>
          <p>你正在申请：{{ selectedApplyRequest.title || typeLabel(selectedApplyRequest.type) + '搭子' }}</p>
          <div class="form-group">
            <label>申请附言（可选）</label>
            <textarea
              class="glass-input"
              v-model="applyMessage"
              placeholder="简单介绍你的时间、动机或想一起完成的事..."
              maxlength="120"
            ></textarea>
            <span class="helper-text">{{ applyMessage.length }}/120 字</span>
          </div>
          <div class="admin-actions">
            <button class="glass-button-primary" :disabled="actionLoading" @click="submitApply">发送申请</button>
            <button class="glass-button-secondary" :disabled="actionLoading" @click="closeApplyPanel">取消</button>
          </div>
        </section>

        <div class="pagination glass-surface" v-if="totalPages > 1">
          <button class="glass-button-secondary" :disabled="page <= 1 || listLoading" @click="setListPage(page - 1)">上一页</button>
          <button class="glass-button-primary">{{ page }}</button>
          <button class="glass-button-secondary" :disabled="page >= totalPages || listLoading" @click="setListPage(page + 1)">下一页</button>
        </div>

        <section class="partner-suggestion-section">
          <div class="section-heading">
            <div>
              <h2>可能感兴趣的搭子</h2>
              <p>从当前列表里挑出几张更容易加入的邀请。</p>
            </div>
          </div>
          <div class="partner-suggestion-grid">
            <div v-if="suggestedList.length === 0" class="partner-suggestion-empty glass-mini-card">
              <span class="partner-mini-map" aria-hidden="true"></span>
              <strong>暂无推荐搭子</strong>
              <p>发布第一条需求后，这里会出现更适合快速加入的卡片。</p>
            </div>
            <article v-for="item in suggestedList" :key="`suggest-${item.requestId}`" class="partner-suggestion-card glass-mini-card">
              <span class="glass-tag partner-type-tag" :class="`partner-type-${item.type || 'other'}`">{{ typeLabel(item.type) }}</span>
              <h3>{{ item.title || typeLabel(item.type) + '搭子' }}</h3>
              <p>{{ item.description }}</p>
              <div>
                <span><i class="partner-fact-icon fact-user" aria-hidden="true"></i>{{ item.currentMatches }}/{{ item.maxMembers }}人</span>
                <span><i class="partner-fact-icon fact-status" aria-hidden="true"></i>{{ statusLabel(item) }}</span>
              </div>
            </article>
          </div>
        </section>
      </div>

      <aside class="partner-aside">
        <section class="glass-mini-card partner-side-card partner-directions-card">
          <div class="partner-side-head">
            <span class="partner-side-icon sprout-icon" aria-hidden="true"></span>
            <strong>热门方向</strong>
          </div>
          <div class="partner-direction-grid">
            <button
              v-for="item in directionCards"
              :key="item.type"
              class="partner-direction-item"
              type="button"
              @click="setType(item.type)"
            >
              <span class="partner-direction-icon" :class="item.icon" aria-hidden="true"></span>
              <small>{{ item.label }}</small>
            </button>
          </div>
          <span class="side-leaf-cluster" aria-hidden="true"></span>
        </section>
        <section class="glass-mini-card partner-side-card partner-activity-card">
          <div class="partner-side-head">
            <span class="partner-side-icon cactus-icon" aria-hidden="true"></span>
            <strong>本周校园活动</strong>
          </div>
          <div class="partner-activity-list">
            <article v-for="item in campusActivities" :key="item.title" class="partner-activity-item">
              <span class="partner-activity-thumb" :class="item.icon" aria-hidden="true"></span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.time }} · {{ item.count }}人参与</small>
              </div>
              <span class="glass-tag">{{ item.tag }}</span>
            </article>
          </div>
          <span class="side-tag-illustration" aria-hidden="true"></span>
        </section>
        <section class="glass-mini-card partner-side-card partner-inspire-card">
          <div class="partner-side-head">
            <span class="partner-side-icon bulb-icon" aria-hidden="true"></span>
            <strong>搭子灵感</strong>
          </div>
          <p>一个人走得快，一群人走得远。今晚试着发起一次自习、夜跑或周末看展。</p>
          <button class="partner-refresh-chip" type="button">换一换</button>
        </section>
        <section class="glass-mini-card partner-side-card partner-tip-card">
          <div class="partner-side-head">
            <span class="partner-side-icon bulb-icon" aria-hidden="true"></span>
            <strong>匹配小贴士</strong>
          </div>
          <p>完善你的标签和兴趣，更容易找到同频的搭子哦。</p>
          <div class="partner-tip-progress"><span></span></div>
          <span class="partner-backpack" aria-hidden="true"></span>
        </section>
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
import partnerHeroCard from '../assets/partner/partner-hero-card.png'
import partnerPageBg from '../assets/partner/partner-page-bg.png'

const authStore = useAuthStore()
const type = ref('')
const keyword = ref('')
const toast = useToast()
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
const {
  items: list,
  page,
  totalPages,
  loading: listLoading,
  error: listError,
  load: loadListPage,
  reset: resetList,
  setPage
} = usePagination(params => api.get('/partner/requests', {
  params: { ...params, type: type.value || undefined, keyword: keyword.value || undefined }
}), { errorMessage: '搭子列表加载失败' })
const typeTabs = [
  { value: '', label: '全部' },
  { value: 'study', label: '学习' },
  { value: 'sport', label: '运动' },
  { value: 'meal', label: '吃饭' },
  { value: 'travel', label: '出行' },
  { value: 'photography', label: '摄影' },
  { value: 'other', label: '其他' }
]
const directionCards = [
  { type: 'study', label: '学习', icon: 'direction-book' },
  { type: 'sport', label: '运动', icon: 'direction-run' },
  { type: 'meal', label: '吃饭', icon: 'direction-meal' },
  { type: 'travel', label: '出行', icon: 'direction-plane' },
  { type: 'photography', label: '摄影', icon: 'direction-camera' },
  { type: 'other', label: '其他', icon: 'direction-more' }
]
const campusActivities = [
  { title: '校园草坪音乐节', time: '6.7 周五 18:30', count: 234, tag: '音乐', icon: 'activity-music' },
  { title: '毕业季市集', time: '6.8 周六 10:00', count: 156, tag: '市集', icon: 'activity-market' },
  { title: '荧光夜跑', time: '6.9 周日 20:00', count: 189, tag: '运动', icon: 'activity-run' }
]
const partnerPageStyle = computed(() => ({
  '--partner-hero-card-image': `url(${partnerHeroCard})`,
  '--partner-page-bg-image': `url(${partnerPageBg})`
}))

const typeMap = {
  study: '学习',
  sport: '运动',
  meal: '吃饭',
  exam: '考试',
  travel: '出行',
  photography: '摄影',
  game: '游戏',
  other: '其他'
}

const activeCount = computed(() => list.value.filter(item => item.status !== 'COMPLETED').length)
const matchedSeats = computed(() => list.value.reduce((sum, item) => sum + (item.currentMatches || 0), 0))
const suggestedList = computed(() => list.value.slice(0, 4))
const selectedApplyRequest = ref(null)
const applyMessage = ref('')

function setType(nextType) {
  type.value = nextType
  loadList()
}

async function loadList() {
  await resetList()
}

async function setListPage(nextPage) {
  await setPage(nextPage)
}

function openApplyPanel(item) {
  selectedApplyRequest.value = item
  applyMessage.value = ''
}

function closeApplyPanel() {
  selectedApplyRequest.value = null
  applyMessage.value = ''
}

async function submitApply() {
  if (!selectedApplyRequest.value) return
  const ok = await runAction(async () => {
    await api.post(`/partner/requests/${selectedApplyRequest.value.requestId}/apply`, { message: applyMessage.value.trim() })
    return true
  }, { fallback: '申请失败' })
  if (!ok) return toast.error('申请失败')
  closeApplyPanel()
  await loadList()
  toast.success('申请已发送')
}

async function cancelRequest(requestId) {
  if (!confirm('确认撤销这条搭子请求吗？撤销后将不再展示给其他同学。')) return
  const ok = await runAction(async () => {
    await api.put(`/partner/requests/${requestId}/cancel`, { reason: '发布者撤销' })
    return true
  }, { fallback: '撤销失败' })
  if (!ok) return toast.error('撤销失败')
  await loadList()
  toast.success('搭子请求已撤销')
}

onMounted(() => {
  document.body.classList.add('partner-route-theme')
  document.body.style.setProperty('--partner-page-bg-image', `url(${partnerPageBg})`)
  loadListPage()
})
onBeforeUnmount(() => {
  document.body.classList.remove('partner-route-theme')
  document.body.style.removeProperty('--partner-page-bg-image')
})
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function daysLeft(t) { if (!t) return 0; return Math.max(0, Math.ceil((new Date(t) - new Date()) / 86400000)) }
function typeLabel(t) { return typeMap[t] || t || '其他' }
function isOwner(item) { return item.publisherInfo?.userId === authStore.user?.userId }
function statusLabel(item) { return item.status === 'COMPLETED' ? '已完成' : '招募中' }
function publisherInitial(item) { return (item.publisherInfo?.nickname || '同').slice(0, 1) }
</script>
