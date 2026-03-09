<template>
  <div class="page">
    <header class="header">
      <h1>Photo Manager</h1>
      <p>Upload, classify, search and manage your photos.</p>
    </header>

    <section class="card topbar">
      <div>
        <strong>{{ username }}</strong>
      </div>
      <button class="danger" @click="logout">Logout</button>
    </section>

    <section class="card">
      <h2>Upload Photo</h2>
      <form class="form" @submit.prevent="onUpload">
        <input v-model.trim="uploadForm.title" type="text" placeholder="Title (optional)" />
        <input v-model.trim="uploadForm.category" type="text" placeholder="Category (e.g. travel)" />
        <input v-model.trim="uploadForm.tags" type="text" placeholder="Tags (comma separated)" />
        <input ref="file" type="file" accept="image/*" required />
        <button type="submit" :disabled="uploading">{{ uploading ? 'Uploading...' : 'Upload' }}</button>
      </form>
    </section>

    <section class="card">
      <div class="gallery-header">
        <h2>My Photos</h2>
        <button @click="fetchPhotos" :disabled="loading">Refresh</button>
      </div>

      <form class="filter-form" @submit.prevent="applyFilters">
        <input v-model.trim="filters.q" type="text" placeholder="Search title/category/tag" />
        <input v-model.trim="filters.category" type="text" placeholder="Category" />
        <input v-model.trim="filters.tag" type="text" placeholder="Tag" />
        <button type="submit">Search</button>
      </form>

      <p v-if="message" class="message">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>

      <p v-if="loading">Loading photos...</p>
      <p v-else-if="photos.length === 0">No photos found.</p>

      <div v-else class="grid">
        <article v-for="photo in photos" :key="photo.id" class="photo-card">
          <img :src="viewUrl(photo.id)" :alt="photo.title" loading="lazy" />
          <div class="meta">
            <h3>{{ photo.title || photo.originalFilename }}</h3>
            <small>{{ formatSize(photo.size) }} • {{ formatDate(photo.createdAt) }}</small>
            <p class="chip-line">
              <span v-if="photo.category" class="chip">{{ photo.category }}</span>
              <span v-for="tag in photo.tags || []" :key="`${photo.id}-${tag}`" class="chip tag">#{{ tag }}</span>
            </p>
          </div>
          <div class="actions">
            <a :href="downloadUrl(photo.id)" target="_blank" rel="noopener noreferrer">Download</a>
            <div class="action-buttons">
              <button class="secondary" @click="onEdit(photo)">Edit</button>
              <button class="danger" @click="onDelete(photo.id)">Delete</button>
            </div>
          </div>
        </article>
      </div>

      <div class="pagination">
        <button @click="goPage(page - 1)" :disabled="page <= 1 || loading">Prev</button>
        <span>Page {{ page }} / {{ totalPages || 1 }} ({{ total }} items)</span>
        <button @click="goPage(page + 1)" :disabled="page >= totalPages || loading">Next</button>
      </div>
    </section>
  </div>
</template>

<script>
import {
  clearAuth,
  deletePhoto,
  downloadUrl,
  getUsername,
  listPhotos,
  updatePhoto,
  uploadPhoto,
  viewUrl
} from '../services/api'

