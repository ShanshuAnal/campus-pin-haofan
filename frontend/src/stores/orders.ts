import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { orderApi } from '@/api'
import type {
  AssignPickupRequest,
  CancelGroupOrderRequest,
  CreateGroupOrderRequest,
  DashboardSummary,
  DashboardSummaryQuery,
  GroupOrderDetail,
  GroupOrderEvent,
  GroupOrderStatus,
  GroupOrderSummary,
  JoinGroupOrderRequest,
  LockGroupOrderRequest,
  MyGroupOrderQuery,
  MyGroupOrderRecord,
  OrderType,
  PaymentActionRequest,
  UpdatePickupStatusRequest
} from '@/types/order'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<GroupOrderSummary[]>([])
  const currentDetail = ref<GroupOrderDetail | null>(null)
  const currentEvents = ref<GroupOrderEvent[]>([])
  const myOrders = ref<MyGroupOrderRecord[]>([])
  const dashboard = ref<DashboardSummary | null>(null)
  const keyword = ref('')
  const status = ref<GroupOrderStatus | ''>('')
  const orderType = ref<OrderType | ''>('')
  const loading = ref(false)
  const eventsLoading = ref(false)
  const myOrdersLoading = ref(false)
  const dashboardLoading = ref(false)
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const myOrdersTotal = ref(0)
  const myOrdersPageNum = ref(1)
  const myOrdersPageSize = ref(10)

  const filteredOrders = computed(() => orders.value)

  const loadOrders = async (targetPage = pageNum.value) => {
    loading.value = true
    try {
      pageNum.value = targetPage
      const result = await orderApi.listGroupOrders({
        keyword: keyword.value.trim() || undefined,
        status: status.value || undefined,
        orderType: orderType.value || undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      orders.value = result.records
      total.value = result.total
      pageNum.value = result.pageNum
      pageSize.value = result.pageSize
    } finally {
      loading.value = false
    }
  }

  const resetAndLoadOrders = () => loadOrders(1)

  const changePageSize = async (size: number) => {
    pageSize.value = size
    await loadOrders(1)
  }

  const createOrder = (payload: CreateGroupOrderRequest) => orderApi.createGroupOrder(payload)

  const joinOrder = (id: number, payload: JoinGroupOrderRequest) => orderApi.joinGroupOrder(id, payload)

  const lockOrder = (id: number, payload: LockGroupOrderRequest) => orderApi.lockGroupOrder(id, payload)

  const cancelOrder = (id: number, payload: CancelGroupOrderRequest) => orderApi.cancelGroupOrder(id, payload)

  const markPayment = (orderId: number, participantId: number, payload: PaymentActionRequest) =>
    orderApi.markPayment(orderId, participantId, payload)

  const confirmPayment = (orderId: number, participantId: number, payload: PaymentActionRequest) =>
    orderApi.confirmPayment(orderId, participantId, payload)

  const assignPickupUser = (id: number, payload: AssignPickupRequest) => orderApi.assignPickupUser(id, payload)

  const updatePickupStatus = (id: number, payload: UpdatePickupStatusRequest) =>
    orderApi.updatePickupStatus(id, payload)

  const loadDetail = async (id: number) => {
    loading.value = true
    try {
      const detail = await orderApi.getGroupOrderDetail(id)
      currentDetail.value = detail
      currentEvents.value = detail.recentEvents ?? []
    } finally {
      loading.value = false
    }
  }

  const loadEvents = async (id: number) => {
    eventsLoading.value = true
    try {
      currentEvents.value = await orderApi.listGroupOrderEvents(id)
    } finally {
      eventsLoading.value = false
    }
  }

  const clearEvents = () => {
    currentEvents.value = []
    eventsLoading.value = false
  }

  const loadMyOrders = async (params: MyGroupOrderQuery = {}) => {
    myOrdersLoading.value = true
    try {
      const result = await orderApi.listMyOrders({
        pageNum: myOrdersPageNum.value,
        pageSize: myOrdersPageSize.value,
        ...params
      })
      myOrders.value = result.records
      myOrdersTotal.value = result.total
      myOrdersPageNum.value = result.pageNum
      myOrdersPageSize.value = result.pageSize
    } finally {
      myOrdersLoading.value = false
    }
  }

  const loadDashboard = async (params: DashboardSummaryQuery = {}) => {
    dashboardLoading.value = true
    try {
      dashboard.value = await orderApi.getDashboardSummary(params)
    } finally {
      dashboardLoading.value = false
    }
  }

  return {
    orders,
    currentDetail,
    currentEvents,
    myOrders,
    dashboard,
    keyword,
    status,
    orderType,
    loading,
    eventsLoading,
    myOrdersLoading,
    dashboardLoading,
    total,
    pageNum,
    pageSize,
    myOrdersTotal,
    myOrdersPageNum,
    myOrdersPageSize,
    filteredOrders,
    loadOrders,
    resetAndLoadOrders,
    changePageSize,
    createOrder,
    joinOrder,
    lockOrder,
    cancelOrder,
    markPayment,
    confirmPayment,
    assignPickupUser,
    updatePickupStatus,
    loadDetail,
    loadEvents,
    clearEvents,
    loadMyOrders,
    loadDashboard
  }
})
