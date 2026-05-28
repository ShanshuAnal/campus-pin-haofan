<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const registerLoading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const goAfterLogin = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/hall'
  router.push(redirect)
}

const submit = async () => {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    goAfterLogin()
  } finally {
    loading.value = false
  }
}

const registerAndLogin = async () => {
  registerLoading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: '演示用户'
    })
    await userStore.login(form)
    ElMessage.success('注册并登录成功')
    goAfterLogin()
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <span class="brand__mark">拼</span>
        <h1>校园拼好饭</h1>
        <p>围绕发起拼单、成员加入、金额分摊、付款标记和取餐协同的课堂演示前端。</p>
      </div>
      <ElForm class="login-form" :model="form" label-position="top" @submit.prevent="submit">
        <ElFormItem label="账号">
          <ElInput v-model="form.username" :prefix-icon="User" />
        </ElFormItem>
        <ElFormItem label="密码">
          <ElInput v-model="form.password" :prefix-icon="Lock" show-password type="password" />
        </ElFormItem>
        <ElButton type="primary" size="large" :loading="loading" native-type="submit">
          登录进入演示
        </ElButton>
        <ElButton class="register-button" text :loading="registerLoading" @click="registerAndLogin">
          注册演示账号
        </ElButton>
      </ElForm>
    </section>
  </main>
</template>
