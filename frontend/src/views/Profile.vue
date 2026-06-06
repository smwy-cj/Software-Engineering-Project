<template>
  <main class="business-page profile-page glass-page" :style="profilePageStyle">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">个人中心</span>
        <h1>{{ authStore.user?.username }}</h1>
        <p>管理认证、个人资料和反馈，让青隅更了解你。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">账号状态</span>
        <strong>{{ authStore.user?.accountStatus }}</strong>
        <p>实名认证：{{ certStatus }}</p>
      </aside>
    </section>

    <section class="glass-surface profile-panel">
      <div class="profile-card-head">
        <div class="profile-avatar">
          <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" alt="头像" />
          <span v-else>{{ authStore.user?.username?.slice(0, 1) || '青' }}</span>
        </div>
        <div>
          <h3>{{ authStore.user?.username }}</h3>
          <p>我的校园空间，记录发布、申请、收藏与互动足迹。</p>
        </div>
      </div>
      <div class="profile-meta">
        <span class="glass-tag">用户ID：{{ authStore.user?.userId }}</span>
        <span class="glass-tag">账号状态：{{ authStore.user?.accountStatus }}</span>
        <span class="glass-tag">实名认证：{{ certStatus }}</span>
      </div>
      <div class="profile-quick-grid">
        <button class="profile-quick-action" type="button" :disabled="activityLoading" @click="openActivity('published')">
          <strong>我的发布</strong>
          <span>树洞与搭子记录</span>
        </button>
        <button class="profile-quick-action" type="button" :disabled="activityLoading" @click="openActivity('applications')">
          <strong>我的申请</strong>
          <span>待回应的同行计划</span>
        </button>
        <div>
          <strong>我的收藏</strong>
          <span>值得回看的互动</span>
        </div>
        <div>
          <strong>成长徽章</strong>
          <span>温柔校园足迹</span>
        </div>
      </div>
    </section>

    <section class="glass-surface profile-panel record-panel" v-if="activeActivity">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">{{ activityTitle }}</h2>
          <p class="panel-desc">{{ activeActivity === 'published' ? '按模块查看你发布过的内容。' : '查看你发起过的申请记录和当前状态。' }}</p>
        </div>
        <button class="close-button" type="button" @click="activeActivity = ''">关闭</button>
      </div>
      <p v-if="activityLoading" class="empty-state loading-state">记录加载中...</p>
      <div v-else class="record-section-list">
        <section v-for="section in activitySections" :key="section.key" class="record-section">
          <div class="section-header">
            <div>
              <h3>
                <span class="section-dot" :class="`section-dot-${section.key}`"></span>
                {{ section.label }}
              </h3>
            </div>
            <span>共 {{ section.items.length }} 条</span>
          </div>

          <div v-if="section.items.length === 0" class="empty-state">
            <span class="empty-icon" :class="`section-dot-${section.key}`"></span>
            <div>
              <strong>{{ section.emptyTitle }}</strong>
              <p>{{ section.emptyDesc }}</p>
            </div>
          </div>

          <article v-for="item in section.items" :key="`${section.key}-${item.id}`" class="record-card">
            <div class="record-card-main">
              <h4 class="record-title">{{ item.title }}</h4>
              <p class="record-desc">{{ item.content }}</p>
              <div class="record-meta">{{ normalizeMeta(item.meta) }}</div>
            </div>
            <span class="status-badge" :class="statusClass(item.status)">
              {{ statusLabel(item.status) }}
            </span>
          </article>
        </section>
      </div>
    </section>

    <section class="glass-surface profile-panel" v-if="certStatus === 'UNCERTIFIED'">
      <h3>实名认证</h3>
      <div class="form-grid">
        <div class="form-group">
          <label>学号</label>
          <input class="glass-input" v-model="certForm.studentId" placeholder="学号" />
        </div>
        <div class="form-group">
          <label>学校</label>
          <input class="glass-input" v-model="certForm.school" placeholder="学校" />
        </div>
        <div class="form-group">
          <label>性别</label>
          <select class="glass-input" v-model="certForm.gender">
            <option value="male">男</option>
            <option value="female">女</option>
          </select>
        </div>
        <div class="form-group">
          <label>年龄</label>
          <input class="glass-input" v-model.number="certForm.age" type="number" min="16" max="60" />
        </div>
      </div>
      <button class="glass-button-primary" :disabled="actionLoading" @click="submitCert">提交认证</button>
    </section>

    <section class="glass-surface profile-panel" v-if="certStatus === 'CERTIFIED'">
      <h3>完善个人资料</h3>
      <div class="form-grid">
        <div class="form-group">
          <label>昵称</label>
          <input class="glass-input" v-model="profileForm.nickname" placeholder="不超过16位，需保持唯一" maxlength="16" />
        </div>
        <div class="form-group">
          <label>头像图片</label>
          <input class="glass-input" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml" @change="handleAvatarFile" />
          <div class="avatar-preview" v-if="profileForm.avatar">
            <img :src="profileForm.avatar" alt="头像预览" />
            <button class="glass-button-secondary" type="button" @click="profileForm.avatar = ''">移除</button>
          </div>
        </div>
        <div class="form-group">
          <label>年级</label>
          <select class="glass-input" v-model="profileForm.grade">
            <option v-for="year in enrollmentYears" :key="year" :value="`${year}级`">{{ year }}级</option>
          </select>
        </div>
        <div class="form-group">
          <label>专业</label>
          <input class="glass-input" v-model="profileForm.major" placeholder="专业" maxlength="64" />
        </div>
      </div>
      <div class="form-group">
        <label>个性签名</label>
        <textarea class="glass-input" v-model="profileForm.bio" placeholder="写一句个人签名..." maxlength="100"></textarea>
      </div>
      <button class="glass-button-primary" :disabled="actionLoading" @click="submitProfile">保存个人资料</button>
    </section>

    <section class="glass-surface profile-panel">
      <h3>提交反馈</h3>
      <div class="form-group">
        <label>类型</label>
        <select class="glass-input" v-model="feedback.type">
          <option value="bug">问题反馈</option>
          <option value="suggestion">功能建议</option>
          <option value="content">内容举报</option>
          <option value="other">其他</option>
        </select>
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea class="glass-input" v-model="feedback.content" placeholder="详细描述..." maxlength="500"></textarea>
      </div>
      <button class="glass-button-primary" :disabled="actionLoading || !feedback.content" @click="submitFeedback">提交反馈</button>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../store/auth'
