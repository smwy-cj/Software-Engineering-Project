import { describe, expect, it } from 'vitest'
import { readJson, writeJson } from './storage'

describe('storage utilities', () => {
  it('reads and writes JSON values', () => {
    writeJson('user', { username: '青隅' })

    expect(readJson('user')).toEqual({ username: '青隅' })
  })

  it('removes invalid JSON and returns fallback', () => {
    localStorage.setItem('user', '{bad-json')

    expect(readJson('user', null)).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
