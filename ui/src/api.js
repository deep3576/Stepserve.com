import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8000/api/v1'

// ── Token helpers ──────────────────────────────────────────
const TOKEN_KEY = 'stepserve_token'

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const storeToken = (t) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

// ── Axios instance factory (recreated when token changes) ──
export function createApiClient(token = '') {
  const client = axios.create({ baseURL: BASE_URL })
  client.interceptors.request.use((config) => {
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  })
  return client
}

// ── Category icon/colour mapping ──────────────────────────
export const CATEGORY_META = {
  Cleaning:    { icon: '🧹', bg: '#e0f0eb' },
  Landscaping: { icon: '🌿', bg: '#e3f2e8' },
  Plumbing:    { icon: '🔧', bg: '#e8eef6' },
  Electrical:  { icon: '⚡', bg: '#fdecea' },
  Carpentry:   { icon: '🪚', bg: '#f0edf8' },
  Painting:    { icon: '🎨', bg: '#fef8ec' },
  HVAC:        { icon: '❄️', bg: '#e6f4f0' },
  Moving:      { icon: '📦', bg: '#faf5e4' },
  'Pet Care':  { icon: '🐾', bg: '#fdf0f5' },
  Windows:     { icon: '🪟', bg: '#e8f4f0' },
  Renovation:  { icon: '🏗️', bg: '#f0e8f8' },
  Other:       { icon: '🔍', bg: '#f5f5f5' },
}

// ── Adapt a backend service object into a ProviderCard-compatible shape ──
export function serviceToProvider(svc, categories = []) {
  const cat = categories.find((c) => c.id === svc.category_id)
  const catName = cat?.name || 'Other'
  const meta = CATEGORY_META[catName] || { icon: '🔧', bg: '#f0f0f0' }
  return {
    id: svc.id,
    name: svc.provider_name || svc.title,
    city: svc.location || 'Canada',
    cat: catName,
    rating: 5,
    reviews: 0,
    price: `$${Number(svc.price).toFixed(0)}/visit`,
    cert: false,
    insured: false,
    icon: meta.icon,
    bg: meta.bg,
    bio: svc.description,
    serviceTitle: svc.title,
  }
}

// ── Auth API ───────────────────────────────────────────────
export async function apiRegister(client, { email, password, role = 'provider' }) {
  const { data } = await client.post('/auth/register', { email, password, role })
  return data // { access_token, token_type }
}

export async function apiLogin(client, { email, password }) {
  const { data } = await client.post('/auth/login', { email, password })
  return data // { access_token, token_type }
}

export async function apiGetMe(client) {
  const { data } = await client.get('/auth/me')
  return data // { id, email, role, is_active }
}

// ── Provider profile ───────────────────────────────────────
export async function apiSaveProviderProfile(client, payload) {
  const { data } = await client.post('/providers/profile', payload)
  return data
}

export async function apiGetProviderDashboard(client) {
  const { data } = await client.get('/provider/dashboard')
  return data // { services, bookings, uploads }
}

// ── Categories ─────────────────────────────────────────────
export async function apiGetCategories(client) {
  const { data } = await client.get('/categories')
  return data // [{ id, name, slug, services_count }]
}

// ── Home / Search ──────────────────────────────────────────
export async function apiGetHome(client) {
  const { data } = await client.get('/stepserve/home')
  return data // { categories, featured, latest, top_locations }
}

export async function apiSearchServices(client, params = {}) {
  const { data } = await client.get('/search/services', { params })
  return data // [services]
}

// ── Services ───────────────────────────────────────────────
export async function apiCreateService(client, payload) {
  const { data } = await client.post('/services', payload)
  return data
}

// ── Admin ──────────────────────────────────────────────────
export async function apiAdminOverview(client) {
  const { data } = await client.get('/admin/overview')
  return data
}

export async function apiAdminUsers(client) {
  const { data } = await client.get('/admin/users')
  return data
}

export async function apiAdminBookings(client) {
  const { data } = await client.get('/admin/bookings')
  return data
}

export async function apiAdminUpdateUserStatus(client, userId, active) {
  const { data } = await client.patch(`/admin/users/${userId}/status`, null, { params: { active } })
  return data
}

// ── Admin: create category ─────────────────────────────────
export async function apiAdminCreateCategory(client, payload) {
  const { data } = await client.post('/categories', payload)
  return data
}
