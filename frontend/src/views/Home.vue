<template>
  <main class="home-page premium-home">
    <section class="home-hero hero-panel glass-surface" :style="{ '--home-hero-image': `url(${heroCampus})` }">
      <div class="hero-content">
        <p class="home-overline">{{ authStore.isLoggedIn ? `欢迎回来，${authStore.user?.username || '同学'}` : '欢迎来到青隅' }}</p>
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
          <dl v-if="authStore.isLoggedIn" class="campus-stats">
            <div v-for="stat in campusStats" :key="stat.label">
              <dt>{{ stat.value }}</dt>
              <dd>{{ stat.label }}</dd>
            </div>
          </dl>
          <div v-else class="guest-action-panel" aria-label="登录注册入口">
            <button class="guest-login-btn" type="button" @click="router.push('/login')">登录</button>
            <button class="guest-register-btn" type="button" @click="router.push('/register')">注册</button>
          </div>
        </div>
      </aside>
    </section>

    <div class="quick-entry-heading">
      <span class="butterfly-mark" aria-hidden="true"></span>
      <h2>快速入口</h2>
    </div>
    <section class="home-command-grid">
      <router-link
        v-for="entry in quickEntries"
        :key="entry.title"
        :to="entry.to"
        :class="['command-card', entry.type]"
      >
        <div class="feature-icon">{{ entry.icon }}</div>
        <div>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.description }}</p>
        </div>
        <span class="command-link">{{ entry.action }}</span>
      </router-link>
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
        <div v-else class="feed-list">
          <article
            v-for="post in displayedPosts"
            :key="post.postId"
            class="feed-item glass-mini-card"
            @click="openPost(post)"
          >
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
            <li v-for="(topic, index) in hotTopics" :key="topic.name">
              <span>{{ index + 1 }}</span>
              <strong>{{ topic.name }}</strong>
              <em>{{ topic.heat }} 热度</em>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import api, { unwrapData, unwrapPage } from '../api'
import { useAsyncState } from '../composables/useAsyncState'
import heroCampus from '../assets/home/hero-campus.png'

const authStore = useAuthStore()
const router = useRouter()
const posts = ref([])
const profileStats = ref({
  treeHoleCount: 0,
  receivedLikes: 0,
  receivedComments: 0,
  unreadNotifications: 0
})
const { loading: feedLoading, error: feedError, run } = useAsyncState('最新动态加载失败')

const quickEntries = [
  {
    type: 'tree',
    icon: '树',
    title: '树洞',
    description: '匿名表达，被理解的温柔角落',
    action: '进入树洞',
    to: '/treehole'
  },
  {
    type: 'partner',
    icon: '搭',
    title: '找搭子',
    description: '同频结伴，一起做喜欢的事',
    action: '查看搭子',
    to: '/partner'
  },
  {
    type: 'love',
    icon: '恋',
    title: '恋爱交友',
    description: '心动互助，遇见特别的 Ta',
    action: '去看看',
    to: '/love'
  },
  {
    type: 'notice',
    icon: '知',
    title: '通知中心',
    description: '系统提醒，不错过重要信息',
    action: '查看通知',
    to: '/notifications'
  }
]

// 首页本地兜底展示数据：接口无数据或开发环境未启动后端时使用，后续可替换为真实首页聚合接口。
const mockLatestPosts = [
  {
    postId: 'mock-1',
    anonymousName: '匿名小熊',
    createdAt: new Date().toISOString(),
    content: '今天傍晚操场的云太好看了，想把这份轻松分享给也在赶作业的你。',
    likeCount: 86,
    commentCount: 23,
    category: '校园碎片',
    mock: true
  },
  {
    postId: 'mock-2',
    anonymousName: '星星同学',
    createdAt: new Date(Date.now() - 1000 * 60 * 38).toISOString(),
    content: '图书馆三楼靠窗的位置很适合复习，想找一个晚自习搭子互相监督。',
    likeCount: 52,
    commentCount: 12,
    category: '找搭子',
    mock: true
  },
  {
    postId: 'mock-3',
    anonymousName: '匿名小友',
    createdAt: new Date(Date.now() - 1000 * 60 * 90).toISOString(),
    content: '被朋友认真听完碎碎念的瞬间，突然觉得今天也没有那么难。',
    likeCount: 41,
    commentCount: 9,
    category: '树洞',
    mock: true
  }
]

const hotTopics = [
  { name: '# 考研搭子互助中', heat: 1268 },
  { name: '# 树洞里的那些事', heat: 998 },
  { name: '# 今天的校园碎片', heat: 856 },
  { name: '# 期末周自救计划', heat: 742 },
  { name: '# 晚风里的告白', heat: 639 }
]

const displayedPosts = computed(() => posts.value.length ? posts.value.slice(0, 3) : mockLatestPosts)
const campusStats = computed(() => {
  const stats = profileStats.value

  return [
    { label: '我的树洞', value: stats.treeHoleCount || 0 },
    { label: '收到点赞', value: stats.receivedLikes || 0 },
    { label: '收到评论', value: stats.receivedComments || 0 },
    { label: '未读通知', value: stats.unreadNotifications ?? authStore.unreadCount ?? 0 }
  ]
})

onMounted(async () => {
  await run(async () => {
    const res = await api.get('/treehole/posts', { params: { size: 5 } })
    const page = unwrapPage(res)
    posts.value = page.content
  })

  if (authStore.isLoggedIn) {
    try {
      const res = await api.get('/profile/stats')
      profileStats.value = unwrapData(res) || profileStats.value
      authStore.unreadCount = profileStats.value.unreadNotifications || 0
    } catch (e) {
      profileStats.value.unreadNotifications = authStore.unreadCount || 0
    }
  }
})

function formatTime(t) {
  return t ? new Date(t).toLocaleDateString('zh-CN') : ''
}

function openPost(post) {
  if (post.mock) {
    router.push('/treehole')
    return
  }

  router.push(`/treehole/${post.postId}`)
}
</script>
