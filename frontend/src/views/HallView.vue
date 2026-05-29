<script setup lang="ts">
import { AlarmClock, Location, Plus, Refresh, Search, Shop, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import OrderCard from '@/components/OrderCard.vue'
import { useOrderStore } from '@/stores/orders'
import type { GroupOrderSummary, GroupOrderStatus } from '@/types/order'
import { formatMoney } from '@/utils/format'

type QuickFilter = 'ALL' | 'JOINABLE' | 'ENDING' | 'ACTIVE'

const orderStore = useOrderStore()
const router = useRouter()
const quickFilter = ref<QuickFilter>('ALL')

const terminalStatuses: GroupOrderStatus[] = ['FINISHED', 'CANCELLED', 'EXPIRED']

const parseDeadline = (order: GroupOrderSummary) => {
  const raw = order.deadlineTime?.includes('T') ? order.deadlineTime : order.deadlineTime?.replace(' ', 'T')
  const time = Date.parse(raw)
  return Number.isNaN(time) ? null : time
}

const getRemainingSeconds = (order: GroupOrderSummary) => {
  if (typeof order.remainingSeconds === 'number') {
    return order.remainingSeconds
  }
  const deadline = parseDeadline(order)
  return deadline ? Math.floor((deadline - Date.now()) / 1000) : null
}

const isJoinable = (order: GroupOrderSummary) => {
  if (terminalStatuses.includes(order.status)) {
    return false
  }
  if (typeof order.joinable === 'boolean') {
    return order.joinable
  }
  const remainingSeconds = getRemainingSeconds(order)
  return (
    order.status === 'CREATED' &&
    order.participantCount < order.maxParticipants &&
    (remainingSeconds === null || remainingSeconds > 0)
  )
}

const isEndingSoon = (order: GroupOrderSummary) => {
  const remainingSeconds = getRemainingSeconds(order)
  return isJoinable(order) && remainingSeconds !== null && remainingSeconds > 0 && remainingSeconds <= 1800
}

const visibleOrders = computed(() => {
  const orders = orderStore.filteredOrders.filter((order) => order.canViewDetail !== false && order.viewable !== false)
  if (quickFilter.value === 'JOINABLE') {
    return orders.filter(isJoinable)
  }
  if (quickFilter.value === 'ENDING') {
    return orders.filter(isEndingSoon)
  }
  if (quickFilter.value === 'ACTIVE') {
    return orders.filter((order) => !terminalStatuses.includes(order.status))
  }
  return orders
})

const joinableCount = computed(() => orderStore.filteredOrders.filter(isJoinable).length)
const endingSoonCount = computed(() => orderStore.filteredOrders.filter(isEndingSoon).length)
const activeCount = computed(() => orderStore.filteredOrders.filter((order) => !terminalStatuses.includes(order.status)).length)
const savedAmount = computed(() =>
  orderStore.filteredOrders.reduce((sum, order) => sum + Number(order.actualDiscountAmount ?? 0), 0)
)
const payableAmount = computed(() =>
  orderStore.filteredOrders.reduce((sum, order) => sum + Number(order.payableTotalAmount ?? 0), 0)
)

const resetFilters = async () => {
  quickFilter.value = 'ALL'
  orderStore.keyword = ''
  orderStore.status = ''
  orderStore.orderType = ''
  await orderStore.resetAndLoadOrders()
}

onMounted(() => orderStore.loadOrders())
</script>

<template>
  <section class="hall-page">
    <div class="hall-hero">
      <div class="hall-hero__content">
        <span class="hall-hero__eyebrow">校园拼单大厅</span>
        <h1>今天想吃什么，一起凑满减</h1>
        <p>按取餐点、截止时间和人数进度快速找到可加入的拼单。</p>
        <div class="hall-search">
          <ElInput
            v-model="orderStore.keyword"
            :prefix-icon="Search"
            size="large"
            clearable
            placeholder="搜索拼单项目或商家..."
            @keyup.enter="orderStore.resetAndLoadOrders"
            @clear="orderStore.resetAndLoadOrders"
          />
          <ElButton type="primary" size="large" :icon="Search" @click="orderStore.resetAndLoadOrders">搜索</ElButton>
        </div>
      </div>
      <div class="hall-wallet">
        <span>拼单钱包</span>
        <strong>{{ formatMoney(payableAmount) }}</strong>
        <small>已一起省下 {{ formatMoney(savedAmount) }}</small>
      </div>
    </div>

    <div class="hall-overview">
      <div class="overview-card overview-card--blue">
        <ElIcon><Shop /></ElIcon>
        <span>进行中</span>
        <strong>{{ activeCount }}</strong>
      </div>
      <div class="overview-card overview-card--green">
        <ElIcon><User /></ElIcon>
        <span>可加入</span>
        <strong>{{ joinableCount }}</strong>
      </div>
      <div class="overview-card overview-card--orange">
        <ElIcon><AlarmClock /></ElIcon>
        <span>30 分钟内截止</span>
        <strong>{{ endingSoonCount }}</strong>
      </div>
      <div class="overview-card overview-card--pink">
        <ElIcon><Location /></ElIcon>
        <span>本页拼单</span>
        <strong>{{ orderStore.total }}</strong>
      </div>
    </div>

    <div class="hall-toolbar">
      <ElRadioGroup v-model="quickFilter" size="large">
        <ElRadioButton label="ALL">全部</ElRadioButton>
        <ElRadioButton label="JOINABLE">可加入</ElRadioButton>
        <ElRadioButton label="ENDING">快截止</ElRadioButton>
        <ElRadioButton label="ACTIVE">进行中</ElRadioButton>
      </ElRadioGroup>

      <div class="hall-toolbar__filters">
        <ElSelect v-model="orderStore.status" clearable placeholder="状态" @change="orderStore.resetAndLoadOrders">
          <ElOption label="待加入" value="CREATED" />
          <ElOption label="已锁单" value="LOCKED" />
          <ElOption label="已下单" value="ORDERED" />
          <ElOption label="配送中" value="DELIVERING" />
          <ElOption label="已到达" value="ARRIVED" />
          <ElOption label="已取餐" value="PICKED_UP" />
          <ElOption label="已完成" value="FINISHED" />
          <ElOption label="已取消" value="CANCELLED" />
          <ElOption label="已过期" value="EXPIRED" />
        </ElSelect>
        <ElSelect v-model="orderStore.orderType" clearable placeholder="类型" @change="orderStore.resetAndLoadOrders">
          <ElOption label="外卖拼单" value="TAKEOUT" />
          <ElOption label="食堂凑单" value="CANTEEN" />
          <ElOption label="奶茶拼单" value="MILK_TEA" />
          <ElOption label="夜宵拼单" value="MIDNIGHT_SNACK" />
        </ElSelect>
        <ElButton :icon="Refresh" @click="orderStore.loadOrders()">刷新</ElButton>
        <ElButton type="primary" :icon="Plus" @click="router.push('/orders/new')">发起拼单</ElButton>
      </div>
    </div>

    <div v-loading="orderStore.loading" class="hall-feed">
      <OrderCard v-for="order in visibleOrders" :key="order.id" :order="order" />
      <ElEmpty v-if="!visibleOrders.length" description="暂无匹配拼单">
        <ElButton type="primary" @click="resetFilters">查看全部拼单</ElButton>
      </ElEmpty>
    </div>

    <ElPagination
      class="pager"
      background
      layout="total, sizes, prev, pager, next"
      :total="orderStore.total"
      :current-page="orderStore.pageNum"
      :page-size="orderStore.pageSize"
      :page-sizes="[5, 10, 20]"
      @current-change="orderStore.loadOrders"
      @size-change="orderStore.changePageSize"
    />
  </section>
</template>

<style scoped>
.hall-page {
  position: relative;
  display: grid;
  gap: 22px;
  max-width: 1180px;
  margin: 0 auto;
}

.hall-page::before {
  content: "";
  position: fixed;
  inset: 68px 0 0 232px;
  z-index: -1;
  background:
    radial-gradient(circle at 12% 18%, rgba(86, 204, 242, 0.18), transparent 30%),
    radial-gradient(circle at 86% 10%, rgba(255, 163, 177, 0.24), transparent 28%),
    linear-gradient(135deg, #f5fbff 0%, #fff8f5 100%);
}

.hall-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 22px;
  align-items: stretch;
}

.hall-hero__content,
.hall-wallet,
.overview-card,
.hall-toolbar {
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 22px 56px rgba(31, 41, 55, 0.1);
  backdrop-filter: blur(14px);
}

.hall-hero__content {
  position: relative;
  overflow: hidden;
  padding: 30px;
  background:
    radial-gradient(circle at 24% 5%, rgba(86, 204, 242, 0.34), transparent 26%),
    radial-gradient(circle at 92% 82%, rgba(255, 163, 177, 0.42), transparent 30%),
    linear-gradient(135deg, rgba(238, 248, 255, 0.92), rgba(255, 245, 247, 0.9));
}

.hall-hero__eyebrow {
  color: #2f7cf6;
  font-size: 13px;
  font-weight: 800;
}

.hall-hero h1 {
  max-width: 560px;
  margin: 12px 0 10px;
  font-size: 34px;
  line-height: 1.2;
}

.hall-hero p {
  margin: 0 0 24px;
  color: #667085;
}

.hall-search {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto;
  gap: 12px;
  max-width: 680px;
}

.hall-wallet {
  display: grid;
  align-content: center;
  gap: 8px;
  min-height: 190px;
  padding: 28px;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.78), transparent 30%),
    linear-gradient(145deg, rgba(47, 124, 246, 0.88), rgba(33, 166, 122, 0.82));
  color: #fff;
}

