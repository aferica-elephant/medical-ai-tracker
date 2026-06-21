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

    <!-- 关键词配置管理 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>搜索关键词配置</span>
          <el-button type="primary" size="small" @click="openAddDialog">新增关键词</el-button>
        </div>
      </template>
      <el-table :data="keywords" stripe style="width: 100%">
        <el-table-column prop="category" label="细分领域" width="140" />
        <el-table-column prop="keyword" label="搜索关键词" />
        <el-table-column prop="round" label="轮次" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="roundTagType(row.round)">{{ roundLabel(row.round) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="toggleKeyword(row)"
              size="small"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该关键词？" @confirm="deleteKeyword(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 搜索任务历史 -->
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

    <!-- 新增/编辑关键词弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑关键词' : '新增关键词'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="细分领域">
          <el-select v-model="form.category" placeholder="选择领域" style="width: 100%;">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            <el-option label="+ 自定义领域" value="__custom__" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.category === '__custom__'" label="自定义领域">
          <el-input v-model="customCategory" placeholder="输入新的细分领域名称" />
        </el-form-item>
        <el-form-item label="搜索关键词">
          <el-input v-model="form.keyword" placeholder="如：国内AI影像诊断产品" />
        </el-form-item>
        <el-form-item label="搜索轮次">
          <el-radio-group v-model="form.round">
            <el-radio :value="1">第1轮 - 广搜</el-radio>
            <el-radio :value="2">第2轮 - 深挖</el-radio>
            <el-radio :value="3">第3轮 - 滚雪球</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKeyword" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const tasks = ref([])
const searching = ref(false)
const searchResult = ref(null)
const keywords = ref([])

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const form = ref({ category: '', keyword: '', round: 1, enabled: true })
const customCategory = ref('')

// 已有领域列表（从关键词数据中提取）
const categories = ref([])

const statusType = (status) => {
  const map = { COMPLETED: 'success', RUNNING: 'warning', FAILED: 'danger', PENDING: 'info' }
  return map[status] || 'info'
}

const roundLabel = (round) => {
  const map = { 1: '广搜', 2: '深挖', 3: '滚雪球' }
  return map[round] || `第${round}轮`
}

const roundTagType = (round) => {
  const map = { 1: '', 2: 'warning', 3: 'success' }
  return map[round] || 'info'
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

const loadKeywords = async () => {
  try {
    const { data } = await api.getKeywords()
    keywords.value = data
    // 提取已有领域列表
    const cats = [...new Set(data.map(k => k.category))]
    categories.value = cats
  } catch (e) {
    console.error('Failed to load keywords', e)
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { category: '', keyword: '', round: 1, enabled: true }
  customCategory.value = ''
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { id: row.id, category: row.category, keyword: row.keyword, round: row.round, enabled: row.enabled }
  customCategory.value = ''
  dialogVisible.value = true
}

const saveKeyword = async () => {
  const payload = { ...form.value }
  // 处理自定义领域
  if (payload.category === '__custom__') {
    if (!customCategory.value.trim()) {
      ElMessage.warning('请输入自定义领域名称')
      return
    }
    payload.category = customCategory.value.trim()
  }
  if (!payload.keyword || !payload.category) {
    ElMessage.warning('请填写完整信息')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await api.updateKeyword(payload.id, payload)
      ElMessage.success('更新成功')
    } else {
      await api.createKeyword(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadKeywords()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggleKeyword = async (row) => {
  try {
    await api.toggleKeyword(row.id)
    loadKeywords()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteKeyword = async (id) => {
  try {
    await api.deleteKeyword(id)
    ElMessage.success('删除成功')
    loadKeywords()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadTasks()
  loadKeywords()
})
</script>
