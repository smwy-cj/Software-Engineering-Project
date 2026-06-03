import { reactive } from 'vue'

const toasts = reactive([])
let nextId = 1

function pushToast(type, message, timeout = 3200) {
  if (!message) return null
  const id = nextId++
  toasts.push({ id, type, message })
  window.setTimeout(() => removeToast(id), timeout)
  return id
}

export function removeToast(id) {
  const index = toasts.findIndex(item => item.id === id)
  if (index >= 0) toasts.splice(index, 1)
}

export function useToast() {
  return {
    toasts,
    success: message => pushToast('success', message),
    error: message => pushToast('error', message, 4200),
    info: message => pushToast('info', message)
  }
}
