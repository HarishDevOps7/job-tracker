
import api from './api'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}

export interface AuthResponse {
  id: number
  fullName: string
  email: string
  role: 'ADMIN' | 'USER'
  message?: string
}

export const authService = {
  async getCsrfToken() {
    return api.get('/auth/csrf')
  },

  async login(data: LoginRequest) {
    return api.post<AuthResponse>('/auth/login', data)
  },

  async register(data: RegisterRequest) {
    return api.post<AuthResponse>('/auth/register', data)
  },

  async getCurrentUser() {
    return api.get<AuthResponse>('/auth/me')
  },

  async logout() {
    return api.post('/auth/logout')
  },
}
