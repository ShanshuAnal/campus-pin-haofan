<script setup lang="ts">
import type { GroupOrderStatus, PaymentStatus, PickupStatus } from '@/types/order'
import { orderStatusText, paymentStatusText, pickupStatusText } from '@/utils/format'

const props = defineProps<{
  status: GroupOrderStatus | PaymentStatus | PickupStatus
  type?: 'order' | 'payment' | 'pickup'
}>()

const tagTypeMap: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'primary'> = {
  CREATED: 'primary',
  LOCKED: 'warning',
  ORDERED: 'warning',
  DELIVERING: 'warning',
  ARRIVED: 'success',
  PICKED_UP: 'success',
  FINISHED: 'info',
  CANCELLED: 'danger',
  EXPIRED: 'info',
  UNPAID: 'danger',
  PAID: 'warning',
  ESCROWED: 'warning',
  CONFIRMED: 'success',
  SETTLED: 'success',
  REFUNDED: 'info',
  WAITING_ORDER: 'info',
  WAITING_DELIVERY: 'warning',
  DISTRIBUTED: 'success'
}

const labelMap = {
  order: orderStatusText,
  payment: paymentStatusText,
  pickup: pickupStatusText
}
</script>

<template>
  <ElTag :type="tagTypeMap[status]" effect="light" round>
    {{ labelMap[type ?? 'order'][status as never] }}
  </ElTag>
</template>
