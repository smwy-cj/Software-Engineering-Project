<template>
  <div>
    <div class="flex-between" style="margin-bottom:16px;">
      <h1 class="page-title">恋爱互助</h1>
      <div>
        <button class="btn-sm" style="margin-right:8px;" @click="$router.push('/love/matches')">我的匹配</button>
        <button class="btn" @click="showCreate = true">发布交友需求</button>
      </div>
    </div>

    <div class="card" v-if="showCreate">
      <h3>发布交友需求</h3>
      <div class="form-group">
        <label>描述</label>
        <textarea v-model="loveForm.description" placeholder="介绍自己和交友期望..." maxlength="200"></textarea>
      </div>
      <div class="form-group">
        <label>有效天数（1-14）</label>
        <input v-model.number="loveForm.validDays" type="number" min="1" max="14" />
      </div>
      <button class="btn" @click="createLoveReq" :disabled="!loveForm.description">发布</button>
      <button class="btn-sm" style="margin-left:8px;" @click="showCreate = false">取消</button>
    </div>

    <div class="filters">
      <select v-model="gender" @change="loadProfiles">
        <option value="">不限性别</option>
        <option value="male">男</option>
        <option value="female">女</option>
      </select>
    </div>

    <div class="card">
      <div v-if="profiles.length === 0" class="empty-state">暂无可浏览的交友资料</div>
      <div v-for="p in profiles" :key="p.userId" class="post-item">
        <div class="post-header">
          <strong>{{ p.nickname }}</strong>
          <span>{{ p.age }}岁</span>
          <span class="tag">{{ p.university }}</span>
        </div>
        <p style="margin:8px 0;">{{ p.declaration }}</p>
        <div class="post-footer">
          <span>{{ p.major }}</span>
          <button class="btn-sm" @click="sendHeart(p.userId)">发送心动</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const profiles = ref([])
const gender = ref('')
const showCreate = ref(false)
const loveForm = ref({ description: '', validDays: 7, scope: 'sameSchool' })

async function loadProfiles() {
  try {
    const res = await api.get('/love/profiles', {
      params: { gender: gender.value || undefined, size: 20 }
    })
    profiles.value = res.data.data.content || []
  } catch (e) { profiles.value = [] }
}

async function createLoveReq() {
  try {
    await api.post('/love/requests', loveForm.value)
    showCreate.value = false
    alert('交友需求发布成功！')
  } catch (e) { alert(e.response?.data?.message || '发布失败') }
}

async function sendHeart(userId) {
  alert('请先通过交友需求列表发送心动')
}

onMounted(loadProfiles)
</script>
