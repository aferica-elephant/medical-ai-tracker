<template>
  <div>
    <h2 style="margin-bottom: 20px;">仪表盘</h2>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center;">
            <div style="font-size: 36px; color: #409EFF; font-weight: bold;">{{ stats.totalAgents || 0 }}</div>
            <div style="color: #999; margin-top: 8px;">智能体总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center;">
            <div style="font-size: 36px; color: #67C23A; font-weight: bold;">{{ stats.activeAgents || 0 }}</div>
            <div style="color: #999; margin-top: 8px;">活跃智能体</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center;">
            <div style="font-size: 36px; color: #F56C6C; font-weight: bold;">{{ stats.offlineAgents || 0 }}</div>
            <div style="color: #999; margin-top: 8px;">已下线</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>领域分布</span>
          </template>
          <div ref="chartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>系统信息</span>
          </template>
          <div style="line-height: 2.5;">
            <div>最近更新：{{ stats.lastUpdateTime || '暂无' }}</div>
            <div style="margin-top: 20px;">
              <el-button type="primary" @click="triggerSearch" :loading="searching" style="width: 100%;">
                <el-icon><Search /></el-icon> 手动触发搜索
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '../api'

const stats = ref({})
const chartRef = ref(null)
const searching = ref(false)
let chartInstance = null

const loadStats = async () => {
  try {
    const { data } = await api.getDashboardStats()
    stats.value = data
    await nextTick()
    renderChart(data.categoryDistribution || {})
  } catch (e) {
    console.error('Failed to load stats', e)
  }
}

const renderChart = (distribution) => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const pieData = Object.entries(distribution).map(([name, value]) => ({ name, value }))
  chartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: pieData
    }]
  })
}

const triggerSearch = async () => {
  searching.value = true
  try {
    const { data } = await api.triggerSearch()
    ElMessage.success(`搜索完成！新增${data.totalNew}个，更新${data.totalUpdated}个`)
    loadStats()
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', () => chartInstance?.resize())
})
</script>
