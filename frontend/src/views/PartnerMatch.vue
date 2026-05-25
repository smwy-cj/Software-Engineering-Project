<template>
  <div class="card" v-if="match">
    <h2>匹配详情</h2>
    <p>状态：<strong>{{ match.status }}</strong></p>
    <div style="margin:16px 0;">
      <h4>需求信息</h4>
      <p>{{ match.request?.type }} — {{ match.request?.description }}</p>
    </div>
    <div style="display:flex;gap:24px;">
      <div>
        <h4>发布者</h4>
        <p>{{ match.publisher?.nickname }}</p>
        <p>{{ match.publisher?.grade }} {{ match.publisher?.major }}</p>
      </div>
      <div>
        <h4>申请人</h4>
        <p>{{ match.applicant?.nickname }}</p>
        <p>{{ match.applicant?.grade }} {{ match.applicant?.major }}</p>
      </div>
    </div>
    <p style="margin-top:12px;">申请附言：{{ match.applyMessage || '无' }}</p>
    <p>申请时间：{{ formatTime(match.applyTime) }}</p>
    <p v-if="match.canChat" style="color:#27ae60;">可以开始聊天</p>

    <div style="margin-top:16px;" v-if="match.status === 'ACCEPTED'">
      <h4>提交评价</h4>
      <div>
        <select v-model.number="review.rating">
          <option :value="5">5星</option><option :value="4">4星</option>
          <option :value="3">3星</option><option :value="2">2星</option><option :value="1">1星</option>
        </select>
      </div>
      <textarea v-model="review.content" placeholder="评价内容（10-100字）" maxlength="100" style="width:100%;margin-top:8px;padding:8px;border:1px solid #ddd;border-radius:6px;"></textarea>
      <button class="btn" style="margin-top:8px;" @click="submitReview">提交评价</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const match = ref(null)
const review = ref({ rating: 5, content: '' })

async function load() {
  try {
    const res = await api.get(`/partner/matches/${route.params.id}`)
    match.value = res.data.data
  } catch (e) { match.value = null }
}

async function submitReview() {
  try {
    await api.post(`/partner/matches/${route.params.id}/reviews`, review.value)
    alert('评价提交成功')
  } catch (e) { alert(e.response?.data?.message || '评价失败') }
}

onMounted(load)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
