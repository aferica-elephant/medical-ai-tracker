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

  // Changes
  getChanges: (params) => api.get('/changes', { params })
}
