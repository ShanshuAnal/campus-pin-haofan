<script setup lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import AmountStat from '@/components/AmountStat.vue'
import OrderCard from '@/components/OrderCard.vue'
import { useOrderStore } from '@/stores/orders'

const orderStore = useOrderStore()
const router = useRouter()

onMounted(() => orderStore.loadOrders())
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>拼单大厅</h1>
        <p>查看正在凑单和已锁定的校园拼单，支持按状态、类型和关键词查询。</p>
      </div>
      <ElButton type="primary" :icon="Plus" @click="router.push('/orders/new')">发起拼单</ElButton>
    </div>

    <div class="stat-strip">
      <AmountStat label="当前应付" :amount="181" tone="blue" />
      <AmountStat label="已节省" :amount="22" tone="green" />
      <AmountStat label="待确认" :amount="39.76" tone="orange" />
    </div>

    <div class="toolbar">
      <ElInput
        v-model="orderStore.keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索标题或商家"
        @keyup.enter="orderStore.resetAndLoadOrders"
        @clear="orderStore.resetAndLoadOrders"
      />
      <ElSelect v-model="orderStore.status" clearable placeholder="拼单状态" @change="orderStore.resetAndLoadOrders">
        <ElOption label="待加入" value="CREATED" />
        <ElOption label="已锁单" value="LOCKED" />
        <ElOption label="已下单" value="ORDERED" />
        <ElOption label="配送中" value="DELIVERING" />
        <ElOption label="已到达" value="ARRIVED" />
        <ElOption label="已取餐" value="PICKED_UP" />
        <ElOption label="已完成" value="FINISHED" />
        <ElOption label="已取消" value="CANCELLED" />
      </ElSelect>
      <ElSelect v-model="orderStore.orderType" clearable placeholder="拼单类型" @change="orderStore.resetAndLoadOrders">
        <ElOption label="外卖拼单" value="TAKEOUT" />
        <ElOption label="食堂凑单" value="CANTEEN" />
        <ElOption label="奶茶拼单" value="MILK_TEA" />
        <ElOption label="夜宵拼单" value="MIDNIGHT_SNACK" />
      </ElSelect>
      <ElButton type="primary" :icon="Search" @click="orderStore.resetAndLoadOrders">查询</ElButton>
      <ElButton :icon="Refresh" @click="orderStore.loadOrders()">刷新</ElButton>
    </div>

    <div v-loading="orderStore.loading" class="order-list">
      <OrderCard v-for="order in orderStore.filteredOrders" :key="order.id" :order="order" />
      <ElEmpty v-if="!orderStore.filteredOrders.length" description="暂无匹配拼单" />
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
