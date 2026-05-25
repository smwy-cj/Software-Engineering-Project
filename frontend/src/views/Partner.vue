<template>
  <div>
    <div class="flex-between" style="margin-bottom:16px;">
      <h1 class="page-title">找搭子</h1>
      <button v-if="authStore.isLoggedIn" class="btn" @click="$router.push('/partner/create')">发布需求</button>
    </div>

    <div class="filters">
      <select v-model="type" @change="loadList">
        <option value="">全部类型</option>
        <option value="study">学习</option>
        <option value="sport">运动</option>
        <option value="meal">吃饭</option>
        <option value="exam">考试</option>
        <option value="travel">出行</option>
        <option value="other">其他</option>
      </select>
      <input v-model="keyword" placeholder="搜索搭子需求..." @keyup.enter="loadList" />
    </div>

    <div class="card">
      <div v-if="list.length === 0" class="empty-state">暂无搭子需求</div>
      <div v-for="item in list" :key="item.requestId" class="post-item">
        <div class="post-header">
          <span class="tag">{{ item.type }}</span>
          <span>{{ item.publisherInfo?.nickname }}</span>
          <span class="post-time">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="post-content">{{ item.description }}</div>
        <div class="post-footer">
          <span>已匹配 {{ item.currentMatches }}/{{ item.maxMembers }}</span>
          <span>剩余 {{ daysLeft(item.expireAt) }} 天</span>
        </div>
        <button class="btn-sm" @click="applyMatch(item.requestId)" v-if="authStore.isLoggedIn">申请匹配</button>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page <= 1" @click="page--; loadList()">上一页</button>
        <button class="active">{{ page }}</button>
        <button :disabled="page >= totalPages" @click="page++; loadList()">下一页</button>
      </div>
    </div>
  </div>
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

onMounted(loadList)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function daysLeft(t) { if (!t) return 0; return Math.max(0, Math.ceil((new Date(t) - new Date()) / 86400000)) }
</script>
