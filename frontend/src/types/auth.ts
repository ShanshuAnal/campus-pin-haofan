export interface UserSummary {
  id: number
  username: string
  nickname: string
  phone: string | null
  status: 'ACTIVE' | 'DISABLED'
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
}

export interface RegisterResponse {
  userId: number
  username: string
  nickname: string
  status: 'ACTIVE' | 'DISABLED'
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserSummary
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface LogoutResponse {
  logout: boolean
}
