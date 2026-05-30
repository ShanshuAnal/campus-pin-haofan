<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import StatusTag from '@/components/StatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import type { MyGroupOrderRecord, MyGroupOrderScope } from '@/types/order'
import { formatMoney, orderTypeText, paymentStatusText, pickupStatusText } from '@/utils/format'

const orderStore = useOrderStore()
const router = useRouter()
const activeScope = ref<MyGroupOrderScope>('CREATED_BY_ME')
const loadFailed = ref(false)

const roleText = {
  CREATOR: '我发起',
  PARTICIPANT: '我参与',
  PICKUP_USER: '我取餐',
  PICKUP: '我取餐',
  RELATED: '相关拼单'
}

const scopeTabs: Array<{ label: string; value: MyGroupOrderScope }> = [
  { label: '我发起的拼单', value: 'CREATED_BY_ME' },
  { label: '我参与的拼单', value: 'JOINED_BY_ME' },
  { label: '待模拟支付拼单', value: 'PENDING_PAYMENT' },
  { label: '历史拼单', value: 'HISTORY' }
]

const emptyText = computed(() => (loadFailed.value ? '加载失败，请稍后重试' : '暂无相关拼单'))

const formatOrderMeta = (row: MyGroupOrderRecord) =>
  `${row.order.merchantName} · ${orderTypeText[row.order.orderType]}`

const formatPayment = (row: MyGroupOrderRecord) =>
  row.myPaymentStatus ? paymentStatusText[row.myPaymentStatus] : '-'

const formatPickup = (row: MyGroupOrderRecord) => (row.pickupStatus ? pickupStatusText[row.pickupStatus] : '-')

const loadMyOrders = async (pageNum = 1) => {
  loadFailed.value = false
  try {
    await orderStore.loadMyOrders({
      scope: activeScope.value,
      pageNum,
      pageSize: orderStore.myOrdersPageSize
    })
  } catch {
    loadFailed.value = true
  }
}

const changeScope = () => loadMyOrders(1)

const changePage = (pageNum: number) => loadMyOrders(pageNum)

onMounted(() => loadMyOrders())
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>我的拼单</h1>
        <p>汇总我发起、参与和负责取餐的拼单，便于跟进模拟支付与取餐状态。</p>
      </div>
    </div>

    <div class="table-panel" v-loading="orderStore.myOrdersLoading">
      <ElTabs v-model="activeScope" @tab-change="changeScope">
        <ElTabPane v-for="tab in scopeTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </ElTabs>

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
        <ElTableColumn label="模拟支付" width="140">
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
        <template #empty>
          <ElEmpty :description="emptyText" />
        </template>
      </ElTable>

      <div class="pager">
        <ElPagination
          background
          layout="prev, pager, next"
          :current-page="orderStore.myOrdersPageNum"
          :page-size="orderStore.myOrdersPageSize"
          :total="orderStore.myOrdersTotal"
          @current-change="changePage"
        />
      </div>
    </div>
  </section>
</template>
