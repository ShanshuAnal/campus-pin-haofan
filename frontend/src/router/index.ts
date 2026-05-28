import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import CreateOrderView from '@/views/CreateOrderView.vue'
import DashboardView from '@/views/DashboardView.vue'
import HallView from '@/views/HallView.vue'
import LoginView from '@/views/LoginView.vue'
import MyOrdersView from '@/views/MyOrdersView.vue'
import OrderDetailView from '@/views/OrderDetailView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/hall',
      children: [
        {
          path: 'hall',
          name: 'hall',
          component: HallView,
          meta: { title: '拼单大厅', requiresAuth: true }
        },
        {
          path: 'orders/new',
          name: 'create-order',
          component: CreateOrderView,
          meta: { title: '发起拼单', requiresAuth: true }
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: OrderDetailView,
          meta: { title: '拼单详情', requiresAuth: true }
        },
        {
          path: 'my-orders',
          name: 'my-orders',
          component: MyOrdersView,
          meta: { title: '我的拼单', requiresAuth: true }
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '数据看板', requiresAuth: true }
        }
      ]
    }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const restored = await userStore.restoreSession()

  if (to.meta.public && restored && to.path === '/login') {
    return '/hall'
  }

  if (to.meta.requiresAuth && !restored) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '首页')} - 校园拼好饭`
})

export default router
