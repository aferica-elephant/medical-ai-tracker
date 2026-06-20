<template>
  <el-container style="min-height: 100vh">
    <el-aside width="220px" style="background-color: #304156">
      <div style="padding: 20px; text-align: center; color: #fff; font-size: 16px; font-weight: bold;">
        医疗AI追踪系统
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/agents">
          <el-icon><List /></el-icon>
          <span>智能体列表</span>
        </el-menu-item>
        <el-menu-item index="/rank">
          <el-icon><Trophy /></el-icon>
          <span>Top100排行</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>搜索管理</span>
        </el-menu-item>
        <el-menu-item index="/changes">
          <el-icon><Document /></el-icon>
          <span>变更日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main style="background-color: #f0f2f5; padding: 20px;">
      <router-view />
    </el-main>
  </el-container>
</template>
