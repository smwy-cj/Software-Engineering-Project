export function readJson(key, fallback = null) {
  const raw = localStorage.getItem(key)
  if (!raw) return fallback

  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(key)
    return fallback
  }
}

export function writeJson(key, value) {
  if (value == null) {
    localStorage.removeItem(key)
    return
  }

  localStorage.setItem(key, JSON.stringify(value))
}
