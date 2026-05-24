<script setup>
// 路由由router/index.js配置，这里不需要直接导入组件
</script>

<template>
  <!-- 路由视图 - 会根据URL自动切换对应组件 -->
  <router-view></router-view>
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  background-color: #f5f7fa;
}
</style>