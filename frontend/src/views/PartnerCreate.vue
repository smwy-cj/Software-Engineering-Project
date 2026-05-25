<template>
  <div class="card" style="max-width:600px;margin:0 auto;">
    <h2>发布搭子需求</h2>
    <div class="form-group">
      <label>搭子类型</label>
      <select v-model="form.type">
        <option value="study">学习</option>
        <option value="sport">运动</option>
        <option value="meal">吃饭</option>
        <option value="exam">考试</option>
        <option value="travel">出行</option>
        <option value="other">其他</option>
      </select>
    </div>
    <div class="form-group">
      <label>需求描述（10-200字）</label>
      <textarea v-model="form.description" placeholder="描述你的搭子需求..." maxlength="200"></textarea>
    </div>
    <div class="form-group">
      <label>匹配条件（JSON格式）</label>
      <textarea v-model="form.conditions" placeholder='{"grade":"2024级","gender":"any"}'></textarea>
    </div>
    <div class="form-group">
      <label>有效天数（1-7）</label>
      <input v-model.number="form.validDays" type="number" min="1" max="7" />
    </div>
    <div class="form-group">
      <label>最大人数（1-10）</label>
      <input v-model.number="form.maxMembers" type="number" min="1" max="10" />
    </div>
    <div style="display:flex;gap:12px;">
      <button class="btn" @click="submit" :disabled="!form.description || !form.type">发布</button>
      <button class="btn-sm" @click="$router.back()">取消</button>
    </div>
    <p v-if="error" class="form-error" style="margin-top:12px;">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const error = ref('')
const form = ref({
  type: 'study',
  description: '',
  conditions: '{}',
  validDays: 3,
  maxMembers: 5,
  visibility: 'sameSchool'
})

async function submit() {
  error.value = ''
  try {
    await api.post('/partner/requests', form.value)
    router.push('/partner')
  } catch (e) {
    error.value = e.response?.data?.message || '发布失败'
  }
}
</script>
