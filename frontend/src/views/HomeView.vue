<template>
  <div class="home">
    <header class="topbar">
      <div class="brand" @click="$router.push('/')">
        <div class="mark" aria-hidden="true"></div>
        <div class="brand-text">
          <div class="brand-title">Photo Salon</div>
          <div class="brand-sub">Works, studies, fragments</div>
        </div>
      </div>

      <nav class="nav">
        <span v-if="authed" class="user">{{ username }}</span>
        <button v-if="canManage" class="btn btn-ghost" @click="$router.push('/photos')">Manage</button>
        <button v-if="authed" class="btn btn-ghost" @click="$router.push('/chat')">Chat</button>
        <button v-if="authed" class="btn btn-primary" @click="logout">Logout</button>
        <button v-else class="btn btn-primary" @click="$router.push('/login')">Login</button>
      </nav>
    </header>

    <main class="main">
      <section class="hero">
        <div class="hero-left">
          <h1 class="hero-title">A quiet wall for your images.</h1>
          <p class="hero-desc">
            A gallery-first homepage inspired by artwork walls: clean spacing, generous type, and a soft backdrop that
            lets photos breathe.
          </p>

          <div v-if="authed" class="search-row">
            <input
              v-model.trim="filters.q"
              class="input"
              type="search"
              placeholder="Search title, category, tag"
              @keydown.enter.prevent="applyFilters"
            />
            <button class="btn btn-primary" :disabled="loading" @click="applyFilters">
              {{ loading ? 'Searching...' : 'Search' }}
            </button>
          </div>

          <div v-if="authed" class="filter-row">
            <input
              v-model.trim="filters.category"
              class="input input-sm"
              type="text"
              placeholder="Category"
              @keydown.enter.prevent="applyFilters"
            />
            <input
              v-model.trim="filters.tag"
              class="input input-sm"
              type="text"
              placeholder="Tag"
              @keydown.enter.prevent="applyFilters"
            />
            <button class="btn btn-ghost" :disabled="loading" @click="clearFilters">Reset</button>
          </div>

          <div v-if="!authed" class="cta">
            <div class="cta-card">
              <div class="cta-title">Login to view your wall</div>
              <div class="cta-desc">This page will pull your photos from the backend and render them as a作品墙.</div>
              <button class="btn btn-primary" @click="$router.push('/login')">Go to Login</button>
            </div>
          </div>
        </div>

        <div class="hero-right">
          <button
            v-for="t in heroThumbs"
            :key="`hero-thumb-${t.slot}`"
            class="frame"
            :class="[`f${t.slot}`, { clickable: !!t.photo }]"
            type="button"
            :disabled="!t.photo"
            :aria-label="t.photo ? `Open ${t.title}` : 'Thumbnail placeholder'"
            @click="t.photo && openLightbox(t.index)"
          >
            <img
              v-if="t.photo"
              class="frame-img"
              :src="viewUrl(t.photo.id)"
              :alt="t.title"
              loading="lazy"
              decoding="async"
            />
            <div v-else class="frame-skeleton" aria-hidden="true"></div>
          </button>
        </div>
      </section>

      <section v-if="authed" class="wall">
        <div class="wall-head">
          <div class="wall-title">
            <h2>Works</h2>
            <p>
              {{ totalLabel }}
              <span v-if="filtersActive" class="muted">• filtered</span>
            </p>
          </div>
          <div class="wall-actions">
            <button class="btn btn-ghost" :disabled="loading" @click="refresh">Refresh</button>
          </div>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="message" class="message">{{ message }}</p>

        <p v-if="loading && photos.length === 0" class="muted">Loading...</p>
        <p v-else-if="!loading && photos.length === 0" class="muted">No photos found.</p>

        <div v-else class="masonry" :style="masonryStyle">
          <article v-for="(photo, idx) in photos" :key="photo.id" class="tile" @click="openLightbox(idx)">
            <img class="tile-img" :src="viewUrl(photo.id)" :alt="photo.title || photo.originalFilename" loading="lazy" />
            <div class="tile-overlay">
              <div class="tile-title">{{ photo.title || photo.originalFilename }}</div>
              <div class="tile-sub">
                <span v-if="photo.category" class="pill">{{ photo.category }}</span>
                <span v-for="tag in photo.tags || []" :key="`${photo.id}-${tag}`" class="pill pill-tag">#{{ tag }}</span>
              </div>
            </div>
          </article>
        </div>

        <div class="pager">
          <button v-if="canLoadMore" class="btn btn-primary" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? 'Loading...' : 'Load more' }}
          </button>
          <div v-else class="muted">End of list</div>
        </div>
      </section>
    </main>

    <div v-if="lightbox.open" class="lightbox" @click.self="closeLightbox">
      <div class="lightbox-panel" role="dialog" aria-modal="true">
        <button class="lb-close" aria-label="Close" @click="closeLightbox">×</button>

        <div class="lb-body">
          <div class="lb-media">
            <img
              v-if="activePhoto"
              class="lb-img"
              :src="viewUrl(activePhoto.id)"
              :alt="activePhoto.title || activePhoto.originalFilename"
            />
            <button class="lb-nav prev" aria-label="Previous" @click="prev" :disabled="photos.length <= 1">‹</button>
            <button class="lb-nav next" aria-label="Next" @click="next" :disabled="photos.length <= 1">›</button>
          </div>

          <aside class="lb-aside">
            <div class="lb-title">{{ (activePhoto && (activePhoto.title || activePhoto.originalFilename)) || '' }}</div>
            <div class="lb-meta">
              <div v-if="activePhoto && activePhoto.category" class="meta-row">
                <span class="meta-k">Category</span>
                <span class="meta-v">{{ activePhoto && activePhoto.category }}</span>
              </div>
              <div v-if="activePhoto && (activePhoto.tags || []).length" class="meta-row">
                <span class="meta-k">Tags</span>
                <span class="meta-v">
                  <span
                    v-for="tag in (activePhoto && activePhoto.tags) || []"
                    :key="`lb-${activePhoto.id}-${tag}`"
                    class="pill pill-tag"
                    >#{{ tag }}</span
                  >
                </span>
              </div>
              <div v-if="activePhoto && activePhoto.createdAt" class="meta-row">
                <span class="meta-k">Created</span>
                <span class="meta-v">{{ formatDate(activePhoto && activePhoto.createdAt) }}</span>
              </div>
              <div v-if="activePhoto && activePhoto.size" class="meta-row">
                <span class="meta-k">Size</span>
                <span class="meta-v">{{ formatSize(activePhoto && activePhoto.size) }}</span>
              </div>
            </div>

            <div class="lb-actions">
              <a v-if="activePhoto" class="btn btn-ghost" :href="viewUrl(activePhoto.id)" target="_blank" rel="noopener"
                >Open</a
              >
              <a
                v-if="activePhoto"
                class="btn btn-primary"
                :href="downloadUrl(activePhoto.id)"
                target="_blank"
                rel="noopener"
                >Download</a
              >
            </div>

            <div class="lb-foot muted">
              {{ lightbox.index + 1 }} / {{ photos.length }}
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { clearAuth, downloadUrl, getRoleCode, getToken, getUsername, listPhotos, viewUrl } from '../services/api'

