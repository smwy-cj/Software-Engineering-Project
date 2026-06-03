<template>
  <main class="business-page profile-page glass-page">
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

    <section class="glass-surface profile-panel" v-if="activeActivity">
      <div class="profile-section-head">
        <div>
          <h3>{{ activityTitle }}</h3>
          <p>{{ activeActivity === 'published' ? '按模块查看你发布过的内容。' : '查看你发起过的申请记录和当前状态。' }}</p>
        </div>
        <button class="glass-button-secondary" type="button" @click="activeActivity = ''">关闭</button>
      </div>
      <p v-if="activityLoading" class="empty-state">加载中...</p>
      <div v-else class="activity-section-list">
        <section v-for="section in activitySections" :key="section.key" class="activity-section glass-mini-card">
          <h4>{{ section.label }}</h4>
          <p v-if="section.items.length === 0" class="activity-empty">暂无记录</p>
          <article v-for="item in section.items" :key="`${section.key}-${item.id}`" class="activity-item">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
              <span>{{ item.meta }}</span>
            </div>
            <em>{{ item.status }}</em>
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
          <label>真实姓名</label>
          <input class="glass-input" v-model="certForm.realName" placeholder="真实姓名" />
        </div>
        <div class="form-group">
          <label>身份证号</label>
          <input class="glass-input" v-model="certForm.idCard" placeholder="身份证号" />
        </div>
        <div class="form-group">
          <label>学校</label>
          <input class="glass-input" v-model="certForm.university" placeholder="学校" />
        </div>
        <div class="form-group">
          <label>专业</label>
          <input class="glass-input" v-model="certForm.major" placeholder="专业" />
        </div>
        <div class="form-group">
          <label>年级</label>
          <input class="glass-input" v-model="certForm.grade" placeholder="年级" />
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
          <label>用户名</label>
          <input class="glass-input" v-model="profileForm.username" placeholder="不超过16位，需保持唯一" maxlength="16" />
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
          <label>性别</label>
          <select class="glass-input" v-model="profileForm.gender">
            <option value="male">男</option>
            <option value="female">女</option>
          </select>
        </div>
        <div class="form-group">
          <label>年龄</label>
          <input class="glass-input" v-model.number="profileForm.age" type="number" min="16" max="60" />
        </div>
        <div class="form-group">
          <label>入学年份</label>
          <select class="glass-input" v-model.number="profileForm.enrollmentYear">
            <option v-for="year in enrollmentYears" :key="year" :value="year">{{ year }}级</option>
          </select>
        </div>
        <div class="form-group">
          <label>专业</label>
          <input class="glass-input" v-model="profileForm.major" placeholder="专业" maxlength="64" />
        </div>
        <div class="form-group">
          <label>身高（cm）</label>
          <input class="glass-input" v-model.number="profileForm.height" type="number" min="120" max="230" />
        </div>
      </div>
      <div class="form-group">
        <label>个性签名</label>
        <textarea class="glass-input" v-model="profileForm.signature" placeholder="写一句个人签名..." maxlength="100"></textarea>
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
import { computed, ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api, { unwrapData } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import { useToast } from '../composables/useToast'

const authStore = useAuthStore()
const certStatus = ref('UNCERTIFIED')
const currentYear = new Date().getFullYear()
const enrollmentYears = Array.from({ length: 10 }, (_, index) => currentYear - index)
const certForm = ref({ studentId: '', realName: '', idCard: '', university: '', major: '', grade: '', gender: 'male', age: 20 })
const profileForm = ref({
  username: authStore.user?.username || '',
  avatar: authStore.user?.avatar || '',
  gender: 'male',
  age: 20,
  enrollmentYear: currentYear,
  height: 170,
  major: '',
  signature: ''
})
const feedback = ref({ type: 'bug', content: '' })
const activeActivity = ref('')
const activityLoading = ref(false)
const activityData = ref({})
const toast = useToast()
const { loading: actionLoading, run: runAction } = useAsyncState('操作失败')

const activityTitle = computed(() => activeActivity.value === 'published' ? '我的发布' : '我的申请')
const activitySections = computed(() => {
  if (activeActivity.value === 'published') {
    return [
      { key: 'treeHole', label: '树洞发布', items: activityData.value.treeHole || [] },
      { key: 'partner', label: '搭子发布', items: activityData.value.partner || [] },
      { key: 'love', label: '交友需求', items: activityData.value.love || [] }
    ]
  }
  return [
    { key: 'partner', label: '搭子申请', items: activityData.value.partner || [] },
    { key: 'love', label: '心动申请', items: activityData.value.love || [] }
  ]
})

onMounted(async () => {
  try {
    const res = await api.get('/auth/cert-status')
    const profile = unwrapData(res)
    certStatus.value = profile.certStatus || 'UNCERTIFIED'
    profileForm.value = {
      username: profile.username || authStore.user?.username || '',
      avatar: profile.avatar || authStore.user?.avatar || '',
      gender: profile.gender || 'male',
      age: profile.age || 20,
      enrollmentYear: profile.enrollmentYear || parseEnrollmentYear(profile.grade) || currentYear,
      height: profile.height || 170,
      major: profile.major || '',
      signature: profile.signature || ''
    }
  } catch (e) {
    toast.error('个人资料加载失败')
  }
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
    enrollmentYear: data.enrollmentYear || profileForm.value.enrollmentYear
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

async function submitFeedback() {
  const ok = await runAction(async () => {
    await api.post('/feedback', feedback.value)
    return true
  }, { fallback: '提交失败' })
  if (!ok) return toast.error('提交失败')
  feedback.value = { type: 'bug', content: '' }
  toast.success('反馈提交成功')
}

function parseEnrollmentYear(grade) {
  const matched = String(grade || '').match(/\d{4}/)
  return matched ? Number(matched[0]) : null
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

.profile-section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.activity-section-list {
  display: grid;
  gap: 14px;
}

.activity-section h4 {
  margin: 0 0 12px;
}

.activity-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 0;
  border-top: 1px solid rgba(77, 107, 168, 0.14);
}

.activity-item p {
  margin: 6px 0;
}

.activity-item span,
.activity-item em,
.activity-empty {
  color: rgba(14, 29, 58, 0.62);
  font-style: normal;
  font-size: 13px;
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
</style>
