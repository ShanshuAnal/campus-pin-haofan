<script setup lang="ts">
import { AlarmClock, Location, Shop, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import StatusTag from '@/components/StatusTag.vue'
import type { GroupOrderSummary } from '@/types/order'
import { formatMoney, orderTypeText } from '@/utils/format'

const props = defineProps<{
  order: GroupOrderSummary
}>()

const router = useRouter()
</script>

<template>
  <article class="order-card">
    <div class="order-card__main">
      <div class="order-card__title">
        <h3>{{ props.order.title }}</h3>
        <StatusTag :status="props.order.status" />
      </div>
      <div class="order-card__meta">
        <span><ElIcon><Shop /></ElIcon>{{ props.order.merchantName }}</span>
        <span><ElIcon><Location /></ElIcon>{{ props.order.pickupLocation }}</span>
        <span><ElIcon><User /></ElIcon>{{ props.order.participantCount }}/{{ props.order.maxParticipants }}</span>
        <span><ElIcon><AlarmClock /></ElIcon>{{ props.order.deadlineTime }}</span>
      </div>
      <p>{{ props.order.remark }}</p>
    </div>
    <div class="order-card__side">
      <span>{{ orderTypeText[props.order.orderType] }}</span>
      <strong>{{ formatMoney(props.order.payableTotalAmount) }}</strong>
      <small>已优惠 {{ formatMoney(props.order.actualDiscountAmount) }}</small>
      <ElButton type="primary" @click="router.push(`/orders/${props.order.id}`)">查看详情</ElButton>
    </div>
  </article>
</template>
