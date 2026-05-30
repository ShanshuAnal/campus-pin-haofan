<script setup lang="ts">
import {
  AlarmClock,
  Check,
  CirclePlus,
  Close,
  Dish,
  InfoFilled,
  Location,
  Money,
  Refresh,
  Tickets,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import StatusTag from '@/components/StatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import { useUserStore } from '@/stores/user'
import type { GroupOrderEvent, Participant, PickupStatus } from '@/types/order'
import { formatMoney, orderStatusText, orderTypeText, paymentStatusText, pickupStatusText } from '@/utils/format'

const route = useRoute()
const orderStore = useOrderStore()
const userStore = useUserStore()
const detail = computed(() => orderStore.currentDetail)
const events = computed(() => orderStore.currentEvents)
const orderId = computed(() => Number(route.params.id))
const joinFormRef = ref<FormInstance>()
const joinSubmitting = ref(false)
const operationLoading = ref('')
const cancelDialogVisible = ref(false)
const cancelReason = ref('')

const joinForm = reactive({
  itemName: '',
  quantity: 1,
  unitPrice: 0,
  itemRemark: '',
  remark: ''
})

const lockForm = reactive({ remark: '' })

const pickupForm = reactive<{
  pickupUserId?: number
  pickupLocation: string
  estimatedArrivalTime: string
  remark: string
}>({
  pickupUserId: undefined,
  pickupLocation: '',
  estimatedArrivalTime: '',
  remark: ''
})

const pickupStatusForm = reactive({
  pickupLocation: '',
  remark: ''
})

const joinRules: FormRules<typeof joinForm> = {
  itemName: [{ required: true, message: '请填写餐品名称', trigger: 'blur' }],
  quantity: [{ required: true, type: 'number', min: 1, message: '数量必须大于 0', trigger: 'change' }],
  unitPrice: [{ required: true, type: 'number', min: 0.01, message: '单价必须大于 0', trigger: 'change' }]
}

const terminalStatuses = ['FINISHED', 'CANCELLED', 'EXPIRED']
const nextPickupStatusMap: Record<PickupStatus, PickupStatus | null> = {
  WAITING_ORDER: 'WAITING_DELIVERY',
  WAITING_DELIVERY: 'ARRIVED',
  ARRIVED: 'PICKED_UP',
  PICKED_UP: 'DISTRIBUTED',
  DISTRIBUTED: null
}
const pickupFlowSteps: Array<{ status: PickupStatus; label: string }> = [
  { status: 'WAITING_ORDER', label: '等待下单' },
  { status: 'WAITING_DELIVERY', label: '等待配送' },
  { status: 'ARRIVED', label: '已到达' },
  { status: 'PICKED_UP', label: '已取餐' },
  { status: 'DISTRIBUTED', label: '已分发' }
]

const currentUserId = computed(() => userStore.user?.id)
const order = computed(() => detail.value?.order)
const permissions = computed(() => detail.value?.permissions ?? {})
const isTerminal = computed(() => Boolean(order.value && terminalStatuses.includes(order.value.status)))
const isCreator = computed(() => Boolean(order.value && currentUserId.value && order.value.creator.id === currentUserId.value))
const currentParticipant = computed(() =>
  detail.value?.participants.find((participant) => participant.user.id === currentUserId.value)
)
const pickupStatus = computed(() => detail.value?.pickupRecord?.pickupStatus ?? detail.value?.pickup?.pickupStatus ?? null)
const pickupLocation = computed(() => detail.value?.pickupRecord?.pickupLocation ?? detail.value?.pickup?.pickupLocation ?? order.value?.pickupLocation ?? '')
const pickupUser = computed(() => detail.value?.pickupRecord?.pickupUser ?? detail.value?.pickup?.pickupUser ?? order.value?.pickupUser ?? null)
const isPickupUser = computed(() => Boolean(currentUserId.value && pickupUser.value?.id === currentUserId.value))

const parseDeadline = computed(() => {
  const value = order.value?.deadlineTime
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const time = Date.parse(normalized)
  return Number.isNaN(time) ? null : time
})

const remainingSeconds = computed(() => {
  if (typeof order.value?.remainingSeconds === 'number') return order.value.remainingSeconds
  if (!parseDeadline.value) return null
  return Math.floor((parseDeadline.value - Date.now()) / 1000)
})

const isCreatedPastDeadline = computed(() =>
  order.value?.status === 'CREATED' && remainingSeconds.value !== null && remainingSeconds.value <= 0
)
const isOperationClosed = computed(() => isTerminal.value || isCreatedPastDeadline.value)

const deadlineText = computed(() => {
  if (!order.value) return '-'
  if (order.value.status === 'FINISHED') return '已完成'
  if (order.value.status === 'CANCELLED') return '已取消'
  if (order.value.status === 'EXPIRED') return '已超时关闭'
  if (remainingSeconds.value === null) return order.value.deadlineTime
  if (remainingSeconds.value <= 0) return '已截止待处理'
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.ceil((remainingSeconds.value % 3600) / 60)
  return hours > 0 ? `${hours}小时${minutes}分钟` : `${minutes}分钟`
})

const discountThreshold = computed(() => Number(order.value?.minAmount ?? order.value?.discountThresholdAmount ?? 0))
const discountGap = computed(() => Math.max(discountThreshold.value - Number(order.value?.originalTotalAmount ?? 0), 0))
const discountPercent = computed(() => {
  if (typeof order.value?.progressPercent === 'number') return Math.min(Math.max(order.value.progressPercent, 0), 100)
  if (discountThreshold.value <= 0) return 100
  return Math.min(Math.round((Number(order.value?.originalTotalAmount ?? 0) / discountThreshold.value) * 100), 100)
})
const simulatedPaidStatuses = ['PAID', 'ESCROWED', 'CONFIRMED', 'SETTLED']
const confirmedPaymentStatuses = ['CONFIRMED', 'SETTLED']
const confirmablePaymentStatuses = ['PAID', 'ESCROWED']
const paidCount = computed(() =>
  detail.value?.participants.filter((participant) => simulatedPaidStatuses.includes(participant.paymentStatus)).length ?? 0
)
const confirmedCount = computed(() =>
  detail.value?.participants.filter((participant) => confirmedPaymentStatuses.includes(participant.paymentStatus)).length ?? 0
)
const payableTotal = computed(() => Number(order.value?.payableTotalAmount ?? 0))
const actualDiscount = computed(() => Number(order.value?.actualDiscountAmount ?? 0))

const canJoinOrder = computed(() => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canJoin === 'boolean') return permissions.value.canJoin
  return Boolean(
    order.value?.status === 'CREATED' &&
      !currentParticipant.value &&
      order.value.participantCount < order.value.maxParticipants &&
      (remainingSeconds.value === null || remainingSeconds.value > 0)
  )
})
const canLockOrder = computed(() => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canLock === 'boolean') return permissions.value.canLock
  return Boolean(isCreator.value && order.value?.status === 'CREATED')
})
const canAssignPickup = computed(() => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canAssignPickupUser === 'boolean') return permissions.value.canAssignPickupUser
  return Boolean(isCreator.value && detail.value?.participants.length)
})
const canUpdatePickup = computed(() => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canUpdatePickupStatus === 'boolean') return permissions.value.canUpdatePickupStatus
  return Boolean((isCreator.value || isPickupUser.value) && pickupStatus.value && pickupStatus.value !== 'DISTRIBUTED')
})
const canCancelOrder = computed(() => {
  if (!order.value || isOperationClosed.value) return false
  if (!['CREATED', 'LOCKED'].includes(order.value.status)) return false
  if (typeof permissions.value.canCancel === 'boolean') return permissions.value.canCancel
  return isCreator.value
})
const canViewEvents = computed(() => {
  if (typeof permissions.value.canViewEvents === 'boolean') return permissions.value.canViewEvents
  return Boolean(isCreator.value || currentParticipant.value || isPickupUser.value)
})
const eventsHiddenText = computed(() => {
  if (canViewEvents.value) return ''
  return '事件时间线仅对拼单发起人、参与者和取餐人可见。'
})
const cancelDisabledReason = computed(() => {
  if (!order.value) return ''
  if (isTerminal.value) return '终态拼单不可取消'
  if (isCreatedPastDeadline.value) return '已超过截止时间，等待系统关闭，不能继续取消或推进流程'
  if (['ORDERED', 'DELIVERING', 'ARRIVED', 'PICKED_UP'].includes(order.value.status)) {
    return '已进入履约阶段，普通取消已关闭，请通过事件记录异常并线下协商'
  }
  return ''
})

