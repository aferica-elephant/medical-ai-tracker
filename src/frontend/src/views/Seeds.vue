<template>
  <div>
    <h2 style="margin-bottom: 20px;">种子库管理</h2>

    <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
      种子库中的产品会作为滚雪球搜索的固定起点，每次搜索都会包含这些种子，降低搜索随机性，确保稳定发现同类产品。
    </el-alert>

    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>种子产品列表</span>
          <div>
            <el-button type="success" size="small" @click="openAddDialog">新增种子</el-button>
            <el-button size="small" @click="openBatchDialog">批量导入</el-button>
          </div>
        </div>
      </template>
      <el-table :data="seeds" stripe style="width: 100%">
        <el-table-column prop="name" label="产品名称" width="180" />
        <el-table-column prop="company" label="所属公司" width="180" />
        <el-table-column prop="category" label="细分领域" width="140" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="toggleSeed(row)"
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
            <el-popconfirm title="确定删除该种子？" @confirm="deleteSeed(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑种子弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑种子' : '新增种子'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="产品名称">
          <el-input v-model="form.name" placeholder="如：蚂蚁阿福" />
        </el-form-item>
        <el-form-item label="所属公司">
          <el-input v-model="form.company" placeholder="如：蚂蚁集团" />
        </el-form-item>
        <el-form-item label="细分领域">
          <el-select v-model="form.category" placeholder="选择领域" style="width: 100%;" filterable allow-create>
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSeed" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量导入种子" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
        每行一个产品，格式：产品名称|公司|领域（领域可省略）
      </el-alert>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="10"
        placeholder="蚂蚁阿福|蚂蚁集团|AI问诊&#10;推想医疗|推想科技|AI影像诊断&#10;科大讯飞医疗|科大讯飞|AI病历语音"
      />
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="batchImport" :loading="batchSaving">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const seeds = ref([])
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const batchSaving = ref(false)
const form = ref({ name: '', company: '', category: '', enabled: true })
const batchText = ref('')

const categories = ref([
  'AI影像诊断', 'AI问诊', 'AI药物研发', 'AI辅助治疗',
  'AI健康管理', 'AI中医', 'AI病历语音', 'AI检验'
])

const loadSeeds = async () => {
  try {
    const { data } = await api.getSeeds()
    seeds.value = data
  } catch (e) {
    console.error('Failed to load seeds', e)
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { name: '', company: '', category: '', enabled: true }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, company: row.company, category: row.category, enabled: row.enabled }
  dialogVisible.value = true
}

const saveSeed = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写产品名称')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await api.updateSeed(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await api.createSeed(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadSeeds()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggleSeed = async (row) => {
  try {
    await api.toggleSeed(row.id)
    loadSeeds()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteSeed = async (id) => {
  try {
    await api.deleteSeed(id)
    ElMessage.success('删除成功')
    loadSeeds()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const openBatchDialog = () => {
  batchText.value = ''
  batchDialogVisible.value = true
}

const batchImport = async () => {
  const lines = batchText.value.trim().split('\n').filter(l => l.trim())
  if (!lines.length) {
    ElMessage.warning('请输入至少一条数据')
    return
  }
  batchSaving.value = true
  let success = 0
  let fail = 0
  for (const line of lines) {
    const parts = line.split('|').map(s => s.trim())
    if (parts[0]) {
      try {
        await api.createSeed({
          name: parts[0],
          company: parts[1] || '',
          category: parts[2] || '',
          enabled: true
        })
        success++
      } catch (e) {
        fail++
      }
    }
  }
  batchDialogVisible.value = false
  ElMessage.success(`导入完成：成功${success}条${fail > 0 ? '，失败' + fail + '条' : ''}`)
  loadSeeds()
  batchSaving.value = false
}

onMounted(loadSeeds)
</script>
