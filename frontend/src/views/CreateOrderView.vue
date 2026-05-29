<script setup lang="ts">
import { Check, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useOrderStore } from '@/stores/orders'
import type { CreateGroupOrderRequest, JoinMealItemRequest } from '@/types/order'

const router = useRouter()
const orderStore = useOrderStore()
const submitting = ref(false)

interface CreatorMealItem extends JoinMealItemRequest {
  key: number
}

const pad = (value: number) => String(value).padStart(2, '0')

const formatDateTime = (date: Date) => {
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const form = reactive<CreateGroupOrderRequest>({
  title: '晚课后奶茶拼单',
  orderType: 'MILK_TEA',
  merchantName: '一号门奶茶',
  pickupLocation: '一教大厅门口',
  deadlineTime: formatDateTime(new Date(Date.now() + 2 * 60 * 60 * 1000)),
  maxParticipants: 5,
  discountThresholdAmount: 60,
  discountAmount: 10,
  remark: '先凑单，锁单后统一外部下单'
})

const creatorItems = ref<CreatorMealItem[]>([
  {
    key: Date.now(),
    itemName: '珍珠奶茶',
    quantity: 1,
    unitPrice: 15,
    remark: ''
  }
])

const hasValidCreatorItems = computed(() =>
  creatorItems.value.some((item) => item.itemName.trim() && item.quantity > 0 && item.unitPrice > 0)
)

const normalizedCreatorItems = computed(() =>
  creatorItems.value
    .filter((item) => item.itemName.trim() && item.quantity > 0 && item.unitPrice > 0)
    .map((item) => ({
      itemName: item.itemName.trim(),
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      remark: item.remark?.trim() || undefined
    }))
)

const creatorItemsTotal = computed(() =>
  normalizedCreatorItems.value.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0)
)

const addCreatorItem = () => {
  creatorItems.value.push({
    key: Date.now() + Math.random(),
    itemName: '',
    quantity: 1,
    unitPrice: 0,
    remark: ''
  })
}

const removeCreatorItem = (key: number) => {
  if (creatorItems.value.length === 1) {
    creatorItems.value = [
      {
        key: Date.now(),
        itemName: '',
        quantity: 1,
        unitPrice: 0,
        remark: ''
      }
    ]
    return
  }
  creatorItems.value = creatorItems.value.filter((item) => item.key !== key)
}

const submit = async () => {
  if (!hasValidCreatorItems.value) {
    ElMessage.info('你还没有填写自己的餐品，可以先创建空拼单，之后邀请同学加入。')
  }
  submitting.value = true
  try {
    const createdOrder = await orderStore.createOrder({
      ...form,
      creatorItems: normalizedCreatorItems.value.length ? normalizedCreatorItems.value : undefined
    })
    ElMessage.success('拼单创建成功')
    router.push(`/orders/${createdOrder.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>发起拼单</h1>
        <p>填写拼单基础信息，也可以顺手填好自己的点餐，创建后直接进入详情页。</p>
      </div>
    </div>

    <ElForm class="form-panel" :model="form" label-width="120px">
      <ElFormItem label="拼单标题">
        <ElInput v-model="form.title" />
      </ElFormItem>
      <ElFormItem label="拼单类型">
        <ElSelect v-model="form.orderType">
          <ElOption label="外卖拼单" value="TAKEOUT" />
          <ElOption label="食堂凑单" value="CANTEEN" />
          <ElOption label="奶茶拼单" value="MILK_TEA" />
          <ElOption label="夜宵拼单" value="MIDNIGHT_SNACK" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="商家或窗口">
        <ElInput v-model="form.merchantName" />
      </ElFormItem>
      <ElFormItem label="取餐地点">
        <ElInput v-model="form.pickupLocation" />
      </ElFormItem>
      <ElFormItem label="截止时间">
        <ElInput v-model="form.deadlineTime" />
      </ElFormItem>
      <ElFormItem label="人数上限">
        <ElInputNumber v-model="form.maxParticipants" :min="2" :max="20" />
      </ElFormItem>
      <ElFormItem label="优惠配置">
        <div class="inline-fields">
          <ElInputNumber v-model="form.discountThresholdAmount" :min="0" :precision="2" />
          <span>满减</span>
          <ElInputNumber v-model="form.discountAmount" :min="0" :precision="2" />
        </div>
      </ElFormItem>
      <ElFormItem label="备注">
        <ElInput v-model="form.remark" type="textarea" :rows="3" />
      </ElFormItem>
      <ElDivider />
      <section class="creator-items">
        <div class="creator-items__header">
          <div>
            <strong>我的点餐</strong>
            <span>建议发起时一起填写，系统会把你自动加入拼单。</span>
          </div>
          <ElButton :icon="Plus" @click="addCreatorItem">添加餐品</ElButton>
        </div>
        <ElAlert
          v-if="!hasValidCreatorItems"
          type="warning"
          title="可以先创建空拼单，但发起人填写自己的餐品后，后续不用再回详情页重复加入。"
          show-icon
          :closable="false"
        />
        <div class="creator-item-list">
          <article v-for="item in creatorItems" :key="item.key" class="creator-item-row">
            <ElInput v-model="item.itemName" placeholder="餐品名称，例如：珍珠奶茶" />
            <ElInputNumber v-model="item.quantity" :min="1" :precision="0" />
            <ElInputNumber v-model="item.unitPrice" :min="0" :precision="2" :step="1" />
            <ElInput v-model="item.remark" placeholder="备注，可选" />
            <ElButton :icon="Delete" circle @click="removeCreatorItem(item.key)" />
          </article>
        </div>
        <div class="creator-items__summary">
          <span>我的点餐合计</span>
          <strong>¥{{ creatorItemsTotal.toFixed(2) }}</strong>
        </div>
      </section>
      <ElFormItem>
        <ElButton type="primary" :icon="Check" :loading="submitting" @click="submit">创建并进入详情</ElButton>
        <ElButton :icon="Plus" @click="router.push('/hall')">返回大厅</ElButton>
      </ElFormItem>
    </ElForm>
  </section>
</template>

<style scoped>
.creator-items {
  display: grid;
  gap: 14px;
}

.creator-items__header,
.creator-items__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.creator-items__header strong {
  display: block;
  font-size: 17px;
}

.creator-items__header span,
.creator-items__summary span {
  color: #667085;
  font-size: 13px;
}

.creator-item-list {
  display: grid;
  gap: 10px;
}

.creator-item-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 118px 132px minmax(160px, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.creator-items__summary {
  justify-content: flex-end;
}

.creator-items__summary strong {
  color: #2f7cf6;
  font-size: 22px;
}

@media (max-width: 900px) {
  .creator-item-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .creator-items__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .creator-item-row {
    grid-template-columns: 1fr;
  }
}
</style>