const nextPickupStatus = computed(() => (pickupStatus.value ? nextPickupStatusMap[pickupStatus.value] : null))
const pickupStepActive = computed(() => {
  if (!pickupStatus.value) return -1
  return Math.max(
    pickupFlowSteps.findIndex((step) => step.status === pickupStatus.value),
    0
  )
})

const flowActive = computed(() => {
  const status = order.value?.status
  if (['CANCELLED', 'EXPIRED'].includes(status ?? '') || isCreatedPastDeadline.value) return 1
  if (status === 'FINISHED') return flowSteps.value.length
  if (['ARRIVED', 'PICKED_UP'].includes(status ?? '') || pickupStatus.value) return 4
  if (paidCount.value > 0 || ['ORDERED', 'DELIVERING'].includes(status ?? '')) return 3
  if (status === 'LOCKED') return 2
  if ((detail.value?.participants.length ?? 0) > 0) return 1
  return 0
})

const flowProcessStatus = computed(() =>
  ['CANCELLED', 'EXPIRED'].includes(order.value?.status ?? '') || isCreatedPastDeadline.value ? 'error' : 'process'
)

const flowSteps = computed(() => {
  const creatorStep = { title: '发起', description: order.value?.creator.nickname ?? '-' }

  if (order.value?.status === 'EXPIRED') {
    return [
      creatorStep,
      { title: '已超时关闭', description: order.value.expiredTime || order.value.expireReason || '暂无' }
    ]
  }

  if (order.value?.status === 'CANCELLED') {
    return [
      creatorStep,
      { title: '已取消', description: order.value.cancelTime || order.value.cancelReason || '暂无' }
    ]
  }

  if (isCreatedPastDeadline.value) {
    return [
      creatorStep,
      { title: '已截止待处理', description: '等待系统关闭' }
    ]
  }

  return [
    creatorStep,
    { title: '加入', description: `${detail.value?.participants.length ?? 0}/${order.value?.maxParticipants ?? 0} 人` },
    { title: '锁单', description: order.value?.lockedTime ? '已生成分摊' : '等待锁单' },
    { title: '模拟支付', description: `${paidCount.value}/${detail.value?.participants.length ?? 0} 已托管` },
    { title: '取餐', description: pickupStatus.value ? pickupStatusText[pickupStatus.value] : '待指定' },
    { title: '完成', description: order.value?.status === 'FINISHED' ? '已完成' : '未完成' }
  ]
})