export default {
  name: 'PhotosView',
  data() {
    return {
      username: getUsername() || '',
      uploadForm: {
        title: '',
        category: '',
        tags: ''
      },
      photos: [],
      loading: false,
      uploading: false,
      message: '',
      error: '',
      filters: {
        q: '',
        category: '',
        tag: ''
      },
      page: 1,
      size: 12,
      total: 0,
      totalPages: 1
    }
  },
  created() {
    this.fetchPhotos()
  },
  methods: {
    downloadUrl,
    viewUrl,
    logout() {
      clearAuth()
      this.$router.replace('/login')
    },
    async fetchPhotos() {
      this.loading = true
      this.error = ''
      try {
        const params = {
          page: this.page,
          size: this.size,
          q: this.filters.q || undefined,
          category: this.filters.category || undefined,
          tag: this.filters.tag || undefined
        }
        const { data } = await listPhotos(params)
        this.photos = data.items
        this.total = data.total
        this.totalPages = data.totalPages || 1
      } catch (e) {
        this.error = e.response?.data?.message || 'Failed to load photos'
      } finally {
        this.loading = false
      }
    },
    applyFilters() {
      this.page = 1
      this.fetchPhotos()
    },
    goPage(nextPage) {
      if (nextPage < 1 || nextPage > this.totalPages) return
      this.page = nextPage
      this.fetchPhotos()
    },
    async onUpload() {
      const file = this.$refs.file.files[0]
      if (!file) {
        this.error = 'Please choose a file'
        return
      }
      const form = new FormData()
      form.append('file', file)
      if (this.uploadForm.title) form.append('title', this.uploadForm.title)
      if (this.uploadForm.category) form.append('category', this.uploadForm.category)
      if (this.uploadForm.tags) form.append('tags', this.uploadForm.tags)

      this.uploading = true
      this.message = ''
      this.error = ''
      try {
        await uploadPhoto(form)
        this.message = 'Upload successful'
        this.uploadForm = { title: '', category: '', tags: '' }
        this.$refs.file.value = ''
        this.page = 1
        await this.fetchPhotos()
      } catch (e) {
        this.error = e.response?.data?.message || 'Upload failed'
      } finally {
        this.uploading = false
      }
    },
    async onEdit(photo) {
      const title = prompt('Title', photo.title || '')
      if (title === null) return
      const category = prompt('Category', photo.category || '')
      if (category === null) return
      const tags = prompt('Tags (comma separated)', (photo.tags || []).join(','))
      if (tags === null) return

      this.error = ''
      this.message = ''
      try {
        await updatePhoto(photo.id, { title, category, tags })
        this.message = 'Photo updated'
        await this.fetchPhotos()
      } catch (e) {
        this.error = e.response?.data?.message || 'Update failed'
      }
    },
    async onDelete(id) {
      if (!confirm('Delete this photo?')) return
      this.error = ''
      this.message = ''
      try {
        await deletePhoto(id)
        this.message = 'Photo deleted'
        if (this.photos.length === 1 && this.page > 1) {
          this.page -= 1
        }
        await this.fetchPhotos()
      } catch (e) {
        this.error = e.response?.data?.message || 'Delete failed'
      }
    },
    formatSize(size) {
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / (1024 * 1024)).toFixed(1)} MB`
    },
    formatDate(dateStr) {
      return new Date(dateStr).toLocaleString()
    }
  }
}
</script>

<style scoped>
:root {
  font-family: "Helvetica Neue", Arial, sans-serif;
}

* {
  box-sizing: border-box;
}

.page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
  background: #f3f4f6;
  min-height: 100vh;
}

.header h1 {
  margin-bottom: 8px;
}

.header p {
  margin-top: 0;
  color: #6b7280;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form,
.filter-form {
  display: grid;
  gap: 10px;
}

.filter-form {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-bottom: 12px;
}

input,
button,
a {
  font: inherit;
}

input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px;
}

button {
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}

button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.danger {
  background: #dc2626;
}

.secondary {
  background: #4b5563;
}

.message {
  color: #047857;
}

.error {
  color: #dc2626;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.photo-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.photo-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
  background: #f9fafb;
}

.meta {
  padding: 10px;
}

.meta h3 {
  margin: 0 0 6px;
  font-size: 15px;
}

.meta small {
  color: #6b7280;
}

.chip-line {
  margin: 8px 0 0;
}

.chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  margin-right: 6px;
  background: #dbeafe;
  color: #1e40af;
  font-size: 12px;
}

.chip.tag {
  background: #ecfccb;
  color: #3f6212;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px 10px;
  gap: 10px;
}

.actions a {
  color: #2563eb;
  text-decoration: none;
}

.actions button {
  padding: 7px 10px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

@media (max-width: 640px) {
  .page {
    padding: 12px;
  }

  .pagination {
    flex-direction: column;
  }
}
</style>
