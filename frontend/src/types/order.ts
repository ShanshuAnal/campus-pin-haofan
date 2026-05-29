import type { UserSummary } from './auth'

export type GroupOrderStatus =
  | 'CREATED'
  | 'LOCKED'
  | 'ORDERED'
  | 'DELIVERING'
  | 'ARRIVED'
  | 'PICKED_UP'
  | 'FINISHED'
  | 'CANCELLED'
  | 'EXPIRED'

export type PaymentStatus = 'UNPAID' | 'PAID' | 'CONFIRMED' | 'REFUNDED'

export type PickupStatus =
  | 'WAITING_ORDER'
  | 'WAITING_DELIVERY'
  | 'ARRIVED'
  | 'PICKED_UP'
  | 'DISTRIBUTED'

export type OrderType = 'TAKEOUT' | 'CANTEEN' | 'MILK_TEA' | 'MIDNIGHT_SNACK'

export type GroupOrderEventType =
  | 'CANCELLED'
  | 'EXPIRED'
  | 'DELAY_REPORTED'
  | 'MERCHANT_DELAY'
  | 'DELIVERY_DELAY'
  | 'PICKUP_EXCEPTION'
  | 'ITEM_MISSING'
  | 'CONTACT_FAILED'
  | 'PAYMENT_DISPUTE'
  | 'NOTE'

export type GroupOrderEventLevel = 'INFO' | 'WARN' | 'ERROR'

