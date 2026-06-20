<template>
  <div>
    <h2 style="margin-bottom: 20px;">变更日志</h2>
    <el-card shadow="hover">
      <el-table :data="changes" stripe style="width: 100%">
        <el-table-column prop="agentId" label="智能体ID" width="100" />
        <el-table-column prop="changeType" label="变更类型" width="100">
          <template #default="{ row }">
            <el-tag :type="changeTypeTag(row.changeType)" size="small">{{ row.changeType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldName" label="变更字段" width="120" />
        <el-table-column prop="oldValue" label="旧值" show-overflow-tooltip />
        <el-table-column prop="newValue" label="新值" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="变更时间" width="180" />
      </el-table>
      <div style="margin-top: 20px; text-align: right;">
        <el-pagination
          v-model:current-page="page"
          :total="total"
          :page-size="20"
          layout="total, prev, pager, next"
          @current-change="loadChanges"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const changes = ref([])
const page = ref(1)
const total = ref(0)

const changeTypeTag = (type) => {
  const map = { NEW: 'success', UPDATED: 'warning', OFFLINE: 'danger' }
  return map[type] || 'info'
}

const loadChanges = async () => {
  try {
    const { data } = await api.getChanges({ page: page.value, size: 20 })
    changes.value = data
    total.value = data.length
  } catch (e) {
    console.error('Failed to load changes', e)
  }
}

onMounted(loadChanges)
</script>
