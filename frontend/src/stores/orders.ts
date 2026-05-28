import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { orderApi } from '@/api'
import type {
  DashboardSummary,
  GroupOrderDetail,
  GroupOrderStatus,
  GroupOrderSummary,
  MyGroupOrderRecord,
  OrderType
} from '@/types/order'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<GroupOrderSummary[]>([])
  const currentDetail = ref<GroupOrderDetail | null>(null)
  const myOrders = ref<MyGroupOrderRecord[]>([])
  const dashboard = ref<DashboardSummary | null>(null)
  const keyword = ref('')
  const status = ref<GroupOrderStatus | ''>('')
  const orderType = ref<OrderType | ''>('')
  const loading = ref(false)

  const filteredOrders = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase()
    return orders.value.filter((order) => {
      const matchesKeyword =
        !normalizedKeyword ||
        order.title.toLowerCase().includes(normalizedKeyword) ||
        order.merchantName.toLowerCase().includes(normalizedKeyword)
      const matchesStatus = !status.value || order.status === status.value
      const matchesType = !orderType.value || order.orderType === orderType.value
      return matchesKeyword && matchesStatus && matchesType
    })
  })

  const loadOrders = async () => {
    loading.value = true
    try {
      orders.value = await orderApi.listGroupOrders()
    } finally {
      loading.value = false
    }
  }

  const loadDetail = async (id: number) => {
    loading.value = true
    try {
      currentDetail.value = await orderApi.getGroupOrderDetail(id)
    } finally {
      loading.value = false
    }
  }

  const loadMyOrders = async () => {
    myOrders.value = await orderApi.listMyOrders()
  }

  const loadDashboard = async () => {
    dashboard.value = await orderApi.getDashboardSummary()
  }

  return {
    orders,
    currentDetail,
    myOrders,
    dashboard,
    keyword,
    status,
    orderType,
    loading,
    filteredOrders,
    loadOrders,
    loadDetail,
    loadMyOrders,
    loadDashboard
  }
})
