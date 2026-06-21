<template>
  <div>
    <h2 style="margin-bottom: 20px;">搜索管理</h2>

    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-button type="primary" @click="triggerSearch" :loading="searching">
        <el-icon><Search /></el-icon> 手动触发全量搜索
      </el-button>
      <span style="margin-left: 20px; color: #999;">搜索将按8个细分领域逐个搜索，预计耗时5-10分钟</span>
    </el-card>

    <el-card shadow="hover" v-if="searchResult">
      <template #header><span>搜索结果</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="搜索任务数">{{ searchResult.totalTasks }}</el-descriptions-item>
        <el-descriptions-item label="新增智能体">{{ searchResult.totalNew }}</el-descriptions-item>
        <el-descriptions-item label="更新智能体">{{ searchResult.totalUpdated }}</el-descriptions-item>
        <el-descriptions-item label="标记下线">{{ searchResult.offlineCount }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ (searchResult.duration / 1000).toFixed(1) }}秒</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header><span>搜索任务历史</span></template>
      <el-table :data="tasks" stripe style="width: 100%">
        <el-table-column prop="keyword" label="搜索关键词" />
        <el-table-column prop="category" label="领域" width="130" />
        <el-table-column prop="round" label="轮次" width="80" />
        <el-table-column prop="resultCount" label="发现数量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="执行时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const tasks = ref([])
const searching = ref(false)
const searchResult = ref(null)

const statusType = (status) => {
  const map = { COMPLETED: 'success', RUNNING: 'warning', FAILED: 'danger', PENDING: 'info' }
  return map[status] || 'info'
}

const triggerSearch = async () => {
  searching.value = true
  searchResult.value = null
  try {
    const { data } = await api.triggerSearch()
    searchResult.value = data
    ElMessage.success('搜索完成！')
    loadTasks()
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const loadTasks = async () => {
  try {
    const { data } = await api.getSearchTasks({ page: 1, size: 50 })
    tasks.value = data.records
  } catch (e) {
    console.error('Failed to load tasks', e)
  }
}

onMounted(loadTasks)
</script>
