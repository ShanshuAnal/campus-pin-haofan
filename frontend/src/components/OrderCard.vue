<script setup lang="ts">
import { AlarmClock, CirclePlus, Location, Shop, User, View } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import StatusTag from '@/components/StatusTag.vue'
import type { GroupOrderSummary } from '@/types/order'
import { formatMoney, orderTypeText } from '@/utils/format'

const props = defineProps<{
  order: GroupOrderSummary
}>()

const router = useRouter()
const terminalStatuses = ['FINISHED', 'CANCELLED', 'EXPIRED']

const merchantName = computed(() => props.order.shopName ?? props.order.merchantName)
const thresholdAmount = computed(() => Number(props.order.minAmount ?? props.order.discountThresholdAmount ?? 0))
const totalAmount = computed(() => Number(props.order.originalTotalAmount ?? 0))
const participantPercent = computed(() => {
  const max = Math.max(Number(props.order.maxParticipants || 1), 1)
  return Math.min(Math.round((props.order.participantCount / max) * 100), 100)
})
const discountPercent = computed(() => {
  if (typeof props.order.progressPercent === 'number') {
    return Math.min(Math.max(Math.round(props.order.progressPercent), 0), 100)
  }
  if (thresholdAmount.value <= 0) {
    return props.order.status === 'CREATED' ? participantPercent.value : 100
  }
  return Math.min(Math.round((totalAmount.value / thresholdAmount.value) * 100), 100)
})
const remainingGap = computed(() => Math.max(thresholdAmount.value - totalAmount.value, 0))

