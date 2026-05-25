<template>
  <div>
    <h1 class="page-title">管理后台</h1>

    <div class="card">
      <h3>待审核内容</h3>
      <div v-if="reviews.length === 0" class="empty-state">暂无待审核内容</div>
      <div v-for="r in reviews" :key="r.reviewId" class="post-item">
        <span class="tag">{{ r.contentType }}</span>
        <span>内容ID: {{ r.contentId }}</span>
        <span>用户ID: {{ r.userId }}</span>
        <div style="margin-top:8px;">
          <button class="btn-sm btn-success" @click="reviewItem(r.reviewId, 'PASSED')">通过</button>
          <button class="btn-sm btn-danger" style="margin-left:8px;" @click="reviewItem(r.reviewId, 'REJECTED')">驳回</button>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>用户管理</h3>
      <div class="form-group">
        <label>用户ID</label>
        <input v-model.number="banUserId" type="number" placeholder="输入要处理的用户ID" />
        <select v-model="banAction" style="margin-top:8px;">
          <option value="WARNED">警告</option>
          <option value="MUTED">禁言</option>
          <option value="BANNED">封禁</option>
        </select>
      </div>
      <div class="form-group">
        <label>原因</label>
        <input v-model="banReason" placeholder="处罚原因" />
      </div>
      <button class="btn btn-danger" @click="banUser">执行处罚</button>
    </div>

    <div class="card">
      <h3>反馈列表</h3>
      <div v-if="feedbacks.length === 0" class="empty-state">暂无反馈</div>
      <div v-for="f in feedbacks" :key="f.feedbackNumber" class="post-item">
        <span class="tag">{{ f.type }}</span>
        <strong>{{ f.feedbackNumber }}</strong>
        <span style="margin-left:8px;">{{ f.status }}</span>
        <p>{{ f.content }}</p>
        <small>来自：{{ f.userInfo?.nickname }}</small>
        <div style="margin-top:4px;">
          <button class="btn-sm" @click="processFeedback(f.feedbackNumber)">处理</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const reviews = ref([])
const feedbacks = ref([])
const banUserId = ref(null)
const banAction = ref('WARNED')
const banReason = ref('')

onMounted(async () => {
  try {
    const [rRes, fRes] = await Promise.all([
      api.get('/admin/reviews/pending', { params: { size: 50 } }),
      api.get('/admin/feedback', { params: { size: 50 } })
    ])
    reviews.value = rRes.data.data.content || []
    feedbacks.value = fRes.data.data.content || []
  } catch (e) { /* need admin */ }
})

async function reviewItem(reviewId, result) {
  try {
    await api.put(`/admin/reviews/${reviewId}`, { result, comment: result === 'PASSED' ? '审核通过' : '内容违规' })
    reviews.value = reviews.value.filter(r => r.reviewId !== reviewId)
  } catch (e) { alert(e.response?.data?.message || '操作失败') }
}

async function banUser() {
  if (!banUserId.value || !banReason.value) return alert('请填写完整信息')
  try {
    await api.post(`/admin/users/${banUserId.value}/ban`, {
      action: banAction.value, reason: banReason.value, duration: 7
    })
    alert('处罚已执行！')
  } catch (e) { alert(e.response?.data?.message || '操作失败') }
}

async function processFeedback(fbNumber) {
  const comment = prompt('处理意见：')
  if (!comment) return
  try {
    await api.put(`/admin/feedback/${fbNumber}`, { status: 'PROCESSING', processComment: comment })
    alert('处理完成！')
  } catch (e) { alert(e.response?.data?.message || '操作失败') }
}
</script>
