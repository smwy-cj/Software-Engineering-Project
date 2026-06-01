<template>
  <main class="business-page treehole-page glass-page">
    <section class="business-hero glass-surface">
      <div>
        <span class="hero-kicker">匿名树洞</span>
        <h1>把心事放进青隅</h1>
        <p>轻轻说出来，校园会给你温柔的回应。这里像一张匿名情绪便签，安全、柔软，也有人认真看见。</p>
      </div>
      <aside class="business-side-card glass-mini-card">
        <span class="glass-tag">今日树洞</span>
        <strong>{{ posts.length }}</strong>
        <p>条最新动态正在被看见</p>
      </aside>
    </section>

    <section class="treehole-layout">
      <div class="treehole-main">
        <section class="composer-panel glass-surface">
          <div class="composer-head">
            <div>
              <h2>发布新动态</h2>
              <p>默认匿名发布，保留一点安全感。</p>
            </div>
            <button v-if="authStore.isLoggedIn && !showCreate" class="glass-button-secondary" @click="showCreate = true">展开发布框</button>
          </div>

          <div v-if="showCreate" class="composer-body">
            <div class="composer-tools">
              <select class="glass-input compact-input" v-model="newPost.category">
                <option value="other">其他</option>
                <option value="emotion">情感</option>
                <option value="study">学习</option>
                <option value="life">生活</option>
                <option value="fun">趣味</option>
              </select>
              <label class="glass-check">
                <input type="checkbox" v-model="newPost.anonymous" />
                <span>匿名发布</span>
              </label>
            </div>
            <textarea class="glass-input composer-textarea" v-model="newPost.content" placeholder="说点什么吧..." maxlength="500"></textarea>
            <div class="composer-actions">
              <span class="helper-text">最多 500 字，适合写下此刻真实的心情。</span>
              <div>
                <button class="glass-button-primary" @click="createPost" :disabled="!newPost.content">发布</button>
                <button class="glass-button-secondary" @click="showCreate = false">取消</button>
              </div>
            </div>
          </div>
        </section>

        <section class="filter-bar glass-surface">
          <select class="glass-input" v-model="category" @change="loadPosts">
            <option value="">全部分类</option>
            <option value="emotion">情感</option>
            <option value="study">学习</option>
            <option value="life">生活</option>
            <option value="fun">趣味</option>
            <option value="other">其他</option>
          </select>
          <input class="glass-input" v-model="keyword" placeholder="搜索树洞内容..." @keyup.enter="loadPosts" />
          <select class="glass-input" v-model="sortBy" @change="loadPosts">
            <option value="publishTime">最新发布</option>
            <option value="hot">最热</option>
          </select>
        </section>

        <section class="feed-list">
          <div v-if="posts.length === 0" class="empty-state empty-treehole glass-surface">这里还很安静，写下第一条校园心情吧。</div>
          <article v-for="post in posts" :key="post.postId" class="feed-item treehole-post glass-mini-card" @click="$router.push(`/treehole/${post.postId}`)">
            <div class="feed-header">
              <div class="feed-avatar"></div>
              <div>
                <div class="feed-name">{{ post.anonymousName || '匿名小友' }}</div>
                <div class="feed-date">{{ formatTime(post.createdAt) }}</div>
              </div>
            </div>
            <p class="feed-content">{{ post.content }}</p>
            <div class="feed-meta">
              <span class="metric-pill">喜欢 {{ post.likeCount }}</span>
              <span class="metric-pill">评论 {{ post.commentCount }}</span>
              <span class="glass-tag">{{ post.category }}</span>
            </div>
          </article>

          <div class="pagination glass-surface" v-if="totalPages > 1">
            <button class="glass-button-secondary" :disabled="page <= 1" @click="page--; loadPosts()">上一页</button>
            <button class="glass-button-primary">{{ page }}</button>
            <button class="glass-button-secondary" :disabled="page >= totalPages" @click="page++; loadPosts()">下一页</button>
          </div>
        </section>
      </div>

      <aside class="business-aside">
        <div class="glass-mini-card insight-card">
          <span class="glass-tag">热门标签</span>
          <div class="tag-cloud">
            <span class="glass-tag">情感</span>
            <span class="glass-tag">学习</span>
            <span class="glass-tag">生活</span>
            <span class="glass-tag">趣味</span>
          </div>
        </div>
        <div class="glass-mini-card insight-card">
          <span class="glass-tag">温柔提示</span>
          <p>如果今天很累，可以先写一句话。被理解常常从一句话开始。</p>
        </div>
      </aside>
    </section>
  </main>
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