const terminalTimeText = (label: string, value?: string | null) => `${label}：${value || '暂无'}`

const terminalNotice = computed(() => {
  if (!order.value) return null
  if (order.value.status === 'FINISHED') {
    const time = terminalTimeText('完成时间', order.value.finishTime)
    return { type: 'success', title: '拼单已完成', content: `餐品已分发，模拟支付和取餐主流程已结束。${time}。` }
  }
  if (order.value.status === 'CANCELLED') {
    const reason = order.value.cancelReason || '发起人已取消该拼单'
    const time = terminalTimeText('取消时间', order.value.cancelTime)
    return { type: 'warning', title: '拼单已取消', content: `${reason}。${time}。不能继续加入、锁单、模拟支付或推进取餐。` }
  }
  if (order.value.status === 'EXPIRED') {
    const time = terminalTimeText('关闭时间', order.value.expiredTime)
    return { type: 'info', title: '已超时关闭', content: `${order.value.expireReason || '系统因超过截止时间关闭该拼单'}。${time}。不能继续加入、锁单、模拟支付或推进取餐。` }
  }
  return null
})

const inlineExceptionNotice = computed(() => {
  if (isTerminal.value || !order.value) return null
  if (cancelDisabledReason.value) return cancelDisabledReason.value
  if (order.value.lastEventSummary) return order.value.lastEventSummary
  return null
})

const nextActionTitle = computed(() => {
  if (isTerminal.value) return '主流程已结束'
  if (isCreatedPastDeadline.value) return '已截止待处理'
  return '当前暂无可执行操作'
})

const nextActionText = computed(() => {
  if (!order.value) return ''
  if (order.value.status === 'EXPIRED') return '已超时关闭，不能继续加入、锁单、模拟支付或推进取餐。'
  if (order.value.status === 'CANCELLED') return '已取消，不能继续加入、锁单、模拟支付或推进取餐。'
  if (order.value.status === 'FINISHED') return '已完成，模拟支付和取餐主流程已结束。'
  if (isCreatedPastDeadline.value) return '已超过截止时间，等待系统关闭，不能继续加入、锁单、取消、模拟支付或推进取餐。'
  return '可查看成员、模拟支付、取餐和事件进展。'
})

const eventTypeText: Record<string, string> = {
  CANCEL: '取消',
  CANCELLED: '取消',
  EXPIRED: '超时关闭',
  DELAY: '延迟',
  EXCEPTION: '异常',
  DELAY_REPORTED: '延迟',
  MERCHANT_DELAY: '商家延迟',
  DELIVERY_DELAY: '配送延迟',
  PICKUP_EXCEPTION: '取餐异常',
  ITEM_MISSING: '缺餐',
  CONTACT_FAILED: '联系失败',
  PAYMENT_DISPUTE: '模拟支付争议',
  NOTE: '备注'
}

const eventLevelText: Record<string, string> = {
  INFO: '普通',
  WARN: '提醒',
  ERROR: '异常'
}

const eventDisplayTitle = (event: GroupOrderEvent) => {
  if (event.eventType === 'EXPIRED') return '系统超时关闭'
  if (['CANCEL', 'CANCELLED'].includes(event.eventType)) return '发起人取消拼单'
  return event.title || eventTypeText[event.eventType] || '事件记录'
}

const eventOperatorText = (event: GroupOrderEvent) => {
  const roleText = event.operatorRole && event.operatorRole !== 'SYSTEM' ? `（${event.operatorRole}）` : ''
  if (event.operator?.nickname) return `${event.operator.nickname}${roleText}`
  if (event.operatorName) return `${event.operatorName}${roleText}`
  if (event.operatorRole === 'SYSTEM' || event.operatorId === null) return '系统'
  if (event.operatorId) return `用户 #${event.operatorId}${roleText}`
  return event.operatorRole || '暂无'
}

const eventStatusText = (status?: string | null) => {
  if (!status) return '暂无'
  return orderStatusText[status as keyof typeof orderStatusText] ?? status
}

const eventTagType = (event: GroupOrderEvent) => {
  if (event.eventLevel === 'ERROR') return 'danger'
  if (event.eventLevel === 'WARN') return 'warning'
  if (['CANCEL', 'CANCELLED', 'EXPIRED'].includes(event.eventType)) return 'info'
  return 'primary'
}

const eventLevelTagType = (event: GroupOrderEvent) => {
  if (event.eventLevel === 'ERROR') return 'danger'
  if (event.eventLevel === 'WARN') return 'warning'
  return 'info'
}

