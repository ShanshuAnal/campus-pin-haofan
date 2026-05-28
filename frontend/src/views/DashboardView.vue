<script setup lang="ts">
import { DataLine, Discount, Finished, Tickets, UserFilled } from '@element-plus/icons-vue'
import { computed, onMounted } from 'vue'

import AmountStat from '@/components/AmountStat.vue'
import { useOrderStore } from '@/stores/orders'

const orderStore = useOrderStore()
const dashboard = computed(() => orderStore.dashboard)

onMounted(orderStore.loadDashboard)
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>数据看板</h1>
        <p>基础统计面向课堂演示，后续接入 GET /api/dashboard/summary。</p>
      </div>
    </div>

    <template v-if="dashboard">
      <div class="dashboard-tiles">
        <div class="metric-tile">
          <ElIcon><Tickets /></ElIcon>
          <span>拼单总数</span>
          <strong>{{ dashboard.orderCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><Finished /></ElIcon>
          <span>已完成</span>
          <strong>{{ dashboard.finishedCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><UserFilled /></ElIcon>
          <span>参与人次</span>
          <strong>{{ dashboard.participantCount }}</strong>
        </div>
        <div class="metric-tile">
          <ElIcon><Discount /></ElIcon>
          <span>确认付款</span>
          <strong>{{ dashboard.confirmedParticipantCount }}</strong>
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
              <ElProgress :percentage="25" color="#2f7cf6" />
            </div>
            <div>
              <span>已锁定拼单</span>
              <ElProgress :percentage="17" color="#f59f00" />
            </div>
            <div>
              <span>已完成拼单</span>
              <ElProgress :percentage="42" color="#21a67a" />
            </div>
          </div>
        </div>
        <aside class="detail-panel">
          <div class="panel-title">
            <strong>演示重点</strong>
            <span>围绕 MVP 主流程</span>
          </div>
          <div class="timeline-list">
            <p><ElIcon><DataLine /></ElIcon> 登录后进入拼单大厅</p>
            <p><ElIcon><DataLine /></ElIcon> 发起拼单并等待成员加入</p>
            <p><ElIcon><DataLine /></ElIcon> 锁单后展示优惠分摊</p>
            <p><ElIcon><DataLine /></ElIcon> 标记付款并推进取餐状态</p>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>
