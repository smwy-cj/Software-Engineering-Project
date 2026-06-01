<template>
  <main class="business-page admin-page glass-page">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">管理后台</span>
        <h1>内容与用户治理</h1>
        <p>审核内容、处理反馈，并保持校园社区的温和秩序。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">待处理</span>
        <strong>{{ reviews.length + feedbacks.length }}</strong>
        <p>条审核与反馈事项</p>
      </aside>
    </section>

    <section class="glass-surface admin-panel">
      <h3>待审核内容</h3>
      <div v-if="reviews.length === 0" class="empty-state empty-admin">当前没有待审核内容，系统很平静。</div>
      <div class="admin-list">
        <article v-for="r in reviews" :key="r.reviewId" class="admin-item glass-mini-card">
          <div class="admin-item-meta">
            <span class="glass-tag">{{ r.contentType }}</span>
            <span>内容ID: {{ r.contentId }}</span>
            <span>用户ID: {{ r.userId }}</span>
          </div>
          <div class="admin-actions">
            <button class="glass-button-secondary success-action" @click="reviewItem(r.reviewId, 'PASSED')">通过</button>
            <button class="glass-button-secondary danger-action" @click="reviewItem(r.reviewId, 'REJECTED')">驳回</button>
          </div>
        </article>
      </div>
    </section>

    <section class="glass-surface admin-panel">
      <h3>用户管理</h3>
      <div class="form-grid">
        <div class="form-group">
          <label>用户ID</label>
          <input class="glass-input" v-model.number="banUserId" type="number" placeholder="输入要处理的用户ID" />
        </div>
        <div class="form-group">
          <label>处罚动作</label>
          <select class="glass-input" v-model="banAction">
            <option value="WARNED">警告</option>
            <option value="MUTED">禁言</option>
            <option value="BANNED">封禁</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>原因</label>
        <input class="glass-input" v-model="banReason" placeholder="处罚原因" />
      </div>
      <button class="glass-button-primary danger-primary" @click="banUser">执行处罚</button>
    </section>

    <section class="glass-surface admin-panel">
      <h3>反馈列表</h3>
      <div v-if="feedbacks.length === 0" class="empty-state empty-admin">当前没有新的反馈，社区秩序保持稳定。</div>
      <div class="admin-list">
        <article v-for="f in feedbacks" :key="f.feedbackNumber" class="admin-item glass-mini-card">
          <div class="admin-item-meta">
            <span class="glass-tag">{{ f.type }}</span>
            <strong>{{ f.feedbackNumber }}</strong>
            <span class="glass-tag">{{ f.status }}</span>
          </div>
          <p>{{ f.content }}</p>
          <small>来自：{{ f.userInfo?.nickname }}</small>
          <div class="admin-actions">
            <button class="glass-button-secondary" @click="processFeedback(f.feedbackNumber)">处理</button>
          </div>
        </article>
      </div>
    </section>
  </main>
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