import api, { unwrapData } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'
import profilePageBg from '../assets/profile/profile-page-bg.png'

const authStore = useAuthStore()
const certStatus = ref('UNCERTIFIED')
const currentYear = new Date().getFullYear()
const enrollmentYears = Array.from({ length: 10 }, (_, index) => currentYear - index)
const certForm = ref({ studentId: '', school: '', gender: 'male', age: 20 })
const profileForm = ref({
  nickname: authStore.user?.username || '',
  avatar: authStore.user?.avatar || '',
  major: '',
  grade: `${currentYear}级`,
  bio: ''
})
const feedback = ref({ type: 'bug', content: '' })
const activeActivity = ref('')
const activityLoading = ref(false)
const activityData = ref({})
const toast = useToast()
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')
const profilePageStyle = computed(() => ({
  '--profile-page-bg-image': `url(${profilePageBg})`
}))

const activityTitle = computed(() => activeActivity.value === 'published' ? '我的发布' : '我的申请')
const activitySections = computed(() => {
  if (activeActivity.value === 'published') {
    return [
      {
        key: 'treeHole',
        label: '树洞发布',
        items: activityData.value.treeHole || [],
        emptyTitle: '暂无发布记录',
        emptyDesc: '你还没有发布树洞内容，可以记录一次校园里的想法。'
      },
      {
        key: 'partner',
        label: '搭子发布',
        items: activityData.value.partner || [],
        emptyTitle: '暂无发布记录',
        emptyDesc: '你还没有发布搭子需求，可以去寻找同频同行的人。'
      },
      {
        key: 'love',
        label: '交友需求',
        items: activityData.value.love || [],
        emptyTitle: '暂无发布记录',
        emptyDesc: '你还没有发布交友需求，完善资料后可以再试试。'
      }
    ]
  }
  return [
    {
      key: 'partner',
      label: '搭子申请',
      items: activityData.value.partner || [],
      emptyTitle: '暂无申请记录',
      emptyDesc: '你还没有发起搭子申请，可以去看看感兴趣的同行计划。'
    },
    {
      key: 'love',
      label: '心动申请',
      items: activityData.value.love || [],
      emptyTitle: '暂无申请记录',
      emptyDesc: '你还没有发起心动申请，可以去浏览新的交友需求。'
    }
  ]
})

