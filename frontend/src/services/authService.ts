
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

export interface MobileRequest {
  mobileNumber: string
}

export interface MobileRegisterRequest {
  fullName: string
  mobileNumber: string
}

export interface OtpVerificationRequest {
  email: string
  otp: string
}

export interface MobileOtpVerificationRequest {
  mobileNumber: string
  otp: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface VerifyResetOtpRequest {
  email: string
  otp: string
}

export interface ResetPasswordRequest {
  email: string
  resetToken: string
  newPassword: string
}

export interface PasswordResetResponse {
  message: string
  resetToken?: string
}

export interface AuthResponse {
  id: number
  fullName: string
  email: string | null
  mobileNumber?: string | null
  role: 'ADMIN' | 'USER'
  emailVerified?: boolean
  mobileVerified?: boolean
  message?: string
}

export const authService = {

  // CSRF
  async getCsrfToken() {
    return api.get('/auth/csrf')
  },

  // Email and password
  async login(data: LoginRequest) {
    return api.post<AuthResponse>('/auth/login', data)
  },

  async register(data: RegisterRequest) {
    return api.post('/auth/register', data)
  },

  // Email OTP
  async verifyEmailOtp(data: OtpVerificationRequest) {
    return api.post('/auth/verify-email', data)
  },

  // Mobile registration
  async registerMobile(data: MobileRegisterRequest) {
    return api.post('/auth/register/mobile', data)
  },

  async verifyMobileOtp(data: MobileOtpVerificationRequest) {
    return api.post('/auth/verify-mobile', data)
  },

  // Mobile login
  async sendMobileLoginOtp(data: MobileRequest) {
    return api.post('/auth/login/mobile', data)
  },

  // Forgot Password
  async forgotPassword(data: ForgotPasswordRequest) {
    return api.post<PasswordResetResponse>(
      '/auth/forgot-password',
      data
    )
  },

  // Verify Password Reset OTP
  async verifyResetOtp(data: VerifyResetOtpRequest) {
    return api.post<PasswordResetResponse>(
      '/auth/verify-reset-otp',
      data
    )
  },

  // Reset Password
  async resetPassword(data: ResetPasswordRequest) {
    return api.post<PasswordResetResponse>(
      '/auth/reset-password',
      data
    )
  },


  async verifyMobileLoginOtp(data: MobileOtpVerificationRequest) {
    return api.post('/auth/verify-mobile-login', data)
  },

  // Current user
  async getCurrentUser() {
    return api.get<AuthResponse>('/auth/me')
  },

  // Logout
  async logout() {
    return api.post('/auth/logout')
  },

  // Google OAuth
  googleLogin() {
    window.location.href =
      'http://localhost:8081/oauth2/authorization/google'
  },
}