const timelineType = (event: GroupOrderEvent) => {
  if (event.eventLevel === 'ERROR') return 'danger'
  if (event.eventLevel === 'WARN') return 'warning'
  if (['CANCEL', 'CANCELLED', 'EXPIRED'].includes(event.eventType)) return 'info'
  return 'primary'
}

const pickupOptions = computed(() => {
  if (!detail.value) return []
  const users = [detail.value.order.creator, ...detail.value.participants.map((participant) => participant.user)]
  return users.filter((user, index, list) => list.findIndex((item) => item.id === user.id) === index)
})

const formatMealItems = (participant: Participant) =>
  participant.mealItems
    .map((item) => `${item.itemName} x${item.quantity} · ${formatMoney(item.subtotalAmount)}`)
    .join(' / ')

const canMarkPayment = (participant: Participant) => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canMarkPayment === 'boolean' && !permissions.value.canMarkPayment) return false
  return Boolean(
    participant.user.id === currentUserId.value &&
      participant.paymentStatus === 'UNPAID' &&
      ['LOCKED', 'ORDERED', 'DELIVERING', 'ARRIVED', 'PICKED_UP'].includes(order.value?.status ?? '')
  )
}

const canConfirmPayment = (participant: Participant) => {
  if (isOperationClosed.value) return false
  if (typeof permissions.value.canConfirmPayment === 'boolean' && !permissions.value.canConfirmPayment) return false
  return Boolean(isCreator.value && confirmablePaymentStatuses.includes(participant.paymentStatus))
}

const isWaitingForLockToPay = (participant: Participant) =>
  !isOperationClosed.value &&
  participant.user.id === currentUserId.value &&
  participant.paymentStatus === 'UNPAID' &&
  order.value?.status === 'CREATED'

const loadEventsIfAllowed = async (id: number) => {
  if (!canViewEvents.value) {
    orderStore.clearEvents()
    return
  }
  await orderStore.loadEvents(id)
}

const refreshCurrentDetail = async () => {
  if (!order.value) return
  await orderStore.loadDetail(order.value.id)
  await loadEventsIfAllowed(order.value.id)
}

const runOrderAction = async (key: string, successMessage: string, action: () => Promise<unknown>) => {
  operationLoading.value = key
  try {
    await action()
    ElMessage.success(successMessage)
    await refreshCurrentDetail()
  } catch {
    // 业务错误由 axios 统一展示，例如权限不足、重复模拟支付、非法状态或取餐状态跳跃。
  } finally {
    operationLoading.value = ''
  }
}

const lockOrder = () => {
  if (!order.value) return
  return runOrderAction('lock', '锁单成功，金额分摊已刷新', () =>
    orderStore.lockOrder(order.value!.id, {
      remark: lockForm.remark.trim() || undefined
    })
  )
}

const cancelOrder = async () => {
  if (!order.value) return
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请填写取消原因')
    return
  }
  operationLoading.value = 'cancel'
  try {
    await orderStore.cancelOrder(order.value.id, {
      cancelReason: cancelReason.value.trim()
    })
    ElMessage.success('拼单已取消')
    cancelDialogVisible.value = false
    cancelReason.value = ''
    await refreshCurrentDetail()
  } catch {
    // 业务错误由 axios 统一展示，例如非发起人取消、已有模拟托管不可取消、履约中不可取消。
  } finally {
    operationLoading.value = ''
  }
}

const markPayment = (participant: Participant) => {
  if (!order.value) return
  return runOrderAction(`mark-${participant.id}`, '已提交模拟支付，进入模拟托管', () =>
    orderStore.markPayment(order.value!.id, participant.id, {
      remark: '前端提交模拟支付'
    })
  )
}

const confirmPayment = (participant: Participant) => {
  if (!order.value) return
  return runOrderAction(`confirm-${participant.id}`, '已确认收款', () =>
    orderStore.confirmPayment(order.value!.id, participant.id, {
      remark: '发起人确认收款'
    })
  )
}

const assignPickupUser = () => {
  if (!order.value || !pickupForm.pickupUserId) {
    ElMessage.warning('请选择取餐人')
    return
  }

  return runOrderAction('assign-pickup', '取餐人已指定', () =>
    orderStore.assignPickupUser(order.value!.id, {
      pickupUserId: pickupForm.pickupUserId as number,
      pickupLocation: pickupForm.pickupLocation.trim() || undefined,
      estimatedArrivalTime: pickupForm.estimatedArrivalTime.trim() || undefined,
      remark: pickupForm.remark.trim() || undefined
    })
  )
}

const updatePickupStatus = () => {
  if (!order.value || !nextPickupStatus.value) return
  const targetStatus = nextPickupStatus.value
  return runOrderAction('pickup-status', `取餐状态已更新为${pickupStatusText[targetStatus]}`, () =>
    orderStore.updatePickupStatus(order.value!.id, {
      pickupStatus: targetStatus,
      pickupLocation: pickupStatusForm.pickupLocation.trim() || undefined,
      remark: pickupStatusForm.remark.trim() || undefined
    })
  )
}

