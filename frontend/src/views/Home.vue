<template>
  <main class="home-page premium-home">
    <section class="home-hero hero-panel glass-surface">
      <div class="hero-content">
        <p class="home-overline">{{ authStore.isLoggedIn ? `欢迎回来，${authStore.user?.username || '同学'}` : '欢迎来到青隅' }}</p>
        <h1 class="hero-title">今天也在校园里<br>遇见一点温柔</h1>
        <p class="hero-description">青隅把匿名倾诉、同频搭子、恋爱交友<br>和消息提醒整理成一个清晰、温暖、可信赖的<br>校园服务中枢。</p>
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

      <aside class="campus-portrait-panel" aria-label="青隅 CampusHub 欢迎卡">
        <div class="wisteria-vine vine-left"></div>
        <div class="wisteria-vine vine-right"></div>
        <div class="portrait-card">
          <span class="brand-seal">青</span>
          <p class="brand-welcome">欢迎来到青隅</p>
          <h2>
            青隅
            <span>CampusHub</span>
          </h2>
          <p class="brand-copy">一个为校园日常留出温柔角落的服务中枢，让倾诉、相遇与回应都有清晰而安心的去处。</p>
          <div class="brand-signature">愿每一次靠近，都被认真接住</div>
          <dl class="campus-stats">
            <div>
              <dt>128</dt>
              <dd>获得点赞</dd>
            </div>
            <div>
              <dt>36</dt>
              <dd>收到评论</dd>
            </div>
            <div>
              <dt>15</dt>
              <dd>收藏动态</dd>
            </div>
            <div>
              <dt>42</dt>
              <dd>互相关注</dd>
            </div>
          </dl>
        </div>
      </aside>
    </section>

    <div class="quick-entry-heading">
      <span class="butterfly-mark" aria-hidden="true"></span>
      <h2>快速入口</h2>
    </div>
    <section class="home-command-grid">
      <article class="command-card tree">
        <div class="feature-icon jar-icon">树</div>
        <div>
          <h3>今日树洞</h3>
          <p>匿名表达被整理成更好阅读的温柔内容流。</p>
        </div>
        <router-link to="/treehole" class="command-link">进入树洞</router-link>
      </article>
      <article class="command-card partner">
        <div class="feature-icon plane-icon">搭</div>
        <div>
          <h3>同频搭子</h3>
          <p>用类型、时间、人数和状态快速判断是否合适。</p>
        </div>
        <router-link to="/partner" class="command-link">查看搭子</router-link>
      </article>
      <article class="command-card love">
        <div class="feature-icon heart-icon">遇</div>
        <div>
          <h3>恋爱交友</h3>
          <p>查看同校交友需求，用真诚表达开启低压力认识。</p>
        </div>
        <router-link to="/love" class="command-link">去看看</router-link>
      </article>
      <article class="command-card notice">
        <div class="feature-icon bell-icon">知</div>
        <div>
          <h3>通知中心</h3>
          <p>系统提醒、互动消息和重要反馈集中查看。</p>
        </div>
        <router-link to="/notifications" class="command-link">查看通知</router-link>
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
        <p v-if="feedError" class="form-error">{{ feedError }}</p>
        <div v-if="feedLoading" class="empty-state empty-treehole">正在加载最新动态...</div>
        <div v-else-if="posts.length === 0" class="empty-state empty-treehole">这里还很安静，写下第一条校园心情吧。</div>
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
        <section class="side-panel glass-surface hot-topic-panel">
          <div class="section-heading compact">
            <div>
              <h2>热门话题</h2>
              <p>正在被同学们讨论。</p>
            </div>
            <router-link to="/treehole" class="glass-button-secondary">查看全部</router-link>
          </div>
          <ol class="topic-list">
            <li>
              <span>1</span>
              <strong># 考研搭子互助中</strong>
              <em>1268 热度</em>
            </li>
            <li>
              <span>2</span>
              <strong># 树洞里的那些事</strong>
              <em>998 热度</em>
            </li>
            <li>
              <span>3</span>
              <strong># 今天的校园碎片</strong>
              <em>856 热度</em>
            </li>
          </ol>
        </section>
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
import api, { unwrapPage } from '../api'
import { useAsyncState } from '../composables/useAsyncState'

const authStore = useAuthStore()
const posts = ref([])
const { loading: feedLoading, error: feedError, run } = useAsyncState('最新动态加载失败')

onMounted(async () => {
  await run(async () => {
    const res = await api.get('/treehole/posts', { params: { size: 5 } })
    posts.value = unwrapPage(res).content
  })
})

function formatTime(t) {
  return t ? new Date(t).toLocaleDateString('zh-CN') : ''
}
</script>
