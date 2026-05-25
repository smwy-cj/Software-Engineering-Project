<template>
  <div>
    <div class="flex-between" style="margin-bottom:16px;">
      <h1 class="page-title">树洞</h1>
      <button v-if="authStore.isLoggedIn" class="btn" @click="showCreate = true">发布动态</button>
    </div>

    <div class="filters">
      <select v-model="category" @change="loadPosts">
        <option value="">全部分类</option>
        <option value="emotion">情感</option>
        <option value="study">学习</option>
        <option value="life">生活</option>
        <option value="fun">趣味</option>
        <option value="other">其他</option>
      </select>
      <input v-model="keyword" placeholder="搜索..." @keyup.enter="loadPosts" />
      <select v-model="sortBy" @change="loadPosts">
        <option value="publishTime">最新发布</option>
        <option value="hot">最热</option>
      </select>
    </div>

    <div class="card" v-if="showCreate">
      <h3>发布新动态</h3>
      <div class="form-group">
        <select v-model="newPost.category" style="width:auto;">
          <option value="other">其他</option>
          <option value="emotion">情感</option>
          <option value="study">学习</option>
          <option value="life">生活</option>
          <option value="fun">趣味</option>
        </select>
      </div>
      <div class="form-group">
        <textarea v-model="newPost.content" placeholder="说点什么吧..." maxlength="500"></textarea>
      </div>
      <label><input type="checkbox" v-model="newPost.anonymous" /> 匿名发布</label>
      <div style="margin-top:12px;">
        <button class="btn" @click="createPost" :disabled="!newPost.content">发布</button>
        <button class="btn-sm" style="margin-left:8px;" @click="showCreate = false">取消</button>
      </div>
    </div>

    <div class="card">
      <div v-if="posts.length === 0" class="empty-state">暂无动态</div>
      <div v-for="post in posts" :key="post.postId" class="post-item" @click="$router.push(`/treehole/${post.postId}`)">
        <div class="post-header">
          <span class="post-user">{{ post.anonymousName }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-footer">
          <span>❤ {{ post.likeCount }}</span>
          <span>💬 {{ post.commentCount }}</span>
          <span class="tag">{{ post.category }}</span>
        </div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page <= 1" @click="page--; loadPosts()">上一页</button>
        <button class="active">{{ page }}</button>
        <button :disabled="page >= totalPages" @click="page++; loadPosts()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const posts = ref([])
const page = ref(1)
const totalPages = ref(1)
const category = ref('')
const keyword = ref('')
const sortBy = ref('publishTime')
const showCreate = ref(false)
const newPost = ref({ content: '', category: 'other', anonymous: true })

async function loadPosts() {
  try {
    const res = await api.get('/treehole/posts', {
      params: { page: page.value, size: 20, category: category.value || undefined, keyword: keyword.value || undefined, sortBy: sortBy.value }
    })
    posts.value = res.data.data.content || []
    totalPages.value = res.data.data.totalPages || 1
  } catch (e) { posts.value = [] }
}

async function createPost() {
  try {
    await api.post('/treehole/posts', newPost.value)
    showCreate.value = false
    newPost.value = { content: '', category: 'other', anonymous: true }
    loadPosts()
  } catch (e) { alert(e.response?.data?.message || '发布失败') }
}

onMounted(loadPosts)
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>
