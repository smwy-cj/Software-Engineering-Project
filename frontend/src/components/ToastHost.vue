<template>
  <div class="toast-host" aria-live="polite" aria-atomic="true">
    <button
      v-for="toast in toasts"
      :key="toast.id"
      class="toast-message"
      :class="`toast-${toast.type}`"
      type="button"
      @click="removeToast(toast.id)"
    >
      {{ toast.message }}
    </button>
  </div>
</template>

<script setup>
import { removeToast, useToast } from '../composables/useToast'

const { toasts } = useToast()
</script>
