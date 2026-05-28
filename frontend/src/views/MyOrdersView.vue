<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import StatusTag from '@/components/StatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import type { MyGroupOrderRecord } from '@/types/order'
import { formatMoney, orderTypeText, paymentStatusText, pickupStatusText } from '@/utils/format'

const orderStore = useOrderStore()
const router = useRouter()

const roleText = {
  CREATOR: '我发起',
  PARTICIPANT: '我参与',
  PICKUP_USER: '我取餐'
}

const formatOrderMeta = (row: MyGroupOrderRecord) =>
  `${row.order.merchantName} · ${orderTypeText[row.order.orderType]}`

const formatPayment = (row: MyGroupOrderRecord) =>
  row.myPaymentStatus ? paymentStatusText[row.myPaymentStatus] : '-'

const formatPickup = (row: MyGroupOrderRecord) => (row.pickupStatus ? pickupStatusText[row.pickupStatus] : '-')

onMounted(orderStore.loadMyOrders)
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>我的拼单</h1>
        <p>汇总我发起、参与和负责取餐的拼单，便于课堂演示状态流转。</p>
      </div>
    </div>

    <div class="table-panel">
      <ElTable :data="orderStore.myOrders" stripe>
        <ElTableColumn label="拼单" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.order.title }}</strong>
            <small class="muted-text">{{ formatOrderMeta(row) }}</small>
          </template>
        </ElTableColumn>
        <ElTableColumn label="角色" width="100">
          <template #default="{ row }">{{ roleText[row.myRole as keyof typeof roleText] }}</template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="120">
          <template #default="{ row }"><StatusTag :status="row.order.status" /></template>
        </ElTableColumn>
        <ElTableColumn label="我的应付" width="120">
          <template #default="{ row }">{{ formatMoney(row.myPayableAmount) }}</template>
        </ElTableColumn>
        <ElTableColumn label="付款" width="120">
          <template #default="{ row }">
            {{ formatPayment(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="取餐" width="120">
          <template #default="{ row }">
            {{ formatPickup(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link @click="router.push(`/orders/${row.order.id}`)">详情</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
  </section>
</template>