const submitJoin = async () => {
  const valid = await joinFormRef.value?.validate().catch(() => false)
  if (!valid || !order.value) return

  const targetOrderId = order.value.id
  joinSubmitting.value = true
  try {
    await orderStore.joinOrder(targetOrderId, {
      remark: joinForm.remark.trim() || undefined,
      mealItems: [
        {
          itemName: joinForm.itemName.trim(),
          quantity: joinForm.quantity,
          unitPrice: joinForm.unitPrice,
          remark: joinForm.itemRemark.trim() || undefined
        }
      ]
    })
    ElMessage.success('加入拼单成功')
    joinFormRef.value?.resetFields()
    await orderStore.loadDetail(targetOrderId)
    await loadEventsIfAllowed(targetOrderId)
  } catch {
    // 业务错误由 axios 统一展示后端返回的重复加入、锁定、满员、金额非法等提示。
  } finally {
    joinSubmitting.value = false
  }
}

const loadCurrentDetail = async () => {
  if (Number.isFinite(orderId.value)) {
    await orderStore.loadDetail(orderId.value)
    await loadEventsIfAllowed(orderId.value)
  }
}

onMounted(loadCurrentDetail)
watch(() => route.params.id, loadCurrentDetail)
watch(
  detail,
  (value) => {
    if (!value) return
    pickupForm.pickupUserId = pickupUser.value?.id ?? value.participants[0]?.user.id ?? value.order.creator.id
    pickupForm.pickupLocation = pickupLocation.value
    pickupForm.estimatedArrivalTime = value.pickupRecord?.estimatedArrivalTime ?? ''
    pickupForm.remark = value.pickupRecord?.remark ?? ''
    pickupStatusForm.pickupLocation = pickupLocation.value
    pickupStatusForm.remark = value.pickupRecord?.remark ?? ''
  },
  { immediate: true }
)
</script>

