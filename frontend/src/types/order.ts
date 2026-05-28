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

export type PaymentStatus = 'UNPAID' | 'PAID' | 'CONFIRMED' | 'REFUNDED'

export type PickupStatus =
  | 'WAITING_ORDER'
  | 'WAITING_DELIVERY'
  | 'ARRIVED'
  | 'PICKED_UP'
  | 'DISTRIBUTED'

export type OrderType = 'TAKEOUT' | 'CANTEEN' | 'MILK_TEA' | 'MIDNIGHT_SNACK'

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

export interface GroupOrderSummary {
  id: number
  title: string
  orderType: OrderType
  merchantName: string
  pickupLocation: string
  creator: UserSummary
  deadlineTime: string
  maxParticipants: number
  participantCount: number
  discountThresholdAmount: number
  discountAmount: number
  originalTotalAmount: number
  actualDiscountAmount: number
  payableTotalAmount: number
  roundingAdjustmentAmount: number
  status: GroupOrderStatus
  pickupUser: UserSummary | null
  remark: string
  lockedTime: string | null
  finishTime: string | null
  cancelTime: string | null
  createTime: string
  updateTime: string
}

export interface GroupOrderDetail {
  order: GroupOrderSummary
  participants: Participant[]
  pickupRecord: PickupRecord | null
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
  myRole: 'CREATOR' | 'PARTICIPANT' | 'PICKUP_USER'
  myParticipantId: number | null
  myPayableAmount: number
  myPaymentStatus: PaymentStatus | null
  pickupStatus: PickupStatus | null
}

export interface DashboardSummary {
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
}
