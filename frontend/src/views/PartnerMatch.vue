<template>
  <main class="business-page match-page glass-page" v-if="match">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">匹配详情</span>
        <h1>{{ match.status }}</h1>
        <p>{{ match.request?.type }} · {{ match.request?.description }}</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">申请时间</span>
        <strong>已提交</strong>
        <p>{{ formatTime(match.applyTime) }}</p>
      </aside>
    </section>

    <section class="match-grid">
      <article class="match-person glass-surface">
        <span class="glass-tag">发布者</span>
        <h2>{{ match.publisher?.nickname || '同学' }}</h2>
        <p>{{ match.publisher?.grade }} {{ match.publisher?.major }}</p>
      </article>
      <article class="match-person glass-surface">
        <span class="glass-tag">申请人</span>
        <h2>{{ match.applicant?.nickname || '同学' }}</h2>
        <p>{{ match.applicant?.grade }} {{ match.applicant?.major }}</p>
      </article>
    </section>

    <section class="glass-surface match-note">
      <div class="section-heading">
        <div>
          <h2>申请附言</h2>
          <p>{{ match.applyMessage || '无' }}</p>
        </div>
        <span class="glass-tag" v-if="match.canChat">可以开始聊天</span>
        <span class="glass-tag" v-else>{{ statusLabel(match.status) }}</span>
      </div>
    </section>

    <section class="glass-surface create-form" v-if="match.status === 'ACCEPTED'">
      <h2>提交评价</h2>
      <div class="form-group">
        <label>评分</label>
        <select class="glass-input compact-input" v-model.number="review.rating">
          <option :value="5">5星</option><option :value="4">4星</option>
          <option :value="3">3星</option><option :value="2">2星</option><option :value="1">1星</option>
        </select>
      </div>
      <div class="form-group">
        <label>评价内容（10-100字）</label>
        <textarea class="glass-input" v-model="review.content" placeholder="评价内容（10-100字）" maxlength="100"></textarea>
      </div>
      <button class="glass-button-primary" :disabled="actionLoading || !review.content" @click="submitReview">提交评价</button>
    </section>
  </main>
  <main class="business-page match-page glass-page" v-else>
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">匹配详情</span>
        <h1>{{ loading ? '正在加载' : '无法查看匹配详情' }}</h1>
        <p>{{ error || '匹配不存在，或当前账号没有访问权限。' }}</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api, { unwrapData } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'

const route = useRoute()
const match = ref(null)
const review = ref({ rating: 5, content: '' })
const toast = useToast()
const { loading, error, run } = useAsyncState('匹配详情加载失败')
const { loading: actionLoading, run: runAction } = useAsyncState('评价失败')

async function load() {
  await run(async () => {
    const res = await api.get(`/partner/matches/${route.params.id}`)
    match.value = unwrapData(res)
  })
}

async function submitReview() {
  const ok = await runAction(async () => {
    await api.post(`/partner/matches/${route.params.id}/reviews`, review.value)
    return true
  })
  if (!ok) return toast.error('评价失败')
  review.value = { rating: 5, content: '' }
  toast.success('评价提交成功')
}

onMounted(load)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function statusLabel(status) {
  return ({ PENDING: '等待发布者处理', ACCEPTED: '已通过', REJECTED: '已拒绝', CANCELED: '已取消', ENDED: '已结束' })[status] || status
}
</script>