const parsedDeadline = computed(() => {
  const raw = props.order.deadlineTime?.includes('T')
    ? props.order.deadlineTime
    : props.order.deadlineTime?.replace(' ', 'T')
  const time = Date.parse(raw)
  return Number.isNaN(time) ? null : time
})
const remainingSeconds = computed(() => {
  if (typeof props.order.remainingSeconds === 'number') {
    return props.order.remainingSeconds
  }
  if (!parsedDeadline.value) {
    return null
  }
  return Math.floor((parsedDeadline.value - Date.now()) / 1000)
})
const isTerminal = computed(() => terminalStatuses.includes(props.order.status))
const isCreatedPastDeadline = computed(() =>
  props.order.status === 'CREATED' && remainingSeconds.value !== null && remainingSeconds.value <= 0
)
const deadlineText = computed(() => {
  const seconds = remainingSeconds.value
  if (props.order.status === 'EXPIRED') return '已超时关闭'
  if (props.order.status === 'CANCELLED') return '已取消'
  if (props.order.status === 'FINISHED') return '已完成'
  if (seconds === null) return props.order.deadlineTime
  if (seconds <= 0) return props.order.status === 'CREATED' ? '已截止待处理' : '已截止'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.ceil((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
})
const canJoin = computed(() => {
  return props.order.status === 'CREATED' && !isCreatedPastDeadline.value && props.order.joinable === true
})
const canViewDetail = computed(() => props.order.canViewDetail !== false && props.order.viewable !== false)
const coverClass = computed(() => `order-card__cover order-card__cover--${props.order.orderType.toLowerCase()}`)
const cardTone = computed(() => {
  if (isTerminal.value || isCreatedPastDeadline.value) return 'terminal'
  return canJoin.value ? 'joinable' : 'muted'
})
const blockedActionText = computed(() => (isTerminal.value || isCreatedPastDeadline.value ? '已关闭' : '不可查看'))
const noticeText = computed(() => {
  if (props.order.status === 'CANCELLED') {
    const reason = props.order.cancelReason || props.order.lastEventSummary || '发起人已取消该拼单'
    return `${reason}${props.order.cancelTime ? ` · ${props.order.cancelTime}` : ''}`
  }
  if (props.order.status === 'EXPIRED') {
    const reason = props.order.expireReason || props.order.lastEventSummary || '系统已超时关闭'
    return `${reason}${props.order.expiredTime ? ` · ${props.order.expiredTime}` : ''}`
  }
  if (props.order.status === 'FINISHED') return '拼单已完成，可查看详情'
  return props.order.lastEventSummary || props.order.remark
})

const goDetail = () => {
  if (!canViewDetail.value) return
  router.push(`/orders/${props.order.id}`)
}
</script>

<template>
  <article class="order-card" :class="`order-card--${cardTone}`">
    <div :class="coverClass">
      <div class="order-card__cover-glow" />
      <span class="order-card__type">{{ orderTypeText[props.order.orderType] }}</span>
      <strong>{{ merchantName }}</strong>
      <small>{{ props.order.pickupLocation }}</small>
    </div>

    <div class="order-card__body">
      <div class="order-card__title">
        <h3>{{ props.order.title }}</h3>
        <ElTag v-if="isCreatedPastDeadline" type="warning" effect="light" round>已截止待处理</ElTag>
        <StatusTag v-else :status="props.order.status" />
      </div>

      <div class="order-card__meta">
        <span><ElIcon><Shop /></ElIcon>{{ merchantName }}</span>
        <span><ElIcon><Location /></ElIcon>{{ props.order.pickupLocation }}</span>
        <span><ElIcon><AlarmClock /></ElIcon>{{ deadlineText }}</span>
      </div>

      <div class="order-card__progress">
        <div>
          <span>人数进度</span>
          <strong>{{ props.order.participantCount }}/{{ props.order.maxParticipants }}</strong>
        </div>
        <ElProgress :percentage="participantPercent" :show-text="false" :stroke-width="8" />
      </div>

      <div class="order-card__progress">
        <div>
          <span>满减进度</span>
          <strong v-if="thresholdAmount > 0">还差 {{ formatMoney(remainingGap) }}</strong>
          <strong v-else>无需门槛</strong>
        </div>
        <ElProgress :percentage="discountPercent" :show-text="false" :stroke-width="8" color="#42b883" />
      </div>

      <p v-if="noticeText" class="order-card__notice">{{ noticeText }}</p>

      <div class="order-card__footer">
        <div>
          <span>当前应付</span>
          <strong>{{ formatMoney(props.order.payableTotalAmount) }}</strong>
          <small>已优惠 {{ formatMoney(props.order.actualDiscountAmount) }}</small>
        </div>
        <ElButton v-if="canJoin" type="primary" :icon="CirclePlus" :disabled="!canViewDetail" @click="goDetail">加入拼单</ElButton>
        <ElTooltip v-else-if="!canViewDetail" content="你无权查看该拼单详情" placement="top">
          <ElButton :icon="View" disabled>{{ blockedActionText }}</ElButton>
        </ElTooltip>
        <ElButton v-else :icon="View" @click="goDetail">查看详情</ElButton>
      </div>
    </div>

    <div class="order-card__capacity">
      <ElProgress type="circle" :percentage="participantPercent" :width="58" :stroke-width="7">
        <template #default>
          <span><ElIcon><User /></ElIcon>{{ props.order.participantCount }}/{{ props.order.maxParticipants }}</span>
        </template>
      </ElProgress>
    </div>
  </article>
</template>

<style scoped>
.order-card {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: 178px minmax(0, 1fr);
  gap: 18px;
  min-height: 232px;
  padding: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 42px rgba(31, 41, 55, 0.12);
}

.order-card--joinable {
  border-color: rgba(66, 153, 225, 0.24);
}

.order-card--terminal {
  border-color: rgba(148, 163, 184, 0.28);
  background: rgba(248, 250, 252, 0.9);
}

.order-card--terminal .order-card__cover {
  filter: saturate(0.72);
}

.order-card__cover {
  position: relative;
  display: grid;
  align-content: end;
  min-height: 208px;
  padding: 16px;
  overflow: hidden;
  border-radius: 14px;
  color: #fff;
  background:
    radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.42), transparent 26%),
    linear-gradient(135deg, #2f80ed, #56ccf2);
}

.order-card__cover--canteen {
  background:
    radial-gradient(circle at 76% 24%, rgba(255, 255, 255, 0.4), transparent 25%),
    linear-gradient(135deg, #19a974, #f2c94c);
}

.order-card__cover--milk_tea {
  background:
    radial-gradient(circle at 28% 18%, rgba(255, 255, 255, 0.45), transparent 25%),
    linear-gradient(135deg, #b7791f, #f6ad55);
}

.order-card__cover--midnight_snack {
  background:
    radial-gradient(circle at 72% 20%, rgba(255, 255, 255, 0.38), transparent 24%),
    linear-gradient(135deg, #805ad5, #ed64a6);
}

.order-card__cover-glow {
  position: absolute;
  inset: auto -36px -42px auto;
  width: 120px;
  height: 120px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
}

.order-card__type {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 12px;
  font-weight: 700;
}

.order-card__cover strong,
.order-card__cover small {
  position: relative;
  z-index: 1;
}

.order-card__cover strong {
  font-size: 20px;
  line-height: 1.25;
}

.order-card__cover small {
  margin-top: 8px;
  opacity: 0.88;
}

.order-card__body {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 4px 78px 4px 0;
}

.order-card__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.order-card h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.35;
}

.order-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: #667085;
  font-size: 13px;
}

.order-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.order-card__progress {
  display: grid;
  gap: 7px;
}

.order-card__progress div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #667085;
  font-size: 13px;
}

.order-card__progress strong {
  color: #142033;
}

.order-card__notice {
  margin: 0;
  padding: 9px 11px;
  border-radius: 10px;
  color: #6b4e16;
  background: rgba(255, 248, 225, 0.86);
  font-size: 13px;
  line-height: 1.45;
}

.order-card__footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: 2px;
}

.order-card__footer span,
.order-card__footer small {
  display: block;
  color: #667085;
  font-size: 12px;
}

.order-card__footer strong {
  display: block;
  margin: 3px 0;
  font-size: 24px;
}

.order-card__capacity {
  position: absolute;
  right: 18px;
  top: 86px;
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 12px 28px rgba(31, 41, 55, 0.12);
}

.order-card__capacity span {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #142033;
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .order-card {
    grid-template-columns: 1fr;
  }

  .order-card__cover {
    min-height: 150px;
  }

  .order-card__body {
    padding-right: 0;
  }

  .order-card__capacity {
    top: 118px;
  }
}
</style>
