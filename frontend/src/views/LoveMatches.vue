<template>
  <div>
    <h1 class="page-title">我的匹配</h1>
    <div class="card">
      <div v-if="matches.length === 0" class="empty-state">暂无匹配记录</div>
      <div v-for="m in matches" :key="m.matchId" class="post-item">
        <div class="post-header">
          <strong>{{ m.partner?.nickname }}</strong>
          <span class="tag">{{ m.status }}</span>
        </div>
        <p>匹配时间：{{ formatTime(m.createdAt) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const matches = ref([])

onMounted(async () => {
  try {
    const res = await api.get('/love/matches')
    matches.value = res.data.data.content || []
  } catch (e) { matches.value = [] }
})

function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
