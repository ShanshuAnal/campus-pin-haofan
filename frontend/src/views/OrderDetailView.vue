<script setup lang="ts">
import { Check, Location, Money, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AmountStat from '@/components/AmountStat.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import { useUserStore } from '@/stores/user'
import type { Participant, PickupStatus } from '@/types/order'
import { formatMoney, orderTypeText, paymentStatusText, pickupStatusText } from '@/utils/format'

const route = useRoute()
const orderStore = useOrderStore()
const userStore = useUserStore()
const detail = computed(() => orderStore.currentDetail)
const orderId = computed(() => Number(route.params.id))
const joinFormRef = ref<FormInstance>()
const joinSubmitting = ref(false)
const operationLoading = ref('')

const joinForm = reactive({
  itemName: '',
  quantity: 1,
  unitPrice: 0,
  itemRemark: '',
  remark: ''
})

const lockForm = reactive({
  remark: ''
})

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

const nextPickupStatusMap: Record<PickupStatus, PickupStatus | null> = {
  WAITING_ORDER: 'WAITING_DELIVERY',
  WAITING_DELIVERY: 'ARRIVED',
  ARRIVED: 'PICKED_UP',
  PICKED_UP: 'DISTRIBUTED',
  DISTRIBUTED: null
}

const currentUserId = computed(() => userStore.user?.id)

const isCreator = computed(
  () => Boolean(detail.value && currentUserId.value && detail.value.order.creator.id === currentUserId.value)
)

const currentParticipant = computed(() =>
  detail.value?.participants.find((participant) => participant.user.id === currentUserId.value)
)

const isPickupUser = computed(() => {
  const pickupUserId = detail.value?.pickupRecord?.pickupUser.id ?? detail.value?.order.pickupUser?.id
  return Boolean(currentUserId.value && pickupUserId === currentUserId.value)
})

const canLockOrder = computed(() => isCreator.value && detail.value?.order.status === 'CREATED')

const canAssignPickup = computed(() => isCreator.value && Boolean(detail.value?.participants.length))

const canUpdatePickup = computed(() => {
  const status = detail.value?.pickupRecord?.pickupStatus
  return Boolean((isCreator.value || isPickupUser.value) && status && status !== 'DISTRIBUTED')
})

const nextPickupStatus = computed(() => {
  const status = detail.value?.pickupRecord?.pickupStatus
  return status ? nextPickupStatusMap[status] : null
})

const discountGap = computed(() => {
  const order = detail.value?.order
  const threshold = order?.discountThresholdAmount ?? 0
  if (!order || threshold <= 0) {
    return 0
  }
  return Math.max(threshold - order.originalTotalAmount, 0)
})

const reachedDiscount = computed(() => {
  const order = detail.value?.order
  const threshold = order?.discountThresholdAmount ?? 0
  return Boolean(order && (threshold <= 0 || order.originalTotalAmount >= threshold))
})

const participantProgress = computed(() => {
  const order = detail.value?.order
  return order ? `${order.participantCount}/${order.maxParticipants}` : '-'
})

const pickupStepActive = computed(() => {
  const status = detail.value?.pickupRecord?.pickupStatus
  if (status === 'DISTRIBUTED') return 3
  if (status === 'PICKED_UP' || status === 'ARRIVED') return 2
  if (status === 'WAITING_DELIVERY') return 1
  return 0
})

const formatMealItems = (participant: Participant) =>
  participant.mealItems
    .map((item) => `${item.itemName} x${item.quantity}（${formatMoney(item.unitPrice)}）`)
    .join('、')

const refreshCurrentDetail = async () => {
  if (detail.value) {
    await orderStore.loadDetail(detail.value.order.id)
  }
}

const runOrderAction = async (key: string, successMessage: string, action: () => Promise<unknown>) => {
  operationLoading.value = key
  try {
    await action()
    ElMessage.success(successMessage)
    await refreshCurrentDetail()
  } catch {
    // 业务错误由 axios 统一展示，例如权限不足、重复付款、非法状态或取餐状态跳跃。
  } finally {
    operationLoading.value = ''
  }
}

const canMarkPayment = (participant: Participant) =>
  participant.user.id === currentUserId.value &&
  participant.paymentStatus === 'UNPAID' &&
  ['LOCKED', 'ORDERED', 'DELIVERING', 'ARRIVED'].includes(detail.value?.order.status ?? '')

const isCurrentUserUnpaidParticipant = (participant: Participant) =>
  participant.user.id === currentUserId.value && participant.paymentStatus === 'UNPAID'

const isWaitingForLockToPay = (participant: Participant) =>
  isCurrentUserUnpaidParticipant(participant) && detail.value?.order.status === 'CREATED'

const canConfirmPayment = (participant: Participant) => isCreator.value && participant.paymentStatus === 'PAID'

const hasParticipantAction = (participant: Participant) =>
  canMarkPayment(participant) || canConfirmPayment(participant) || isWaitingForLockToPay(participant)

const lockOrder = () => {
  if (!detail.value) return
  const targetOrderId = detail.value.order.id
  return runOrderAction('lock', '锁单成功，金额分摊已刷新', () =>
    orderStore.lockOrder(targetOrderId, {
      remark: lockForm.remark.trim() || undefined
    })
  )
}

const markPayment = (participant: Participant) => {
  if (!detail.value) return
  const targetOrderId = detail.value.order.id
  return runOrderAction(`mark-${participant.id}`, '已标记付款', () =>
    orderStore.markPayment(targetOrderId, participant.id, {
      remark: '前端标记已付款'
    })
  )
}

const confirmPayment = (participant: Participant) => {
  if (!detail.value) return
  const targetOrderId = detail.value.order.id
  return runOrderAction(`confirm-${participant.id}`, '已确认付款', () =>
    orderStore.confirmPayment(targetOrderId, participant.id, {
      remark: '发起人确认付款'
    })
  )
}

const assignPickupUser = () => {
  if (!detail.value || !pickupForm.pickupUserId) {
    ElMessage.warning('请选择取餐人')
    return
  }

  const targetOrderId = detail.value.order.id
  return runOrderAction('assign-pickup', '取餐人已指定', () =>
    orderStore.assignPickupUser(targetOrderId, {
      pickupUserId: pickupForm.pickupUserId as number,
      pickupLocation: pickupForm.pickupLocation.trim() || undefined,
      estimatedArrivalTime: pickupForm.estimatedArrivalTime.trim() || undefined,
      remark: pickupForm.remark.trim() || undefined
    })
  )
}

const updatePickupStatus = () => {
  if (!detail.value || !nextPickupStatus.value) {
    return
  }

  const targetOrderId = detail.value.order.id
  const targetStatus = nextPickupStatus.value
  return runOrderAction('pickup-status', `取餐状态已更新为${pickupStatusText[targetStatus]}`, () =>
    orderStore.updatePickupStatus(targetOrderId, {
      pickupStatus: targetStatus,
      pickupLocation: pickupStatusForm.pickupLocation.trim() || undefined,
      remark: pickupStatusForm.remark.trim() || undefined
    })
  )
}

const submitJoin = async () => {
  const valid = await joinFormRef.value?.validate().catch(() => false)
  if (!valid || !detail.value) {
    return
  }

  const targetOrderId = detail.value.order.id
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
  } catch {
    // 业务错误由 axios 统一展示后端返回的重复加入、锁定、满员、金额非法等提示。
  } finally {
    joinSubmitting.value = false
  }
}

