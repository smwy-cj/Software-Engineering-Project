function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
  return decodeURIComponent(
    atob(padded)
      .split('')
      .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join('')
  )
}

export function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const [, payload] = token.split('.')
  if (!payload) return null

  try {
    return JSON.parse(decodeBase64Url(payload))
  } catch {
    return null
  }
}

export function getRoleFromToken(token) {
  return parseJwtPayload(token)?.role || 'USER'
}

export function isTokenExpired(token, clockSkewSeconds = 30) {
  const exp = parseJwtPayload(token)?.exp
  if (!exp) return false
  return exp * 1000 <= Date.now() + clockSkewSeconds * 1000
}
