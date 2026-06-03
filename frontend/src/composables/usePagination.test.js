import { describe, expect, it, vi } from 'vitest'
import { usePagination } from './usePagination'

describe('usePagination', () => {
  it('loads page content and page metadata', async () => {
    const loader = vi.fn().mockResolvedValue({
      data: { data: { content: [{ id: 1 }], totalPages: 4 } }
    })
    const pagination = usePagination(loader)

    await pagination.load()

    expect(loader).toHaveBeenCalledWith({ page: 1, size: 20 })
    expect(pagination.items.value).toEqual([{ id: 1 }])
    expect(pagination.totalPages.value).toBe(4)
  })

  it('resets to the first page before loading', async () => {
    const loader = vi.fn().mockResolvedValue({
      data: { data: { content: [], totalPages: 2 } }
    })
    const pagination = usePagination(loader)

    await pagination.setPage(2)
    await pagination.reset({ keyword: 'test' })

    expect(pagination.page.value).toBe(1)
    expect(loader).toHaveBeenLastCalledWith({ page: 1, size: 20, keyword: 'test' })
  })
})
