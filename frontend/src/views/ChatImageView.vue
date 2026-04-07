<template>
  <div class="page" :class="{ 'theme-dark': dark }">
    <header class="header">
      <div class="header-left">
        <button class="home-btn" type="button" @click="goHome" aria-label="Back to home">
          <span class="home-btn-icon" aria-hidden="true">←</span>
          <span>Home</span>
        </button>
        <div class="title-wrap">
          <h1>Chat</h1>
          <p>Send a message or upload an image.</p>
        </div>
      </div>

      <button class="theme-btn" type="button" @click="toggleTheme" :aria-label="dark ? 'Switch to light' : 'Switch to dark'">
        <span aria-hidden="true">{{ dark ? '☀︎' : '☾' }}</span>
      </button>
    </header>

    <main class="layout">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">Chat</div>
          <div class="panel-sub">{{ messages.length }} messages</div>
        </div>

        <div ref="scroller" class="messages" role="log" aria-live="polite">
          <transition-group name="msg" tag="div" class="messages-inner">
            <div
              v-for="m in messages"
              :key="m.id"
              class="bubble"
              :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'"
            >
              {{ m.text }}
            </div>
          </transition-group>
        </div>

        <form class="composer" @submit.prevent="sendMessage">
          <input
            v-model.trim="input"
            class="composer-input"
            type="text"
            placeholder="Type a message..."
            autocomplete="off"
          />
          <button class="composer-send" type="submit" :disabled="!input">Send</button>
          <label class="composer-file">
            <span>Image</span>
            <input type="file" accept="image/*" @change="handleImage" />
          </label>
        </form>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">Preview</div>
          <button v-if="imageUrl" type="button" class="link-btn" @click="clearImage">Clear</button>
        </div>

        <div class="preview">
          <transition name="preview">
            <img v-if="imageUrl" class="preview-img" :src="imageUrl" alt="preview" />
          </transition>
          <div v-if="!imageUrl" class="preview-empty">No image selected</div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
const THEME_KEY = 'chat_image_theme_dark'

export default {
  name: 'ChatImageView',
  data() {
    return {
      messages: [{ id: 1, role: 'ai', text: 'Hello! Send me an image or message.' }],
      nextId: 2,
      input: '',
      imageUrl: '',
      dark: true
    }
  },
  mounted() {
    const v = localStorage.getItem(THEME_KEY)
    if (v === '0') this.dark = false
    if (v === '1') this.dark = true
    this.scrollToBottom()
  },
  beforeDestroy() {
    this.revokeImageUrl()
  },
  methods: {
    goHome() {
      this.$router.push('/')
    },
    toggleTheme() {
      this.dark = !this.dark
      localStorage.setItem(THEME_KEY, this.dark ? '1' : '0')
    },
    sendMessage() {
      const text = (this.input || '').trim()
      if (!text) return
      this.messages.push({ id: this.nextId++, role: 'user', text })
      this.input = ''
      this.$nextTick(this.scrollToBottom)
    },
    handleImage(e) {
      const file = e?.target?.files?.[0]
      if (!file) return
      if (file.type && !file.type.startsWith('image/')) return

      this.revokeImageUrl()
      this.imageUrl = URL.createObjectURL(file)
      this.messages.push({ id: this.nextId++, role: 'user', text: '[Image uploaded]' })
      this.$nextTick(this.scrollToBottom)

      // allow selecting the same file again
      if (e && e.target) e.target.value = ''
    },
    clearImage() {
      this.revokeImageUrl()
      this.imageUrl = ''
    },
    revokeImageUrl() {
      try {
        if (this.imageUrl) URL.revokeObjectURL(this.imageUrl)
      } catch (err) {
        // noop
      }
    },
    scrollToBottom() {
      const el = this.$refs.scroller
      if (!el) return
      el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
.page {
  --bg: #fbfbfc;
  --panel: #ffffff;
  --panel-border: rgba(15, 15, 20, 0.1);
  --ink: #15161a;
  --muted: rgba(21, 22, 26, 0.62);
  --bubble-ai: #ffffff;
  --bubble-ai-border: rgba(15, 15, 20, 0.1);
  --bubble-user: #2563eb;
  --bubble-user-ink: rgba(255, 255, 255, 0.96);
  --btn: #15161a;
  --btn-ink: rgba(255, 255, 255, 0.96);
  --btn-muted: rgba(21, 22, 26, 0.08);
  --shadow: 0 16px 60px rgba(0, 0, 0, 0.08);

  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--ink);
}

.theme-dark {
  --bg: #0b0d12;
  --panel: rgba(255, 255, 255, 0.06);
  --panel-border: rgba(255, 255, 255, 0.12);
  --ink: rgba(255, 255, 255, 0.92);
  --muted: rgba(255, 255, 255, 0.62);
  --bubble-ai: rgba(255, 255, 255, 0.08);
  --bubble-ai-border: rgba(255, 255, 255, 0.14);
  --btn: rgba(255, 255, 255, 0.9);
  --btn-ink: #0b0d12;
  --btn-muted: rgba(255, 255, 255, 0.1);
  --shadow: 0 22px 80px rgba(0, 0, 0, 0.35);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.65), rgba(255, 255, 255, 0));
}

.theme-dark .header {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.title-wrap {
  min-width: 0;
}

.title-wrap h1 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0.2px;
}

.title-wrap p {
  margin: 3px 0 0;
  font-size: 13px;
  color: var(--muted);
}

.theme-btn {
  border: 1px solid var(--panel-border);
  background: var(--panel);
  color: var(--ink);
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.06);
}

.home-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 12px;
  border: 1px solid var(--panel-border);
  background: var(--panel);
  color: var(--ink);
  cursor: pointer;
}

.home-btn-icon {
  font-size: 14px;
  line-height: 1;
  opacity: 0.82;
}

.layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 14px;
}

.panel {
  background: var(--panel);
  border: 1px solid var(--panel-border);
  border-radius: 18px;
  box-shadow: var(--shadow);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--panel-border);
}

.panel-title {
  font-weight: 700;
  letter-spacing: 0.2px;
}

.panel-sub {
  font-size: 12px;
  color: var(--muted);
}

.messages {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.messages-inner {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.35;
  word-break: break-word;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}

.bubble-ai {
  background: var(--bubble-ai);
  border: 1px solid var(--bubble-ai-border);
}

.bubble-user {
  margin-left: auto;
  background: var(--bubble-user);
  color: var(--bubble-user-ink);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.composer {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--panel-border);
  align-items: center;
}

.composer-input {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--panel-border);
  background: transparent;
  color: var(--ink);
  outline: none;
}

.composer-input::placeholder {
  color: var(--muted);
}

.composer-send {
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: var(--btn);
  color: var(--btn-ink);
  cursor: pointer;
}

.composer-send:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.composer-file {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--panel-border);
  background: var(--btn-muted);
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  color: var(--ink);
}

.composer-file input {
  display: none;
}

.preview {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.preview-img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 16px;
  box-shadow: 0 20px 70px rgba(0, 0, 0, 0.18);
}

.preview-empty {
  color: var(--muted);
  font-size: 14px;
}

.link-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
}

.msg-enter-active,
.msg-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.msg-enter,
.msg-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.preview-enter-active,
.preview-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.preview-enter,
.preview-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .panel {
    min-height: 520px;
  }
}

@media (max-width: 640px) {
  .header {
    padding: 14px 14px;
  }
  .layout {
    padding: 12px;
  }
  .composer {
    flex-wrap: wrap;
  }
  .composer-file {
    width: 100%;
    justify-content: center;
  }
}
</style>
