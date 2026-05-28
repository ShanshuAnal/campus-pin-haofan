<script setup lang="ts">
import { Check, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({
  title: '晚课后奶茶拼单',
  orderType: 'MILK_TEA',
  merchantName: '一号门奶茶',
  pickupLocation: '一教大厅门口',
  deadlineTime: '2026-05-28 19:30:00',
  maxParticipants: 5,
  discountThresholdAmount: 60,
  discountAmount: 10,
  remark: '先凑单，锁单后统一外部下单'
})

const submit = () => {
  ElMessage.success('已使用 mock 数据模拟发起成功')
  router.push('/hall')
}
</script>

<template>
  <section class="page-stack">
    <div class="page-header">
      <div>
        <h1>发起拼单</h1>
        <p>填写拼单基础信息，后续联调时会映射到 POST /api/group-orders。</p>
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
      <ElFormItem>
        <ElButton type="primary" :icon="Check" @click="submit">模拟创建</ElButton>
        <ElButton :icon="Plus" @click="router.push('/hall')">返回大厅</ElButton>
      </ElFormItem>
    </ElForm>
  </section>
</template>
