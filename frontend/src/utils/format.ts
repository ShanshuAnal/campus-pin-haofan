import type { GroupOrderStatus, OrderType, PaymentStatus, PickupStatus } from '@/types/order'

export const formatMoney = (amount: number | null | undefined) => `¥${Number(amount ?? 0).toFixed(2)}`

export const orderTypeText: Record<OrderType, string> = {
  TAKEOUT: '外卖拼单',
  CANTEEN: '食堂凑单',
  MILK_TEA: '奶茶拼单',
  MIDNIGHT_SNACK: '夜宵拼单'
}

export const orderStatusText: Record<GroupOrderStatus, string> = {
  CREATED: '待加入',
  LOCKED: '已锁单',
  ORDERED: '已下单',
  DELIVERING: '配送中',
  ARRIVED: '已到达',
  PICKED_UP: '已取餐',
  FINISHED: '已完成',
  CANCELLED: '已取消',
  EXPIRED: '已超时关闭'
}

export const paymentStatusText: Record<PaymentStatus, string> = {
  UNPAID: '待模拟支付',
  PAID: '已模拟支付',
  ESCROWED: '托管中',
  CONFIRMED: '发起人已确认',
  SETTLED: '已结算给发起人',
  REFUNDED: '已模拟退款'
}

export const pickupStatusText: Record<PickupStatus, string> = {
  WAITING_ORDER: '待下单',
  WAITING_DELIVERY: '待配送',
  ARRIVED: '已到达',
  PICKED_UP: '已取餐',
  DISTRIBUTED: '已分发'
}
