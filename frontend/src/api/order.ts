import { http } from './http'
import type { PageResult } from '@/types/api'
import type {
  AssignPickupRequest,
  AssignPickupResponse,
  ConfirmPaymentResponse,
  CreateGroupOrderRequest,
  DashboardSummary,
  DashboardSummaryQuery,
  GroupOrderDetail,
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

export const orderApi = {
  listGroupOrders: (params: GroupOrderQuery) =>
    unwrap<PageResult<GroupOrderSummary>>(
      http.get('/group-orders', {
        params
      })
    ),
  createGroupOrder: (data: CreateGroupOrderRequest) =>
    unwrap<GroupOrderSummary>(http.post('/group-orders', data)),
  getGroupOrderDetail: (id: number) => unwrap<GroupOrderDetail>(http.get(`/group-orders/${id}`)),
  joinGroupOrder: (id: number, data: JoinGroupOrderRequest) =>
    unwrap<JoinGroupOrderResponse>(http.post(`/group-orders/${id}/participants`, data)),
  lockGroupOrder: (id: number, data: LockGroupOrderRequest) =>
    unwrap<LockGroupOrderResponse>(http.post(`/group-orders/${id}/lock`, data)),
  markPayment: (orderId: number, participantId: number, data: PaymentActionRequest) =>
    unwrap<MarkPaymentResponse>(
      http.post(`/group-orders/${orderId}/participants/${participantId}/payments/mark`, data)
    ),
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
