import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000
})

export default {
  // Dashboard
  getDashboardStats: () => api.get('/dashboard/stats'),

  // Agents
  getAgents: (params) => api.get('/agents', { params }),
  getAgent: (id) => api.get(`/agents/${id}`),
  getRanking: () => api.get('/agents/rank'),
  getCategories: () => api.get('/agents/categories'),

  // Search
  triggerSearch: () => api.post('/search/trigger'),
  getSearchTasks: (params) => api.get('/search/tasks', { params }),

  // Keywords
  getKeywords: () => api.get('/keywords'),
  createKeyword: (data) => api.post('/keywords', data),
  updateKeyword: (id, data) => api.put(`/keywords/${id}`, data),
  deleteKeyword: (id) => api.delete(`/keywords/${id}`),
  toggleKeyword: (id) => api.patch(`/keywords/${id}/toggle`),

  // Seeds
  getSeeds: () => api.get('/seeds'),
  createSeed: (data) => api.post('/seeds', data),
  updateSeed: (id, data) => api.put(`/seeds/${id}`, data),
  deleteSeed: (id) => api.delete(`/seeds/${id}`),
  toggleSeed: (id) => api.patch(`/seeds/${id}/toggle`),

  // Changes
  getChanges: (params) => api.get('/changes', { params })
}
