<template>
  <div>
    <div class="flex-between" style="margin-bottom:16px;">
      <h1 class="page-title">消息通知</h1>
      <button class="btn-sm" @click="markAllRead">全部已读</button>
    </div>

    <div class="card">
      <div v-if="notifs.length === 0" class="empty-state">暂无通知</div>
      <div v-for="n in notifs" :key="n.id" class="post-item" :style="{ opacity: n.isRead ? 0.5 : 1 }">
        <div class="post-header">
          <strong>{{ n.title }}</strong>
          <span class="post-time">{{ formatTime(n.createdAt) }}</span>
        </div>
        <p style="margin:4px 0;">{{ n.content }}</p>
        <button class="btn-sm" v-if="!n.isRead" @click="markRead(n.id)">标记已读</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const notifs = ref([])

async function loadNotifs() {
  try {
    const res = await api.get('/notifications', { params: { size: 50 } })
    notifs.value = res.data.data.content || []
  } catch (e) { notifs.value = [] }
}

async function markRead(id) {
  await api.put(`/notifications/${id}/read`)
  loadNotifs()
}

async function markAllRead() {
  await api.put('/notifications/read-all')
  loadNotifs()
}

onMounted(loadNotifs)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
