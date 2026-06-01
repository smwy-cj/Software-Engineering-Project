<template>
  <main class="home-page premium-home">
    <section class="home-hero hero-panel glass-surface">
      <div class="hero-content">
        <p class="home-overline">{{ authStore.isLoggedIn ? `欢迎回来，${authStore.user?.username || '同学'}` : '欢迎来到青隅' }}</p>
        <h1 class="hero-title">今天也在校园里遇见一点温柔</h1>
        <p class="hero-description">青隅把匿名倾诉、同频搭子、恋爱交友和消息提醒整理成一个清晰、温暖、可信赖的校园服务中枢。</p>
        <div class="hero-actions">
          <template v-if="authStore.isLoggedIn">
            <router-link to="/treehole" class="glass-button-primary">进入树洞</router-link>
            <router-link to="/partner/create" class="glass-button-secondary">发布搭子</router-link>
          </template>
          <template v-else>
            <router-link to="/login" class="glass-button-primary">登录</router-link>
            <router-link to="/register" class="glass-button-secondary">注册</router-link>
          </template>
        </div>
      </div>

      <aside class="campus-brand-panel" aria-label="青隅 CampusHub 欢迎卡">
        <div class="brand-aura brand-aura-a"></div>
        <div class="brand-aura brand-aura-b"></div>
        <div class="brand-orbit brand-orbit-a"></div>
        <div class="brand-orbit brand-orbit-b"></div>
        <div class="brand-thread brand-thread-a"></div>
        <div class="brand-thread brand-thread-b"></div>
        <div class="brand-card-content">
          <span class="brand-seal">青</span>
          <p class="brand-welcome">欢迎来到青隅</p>
          <h2>
            青隅
            <span>CampusHub</span>
          </h2>
          <p class="brand-copy">一个为校园日常留出温柔角落的服务中枢，让倾诉、相遇与回应都有清晰而安心的去处。</p>
          <div class="brand-signature">愿每一次靠近，都被认真接住</div>
        </div>
      </aside>
    </section>

    <section class="home-command-grid">
      <article class="command-card tree">
        <div class="feature-icon">树</div>
        <div>
          <h3>今日树洞</h3>
          <p>匿名表达被整理成更好阅读的温柔内容流。</p>
        </div>
        <router-link to="/treehole" class="command-link">进入树洞</router-link>
      </article>
      <article class="command-card partner">
        <div class="feature-icon">搭</div>
        <div>
          <h3>同频搭子</h3>
          <p>用类型、时间、人数和状态快速判断是否合适。</p>
        </div>
        <router-link to="/partner" class="command-link">查看搭子</router-link>
      </article>
      <article class="command-card love">
        <div class="feature-icon">遇</div>
        <div>
          <h3>恋爱交友</h3>
          <p>查看同校交友需求，用真诚表达开启低压力认识。</p>
        </div>
        <router-link to="/love" class="command-link">去看看</router-link>
      </article>
    </section>

    <section class="home-content-grid">
      <section class="feed-section glass-surface">
        <div class="section-heading">
          <div>
            <h2>最新树洞动态</h2>
            <p>来自校园角落的新鲜回应。</p>
          </div>
          <router-link to="/treehole" class="glass-button-secondary">查看全部</router-link>
        </div>
        <div v-if="posts.length === 0" class="empty-state empty-treehole">这里还很安静，写下第一条校园心情吧。</div>
        <div v-else class="feed-list">
          <article v-for="post in posts" :key="post.postId" class="feed-item glass-mini-card" @click="$router.push(`/treehole/${post.postId}`)">
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
        </div>
      </section>

      <aside class="home-side-stack">
        <section class="side-panel glass-surface">
          <div class="section-heading compact">
            <div>
              <h2>未读通知</h2>
              <p>重要消息先看见。</p>
            </div>
            <strong>{{ authStore.unreadCount || 0 }}</strong>
          </div>
          <div class="mini-timeline">
            <span></span>
            <p>搭子申请、审核反馈和互动消息会在这里集中提醒。</p>
          </div>
        </section>
        <section class="side-panel glass-surface">
          <div class="section-heading compact">
            <div>
              <h2>同频搭子</h2>
              <p>今天适合发起一个轻量邀约。</p>
            </div>
          </div>
          <router-link to="/partner/create" class="glass-button-primary side-action">发布搭子</router-link>
        </section>
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

onMounted(async () => {
  try {
    const res = await api.get('/treehole/posts', { params: { size: 5 } })
    posts.value = res.data.data.content || []
  } catch (e) { /* silently fail for unauthenticated */ }
})

function formatTime(t) {
  return t ? new Date(t).toLocaleDateString('zh-CN') : ''
}
</script>
