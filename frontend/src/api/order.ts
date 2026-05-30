import { http } from './http'
import type { PageResult } from '@/types/api'
import type {
  AssignPickupRequest,
  AssignPickupResponse,
  CancelGroupOrderRequest,
  CancelGroupOrderResponse,
  ConfirmPaymentResponse,
  CreateGroupOrderRequest,
  DashboardSummary,
  DashboardSummaryQuery,
  GroupOrderDetail,
  GroupOrderEvent,
  GroupOrderQuery,
  GroupOrderSummary,
  JoinGroupOrderRequest,
  JoinGroupOrderResponse,
  LockGroupOrderRequest,
  LockGroupOrderResponse,
  MarkPaymentResponse,
  MyGroupOrderQuery,
  MyGroupOrderRecord,
  PaymentActionRequest,
  UpdatePickupStatusRequest,
  UpdatePickupStatusResponse
} from '@/types/order'

const unwrap = <T>(request: Promise<unknown>) => request as Promise<T>
type LooseRecord = Record<string, any>

const asUserSummary = (payload: LooseRecord | null | undefined) => ({
  id: Number(payload?.id ?? payload?.userId ?? 0),
  username: String(payload?.username ?? payload?.account ?? ''),
  nickname: String(payload?.nickname ?? payload?.username ?? payload?.account ?? '同学'),
  phone: payload?.phone ?? null,
  status: payload?.status ?? 'ACTIVE'
})

const normalizeGroupOrderDetail = (payload: unknown): GroupOrderDetail => {
  const raw = payload as LooseRecord
  if (raw.order) {
    const detail = raw as GroupOrderDetail
    return {
      ...detail,
      pickup: detail.pickup ?? (detail.pickupRecord
        ? {
            pickupStatus: detail.pickupRecord.pickupStatus,
            pickupLocation: detail.pickupRecord.pickupLocation,
            pickupUser: detail.pickupRecord.pickupUser
          }
        : null),
      recentEvents: detail.recentEvents ?? []
    }
  }

  const order = {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    orderType: raw.orderType ?? 'TAKEOUT',
    merchantName: String(raw.merchantName ?? raw.shopName ?? ''),
    shopName: raw.shopName,
    pickupLocation: String(raw.pickupLocation ?? raw.pickup?.pickupLocation ?? ''),
    creator: asUserSummary(raw.creator),
    deadlineTime: String(raw.deadlineTime ?? ''),
    maxParticipants: Number(raw.maxParticipants ?? raw.maxParticipantCount ?? raw.participantLimit ?? 1),
    participantCount: Number(raw.participantCount ?? raw.participants?.length ?? 0),
    minAmount: raw.minAmount ?? raw.discountThresholdAmount,
    discountThresholdAmount: raw.discountThresholdAmount ?? raw.minAmount ?? null,
    discountAmount: Number(raw.discountAmount ?? 0),
    originalTotalAmount: Number(raw.originalTotalAmount ?? raw.totalAmount ?? 0),
    actualDiscountAmount: Number(raw.actualDiscountAmount ?? 0),
    payableTotalAmount: Number(raw.payableTotalAmount ?? raw.payableAmount ?? raw.totalAmount ?? 0),
    roundingAdjustmentAmount: Number(raw.roundingAdjustmentAmount ?? 0),
    status: raw.status,
    joinable: raw.joinable,
    progressPercent: raw.progressPercent,
    remainingSeconds: raw.remainingSeconds,
    lastEventSummary: raw.lastEventSummary ?? null,
    canViewDetail: raw.canViewDetail ?? raw.viewable,
    viewable: raw.viewable ?? raw.canViewDetail,
    pickupUser: raw.pickupUser ? asUserSummary(raw.pickupUser) : (raw.pickup?.pickupUser ? asUserSummary(raw.pickup.pickupUser) : null),
    remark: String(raw.remark ?? ''),
    lockedTime: raw.lockedTime ?? raw.locked_time ?? null,
    finishTime: raw.finishTime ?? raw.finish_time ?? null,
    cancelTime: raw.cancelTime ?? raw.cancel_time ?? null,
    cancelReason: raw.cancelReason ?? raw.cancel_reason ?? null,
    expiredTime: raw.expiredTime ?? raw.expired_time ?? null,
    expireReason: raw.expireReason ?? raw.expire_reason ?? null,
    createTime: String(raw.createTime ?? ''),
    updateTime: String(raw.updateTime ?? '')
  } as GroupOrderSummary

  const participants = (raw.participants ?? []).map((participant: LooseRecord) => ({
    id: Number(participant.id),
    groupOrderId: Number(participant.groupOrderId ?? raw.id),
    user: asUserSummary(participant.user ?? participant),
    originalAmount: Number(participant.originalAmount ?? 0),
    discountShareAmount: Number(participant.discountShareAmount ?? participant.shareDiscountAmount ?? 0),
    payableAmount: Number(participant.payableAmount ?? 0),
    roundingAdjustmentAmount: Number(participant.roundingAdjustmentAmount ?? 0),
    paymentStatus: participant.paymentStatus ?? 'UNPAID',
    paidMarkTime: participant.paidMarkTime ?? null,
    paidConfirmTime: participant.paidConfirmTime ?? null,
    joinTime: String(participant.joinTime ?? ''),
    remark: String(participant.remark ?? ''),
    mealItems: (participant.mealItems ?? participant.items ?? []).map((item: LooseRecord) => ({
      id: Number(item.id ?? 0),
      groupOrderId: Number(item.groupOrderId ?? raw.id),
      participantId: Number(item.participantId ?? participant.id),
      itemName: String(item.itemName ?? ''),
      quantity: Number(item.quantity ?? 1),
      unitPrice: Number(item.unitPrice ?? 0),
      subtotalAmount: Number(item.subtotalAmount ?? Number(item.unitPrice ?? 0) * Number(item.quantity ?? 1)),
      remark: String(item.remark ?? '')
    }))
  }))

  const pickupRecord = raw.pickupRecord
    ? raw.pickupRecord
    : raw.pickup
      ? {
          id: 0,
          groupOrderId: Number(raw.id),
          pickupUser: raw.pickup.pickupUser ? asUserSummary(raw.pickup.pickupUser) : (order.pickupUser ?? asUserSummary(null)),
          pickupLocation: raw.pickup.pickupLocation ?? order.pickupLocation,
          pickupStatus: raw.pickup.pickupStatus,
          estimatedArrivalTime: null,
          actualArrivalTime: null,
          pickedUpTime: null,
          distributedTime: null,
          remark: ''
        }
      : null

  return {
    order,
    participants,
    pickupRecord,
    pickup: raw.pickup ?? (pickupRecord
      ? {
          pickupStatus: pickupRecord.pickupStatus,
          pickupLocation: pickupRecord.pickupLocation,
          pickupUser: pickupRecord.pickupUser
        }
      : null),
    permissions: raw.permissions,
    currentUserRole: raw.currentUserRole,
    recentEvents: raw.recentEvents ?? []
  }
}

