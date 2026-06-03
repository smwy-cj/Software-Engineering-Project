import { describe, expect, it } from 'vitest'
import { getApiErrorMessage, unwrapData, unwrapPage } from './index'

describe('api helpers', () => {
  it('unwraps normal data payloads', () => {
    expect(unwrapData({ data: { data: { ok: true } } })).toEqual({ ok: true })
  })

  it('normalizes page payloads', () => {
    const page = unwrapPage({ data: { data: { content: [{ id: 1 }], totalPages: 3, unreadCount: 2 } } })

    expect(page.content).toEqual([{ id: 1 }])
    expect(page.totalPages).toBe(3)
    expect(page.unreadCount).toBe(2)
  })

  it('maps common request errors to user-facing messages', () => {
    expect(getApiErrorMessage({ response: { data: { message: '权限不足' } } }, '失败')).toBe('权限不足')
    expect(getApiErrorMessage({ code: 'ECONNABORTED' }, '失败')).toBe('请求超时，请稍后再试')
    expect(getApiErrorMessage({ request: {} }, '失败')).toBe('网络连接异常，请检查后重试')
  })
})
