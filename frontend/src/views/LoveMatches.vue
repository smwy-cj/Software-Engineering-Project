<template>
  <main class="business-page love-matches-page glass-page">
    <section class="business-hero love-hero glass-surface">
      <div>
        <span class="hero-kicker">我的匹配</span>
        <h1>心动互动记录</h1>
        <p>查看已经建立的匹配关系，保留每一次认真靠近的痕迹。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">匹配数量</span>
        <strong>{{ matches.length }}</strong>
        <p>条历史记录</p>
      </aside>
    </section>

    <section class="glass-surface notification-panel">
      <p v-if="error" class="form-error">{{ error }}</p>
      <div v-if="loading" class="empty-state empty-love">正在加载匹配记录...</div>
      <div v-else-if="matches.length === 0" class="empty-state empty-love">今天还没有心动留言，勇敢一点也许会遇见惊喜。</div>
      <div v-else class="notification-list">
        <article v-for="m in matches" :key="m.matchId" class="notification-item glass-mini-card">
          <div class="feed-avatar"></div>
          <div class="notification-body">
            <div class="post-header">
              <strong>{{ m.partner?.nickname || '同学' }}</strong>
              <span class="glass-tag">{{ m.status }}</span>
            </div>
            <p>匹配时间：{{ formatTime(m.createdAt) }}</p>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { unwrapPage } from '../api'
import { useAsyncState } from '../composables/useAsyncState'

const matches = ref([])
const { loading, error, run } = useAsyncState('匹配记录加载失败')

onMounted(async () => {
  await run(async () => {
    const res = await api.get('/love/matches')
    matches.value = unwrapPage(res).content
  })
})

function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