<template>
  <section class="detail-flow-page" v-loading="orderStore.loading">
    <template v-if="detail && order">
      <div class="detail-hero">
        <div class="detail-hero__main">
          <div class="detail-hero__title">
            <ElTag v-if="isCreatedPastDeadline" type="warning" effect="light" round>已截止待处理</ElTag>
            <StatusTag v-else :status="order.status" />
            <h1>{{ order.title }}</h1>
            <p>{{ order.merchantName }} · {{ orderTypeText[order.orderType] }}</p>
          </div>
          <div class="detail-hero__facts">
            <span><ElIcon><Location /></ElIcon>{{ order.pickupLocation }}</span>
            <span><ElIcon><AlarmClock /></ElIcon>{{ deadlineText }}</span>
            <span><ElIcon><UserFilled /></ElIcon>{{ order.participantCount }}/{{ order.maxParticipants }} 人</span>
          </div>
        </div>
        <div class="detail-hero__actions">
          <ElButton :icon="Refresh" @click="refreshCurrentDetail">刷新</ElButton>
          <ElButton
            v-if="canCancelOrder"
            type="danger"
            plain
            :icon="Close"
            @click="cancelDialogVisible = true"
          >
            取消拼单
          </ElButton>
          <ElTooltip v-else-if="cancelDisabledReason" :content="cancelDisabledReason" placement="bottom">
            <ElButton type="danger" plain disabled :icon="Close">取消关闭</ElButton>
          </ElTooltip>
          <ElButton
            v-if="canLockOrder"
            type="primary"
            :icon="Check"
            :loading="operationLoading === 'lock'"
            @click="lockOrder"
          >
            锁定拼单
          </ElButton>
          <ElButton v-else-if="canJoinOrder" type="primary" :icon="CirclePlus" @click="joinFormRef?.$el?.scrollIntoView({ behavior: 'smooth' })">
            加入拼单
          </ElButton>
        </div>
      </div>

      <ElAlert
        v-if="terminalNotice"
        class="terminal-alert"
        :type="terminalNotice.type"
        :title="terminalNotice.title"
        :description="terminalNotice.content"
        show-icon
        :closable="false"
      />
      <ElAlert
        v-else-if="inlineExceptionNotice"
        class="terminal-alert"
        type="warning"
        :title="inlineExceptionNotice"
        show-icon
        :closable="false"
      />

      <div class="flow-panel">
        <ElSteps :active="flowActive" finish-status="success" :process-status="flowProcessStatus" align-center>
          <ElStep v-for="step in flowSteps" :key="step.title" :title="step.title" :description="step.description" />
        </ElSteps>
      </div>

      <div class="detail-layout">
        <main class="detail-main">
          <section class="detail-section amount-section">
            <div class="section-title">
              <div>
                <strong>金额与满减</strong>
                <span>锁单后由后端固化优惠分摊</span>
              </div>
              <ElTag :type="discountGap <= 0 ? 'success' : 'warning'">
                {{ discountGap <= 0 ? '已达到满减' : `还差 ${formatMoney(discountGap)}` }}
              </ElTag>
            </div>
            <div class="amount-grid">
              <div class="amount-tile">
                <span>原始总额</span>
                <strong>{{ formatMoney(order.originalTotalAmount) }}</strong>
              </div>
              <div class="amount-tile amount-tile--green">
                <span>已省</span>
                <strong>{{ formatMoney(actualDiscount) }}</strong>
              </div>
              <div class="amount-tile amount-tile--blue">
                <span>应付总额</span>
                <strong>{{ formatMoney(payableTotal) }}</strong>
              </div>
              <div class="amount-tile amount-tile--orange">
                <span>确认收款</span>
                <strong>{{ confirmedCount }}/{{ detail.participants.length }}</strong>
              </div>
            </div>
            <div class="discount-progress">
              <span>满减门槛 {{ formatMoney(discountThreshold) }}</span>
              <ElProgress :percentage="discountPercent" :stroke-width="10" color="#42b883" />
            </div>
          </section>

          <ElAlert
            class="payment-sim-alert"
            type="info"
            title="本系统不接入真实支付，仅模拟拼单资金状态。"
            description="支付到模拟托管、确认收款、模拟结算和模拟退款均为课堂演示状态，不产生真实资金流。"
            show-icon
            :closable="false"
          />

          <section class="detail-section">
            <div class="section-title">
              <div>
                <strong>成员金额与餐品</strong>
                <span>共 {{ detail.participants.length }} 人参与</span>
              </div>
            </div>
            <div class="participant-list">
              <article v-for="participant in detail.participants" :key="participant.id" class="participant-card">
                <div class="participant-card__head">
                  <div>
                    <strong>{{ participant.user.nickname }}</strong>
                    <span>{{ participant.user.username }}</span>
                  </div>
                  <StatusTag :status="participant.paymentStatus" type="payment" />
                </div>
                <div class="meal-line">
                  <ElIcon><Dish /></ElIcon>
                  <span>{{ formatMealItems(participant) || '暂无餐品' }}</span>
                </div>
                <div class="participant-money">
                  <span>原价 {{ formatMoney(participant.originalAmount) }}</span>
                  <span>优惠 {{ formatMoney(participant.discountShareAmount) }}</span>
                  <strong>应付 {{ formatMoney(participant.payableAmount) }}</strong>
                </div>
                <div v-if="participant.remark" class="participant-remark">备注：{{ participant.remark }}</div>
                <div class="participant-actions">
                  <ElButton
                    v-if="canMarkPayment(participant)"
                    size="small"
                    type="primary"
                    :loading="operationLoading === `mark-${participant.id}`"
                    @click="markPayment(participant)"
                  >
                    支付到模拟托管
                  </ElButton>
                  <ElButton
                    v-if="canConfirmPayment(participant)"
                    size="small"
                    type="success"
                    :loading="operationLoading === `confirm-${participant.id}`"
                    @click="confirmPayment(participant)"
                  >
                    确认收款
                  </ElButton>
                  <ElTooltip v-if="isWaitingForLockToPay(participant)" content="锁单生成应付金额后才能提交模拟支付" placement="top">
                    <ElButton size="small" disabled>待锁单</ElButton>
                  </ElTooltip>
                  <span v-if="!canMarkPayment(participant) && !canConfirmPayment(participant) && !isWaitingForLockToPay(participant)" class="muted-text">
                    {{ paymentStatusText[participant.paymentStatus] }}
                  </span>
                </div>
              </article>
            </div>
          </section>

          <section class="detail-section event-section" v-loading="orderStore.eventsLoading">
            <div class="section-title">
              <div>
                <strong>事件时间线</strong>
                <span>完整事件列表来自 /events，取消、超时、延迟和异常事件只做记录</span>
              </div>
              <ElButton v-if="canViewEvents" size="small" :loading="orderStore.eventsLoading" @click="loadEventsIfAllowed(order.id)">刷新事件</ElButton>
            </div>
            <ElAlert
              v-if="!canViewEvents"
              type="info"
              :title="eventsHiddenText"
              show-icon
              :closable="false"
            />
            <ElTimeline v-else-if="events.length" class="event-timeline">
              <ElTimelineItem
                v-for="event in events"
                :key="event.id"
                :timestamp="event.eventTime"
                :type="timelineType(event)"
              >
                <div class="event-title">
                  <strong>{{ eventDisplayTitle(event) }}</strong>
                  <div class="event-tags">
                    <ElTag size="small" :type="eventTagType(event)" effect="light">
                      {{ eventTypeText[event.eventType] ?? event.eventType }}
                    </ElTag>
                    <ElTag size="small" :type="eventLevelTagType(event)" effect="plain">
                      {{ eventLevelText[event.eventLevel] ?? event.eventLevel }}
                    </ElTag>
                  </div>
                </div>
                <p>{{ event.content || '暂无事件内容' }}</p>
                <div class="event-meta">
                  <span>操作人：{{ eventOperatorText(event) }}</span>
                  <span>事件时间：{{ event.eventTime || '暂无' }}</span>
                  <span>状态变化：{{ eventStatusText(event.beforeStatus) }} → {{ eventStatusText(event.afterStatus) }}</span>
                </div>
              </ElTimelineItem>
            </ElTimeline>
            <ElEmpty v-else description="暂无事件记录" />
          </section>
        </main>

        <aside class="detail-side">
          <section v-if="canJoinOrder" class="detail-section join-section">
            <div class="section-title">
              <div>
                <strong>加入拼单</strong>
                <span>提交后刷新详情</span>
              </div>
            </div>
            <ElForm ref="joinFormRef" :model="joinForm" :rules="joinRules" label-position="top">
              <ElFormItem label="餐品名称" prop="itemName">
                <ElInput v-model="joinForm.itemName" placeholder="例如：珍珠奶茶" />
              </ElFormItem>
              <div class="inline-fields">
                <ElFormItem label="数量" prop="quantity">
                  <ElInputNumber v-model="joinForm.quantity" :min="1" :precision="0" />
                </ElFormItem>
                <ElFormItem label="单价" prop="unitPrice">
                  <ElInputNumber v-model="joinForm.unitPrice" :min="0.01" :precision="2" :step="1" />
                </ElFormItem>
              </div>
              <ElFormItem label="餐品备注" prop="itemRemark">
                <ElInput v-model="joinForm.itemRemark" placeholder="少冰、加辣等" />
              </ElFormItem>
              <ElFormItem label="成员备注" prop="remark">
                <ElInput v-model="joinForm.remark" type="textarea" :rows="2" placeholder="给发起人的说明" />
              </ElFormItem>
              <ElButton type="primary" :loading="joinSubmitting" @click="submitJoin">提交加入</ElButton>
            </ElForm>
          </section>

          <section class="detail-section pickup-section">
            <div class="section-title">
              <div>
                <strong>取餐协同</strong>
                <span>发起人或取餐人推进状态</span>
              </div>
            </div>
            <div class="pickup-summary">
              <div>
                <ElIcon><UserFilled /></ElIcon>
                <span>取餐人</span>
                <strong>{{ pickupUser?.nickname ?? '暂未指定' }}</strong>
              </div>
              <div>
                <ElIcon><Location /></ElIcon>
                <span>取餐点</span>
                <strong>{{ pickupLocation || order.pickupLocation }}</strong>
              </div>
              <div>
                <ElIcon><Tickets /></ElIcon>
                <span>取餐状态</span>
                <strong>{{ pickupStatus ? pickupStatusText[pickupStatus] : '待指定' }}</strong>
              </div>
            </div>

            <div v-if="pickupStatus" class="pickup-flow">
              <div
                v-for="(step, index) in pickupFlowSteps"
                :key="step.status"
                class="pickup-flow__step"
                :class="{
                  'pickup-flow__step--done': index < pickupStepActive,
                  'pickup-flow__step--current': index === pickupStepActive,
                  'pickup-flow__step--pending': index > pickupStepActive
                }"
              >
                <span class="pickup-flow__index">{{ index + 1 }}</span>
                <span class="pickup-flow__label">{{ step.label }}</span>
              </div>
            </div>
            <div v-else class="pickup-placeholder">指定取餐人后显示取餐进度</div>

            <div v-if="canAssignPickup" class="pickup-action-panel">
              <strong>指定取餐人</strong>
              <ElSelect v-model="pickupForm.pickupUserId" placeholder="请选择取餐人">
                <ElOption
                  v-for="user in pickupOptions"
                  :key="user.id"
                  :label="`${user.nickname}（${user.username}）`"
                  :value="user.id"
                />
              </ElSelect>
              <ElInput v-model="pickupForm.pickupLocation" placeholder="取餐地点" />
              <ElInput v-model="pickupForm.estimatedArrivalTime" placeholder="预计到达时间，可选" />
              <ElInput v-model="pickupForm.remark" type="textarea" :rows="2" placeholder="取餐备注，可选" />
              <ElButton type="primary" :loading="operationLoading === 'assign-pickup'" @click="assignPickupUser">指定取餐人</ElButton>
            </div>

            <div v-if="canUpdatePickup && nextPickupStatus" class="pickup-action-panel">
              <strong>推进取餐状态：{{ pickupStatusText[nextPickupStatus] }}</strong>
              <ElInput v-model="pickupStatusForm.pickupLocation" placeholder="当前地点，可选" />
              <ElInput v-model="pickupStatusForm.remark" type="textarea" :rows="2" placeholder="状态备注，可选" />
              <ElButton type="success" :loading="operationLoading === 'pickup-status'" @click="updatePickupStatus">
                更新为{{ pickupStatusText[nextPickupStatus] }}
              </ElButton>
            </div>
          </section>

          <section v-if="canLockOrder" class="detail-section lock-section">
            <div class="section-title">
              <div>
                <strong>锁单确认</strong>
                <span>锁定后不能继续加入</span>
              </div>
            </div>
            <ElInput v-model="lockForm.remark" placeholder="锁单备注，可选" />
            <ElButton type="primary" :icon="Check" :loading="operationLoading === 'lock'" @click="lockOrder">锁定拼单</ElButton>
          </section>

          <section v-if="!canJoinOrder && !canLockOrder && !canCancelOrder && !canAssignPickup && !canUpdatePickup" class="detail-section next-action">
            <strong>{{ nextActionTitle }}</strong>
            <span>{{ nextActionText }}</span>
          </section>
        </aside>
      </div>

      <ElDialog v-model="cancelDialogVisible" title="取消拼单" width="420px">
        <div class="cancel-dialog">
          <ElAlert
            type="warning"
            title="取消后不能继续加入、锁单、模拟支付或推进取餐"
            show-icon
            :closable="false"
          />
          <ElInput
            v-model="cancelReason"
            type="textarea"
            :rows="4"
            maxlength="120"
            show-word-limit
            placeholder="请填写取消原因，例如：人数不够，先取消"
          />
        </div>
        <template #footer>
          <ElButton @click="cancelDialogVisible = false">返回</ElButton>
          <ElButton type="danger" :loading="operationLoading === 'cancel'" @click="cancelOrder">确认取消</ElButton>
        </template>
      </ElDialog>
    </template>
    <ElEmpty v-else description="未找到拼单详情" />
  </section>