const statusTextMap = {
  PUBLISHED: '已发布',
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CLOSED: '已关闭',
  EXPIRED: '已过期',
  DRAFT: '草稿',
  ACCEPTED: '已通过'
}

onMounted(async () => {
  document.body.classList.add('profile-route-theme')
  document.body.style.setProperty('--profile-page-bg-image', `url(${profilePageBg})`)
  try {
    const res = await api.get('/auth/cert-status')
    const profile = unwrapData(res)
    certStatus.value = profile.certStatus || 'UNCERTIFIED'
    profileForm.value = {
      nickname: profile.nickname || profile.username || authStore.user?.username || '',
      avatar: profile.avatar || authStore.user?.avatar || '',
      major: profile.major || '',
      grade: profile.grade || `${currentYear}级`,
      bio: profile.bio || ''
    }
  } catch (e) {
    toast.error('个人资料加载失败')
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('profile-route-theme')
  document.body.style.removeProperty('--profile-page-bg-image')
})

async function submitCert() {
  const ok = await runAction(async () => {
    await api.post('/auth/certify', certForm.value)
    return true
  }, { fallback: '认证失败' })
  if (!ok) return toast.error('认证失败')
  certStatus.value = 'CERTIFIED'
  toast.success('认证提交成功')
}

async function submitProfile() {
  const data = await runAction(async () => {
    const res = await api.put('/auth/profile', profileForm.value)
    return unwrapData(res)
  }, { fallback: '保存失败' })
  if (!data) return toast.error('保存失败')
  authStore.updateUserInfo({ username: data.username, avatar: data.avatar })
  profileForm.value = {
    ...profileForm.value,
    ...data,
    nickname: data.nickname || data.username || profileForm.value.nickname
  }
  toast.success('个人资料保存成功')
}

async function openActivity(type) {
  activeActivity.value = type
  activityLoading.value = true
  activityData.value = {}
  try {
    const url = type === 'published' ? '/profile/published' : '/profile/applications'
    const res = await api.get(url)
    activityData.value = unwrapData(res) || {}
  } catch (e) {
    toast.error('记录加载失败')
  } finally {
    activityLoading.value = false
  }
}

function handleAvatarFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件')
    event.target.value = ''
    return
  }
  if (file.size > 512 * 1024) {
    toast.error('头像图片不能超过512KB')
    event.target.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.value.avatar = String(reader.result || '')
  }
  reader.onerror = () => toast.error('头像读取失败')
  reader.readAsDataURL(file)
}

function statusLabel(status) {
  return statusTextMap[status] || '处理中'
}

function statusClass(status) {
  return `status-${String(status || 'unknown').toLowerCase()}`
}

function normalizeMeta(meta) {
  return String(meta || '')
    .replace(/最多\s+(\d+)\s+人\s+·\s+有效\s+(\d+)\s+天/, '最多 $1 人加入 · 有效期 $2 天')
}

async function submitFeedback() {
  const ok = await runAction(async () => {
    await api.post('/feedback', feedback.value)
    return true
  }, { fallback: '提交失败' })
  if (!ok) return toast.error('提交失败')
  feedback.value = { type: 'bug', content: '' }
  toast.success('反馈提交成功')
}

</script>