.hall-wallet span,
.hall-wallet small {
  opacity: 0.86;
}

.hall-wallet strong {
  font-size: 36px;
  line-height: 1.1;
}

.hall-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.overview-card .el-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 18px;
}

.overview-card span {
  color: #667085;
  font-size: 13px;
}

.overview-card strong {
  font-size: 28px;
}

.overview-card--blue .el-icon {
  color: #2f7cf6;
  background: rgba(47, 124, 246, 0.12);
}

.overview-card--green .el-icon {
  color: #21a67a;
  background: rgba(33, 166, 122, 0.12);
}

.overview-card--orange .el-icon {
  color: #f59f00;
  background: rgba(245, 159, 0, 0.14);
}

.overview-card--pink .el-icon {
  color: #e85d75;
  background: rgba(232, 93, 117, 0.12);
}

.hall-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
}

.hall-toolbar__filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.hall-toolbar__filters .el-select {
  width: 150px;
}

.hall-feed {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  min-height: 260px;
}

.hall-feed :deep(.el-empty) {
  grid-column: 1 / -1;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.pager {
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .hall-hero,
  .hall-feed {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hall-page::before {
    inset-left: 0;
  }

  .hall-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hall-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .hall-toolbar__filters {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .hall-hero__content,
  .hall-wallet {
    padding: 22px;
  }

  .hall-hero h1 {
    font-size: 28px;
  }

  .hall-search,
  .hall-overview {
    grid-template-columns: 1fr;
  }

  .hall-toolbar__filters .el-select,
  .hall-toolbar__filters .el-button {
    width: 100%;
  }
}
</style>
