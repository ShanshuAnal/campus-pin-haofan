import { http } from './http'
import type {
  LoginRequest,
  LoginResponse,
  LogoutRequest,
  LogoutResponse,
  RefreshRequest,
  RegisterRequest,
  RegisterResponse,
  UserSummary
} from '@/types/auth'

const unwrap = <T>(request: Promise<unknown>) => request as Promise<T>

export const authApi = {
  register: (data: RegisterRequest) => unwrap<RegisterResponse>(http.post('/auth/register', data)),
  login: (data: LoginRequest) => unwrap<LoginResponse>(http.post('/auth/login', data)),
  logout: (data: LogoutRequest) => unwrap<LogoutResponse>(http.post('/auth/logout', data)),
  refresh: (data: RefreshRequest) => unwrap<LoginResponse>(http.post('/auth/refresh', data)),
  me: () => unwrap<UserSummary>(http.get('/auth/me'))
}