export default {
  name: 'HomeView',
  data() {
    return {
      username: getUsername() || '',
      roleCode: getRoleCode(),
      photos: [],
      loading: false,
      loadingMore: false,
      error: '',
      message: '',
      page: 1,
      size: 24,
      total: 0,
      totalPages: 1,
      filters: {
        q: '',
        category: '',
        tag: ''
      },
      lightbox: {
        open: false,
        index: 0
      },
      onKeydown: null
    }
  },
  computed: {
    authed() {
      return !!getToken()
    },
    canManage() {
      // role_code=0 super, role_code=3 normal (own manage). role_code=1 read-only.
      const role = Number(this.roleCode)
      return this.authed && (role === 0 || role === 3)
    },
    canLoadMore() {
      return this.page < (this.totalPages || 1)
    },
    activePhoto() {
      return this.photos[this.lightbox.index] || null
    },
    filtersActive() {
      return !!(this.filters.q || this.filters.category || this.filters.tag)
    },
    totalLabel() {
      if (!this.total) return `${this.photos.length} items`
      return `${this.total} items`
    },
    masonryStyle() {
      // Responsive columns, CSS-only masonry (columns + break-inside).
      return {
        '--cols': '4'
      }
    },
    heroThumbs() {
      const slots = [1, 2, 3]
      const items = (this.photos || []).slice(0, 3).map((p, idx) => ({
        slot: slots[idx],
        photo: p,
        index: idx,
        title: p.title || p.originalFilename || 'Photo'
      }))
      while (items.length < 3) {
        items.push({ slot: slots[items.length], photo: null, index: -1, title: 'Photo' })
      }
      return items
    }
  },
  created() {
    if (this.authed) this.fetchFirstPage()
  },
  beforeDestroy() {
    this.detachKeydown()
  },
  methods: {
    viewUrl,
    downloadUrl,
    logout() {
      clearAuth()
      this.closeLightbox()
      this.$router.replace('/login')
    },
    clearFilters() {
      this.filters = { q: '', category: '', tag: '' }
      this.applyFilters()
    },
    refresh() {
      this.fetchFirstPage()
    },
    applyFilters() {
      this.fetchFirstPage()
    },
    async fetchFirstPage() {
      this.page = 1
      this.photos = []
      await this.fetchPhotos({ append: false })
    },
    async loadMore() {
      if (this.loadingMore || !this.canLoadMore) return
      this.page += 1
      await this.fetchPhotos({ append: true })
    },
    async fetchPhotos({ append }) {
      if (!this.authed) return
      this.error = ''
      this.message = ''
      if (append) this.loadingMore = true
      else this.loading = true

      try {
        const params = {
          page: this.page,
          size: this.size,
          q: this.filters.q || undefined,
          category: this.filters.category || undefined,
          tag: this.filters.tag || undefined
        }
        const { data } = await listPhotos(params)
        const items = data.items || []
        this.total = data.total || 0
        this.totalPages = data.totalPages || 1
        this.photos = append ? this.photos.concat(items) : items
      } catch (e) {
        this.error = e.response?.data?.message || 'Failed to load photos'
      } finally {
        this.loading = false
        this.loadingMore = false
      }
    },
    openLightbox(index) {
      this.lightbox.index = index
      this.lightbox.open = true
      this.attachKeydown()
    },
    closeLightbox() {
      this.lightbox.open = false
      this.detachKeydown()
    },
    prev() {
      if (this.photos.length === 0) return
      this.lightbox.index = (this.lightbox.index - 1 + this.photos.length) % this.photos.length
    },
    next() {
      if (this.photos.length === 0) return
      this.lightbox.index = (this.lightbox.index + 1) % this.photos.length
    },
    attachKeydown() {
      if (this.onKeydown) return
      this.onKeydown = e => {
        if (!this.lightbox.open) return
        if (e.key === 'Escape') this.closeLightbox()
        if (e.key === 'ArrowLeft') this.prev()
        if (e.key === 'ArrowRight') this.next()
      }
      window.addEventListener('keydown', this.onKeydown)
    },
    detachKeydown() {
      if (!this.onKeydown) return
      window.removeEventListener('keydown', this.onKeydown)
      this.onKeydown = null
    },
    formatSize(size) {
      if (!size && size !== 0) return ''
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / (1024 * 1024)).toFixed(1)} MB`
    },
    formatDate(dateStr) {
      try {
        return new Date(dateStr).toLocaleString()
      } catch {
        return String(dateStr || '')
      }
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  color: var(--ink);
  background:
    radial-gradient(1200px 600px at 12% 8%, rgba(248, 205, 154, 0.45), transparent 60%),
    radial-gradient(900px 520px at 84% 16%, rgba(168, 212, 255, 0.42), transparent 62%),
    radial-gradient(980px 560px at 64% 88%, rgba(202, 174, 255, 0.28), transparent 58%),
    linear-gradient(180deg, #fbfbfc, #f4f3f2);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  backdrop-filter: blur(14px);
  background: rgba(251, 251, 252, 0.68);
  border-bottom: 1px solid rgba(15, 15, 20, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 45%),
    linear-gradient(135deg, #191a1d, #34363d);
  box-shadow: 0 18px 40px rgba(15, 15, 20, 0.16);
}

.brand-title {
  font-family: var(--font-display);
  letter-spacing: 0.2px;
  font-size: 18px;
  line-height: 1.1;
}

.brand-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user {
  font-size: 13px;
  color: var(--muted);
  padding: 8px 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 15, 20, 0.1);
  background: rgba(255, 255, 255, 0.6);
}

.main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 26px 20px 60px;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 22px;
  padding: 22px 0 18px;
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(34px, 4.2vw, 56px);
  line-height: 1.02;
  margin: 0;
  letter-spacing: -0.6px;
}

.hero-desc {
  margin: 14px 0 0;
  color: var(--muted);
  max-width: 56ch;
  font-size: 15px;
  line-height: 1.7;
}

.search-row {
  margin-top: 18px;
  display: flex;
  gap: 10px;
}

.filter-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cta {
  margin-top: 18px;
}

.cta-card {
  border-radius: 18px;
  border: 1px solid rgba(15, 15, 20, 0.1);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 60px rgba(15, 15, 20, 0.08);
  padding: 16px 16px 14px;
  max-width: 420px;
}

.cta-title {
  font-weight: 650;
}

.cta-desc {
  color: var(--muted);
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.hero-right {
  position: relative;
  min-height: 260px;
}

.frame {
  position: absolute;
  padding: 0;
  border-radius: 18px;
  border: 1px solid rgba(15, 15, 20, 0.1);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.35)),
    radial-gradient(200px 120px at 30% 40%, rgba(255, 212, 166, 0.35), transparent 70%),
    radial-gradient(220px 140px at 72% 30%, rgba(176, 214, 255, 0.35), transparent 72%);
  box-shadow: 0 26px 70px rgba(15, 15, 20, 0.12);
  overflow: hidden;
  cursor: default;
  transition: transform 180ms ease, box-shadow 180ms ease, filter 180ms ease;
}

.frame.clickable {
  cursor: pointer;
}

.frame.clickable:hover {
  transform: translateY(-2px) rotate(0.2deg);
  box-shadow: 0 32px 92px rgba(15, 15, 20, 0.16);
  filter: saturate(1.03) contrast(1.02);
}

.frame:disabled {
  opacity: 0.96;
}

.frame-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transform: scale(1.02);
}

.frame-skeleton {
  width: 100%;
  height: 100%;
  background:
    linear-gradient(110deg, rgba(255, 255, 255, 0.10) 8%, rgba(255, 255, 255, 0.30) 18%, rgba(255, 255, 255, 0.10) 33%),
    radial-gradient(260px 180px at 30% 35%, rgba(255, 212, 166, 0.22), transparent 70%),
    radial-gradient(240px 160px at 72% 30%, rgba(176, 214, 255, 0.22), transparent 72%);
  background-size: 200% 100%, auto, auto;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 180% 0, 0 0, 0 0;
  }
  100% {
    background-position: -40% 0, 0 0, 0 0;
  }
}

.f1 {
  width: 78%;
  height: 68%;
  right: 2%;
  top: 0;
  transform: rotate(2.5deg);
}

.f2 {
  width: 70%;
  height: 64%;
  right: 12%;
  top: 18%;
  transform: rotate(-3.4deg);
  opacity: 0.92;
}

.f3 {
  width: 62%;
  height: 60%;
  right: 22%;
  top: 36%;
  transform: rotate(2deg);
  opacity: 0.86;
}

.wall {
  margin-top: 22px;
  border-radius: 22px;
  padding: 18px 16px 10px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(15, 15, 20, 0.09);
  box-shadow: 0 22px 80px rgba(15, 15, 20, 0.08);
}

.wall-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  padding: 6px 6px 14px;
}

.wall-title h2 {
  margin: 0;
  font-family: var(--font-display);
  letter-spacing: -0.3px;
}

.wall-title p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.masonry {
  column-count: var(--cols);
  column-gap: 14px;
  padding: 4px 6px 12px;
}

.tile {
  break-inside: avoid;
  margin: 0 0 14px;
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  box-shadow: 0 16px 46px rgba(15, 15, 20, 0.10);
  transform: translateZ(0);
}

.tile-img {
  width: 100%;
  height: auto;
  display: block;
  filter: saturate(1.02) contrast(1.02);
  transform: scale(1.001);
  transition: transform 240ms ease, filter 240ms ease;
}

.tile-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 12px 11px;
  background: linear-gradient(180deg, transparent, rgba(10, 10, 12, 0.66));
  color: rgba(255, 255, 255, 0.96);
  opacity: 0;
  transform: translateY(6px);
  transition: opacity 220ms ease, transform 220ms ease;
}

.tile-title {
  font-weight: 650;
  letter-spacing: 0.2px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

.tile-sub {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tile:hover .tile-img {
  transform: scale(1.02);
  filter: saturate(1.06) contrast(1.05);
}

.tile:hover .tile-overlay {
  opacity: 1;
  transform: translateY(0);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0 18px;
}

.lightbox {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(10, 10, 12, 0.62);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
}

.lightbox-panel {
  width: min(1180px, 100%);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 50px 160px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  position: relative;
}

.lb-close {
  position: absolute;
  right: 14px;
  top: 12px;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(15, 15, 20, 0.14);
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.lb-body {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr;
  min-height: min(78vh, 720px);
}

.lb-media {
  position: relative;
  background: linear-gradient(180deg, rgba(15, 15, 20, 0.06), rgba(15, 15, 20, 0.02));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.lb-img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 16px;
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.18);
}

.lb-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  width: 42px;
  height: 42px;
  border-radius: 999px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(15, 15, 20, 0.14);
  font-size: 24px;
  line-height: 1;
}

.lb-nav:disabled {
  opacity: 0.45;
  cursor: default;
}

.lb-nav.prev {
  left: 14px;
}

.lb-nav.next {
  right: 14px;
}

.lb-aside {
  padding: 18px 18px 16px;
  border-left: 1px solid rgba(15, 15, 20, 0.08);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.lb-title {
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1.15;
  letter-spacing: -0.2px;
}

.lb-meta {
  display: grid;
  gap: 10px;
}

.meta-row {
  display: grid;
  grid-template-columns: 82px 1fr;
  gap: 10px;
  align-items: start;
  font-size: 13px;
}

.meta-k {
  color: var(--muted);
}

.meta-v {
  color: var(--ink);
}

.lb-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.lb-foot {
  margin-top: auto;
}

.input {
  width: 100%;
  padding: 12px 12px;
  border-radius: 14px;
  border: 1px solid rgba(15, 15, 20, 0.12);
  background: rgba(255, 255, 255, 0.74);
  outline: none;
  font: inherit;
  box-shadow: 0 10px 30px rgba(15, 15, 20, 0.06);
}

.input:focus {
  border-color: rgba(34, 88, 210, 0.34);
  box-shadow: 0 14px 40px rgba(34, 88, 210, 0.10);
}

.input-sm {
  padding: 10px 12px;
  border-radius: 12px;
  width: min(240px, 100%);
}

.btn {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  cursor: pointer;
  font: inherit;
  font-weight: 650;
  letter-spacing: 0.2px;
  transition: transform 140ms ease, box-shadow 140ms ease, background 140ms ease, opacity 140ms ease;
}

.btn:active {
  transform: translateY(1px);
}

.btn-primary {
  background: linear-gradient(135deg, #1a1b1f, #34363d);
  color: #fff;
  box-shadow: 0 18px 46px rgba(15, 15, 20, 0.18);
}

.btn-primary:hover {
  box-shadow: 0 20px 60px rgba(15, 15, 20, 0.22);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: default;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(15, 15, 20, 0.12);
  color: var(--ink);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 10px 30px rgba(15, 15, 20, 0.08);
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.22);
  font-size: 12px;
  line-height: 1;
  backdrop-filter: blur(8px);
}

.pill-tag {
  background: rgba(255, 255, 255, 0.14);
}

.muted {
  color: var(--muted);
}

.error {
  margin: 0 6px 10px;
  color: #b91c1c;
  font-size: 13px;
}

.message {
  margin: 0 6px 10px;
  color: #0f766e;
  font-size: 13px;
}

@media (max-width: 980px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .hero-right {
    display: none;
  }
  .masonry {
    column-count: 3;
  }
  .lb-body {
    grid-template-columns: 1fr;
  }
  .lb-aside {
    border-left: none;
    border-top: 1px solid rgba(15, 15, 20, 0.08);
  }
}

@media (max-width: 640px) {
  .topbar {
    padding: 14px 14px;
  }
  .main {
    padding: 18px 14px 52px;
  }
  .masonry {
    column-count: 2;
  }
  .search-row {
    flex-direction: column;
  }
}
</style>
