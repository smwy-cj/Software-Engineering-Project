import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js'
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true
      }
    }
  }
})
