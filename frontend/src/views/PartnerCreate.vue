<template>
  <main class="business-page create-page glass-page">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">发布搭子需求</span>
        <h1>把计划发出去</h1>
        <p>写清类型、人数和有效时间，让同频的人更容易加入。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">可见范围</span>
        <strong>同校</strong>
        <p>默认面向同校同学招募</p>
      </aside>
    </section>

    <section class="create-form glass-surface">
      <div class="form-grid">
        <div class="form-group">
          <label>搭子类型</label>
          <select class="glass-input" v-model="form.type">
            <option value="study">学习</option>
            <option value="sport">运动</option>
            <option value="meal">吃饭</option>
            <option value="exam">考试</option>
            <option value="travel">出行</option>
            <option value="other">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>有效天数（1-7）</label>
          <input class="glass-input" v-model.number="form.validDays" type="number" min="1" max="7" />
        </div>
        <div class="form-group">
          <label>最大人数（1-10）</label>
          <input class="glass-input" v-model.number="form.maxMembers" type="number" min="1" max="10" />
        </div>
      </div>
      <div class="form-group">
        <label>需求描述</label>
        <textarea class="glass-input composer-textarea" v-model="form.description" placeholder="描述你的搭子需求..." maxlength="800"></textarea>
      </div>
      <div class="composer-actions">
        <span class="helper-text">描述越具体，越容易被合适的人理解。</span>
        <div>
          <button class="glass-button-primary" @click="submit" :disabled="!form.description || !form.type">发布</button>
          <button class="glass-button-secondary" @click="$router.back()">取消</button>
        </div>
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
    </section>
  </main>
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
