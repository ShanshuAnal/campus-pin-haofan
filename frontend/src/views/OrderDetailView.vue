<script setup lang="ts">
import { Check, Location, Money, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import AmountStat from '@/components/AmountStat.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import type { Participant } from '@/types/order'
import { formatMoney, orderTypeText, paymentStatusText, pickupStatusText } from '@/utils/format'

const route = useRoute()
const orderStore = useOrderStore()
const detail = computed(() => orderStore.currentDetail)

const runMockAction = (action: string) => {
  ElMessage.success(`${action}已模拟完成，真实联调时调用对应 API`)
}

const formatMealItems = (participant: Participant) =>
  participant.mealItems.map((item) => `${item.itemName} x${item.quantity}`).join('、')

onMounted(() => {
  orderStore.loadDetail(Number(route.params.id))
})
</script>

<template>
  <section class="page-stack" v-loading="orderStore.loading">
    <template v-if="detail">
      <div class="page-header">
        <div>
          <h1>{{ detail.order.title }}</h1>
          <p>{{ detail.order.merchantName }} · {{ orderTypeText[detail.order.orderType] }}</p>
        </div>
        <div class="header-actions">
          <StatusTag :status="detail.order.status" />
          <ElButton :icon="Refresh" @click="orderStore.loadDetail(detail.order.id)">刷新</ElButton>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-panel detail-panel--wide">
          <div class="panel-title">
            <strong>金额分摊</strong>
            <span>锁单后以后端计算结果为准</span>
          </div>
          <div class="stat-strip stat-strip--compact">
            <AmountStat label="原始总额" :amount="detail.order.originalTotalAmount" />
            <AmountStat label="实际优惠" :amount="detail.order.actualDiscountAmount" tone="green" />
            <AmountStat label="应付总额" :amount="detail.order.payableTotalAmount" tone="blue" />
          </div>
          <ElTable :data="detail.participants" stripe>
            <ElTableColumn label="成员" min-width="120">
              <template #default="{ row }">
                <strong>{{ row.user.nickname }}</strong>
                <small class="muted-text">{{ row.user.username }}</small>
              </template>
            </ElTableColumn>
            <ElTableColumn label="餐品" min-width="180">
              <template #default="{ row }">
                {{ formatMealItems(row) }}
              </template>
            </ElTableColumn>
            <ElTableColumn label="原价" width="110">
              <template #default="{ row }">{{ formatMoney(row.originalAmount) }}</template>
            </ElTableColumn>
            <ElTableColumn label="应付" width="110">
              <template #default="{ row }">{{ formatMoney(row.payableAmount) }}</template>
            </ElTableColumn>
            <ElTableColumn label="付款" width="120">
              <template #default="{ row }">
                <StatusTag :status="row.paymentStatus" type="payment" />
              </template>
            </ElTableColumn>
          </ElTable>
        </div>

        <aside class="detail-panel">
          <div class="panel-title">
            <strong>取餐协同</strong>
            <span>指定取餐人后推进状态</span>
          </div>
          <div class="pickup-card">
            <ElIcon><UserFilled /></ElIcon>
            <div>
              <span>取餐人</span>
              <strong>{{ detail.order.pickupUser?.nickname ?? '暂未指定' }}</strong>
            </div>
          </div>
          <div class="pickup-card">
            <ElIcon><Location /></ElIcon>
            <div>
              <span>取餐地点</span>
              <strong>{{ detail.pickupRecord?.pickupLocation ?? detail.order.pickupLocation }}</strong>
            </div>
          </div>
          <div class="pickup-card">
            <ElIcon><Money /></ElIcon>
            <div>
              <span>当前取餐状态</span>
              <strong>
                {{ detail.pickupRecord ? pickupStatusText[detail.pickupRecord.pickupStatus] : '待指定' }}
              </strong>
            </div>
          </div>
          <ElSteps
            :active="detail.pickupRecord?.pickupStatus === 'ARRIVED' ? 2 : detail.pickupRecord?.pickupStatus === 'PICKED_UP' ? 3 : 1"
            finish-status="success"
            direction="vertical"
          >
            <ElStep title="等待下单" />
            <ElStep title="配送/到达" />
            <ElStep title="取餐分发" />
          </ElSteps>
        </aside>
      </div>

      <div class="action-row">
        <ElButton type="primary" :icon="Check" @click="runMockAction('锁定拼单')">锁定拼单</ElButton>
        <ElButton @click="runMockAction('标记付款')">标记付款</ElButton>
        <ElButton @click="runMockAction('确认付款')">确认付款</ElButton>
        <ElButton @click="runMockAction('更新取餐状态')">更新取餐状态</ElButton>
      </div>
    </template>
    <ElEmpty v-else description="未找到拼单详情" />
  </section>
</template>
