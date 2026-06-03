import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api'
import { useAuthStore } from './store/auth'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

setUnauthorizedHandler(() => {
  const authStore = useAuthStore(pinia)
  authStore.logout()
  if (router.currentRoute.value.path !== '/login') {
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

app.mount('#app')