</template>

<style scoped>
.detail-flow-page {
  display: grid;
  gap: 20px;
  max-width: 1220px;
  margin: 0 auto;
}

.detail-flow-page::before {
  content: "";
  position: fixed;
  inset: 68px 0 0 232px;
  z-index: -1;
  background:
    radial-gradient(circle at 15% 14%, rgba(86, 204, 242, 0.16), transparent 28%),
    radial-gradient(circle at 82% 9%, rgba(255, 163, 177, 0.2), transparent 30%),
    linear-gradient(135deg, #f6fbff 0%, #fff8f5 100%);
}

.detail-hero,
.flow-panel,
.detail-section {
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 52px rgba(31, 41, 55, 0.1);
  backdrop-filter: blur(14px);
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 26px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 0%, rgba(86, 204, 242, 0.34), transparent 26%),
    radial-gradient(circle at 96% 88%, rgba(255, 163, 177, 0.34), transparent 28%),
    linear-gradient(135deg, rgba(238, 248, 255, 0.96), rgba(255, 246, 247, 0.92));
}

.detail-hero__title h1 {
  margin: 12px 0 8px;
  font-size: 32px;
  line-height: 1.2;
}

.detail-hero__title p,
.detail-hero__facts,
.section-title span,
.next-action span,
.muted-text {
  color: #667085;
}

