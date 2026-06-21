import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import AgentList from '../views/AgentList.vue'
import Rank from '../views/Rank.vue'
import Search from '../views/Search.vue'
import Seeds from '../views/Seeds.vue'
import Changes from '../views/Changes.vue'

const routes = [
  { path: '/', component: Dashboard },
  { path: '/agents', component: AgentList },
  { path: '/rank', component: Rank },
  { path: '/search', component: Search },
  { path: '/seeds', component: Seeds },
  { path: '/changes', component: Changes }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
