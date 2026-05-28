import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { authApi, tokenStorage } from '@/api'
import type { LoginRequest, RegisterRequest, UserSummary } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserSummary | null>(tokenStorage.getUser())
  const accessToken = ref(tokenStorage.getAccessToken())
  const refreshToken = ref(tokenStorage.getRefreshToken())
  const restoring = ref(false)
  const restored = ref(false)

  const isLoggedIn = computed(() => Boolean(user.value && accessToken.value))

  const saveSession = (result: { accessToken: string; refreshToken: string; user: UserSummary }) => {
    user.value = result.user
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    tokenStorage.setAccessToken(result.accessToken)
    tokenStorage.setRefreshToken(result.refreshToken)
    tokenStorage.setUser(result.user)
  }

  const clearSession = () => {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    tokenStorage.clear()
  }

  const register = (payload: RegisterRequest) => authApi.register(payload)

  const login = async (payload: LoginRequest) => {
    const result = await authApi.login(payload)
    saveSession(result)
    restored.value = true
  }

  const refreshSession = async () => {
    if (!refreshToken.value) {
      clearSession()
      return false
    }

    const result = await authApi.refresh({ refreshToken: refreshToken.value })
    saveSession(result)
    return true
  }

  const restoreSession = async () => {
    if (restored.value || restoring.value) {
      return isLoggedIn.value
    }

    if (!accessToken.value && !refreshToken.value) {
      clearSession()
      restored.value = true
      return false
    }

    restoring.value = true
    try {
      if (accessToken.value) {
        const currentUser = await authApi.me()
        user.value = currentUser
        tokenStorage.setUser(currentUser)
        restored.value = true
        return true
      }

      const refreshed = await refreshSession()
      restored.value = true
      return refreshed
    } catch {
      try {
        const refreshed = await refreshSession()
        restored.value = true
        return refreshed
      } catch {
        clearSession()
        restored.value = true
        return false
      }
    } finally {
      restoring.value = false
    }
  }

  const logout = async () => {
    const currentRefreshToken = refreshToken.value
    try {
      if (currentRefreshToken && accessToken.value) {
        await authApi.logout({ refreshToken: currentRefreshToken })
      }
    } finally {
      clearSession()
      restored.value = true
    }
  }

  return {
    user,
    accessToken,
    refreshToken,
    restoring,
    restored,
    isLoggedIn,
    register,
    login,
    refreshSession,
    restoreSession,
    logout
  }
})