const normalizeCreatedOrder = (payload: unknown): GroupOrderSummary => {
  const raw = payload as LooseRecord
  if (raw.order) {
    return normalizeGroupOrderDetail(payload).order
  }
  return normalizeGroupOrderDetail(payload).order
}

const normalizeGroupOrderEvent = (payload: unknown): GroupOrderEvent => {
  const raw = payload as LooseRecord
  const operator = raw.operator ? asUserSummary(raw.operator) : null

  return {
    id: Number(raw.id ?? 0),
    eventType: raw.eventType ?? raw.event_type ?? 'NOTE',
    eventLevel: raw.eventLevel ?? raw.event_level ?? raw.level ?? 'INFO',
    operatorId: raw.operatorId ?? raw.operator_id ?? operator?.id ?? null,
    operatorRole: raw.operatorRole ?? raw.operator_role ?? null,
    operatorName: raw.operatorName ?? raw.operator_name ?? operator?.nickname ?? null,
    operator,
    title: String(raw.title ?? ''),
    content: String(raw.content ?? ''),
    eventTime: String(raw.eventTime ?? raw.event_time ?? ''),
    beforeStatus: raw.beforeStatus ?? raw.before_status ?? null,
    afterStatus: raw.afterStatus ?? raw.after_status ?? null
  }
}

const normalizeGroupOrderEvents = (payload: unknown): GroupOrderEvent[] => {
  const raw = payload as LooseRecord
  const records = Array.isArray(payload)
    ? payload
    : Array.isArray(raw.records)
      ? raw.records
      : Array.isArray(raw.list)
        ? raw.list
        : []

  return records.map(normalizeGroupOrderEvent)
}

export const orderApi = {
  listGroupOrders: (params: GroupOrderQuery) =>
    unwrap<PageResult<GroupOrderSummary>>(
      http.get('/group-orders', {
        params
      })
    ),
  createGroupOrder: (data: CreateGroupOrderRequest) =>
    http.post('/group-orders', data).then(normalizeCreatedOrder),
  getGroupOrderDetail: (id: number) => http.get(`/group-orders/${id}`).then(normalizeGroupOrderDetail),
  listGroupOrderEvents: (id: number) =>
    http.get(`/group-orders/${id}/events`).then(normalizeGroupOrderEvents),
  joinGroupOrder: (id: number, data: JoinGroupOrderRequest) =>
    unwrap<JoinGroupOrderResponse>(http.post(`/group-orders/${id}/participants`, data)),
  lockGroupOrder: (id: number, data: LockGroupOrderRequest) =>
    unwrap<LockGroupOrderResponse>(http.post(`/group-orders/${id}/lock`, data)),
  markPayment: (orderId: number, participantId: number, data: PaymentActionRequest) =>
    unwrap<MarkPaymentResponse>(
      http.post(`/group-orders/${orderId}/participants/${participantId}/payments/mark`, data)
    ),
  cancelGroupOrder: (id: number, data: CancelGroupOrderRequest) =>
    unwrap<CancelGroupOrderResponse>(http.post(`/group-orders/${id}/cancel`, data)),
  confirmPayment: (orderId: number, participantId: number, data: PaymentActionRequest) =>
    unwrap<ConfirmPaymentResponse>(
      http.post(`/group-orders/${orderId}/participants/${participantId}/payments/confirm`, data)
    ),
  assignPickupUser: (id: number, data: AssignPickupRequest) =>
    unwrap<AssignPickupResponse>(http.put(`/group-orders/${id}/pickup-assignee`, data)),
  updatePickupStatus: (id: number, data: UpdatePickupStatusRequest) =>
    unwrap<UpdatePickupStatusResponse>(http.patch(`/group-orders/${id}/pickup-status`, data)),
  listMyOrders: (params: MyGroupOrderQuery) =>
    unwrap<PageResult<MyGroupOrderRecord>>(
      http.get('/my/group-orders', {
        params
      })
    ),
  getDashboardSummary: (params: DashboardSummaryQuery = {}) =>
    unwrap<DashboardSummary>(
      http.get('/dashboard/summary', {
        params
      })
    )
}
