<script setup lang="ts">
import { formatMoney } from '@/utils/format'

defineProps<{
  label: string
  amount: number
  tone?: 'green' | 'blue' | 'orange'
}>()
</script>

<template>
  <div class="amount-stat" :class="tone">
    <span>{{ label }}</span>
    <strong>{{ formatMoney(amount) }}</strong>
  </div>
</template>
