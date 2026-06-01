<template>
  <main class="business-page partner-page glass-page">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">校园搭子</span>
        <h1>找到同频的同行者</h1>
        <p>学习、运动、吃饭、出行，把临时计划变成轻松邀约。每张卡片都是一份校园活动邀请。</p>
      </div>
      <button v-if="authStore.isLoggedIn" class="glass-button-primary" @click="$router.push('/partner/create')">发布需求</button>
    </section>

    <section class="filter-bar partner-filter glass-surface">
      <div class="segmented-scroll">
        <button class="nav-pill" :class="{ active: type === '' }" @click="type = ''; loadList()">全部</button>
        <button class="nav-pill" :class="{ active: type === 'study' }" @click="type = 'study'; loadList()">学习</button>
        <button class="nav-pill" :class="{ active: type === 'sport' }" @click="type = 'sport'; loadList()">运动</button>
        <button class="nav-pill" :class="{ active: type === 'meal' }" @click="type = 'meal'; loadList()">吃饭</button>
        <button class="nav-pill" :class="{ active: type === 'travel' }" @click="type = 'travel'; loadList()">出行</button>
        <button class="nav-pill" :class="{ active: type === 'other' }" @click="type = 'other'; loadList()">其他</button>
      </div>
      <input class="glass-input" v-model="keyword" placeholder="搜索搭子需求..." @keyup.enter="loadList" />
    </section>

    <section class="partner-grid">
      <div v-if="list.length === 0" class="empty-state empty-partner glass-surface">还没有新的搭子邀请，试着发起一个计划吧。</div>
      <article v-for="item in list" :key="item.requestId" class="partner-card glass-surface">
        <div class="partner-card-head">
          <span class="glass-tag">{{ typeLabel(item.type) }}</span>
          <span class="glass-tag">剩余 {{ daysLeft(item.expireAt) }} 天</span>
        </div>
        <h2>{{ item.title || typeLabel(item.type) + '搭子' }}</h2>
        <p>{{ item.description }}</p>
        <div class="partner-facts">
          <span>时间 {{ formatTime(item.createdAt) }}</span>
          <span>人数 {{ item.currentMatches }}/{{ item.maxMembers }}</span>
          <span>状态 {{ statusLabel(item) }}</span>
        </div>
        <div class="partner-card-foot">
          <div class="partner-publisher">
            <img v-if="item.publisherInfo?.avatar" :src="item.publisherInfo.avatar" alt="" />
            <span v-else class="publisher-avatar">{{ publisherInitial(item) }}</span>
            <span class="publisher">{{ item.publisherInfo?.nickname || '匿名同学' }}</span>
          </div>
          <button
            class="glass-button-secondary danger-action"
            @click="cancelRequest(item.requestId)"
            v-if="isOwner(item) && item.status !== 'COMPLETED'"
          >撤销</button>
          <button
            class="glass-button-secondary"
            @click="applyMatch(item.requestId)"
            v-else-if="authStore.isLoggedIn && item.status !== 'COMPLETED'"
          >申请加入</button>
          <span class="glass-tag" v-else-if="item.status === 'COMPLETED'">已完成</span>
        </div>
      </article>
    </section>

    <div class="pagination glass-surface" v-if="totalPages > 1">
      <button class="glass-button-secondary" :disabled="page <= 1" @click="page--; loadList()">上一页</button>
      <button class="glass-button-primary">{{ page }}</button>
      <button class="glass-button-secondary" :disabled="page >= totalPages" @click="page++; loadList()">下一页</button>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const list = ref([])
const page = ref(1)
const totalPages = ref(1)
const type = ref('')
const keyword = ref('')

const typeMap = {
  study: '学习',
  sport: '运动',
  meal: '吃饭',
  exam: '考试',
  travel: '出行',
  game: '游戏',
  other: '其他'
}

async function loadList() {
  try {
    const res = await api.get('/partner/requests', {
      params: { page: page.value, size: 20, type: type.value || undefined, keyword: keyword.value || undefined }
    })
    list.value = res.data.data.content || []
    totalPages.value = res.data.data.totalPages || 1
  } catch (e) { list.value = [] }
}

async function applyMatch(requestId) {
  const msg = prompt('申请附言（可选）：')
  try {
    await api.post(`/partner/requests/${requestId}/apply`, { message: msg || '' })
    alert('申请已发送！')
  } catch (e) { alert(e.response?.data?.message || '申请失败') }
}

async function cancelRequest(requestId) {
  if (!confirm('确认撤销这条搭子请求吗？撤销后将不再展示给其他同学。')) return
  try {
    await api.put(`/partner/requests/${requestId}/cancel`, { reason: '发布者撤销' })
    await loadList()
  } catch (e) { alert(e.response?.data?.message || '撤销失败') }
}

onMounted(loadList)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function daysLeft(t) { if (!t) return 0; return Math.max(0, Math.ceil((new Date(t) - new Date()) / 86400000)) }
function typeLabel(t) { return typeMap[t] || t || '其他' }
function isOwner(item) { return item.publisherInfo?.userId === authStore.user?.userId }
function statusLabel(item) { return item.status === 'COMPLETED' ? '已完成' : '招募中' }
function publisherInitial(item) { return (item.publisherInfo?.nickname || '同').slice(0, 1) }
</script>
