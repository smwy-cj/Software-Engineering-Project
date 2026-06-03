import { ref } from 'vue'
import { getApiErrorMessage } from '../api'

export function useAsyncState(defaultError = '操作失败') {
  const loading = ref(false)
  const error = ref('')

  async function run(task, options = {}) {
    if (loading.value && options.preventOverlap !== false) return null
    loading.value = true
    error.value = ''

    try {
      return await task()
    } catch (err) {
      error.value = getApiErrorMessage(err, options.fallback || defaultError)
      if (options.throwError) throw err
      return null
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = ''
  }

  return { loading, error, run, clearError }
}
