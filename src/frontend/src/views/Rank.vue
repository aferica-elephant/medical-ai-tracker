<template>
  <div>
    <h2 style="margin-bottom: 20px;">Top 100 医疗AI智能体排行</h2>
    <el-card shadow="hover">
      <el-table :data="agents" stripe style="width: 100%">
        <el-table-column type="index" label="排名" width="70">
          <template #default="{ $index }">
            <span :style="{ fontWeight: $index < 3 ? 'bold' : 'normal', color: $index < 3 ? '#E6A23C' : '#333' }">
              {{ $index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="产品名称" width="180" />
        <el-table-column prop="company" label="所属公司" width="180" />
        <el-table-column prop="category" label="细分领域" width="130" />
        <el-table-column prop="targetUser" label="面向用户" width="100" />
        <el-table-column prop="description" label="简介" show-overflow-tooltip />
        <el-table-column prop="lastVerifiedDate" label="最近验证" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const agents = ref([])

const loadRanking = async () => {
  try {
    const { data } = await api.getRanking()
    agents.value = data
  } catch (e) {
    console.error('Failed to load ranking', e)
  }
}

onMounted(loadRanking)
</script>
