<template>
  <div>
    <h1 class="page-title">个人中心</h1>
    <div class="card">
      <h3>{{ authStore.user?.username }}</h3>
      <p>用户ID：{{ authStore.user?.userId }}</p>
      <p>账号状态：{{ authStore.user?.accountStatus }}</p>
      <p>实名认证：{{ certStatus }}</p>
    </div>

    <div class="card" v-if="certStatus === 'UNCERTIFIED'">
      <h3>实名认证</h3>
      <div class="form-group">
        <label>学号</label>
        <input v-model="certForm.studentId" placeholder="学号" />
      </div>
      <div class="form-group">
        <label>真实姓名</label>
        <input v-model="certForm.realName" placeholder="真实姓名" />
      </div>
      <div class="form-group">
        <label>身份证号</label>
        <input v-model="certForm.idCard" placeholder="身份证号" />
      </div>
      <div class="form-group">
        <label>学校</label>
        <input v-model="certForm.university" placeholder="学校" />
      </div>
      <div class="form-group">
        <label>专业</label>
        <input v-model="certForm.major" placeholder="专业" />
      </div>
      <div class="form-group">
        <label>年级</label>
        <input v-model="certForm.grade" placeholder="年级" />
      </div>
      <div class="form-group">
        <label>性别</label>
        <select v-model="certForm.gender">
          <option value="male">男</option>
          <option value="female">女</option>
        </select>
      </div>
      <div class="form-group">
        <label>年龄</label>
        <input v-model.number="certForm.age" type="number" min="16" max="60" />
      </div>
      <button class="btn" @click="submitCert">提交认证</button>
    </div>

    <div class="card" v-if="certStatus === 'CERTIFIED'">
      <h3>完善交友资料</h3>
      <div class="form-group">
        <label>性别</label>
        <select v-model="loveForm.gender">
          <option value="male">男</option>
          <option value="female">女</option>
        </select>
      </div>
      <div class="form-group">
        <label>年龄</label>
        <input v-model.number="loveForm.age" type="number" />
      </div>
      <div class="form-group">
        <label>身高（cm）</label>
        <input v-model.number="loveForm.height" type="number" />
      </div>
      <div class="form-group">
        <label>择偶标准（JSON）</label>
        <textarea v-model="loveForm.matePreference" placeholder='{"gender":"female","ageRange":[18,22]}'></textarea>
      </div>
      <div class="form-group">
        <label>交友宣言</label>
        <textarea v-model="loveForm.declaration" placeholder="写一句交友宣言..." maxlength="100"></textarea>
      </div>
      <button class="btn" @click="submitLoveProfile">保存</button>
    </div>

    <div class="card">
      <h3>提交反馈</h3>
      <div class="form-group">
        <label>类型</label>
        <select v-model="feedback.type">
          <option value="bug">问题反馈</option>
          <option value="suggestion">功能建议</option>
          <option value="content">内容举报</option>
          <option value="other">其他</option>
        </select>
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea v-model="feedback.content" placeholder="详细描述..." maxlength="500"></textarea>
      </div>
      <button class="btn" @click="submitFeedback">提交反馈</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const certStatus = ref('UNCERTIFIED')
const certForm = ref({ studentId: '', realName: '', idCard: '', university: '', major: '', grade: '', gender: 'male', age: 20 })
const loveForm = ref({ gender: 'male', age: 20, height: 170, matePreference: '{}', declaration: '' })
const feedback = ref({ type: 'bug', content: '' })

onMounted(async () => {
  try {
    const res = await api.get('/auth/cert-status')
    certStatus.value = res.data.data.certStatus || 'UNCERTIFIED'
  } catch (e) { /* fail */ }
})

async function submitCert() {
  try {
    await api.post('/auth/certify', certForm.value)
    alert('认证提交成功！')
    certStatus.value = 'CERTIFIED'
  } catch (e) { alert(e.response?.data?.message || '认证失败') }
}

async function submitLoveProfile() {
  try {
    await api.put('/love/profiles/me', loveForm.value)
    alert('交友资料保存成功！')
  } catch (e) { alert(e.response?.data?.message || '保存失败') }
}

async function submitFeedback() {
  try {
    await api.post('/feedback', feedback.value)
    alert('反馈提交成功！')
    feedback.value = { type: 'bug', content: '' }
  } catch (e) { alert(e.response?.data?.message || '提交失败') }
}
</script>
