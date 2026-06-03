import { describe, expect, it } from 'vitest'
import { getRoleFromToken, isTokenExpired, parseJwtPayload } from './jwt'

function createToken(payload) {
  const encode = value => btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.`
}

describe('jwt utilities', () => {
  it('parses role from a token payload', () => {
    const token = createToken({ role: 'ADMIN', exp: Math.floor(Date.now() / 1000) + 3600 })

    expect(parseJwtPayload(token).role).toBe('ADMIN')
    expect(getRoleFromToken(token)).toBe('ADMIN')
  })

  it('falls back safely for malformed tokens', () => {
    expect(parseJwtPayload('bad-token')).toBeNull()
    expect(getRoleFromToken('bad-token')).toBe('USER')
  })

  it('detects expired tokens', () => {
    const token = createToken({ exp: Math.floor(Date.now() / 1000) - 60 })

    expect(isTokenExpired(token)).toBe(true)
  })
})
