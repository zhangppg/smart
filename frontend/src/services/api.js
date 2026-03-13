import axios from 'axios'

const TOKEN_KEY = 'photo_manager_token'
const USERNAME_KEY = 'photo_manager_username'
const ROLE_CODE_KEY = 'photo_manager_role_code'

function resolveApiBaseUrl() {
  const envUrl = process.env.VUE_APP_API_BASE_URL
  if (envUrl) return envUrl

  // When accessing from another device (tablet/phone), "localhost" would point to that device.
  // Default to the same host as the frontend, but on backend port 8080.
  if (typeof window !== 'undefined' && window.location) {
    const host = window.location.hostname
    const protocol = window.location.protocol || 'http:'
    return `${protocol}//${host}:8080`
  }

  return 'http://localhost:8080'
}

const api = axios.create({
  baseURL: resolveApiBaseUrl()
})

const TOAST_EVENT = 'app-toast'
let lastServerGlitchAt = 0

function emitToast(message, durationMs) {
  try {
    window.dispatchEvent(
      new CustomEvent(TOAST_EVENT, {
        detail: { message, durationMs }
      })
    )
  } catch (e) {
    // noop (e.g. SSR-like environments)
  }
}

function maybeNotifyServerGlitch(error) {
  const status = error?.response?.status
  const isNetworkError = !error?.response
  const isServerError = typeof status === 'number' && status >= 500
  if (!isNetworkError && !isServerError) return

  const now = Date.now()
  if (now - lastServerGlitchAt < 1200) return
  lastServerGlitchAt = now
  emitToast('稍等一下，服务器开小差了', 2000)
}

api.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    maybeNotifyServerGlitch(error)
    return Promise.reject(error)
  }
)

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY)
}

export function getRoleCode() {
  const v = localStorage.getItem(ROLE_CODE_KEY)
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

export function saveAuth(token, username, roleCode) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
  localStorage.setItem(ROLE_CODE_KEY, String(Number(roleCode) || 0))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(ROLE_CODE_KEY)
}

export function register(username, password) {
  return api.post('/api/auth/register', { username, password })
}

export function registerWithRole(username, password, roleCode) {
  return api.post('/api/auth/register', { username, password, roleCode })
}

export function login(username, password) {
  return api.post('/api/auth/login', { username, password })
}

export function listPhotos(params) {
  return api.get('/api/photos', { params })
}

export function uploadPhoto(formData) {
  return api.post('/api/photos/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deletePhoto(id) {
  return api.delete(`/api/photos/${id}`)
}

export function updatePhoto(id, payload) {
  return api.put(`/api/photos/${id}`, payload)
}

export function viewUrl(id) {
  const token = encodeURIComponent(getToken() || '')
  return `${api.defaults.baseURL}/api/photos/${id}/view?token=${token}`
}

export function downloadUrl(id) {
  const token = encodeURIComponent(getToken() || '')
  return `${api.defaults.baseURL}/api/photos/${id}/download?token=${token}`
}
