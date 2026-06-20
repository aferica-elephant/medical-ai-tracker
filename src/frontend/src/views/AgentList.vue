<template>
  <div>
    <h2 style="margin-bottom: 20px;">智能体列表</h2>

    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="搜索名称/公司" clearable @clear="loadAgents" />
        </el-form-item>
        <el-form-item label="领域">
          <el-select v-model="category" placeholder="全部" clearable @change="loadAgents">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" placeholder="全部" clearable @change="loadAgents">
            <el-option label="活跃" value="active" />
            <el-option label="已下线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAgents">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table :data="agents" stripe style="width: 100%">
        <el-table-column prop="name" label="产品名称" width="180" />
        <el-table-column prop="company" label="所属公司" width="180" />
        <el-table-column prop="category" label="细分领域" width="130" />
        <el-table-column prop="targetUser" label="面向用户" width="100" />
        <el-table-column prop="description" label="简介" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '活跃' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastVerifiedDate" label="最近验证" width="120" />
      </el-table>
      <div style="margin-top: 20px; text-align: right;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadAgents"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const agents = ref([])
const categories = ref([])
const keyword = ref('')
const category = ref('')
const status = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const loadAgents = async () => {
  try {
    const { data } = await api.getAgents({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      status: status.value || undefined
    })
    agents.value = data.records
    total.value = data.total
  } catch (e) {
    console.error('Failed to load agents', e)
  }
}

const loadCategories = async () => {
  try {
    const { data } = await api.getCategories()
    categories.value = Object.keys(data)
  } catch (e) {
    console.error('Failed to load categories', e)
  }
}

onMounted(() => {
  loadAgents()
  loadCategories()
})
</script>
