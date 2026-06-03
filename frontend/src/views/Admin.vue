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
      <p v-if="loadError" class="form-error">{{ loadError }}</p>
      <div v-if="loading" class="empty-state empty-admin">正在加载待处理事项...</div>
      <div v-else-if="reviews.length === 0" class="empty-state empty-admin">当前没有待审核内容，系统很平静。</div>
      <div class="admin-list">
        <article v-for="r in reviews" :key="r.reviewId" class="admin-item glass-mini-card">
          <div class="admin-item-meta">
            <span class="glass-tag">{{ contentTypeLabel(r.contentType) }}</span>
            <span>内容ID: {{ r.contentId }}</span>
            <span>用户ID: {{ r.userId }}</span>
            <span v-if="r.submitTime">提交时间: {{ formatTime(r.submitTime) }}</span>
          </div>
          <p>{{ reviewSnapshot(r) }}</p>
          <div class="admin-actions">
            <button class="glass-button-secondary success-action" :disabled="actionLoading" @click="reviewItem(r.reviewId, 'PASSED')">通过</button>
            <button class="glass-button-secondary danger-action" :disabled="actionLoading" @click="reviewItem(r.reviewId, 'REJECTED')">驳回</button>
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
      <button class="glass-button-primary danger-primary" :disabled="actionLoading" @click="banUser">执行处罚</button>
    </section>

    <section class="glass-surface admin-panel">
      <h3>反馈列表</h3>
      <div v-if="loading" class="empty-state empty-admin">正在加载反馈...</div>
      <div v-else-if="feedbacks.length === 0" class="empty-state empty-admin">当前没有新的反馈，社区秩序保持稳定。</div>
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
            <button class="glass-button-secondary" :disabled="actionLoading" @click="processFeedback(f.feedbackNumber)">处理</button>
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
import { useToast } from '../composables/useToast'

const reviews = ref([])
const feedbacks = ref([])
const banUserId = ref(null)
const banAction = ref('WARNED')
const banReason = ref('')
const toast = useToast()
const { loading, error: loadError, run: runLoad } = useAsyncState('管理数据加载失败')
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
const contentTypeMap = {
  treeholePost: '树洞动态',
  partnerReq: '搭子需求',
  loveReq: '交友需求'
}

async function loadAdminData() {
  await runLoad(async () => {
    const [rRes, fRes] = await Promise.all([
      api.get('/admin/reviews/pending', { params: { size: 50 } }),
      api.get('/admin/feedback', { params: { size: 50 } })
    ])
    reviews.value = unwrapPage(rRes).content
    feedbacks.value = unwrapPage(fRes).content
  })
}

async function reviewItem(reviewId, result) {
  const ok = await runAction(async () => {
    await api.put(`/admin/reviews/${reviewId}`, { result, comment: result === 'PASSED' ? '审核通过' : '内容违规' })
    return true
  })
  if (!ok) return toast.error('审核操作失败')
  reviews.value = reviews.value.filter(r => r.reviewId !== reviewId)
  toast.success(result === 'PASSED' ? '内容已通过' : '内容已驳回')
}

async function banUser() {
  if (!banUserId.value || !banReason.value) {
    toast.error('请填写完整信息')
    return
  }

  const ok = await runAction(async () => {
    await api.post(`/admin/users/${banUserId.value}/ban`, {
      action: banAction.value, reason: banReason.value, duration: 7
    })
    return true
  })
  if (!ok) return toast.error('处罚执行失败')
  banReason.value = ''
  toast.success('处罚已执行')
}

async function processFeedback(fbNumber) {
  const comment = prompt('处理意见：')
  if (!comment) return
  const ok = await runAction(async () => {
    await api.put(`/admin/feedback/${fbNumber}`, { status: 'PROCESSING', processComment: comment })
    return true
  }, { fallback: '反馈处理失败' })
  if (!ok) return toast.error('反馈处理失败')
  feedbacks.value = feedbacks.value.filter(f => f.feedbackNumber !== fbNumber)
  toast.success('反馈处理完成')
}

onMounted(loadAdminData)
function contentTypeLabel(type) { return contentTypeMap[type] || type || '待审核内容' }
function reviewSnapshot(item) { return item.contentSnapshot || '暂无正文快照，请进入内容详情后再审核。' }
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
