import { ref } from 'vue'
import { unwrapPage } from '../api'
import { useAsyncState } from './useAsyncState'

export function usePagination(loader, options = {}) {
  const items = ref([])
  const page = ref(options.initialPage || 1)
  const totalPages = ref(1)
  const pageSize = ref(options.pageSize || 20)
  const { loading, error, run } = useAsyncState(options.errorMessage || '列表加载失败')

  async function load(extraParams = {}) {
    const result = await run(async () => {
      const response = await loader({
        page: page.value,
        size: pageSize.value,
        ...extraParams
      })
      return unwrapPage(response)
    })

    if (!result) {
      items.value = []
      totalPages.value = 1
      return null
    }

    items.value = result.content
    totalPages.value = result.totalPages
    return result
  }

  async function setPage(nextPage, extraParams = {}) {
    const normalized = Math.min(Math.max(1, nextPage), totalPages.value || 1)
    if (normalized === page.value && items.value.length) return null
    page.value = normalized
    return load(extraParams)
  }

  async function reset(extraParams = {}) {
    page.value = 1
    return load(extraParams)
  }

  return { items, page, totalPages, pageSize, loading, error, load, setPage, reset }
}