export interface GroupOrderQuery {
  status?: GroupOrderStatus
  orderType?: OrderType
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export type MyGroupOrderScope =
  | 'CREATED_BY_ME'
  | 'JOINED_BY_ME'
  | 'PICKUP_BY_ME'
  | 'PENDING_PAYMENT'
  | 'HISTORY'

export interface MyGroupOrderQuery {
  scope?: MyGroupOrderScope
  status?: GroupOrderStatus
  pageNum?: number
  pageSize?: number
}

export interface DashboardSummaryQuery {
  startTime?: string
  endTime?: string
  scope?: 'ALL' | 'MINE'
}

export interface CreateGroupOrderRequest {
  title: string
  orderType: OrderType
  merchantName: string
  pickupLocation: string
  deadlineTime: string
  maxParticipants?: number
  discountThresholdAmount?: number
  discountAmount?: number
  remark?: string
  creatorItems?: JoinMealItemRequest[]
}

export interface JoinMealItemRequest {
  itemName: string
  quantity: number
  unitPrice: number
  remark?: string
}

export interface JoinGroupOrderRequest {
  remark?: string
  mealItems: JoinMealItemRequest[]
}

export interface LockGroupOrderRequest {
  remark?: string
}

export interface CancelGroupOrderRequest {
  cancelReason: string
}

export interface LockAllocation {
  participantId: number
  userId: number
  originalAmount: number
  discountShareAmount: number
  payableAmount: number
  roundingAdjustmentAmount: number
  paymentStatus: PaymentStatus
}

export interface MealItem {
  id: number
  groupOrderId: number
  participantId: number
  itemName: string
  quantity: number
  unitPrice: number
  subtotalAmount: number
  remark: string
}

export interface Participant {
  id: number
  groupOrderId: number
  user: UserSummary
  originalAmount: number
  discountShareAmount: number
  payableAmount: number
  roundingAdjustmentAmount: number
  paymentStatus: PaymentStatus
  paidMarkTime: string | null
  paidConfirmTime: string | null
  joinTime: string
  remark: string
  mealItems: MealItem[]
}

export interface PickupRecord {
  id: number
  groupOrderId: number
  pickupUser: UserSummary
  pickupLocation: string | null
  pickupStatus: PickupStatus
  estimatedArrivalTime: string | null
  actualArrivalTime: string | null
  pickedUpTime: string | null
  distributedTime: string | null
  remark: string
}

export interface PaymentRecord {
  id: number
  groupOrderId: number
  participantId: number
  userId: number
  amount: number
  paymentStatus: PaymentStatus
  markTime: string | null
  confirmUserId: number | null
  confirmTime: string | null
  remark: string | null
}

export interface GroupOrderSummary {
  id: number
  title: string
  orderType: OrderType
  merchantName: string
  shopName?: string
  pickupLocation: string
  creator: UserSummary
  deadlineTime: string
  maxParticipants: number
  participantCount: number
  minAmount?: number | string | null
  discountThresholdAmount: number | null
  discountAmount: number
  originalTotalAmount: number
  actualDiscountAmount: number
  payableTotalAmount: number
  roundingAdjustmentAmount: number
  status: GroupOrderStatus
  joinable?: boolean
  progressPercent?: number
  remainingSeconds?: number
  lastEventSummary?: string | null
  canViewDetail?: boolean
  viewable?: boolean
  pickupUser: UserSummary | null
  remark: string
  lockedTime: string | null
  finishTime: string | null
  cancelTime: string | null
  cancelReason?: string | null
  expiredTime?: string | null
  expireReason?: string | null
  createTime: string
  updateTime: string
}

export interface CancelGroupOrderResponse {
  id: number
  status: GroupOrderStatus
  cancelReason: string | null
  cancelTime: string | null
}

export interface GroupOrderPermissions {
  canJoin?: boolean
  canLock?: boolean
  canCancel?: boolean
  canMarkPayment?: boolean
  canConfirmPayment?: boolean
  canAssignPickupUser?: boolean
  canUpdatePickupStatus?: boolean
  canCreateEvent?: boolean
  canViewEvents?: boolean
}

export interface PickupSnapshot {
  pickupStatus: PickupStatus
  pickupLocation: string | null
  pickupUser?: UserSummary | null
}

export interface GroupOrderEvent {
  id: number
  eventType: GroupOrderEventType
  eventLevel: GroupOrderEventLevel
  operatorId: number | null
  operatorRole: string | null
  title: string
  content: string
  eventTime: string
}

export interface GroupOrderDetail {
  order: GroupOrderSummary
  participants: Participant[]
  pickupRecord: PickupRecord | null
  pickup?: PickupSnapshot | null
  permissions?: GroupOrderPermissions
  currentUserRole?: 'CREATOR' | 'PARTICIPANT' | 'PICKUP_USER' | 'RELATED' | string
  recentEvents?: GroupOrderEvent[]
}

export interface GroupOrderAmount {
  participantCount: number
  originalTotalAmount: number
  discountThresholdAmount: number | null
  discountAmount: number
  actualDiscountAmount: number
  payableTotalAmount: number
}

export interface JoinGroupOrderResponse {
  participant: Participant
  orderAmount: GroupOrderAmount
}

export interface LockGroupOrderResponse {
  order: Pick<
    GroupOrderSummary,
    | 'id'
    | 'status'
    | 'originalTotalAmount'
    | 'actualDiscountAmount'
    | 'payableTotalAmount'
    | 'roundingAdjustmentAmount'
    | 'lockedTime'
  >
  allocations: LockAllocation[]
}

export interface PaymentActionRequest {
  remark?: string
}

export interface MarkPaymentResponse {
  participantId: number
  paymentStatus: PaymentStatus
  paidMarkTime: string
  paymentRecord: PaymentRecord
}

export interface ConfirmPaymentResponse {
  participantId: number
  paymentStatus: PaymentStatus
  paidConfirmTime: string
  paymentRecord: PaymentRecord
}

export interface AssignPickupRequest {
  pickupUserId: number
  pickupLocation?: string
  estimatedArrivalTime?: string
  remark?: string
}

export interface AssignPickupResponse {
  orderId: number
  pickupUserId: number
  pickupRecord: PickupRecord
}

export interface UpdatePickupStatusRequest {
  pickupStatus: PickupStatus
  pickupLocation?: string
  actualArrivalTime?: string
  pickedUpTime?: string
  distributedTime?: string
  remark?: string
}

export interface UpdatePickupStatusResponse {
  pickupRecord: PickupRecord
  orderStatus: GroupOrderStatus
}

export interface MyGroupOrderRecord {
  order: Pick<
    GroupOrderSummary,
    | 'id'
    | 'title'
    | 'orderType'
    | 'merchantName'
    | 'pickupLocation'
    | 'deadlineTime'
    | 'participantCount'
    | 'status'
    | 'payableTotalAmount'
    | 'pickupUser'
  >
  myRole: 'CREATOR' | 'PARTICIPANT' | 'PICKUP_USER' | 'PICKUP' | 'RELATED'
  myParticipantId: number | null
  myPayableAmount: number | null
  myPaymentStatus: PaymentStatus | null
  pickupStatus: PickupStatus | null
}

export interface DashboardRankItem {
  name: string
  count: number
}

export interface DashboardSummary {
  todayOrderCount: number
  successOrderCount: number
  totalSavedAmount: number
  orderCount: number
  createdCount: number
  lockedCount: number
  finishedCount: number
  cancelledCount: number
  participantCount: number
  originalTotalAmount: number
  actualDiscountAmount: number
  payableTotalAmount: number
  paidParticipantCount: number
  confirmedParticipantCount: number
  popularTypes: DashboardRankItem[]
  popularMerchants: DashboardRankItem[]
}
