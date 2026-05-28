<script setup lang="ts">
import { Discount, Finished, Tickets, Trophy, UserFilled } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'

import AmountStat from '@/components/AmountStat.vue'
import { useOrderStore } from '@/stores/orders'
import { orderTypeText } from '@/utils/format'
import type { OrderType } from '@/types/order'

const orderStore = useOrderStore()
const dashboard = computed(() => orderStore.dashboard)
const loadFailed = ref(false)

const percent = (value: number, total: number) => {
  if (!total) return 0
  return Math.min(100, Math.round((value / total) * 100))
}

const createdPercent = computed(() => percent(dashboard.value?.createdCount ?? 0, dashboard.value?.orderCount ?? 0))
const lockedPercent = computed(() => percent(dashboard.value?.lockedCount ?? 0, dashboard.value?.orderCount ?? 0))
const finishedPercent = computed(() => percent(dashboard.value?.finishedCount ?? 0, dashboard.value?.orderCount ?? 0))

const formatRankName = (name: string) => orderTypeText[name as OrderType] ?? name

const loadDashboard = async () => {
  loadFailed.value = false
  try {
    await orderStore.loadDashboard()
  } catch {
    loadFailed.value = true
  }
}

onMounted(loadDashboard)
</script>

<template>
  <section class="page-stack" v-loading="orderStore.dashboardLoading">
    <div class="page-header">
      <div>
        <h1>数据看板</h1>
        <p>基础统计来自真实接口，展示拼单规模、节省金额和热门偏好。</p>
      </div>
      <div class="header-actions">
        <ElButton @click="loadDashboard">刷新</ElButton>
      </div>
    </div>

    <template v-if="dashboard">
      <div class="dashboard-tiles">
        <div class="metric-tile">
          <ElIcon><Tickets /></ElIcon>
          <span>今日拼单数</span>
          <strong>{{ dashboard.todayOrderCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><Finished /></ElIcon>
          <span>成功拼单数</span>
          <strong>{{ dashboard.successOrderCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><UserFilled /></ElIcon>
          <span>参与人次</span>
          <strong>{{ dashboard.participantCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><Discount /></ElIcon>
          <span>累计节省金额</span>
          <strong>¥{{ Number(dashboard.totalSavedAmount ?? 0).toFixed(2) }}</strong>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-panel detail-panel--wide">
          <div class="panel-title">
            <strong>金额概览</strong>
            <span>金额字段保留两位小数展示</span>
          </div>
          <div class="stat-strip stat-strip--compact">
            <AmountStat label="原始总额" :amount="dashboard.originalTotalAmount" />
            <AmountStat label="优惠总额" :amount="dashboard.actualDiscountAmount" tone="green" />
            <AmountStat label="应付总额" :amount="dashboard.payableTotalAmount" tone="blue" />
          </div>
          <div class="progress-list">
            <div>
              <span>待加入拼单</span>
              <ElProgress :percentage="createdPercent" color="#2f7cf6" />
            </div>
            <div>
              <span>已锁定拼单</span>
              <ElProgress :percentage="lockedPercent" color="#f59f00" />
            </div>
            <div>
              <span>已完成拼单</span>
              <ElProgress :percentage="finishedPercent" color="#21a67a" />
            </div>
          </div>
        </div>
        <aside class="detail-panel">
          <div class="panel-title">
            <strong>热门偏好</strong>
            <span>类型与商家排行</span>
          </div>
          <div class="rank-section">
            <strong>热门类型</strong>
            <div v-if="dashboard.popularTypes.length" class="timeline-list">
              <p v-for="item in dashboard.popularTypes" :key="item.name">
                <ElIcon><Trophy /></ElIcon>
                {{ formatRankName(item.name) }} · {{ item.count }} 单
              </p>
            </div>
            <ElEmpty v-else description="暂无类型统计" />
          </div>
          <div class="rank-section">
            <strong>热门店铺</strong>
            <div v-if="dashboard.popularMerchants.length" class="timeline-list">
              <p v-for="item in dashboard.popularMerchants" :key="item.name">
                <ElIcon><Trophy /></ElIcon>
                {{ item.name }} · {{ item.count }} 单
              </p>
            </div>
            <ElEmpty v-else description="暂无店铺统计" />
          </div>
        </aside>
      </div>
    </template>
    <ElEmpty v-else :description="loadFailed ? '数据加载失败，请稍后重试' : '暂无看板数据'" />
  </section>
</template>
