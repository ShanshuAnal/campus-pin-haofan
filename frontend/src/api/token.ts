import type { UserSummary } from '@/types/auth'

const ACCESS_TOKEN_KEY = 'campus_haofan_access_token'
const REFRESH_TOKEN_KEY = 'campus_haofan_refresh_token'
const USER_KEY = 'campus_haofan_user'

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  setAccessToken: (token: string) => localStorage.setItem(ACCESS_TOKEN_KEY, token),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setRefreshToken: (token: string) => localStorage.setItem(REFRESH_TOKEN_KEY, token),
  getUser: (): UserSummary | null => {
    const rawUser = localStorage.getItem(USER_KEY)
    if (!rawUser) {
      return null
    }

    try {
      return JSON.parse(rawUser) as UserSummary
    } catch {
      localStorage.removeItem(USER_KEY)
      return null
    }
  },
  setUser: (user: UserSummary) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}
