<script setup lang="ts">
import {
  DataAnalysis,
  HomeFilled,
  Plus,
  SwitchButton,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { path: '/hall', label: '拼单大厅', icon: HomeFilled },
  { path: '/orders/new', label: '发起拼单', icon: Plus },
  { path: '/my-orders', label: '我的拼单', icon: Tickets },
  { path: '/dashboard', label: '数据看板', icon: DataAnalysis }
]

const activePath = computed(() => {
  if (route.path.startsWith('/orders/') && route.path !== '/orders/new') {
    return '/hall'
  }
  return route.path
})

const logout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <ElContainer class="app-shell">
    <ElHeader class="topbar">
      <div class="brand">
        <span class="brand__mark">拼</span>
        <div>
          <strong>校园拼好饭</strong>
          <small>校园拼单协同演示</small>
        </div>
      </div>
      <div class="topbar__actions">
        <ElButton text :icon="User">{{ userStore.user?.nickname ?? '演示用户' }}</ElButton>
        <ElButton :icon="SwitchButton" @click="logout">退出</ElButton>
      </div>
    </ElHeader>
    <ElContainer class="shell-body">
      <ElAside width="232px" class="sidebar">
        <ElMenu :default-active="activePath" router>
          <ElMenuItem v-for="item in navItems" :key="item.path" :index="item.path">
            <ElIcon><component :is="item.icon" /></ElIcon>
            <span>{{ item.label }}</span>
          </ElMenuItem>
        </ElMenu>
      </ElAside>
      <ElMain class="main-content">
        <RouterView />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>
