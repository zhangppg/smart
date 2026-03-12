<template>
  <div class="app-shell">
    <router-view />

    <transition name="toast-fade">
      <div v-if="toast.open" class="toast" role="status" aria-live="polite">
        <span class="toast-dot" aria-hidden="true"></span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      toast: {
        open: false,
        message: ''
      },
      toastTimer: null,
      onToastEvent: null
    }
  },
  mounted() {
    this.onToastEvent = e => {
      const msg = e?.detail?.message || ''
      const durationMs = Number(e?.detail?.durationMs || 2000)
      if (!msg) return

      this.toast.message = msg
      this.toast.open = true

      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => {
        this.toast.open = false
      }, Math.max(300, durationMs))
    }
    window.addEventListener('app-toast', this.onToastEvent)
  },
  beforeDestroy() {
    if (this.onToastEvent) window.removeEventListener('app-toast', this.onToastEvent)
    if (this.toastTimer) clearTimeout(this.toastTimer)
  }
}
</script>

<style>
.app-shell {
  min-height: 100vh;
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  z-index: 9999;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.96);
  background: rgba(20, 20, 24, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(10px);
  max-width: min(560px, calc(100vw - 24px));
}

.toast-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ffbd7a, #a9d3ff);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.06);
  flex: 0 0 auto;
}

.toast-text {
  font-size: 13px;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.toast-fade-enter,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}
</style>