.detail-hero__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 16px;
}

.detail-hero__facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-hero__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.terminal-alert {
  border-radius: 14px;
}

.payment-sim-alert {
  border-radius: 14px;
}

.flow-panel {
  padding: 20px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.detail-main,
.detail-side {
  display: grid;
  gap: 18px;
  align-content: start;
}

.detail-section {
  padding: 18px;
}

.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-title strong {
  display: block;
  font-size: 18px;
}

.section-title span {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.amount-tile {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
}

.amount-tile span {
  color: #667085;
  font-size: 13px;
}

.amount-tile strong {
  color: #142033;
  font-size: 24px;
}

.amount-tile--green strong {
  color: #21a67a;
}

.amount-tile--blue strong {
  color: #2f7cf6;
}

.amount-tile--orange strong {
  color: #f59f00;
}

.discount-progress {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: #667085;
}

.participant-list {
  display: grid;
  gap: 12px;
}

.participant-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e3e8ef;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.participant-card__head,
.participant-money,
.participant-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.participant-card__head span,
.participant-remark {
  display: block;
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.meal-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #142033;
}

.meal-line .el-icon {
  margin-top: 2px;
  color: #2f7cf6;
}

.participant-money span {
  color: #667085;
}

.participant-money strong {
  color: #2f7cf6;
  font-size: 18px;
}

.event-timeline p {
  margin: 6px 0 0;
  color: #667085;
}

.event-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.event-title strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.event-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.event-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #667085;
  font-size: 12px;
}

.event-meta span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.inline-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.join-section .el-button,
.lock-section .el-button,
.pickup-action-panel .el-button {
  width: 100%;
}

.pickup-summary {
  display: grid;
  gap: 10px;
}

.pickup-summary > div {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  column-gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.pickup-summary .el-icon {
  grid-row: span 2;
  align-self: center;
  color: #2f7cf6;
  font-size: 20px;
}

.pickup-summary span {
  color: #667085;
  font-size: 13px;
}

.pickup-summary strong {
  margin-top: 2px;
}

.pickup-flow {
  position: relative;
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e5edf7;
  border-radius: 14px;
  background: #fbfdff;
}

.pickup-flow__step {
  position: relative;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 34px;
  color: #98a2b3;
  font-weight: 700;
}

.pickup-flow__step:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 13px;
  top: 28px;
  width: 2px;
  height: 14px;
  border-radius: 999px;
  background: #d8e0ea;
}

.pickup-flow__step--done,
.pickup-flow__step--current {
  color: #142033;
}

.pickup-flow__step--done::after {
  background: #42b883;
}

.pickup-flow__index {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 2px solid #d8e0ea;
  border-radius: 999px;
  background: #fff;
  color: inherit;
  font-size: 13px;
}

.pickup-flow__step--done .pickup-flow__index {
  border-color: #42b883;
  background: #42b883;
  color: #fff;
}

.pickup-flow__step--current .pickup-flow__index {
  border-color: #2f7cf6;
  background: #eff6ff;
  color: #2f7cf6;
}

.pickup-flow__label {
  min-width: 0;
  line-height: 1.35;
}

.pickup-placeholder,
.next-action {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px dashed #d8e0ea;
  border-radius: 12px;
  color: #667085;
  background: #f8fafc;
}

.pickup-action-panel,
.lock-section {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.cancel-dialog {
  display: grid;
  gap: 14px;
}

@media (max-width: 1100px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .detail-flow-page::before {
    inset-left: 0;
  }

  .amount-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .detail-hero {
    flex-direction: column;
  }

  .detail-hero__title h1 {
    font-size: 26px;
  }

  .amount-grid,
  .inline-fields,
  .event-meta {
    grid-template-columns: 1fr;
  }
}
</style>