<style scoped>
.profile-avatar {
  overflow: hidden;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-quick-grid {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.profile-quick-action {
  display: grid;
  gap: 5px;
  padding: 14px;
  min-height: 86px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(79, 145, 217, 0.14);
  border-radius: 18px;
  text-align: left;
  cursor: pointer;
  color: inherit;
  font: inherit;
}

.profile-quick-action:hover {
  transform: translateY(-1px);
}

.record-panel {
  display: grid;
  gap: 18px;
  color: #3e3856;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.panel-title {
  margin: 0;
  color: #3e3856;
  font-size: clamp(22px, 4vw, 24px);
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.25;
}

.panel-desc {
  margin: 8px 0 0;
  color: #6b6680;
  font-size: 14px;
  line-height: 1.65;
}

.close-button {
  min-width: 62px;
  min-height: 34px;
  padding: 0 18px;
  border: 1px solid rgba(111, 92, 150, 0.16);
  border-radius: 999px;
  color: #574a78;
  background: rgba(238, 232, 255, 0.72);
  box-shadow: 0 8px 18px rgba(91, 72, 129, 0.08);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  transition: background 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.close-button:hover {
  background: rgba(226, 217, 255, 0.9);
  box-shadow: 0 10px 22px rgba(91, 72, 129, 0.12);
  transform: translateY(-1px);
}

.record-section-list {
  display: grid;
  gap: 16px;
}

.record-section {
  display: grid;
  gap: 10px;
  width: 100%;
  padding: 16px;
  border: 1px solid rgba(111, 92, 150, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 12px 28px rgba(91, 72, 129, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 0 2px;
}

.section-header h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #3e3856;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.section-header span {
  flex: 0 0 auto;
  color: #8b86a0;
  font-size: 12px;
  font-weight: 600;
}

.section-dot,
.empty-icon {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #9b8cf4;
  box-shadow: 0 0 0 4px rgba(155, 140, 244, 0.14);
}

.section-dot-partner {
  background: #77a8f7;
  box-shadow: 0 0 0 4px rgba(119, 168, 247, 0.14);
}

.section-dot-love {
  background: #e69ac6;
  box-shadow: 0 0 0 4px rgba(230, 154, 198, 0.14);
}

.record-card {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 14px 14px 13px;
  border: 1px solid rgba(111, 92, 150, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 8px 20px rgba(91, 72, 129, 0.05);
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.record-card:hover {
  box-shadow: 0 12px 26px rgba(91, 72, 129, 0.1);
  transform: translateY(-1px);
}

.record-card-main {
  min-width: 0;
  padding-right: 4px;
}

.record-title {
  margin: 0;
  padding-right: 72px;
  color: #3f3958;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.42;
}

.record-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 7px 0 0;
  color: #6b6680;
  font-size: 14px;
  line-height: 1.55;
}

.record-meta {
  margin-top: 8px;
  color: #928da5;
  font-size: 12px;
  line-height: 1.45;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 9px;
  border-radius: 999px;
  color: #5f4d91;
  background: rgba(231, 224, 255, 0.86);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.status-approved,
.status-accepted,
.status-published {
  color: #4f4380;
  background: rgba(225, 220, 255, 0.86);
}

.status-pending {
  color: #6b5798;
  background: rgba(239, 232, 255, 0.88);
}

.status-rejected,
.status-closed,
.status-expired {
  color: #7f6073;
  background: rgba(245, 226, 236, 0.88);
}

.empty-state {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 14px;
  border: 1px dashed rgba(111, 92, 150, 0.18);
  border-radius: 16px;
  color: #6b6680;
  background: rgba(249, 247, 255, 0.68);
}

.loading-state {
  justify-content: center;
}

.empty-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.empty-state strong {
  display: block;
  color: #4a4264;
  font-size: 14px;
  font-weight: 700;
}

.empty-state p {
  margin: 4px 0 0;
  color: #7e7892;
  font-size: 13px;
  line-height: 1.45;
}

.record-section .empty-state {
  min-height: 76px;
}

.status-badge,
.record-meta,
.section-header span {
  font-style: normal;
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.avatar-preview img {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid rgba(77, 107, 168, 0.18);
}

@media (max-width: 560px) {
  .panel-header,
  .record-card {
    align-items: flex-start;
  }

  .panel-header {
    flex-direction: column;
  }

  .close-button {
    align-self: flex-start;
  }

  .record-section {
    padding: 14px;
  }

  .record-card {
    padding: 14px;
  }

  .record-title {
    padding-right: 68px;
  }
}
</style>