const loadCurrentDetail = () => {
  if (Number.isFinite(orderId.value)) {
    orderStore.loadDetail(orderId.value)
  }
}

onMounted(loadCurrentDetail)
watch(() => route.params.id, loadCurrentDetail)
watch(
  detail,
  (value) => {
    if (!value) return
    pickupForm.pickupUserId = value.order.pickupUser?.id ?? value.pickupRecord?.pickupUser.id ?? value.participants[0]?.user.id
    pickupForm.pickupLocation = value.pickupRecord?.pickupLocation ?? value.order.pickupLocation
    pickupForm.estimatedArrivalTime = value.pickupRecord?.estimatedArrivalTime ?? ''
    pickupForm.remark = value.pickupRecord?.remark ?? ''
    pickupStatusForm.pickupLocation = value.pickupRecord?.pickupLocation ?? value.order.pickupLocation
    pickupStatusForm.remark = value.pickupRecord?.remark ?? ''
  },
  { immediate: true }
)
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
            <strong>拼单信息</strong>
            <span>详情数据来自真实接口</span>
          </div>
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="发起人">
              {{ detail.order.creator.nickname }}（{{ detail.order.creator.username }}）
            </ElDescriptionsItem>
            <ElDescriptionsItem label="人数">{{ participantProgress }}</ElDescriptionsItem>
            <ElDescriptionsItem label="截止时间">{{ detail.order.deadlineTime }}</ElDescriptionsItem>
            <ElDescriptionsItem label="取餐地点">{{ detail.order.pickupLocation }}</ElDescriptionsItem>
            <ElDescriptionsItem label="备注">{{ detail.order.remark || '无' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="付款状态">
              {{ detail.participants.length ? detail.participants.map((item) => paymentStatusText[item.paymentStatus]).join(' / ') : '暂无成员' }}
            </ElDescriptionsItem>
          </ElDescriptions>

          <div class="panel-title panel-title--spaced">
            <strong>金额分摊</strong>
            <span>当前金额与满减进度实时刷新</span>
          </div>
          <div class="stat-strip stat-strip--compact">
            <AmountStat label="原始总额" :amount="detail.order.originalTotalAmount" />
            <AmountStat label="实际优惠" :amount="detail.order.actualDiscountAmount" tone="green" />
            <AmountStat label="应付总额" :amount="detail.order.payableTotalAmount" tone="blue" />
            <AmountStat label="满减差额" :amount="discountGap" tone="orange" />
          </div>
          <div class="discount-line">
            <span>满减门槛：{{ formatMoney(detail.order.discountThresholdAmount) }}</span>
            <ElTag :type="reachedDiscount ? 'success' : 'warning'">
              {{ reachedDiscount ? '已达到满减' : '未达到满减' }}
            </ElTag>
          </div>

          <div class="panel-title panel-title--spaced">
            <strong>参与者与餐品明细</strong>
            <span>共 {{ detail.participants.length }} 人</span>
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
                <small v-if="row.remark" class="muted-text">备注：{{ row.remark }}</small>
              </template>
            </ElTableColumn>
            <ElTableColumn label="原价" width="110">
              <template #default="{ row }">{{ formatMoney(row.originalAmount) }}</template>
            </ElTableColumn>
            <ElTableColumn label="优惠分摊" width="110">
              <template #default="{ row }">{{ formatMoney(row.discountShareAmount) }}</template>
            </ElTableColumn>
            <ElTableColumn label="应付" width="110">
              <template #default="{ row }">{{ formatMoney(row.payableAmount) }}</template>
            </ElTableColumn>
            <ElTableColumn label="付款" width="120">
              <template #default="{ row }">
                <StatusTag :status="row.paymentStatus" type="payment" />
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="190">
              <template #default="{ row }">
                <div class="table-actions">
                  <ElButton
                    v-if="canMarkPayment(row)"
                    size="small"
                    type="primary"
                    :loading="operationLoading === `mark-${row.id}`"
                    @click="markPayment(row)"
                  >
                    标记付款
                  </ElButton>
                  <ElButton
                    v-if="canConfirmPayment(row)"
                    size="small"
                    type="success"
                    :loading="operationLoading === `confirm-${row.id}`"
                    @click="confirmPayment(row)"
                  >
                    确认付款
                  </ElButton>
                  <ElTooltip
                    v-if="isWaitingForLockToPay(row)"
                    content="锁单生成应付金额后才能标记付款"
                    placement="top"
                  >
                    <ElButton size="small" disabled>待锁单</ElButton>
                  </ElTooltip>
                  <span v-if="!hasParticipantAction(row)" class="muted-text">无可用操作</span>
                </div>
              </template>
            </ElTableColumn>
          </ElTable>

          <div v-if="canLockOrder" class="inline-action-panel">
            <div>
              <strong>锁定拼单</strong>
              <span>锁单后后端生成优惠分摊和成员应付金额</span>
            </div>
            <ElInput v-model="lockForm.remark" placeholder="锁单备注，可选" />
            <ElButton type="primary" :icon="Check" :loading="operationLoading === 'lock'" @click="lockOrder">
              锁定拼单
            </ElButton>
          </div>
        </div>

        <aside class="side-stack">
          <div class="detail-panel">
            <div class="panel-title">
              <strong>加入拼单</strong>
              <span>提交后刷新详情</span>
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
          </div>

          <div class="detail-panel">
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
              v-if="detail.pickupRecord"
              class="pickup-steps"
              :active="pickupStepActive"
              :space="64"
              finish-status="success"
              direction="vertical"
            >
              <ElStep title="等待下单" />
              <ElStep title="配送/到达" />
              <ElStep title="取餐分发" />
            </ElSteps>
            <div v-else class="pickup-placeholder">指定取餐人后显示取餐进度</div>

            <div v-if="canAssignPickup" class="pickup-action-panel">
              <div class="panel-title panel-title--compact">
                <strong>指定取餐人</strong>
                <span>取餐人必须是参与者</span>
              </div>
              <ElSelect v-model="pickupForm.pickupUserId" placeholder="请选择取餐人">
                <ElOption
                  v-for="participant in detail.participants"
                  :key="participant.id"
                  :label="`${participant.user.nickname}（${participant.user.username}）`"
                  :value="participant.user.id"
                />
              </ElSelect>
              <ElInput v-model="pickupForm.pickupLocation" placeholder="取餐地点" />
              <ElInput v-model="pickupForm.estimatedArrivalTime" placeholder="预计到达时间，可选" />
              <ElInput v-model="pickupForm.remark" type="textarea" :rows="2" placeholder="取餐备注，可选" />
              <ElButton type="primary" :loading="operationLoading === 'assign-pickup'" @click="assignPickupUser">
                指定取餐人
              </ElButton>
            </div>

            <div v-if="canUpdatePickup && nextPickupStatus" class="pickup-action-panel">
              <div class="panel-title panel-title--compact">
                <strong>更新取餐状态</strong>
                <span>下一步：{{ pickupStatusText[nextPickupStatus] }}</span>
              </div>
              <ElInput v-model="pickupStatusForm.pickupLocation" placeholder="当前地点，可选" />
              <ElInput v-model="pickupStatusForm.remark" type="textarea" :rows="2" placeholder="状态备注，可选" />
              <ElButton type="success" :loading="operationLoading === 'pickup-status'" @click="updatePickupStatus">
                更新为{{ pickupStatusText[nextPickupStatus] }}
              </ElButton>
            </div>
          </div>
        </aside>
      </div>
    </template>
    <ElEmpty v-else description="未找到拼单详情" />
  </section>
</template>
